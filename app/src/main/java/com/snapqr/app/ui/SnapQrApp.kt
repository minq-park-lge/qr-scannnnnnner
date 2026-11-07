package com.snapqr.app.ui

import com.snapqr.app.R
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

@Composable
fun SnapQrApp() {
    val context = LocalContext.current
    fun granted() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

    var hasPermission by remember { mutableStateOf(granted()) }
    var askedOnce by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
        askedOnce = true
    }

    // 설정 화면에서 권한 허용 후 돌아온 경우 반영
    LifecycleResumeEffect(Unit) {
        hasPermission = granted()
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    if (hasPermission) {
        ScannerScreen()
    } else {
        PermissionScreen(
            onRequest = {
                if (askedOnce) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } else {
                    launcher.launch(Manifest.permission.CAMERA)
                }
            },
        )
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Outlined.CameraAlt, null, tint = Color.White, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.perm_message),
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRequest) { Text(stringResource(R.string.perm_button)) }
        }
    }
}
