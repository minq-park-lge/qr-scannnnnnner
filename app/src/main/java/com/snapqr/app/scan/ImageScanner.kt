package com.snapqr.app.scan

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** 갤러리 이미지(스크린샷 등)에서 QR 인식 */
object ImageScanner {

    suspend fun scan(context: Context, uri: Uri): Detection? {
        val image = runCatching { InputImage.fromFilePath(context, uri) }.getOrNull() ?: return null
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
        return try {
            suspendCancellableCoroutine { cont ->
                scanner.process(image)
                    .addOnSuccessListener { list ->
                        val best = list.mapNotNull(Detection::from).maxByOrNull { it.area }
                        cont.resume(best)
                    }
                    .addOnFailureListener { cont.resume(null) }
            }
        } finally {
            scanner.close()
        }
    }
}
