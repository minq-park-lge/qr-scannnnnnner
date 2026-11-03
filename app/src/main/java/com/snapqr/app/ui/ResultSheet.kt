package com.snapqr.app.ui

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
import com.snapqr.app.content.ActionLauncher
import com.snapqr.app.content.ScanContent
import com.snapqr.app.content.TargetApp
import com.snapqr.app.content.TargetResolver
import com.snapqr.app.preview.LinkPreview
import com.snapqr.app.preview.PreviewState
import com.snapqr.app.scan.ScanYellow
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
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dragY = remember { Animatable(0f) }

    val target by produceState<TargetApp?>(null, content) {
        value = withContext(Dispatchers.Default) { TargetResolver.resolve(context, content) }
    }
    val open = { ActionLauncher.open(context, content) }

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
                    colors = ButtonDefaults.buttonColors(containerColor = ScanYellow, contentColor = Color.Black),
                ) {
                    Icon(primaryIcon(content, target), null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        primaryLabel(content, target),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(onClick = { ActionLauncher.copy(context, copyText(content)) }, Modifier.size(52.dp)) {
                    Icon(Icons.Outlined.ContentCopy, "복사")
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(onClick = { ActionLauncher.share(context, content.raw) }, Modifier.size(52.dp)) {
                    Icon(Icons.Outlined.Share, "공유")
                }
            }
        }
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
        IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "닫기") }
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
                    "$originalHost → ${p.finalHost} 로 이동",
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
            Text("미리보기를 불러오지 못했어요", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            if (secure) "HTTPS" else "보안 연결 아님",
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
            "네트워크" to content.ssid,
            content.password?.let { "비밀번호" to it },
            "보안" to when (content.encryption) {
                com.google.mlkit.vision.barcode.common.Barcode.WiFi.TYPE_OPEN -> "없음"
                com.google.mlkit.vision.barcode.common.Barcode.WiFi.TYPE_WEP -> "WEP"
                else -> "WPA/WPA2/WPA3"
            },
        )
        is ScanContent.Phone -> listOf("전화번호" to content.number)
        is ScanContent.Sms -> listOfNotNull("받는 사람" to content.number, content.body?.takeIf { it.isNotBlank() }?.let { "내용" to it })
        is ScanContent.Email -> listOfNotNull(
            "받는 사람" to content.address,
            content.subject?.takeIf { it.isNotBlank() }?.let { "제목" to it },
            content.body?.takeIf { it.isNotBlank() }?.let { "내용" to it },
        )
        is ScanContent.Geo -> listOf("좌표" to "%.6f, %.6f".format(content.lat, content.lng))
        is ScanContent.Contact -> listOfNotNull(
            content.name?.let { "이름" to it },
            content.org?.let { "회사" to it },
            content.phones.takeIf { it.isNotEmpty() }?.let { "전화" to it.joinToString("\n") },
            content.emails.takeIf { it.isNotEmpty() }?.let { "이메일" to it.joinToString("\n") },
            content.urls.takeIf { it.isNotEmpty() }?.let { "웹" to it.joinToString("\n") },
            content.address?.let { "주소" to it },
        )
        is ScanContent.Event -> {
            val ctx = LocalContext.current
            listOfNotNull(
                content.summary?.let { "제목" to it },
                content.startMillis?.let { s ->
                    "일시" to DateUtils.formatDateRange(
                        ctx, s, content.endMillis ?: s,
                        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_WEEKDAY,
                    )
                },
                content.location?.let { "장소" to it },
                content.description?.let { "설명" to it },
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
            if (app.installed) "${app.label} 앱으로 열려요" else "앱이 설치되어 있지 않아요 · 스토어로 이동",
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

private fun typeLabel(c: ScanContent) = when (c) {
    is ScanContent.Web -> if (c.playStorePackage != null) "Google Play 앱" else "웹사이트"
    is ScanContent.AppLink -> if (c.playStorePackage != null) "Google Play 앱" else "앱 링크"
    is ScanContent.Wifi -> "Wi-Fi"
    is ScanContent.Phone -> "전화번호"
    is ScanContent.Sms -> "문자"
    is ScanContent.Email -> "이메일"
    is ScanContent.Geo -> "위치"
    is ScanContent.Contact -> "연락처"
    is ScanContent.Event -> "일정"
    is ScanContent.Text -> "텍스트"
}

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

private fun primaryLabel(c: ScanContent, target: TargetApp?): String = when (c) {
    is ScanContent.Web -> when {
        c.playStorePackage != null -> "Play 스토어에서 보기"
        target != null -> "${target.label}에서 열기"
        else -> "열기"
    }
    is ScanContent.AppLink -> when {
        c.playStorePackage != null -> "Play 스토어에서 보기"
        target?.installed == true -> "${target.label} 열기"
        target?.installed == false -> "스토어에서 설치"
        else -> "앱에서 열기"
    }
    is ScanContent.Wifi -> "Wi-Fi 연결"
    is ScanContent.Phone -> "전화 걸기"
    is ScanContent.Sms -> "문자 보내기"
    is ScanContent.Email -> "메일 쓰기"
    is ScanContent.Geo -> "지도에서 보기"
    is ScanContent.Contact -> "연락처에 추가"
    is ScanContent.Event -> "캘린더에 추가"
    is ScanContent.Text -> "웹에서 검색"
}
