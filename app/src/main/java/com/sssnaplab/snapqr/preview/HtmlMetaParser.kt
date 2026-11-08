package com.sssnaplab.snapqr.preview

import java.net.URI

/**
 * 가벼운 정규식 기반 <head> 메타 파서.
 * Jsoup 같은 무거운 의존성 없이 OpenGraph / Twitter Card / <title> / 파비콘만 추출.
 */
internal object HtmlMetaParser {

    private val META = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val LINK = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val TITLE = Regex("<title[^>]*>([\\s\\S]*?)</title>", RegexOption.IGNORE_CASE)
    private val ATTR = Regex("([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s\"'>]+))")
    private val CHARSET = Regex("<meta[^>]+charset\\s*=\\s*[\"']?([-a-zA-Z0-9_]+)", RegexOption.IGNORE_CASE)

    data class Result(
        val title: String?,
        val description: String?,
        val image: String?,
        val favicon: String?,
        val siteName: String?,
    )

    fun sniffCharset(head: String): String? = CHARSET.find(head)?.groupValues?.get(1)

    fun parse(html: String, baseUrl: String): Result {
        val meta = HashMap<String, String>()
        META.findAll(html).forEach { m ->
            val attrs = attrs(m.value)
            val key = (attrs["property"] ?: attrs["name"] ?: attrs["itemprop"])?.lowercase() ?: return@forEach
            val content = attrs["content"]?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEach
            meta.putIfAbsent(key, decode(content))
        }

        var favicon: String? = null
        var faviconScore = -1
        LINK.findAll(html).forEach { m ->
            val attrs = attrs(m.value)
            val rel = attrs["rel"]?.lowercase() ?: return@forEach
            val href = attrs["href"]?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEach
            // 고해상도 아이콘 우선
            val score = when {
                "apple-touch-icon" in rel -> 3
                "icon" in rel && attrs["sizes"]?.let { s -> s.contains("192") || s.contains("180") || s.contains("96") } == true -> 2
                "icon" in rel -> 1
                else -> -1
            }
            if (score > faviconScore) {
                faviconScore = score
                favicon = href
            }
        }

        val title = meta["og:title"] ?: meta["twitter:title"]
            ?: TITLE.find(html)?.groupValues?.get(1)?.let { decode(it.trim()) }?.takeIf { it.isNotEmpty() }
        val description = meta["og:description"] ?: meta["twitter:description"] ?: meta["description"]
        val image = meta["og:image:secure_url"] ?: meta["og:image"] ?: meta["og:image:url"]
            ?: meta["twitter:image"] ?: meta["twitter:image:src"]

        return Result(
            title = title?.collapse(),
            description = description?.collapse(),
            image = image?.let { resolve(baseUrl, it) },
            favicon = resolve(baseUrl, favicon ?: "/favicon.ico"),
            siteName = meta["og:site_name"]?.collapse(),
        )
    }

    private fun attrs(tag: String): Map<String, String> {
        val out = HashMap<String, String>()
        ATTR.findAll(tag).forEach { m ->
            val v = m.groups[3]?.value ?: m.groups[4]?.value ?: m.groups[5]?.value ?: ""
            out.putIfAbsent(m.groupValues[1].lowercase(), v)
        }
        return out
    }

    private fun resolve(base: String, href: String): String? = runCatching {
        URI(base).resolve(href.replace(" ", "%20")).toString()
    }.getOrNull()

    private fun String.collapse() = replace(Regex("\\s+"), " ").trim().takeIf { it.isNotEmpty() }

    private val ENTITY = Regex("&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);")
    private val NAMED = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "middot" to "·", "hellip" to "…", "ndash" to "–", "mdash" to "—", "copy" to "©", "reg" to "®",
    )

    fun decode(s: String): String = ENTITY.replace(s) { m ->
        val e = m.groupValues[1]
        when {
            e.startsWith("#x") || e.startsWith("#X") -> e.substring(2).toIntOrNull(16)?.let { String(Character.toChars(it)) }
            e.startsWith("#") -> e.substring(1).toIntOrNull()?.let { String(Character.toChars(it)) }
            else -> NAMED[e.lowercase()]
        } ?: m.value
    }
}
