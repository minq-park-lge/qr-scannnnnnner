package com.sssnaplab.snapqr

import android.app.Application
import android.os.Process
import androidx.camera.lifecycle.ProcessCameraProvider
import com.sssnaplab.snapqr.preview.LinkPreviewer
import com.sssnaplab.snapqr.scan.QrEngine

class SnapQrApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // CameraX 초기화(카메라 열거/특성 조회)를 Activity 생성과 병렬로 시작.
        // LifecycleCameraController 가 같은 싱글톤을 사용하므로 바인딩 시점에 이미 준비됨.
        ProcessCameraProvider.getInstance(this)

        // ML Kit 네이티브 로드 + 예열, OkHttp 클라이언트 생성을 백그라운드에서 미리
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
            QrEngine.warmUp()
            LinkPreviewer.client
        }, "snapqr-warmup").start()
    }
}
