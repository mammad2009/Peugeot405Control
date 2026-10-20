package com.mamad.peugeot405

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// زمان‌بندی دقیقاً مثل کد آردوینو (channelAnim + bootAnim) به میلی‌ثانیه
private const val STAGE1 = 2500f      // MINI GARAGE MAMMAD
private const val STAGE2 = 2700f      // لوگوی پژو + PEUGEOT + 405

// حرکت نرم: اول تند ، آخرش آروم. خروجی از ۱ (دور) تا ۰ (رسیده)
private fun invSq(t: Float, t0: Float, dur: Float): Float {
    if (t <= t0) return 1f
    if (t >= t0 + dur) return 0f
    val r = 1f - (t - t0) / dur
    return r * r
}

private fun frac(t: Float, t0: Float, dur: Float): Float = ((t - t0) / dur).coerceIn(0f, 1f)

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val clock = remember { Animatable(0f) }
    var finished by remember { mutableStateOf(false) }
    val done by rememberUpdatedState(onDone)

    LaunchedEffect(Unit) {
        clock.animateTo(STAGE1 + STAGE2 + 60f, tween((STAGE1 + STAGE2 + 60f).toInt(), easing = LinearEasing))
        if (!finished) {
            finished = true
            done()
        }
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(Bg)
            .pointerInput(Unit) {
                // لمس صفحه = رد کردن انیمیشن
                detectTapGestures {
                    if (!finished) {
                        finished = true
                        done()
                    }
                }
            }
    ) {
        val wPx = with(LocalDensity.current) { maxWidth.toPx() }

        // ---------- مرحله ۱: MINI GARAGE از چپ ، MAMMAD از راست ، خط از وسط باز میشه ----------
        Column(
            Modifier.align(Alignment.Center)
                .graphicsLayer { alpha = if (clock.value < STAGE1) 1f else 0f },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "MINI GARAGE", color = Silver, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                modifier = Modifier.graphicsLayer { translationX = -invSq(clock.value, 0f, 450f) * wPx }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "MAMMAD", color = Color.White, fontSize = 50.sp, fontWeight = FontWeight.Black,
                letterSpacing = 6.sp,
                style = TextStyle(shadow = Shadow(Cyan.copy(alpha = 0.55f), Offset.Zero, 28f)),
                modifier = Modifier.graphicsLayer { translationX = invSq(clock.value, 250f, 450f) * wPx }
            )
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.width(230.dp).height(2.dp)
                    .graphicsLayer { scaleX = frac(clock.value, 900f, 400f) }
                    .background(Cyan)
            )
        }

        // ---------- مرحله ۲: شیر پژو با خط اسکن باز میشه ، بعد PEUGEOT و 405 از راست میان ----------
        Row(
            Modifier.align(Alignment.Center)
                .graphicsLayer { alpha = if (clock.value >= STAGE1) 1f else 0f },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(118.dp, 125.dp).drawWithContent {
                    val rev = frac(clock.value - STAGE1, 0f, 700f)
                    clipRect(right = size.width * rev) {
                        this@drawWithContent.drawContent()
                    }
                    if (rev > 0f && rev < 1f) {
                        drawRect(
                            Cyan, Offset(size.width * rev - 1.5.dp.toPx(), 0f),
                            Size(2.dp.toPx(), size.height)
                        )
                    }
                }
            ) {
                Image(
                    painterResource(R.drawable.peugeot_lion), null,
                    Modifier.size(118.dp, 125.dp), contentScale = ContentScale.Fit
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "PEUGEOT", color = Silver, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    modifier = Modifier.graphicsLayer {
                        translationX = invSq(clock.value - STAGE1, 700f, 350f) * wPx
                    }
                )
                Text(
                    "405", color = Color.White, fontSize = 68.sp, fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    style = TextStyle(shadow = Shadow(Cyan.copy(alpha = 0.5f), Offset.Zero, 26f)),
                    modifier = Modifier.graphicsLayer {
                        translationX = invSq(clock.value - STAGE1, 950f, 400f) * wPx
                    }
                )
            }
        }

        // ---------- بسته شدن از بالا و پایین (مثل خاموش شدن تلویزیون) ----------
        Canvas(Modifier.fillMaxSize()) {
            val t = clock.value
            val b = if (t < STAGE1) frac(t, 2150f, 350f) else frac(t, STAGE1 + 2350f, 350f)
            if (b > 0f) {
                val bar = b * size.height / 2f
                drawRect(Bg, Offset(0f, 0f), Size(size.width, bar))
                drawRect(Bg, Offset(0f, size.height - bar), Size(size.width, bar))
                if (t >= STAGE1 && b > 0.65f) {
                    drawRect(Silver, Offset(0f, size.height / 2f - 1.dp.toPx()), Size(size.width, 2.dp.toPx()))
                }
            }
        }
    }
}
