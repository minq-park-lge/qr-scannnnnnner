package com.snapqr.app.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snapqr.app.scan.QrOverlay
import com.snapqr.app.scan.ScanEvent
import com.snapqr.app.scan.ScanViewModel
import com.snapqr.app.scan.ScannerCamera

@Composable
fun ScannerScreen(vm: ScanViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val view = LocalView.current

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
        state.locked?.let {
            Text(
                it.rawValue,
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(24.dp)
                    .background(Color(0xFFFFD60A), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}
