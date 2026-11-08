package com.sssnaplab.snapqr.scan

import android.app.Application
import android.os.SystemClock
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sssnaplab.snapqr.content.ContentParser
import com.sssnaplab.snapqr.content.CustomTabsWarmer
import com.sssnaplab.snapqr.content.ScanContent
import com.sssnaplab.snapqr.preview.LinkPreviewer
import com.sssnaplab.snapqr.preview.PreviewState
import com.sssnaplab.snapqr.security.RiskLevel
import com.sssnaplab.snapqr.security.RiskReason
import com.sssnaplab.snapqr.security.RiskReport
import com.sssnaplab.snapqr.security.UrlRiskAnalyzer
import com.sssnaplab.snapqr.security.analyze
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScanUiState(
    /** 화면에 그릴 마커 꼭짓점. null 이면 마커 숨김 */
    val trackedCorners: List<Offset>? = null,
    /** 확정(잠금)된 스캔 결과. null 이면 스캔 대기 중 */
    val locked: Detection? = null,
    /** 잠긴 결과의 해석된 내용 */
    val content: ScanContent? = null,
    /** 웹 링크일 때 URL 프리뷰 상태 */
    val preview: PreviewState? = null,
    /** 큐싱 검사 결과 (링크일 때만). 리다이렉트 최종 URL 확인 후 갱신 */
    val risk: RiskReport? = null,
    val torchOn: Boolean = false,
    val torchAvailable: Boolean = false,
)

sealed interface ScanEvent {
    data object Haptic : ScanEvent
}

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private val _events = Channel<ScanEvent>(Channel.CONFLATED)
    val events = _events.receiveAsFlow()

    private var hideJob: Job? = null
    private var previewJob: Job? = null

    /** 결과 시트를 닫은 직후 같은 코드가 화면에 남아있어 바로 다시 잡히는 것을 방지 */
    private var suppressedValue: String? = null
    private var suppressedLastSeen = 0L

    fun onFrame(detections: List<Detection>) {
        val now = SystemClock.elapsedRealtime()
        val current = _state.value

        suppressedValue?.let { s ->
            if (detections.any { it.rawValue == s }) suppressedLastSeen = now
            else if (now - suppressedLastSeen > SUPPRESS_GONE_MS) suppressedValue = null
        }

        // 잠금 상태면 잠긴 코드를 계속 추적, 아니면 가장 크게 보이는 코드
        val target = current.locked?.let { l -> detections.firstOrNull { it.rawValue == l.rawValue } }
            ?: if (current.locked == null) detections.maxByOrNull { it.area } else null

        if (target != null) {
            hideJob?.cancel()
            hideJob = null
            val shouldLock = current.locked == null && target.rawValue != suppressedValue
            _state.update {
                it.copy(
                    trackedCorners = target.corners,
                    locked = if (shouldLock) target else it.locked,
                )
            }
            if (shouldLock) {
                _events.trySend(ScanEvent.Haptic)
                onLocked(target)
            }
        } else if (current.trackedCorners != null && hideJob == null) {
            // 한두 프레임 놓쳐도 마커가 깜빡이지 않도록 약간 유예
            hideJob = viewModelScope.launch {
                delay(HIDE_DELAY_MS)
                _state.update { it.copy(trackedCorners = null) }
                hideJob = null
            }
        }
    }

    /** 갤러리 이미지 등 카메라 외 경로로 얻은 결과 */
    fun onExternalResult(detection: Detection) {
        _state.update { it.copy(locked = detection, trackedCorners = null) }
        _events.trySend(ScanEvent.Haptic)
        onLocked(detection)
    }

    private fun onLocked(detection: Detection) {
        val content = ContentParser.parse(detection.barcode)
        val previewUri = when (content) {
            is ScanContent.Web -> content.uri
            // 플레이스토어 앱 링크도 스토어 페이지로 앱 이름/아이콘 프리뷰
            is ScanContent.AppLink -> content.playStorePackage?.let {
                android.net.Uri.parse("https://play.google.com/store/apps/details?id=$it")
            }
            else -> null
        }
        // 로컬 휴리스틱이라 즉시 계산됨 (1ms 미만) → 카드와 동시에 표시
        val risk = when (content) {
            is ScanContent.Web -> UrlRiskAnalyzer.analyze(content.uri)
            is ScanContent.AppLink -> UrlRiskAnalyzer.analyze(content.uri.scheme, null)
            else -> null
        }
        _state.update {
            it.copy(content = content, preview = previewUri?.let { PreviewState.Loading }, risk = risk)
        }
        // 위험 신호가 없는 웹 링크만 브라우저에 미리 로딩 → 열기 누르면 즉시 표시
        if (content is ScanContent.Web && content.playStorePackage == null && risk?.level == RiskLevel.SAFE) {
            CustomTabsWarmer.prefetch(content.uri)
        }
        previewJob?.cancel()
        if (previewUri != null) {
            previewJob = viewModelScope.launch {
                val result = try {
                    PreviewState.Loaded(LinkPreviewer.load(previewUri))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    PreviewState.Failed(e.message ?: e.javaClass.simpleName)
                }
                _state.update { s ->
                    if (s.content !== content) return@update s
                    s.copy(preview = result, risk = refineRisk(s.risk, result))
                }
                // 단축 URL 등이 안전한 최종 목적지로 확인되면 그때 프리페치
                val now = _state.value
                if (now.content === content && risk?.level != RiskLevel.SAFE &&
                    now.risk?.level == RiskLevel.SAFE && result is PreviewState.Loaded
                ) {
                    CustomTabsWarmer.prefetch(result.preview.finalUrl)
                }
            }
        }
    }

    /** 리다이렉트를 따라간 최종 URL 로 재검사 (단축 URL 의 실제 목적지 판정) */
    private fun refineRisk(initial: RiskReport?, preview: PreviewState): RiskReport? {
        if (initial == null || preview !is PreviewState.Loaded) return initial
        val finalReport = UrlRiskAnalyzer.analyze(preview.preview.finalUrl)
        val kept = initial.findings.filter {
            it.reason != RiskReason.SHORTENER && it.reason != RiskReason.INSECURE_HTTP
        }
        return RiskReport(kept) + finalReport
    }

    fun dismissResult() {
        val locked = _state.value.locked ?: return
        suppressedValue = locked.rawValue
        suppressedLastSeen = SystemClock.elapsedRealtime()
        previewJob?.cancel()
        _state.update { it.copy(locked = null, content = null, preview = null, risk = null) }
    }

    fun toggleTorch() = _state.update { it.copy(torchOn = !it.torchOn) }

    fun setTorchAvailable(available: Boolean) = _state.update {
        it.copy(torchAvailable = available, torchOn = it.torchOn && available)
    }

    private companion object {
        const val HIDE_DELAY_MS = 220L
        const val SUPPRESS_GONE_MS = 700L
    }
}
