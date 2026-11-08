package com.sssnaplab.snapqr.security

enum class RiskLevel { SAFE, CAUTION, DANGER }

enum class RiskReason(val level: RiskLevel) {
    /** javascript:, data:, file: 등 스크립트 실행/로컬 파일 접근 스킴 */
    DANGEROUS_SCHEME(RiskLevel.DANGER),
    /** https://naver.com@evil.example 처럼 @ 앞부분으로 진짜 주소를 위장 */
    USERINFO_TRICK(RiskLevel.DANGER),
    /** 도메인에 브랜드명이 들어있지만 공식 도메인이 아님 (naver-login.xyz) */
    BRAND_IMPERSONATION(RiskLevel.DANGER),
    /** 도메인 일부에 브랜드명이 섞여 있음 (navers-event.com) – 자회사일 수도 있어 주의 단계 */
    BRAND_MENTION(RiskLevel.CAUTION),
    /** 브랜드와 철자가 한두 글자 다른 도메인 (paypa1.com, naevr.com) */
    LOOKALIKE_DOMAIN(RiskLevel.DANGER),
    /** 키릴/그리스 문자 등으로 라틴 문자를 흉내낸 국제화 도메인 */
    HOMOGRAPH(RiskLevel.DANGER),
    /** 안드로이드 앱 설치 파일 직접 다운로드 (스미싱 주요 수법) */
    APK_DOWNLOAD(RiskLevel.DANGER),
    IP_ADDRESS(RiskLevel.CAUTION),
    SHORTENER(RiskLevel.CAUTION),
    SUSPICIOUS_TLD(RiskLevel.CAUTION),
    INSECURE_HTTP(RiskLevel.CAUTION),
    UNUSUAL_PORT(RiskLevel.CAUTION),
}

data class RiskFinding(val reason: RiskReason, val arg: String? = null)

data class RiskReport(val findings: List<RiskFinding>) {
    val level: RiskLevel = findings.maxOfOrNull { it.reason.level } ?: RiskLevel.SAFE

    /** 위험도 높은 순으로 정렬 */
    val sorted: List<RiskFinding> get() = findings.sortedByDescending { it.reason.level }

    operator fun plus(other: RiskReport) = RiskReport((findings + other.findings).distinctBy { it.reason to it.arg })

    companion object {
        val SAFE = RiskReport(emptyList())
    }
}
