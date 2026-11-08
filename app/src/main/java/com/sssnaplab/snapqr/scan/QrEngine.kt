package com.sssnaplab.snapqr.scan

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * 프로세스 전체에서 공유하는 QR 디텍터.
 *
 * 카메라 화면이 열릴 때마다 디텍터를 새로 만들면 네이티브 라이브러리 로딩 +
 * 모델 초기화 비용이 첫 프레임 인식에 그대로 얹힌다. 앱 시작 시 백그라운드에서
 * 한 번 만들어 더미 프레임으로 예열해 두고, 이후 카메라/갤러리가 재사용한다.
 */
object QrEngine {

    val scanner: BarcodeScanner by lazy {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }

    /** 네이티브 라이브러리 로드 + 추론 파이프라인 초기화를 미리 끝내 둔다 */
    fun warmUp() {
        runCatching {
            val blank = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            scanner.process(InputImage.fromBitmap(blank, 0))
        }
    }
}
