package com.mamad.peugeot405

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// نسبت ارتفاع به عرض عکس شیر (peugeot_lion.png = 240x254)
const val LION_RATIO = 1.058f

// ======================= سه چنگال (امضای نوری پژو) =======================

fun DrawScope.drawClaws(origin: Offset, length: Float, color: Color, progress: Float, thickness: Float, gap: Float) {
    if (progress <= 0f) return
    for (i in 0..2) {
        val len = length * (1f - i * 0.2f) * progress
        val o = Offset(origin.x + i * gap, origin.y + i * gap * 0.15f)
        drawLine(
            color.copy(alpha = color.alpha * (1f - i * 0.18f)),
            o, Offset(o.x + len * 0.42f, o.y + len), thickness, StrokeCap.Round
        )
    }
}

// ======================= ظاهر شدن فنری هر عنصر =======================

@Composable
fun Modifier.reveal(delayMs: Int = 0, fromY: Dp = 36.dp): Modifier {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        p.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 140f))
    }
    return this.graphicsLayer {
        val v = p.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * fromY.toPx()
        val s = 0.92f + 0.08f * v
        scaleX = s
        scaleY = s
    }
}

// ======================= شیر پژو با درخشش متحرک =======================

@Composable
fun LionLogo(width: Dp, modifier: Modifier = Modifier, shimmer: () -> Float = { -1f }) {
    Image(
        painterResource(R.drawable.peugeot_lion), null,
        modifier.size(width, width * LION_RATIO)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val s = shimmer()
                if (s in 0f..1f) {
                    val x = -size.width * 0.6f + s * size.width * 2.2f
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.95f), Color.Transparent),
                            start = Offset(x, 0f), end = Offset(x + size.width * 0.55f, size.height * 0.55f)
                        ),
                        blendMode = BlendMode.SrcAtop
                    )
                }
            },
        contentScale = ContentScale.Fit
    )
}

// ======================= پس‌زمینه‌ی زنده =======================

@Composable
fun AmbientBackground(modifier: Modifier = Modifier, accent: Color = Cyan) {
    val inf = rememberInfiniteTransition(label = "amb")
    val a = inf.animateFloat(0f, 1f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "ambA")
    Canvas(modifier.fillMaxSize()) {
        val ph = a.value * 2f * PI.toFloat()
        val c1 = Offset(size.width * (0.18f + 0.16f * sin(ph)), size.height * (0.12f + 0.07f * cos(ph)))
        val c2 = Offset(size.width * (0.86f - 0.14f * cos(ph)), size.height * (0.78f + 0.06f * sin(ph)))
        drawCircle(
            Brush.radialGradient(listOf(accent.copy(alpha = 0.13f), Color.Transparent), center = c1, radius = size.width * 0.85f),
            radius = size.width * 0.85f, center = c1
        )
        drawCircle(
            Brush.radialGradient(listOf(PeugeotBlue.copy(alpha = 0.12f), Color.Transparent), center = c2, radius = size.width * 0.9f),
            radius = size.width * 0.9f, center = c2
        )
    }
}

// ======================= کلید تغییر زبان (فارسی / English) =======================

@Composable
fun LangToggle(fa: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val pos by animateFloatAsState(if (fa) 0f else 1f, spring(dampingRatio = 0.7f, stiffness = 320f), label = "langPos")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val shape = RoundedCornerShape(50)
        Box(
            modifier.size(108.dp, 36.dp).clip(shape).background(Panel)
                .border(1.dp, Color(0x33FFFFFF), shape)
        ) {
            Box(
                Modifier.fillMaxHeight().width(54.dp)
                    .offset { IntOffset((pos * 54.dp.toPx()).roundToInt(), 0) }
                    .padding(3.dp).clip(shape)
                    .background(Brush.horizontalGradient(listOf(PeugeotBlue, Cyan)))
            )
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight().clickable { onChange(true) }, contentAlignment = Alignment.Center) {
                    Text("فارسی", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (fa) Color.White else Steel)
                }
                Box(Modifier.weight(1f).fillMaxHeight().clickable { onChange(false) }, contentAlignment = Alignment.Center) {
                    Text("EN", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, color = if (!fa) Color.White else Steel)
                }
            }
        }
    }
}

// ======================= صحنه‌ی بالای صفحه‌ی کنترل (آرم شیر + ۴۰۵) =======================

/**
 * به جای عکس ماکت: آرم پژو و نوشته‌ی PEUGEOT 405.
 * آرم با وضعیت تعلیق بالا و پایین می‌رود و کج می‌شود، سایه‌ی کف کوچک و بزرگ می‌شود.
 */
@Composable
fun EmblemStage(posF: () -> Float, posB: () -> Float, maybach: Boolean, modifier: Modifier = Modifier) {
    val S = LocalStrings.current
    val frontOnRight = LocalLayoutDirection.current == LayoutDirection.Rtl
    val gold by animateFloatAsState(if (maybach) 1f else 0f, tween(700), label = "stGold")
    val inf = rememberInfiniteTransition(label = "stInf")
    val breathe = inf.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "stBreathe"
    )
    val sweep = inf.animateFloat(0f, 1f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "stSweep")
    val accent = lerp(Cyan, Gold, gold)

    Box(modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val nF = ((posF() - 80f) / 32f).coerceIn(0f, 1f)
            val nB = ((posB() - 84f) / 31f).coerceIn(0f, 1f)
            val heave = (nF + nB) / 2f
            val cx = size.width / 2f
            val floorY = size.height * 0.86f

            // هاله‌ی پشت آرم
            val ac = Offset(cx, size.height * 0.42f)
            drawCircle(
                Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.20f + 0.10f * breathe.value + 0.10f * heave), Color.Transparent),
                    center = ac, radius = size.width * 0.55f
                ),
                radius = size.width * 0.55f, center = ac
            )

            // شبکه‌ی پرسپکتیو کف
            for (k in -4..4) {
                val x0 = cx + k * size.width * 0.07f
                val x1 = cx + k * size.width * 0.20f
                drawLine(accent.copy(alpha = 0.10f), Offset(x0, floorY), Offset(x1, size.height), 1.dp.toPx())
            }
            for (j in 1..3) {
                val y = floorY + (size.height - floorY) * (j / 3f) * (j / 3f)
                drawLine(accent.copy(alpha = 0.10f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            }

            // خط کف + نور متحرک
            drawLine(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, accent.copy(alpha = 0.85f), Color.Transparent),
                    startX = 0f, endX = size.width
                ),
                Offset(0f, floorY), Offset(size.width, floorY), 1.6.dp.toPx()
            )
            val hx = sweep.value * size.width
            drawCircle(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), center = Offset(hx, floorY), radius = 46.dp.toPx()
                ),
                radius = 46.dp.toPx(), center = Offset(hx, floorY)
            )

            // سایه‌ی کف: هرچه آرم بالاتر برود کوچک‌تر و کم‌رنگ‌تر می‌شود
            val rw = size.width * (0.30f - 0.08f * heave)
            drawOval(
                Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.50f * (1f - 0.5f * heave)), Color.Transparent),
                    center = Offset(cx, floorY), radius = rw
                ),
                topLeft = Offset(cx - rw, floorY - rw * 0.14f), size = Size(rw * 2f, rw * 0.28f)
            )
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                Modifier
                    .graphicsLayer {
                        val nF = ((posF() - 80f) / 32f).coerceIn(0f, 1f)
                        val nB = ((posB() - 84f) / 31f).coerceIn(0f, 1f)
                        val dir = if (frontOnRight) -1f else 1f
                        rotationZ = dir * (nF - nB) * 6f
                        translationY = -((nF + nB) / 2f) * 16.dp.toPx() - 6.dp.toPx()
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                LionLogo(78.dp, shimmer = {
                    val s = sweep.value
                    if (s < 0.4f) s / 0.4f else -1f
                })
                Spacer(Modifier.width(14.dp))
                Canvas(Modifier.size(24.dp, 76.dp)) {
                    drawClaws(Offset(2.dp.toPx(), 4.dp.toPx()), size.height * 0.92f, accent.copy(alpha = 0.9f), 1f, 2.6.dp.toPx(), 7.dp.toPx())
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("PEUGEOT", color = Silver, fontSize = 13.sp, letterSpacing = 7.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "405",
                        style = TextStyle(
                            brush = Brush.verticalGradient(listOf(Color.White, accent.copy(alpha = 0.65f))),
                            fontSize = 58.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp
                        )
                    )
                    Text(S.stageTag, color = accent.copy(alpha = 0.8f), fontSize = 9.sp, letterSpacing = 4.sp)
                }
            }
        }
    }
}
