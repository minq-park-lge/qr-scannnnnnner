package com.snapqr.app.scan

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

val ScanYellow = Color(0xFFFFD60A)

/**
 * 아이폰 카메라처럼 QR 코드 위에 노란 브래킷 마커를 그린다.
 *  - 코드를 따라 스프링 애니메이션으로 부드럽게 추적
 *  - 처음 잡힐 때 살짝 크게 시작했다가 착 달라붙는 "팝" 효과
 *  - 코드가 사라지면 페이드 아웃
 */
@Composable
fun QrOverlay(
    corners: List<Offset>?,
    locked: Boolean,
    modifier: Modifier = Modifier,
) {
    val animated = remember { List(4) { Animatable(Offset.Zero, Offset.VectorConverter) } }
    var hasPosition by remember { mutableStateOf(false) }
    val pop = remember { Animatable(1f) }

    LaunchedEffect(corners) {
        if (corners == null || corners.size != 4) return@LaunchedEffect
        coroutineScope {
            if (!hasPosition) {
                animated.forEachIndexed { i, a -> a.snapTo(corners[i]) }
                hasPosition = true
            } else {
                animated.forEachIndexed { i, a ->
                    launch {
                        a.animateTo(
                            corners[i],
                            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1400f),
                        )
                    }
                }
            }
        }
    }

    // 프레임마다 위 LaunchedEffect 가 재시작되므로 팝 애니메이션은 별도 effect 로 분리
    LaunchedEffect(hasPosition) {
        if (hasPosition) {
            pop.snapTo(1.25f)
            pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    val visible = corners != null
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (visible) 90 else 220),
        finishedListener = { if (it == 0f) hasPosition = false },
        label = "overlayAlpha",
    )
    val fillAlpha by animateFloatAsState(
        targetValue = if (locked) 0.22f else 0.10f,
        animationSpec = tween(160),
        label = "fillAlpha",
    )

    Canvas(modifier) {
        if (!hasPosition || alpha <= 0.01f) return@Canvas
        val pts = animated.map { it.value }
        val center = Offset(pts.sumOf { it.x.toDouble() }.toFloat() / 4f, pts.sumOf { it.y.toDouble() }.toFloat() / 4f)

        // 코드보다 약간 바깥으로 여유를 둔다
        val padded = pts.map { center + (it - center) * 1.12f }

        scale(pop.value, pivot = center) {
            val quad = Path().apply {
                moveTo(padded[0].x, padded[0].y)
                for (i in 1..3) lineTo(padded[i].x, padded[i].y)
                close()
            }
            drawPath(quad, ScanYellow.copy(alpha = fillAlpha * alpha))

            val edge = (padded[0] - padded[1]).getDistance()
            val stroke = (edge * 0.045f).coerceIn(6f, 14f)
            val brackets = Path()
            for (i in 0..3) {
                val c = padded[i]
                val prev = padded[(i + 3) % 4]
                val next = padded[(i + 1) % 4]
                val a = c + (prev - c) * 0.24f
                val b = c + (next - c) * 0.24f
                brackets.moveTo(a.x, a.y)
                brackets.lineTo(c.x, c.y)
                brackets.lineTo(b.x, b.y)
            }
            drawPath(
                brackets,
                ScanYellow.copy(alpha = alpha),
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
