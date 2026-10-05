package com.mamad.peugeot405

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

val Bg = Color(0xFF0B0D12)
val Panel = Color(0xFF141A24)
val Cyan = Color(0xFF4FC3F7)
val Violet = Color(0xFFB388FF)
val Gold = Color(0xFFFFC857)

private const val LIFT = 3.4f      // بیشترین جابه‌جایی بدنه (واحد نقاشی)
private const val AXLE_GAP = 56f   // فاصله دو محور

@Composable
fun CarStage(
    frontTarget: Float,
    rearTarget: Float,
    frontMs: Int,
    rearMs: Int,
    maybach: Boolean,
    modifier: Modifier = Modifier
) {
    val front by animateFloatAsState(frontTarget, tween(frontMs, easing = FastOutSlowInEasing), label = "front")
    val rear by animateFloatAsState(rearTarget, tween(rearMs, easing = FastOutSlowInEasing), label = "rear")
    val amp by animateFloatAsState(if (maybach) 1f else 0f, tween(700), label = "amp")
    val inf = rememberInfiniteTransition(label = "inf")
    val phase by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "phase")
    val glow by inf.animateFloat(
        0.35f, 0.95f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Box(modifier.background(Brush.verticalGradient(listOf(Color(0xFF111826), Bg)))) {
        Text(
            "405",
            modifier = Modifier.align(Alignment.Center),
            fontSize = 150.sp,
            fontWeight = FontWeight.Black,
            color = Color.White.copy(alpha = 0.04f)
        )
        Canvas(Modifier.fillMaxSize()) {
            val u = size.width * 0.92f / 100f
            val offX = size.width * 0.04f
            val groundY = size.height * 0.80f
            val accent = lerp(Cyan, Gold, amp)

            val dance = sin(phase * 2f * PI.toFloat()) * amp
            val fLift = (front - 0.5f) * 2f + dance * 0.45f
            val rLift = (rear - 0.5f) * 2f - dance * 0.45f
            val avg = (fLift + rLift) / 2f
            val deg = -(atan2((fLift - rLift) * LIFT, AXLE_GAP) * 180f / PI.toFloat())

            withTransform({ translate(left = offX, top = groundY - 36f * u) }) {
                // نور زیر ماشین
                val gc = Offset(50f * u, 36f * u)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.42f * glow), Color.Transparent),
                        center = gc,
                        radius = 48f * u
                    ),
                    radius = 48f * u,
                    center = gc
                )
                // زمین
                drawLine(
                    Color(0x33FFFFFF),
                    Offset(-offX, 36.5f * u),
                    Offset(size.width, 36.5f * u),
                    strokeWidth = 2f
                )
                // بدنه (بالا و پایین می‌رود و می‌چرخد)
                withTransform({
                    translate(left = 0f, top = -avg * LIFT * u)
                    rotate(degrees = deg, pivot = Offset(50f * u, 24f * u))
                }) {
                    drawBody(u, glow)
                }
                // چرخ‌ها روی زمین ثابت‌اند
                drawWheel(u, 22f, 30f, 6.4f)
                drawWheel(u, 78f, 30f, 6.4f)
            }
        }
    }
}

private fun DrawScope.drawBody(u: Float, glow: Float) {
    val body = Path().apply {
        moveTo(4f * u, 29.2f * u)
        lineTo(3f * u, 24.5f * u)
        quadraticBezierTo(3f * u, 19.8f * u, 7.5f * u, 19.2f * u)
        lineTo(23f * u, 17.8f * u)
        lineTo(32f * u, 9.8f * u)
        quadraticBezierTo(33.2f * u, 8.5f * u, 35.5f * u, 8.4f * u)
        lineTo(58f * u, 8.2f * u)
        quadraticBezierTo(60.5f * u, 8.3f * u, 62.5f * u, 10f * u)
        lineTo(73f * u, 17.2f * u)
        lineTo(91f * u, 19.8f * u)
        quadraticBezierTo(97.5f * u, 21f * u, 98.3f * u, 25f * u)
        lineTo(98f * u, 29.2f * u)
        close()
    }
    drawPath(
        body,
        Brush.verticalGradient(listOf(Color(0xFFD9DFEA), Color(0xFF8D96A8)), 8f * u, 29.2f * u)
    )

    val skirt = Path().apply {
        moveTo(4f * u, 27f * u)
        lineTo(98f * u, 26.6f * u)
        lineTo(98f * u, 29.2f * u)
        lineTo(4f * u, 29.2f * u)
        close()
    }
    clipPath(body) {
        drawPath(skirt, Color(0xFF2A303B))
        drawCircle(color = Color(0xFF0A0D13), radius = 7.6f * u, center = Offset(22f * u, 30f * u))
        drawCircle(color = Color(0xFF0A0D13), radius = 7.6f * u, center = Offset(78f * u, 30f * u))
    }

    val w1 = Path().apply {
        moveTo(25.4f * u, 17.3f * u)
        lineTo(33.4f * u, 10.5f * u)
        lineTo(45.4f * u, 10.3f * u)
        lineTo(45.4f * u, 17.3f * u)
        close()
    }
    val w2 = Path().apply {
        moveTo(47.8f * u, 10.3f * u)
        lineTo(58.4f * u, 10.2f * u)
        lineTo(60.6f * u, 11.4f * u)
        lineTo(69.8f * u, 17.3f * u)
        lineTo(47.8f * u, 17.3f * u)
        close()
    }
    val glass = Brush.verticalGradient(listOf(Color(0xFF2E4466), Color(0xFF111B2E)), 10f * u, 17.3f * u)
    drawPath(w1, glass)
    drawPath(w2, glass)

    drawLine(Color(0x55000000), Offset(46.6f * u, 10.5f * u), Offset(46.6f * u, 27f * u), strokeWidth = 1.4f)
    drawLine(Color(0x66000000), Offset(51f * u, 19.6f * u), Offset(56f * u, 19.6f * u), strokeWidth = 2f)
    drawPath(body, Color.White.copy(alpha = 0.35f), style = Stroke(width = 1.5f))

    // چراغ جلو
    val hc = Offset(99f * u, 22.5f * u)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF3B0).copy(alpha = 0.15f + 0.4f * glow), Color.Transparent),
            center = hc,
            radius = 10f * u
        ),
        radius = 10f * u,
        center = hc
    )
    drawRoundRect(
        Color(0xFFFFF6CC),
        Offset(92.8f * u, 21.2f * u),
        Size(5.2f * u, 2.2f * u),
        CornerRadius(1f * u)
    )
    // چراغ عقب
    drawRoundRect(
        Color(0xFFFF3B3B),
        Offset(2.6f * u, 21.4f * u),
        Size(3.6f * u, 2.4f * u),
        CornerRadius(1f * u)
    )
}

private fun DrawScope.drawWheel(u: Float, cx: Float, cy: Float, r: Float) {
    val c = Offset(cx * u, cy * u)
    drawCircle(color = Color(0xFF0E1015), radius = r * u, center = c)
    drawCircle(color = Color(0xFF8E97A8), radius = r * 0.64f * u, center = c)
    drawCircle(color = Color(0xFF1B2029), radius = r * 0.52f * u, center = c)
    for (i in 0 until 5) {
        val a = Math.toRadians(i * 72.0 - 90.0)
        val end = Offset(
            c.x + (cos(a) * r * 0.5f * u).toFloat(),
            c.y + (sin(a) * r * 0.5f * u).toFloat()
        )
        drawLine(Color(0xFFB8C0CF), c, end, strokeWidth = 1.6f)
    }
    drawCircle(color = Color(0xFF8E97A8), radius = r * 0.14f * u, center = c)
}
