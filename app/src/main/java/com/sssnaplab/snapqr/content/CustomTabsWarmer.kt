package com.sssnaplab.snapqr.content

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession

/**
 * 브라우저(Custom Tabs) 예열 + 프리페치.
 *
 *  - 스캐너 화면이 보이는 동안 브라우저 프로세스를 미리 띄워 둠 (warmup)
 *  - QR 을 잡는 순간 mayLaunchUrl 로 DNS/TLS/페이지 로딩을 미리 시작
 *  → "열기" 를 누르면 페이지가 거의 즉시 표시됨
 */
object CustomTabsWarmer {

    @Volatile var session: CustomTabsSession? = null
        private set

    private var connection: CustomTabsServiceConnection? = null

    fun bind(context: Context) {
        if (connection != null) return
        val app = context.applicationContext
        val pkg = runCatching { CustomTabsClient.getPackageName(app, null) }.getOrNull() ?: return
        val conn = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                runCatching { client.warmup(0) }
                session = runCatching { client.newSession(null) }.getOrNull()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                session = null
            }
        }
        if (runCatching { CustomTabsClient.bindCustomTabsService(app, pkg, conn) }.getOrDefault(false)) {
            connection = conn
        }
    }

    fun unbind(context: Context) {
        connection?.let { runCatching { context.applicationContext.unbindService(it) } }
        connection = null
        session = null
    }

    /** 위험하지 않다고 판단된 링크만 호출할 것 (악성 페이지 선로딩 방지) */
    fun prefetch(uri: Uri) {
        runCatching { session?.mayLaunchUrl(uri, null, null) }
    }
}
