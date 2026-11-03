package com.snapqr.app.content

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import com.google.mlkit.vision.barcode.common.Barcode
import java.net.URISyntaxException

/**
 * 스캔 결과를 "원래 의도에 맞는" 목적지로 보낸다.
 *
 *  웹 링크  : 해당 링크를 처리하는 네이티브 앱(App Links)이 있으면 앱으로, 없으면 Custom Tab
 *  플레이스토어 링크 : Play 스토어 앱으로 직접
 *  intent:// : 대상 앱 실행, 미설치 시 browser_fallback_url → 플레이스토어 순으로 폴백
 *  커스텀 스킴 : 해당 앱 실행, 없으면 안내
 *  Wi-Fi / 전화 / 문자 / 메일 / 지도 / 연락처 / 일정 : 각 시스템 앱으로
 */
object ActionLauncher {

    const val PLAY_STORE_PACKAGE = "com.android.vending"

    fun open(context: Context, content: ScanContent) {
        when (content) {
            is ScanContent.Web -> openWeb(context, content.uri, content.playStorePackage)
            is ScanContent.AppLink -> openAppLink(context, content)
            is ScanContent.Wifi -> connectWifi(context, content)
            is ScanContent.Phone -> start(context, Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", content.number, null)))
            is ScanContent.Sms -> start(
                context,
                Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", content.number, null))
                    .putExtra("sms_body", content.body.orEmpty()),
            )
            is ScanContent.Email -> start(
                context,
                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(content.address))
                    content.subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                    content.body?.let { putExtra(Intent.EXTRA_TEXT, it) }
                },
            )
            is ScanContent.Geo -> start(
                context,
                Intent(Intent.ACTION_VIEW, Uri.parse("geo:${content.lat},${content.lng}?q=${content.lat},${content.lng}")),
            )
            is ScanContent.Contact -> start(context, contactIntent(content))
            is ScanContent.Event -> start(context, eventIntent(content))
            is ScanContent.Text -> share(context, content.raw)
        }
    }

    // region Web

    fun openWeb(context: Context, uri: Uri, playStorePackage: String? = ScanContent.playStorePackageOf(uri)) {
        if (playStorePackage != null && openPlayStore(context, playStorePackage)) return

        // 1) 링크를 처리하는 네이티브 앱(인증된 App Link 또는 유일한 핸들러)이 있으면 그 앱으로 바로
        val view = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                context.startActivity(
                    Intent(view).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER
                    )
                )
                return
            } catch (_: ActivityNotFoundException) {
                // 네이티브 앱 없음 → 브라우저로
            }
        }

        // 2) Custom Tab (빠르고 앱 안에서 자연스럽게 열림), 3) 일반 브라우저
        try {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder().setToolbarColor(0xFF1C1C1E.toInt()).build()
                )
                .build()
                .apply { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                .launchUrl(context, uri)
        } catch (_: ActivityNotFoundException) {
            start(context, view)
        }
    }

    fun openPlayStore(context: Context, packageName: String): Boolean {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            .setPackage(PLAY_STORE_PACKAGE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(market)
            true
        } catch (_: ActivityNotFoundException) {
            // Play 스토어 앱이 없는 기기 → 다른 마켓 앱 또는 웹
            try {
                context.startActivity(Intent(market).setPackage(null))
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }
    }

    // endregion

    // region App links

    private fun openAppLink(context: Context, content: ScanContent.AppLink) {
        val uri = content.uri
        when (uri.scheme?.lowercase()) {
            "market" -> {
                val pkg = content.playStorePackage
                if (pkg != null && openPlayStore(context, pkg)) return
                if (!tryStart(context, Intent(Intent.ACTION_VIEW, uri))) {
                    pkg?.let { openWeb(context, Uri.parse("https://play.google.com/store/apps/details?id=$it"), null) }
                }
            }
            "intent" -> openIntentUri(context, content.raw)
            else -> {
                if (!tryStart(context, Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))) {
                    toast(context, "이 링크를 열 수 있는 앱이 없어요 (${uri.scheme}://)")
                }
            }
        }
    }

    /** Chrome 과 동일한 규칙으로 intent:// URI 처리 (보안상 component/selector 제거) */
    private fun openIntentUri(context: Context, raw: String) {
        val intent = try {
            Intent.parseUri(raw, Intent.URI_INTENT_SCHEME)
        } catch (_: URISyntaxException) {
            toast(context, "잘못된 앱 링크예요")
            return
        }
        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
        val targetPackage = intent.`package`

        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.component = null
        intent.selector = null

        if (tryStart(context, intent)) return

        when {
            !fallbackUrl.isNullOrBlank() -> {
                val fb = Uri.parse(fallbackUrl)
                if (fb.scheme == "http" || fb.scheme == "https") {
                    openWeb(context, fb)
                    return
                }
            }
        }
        if (targetPackage != null && openPlayStore(context, targetPackage)) return
        toast(context, "이 링크를 열 수 있는 앱이 없어요")
    }

    // endregion

    // region Wi-Fi

    private fun connectWifi(context: Context, wifi: ScanContent.Wifi) {
        // 비밀번호는 항상 클립보드에도 복사 (수동 입력 대비)
        wifi.password?.let { copy(context, it, label = "Wi-Fi password", silent = true) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val builder = WifiNetworkSuggestion.Builder().setSsid(wifi.ssid)
            val pw = wifi.password
            val isWpa3 = Regex("(?i)(^|[;:])T:(SAE|WPA3)(;|$)").containsMatchIn(wifi.raw)
            if (pw != null) {
                when {
                    isWpa3 -> builder.setWpa3Passphrase(pw)
                    wifi.encryption == Barcode.WiFi.TYPE_WPA -> builder.setWpa2Passphrase(pw)
                    // WEP 은 Suggestion API 미지원 → 설정 화면으로
                    wifi.encryption == Barcode.WiFi.TYPE_WEP -> {
                        openWifiSettings(context, wifi); return
                    }
                    else -> builder.setWpa2Passphrase(pw)
                }
            }
            if (wifi.hidden) builder.setIsHiddenSsid(true)
            val suggestion = runCatching { builder.build() }.getOrNull()
            if (suggestion != null) {
                val intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS)
                    .putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, arrayListOf(suggestion))
                if (tryStart(context, intent)) return
            }
        }
        openWifiSettings(context, wifi)
    }

    private fun openWifiSettings(context: Context, wifi: ScanContent.Wifi) {
        if (wifi.password != null) toast(context, "비밀번호를 복사했어요. \"${wifi.ssid}\" 에 연결하세요")
        val panel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_WIFI)
        else Intent(Settings.ACTION_WIFI_SETTINGS)
        start(context, panel)
    }

    // endregion

    // region Contact / Event

    private fun contactIntent(c: ScanContent.Contact) =
        Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            c.name?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
            c.org?.let { putExtra(ContactsContract.Intents.Insert.COMPANY, it) }
            c.phones.getOrNull(0)?.let { putExtra(ContactsContract.Intents.Insert.PHONE, it) }
            c.phones.getOrNull(1)?.let { putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, it) }
            c.phones.getOrNull(2)?.let { putExtra(ContactsContract.Intents.Insert.TERTIARY_PHONE, it) }
            c.emails.getOrNull(0)?.let { putExtra(ContactsContract.Intents.Insert.EMAIL, it) }
            c.emails.getOrNull(1)?.let { putExtra(ContactsContract.Intents.Insert.SECONDARY_EMAIL, it) }
            c.address?.let { putExtra(ContactsContract.Intents.Insert.POSTAL, it) }
            c.urls.firstOrNull()?.let { putExtra(ContactsContract.Intents.Insert.NOTES, it) }
        }

    private fun eventIntent(e: ScanContent.Event) =
        Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
            e.summary?.let { putExtra(CalendarContract.Events.TITLE, it) }
            e.location?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
            e.description?.let { putExtra(CalendarContract.Events.DESCRIPTION, it) }
            e.startMillis?.let { putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it) }
            e.endMillis?.let { putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it) }
        }

    // endregion

    // region Utilities

    fun copy(context: Context, text: String, label: String = "QR", silent: Boolean = false) {
        val cm = context.getSystemService(ClipboardManager::class.java) ?: return
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        // Android 13+ 는 시스템이 복사 알림을 띄우므로 중복 토스트 생략
        if (!silent && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) toast(context, "복사했어요")
    }

    fun share(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        start(context, Intent.createChooser(send, null))
    }

    fun webSearch(context: Context, text: String) {
        openWeb(context, Uri.parse("https://www.google.com/search?q=" + Uri.encode(text)), null)
    }

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    private fun start(context: Context, intent: Intent) {
        if (!tryStart(context, intent)) toast(context, "이 작업을 처리할 앱이 없어요")
    }

    private fun toast(context: Context, msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    // endregion
}
