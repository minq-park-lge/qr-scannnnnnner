package com.snapqr.app.content

import android.net.Uri

/** QR 원문을 의도(intent) 단위로 해석한 결과 */
sealed interface ScanContent {
    val raw: String

    /** http/https 웹 링크 */
    data class Web(override val raw: String, val uri: Uri) : ScanContent {
        val playStorePackage: String? = playStorePackageOf(uri)
    }

    /** market://, intent://, 앱 커스텀 스킴 (예: thinq://, kakaotalk://) */
    data class AppLink(override val raw: String, val uri: Uri) : ScanContent {
        val playStorePackage: String? = playStorePackageOf(uri)
    }

    data class Wifi(
        override val raw: String,
        val ssid: String,
        val password: String?,
        val encryption: Int, // Barcode.WiFi.TYPE_*
        val hidden: Boolean,
    ) : ScanContent

    data class Phone(override val raw: String, val number: String) : ScanContent
    data class Sms(override val raw: String, val number: String, val body: String?) : ScanContent
    data class Email(override val raw: String, val address: String, val subject: String?, val body: String?) : ScanContent
    data class Geo(override val raw: String, val lat: Double, val lng: Double) : ScanContent

    data class Contact(
        override val raw: String,
        val name: String?,
        val org: String?,
        val phones: List<String>,
        val emails: List<String>,
        val urls: List<String>,
        val address: String?,
    ) : ScanContent

    data class Event(
        override val raw: String,
        val summary: String?,
        val location: String?,
        val description: String?,
        val startMillis: Long?,
        val endMillis: Long?,
    ) : ScanContent

    data class Text(override val raw: String) : ScanContent

    companion object {
        fun playStorePackageOf(uri: Uri): String? {
            val scheme = uri.scheme?.lowercase()
            val host = uri.host?.lowercase()
            return when {
                scheme == "market" -> uri.getQueryParameter("id")
                (scheme == "https" || scheme == "http") && host == "play.google.com" &&
                    uri.path?.startsWith("/store/apps/details") == true -> uri.getQueryParameter("id")
                else -> null
            }?.takeIf { it.isNotBlank() }
        }
    }
}
