package com.sssnaplab.snapqr.preview

import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.nio.charset.Charset
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * URL 프리뷰 로더.
 *  - 리다이렉트를 끝까지 따라가 최종 도메인을 보여줌 (단축 URL 피싱 확인용)
 *  - <head> 부분만 최대 256KB 까지 읽고 끊어서 빠르게 응답
 *  - 짧은 타임아웃 + 메모리 캐시
 */
object LinkPreviewer {

    private const val MAX_BYTES = 256L * 1024
    private val cache = LruCache<String, LinkPreview>(64)

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(8, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    suspend fun load(uri: Uri): LinkPreview {
        val key = uri.toString()
        cache.get(key)?.let { return it }

        val request = Request.Builder()
            .url(key)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/130.0 Mobile Safari/537.36",
            )
            .header("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", Locale.getDefault().toLanguageTag() + ",en;q=0.7")
            .build()

        val response = client.newCall(request).await()
        val preview = response.use { res ->
            withContext(Dispatchers.IO) { read(res) }
        }
        cache.put(key, preview)
        return preview
    }

    private fun read(res: Response): LinkPreview {
        val finalUrl = res.request.url.toString()
        val finalUri = Uri.parse(finalUrl)
        val body = res.body ?: return LinkPreview(finalUri, null, null, null, faviconOf(finalUri), null)
        val type = body.contentType()

        if (type?.type == "image") {
            return LinkPreview(finalUri, null, null, finalUrl, null, null)
        }
        if (!res.isSuccessful || (type != null && type.subtype !in setOf("html", "xhtml+xml"))) {
            return LinkPreview(finalUri, null, null, null, faviconOf(finalUri), null)
        }

        // <head> 만 필요하므로 </head> 를 만나거나 한도에 도달하면 중단
        val source = body.source()
        val buffer = Buffer()
        while (buffer.size < MAX_BYTES) {
            if (source.read(buffer, 8 * 1024) == -1L) break
            val peek = buffer.snapshot().utf8()
            if (peek.contains("</head>", ignoreCase = true)) break
        }
        val bytes = buffer.readByteArray()
        val latin = String(bytes, Charsets.ISO_8859_1)
        val charset = type?.charset()
            ?: HtmlMetaParser.sniffCharset(latin)?.let { runCatching { Charset.forName(it) }.getOrNull() }
            ?: Charsets.UTF_8
        val html = String(bytes, charset)

        val meta = HtmlMetaParser.parse(html, finalUrl)
        return LinkPreview(
            finalUrl = finalUri,
            title = meta.title,
            description = meta.description,
            imageUrl = meta.image,
            faviconUrl = meta.favicon,
            siteName = meta.siteName,
        )
    }

    private fun faviconOf(uri: Uri): String? =
        uri.scheme?.let { s -> uri.host?.let { h -> "$s://$h/favicon.ico" } }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { runCatching { cancel() } }
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (cont.isActive) cont.resume(response) else response.close()
            }
            override fun onFailure(call: Call, e: IOException) {
                if (!cont.isCancelled) cont.resumeWithException(e)
            }
        })
    }
}
