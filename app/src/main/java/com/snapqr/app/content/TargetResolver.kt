package com.snapqr.app.content

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build

/** "어느 앱으로 열리는지" 를 미리 보여주기 위한 대상 앱 정보 */
data class TargetApp(val packageName: String, val label: String, val icon: Drawable?, val installed: Boolean = true)

object TargetResolver {

    fun resolve(context: Context, content: ScanContent): TargetApp? {
        val pm = context.packageManager
        return when (content) {
            is ScanContent.Web ->
                if (content.playStorePackage != null) app(pm, ActionLauncher.PLAY_STORE_PACKAGE)
                else nativeHandler(pm, content.uri)
            is ScanContent.AppLink -> when (content.uri.scheme?.lowercase()) {
                "market" -> app(pm, ActionLauncher.PLAY_STORE_PACKAGE)
                "intent" -> runCatching { Intent.parseUri(content.raw, Intent.URI_INTENT_SCHEME) }.getOrNull()
                    ?.let { intent ->
                        intent.`package`?.let { pkg -> app(pm, pkg) ?: TargetApp(pkg, pkg, null, installed = false) }
                            ?: handler(pm, intent.apply { component = null; selector = null })
                    }
                else -> handler(pm, Intent(Intent.ACTION_VIEW, content.uri))
            }
            else -> null
        }
    }

    /** 브라우저가 아닌 앱이 이 링크를 처리하는지 (App Links / 딥링크) */
    private fun nativeHandler(pm: PackageManager, uri: Uri): TargetApp? {
        val browsable = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        val browsers = query(pm, Intent(Intent.ACTION_VIEW, Uri.parse("https://example.invalid/"))
            .addCategory(Intent.CATEGORY_BROWSABLE)).toSet()
        val handlers = query(pm, browsable).filter { it !in browsers }
        return handlers.firstOrNull()?.let { app(pm, it) }
    }

    private fun handler(pm: PackageManager, intent: Intent): TargetApp? {
        val info = if (Build.VERSION.SDK_INT >= 33) {
            pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION") pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        } ?: return null
        val pkg = info.activityInfo?.packageName ?: return null
        // 여러 앱이 처리 가능하면 시스템 선택기(android) 가 반환됨
        if (pkg == "android") return null
        return app(pm, pkg)
    }

    private fun query(pm: PackageManager, intent: Intent): List<String> {
        val list = if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION") pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return list.mapNotNull { it.activityInfo?.packageName }.distinct()
    }

    private fun app(pm: PackageManager, pkg: String): TargetApp? = runCatching {
        val ai = if (Build.VERSION.SDK_INT >= 33) {
            pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") pm.getApplicationInfo(pkg, 0)
        }
        TargetApp(pkg, pm.getApplicationLabel(ai).toString(), pm.getApplicationIcon(ai))
    }.getOrNull()
}
