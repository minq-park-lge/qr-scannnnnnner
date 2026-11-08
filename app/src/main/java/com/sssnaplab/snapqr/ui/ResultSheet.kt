package com.sssnaplab.snapqr.ui

import com.sssnaplab.snapqr.security.RiskReport
import com.sssnaplab.snapqr.security.RiskReason
import com.sssnaplab.snapqr.security.RiskLevel
import com.sssnaplab.snapqr.security.RiskFinding
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import com.sssnaplab.snapqr.R
import androidx.compose.ui.res.stringResource
import android.text.format.DateUtils
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shop
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.sssnaplab.snapqr.content.ActionLauncher
import com.sssnaplab.snapqr.content.ScanContent
import com.sssnaplab.snapqr.content.TargetApp
import com.sssnaplab.snapqr.content.TargetResolver
import com.sssnaplab.snapqr.preview.LinkPreview
import com.sssnaplab.snapqr.preview.PreviewState
import com.sssnaplab.snapqr.scan.ScanYellow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * 스캔 결과 카드. 카메라는 계속 살아있고, 아래에서 올라오는 카드에
 * URL 프리뷰를 먼저 보여준 뒤 버튼(또는 카드) 탭 시 의도에 맞는 앱으로 연결.
 * 아래로 스와이프하면 닫힘.
 */
@Composable
fun ResultSheet(
    content: ScanContent,
    preview: PreviewState?,
    risk: RiskReport?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dragY = remember { Animatable(0f) }

    val target by produceState<TargetApp?>(null, content) {
        value = withContext(Dispatchers.Default) { TargetResolver.resolve(context, content) }
    }
    var confirmOpen by remember { mutableStateOf(false) }
    val danger = risk?.level == RiskLevel.DANGER
    // 위험 링크는 한 번 더 확인 후 열기
    val open: () -> Unit = {
        if (danger) {
            confirmOpen = true
        } else {
            ActionLauncher.open(context, content)
        }
    }

    if (confirmOpen) {
        AlertDialog(
            onDismissRequest = { confirmOpen = false },
            icon = { Icon(Icons.Outlined.GppMaybe, null, tint = DangerRed) },
            title = { Text(stringResource(R.string.confirm_title)) },
            text = { Text(stringResource(R.string.confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmOpen = false
                    ActionLauncher.open(context, content)
                }) { Text(stringResource(R.string.action_open_anyway), color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmOpen = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
            .offset { IntOffset(0, dragY.value.roundToInt()) }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { d -> scope.launch { dragY.snapTo((dragY.value + d).coerceAtLeast(0f)) } },
                onDragStopped = { v ->
                    if (dragY.value > 160f || v > 1800f) onDismiss()
                    else dragY.animateTo(0f)
                },
            ),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        tonalElevation = 6.dp,
        shadowElevation = 16.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            // 드래그 핸들
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(36.dp, 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
            Spacer(Modifier.height(10.dp))

            Header(content, onDismiss)
            Spacer(Modifier.height(12.dp))

            risk?.let {
                RiskBanner(it)
                Spacer(Modifier.height(10.dp))
            }

            when (content) {
                is ScanContent.Web -> WebPreviewCard(content.uri.toString(), preview, onClick = open)
                is ScanContent.AppLink -> {
                    if (preview != null) WebPreviewCard(content.raw, preview, onClick = open)
                    else RawBox(content.raw)
                }
                else -> Details(content)
            }

            target?.let {
                Spacer(Modifier.height(10.dp))
                TargetRow(it)
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        if (content is ScanContent.Text) ActionLauncher.webSearch(context, content.raw) else open()
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = if (danger) {
                        ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White)
                    } else {
                        ButtonDefaults.buttonColors(containerColor = ScanYellow, contentColor = Color.Black)
                    },
                ) {
                    Icon(if (danger) Icons.Outlined.GppMaybe else primaryIcon(content, target), null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (danger) stringResource(R.string.action_open_anyway) else primaryLabel(content, target),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(onClick = { ActionLauncher.copy(context, copyText(content)) }, Modifier.size(52.dp)) {
                    Icon(Icons.Outlined.ContentCopy, stringResource(R.string.cd_copy))
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(onClick = { ActionLauncher.share(context, content.raw) }, Modifier.size(52.dp)) {
                    Icon(Icons.Outlined.Share, stringResource(R.string.cd_share))
                }
            }
        }
    }
}

private val DangerRed = Color(0xFFFF453A)
private val CautionAmber = Color(0xFFFF9F0A)
private val SafeGreen = Color(0xFF30D158)

@Composable
private fun RiskBanner(report: RiskReport) {
    when (report.level) {
        RiskLevel.SAFE -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            Icon(Icons.Outlined.VerifiedUser, null, tint = SafeGreen, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.risk_safe), fontSize = 13.sp, color = SafeGreen)
        }
        else -> {
            val color = if (report.level == RiskLevel.DANGER) DangerRed else CautionAmber
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.16f))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (report.level == RiskLevel.DANGER) Icons.Outlined.GppMaybe else Icons.Outlined.Warning,
                        null, tint = color, modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(if (report.level == RiskLevel.DANGER) R.string.risk_title_danger else R.string.risk_title_caution),
                        color = color,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                report.sorted.forEach { f ->
                    Spacer(Modifier.height(4.dp))
                    Text("• " + riskText(f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun riskText(f: RiskFinding): String {
    val a = f.arg.orEmpty()
    return when (f.reason) {
        RiskReason.DANGEROUS_SCHEME -> stringResource(R.string.risk_dangerous_scheme, a)
        RiskReason.USERINFO_TRICK -> stringResource(R.string.risk_userinfo, a)
        RiskReason.BRAND_IMPERSONATION -> stringResource(R.string.risk_brand, a)
        RiskReason.BRAND_MENTION -> stringResource(R.string.risk_brand_mention, a)
        RiskReason.LOOKALIKE_DOMAIN -> stringResource(R.string.risk_lookalike, a)
        RiskReason.HOMOGRAPH -> stringResource(R.string.risk_homograph, a)
        RiskReason.APK_DOWNLOAD -> stringResource(R.string.risk_apk)
        RiskReason.IP_ADDRESS -> stringResource(R.string.risk_ip)
        RiskReason.SHORTENER -> stringResource(R.string.risk_shortener)
        RiskReason.SUSPICIOUS_TLD -> stringResource(R.string.risk_tld, a)
        RiskReason.INSECURE_HTTP -> stringResource(R.string.risk_http)
        RiskReason.UNUSUAL_PORT -> stringResource(R.string.risk_port, a)
    }
}

@Composable
private fun Header(content: ScanContent, onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(ScanYellow),
            contentAlignment = Alignment.Center,
        ) {
            Icon(typeIcon(content), null, tint = Color.Black, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(typeLabel(content), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, stringResource(R.string.cd_close)) }
    }
}

@Composable
private fun WebPreviewCard(original: String, state: PreviewState?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        when (state) {
            is PreviewState.Loaded -> LoadedPreview(original, state.preview)
            is PreviewState.Failed, null -> FallbackPreview(original, failed = state is PreviewState.Failed)
            PreviewState.Loading -> LoadingPreview(original)
        }
    }
}

@Composable
private fun LoadedPreview(original: String, p: LinkPreview) {
    p.imageUrl?.let { img ->
        AsyncImage(
            model = img,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1.91f).background(Color.Black),
        )
    }
    Column(Modifier.padding(14.dp)) {
        HostRow(p.faviconUrl, p.siteName ?: p.finalHost, secure = p.finalUrl.scheme == "https")
        p.title?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        p.description?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val originalHost = android.net.Uri.parse(original).host?.removePrefix("www.")
        if (originalHost != null && originalHost != p.finalHost && p.finalHost.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Warning, null, tint = Color(0xFFFF9F0A), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.preview_redirect, originalHost, p.finalHost),
                    fontSize = 12.sp,
                    color = Color(0xFFFF9F0A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        UrlText(p.finalUrl.toString())
    }
}

@Composable
private fun LoadingPreview(original: String) {
    val t = rememberInfiniteTransition(label = "shimmer")
    val a by t.animateFloat(0.25f, 0.55f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "a")
    Column(Modifier.padding(14.dp)) {
        HostRow(null, android.net.Uri.parse(original).host.orEmpty(), secure = original.startsWith("https", true))
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(0.85f).height(16.dp).alpha(a).clip(RoundedCornerShape(6.dp)).background(Color.White))
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(0.6f).height(12.dp).alpha(a).clip(RoundedCornerShape(6.dp)).background(Color.White))
        Spacer(Modifier.height(10.dp))
        UrlText(original)
    }
}

@Composable
private fun FallbackPreview(original: String, failed: Boolean) {
    Column(Modifier.padding(14.dp)) {
        val uri = android.net.Uri.parse(original)
        HostRow(null, uri.host ?: uri.scheme.orEmpty(), secure = original.startsWith("https", true))
        if (failed) {
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.preview_failed), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        UrlText(original)
    }
}

@Composable
private fun HostRow(favicon: String?, host: String, secure: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (favicon != null) {
            AsyncImage(
                model = favicon,
                contentDescription = null,
                modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)),
            )
        } else {
            Icon(Icons.Outlined.Language, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            host,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            if (secure) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
            stringResource(if (secure) R.string.cd_secure else R.string.cd_not_secure),
            Modifier.size(14.dp),
            tint = if (secure) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFFF453A),
        )
    }
}

@Composable
private fun UrlText(url: String) {
    Text(
        url,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun RawBox(text: String) {
    SelectionContainer {
        Text(
            text,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
        )
    }
}

@Composable
private fun Details(content: ScanContent) {
    val rows: List<Pair<String, String>> = when (content) {
        is ScanContent.Wifi -> listOfNotNull(
            stringResource(R.string.label_network) to content.ssid,
            content.password?.let { pw -> stringResource(R.string.label_password) to pw },
            stringResource(R.string.label_security) to when (content.encryption) {
                com.google.mlkit.vision.barcode.common.Barcode.WiFi.TYPE_OPEN -> stringResource(R.string.security_none)
                com.google.mlkit.vision.barcode.common.Barcode.WiFi.TYPE_WEP -> "WEP"
                else -> "WPA/WPA2/WPA3"
            },
        )
        is ScanContent.Phone -> listOf(stringResource(R.string.label_phone_number) to content.number)
        is ScanContent.Sms -> listOfNotNull(stringResource(R.string.label_recipient) to content.number, content.body?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.label_message) to it })
        is ScanContent.Email -> listOfNotNull(
            stringResource(R.string.label_recipient) to content.address,
            content.subject?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.label_subject) to it },
            content.body?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.label_message) to it },
        )
        is ScanContent.Geo -> listOf(stringResource(R.string.label_coordinates) to "%.6f, %.6f".format(java.util.Locale.US, content.lat, content.lng))
        is ScanContent.Contact -> listOfNotNull(
            content.name?.let { stringResource(R.string.label_name) to it },
            content.org?.let { stringResource(R.string.label_company) to it },
            content.phones.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.label_phone) to it.joinToString("\n") },
            content.emails.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.label_email) to it.joinToString("\n") },
            content.urls.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.label_web) to it.joinToString("\n") },
            content.address?.let { stringResource(R.string.label_address) to it },
        )
        is ScanContent.Event -> {
            val ctx = LocalContext.current
            listOfNotNull(
                content.summary?.let { stringResource(R.string.label_title) to it },
                content.startMillis?.let { s ->
                    stringResource(R.string.label_when) to DateUtils.formatDateRange(
                        ctx, s, content.endMillis ?: s,
                        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_WEEKDAY,
                    )
                },
                content.location?.let { stringResource(R.string.label_location) to it },
                content.description?.let { stringResource(R.string.label_description) to it },
            )
        }
        is ScanContent.Text, is ScanContent.Web, is ScanContent.AppLink -> emptyList()
    }

    if (rows.isEmpty()) {
        RawBox(content.raw)
        return
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { (k, v) ->
                    Row {
                        Text(k, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.width(72.dp))
                        Text(v, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetRow(app: TargetApp) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        val bmp = remember(app.icon) { app.icon?.toBitmap(64, 64)?.asImageBitmap() }
        if (bmp != null) {
            Image(bmp, null, Modifier.size(20.dp).clip(RoundedCornerShape(5.dp)))
        } else {
            Icon(Icons.Outlined.Widgets, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            if (app.installed) stringResource(R.string.target_opens_in, app.label) else stringResource(R.string.target_not_installed),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun copyText(c: ScanContent): String = when (c) {
    is ScanContent.Wifi -> c.password ?: c.ssid
    is ScanContent.Phone -> c.number
    is ScanContent.Email -> c.address
    else -> c.raw
}

@Composable
private fun typeLabel(c: ScanContent): String = stringResource(
    when (c) {
        is ScanContent.Web -> if (c.playStorePackage != null) R.string.type_play_app else R.string.type_website
        is ScanContent.AppLink -> if (c.playStorePackage != null) R.string.type_play_app else R.string.type_app_link
        is ScanContent.Wifi -> R.string.type_wifi
        is ScanContent.Phone -> R.string.type_phone
        is ScanContent.Sms -> R.string.type_sms
        is ScanContent.Email -> R.string.type_email
        is ScanContent.Geo -> R.string.type_location
        is ScanContent.Contact -> R.string.type_contact
        is ScanContent.Event -> R.string.type_event
        is ScanContent.Text -> R.string.type_text
    }
)

private fun typeIcon(c: ScanContent): ImageVector = when (c) {
    is ScanContent.Web -> if (c.playStorePackage != null) Icons.Outlined.Shop else Icons.Outlined.Language
    is ScanContent.AppLink -> if (c.playStorePackage != null) Icons.Outlined.Shop else Icons.Outlined.Widgets
    is ScanContent.Wifi -> Icons.Outlined.Wifi
    is ScanContent.Phone -> Icons.Outlined.Call
    is ScanContent.Sms -> Icons.Outlined.Sms
    is ScanContent.Email -> Icons.Outlined.Email
    is ScanContent.Geo -> Icons.Outlined.LocationOn
    is ScanContent.Contact -> Icons.Outlined.PersonAdd
    is ScanContent.Event -> Icons.Outlined.CalendarMonth
    is ScanContent.Text -> Icons.Outlined.TextSnippet
}

private fun primaryIcon(c: ScanContent, target: TargetApp?): ImageVector = when (c) {
    is ScanContent.Text -> Icons.Outlined.Search
    is ScanContent.Web, is ScanContent.AppLink -> Icons.AutoMirrored.Outlined.OpenInNew
    else -> typeIcon(c)
}

@Composable
private fun primaryLabel(c: ScanContent, target: TargetApp?): String = when (c) {
    is ScanContent.Web -> when {
        c.playStorePackage != null -> stringResource(R.string.action_play_store)
        target != null -> stringResource(R.string.action_open_in, target.label)
        else -> stringResource(R.string.action_open)
    }
    is ScanContent.AppLink -> when {
        c.playStorePackage != null -> stringResource(R.string.action_play_store)
        target?.installed == true -> stringResource(R.string.action_open_app, target.label)
        target?.installed == false -> stringResource(R.string.action_install)
        else -> stringResource(R.string.action_open_in_app)
    }
    is ScanContent.Wifi -> stringResource(R.string.action_wifi)
    is ScanContent.Phone -> stringResource(R.string.action_call)
    is ScanContent.Sms -> stringResource(R.string.action_sms)
    is ScanContent.Email -> stringResource(R.string.action_email)
    is ScanContent.Geo -> stringResource(R.string.action_map)
    is ScanContent.Contact -> stringResource(R.string.action_add_contact)
    is ScanContent.Event -> stringResource(R.string.action_add_event)
    is ScanContent.Text -> stringResource(R.string.action_web_search)
}
