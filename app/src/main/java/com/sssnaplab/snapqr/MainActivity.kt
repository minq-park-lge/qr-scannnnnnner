package com.sssnaplab.snapqr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sssnaplab.snapqr.content.CustomTabsWarmer
import com.sssnaplab.snapqr.ui.SnapQrApp
import com.sssnaplab.snapqr.ui.SnapQrTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            SnapQrTheme {
                SnapQrApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        CustomTabsWarmer.bind(this)
    }

    override fun onStop() {
        super.onStop()
        CustomTabsWarmer.unbind(this)
    }
}
