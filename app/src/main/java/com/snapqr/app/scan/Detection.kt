package com.snapqr.app.scan

import androidx.compose.ui.geometry.Offset
import com.google.mlkit.vision.barcode.common.Barcode

/**
 * 한 프레임에서 인식된 QR 코드.
 * [corners] 는 PreviewView 좌표계(=화면 좌표)로 이미 변환된 4개 꼭짓점 (시계방향, 좌상단부터).
 */
data class Detection(
    val rawValue: String,
    val barcode: Barcode,
    val corners: List<Offset>,
) {
    /** 다각형 면적 (shoelace) – 여러 코드가 동시에 보일 때 가장 큰 것을 우선 */
    val area: Float
        get() {
            var sum = 0f
            for (i in corners.indices) {
                val a = corners[i]
                val b = corners[(i + 1) % corners.size]
                sum += a.x * b.y - b.x * a.y
            }
            return kotlin.math.abs(sum) / 2f
        }

    companion object {
        fun from(barcode: Barcode): Detection? {
            val value = barcode.rawValue ?: return null
            if (value.isEmpty()) return null
            val pts = barcode.cornerPoints
            val corners = if (pts != null && pts.size == 4) {
                pts.map { Offset(it.x.toFloat(), it.y.toFloat()) }
            } else {
                val box = barcode.boundingBox ?: return null
                listOf(
                    Offset(box.left.toFloat(), box.top.toFloat()),
                    Offset(box.right.toFloat(), box.top.toFloat()),
                    Offset(box.right.toFloat(), box.bottom.toFloat()),
                    Offset(box.left.toFloat(), box.bottom.toFloat()),
                )
            }
            return Detection(value, barcode, corners)
        }
    }
}
