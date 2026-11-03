package com.snapqr.app.scan

import android.app.Application
import android.os.SystemClock
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
        // 이후 단계에서 콘텐츠 파싱 / URL 프리뷰 로딩을 연결
    }

    fun dismissResult() {
        val locked = _state.value.locked ?: return
        suppressedValue = locked.rawValue
        suppressedLastSeen = SystemClock.elapsedRealtime()
        _state.update { it.copy(locked = null) }
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
