package com.sssnaplab.snapqr.content

import android.net.Uri
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.Calendar
import java.util.TimeZone

object ContentParser {

    private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.\\-]*:")
    private val BARE_DOMAIN = Regex(
        "^(?:[a-zA-Z0-9](?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}(?::\\d+)?(?:[/?#]\\S*)?$"
    )

    /** ML Kit 이 분류해 준 구조화 데이터를 우선 사용하고, 없으면 원문으로 판단 */
    fun parse(barcode: Barcode): ScanContent {
        val raw = barcode.rawValue.orEmpty()
        when (barcode.valueType) {
            Barcode.TYPE_WIFI -> barcode.wifi?.let { w ->
                val ssid = w.ssid
                if (!ssid.isNullOrEmpty()) {
                    return ScanContent.Wifi(
                        raw, ssid, w.password?.takeIf { it.isNotEmpty() }, w.encryptionType,
                        hidden = Regex("(?i)(^|[;:])H:true(;|$)").containsMatchIn(raw),
                    )
                }
            }
            Barcode.TYPE_PHONE -> barcode.phone?.number?.takeIf { it.isNotBlank() }?.let {
                return ScanContent.Phone(raw, it)
            }
            Barcode.TYPE_SMS -> barcode.sms?.let { s ->
                s.phoneNumber?.takeIf { it.isNotBlank() }?.let { return ScanContent.Sms(raw, it, s.message) }
            }
            Barcode.TYPE_EMAIL -> barcode.email?.let { e ->
                e.address?.takeIf { it.isNotBlank() }?.let { return ScanContent.Email(raw, it, e.subject, e.body) }
            }
            Barcode.TYPE_GEO -> barcode.geoPoint?.let { return ScanContent.Geo(raw, it.lat, it.lng) }
            Barcode.TYPE_CONTACT_INFO -> barcode.contactInfo?.let { c ->
                return ScanContent.Contact(
                    raw = raw,
                    name = c.name?.formattedName?.takeIf { it.isNotBlank() }
                        ?: listOfNotNull(c.name?.first, c.name?.last).joinToString(" ").takeIf { it.isNotBlank() },
                    org = c.organization?.takeIf { it.isNotBlank() },
                    phones = c.phones.mapNotNull { it.number?.takeIf(String::isNotBlank) },
                    emails = c.emails.mapNotNull { it.address?.takeIf(String::isNotBlank) },
                    urls = c.urls.filter { it.isNotBlank() },
                    address = c.addresses.firstOrNull()?.addressLines?.joinToString(" ")?.takeIf { it.isNotBlank() },
                )
            }
            Barcode.TYPE_CALENDAR_EVENT -> barcode.calendarEvent?.let { ev ->
                return ScanContent.Event(
                    raw = raw,
                    summary = ev.summary,
                    location = ev.location,
                    description = ev.description,
                    startMillis = ev.start?.toMillis(),
                    endMillis = ev.end?.toMillis(),
                )
            }
        }
        return parseRaw(raw)
    }

    /** 원문 문자열만으로 해석 (갤러리/테스트 공용) */
    fun parseRaw(input: String): ScanContent {
        val raw = input.trim()
        if (raw.isEmpty()) return ScanContent.Text(input)

        if (SCHEME.containsMatchIn(raw) && !raw.contains('\n')) {
            val uri = Uri.parse(raw)
            when (uri.scheme?.lowercase()) {
                "http", "https" -> if (!uri.host.isNullOrEmpty()) return ScanContent.Web(raw, uri)
                "tel" -> return ScanContent.Phone(raw, uri.schemeSpecificPart)
                "mailto" -> return ScanContent.Email(raw, uri.schemeSpecificPart.substringBefore('?'), null, null)
                "smsto", "sms" -> {
                    val parts = uri.schemeSpecificPart.split(':', limit = 2)
                    return ScanContent.Sms(raw, parts[0], parts.getOrNull(1))
                }
                "geo" -> {
                    val ll = uri.schemeSpecificPart.substringBefore('?').split(',')
                    val lat = ll.getOrNull(0)?.toDoubleOrNull()
                    val lng = ll.getOrNull(1)?.toDoubleOrNull()
                    if (lat != null && lng != null) return ScanContent.Geo(raw, lat, lng)
                }
                // WIFI:, BEGIN:VCARD 등은 ML Kit 이 위에서 처리. 여기 오는 건 앱 스킴
                "wifi", "begin", "mecard", "matmsg", "bizcard" -> {}
                else -> return ScanContent.AppLink(raw, uri)
            }
        }

        // 스킴 없는 도메인 (예: "lge.com/thinq") → https 로 보정
        if (BARE_DOMAIN.matches(raw) && !raw.contains(' ')) {
            return ScanContent.Web(raw, Uri.parse("https://$raw"))
        }
        return ScanContent.Text(raw)
    }

    private fun Barcode.CalendarDateTime.toMillis(): Long? {
        if (year <= 0) return null
        val tz = if (isUtc) TimeZone.getTimeZone("UTC") else TimeZone.getDefault()
        return Calendar.getInstance(tz).apply {
            clear()
            set(year, month - 1, day, hours.coerceAtLeast(0), minutes.coerceAtLeast(0), seconds.coerceAtLeast(0))
        }.timeInMillis
    }
}
