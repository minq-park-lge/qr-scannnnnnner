package com.sssnaplab.snapqr.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class UrlRiskAnalyzerTest {

    private fun check(url: String): RiskReport {
        val u = URI(url)
        return UrlRiskAnalyzer.analyze(u.scheme, u.host, u.userInfo, u.port, u.path)
    }

    private fun reasons(url: String) = check(url).findings.map { it.reason }.toSet()

    @Test fun officialDomainsAreSafe() {
        listOf(
            "https://www.naver.com/",
            "https://m.naver.com/news",
            "https://naver.me/abc",
            "https://www.google.co.kr/search?q=x",
            "https://accounts.google.com/",
            "https://www.lg.com/kr",
            "https://www.lge.com/",
            "https://pay.kakao.com/",
            "https://www.paypal.com/signin",
            "https://www.amazon.co.jp/",
            "https://example.com/",
            "https://never.com/",          // 실제 단어 – naver 오탐 금지
            "https://www.samsung.com/sec/",
            "https://lgthinq.com/",
        ).forEach { assertEquals(it, RiskLevel.SAFE, check(it).level) }
    }

    @Test fun brandImpersonation() {
        assertTrue(RiskReason.BRAND_IMPERSONATION in reasons("https://naver-login.xyz/"))
        assertTrue(RiskReason.BRAND_IMPERSONATION in reasons("https://kakao.com.verify-account.top/"))
        assertTrue(RiskReason.BRAND_IMPERSONATION in reasons("https://secure-paypal.co/"))
        assertEquals(RiskLevel.DANGER, check("https://toss-pay.kr/").level)
    }

    @Test fun lookalikeDomains() {
        listOf("https://paypa1.com/", "https://g00gle.com/", "https://amazom.com/", "https://naevr.com/", "https://rnicrosoft.com/")
            .forEach { assertTrue(it, RiskReason.LOOKALIKE_DOMAIN in reasons(it)) }
    }

    @Test fun brandMentionIsCautionOnly() {
        val r = check("https://naverblogfan.com/")
        assertEquals(RiskLevel.CAUTION, r.level)
        assertTrue(RiskReason.BRAND_MENTION in r.findings.map { it.reason })
    }

    @Test fun userInfoTrick() {
        assertTrue(RiskReason.USERINFO_TRICK in reasons("https://www.naver.com@evil.example/login"))
    }

    @Test fun homographIdn() {
        // "аpple.com" 의 첫 글자가 키릴 문자 а
        val host = java.net.IDN.toASCII("аpple.com")
        assertTrue(RiskReason.HOMOGRAPH in reasons("https://$host/"))
        // 한글 도메인은 정상
        val korean = java.net.IDN.toASCII("한국인터넷진흥원.한국")
        assertEquals(RiskLevel.SAFE, check("https://$korean/").level)
    }

    @Test fun apkIpShortenerTldHttpPort() {
        assertTrue(RiskReason.APK_DOWNLOAD in reasons("https://cdn.example.com/delivery/Track.APK"))
        assertTrue(RiskReason.IP_ADDRESS in reasons("http://203.0.113.7/pay"))
        assertTrue(RiskReason.SHORTENER in reasons("https://bit.ly/3abcd"))
        assertTrue(RiskReason.SUSPICIOUS_TLD in reasons("https://parcel-update.top/"))
        assertTrue(RiskReason.INSECURE_HTTP in reasons("http://example.com/"))
        assertTrue(RiskReason.UNUSUAL_PORT in reasons("https://example.com:8443/"))
    }

    @Test fun dangerousSchemes() {
        assertEquals(RiskLevel.DANGER, UrlRiskAnalyzer.analyze("javascript", null).level)
        assertEquals(RiskLevel.SAFE, UrlRiskAnalyzer.analyze("market", null).level)
    }

    @Test fun registrableDomain() {
        assertEquals("example.co.kr", UrlRiskAnalyzer.registrableDomain("a.b.example.co.kr"))
        assertEquals("naver.com", UrlRiskAnalyzer.registrableDomain("m.blog.naver.com"))
    }
}
