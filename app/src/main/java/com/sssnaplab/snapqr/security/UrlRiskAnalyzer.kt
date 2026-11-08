package com.sssnaplab.snapqr.security

import java.net.IDN
import java.util.Locale

/**
 * 큐싱(QR 피싱) 휴리스틱 검사기.
 *
 * 전부 로컬 문자열 연산이라 1ms 미만 → 인식/카드 표시 속도에 영향 없음.
 * 네트워크 조회 없이 흔한 수법을 잡는다:
 *  위장 주소(@), 브랜드 사칭/유사 철자, 동형 문자(IDN), APK 다운로드,
 *  IP 주소, 단축 URL, 스팸성 TLD, 평문 HTTP, 비표준 포트, 위험 스킴.
 */
object UrlRiskAnalyzer {

    fun analyze(
        scheme: String?,
        host: String?,
        userInfo: String? = null,
        port: Int = -1,
        path: String? = null,
    ): RiskReport {
        val s = scheme?.lowercase(Locale.ROOT)
        val out = ArrayList<RiskFinding>(4)

        if (s in DANGEROUS_SCHEMES) {
            out += RiskFinding(RiskReason.DANGEROUS_SCHEME, s)
            return RiskReport(out)
        }
        if (s != "http" && s != "https") return RiskReport(out)

        val h = host?.lowercase(Locale.ROOT)?.trimEnd('.')?.removePrefix("[")?.removeSuffix("]")
        if (h.isNullOrEmpty()) return RiskReport(out)

        if (!userInfo.isNullOrEmpty()) out += RiskFinding(RiskReason.USERINFO_TRICK, userInfo)
        if (s == "http") out += RiskFinding(RiskReason.INSECURE_HTTP)
        if (port != -1 && port != 80 && port != 443) out += RiskFinding(RiskReason.UNUSUAL_PORT, port.toString())
        if (path != null && path.lowercase(Locale.ROOT).endsWith(".apk")) out += RiskFinding(RiskReason.APK_DOWNLOAD)

        if (IPV4.matches(h) || h.contains(':')) {
            out += RiskFinding(RiskReason.IP_ADDRESS, h)
            return RiskReport(out)
        }

        val registrable = registrableDomain(h)
        if (registrable in SHORTENERS) out += RiskFinding(RiskReason.SHORTENER, registrable)

        val tld = h.substringAfterLast('.')
        if (tld in SUSPICIOUS_TLDS) out += RiskFinding(RiskReason.SUSPICIOUS_TLD, tld)

        homograph(h)?.let { out += RiskFinding(RiskReason.HOMOGRAPH, it) }
        brandCheck(h, registrable)?.let { out += it }

        return RiskReport(out)
    }

    // region Domain helpers

    /** 공개 접미사(co.kr 등)를 고려한 등록 도메인 (naver.com / example.co.kr) */
    fun registrableDomain(host: String): String {
        val labels = host.split('.')
        if (labels.size <= 2) return host
        val lastTwo = labels.takeLast(2).joinToString(".")
        return if (lastTwo in TWO_LEVEL_SUFFIXES) labels.takeLast(3).joinToString(".") else lastTwo
    }

    private fun isOfficial(host: String, officials: Set<String>) =
        officials.any { host == it || host.endsWith(".$it") }

    private fun brandCheck(host: String, registrable: String): RiskFinding? {
        // 공식 도메인 또는 브랜드와 철자가 비슷한 정상 서비스면 통과
        if (registrable in LEGIT_LOOKALIKES) return null
        for (b in BRANDS) if (isOfficial(host, b.domains)) return null

        val tokens = host.split('.', '-', '_').filter { it.isNotEmpty() }
        // 1) 브랜드명이 독립된 토큰으로 등장 → 사칭 (naver-login.xyz, kakao.com.verify.top)
        BRANDS.firstOrNull { b -> tokens.any { it == b.key } }?.let {
            return RiskFinding(RiskReason.BRAND_IMPERSONATION, it.display)
        }
        // 2) 유사 철자 (paypa1.com, g00gle.com, amazom.com, naevr.com)
        val raw = registrable.substringBefore('.')
        val name = normalizeLeet(raw)
        for (b in BRANDS) {
            if (b.key.length < 5) continue
            val leetSubstituted = name == b.key && raw != b.key
            val typo = when {
                b.key.length >= 6 -> withinOneEdit(name, b.key)
                else -> isTransposition(name, b.key) // 5글자는 오탐 방지 위해 자리바꿈만 (never ≠ naver)
            }
            if (leetSubstituted || typo) return RiskFinding(RiskReason.LOOKALIKE_DOMAIN, b.display)
        }
        // 3) 토큰 일부에 브랜드명 포함 (navers-event.com) → 주의
        BRANDS.firstOrNull { b -> b.key.length >= 5 && tokens.any { it.contains(b.key) } }?.let {
            return RiskFinding(RiskReason.BRAND_MENTION, it.display)
        }
        return null
    }

    private fun isTransposition(a: String, b: String): Boolean {
        if (a.length != b.length || a == b) return false
        val diff = a.indices.filter { a[it] != b[it] }
        return diff.size == 2 && diff[1] == diff[0] + 1 && a[diff[0]] == b[diff[1]] && a[diff[1]] == b[diff[0]]
    }

    private fun normalizeLeet(s: String): String = s
        .replace("rn", "m").replace("vv", "w")
        .map {
            when (it) {
                '0' -> 'o'; '1' -> 'l'; '3' -> 'e'; '4' -> 'a'; '5' -> 's'; '7' -> 't'; '8' -> 'b'; '9' -> 'g'
                else -> it
            }
        }.joinToString("")

    /** 편집거리 ≤ 1 (치환/삽입/삭제) 또는 인접 문자 바꿈 */
    internal fun withinOneEdit(a: String, b: String): Boolean {
        if (a == b) return false
        if (kotlin.math.abs(a.length - b.length) > 1) return false
        if (a.length == b.length) {
            val diff = a.indices.filter { a[it] != b[it] }
            return diff.size == 1 ||
                (diff.size == 2 && diff[1] == diff[0] + 1 && a[diff[0]] == b[diff[1]] && a[diff[1]] == b[diff[0]])
        }
        val (shorter, longer) = if (a.length < b.length) a to b else b to a
        var i = 0
        var j = 0
        var skipped = false
        while (i < shorter.length && j < longer.length) {
            if (shorter[i] != longer[j]) {
                if (skipped) return false
                skipped = true
                j++
            } else {
                i++; j++
            }
        }
        return true
    }

    /** 라틴 문자와 키릴/그리스 문자를 섞거나, .com 등에 키릴 문자만 쓴 도메인 */
    private fun homograph(host: String): String? {
        if (!host.split('.').any { it.startsWith("xn--") }) return null
        val unicode = runCatching { IDN.toUnicode(host, IDN.ALLOW_UNASSIGNED) }.getOrNull() ?: return null
        val labels = unicode.split('.')
        val tldIsLatin = labels.last().all { it in 'a'..'z' || it in '0'..'9' || it == '-' }
        for (label in labels.dropLast(1)) {
            val hasLatin = label.any { it in 'a'..'z' }
            val hasConfusable = label.any {
                val block = Character.UnicodeBlock.of(it)
                block == Character.UnicodeBlock.CYRILLIC || block == Character.UnicodeBlock.GREEK
            }
            if (hasConfusable && (hasLatin || tldIsLatin)) return unicode
        }
        return null
    }

    // endregion

    // region Data

    /** 브랜드와 철자가 비슷하지만 정상인 서비스 (오탐 방지) */
    private val LEGIT_LOOKALIKES = setOf(
        "paypay.ne.jp", "paypay-card.co.jp", "paypay-bank.co.jp",
    )

    private val IPV4 = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")

    private val DANGEROUS_SCHEMES = setOf("javascript", "vbscript", "data", "file", "content")

    private val TWO_LEVEL_SUFFIXES = setOf(
        "co.kr", "or.kr", "go.kr", "ne.kr", "re.kr", "ac.kr", "pe.kr", "ms.kr", "hs.kr",
        "co.jp", "ne.jp", "or.jp", "ac.jp", "go.jp", "gr.jp",
        "co.uk", "org.uk", "ac.uk", "gov.uk", "me.uk",
        "com.cn", "net.cn", "org.cn", "gov.cn", "com.tw", "com.hk",
        "com.au", "net.au", "org.au", "co.nz", "co.in", "com.sg", "com.my",
        "com.br", "com.mx", "com.ar", "com.co", "com.es", "com.tr", "co.za",
    )

    private val SHORTENERS = setOf(
        "bit.ly", "t.co", "tinyurl.com", "goo.gl", "ow.ly", "is.gd", "buff.ly", "rebrand.ly",
        "cutt.ly", "shorturl.at", "t.ly", "rb.gy", "s.id", "tiny.cc", "v.gd", "shorturl.asia",
        "han.gl", "me2.do", "vo.la", "url.kr", "bitly.kr", "buly.kr", "zrr.kr", "c11.kr", "muz.so",
        "dwz.cn", "t.cn", "url.cn",
    )

    private val SUSPICIOUS_TLDS = setOf(
        "zip", "mov", "top", "xyz", "click", "icu", "cyou", "cfd", "sbs", "rest", "buzz", "monster",
        "quest", "lol", "tk", "ml", "ga", "cf", "gq", "work", "support", "country", "kim", "loan",
        "win", "bid", "men", "date", "racing", "review", "stream", "download", "gdn",
    )

    private class Brand(val key: String, val display: String, vararg domains: String) {
        val domains: Set<String> = domains.toSet()
    }

    private val BRANDS = listOf(
        // 국내
        Brand("naver", "Naver", "naver.com", "naver.me", "navercorp.com", "naver.net", "pstatic.net"),
        Brand("kakao", "Kakao", "kakao.com", "kakaocorp.com", "kakaopay.com", "kakaobank.com", "kakao.co.kr", "kakaocdn.net"),
        Brand("daum", "Daum", "daum.net", "daumcdn.net"),
        Brand("toss", "Toss", "toss.im", "tossbank.com", "toss.me", "tosspayments.com"),
        Brand("coupang", "Coupang", "coupang.com", "coupangcdn.com"),
        Brand("kbstar", "KB국민은행", "kbstar.com", "kbcard.com", "kbfg.com"),
        Brand("shinhan", "Shinhan", "shinhan.com", "shinhancard.com"),
        Brand("wooribank", "Woori Bank", "wooribank.com", "wooricard.com"),
        Brand("hanabank", "Hana Bank", "hanabank.com", "kebhana.com", "hanacard.co.kr"),
        Brand("nonghyup", "NH", "nonghyup.com", "nhbank.com"),
        Brand("samsung", "Samsung", "samsung.com", "samsungcard.com", "samsung.net"),
        Brand("lgthinq", "LG ThinQ", "lgthinq.com", "lge.com", "lg.com"),
        Brand("epost", "우체국 epost", "epost.go.kr", "epost.kr"),
        Brand("cjlogistics", "CJ Logistics", "cjlogistics.com"),
        Brand("hanjin", "Hanjin", "hanjin.com", "hanjin.co.kr"),
        Brand("upbit", "Upbit", "upbit.com"),
        Brand("bithumb", "Bithumb", "bithumb.com"),
        // 글로벌
        Brand("google", "Google", "google.com", "google.co.kr", "google.co.jp", "goo.gl", "g.co", "youtube.com", "youtu.be", "gstatic.com", "googleapis.com", "android.com"),
        Brand("youtube", "YouTube", "youtube.com", "youtu.be"),
        Brand("apple", "Apple", "apple.com", "icloud.com", "apple.co"),
        Brand("icloud", "iCloud", "icloud.com", "apple.com"),
        Brand("microsoft", "Microsoft", "microsoft.com", "live.com", "office.com", "outlook.com", "microsoftonline.com", "aka.ms"),
        Brand("outlook", "Outlook", "outlook.com", "live.com", "microsoft.com", "office.com"),
        Brand("paypal", "PayPal", "paypal.com", "paypal.me", "paypalobjects.com"),
        Brand("amazon", "Amazon", "amazon.com", "amazon.co.jp", "amazon.co.uk", "amazon.de", "amazon.fr", "amazon.es", "amzn.to", "amazonaws.com"),
        Brand("netflix", "Netflix", "netflix.com"),
        Brand("facebook", "Facebook", "facebook.com", "fb.com", "fb.me", "meta.com"),
        Brand("instagram", "Instagram", "instagram.com"),
        Brand("whatsapp", "WhatsApp", "whatsapp.com", "wa.me"),
        Brand("telegram", "Telegram", "telegram.org", "t.me"),
        Brand("binance", "Binance", "binance.com"),
        Brand("coinbase", "Coinbase", "coinbase.com"),
        Brand("ebay", "eBay", "ebay.com"),
        Brand("aliexpress", "AliExpress", "aliexpress.com"),
        Brand("alipay", "Alipay", "alipay.com"),
        Brand("taobao", "Taobao", "taobao.com"),
        Brand("wechat", "WeChat", "wechat.com", "qq.com"),
        Brand("rakuten", "Rakuten", "rakuten.co.jp", "rakuten.com"),
        Brand("yahoo", "Yahoo", "yahoo.com", "yahoo.co.jp"),
        Brand("line", "LINE", "line.me", "linecorp.com", "line-apps.com"),
        Brand("dhl", "DHL", "dhl.com", "dhl.de"),
        Brand("fedex", "FedEx", "fedex.com"),
        Brand("usps", "USPS", "usps.com"),
        Brand("ups", "UPS", "ups.com"),
        Brand("laposte", "La Poste", "laposte.fr", "laposte.net"),
        Brand("correos", "Correos", "correos.es"),
        Brand("ameli", "Ameli", "ameli.fr"),
        Brand("impots", "impots.gouv", "impots.gouv.fr"),
    )

    // endregion
}
