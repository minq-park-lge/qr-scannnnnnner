package com.snapqr.app.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.snapqr.app.scan.ImageScanner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snapqr.app.content.ScanContent
import com.snapqr.app.scan.QrOverlay
import com.snapqr.app.scan.ScanEvent
import com.snapqr.app.scan.ScanViewModel
import com.snapqr.app.scan.ScanYellow
import com.snapqr.app.scan.ScannerCamera

@Composable
fun ScannerScreen(vm: ScanViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val detection = ImageScanner.scan(context, uri)
            if (detection != null) vm.onExternalResult(detection)
            else Toast.makeText(context, "이미지에서 QR 코드를 찾지 못했어요", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                ScanEvent.Haptic -> view.performHapticFeedback(
                    if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
                    else HapticFeedbackConstants.VIRTUAL_KEY
                )
            }
        }
    }

    BackHandler(enabled = state.content != null) { vm.dismissResult() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ScannerCamera(
            modifier = Modifier.fillMaxSize(),
            torchOn = state.torchOn,
            onFrame = vm::onFrame,
            onTorchAvailable = vm::setTorchAvailable,
        )
        QrOverlay(
            corners = state.trackedCorners,
            locked = state.locked != null,
            modifier = Modifier.fillMaxSize(),
        )

        // 상단 스크림 + 컨트롤
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                IconButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.4f),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, "갤러리에서 스캔")
                }
                TopControls(
                    torchAvailable = state.torchAvailable,
                    torchOn = state.torchOn,
                    onTorch = vm::toggleTorch,
                )
            }
        }

        // 스캔 대기 힌트
        AnimatedVisibility(
            visible = state.content == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp),
        ) {
            Text(
                if (state.trackedCorners == null) "QR 코드를 비춰주세요" else "인식 중…",
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }

        // 결과 카드: 닫힐 때도 마지막 내용을 유지하며 슬라이드 아웃
        val lastContent = remember { LastValue<ScanContent>() }
        state.content?.let { lastContent.value = it }
        AnimatedVisibility(
            visible = state.content != null,
            enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            lastContent.value?.let { content ->
                ResultSheet(
                    content = content,
                    preview = if (state.content === content) state.preview else null,
                    onDismiss = vm::dismissResult,
                )
            }
        }
    }
}

@Composable
private fun TopControls(torchAvailable: Boolean, torchOn: Boolean, onTorch: () -> Unit) {
    if (torchAvailable) {
        IconButton(
            onClick = onTorch,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (torchOn) ScanYellow else Color.Black.copy(alpha = 0.4f),
                contentColor = if (torchOn) Color.Black else Color.White,
            ),
        ) {
            Icon(if (torchOn) Icons.Outlined.FlashlightOn else Icons.Outlined.FlashlightOff, "플래시")
        }
    }
}

/** 리컴포지션 없이 마지막 non-null 값을 기억하는 홀더 (exit 애니메이션용) */
private class LastValue<T> {
    var value: T? = null
}
