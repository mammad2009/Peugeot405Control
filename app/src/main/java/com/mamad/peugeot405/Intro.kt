package com.mamad.peugeot405

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures

// ============================================================================
//  انیمیشن شروع برنامه، دقیقاً مثل کد آردوینو (channelAnim + bootAnim):
//   ۱) MINI GARAGE از چپ و MAMMAD از راست می‌آید ، خط زیرش باز می‌شود ، از بالا و پایین جمع می‌شود
//   ۲) شیر پژو با خط اسکن باز می‌شود ، PEUGEOT و 405 از راست می‌آیند ، مثل خاموش شدن تلویزیون جمع می‌شود
//  همه‌ی اعداد در «واحد OLED» هستند (صفحه‌ی ۱۲۸ در ۶۴) و روی عرض گوشی کشیده می‌شوند.
// ============================================================================

private const val CH_MS = 2500f      // مدت صحنه‌ی اسم کانال
private const val BOOT_MS = 2700f    // مدت صحنه‌ی لوگوی پژو
private const val FADE_MS = 300f     // محو شدن آخر
private const val TOTAL_MS = CH_MS + BOOT_MS

// حرکت نرم (مثل slideX / slideL در آردوینو): شروع سریع و توقف نرم
private fun slide(t: Float, t0: Float, dur: Float, from: Float, target: Float): Float {
    if (t <= t0) return from
    if (t >= t0 + dur) return target
    val r = 1f - (t - t0) / dur
    return target + (from - target) * r * r
}

@Composable
fun IntroSplash(onDone: () -> Unit) {
    val clock = remember { Animatable(0f) }
    var finished by remember { mutableStateOf(false) }
    val done by rememberUpdatedState(onDone)

    fun finish() {
        if (!finished) {
            finished = true
            done()
        }
    }

    LaunchedEffect(Unit) {
        val total = TOTAL_MS + FADE_MS
        clock.animateTo(total, tween(total.toInt(), easing = LinearEasing))
        finish()
    }

    val t = clock.value
    val fade = 1f - ((t - TOTAL_MS) / FADE_MS).coerceIn(0f, 1f)

    BoxWithConstraints(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = fade }
            .background(Bg)
            .pointerInput(Unit) { detectTapGestures { finish() } }
    ) {
        val s: Dp = maxWidth / 128f            // اندازه‌ی یک واحد OLED
        val density = LocalDensity.current
        fun u(v: Float): TextUnit = with(density) { (s * v).toSp() }

        val glowStyle = { size: Float ->
            TextStyle(
                color = Color.White,
                fontSize = u(size),
                fontWeight = FontWeight.Black,
                shadow = Shadow(Cyan.copy(alpha = 0.55f), Offset.Zero, 22f)
            )
        }

        // صحنه‌ی ۱۲۸ در ۶۴ وسط صفحه
        Box(Modifier.align(Alignment.Center).size(maxWidth, maxWidth / 2f).clipToBounds()) {

            if (t < CH_MS) {
                // ---------- MINI GARAGE MAMMAD ----------
                val x1 = slide(t, 0f, 450f, -102f, 13f)
                val x2 = slide(t, 250f, 450f, 128f, 8f)
                Box(Modifier.offset(s * x1, s * 10f).size(s * 102f, s * 10f), contentAlignment = Alignment.Center) {
                    Text(
                        "MINI GARAGE", style = glowStyle(13.2f), maxLines = 1, softWrap = false,
                        modifier = Modifier.wrapContentSize(Alignment.Center, unbounded = true)
                    )
                }
                Box(Modifier.offset(s * x2, s * 28f).size(s * 112f, s * 16f), contentAlignment = Alignment.Center) {
                    Text(
                        "MAMMAD", style = glowStyle(21.5f), maxLines = 1, softWrap = false,
                        modifier = Modifier.wrapContentSize(Alignment.Center, unbounded = true)
                    )
                }
                if (t > 900f) {
                    val len = ((t - 900f) * 100f / 400f).coerceAtMost(100f)
                    Box(
                        Modifier.offset(s * (64f - len / 2f), s * 52f).size(s * len, s * 2f)
                            .background(Brush.horizontalGradient(listOf(Cyan, Color.White, Cyan)))
                    )
                }
                if (t > 2150f) {
                    val bar = ((t - 2150f) * 32f / 350f).coerceAtMost(32f)
                    Box(Modifier.offset(0.dp, 0.dp).size(maxWidth, s * bar).background(Bg))
                    Box(Modifier.offset(0.dp, s * (64f - bar)).size(maxWidth, s * bar).background(Bg))
                }
            } else {
                // ---------- لوگوی پژو و 405 ----------
                val tb = t - CH_MS
                val rev = if (tb < 700f) tb * 62f / 700f else 62f
                // هاله‌ی پشت شیر
                Canvas(Modifier.size(maxWidth, maxWidth / 2f)) {
                    val a = (rev / 62f) * 0.28f
                    val c = Offset(size.width * 0.25f, size.height * 0.5f)
                    drawCircle(
                        Brush.radialGradient(listOf(Cyan.copy(alpha = a), Color.Transparent), center = c, radius = size.width * 0.45f),
                        radius = size.width * 0.45f, center = c
                    )
                }
                Image(
                    painterResource(R.drawable.peugeot_lion), null,
                    Modifier.offset(s * 2f, 0.dp).size(s * 62f, s * 64f)
                        .drawWithContent {
                            clipRect(right = size.width * (rev / 62f)) { this@drawWithContent.drawContent() }
                        },
                    contentScale = ContentScale.Fit
                )
                if (rev < 62f) {
                    // خط اسکن
                    Box(
                        Modifier.offset(s * (2f + rev), 0.dp).size(s * 1.2f, s * 64f)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Cyan, Color.White, Cyan, Color.Transparent)))
                    )
                }
                val xp = slide(tb, 700f, 350f, 128f, 74f)
                val x4 = slide(tb, 950f, 400f, 128f, 73f)
                Box(Modifier.offset(s * xp, s * 17f).size(s * 51f, s * 7f), contentAlignment = Alignment.Center) {
                    Text(
                        "PEUGEOT",
                        style = TextStyle(
                            color = Silver, fontSize = u(9.6f), fontWeight = FontWeight.Bold,
                            letterSpacing = u(0.5f), shadow = Shadow(Cyan.copy(alpha = 0.4f), Offset.Zero, 14f)
                        ),
                        maxLines = 1, softWrap = false,
                        modifier = Modifier.wrapContentSize(Alignment.Center, unbounded = true)
                    )
                }
                Box(Modifier.offset(s * x4, s * 32f).size(s * 52f, s * 20f), contentAlignment = Alignment.Center) {
                    Text(
                        "405", style = glowStyle(27f), maxLines = 1, softWrap = false,
                        modifier = Modifier.wrapContentSize(Alignment.Center, unbounded = true)
                    )
                }
                // آخرش مثل خاموش شدن تلویزیون جمع می‌شود
                if (tb > 2350f) {
                    val bar = ((tb - 2350f) * 31f / 350f).coerceAtMost(31f)
                    Box(Modifier.offset(0.dp, 0.dp).size(maxWidth, s * bar).background(Bg))
                    Box(Modifier.offset(0.dp, s * (64f - bar)).size(maxWidth, s * bar).background(Bg))
                    if (bar > 20f) {
                        Box(
                            Modifier.offset(0.dp, s * 31f).size(maxWidth, s * 2f)
                                .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White, Color.Transparent)))
                        )
                    }
                }
            }
        }

        Text(
            "برای رد کردن لمس کن", color = Steel.copy(alpha = 0.55f), fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp)
        )
    }
}
