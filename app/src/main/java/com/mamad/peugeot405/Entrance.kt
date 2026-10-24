package com.mamad.peugeot405

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun seg(t: Float, a: Float, b: Float): Float = ((t - a) / (b - a)).coerceIn(0f, 1f)

// فنر با کمی جهش (برای ظاهر شدن آرم)
private val Overshoot = Easing { t ->
    val s = 1.6f
    val x = t - 1f
    x * x * ((s + 1f) * x + s) + 1f
}

// ======================= آیکن‌ها (رسم دستی، چون بسته‌ی آیکن کامل نداریم) =======================

fun DrawScope.drawBluetooth(c: Offset, s: Float, color: Color, strokeW: Float) {
    val p = Path().apply {
        moveTo(c.x - 0.5f * s, c.y - 0.5f * s)
        lineTo(c.x + 0.5f * s, c.y + 0.5f * s)
        lineTo(c.x, c.y + s)
        lineTo(c.x, c.y - s)
        lineTo(c.x + 0.5f * s, c.y - 0.5f * s)
        lineTo(c.x - 0.5f * s, c.y + 0.5f * s)
    }
    drawPath(p, color, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

// قفل بسته یا باز (خطی، مثل آیکن روی ریموت)
fun DrawScope.drawPadlock(c: Offset, s: Float, color: Color, open: Boolean, strokeW: Float) {
    val r = 0.27f * s
    val bodyTop = c.y - 0.12f * s
    val bodyH = 0.78f * s
    val arcTop = c.y - 0.65f * s + (if (open) -0.16f * s else 0f)
    val left = c.x - r
    val right = c.x + r
    val shackle = Path().apply {
        moveTo(left, bodyTop)
        lineTo(left, arcTop + r)
        arcTo(Rect(left, arcTop, right, arcTop + 2f * r), 180f, 180f, false)
        lineTo(right, if (open) arcTop + r + 0.2f * s else bodyTop)
    }
    val st = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(shackle, color, style = st)
    drawRoundRect(
        color, topLeft = Offset(c.x - 0.5f * s, bodyTop), size = Size(s, bodyH),
        cornerRadius = CornerRadius(0.14f * s), style = Stroke(width = strokeW)
    )
    drawCircle(color, 0.07f * s, Offset(c.x, bodyTop + 0.30f * s))
    drawLine(color, Offset(c.x, bodyTop + 0.34f * s), Offset(c.x, bodyTop + 0.52f * s), strokeW, StrokeCap.Round)
}

// ======================= صفحه‌ی شروع: آرم پژو + PEUGEOT 405 =======================

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val tl = remember { Animatable(0f) }
    val out = remember { Animatable(0f) }
    var skip by remember { mutableStateOf(false) }
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(skip) {
        if (!skip) {
            tl.animateTo(1f, tween(3600, easing = LinearEasing))
            delay(450)
        }
        out.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        done()
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            Modifier.fillMaxSize().background(Bg)
                .pointerInput(Unit) { detectTapGestures { skip = true } }
                .graphicsLayer {
                    alpha = 1f - out.value
                    val s = 1f + 0.10f * out.value
                    scaleX = s
                    scaleY = s
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val t = tl.value
                val c = Offset(size.width / 2f, size.height * 0.40f)
                val glow = seg(t, 0.05f, 0.5f)
                drawCircle(
                    Brush.radialGradient(
                        listOf(PeugeotBlue.copy(alpha = 0.38f * glow), Cyan.copy(alpha = 0.08f * glow), Color.Transparent),
                        center = c, radius = size.width * 0.9f
                    ),
                    radius = size.width * 0.9f, center = c
                )
                // سه چنگال که از بالا چپ صفحه کشیده می‌شوند و محو می‌شوند
                val cp = seg(t, 0f, 0.28f)
                val fade = 1f - seg(t, 0.30f, 0.58f)
                drawClaws(
                    Offset(size.width * 0.10f, size.height * 0.10f), size.height * 0.60f,
                    Cyan.copy(alpha = 0.6f * fade), cp, 3.dp.toPx(), 28.dp.toPx()
                )
                // موج دور آرم
                val rp = seg(t, 0.18f, 0.62f)
                if (rp > 0f && rp < 1f) {
                    drawCircle(
                        Cyan.copy(alpha = 0.55f * (1f - rp)), 64.dp.toPx() + rp * size.width * 0.65f, c,
                        style = Stroke(1.6.dp.toPx())
                    )
                }
                // نوار پیشرفت باریک پایین
                val by = size.height - 40.dp.toPx()
                val bw = size.width * 0.34f
                drawLine(Color(0x22FFFFFF), Offset(c.x - bw / 2f, by), Offset(c.x + bw / 2f, by), 2.dp.toPx(), StrokeCap.Round)
                drawLine(Cyan, Offset(c.x - bw / 2f, by), Offset(c.x - bw / 2f + bw * t, by), 2.dp.toPx(), StrokeCap.Round)
            }
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                LionLogo(
                    130.dp,
                    Modifier.graphicsLayer {
                        val p = Overshoot.transform(seg(tl.value, 0.15f, 0.55f))
                        scaleX = 0.4f + 0.6f * p
                        scaleY = 0.4f + 0.6f * p
                        alpha = seg(tl.value, 0.15f, 0.35f)
                        rotationZ = (1f - p) * -14f
                    },
                    shimmer = {
                        val s = seg(tl.value, 0.62f, 0.92f)
                        if (s > 0.001f && s < 0.999f) s else -1f
                    }
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    "PEUGEOT".forEachIndexed { i, ch ->
                        Text(
                            ch.toString(), color = Silver, fontSize = 26.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.graphicsLayer {
                                val p = seg(tl.value, 0.42f + i * 0.03f, 0.56f + i * 0.03f)
                                alpha = p
                                translationY = (1f - p) * 22.dp.toPx()
                            }
                        )
                    }
                }
                Text(
                    "405",
                    style = TextStyle(
                        brush = Brush.verticalGradient(listOf(Color.White, Color(0xFF7F8AA3))),
                        fontSize = 84.sp, fontWeight = FontWeight.Black, letterSpacing = 10.sp
                    ),
                    modifier = Modifier.graphicsLayer {
                        val p = Overshoot.transform(seg(tl.value, 0.55f, 0.82f))
                        alpha = seg(tl.value, 0.55f, 0.70f)
                        scaleX = 0.7f + 0.3f * p
                        scaleY = 0.7f + 0.3f * p
                    }
                )
                Canvas(Modifier.size(230.dp, 6.dp)) {
                    val p = seg(tl.value, 0.70f, 0.95f)
                    val y = size.height / 2f
                    drawLine(
                        Brush.horizontalGradient(listOf(Color.Transparent, Cyan, PeugeotBlue, Color.Transparent)),
                        Offset(size.width * (0.5f - 0.5f * p), y), Offset(size.width * (0.5f + 0.5f * p), y),
                        2.dp.toPx(), StrokeCap.Round
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "PNEUMATIC CONTROL", color = Steel, fontSize = 11.sp, letterSpacing = 6.sp,
                    modifier = Modifier.graphicsLayer { alpha = seg(tl.value, 0.80f, 0.96f) }
                )
            }
        }
    }
}

// ======================= صفحه‌ی اتصال =======================

@Composable
fun ConnectScreen(vm: MainViewModel, onPick: () -> Unit, onSkip: () -> Unit) {
    val S = LocalStrings.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(350); shown = true }
    // ظاهر شدن با فنر؛ محو شدن بعد از اتصال موفق
    val appear by animateFloatAsState(
        if (shown) 1f else 0f, spring(dampingRatio = 0.62f, stiffness = 60f), label = "cAppear"
    )
    val hide by animateFloatAsState(
        if (vm.connected) 1f else 0f, tween(800, easing = FastOutSlowInEasing), label = "cHide"
    )
    val busy by animateFloatAsState(if (vm.connecting) 1f else 0f, tween(500), label = "cBusy")
    var pressed by remember { mutableStateOf(false) }
    val press by animateFloatAsState(if (pressed) 0.94f else 1f, tween(100), label = "cPress")
    val inf = rememberInfiniteTransition(label = "cInf")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3800, easing = LinearEasing)), label = "cT")
    val sweep by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "cSweep")

    BoxWithConstraints(Modifier.fillMaxSize().background(Bg)) {
        val centerShift = maxHeight * 0.05f

        // حلقه‌های هم‌مرکز و موج‌های رادار
        Canvas(Modifier.fillMaxSize()) {
            val vis = appear.coerceIn(0f, 1f) * (1f - hide)
            if (vis > 0.001f) {
                val c = Offset(size.width / 2f, size.height / 2f - centerShift.toPx())
                val base = 82.dp.toPx()
                val ph = t * 2f * PI.toFloat()
                val n = 26
                for (i in 0 until n) {
                    val f = i / (n - 1f)
                    val rad = base * 1.2f + f * size.width * 0.62f
                    val off = sin(ph + f * 6f) * f * 16.dp.toPx()
                    val a = (0.62f * (1f - f) + 0.07f) * vis * (0.55f + 0.45f * busy)
                    drawCircle(
                        lerp(Cyan, Color(0xFF0B3A66), f).copy(alpha = a), rad,
                        c + Offset(off, off * 0.45f), style = Stroke(0.8.dp.toPx())
                    )
                }
                for (k in 0..2) {
                    val p = (t + k / 3f) % 1f
                    drawCircle(
                        Cyan.copy(alpha = (1f - p) * 0.45f * vis * (0.35f + 0.65f * busy)),
                        base + p * size.width * 0.55f, c, style = Stroke(1.6.dp.toPx())
                    )
                }
                if (busy > 0.01f) {
                    drawArc(
                        Cyan.copy(alpha = busy * vis), sweep, 100f, false,
                        Offset(c.x - base, c.y - base), Size(base * 2f, base * 2f),
                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // آرم و نام برنامه بالای صفحه
        Column(
            Modifier.align(Alignment.TopCenter).padding(top = 40.dp)
                .graphicsLayer {
                    alpha = appear.coerceIn(0f, 1f) * (1f - hide)
                    translationY = -(1f - appear) * 60.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LionLogo(46.dp, shimmer = { if (t < 0.3f) t / 0.3f else -1f })
            Spacer(Modifier.height(6.dp))
            Text("PEUGEOT", color = Silver, fontSize = 12.sp, letterSpacing = 8.sp, fontWeight = FontWeight.Medium)
            Text("405", color = Color.White, fontSize = 40.sp, letterSpacing = 6.sp, fontWeight = FontWeight.Black)
            Text("PNEUMATIC CONTROL", color = Steel, fontSize = 10.sp, letterSpacing = 4.sp)
        }

        // کلید زبان
        LangToggle(
            fa = vm.isFa, onChange = { vm.setLang(it) },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                .graphicsLayer { alpha = appear.coerceIn(0f, 1f) * (1f - hide) }
        )

        // دکمه‌ی اتصال
        Box(
            Modifier.align(Alignment.Center).offset(y = -centerShift).size(150.dp)
                .graphicsLayer {
                    val sc = (0.55f + 0.45f * appear) * (1f + 0.5f * hide) * press *
                        (1f + 0.025f * sin(t * 2f * PI.toFloat()))
                    scaleX = sc
                    scaleY = sc
                    alpha = appear.coerceIn(0f, 1f) * (1f - hide)
                    translationY = (1f - appear) * 160.dp.toPx()
                }
                .clip(CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            try { tryAwaitRelease() } finally { pressed = false }
                        },
                        onTap = {
                            if (!vm.connecting && !vm.connected) {
                                if (!vm.connectQuick()) onPick()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFF0E202E), Color(0xFF04070A)), center = center, radius = r),
                    radius = r, center = center
                )
                drawCircle(
                    brush = Brush.sweepGradient(listOf(Cyan, Color(0xFF0B3A66), Cyan.copy(alpha = 0.2f), Cyan), center),
                    radius = r - 1.dp.toPx(), center = center, style = Stroke(2.dp.toPx())
                )
                drawCircle(Cyan.copy(alpha = 0.18f), r - 11.dp.toPx(), center, style = Stroke(1.dp.toPx()))
                drawBluetooth(center, 24.dp.toPx(), Cyan, 3.dp.toPx())
            }
        }

        // متن زیر دکمه
        val title = when {
            vm.connected -> S.connected
            vm.connecting -> S.connecting
            else -> S.connectModule
        }
        val sub = if (vm.statusCode == "idle") S.connectHint else vm.status
        Column(
            Modifier.align(Alignment.Center).offset(y = -centerShift + 125.dp)
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    alpha = appear.coerceIn(0f, 1f) * (1f - hide)
                    translationY = (1f - appear) * 160.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(sub, color = Steel, fontSize = 12.sp)
        }

        // لینک‌های پایین صفحه
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp)
                .graphicsLayer { alpha = appear.coerceIn(0f, 1f) * (1f - hide) },
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                S.pickDevice, color = Steel, fontSize = 12.sp,
                modifier = Modifier.clickable { vm.refreshDevices(); onPick() }.padding(12.dp)
            )
            Text(
                S.demoMode, color = Steel, fontSize = 12.sp,
                modifier = Modifier.clickable { onSkip() }.padding(12.dp)
            )
        }
    }
}

// ======================= ریموت پژو (واقعی‌تر) =======================

private fun chromeBrush(x0: Float, x1: Float) = Brush.horizontalGradient(
    listOf(Color(0xFF4F555F), Color(0xFFF4F6FA), Color(0xFF8A92A1), Color(0xFFE9ECF2), Color(0xFF434953)),
    startX = x0, endX = x1
)

/**
 * بدنه‌ی ریموت: پلاستیک براق مشکی، لبه‌ی کروم، پلاک شیر، حلقه‌ی کلید، تیغه‌ی تاشو
 * tilt: -1..1 (حرکت برق روی بدنه) ، keyOut: 0 بسته .. 1 باز ، sig: 0..1 پیشرفت ارسال سیگنال (1 = بیکار)
 */
private fun DrawScope.drawRemoteBody(locked: Boolean?, connected: Boolean, tilt: Float, keyOut: Float, sig: Float) {
    val w = size.width
    val h = size.height
    val left = w * 0.05f
    val right = w * 0.95f
    val top = h * 0.06f
    val bottom = h * 0.80f
    val accent = when (locked) {
        true -> Cyan
        false -> Gold
        null -> Steel
    }
    val body = Path().apply {
        addRoundRect(
            RoundRect(
                left, top, right, bottom,
                CornerRadius(w * 0.30f), CornerRadius(w * 0.30f), CornerRadius(w * 0.15f), CornerRadius(w * 0.15f)
            )
        )
    }

    // چراغ‌های جلوی ماشین که موقع قفل/باز چشمک می‌زنند
    val blink = if (sig < 1f) max(0f, sin(sig * PI.toFloat() * 4f)) else 0f
    if (blink > 0.01f) {
        for (sx in listOf(-0.10f, 1.10f)) {
            val c = Offset(w * sx, h * 0.52f)
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFFFFA726).copy(alpha = 0.55f * blink), Color.Transparent), center = c, radius = w * 0.75f),
                radius = w * 0.75f, center = c
            )
        }
    }

    // موج‌های سیگنال رادیویی
    if (sig < 1f) {
        val c = Offset(w / 2f, h * 0.19f)
        for (k in 0..2) {
            val p = (sig - k * 0.14f).coerceIn(0f, 1f)
            if (p > 0f && p < 1f) {
                drawCircle(accent.copy(alpha = (1f - p) * 0.65f), w * 0.25f + p * w * 0.95f, c, style = Stroke(w * 0.008f))
            }
        }
    }

    // سایه‌ی نرم زیر ریموت
    for (i in 1..8) {
        translate(top = i * w * 0.012f) { drawPath(body, Color.Black.copy(alpha = 0.07f)) }
    }

    // تیغه‌ی کلید (پشت بدنه؛ با باز شدن ریموت می‌چرخد و بیرون می‌آید)
    val px = w * 0.5f
    val py = h * 0.66f
    val bl = h * 0.27f
    val bw = w * 0.17f
    rotate(degrees = 180f * (1f - keyOut), pivot = Offset(px, py)) {
        val blade = Path().apply {
            addRoundRect(RoundRect(px - bw / 2f, py, px + bw / 2f, py + bl, CornerRadius(bw * 0.3f)))
        }
        drawPath(blade, chromeBrush(px - bw / 2f, px + bw / 2f))
        for (i in 0..4) {
            drawRect(
                Color(0xFF2C3037),
                Offset(px + bw / 2f - bw * 0.22f, py + bl * (0.38f + i * 0.115f)), Size(bw * 0.22f, bl * 0.05f)
            )
        }
        drawLine(Color.Black.copy(alpha = 0.35f), Offset(px, py + bl * 0.30f), Offset(px, py + bl * 0.96f), w * 0.006f)
    }

    // بدنه
    drawPath(
        body,
        Brush.verticalGradient(
            listOf(Color(0xFF30353F), Color(0xFF181B21), Color(0xFF0A0C10)), startY = top, endY = bottom
        )
    )
    clipPath(body) {
        // انعکاس براق بالا
        drawRect(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.17f), Color.White.copy(alpha = 0.02f), Color.Transparent),
                startY = top, endY = top + (bottom - top) * 0.45f
            ),
            Offset(left, top), Size(right - left, (bottom - top) * 0.45f)
        )
        // برق متحرک (با حرکت ریموت جابه‌جا می‌شود)
        val bx = w * (0.5f + 0.5f * tilt)
        drawRect(
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.20f), Color.Transparent),
                start = Offset(bx - w * 0.30f, top), end = Offset(bx + w * 0.10f, bottom)
            ),
            Offset(left, top), Size(right - left, bottom - top)
        )
        // سایه‌ی پایین
        drawRect(
            Brush.verticalGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)), startY = bottom - h * 0.12f, endY = bottom
            ),
            Offset(left, bottom - h * 0.12f), Size(right - left, h * 0.12f)
        )
    }
    // لبه‌ی کروم
    drawPath(
        body,
        Brush.linearGradient(
            listOf(Color(0xFFF4F6FA), Color(0xFF4A5160), Color(0xFF14171C), Color(0xFFA3ABBB), Color(0xFF3B414D)),
            start = Offset(0f, 0f), end = Offset(w, h)
        ),
        style = Stroke(w * 0.016f)
    )
    scale(0.965f, pivot = Offset(w / 2f, (top + bottom) / 2f)) {
        drawPath(body, Color.Black.copy(alpha = 0.5f), style = Stroke(w * 0.005f))
    }

    // پلاک شیر (قاب فرورفته‌ی پشت آرم)
    val pc = Offset(w / 2f, h * 0.19f)
    val pr = w * 0.215f
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFF1D2128), Color(0xFF07080B)), center = pc, radius = pr),
        radius = pr, center = pc
    )
    drawCircle(
        Brush.linearGradient(
            listOf(Color(0xFF3B414D), Color(0xFFF1F4F9), Color(0xFF50565F)),
            start = Offset(pc.x - pr, pc.y - pr), end = Offset(pc.x + pr, pc.y + pr)
        ),
        radius = pr, center = pc, style = Stroke(w * 0.012f)
    )

    // حلقه‌ی کلید
    val ex = w * 0.19f
    val ey = top + w * 0.105f
    drawCircle(Color.Black.copy(alpha = 0.5f), w * 0.105f, Offset(w * 0.085f, top + w * 0.05f), style = Stroke(w * 0.024f))
    drawCircle(
        Brush.linearGradient(
            listOf(Color(0xFFF4F6FA), Color(0xFF7C8494), Color(0xFFD9DDE6)),
            start = Offset(0f, 0f), end = Offset(w * 0.2f, w * 0.2f)
        ),
        radius = w * 0.095f, center = Offset(w * 0.085f, top + w * 0.045f), style = Stroke(w * 0.014f)
    )
    drawCircle(
        Brush.radialGradient(
            listOf(Color.White, Color(0xFFC3C9D4), Color(0xFF5E6573), Color(0xFF20242B)),
            center = Offset(ex - w * 0.015f, ey - w * 0.015f), radius = w * 0.08f
        ),
        radius = w * 0.058f, center = Offset(ex, ey)
    )
    drawCircle(Color(0xFF0B0C10), w * 0.026f, Offset(ex, ey))

    // چراغ وضعیت
    val pulse = if (sig < 1f) 1f - sig else 0f
    val ledCol = if (!connected) Color(0xFFC0392B) else when (locked) {
        true -> Cyan
        false -> Gold
        null -> Color(0xFF3FD07A)
    }
    val lx = w * 0.80f
    val ly = top + w * 0.105f
    drawCircle(
        Brush.radialGradient(
            listOf(ledCol.copy(alpha = 0.45f + 0.4f * pulse), Color.Transparent),
            center = Offset(lx, ly), radius = w * (0.09f + 0.08f * pulse)
        ),
        radius = w * (0.09f + 0.08f * pulse), center = Offset(lx, ly)
    )
    drawCircle(Color(0xFF050608), w * 0.034f, Offset(lx, ly))
    drawCircle(ledCol, w * 0.022f, Offset(lx, ly))
}

@Composable
private fun RemoteKey(
    open: Boolean, active: Boolean, accent: Color, label: String,
    onPress: () -> Unit, modifier: Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }
    val wave = remember { Animatable(1f) }
    val haptic = LocalHapticFeedback.current
    val onP by rememberUpdatedState(onPress)
    LaunchedEffect(tick) {
        if (tick > 0) {
            wave.snapTo(0f)
            wave.animateTo(1f, tween(800, easing = LinearOutSlowInEasing))
        }
    }
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(90), label = "rkPress")
    val act by animateFloatAsState(if (active) 1f else 0f, tween(450), label = "rkAct")
    Box(
        modifier.pointerInput(Unit) {
            detectTapGestures(onPress = {
                pressed = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onP()
                tick++
                try {
                    tryAwaitRelease()
                } finally {
                    pressed = false
                }
            })
        }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val rr = size.width * 0.22f
            // قاب کروم دور دکمه
            drawRoundRect(
                Brush.linearGradient(
                    listOf(Color(0xFFF2F4F8), Color(0xFF5A606C), Color(0xFFCBD0DA), Color(0xFF3A3F49)),
                    start = Offset.Zero, end = Offset(size.width, size.height)
                ),
                cornerRadius = CornerRadius(rr)
            )
            val inset = 3.5.dp.toPx()
            drawRoundRect(
                Color(0xFF050608), Offset(inset, inset),
                Size(size.width - 2f * inset, size.height - 2f * inset), CornerRadius(rr - inset)
            )
            // لاستیک دکمه: لبه‌ی عمق + سطح بالایی که موقع فشردن پایین می‌رود
            val ci = 6.dp.toPx()
            val cw = size.width - 2f * ci
            val ch = size.height - 2f * ci - 3.dp.toPx()
            val capR = CornerRadius(rr - ci)
            drawRoundRect(Color(0xFF07080B), Offset(ci, ci + 3.dp.toPx()), Size(cw, ch), capR)
            val capTop = ci + 3.dp.toPx() * press
            drawRoundRect(
                Brush.verticalGradient(listOf(Color(0xFF30353F), Color(0xFF14171D)), startY = capTop, endY = capTop + ch),
                Offset(ci, capTop), Size(cw, ch), capR
            )
            drawRoundRect(accent.copy(alpha = 0.10f * act + 0.30f * press), Offset(ci, capTop), Size(cw, ch), capR)
            drawRoundRect(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                    startY = capTop, endY = capTop + ch * 0.5f
                ),
                Offset(ci, capTop), Size(cw, ch * 0.5f), CornerRadius(rr - ci)
            )
            drawRoundRect(
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.03f), Color.Black.copy(alpha = 0.7f)),
                    start = Offset(ci, capTop), end = Offset(ci + cw, capTop + ch)
                ),
                Offset(ci, capTop), Size(cw, ch), capR, style = Stroke(1.3.dp.toPx())
            )
            // نور پشت آیکن
            val glow = 0.55f * act + 0.45f * press
            val iconC = Offset(size.width / 2f, capTop + ch * 0.46f)
            if (glow > 0.01f) {
                drawCircle(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.35f * glow), Color.Transparent), center = iconC, radius = size.width * 0.42f),
                    radius = size.width * 0.42f, center = iconC
                )
                drawRoundRect(accent.copy(alpha = glow * 0.8f), Offset(ci, capTop), Size(cw, ch), capR, style = Stroke(1.4.dp.toPx()))
            }
            // موج هنگام فشردن
            val wv = wave.value
            if (wv < 1f) {
                val d = wv * 24.dp.toPx()
                drawRoundRect(
                    accent.copy(alpha = (1f - wv) * 0.75f),
                    topLeft = Offset(-d, -d), size = Size(size.width + 2f * d, size.height + 2f * d),
                    cornerRadius = CornerRadius(rr + d), style = Stroke(2.dp.toPx())
                )
            }
            // آیکن قفل با حالت برجسته
            val s = size.width * 0.34f
            val sw = 2.4.dp.toPx()
            drawPadlock(iconC + Offset(1.2.dp.toPx(), 1.4.dp.toPx()), s, Color.Black.copy(alpha = 0.7f), open, sw)
            drawPadlock(iconC + Offset(-0.6.dp.toPx(), -0.8.dp.toPx()), s, Color.White.copy(alpha = 0.18f), open, sw)
            val ic = lerp(Silver.copy(alpha = 0.92f), accent, (act * 0.7f + press).coerceIn(0f, 1f))
            drawPadlock(iconC, s, ic, open, sw)
        }
        Text(
            label, color = Steel, fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 11.dp)
        )
    }
}

@Composable
fun PeugeotRemote(
    locked: Boolean?,
    connected: Boolean,
    onLock: () -> Unit,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 220.dp
) {
    val S = LocalStrings.current
    val h = width * 2.1f
    val density = LocalDensity.current
    val sig = remember { Animatable(1f) }
    var sigTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(sigTick) {
        if (sigTick > 0) {
            sig.snapTo(0f)
            sig.animateTo(1f, tween(1300, easing = LinearOutSlowInEasing))
        }
    }
    // تیغه‌ی کلید با «باز» فنری بیرون می‌آید و با «قفل» تا می‌شود
    val keyOut by animateFloatAsState(
        if (locked == false) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 70f), label = "keyOut"
    )
    // حرکت آرام و شناور ریموت در هوا
    val inf = rememberInfiniteTransition(label = "rmInf")
    val swayA = inf.animateFloat(
        -1f, 1f, infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "swayA"
    )
    val swayB = inf.animateFloat(
        -1f, 1f, infiniteRepeatable(tween(4700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "swayB"
    )
    Box(
        modifier.size(width, h).graphicsLayer {
            cameraDistance = 14f * density.density
            rotationY = swayA.value * 9f
            rotationX = swayB.value * 4f
            translationY = swayB.value * 5.dp.toPx()
        }
    ) {
        Canvas(Modifier.fillMaxSize()) { drawRemoteBody(locked, connected, swayA.value, keyOut, sig.value) }
        LionLogo(
            width * 0.30f,
            Modifier.align(Alignment.TopCenter).offset(y = h * 0.115f),
            shimmer = { val s = sig.value; if (s < 1f) s else -1f }
        )
        // دکمه‌ی قفل (K)
        RemoteKey(
            open = false, active = locked == true, accent = Cyan, label = S.remoteLock,
            onPress = { sigTick++; onLock() },
            modifier = Modifier.offset(x = width * 0.20f, y = h * 0.30f).size(width * 0.60f, h * 0.215f)
        )
        // دکمه‌ی باز (U)
        RemoteKey(
            open = true, active = locked == false, accent = Gold, label = S.remoteUnlock,
            onPress = { sigTick++; onUnlock() },
            modifier = Modifier.offset(x = width * 0.20f, y = h * 0.53f).size(width * 0.60f, h * 0.215f)
        )
    }
}

// ======================= ظاهر شدن ریموت بعد از اتصال =======================

@Composable
fun RemoteIntro(vm: MainViewModel, onLeaving: () -> Unit, onDone: () -> Unit) {
    val S = LocalStrings.current
    var appear by remember { mutableStateOf(false) }
    var leave by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { delay(200); appear = true }
    val ap by animateFloatAsState(
        if (appear) 1f else 0f, spring(dampingRatio = 0.68f, stiffness = 55f), label = "rAppear"
    )
    val lp by animateFloatAsState(
        if (leave) 1f else 0f, tween(800, easing = FastOutSlowInEasing), label = "rLeave",
        finishedListener = { if (leave && it >= 0.999f) onDone() }
    )

    fun startLeave() {
        if (!leave) {
            leave = true
            onLeaving()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Bg.copy(alpha = 1f - lp))) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val rw = minOf(maxWidth * 0.62f, (maxHeight - 190.dp) / 2.1f, 235.dp)

        Canvas(Modifier.fillMaxSize()) {
            val a = ap.coerceIn(0f, 1f) * (1f - lp)
            drawRect(
                Brush.radialGradient(
                    listOf(Cyan.copy(alpha = 0.20f * a), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.46f), radius = size.width * 0.95f
                )
            )
        }

        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.graphicsLayer {
                    alpha = ap.coerceIn(0f, 1f) * (1f - lp)
                    val sc = (0.55f + 0.45f * ap) * (1f - 0.88f * lp)
                    scaleX = sc
                    scaleY = sc
                    translationY = (1f - ap) * 280.dp.toPx() - lp * hPx * 0.42f
                    translationX = -lp * wPx * 0.34f
                    rotationZ = (1f - ap) * -16f + lp * 28f
                }
            ) {
                PeugeotRemote(
                    locked = vm.locked, connected = vm.connected, width = rw,
                    onLock = { vm.remoteLock() },
                    onUnlock = {
                        vm.remoteUnlock()
                        // بعد از «باز»، ریموت کوچک می‌شود و می‌رود داخل منوی برنامه
                        if (vm.connected) scope.launch { delay(900); startLeave() }
                    }
                )
            }
            Spacer(Modifier.height(18.dp))
            Column(
                Modifier.graphicsLayer { alpha = ap.coerceIn(0f, 1f) * (1f - lp) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(S.lockText(vm.locked), color = Silver, fontSize = 13.sp)
                Text(
                    S.enterMenu, color = Steel, fontSize = 12.sp,
                    modifier = Modifier.clickable { startLeave() }.padding(12.dp)
                )
            }
        }
    }
}

// ======================= ریموت داخل منوی برنامه =======================

@Composable
fun RemoteOverlay(vm: MainViewModel, visible: Boolean, onClose: () -> Unit) {
    val S = LocalStrings.current
    val p by animateFloatAsState(
        if (visible) 1f else 0f, spring(dampingRatio = 0.8f, stiffness = 140f), label = "ov"
    )
    if (!visible && p < 0.01f) return
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = p.coerceIn(0f, 1f) }
            .background(Color(0xEB000000))
            .pointerInput(Unit) { detectTapGestures { onClose() } }
    ) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val rw = minOf(maxWidth * 0.62f, (maxHeight - 190.dp) / 2.1f, 235.dp)
        Column(
            Modifier.align(Alignment.Center)
                .graphicsLayer {
                    val sc = 0.25f + 0.75f * p
                    scaleX = sc
                    scaleY = sc
                    translationX = -(1f - p) * wPx * 0.30f
                    translationY = -(1f - p) * hPx * 0.40f
                }
                .pointerInput(Unit) { detectTapGestures { } },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PeugeotRemote(
                locked = vm.locked, connected = vm.connected, width = rw,
                onLock = { vm.remoteLock() }, onUnlock = { vm.remoteUnlock() }
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (!vm.connected) S.noConnection
                else if (vm.locked == null) S.remoteReady else S.lockText(vm.locked),
                color = if (vm.connected) Silver else Color(0xFFF87171), fontSize = 13.sp
            )
        }
        Box(
            Modifier.align(Alignment.TopStart).padding(16.dp).size(42.dp).clip(CircleShape)
                .background(Panel).clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, null, tint = Color.White)
        }
    }
}
