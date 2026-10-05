package com.mamad.peugeot405

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2

val Bg = Color(0xFF0B0D12)
val Panel = Color(0xFF141A24)
val Cyan = Color(0xFF4FC3F7)
val Violet = Color(0xFFB388FF)
val Gold = Color(0xFFFFC857)

// ابعاد قاب عکس و محل محورها (پیکسل‌های عکس 1080x630)
private const val FRAME_W = 1080f
private const val FRAME_H = 630f
private const val REAR_X = 386.25f
private const val REAR_Y = 414f
private const val FRONT_X = 910.5f
// بیشترین جابه‌جایی بدنه در عکس (پیکسل)
private const val AMP_F = 39f
private const val AMP_R = 41.25f

/**
 * عکس واقعی ۴۰۵ در چند لایه: پس‌زمینه، لاستیک دور، چرخ‌ها، بدنه.
 * بدنه با زاویه و جابه‌جایی بالا/پایین می‌رود و چرخ‌ها روی زمین می‌مانند.
 * عکس اصلی = جلو پایین (سروو 80) و عقب بالا (سروو 115).
 */
@Composable
fun CarStage(
    posF: () -> Float,
    posB: () -> Float,
    maybach: Boolean,
    modifier: Modifier = Modifier
) {
    val gold by animateFloatAsState(if (maybach) 0.30f else 0f, tween(600), label = "gold")
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(FRAME_W / FRAME_H).background(Bg)) {
        val s = with(LocalDensity.current) { maxWidth.toPx() } / FRAME_W
        val full = Modifier.fillMaxSize()
        Image(painterResource(R.drawable.car_bg), null, full, contentScale = ContentScale.FillBounds)
        Image(painterResource(R.drawable.car_far), null, full, contentScale = ContentScale.FillBounds)
        Image(painterResource(R.drawable.car_wheels), null, full, contentScale = ContentScale.FillBounds)
        Image(
            painterResource(R.drawable.car_body), null,
            full.graphicsLayer {
                val nF = (posF() - 80f) / 32f
                val nB = (posB() - 84f) / 31f
                val df = nF * AMP_F * s
                val dr = (nB - 1f) * AMP_R * s
                val th = atan2(df - dr, (FRONT_X - REAR_X) * s)
                transformOrigin = TransformOrigin(REAR_X / FRAME_W, REAR_Y / FRAME_H)
                rotationZ = -(th * 180f / PI.toFloat())
                translationY = -dr
            },
            contentScale = ContentScale.FillBounds
        )
        // نور طلایی هنگام میباخ
        Box(full.background(Brush.verticalGradient(listOf(Color.Transparent, Gold.copy(alpha = gold)))))
        // محو شدن لبه پایین در پس‌زمینه برنامه
        Box(
            Modifier.fillMaxWidth().height(36.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Bg)))
        )
    }
}
