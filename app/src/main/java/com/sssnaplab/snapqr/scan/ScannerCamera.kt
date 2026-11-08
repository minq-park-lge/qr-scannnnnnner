package com.sssnaplab.snapqr.scan

import android.util.Size
import android.view.ViewGroup
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

/**
 * 카메라 프리뷰 + 실시간 QR 인식.
 *
 * 성능 포인트
 *  - CameraController 에서 IMAGE_ANALYSIS 만 활성화 (ImageCapture 미사용 → 센서 대역폭/해상도 여유)
 *  - 앱 시작 시 예열된 공유 디텍터(QrEngine) 재사용 → 첫 프레임부터 즉시 인식
 *  - QR 포맷만 탐색하도록 제한 → ML Kit 프레임당 처리 시간 대폭 감소
 *  - 1080p 분석 해상도: 멀리 있거나 작은 QR 도 줌 없이 인식 (줌 강제 X)
 *  - KEEP_ONLY_LATEST 백프레셔: 지연 누적 없이 항상 최신 프레임만 처리
 *  - COORDINATE_SYSTEM_VIEW_REFERENCED: 꼭짓점이 이미 화면 좌표로 변환되어 와서 오버레이가 정확히 겹침
 *  - PreviewView PERFORMANCE 모드(SurfaceView) 로 프리뷰 지연 최소화
 */
@Composable
fun ScannerCamera(
    modifier: Modifier = Modifier,
    torchOn: Boolean,
    onFrame: (List<Detection>) -> Unit,
    onTorchAvailable: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnFrame = rememberUpdatedState(onFrame)
    val currentOnTorch = rememberUpdatedState(onTorchAvailable)

    val scanner = QrEngine.scanner
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    val controller = remember {
        val selector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(1920, 1080),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                )
            )
            .build()
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            previewResolutionSelector = selector
            imageAnalysisResolutionSelector = selector
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            isTapToFocusEnabled = true
            isPinchToZoomEnabled = true
            setImageAnalysisAnalyzer(
                analysisExecutor,
                MlKitAnalyzer(
                    listOf(scanner),
                    ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                    ContextCompat.getMainExecutor(context),
                ) { result ->
                    val barcodes = result.getValue(scanner).orEmpty()
                    currentOnFrame.value(barcodes.mapNotNull(Detection::from))
                },
            )
        }
    }

    LaunchedEffect(torchOn) { controller.enableTorch(torchOn) }

    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        controller.initializationFuture.addListener({
            runCatching {
                // 카메라 초기화 후 플래시 유무 확인
                val info = controller.cameraInfo
                currentOnTorch.value(info?.hasFlashUnit() == true)
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            controller.unbind()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            // scanner 는 프로세스 공유 인스턴스(QrEngine)이므로 닫지 않음
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                this.controller = controller
            }
        },
    )
}
