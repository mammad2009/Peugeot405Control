package com.mamad.peugeot405

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

private val Ink = Color(0xFF07080B)

// ---------- ورود نرم و پله‌ای (از پایین میاد بالا و کم‌کم پررنگ میشه) ----------

@Composable
fun Modifier.staggerIn(index: Int): Modifier {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(120L + index * 120L)
        p.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 110f))
    }
    return this.graphicsLayer {
        val v = p.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * 90.dp.toPx()
        val s = 0.90f + 0.10f * v
        scaleX = s
        scaleY = s
    }
}

// ---------- دکمه‌ی پایه: فشردن = onDown ، رها کردن = onUp ----------
// content دو مقدار می‌گیرد: pressed (الان لمس شده؟) و bob (۰ تا ۱ تکرارشونده وقتی نگه داشته‌ای)

@Composable
fun PressBox(
    modifier: Modifier,
    accent: Color,
    filled: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(50),
    onDown: () -> Unit = {},
    onUp: () -> Unit = {},
    content: @Composable BoxScope.(Boolean, Float) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(0) }
    val wave = remember { Animatable(1f) }
    val bob = remember { Animatable(0f) }
    val down by rememberUpdatedState(onDown)
    val up by rememberUpdatedState(onUp)
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(tick) {
        if (tick > 0) {
            wave.snapTo(0f)
            wave.animateTo(1f, tween(700, easing = LinearOutSlowInEasing))
        }
    }
    LaunchedEffect(pressed) {
        if (pressed) {
            while (true) {
                bob.snapTo(0f)
                bob.animateTo(1f, tween(650, easing = LinearEasing))
            }
        } else {
            bob.snapTo(0f)
        }
    }

    val pa by animateFloatAsState(if (pressed) 1f else 0f, tween(120), label = "pressAmt")
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, tween(90), label = "pressScale")
    val baseTop = if (filled) 0.92f else 0.16f
    val baseBottom = if (filled) 0.62f else 0.04f
    val top = lerp(baseTop, 0.55f, pa)
    val bottom = lerp(baseBottom, 0.30f, pa)

    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = top), accent.copy(alpha = bottom))))
            .drawWithContent {
                drawContent()
                val w = wave.value
                if (w < 1f) {
                    drawCircle(accent.copy(alpha = (1f - w) * 0.38f), size.maxDimension * w, center)
                }
            }
            .border(1.dp, accent.copy(alpha = lerp(0.55f, 1f, pa)), shape)
            .pointerInput(enabled) {
                detectTapGestures(onPress = {
                    if (enabled) {
                        pressed = true
                        tick++
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        down()
                        try {
                            tryAwaitRelease()
                        } finally {
                            pressed = false
                            up()
                        }
                    }
                })
            },
        contentAlignment = Alignment.Center
    ) {
        content(pressed, bob.value)
    }
}

// فلش‌های چندتایی. anim بین ۰ و ۱: وقتی دکمه نگه داشته میشه فلش‌ها پشت سر هم موج می‌زنند
@Composable
fun Chevrons(up: Boolean, count: Int, tint: Color, size: Dp = 30.dp, anim: Float = 0f) {
    Canvas(Modifier.size(size, size)) {
        val w = this.size.width
        val h = this.size.height
        val gap = h * 0.30f
        val dy = h * 0.17f
        val startY = h / 2f - gap * (count - 1) / 2f
        val moving = anim > 0f
        for (i in 0 until count) {
            // فلش بیرونی‌تر زودتر روشن میشه تا حس حرکت به سمت جهت دکمه بیاد
            val order = if (up) i else (count - 1 - i)
            val wv = if (moving) sin(((anim + order * 0.22f) % 1f) * PI.toFloat()) else 1f
            val shift = if (moving) (if (up) -1f else 1f) * wv * h * 0.10f else 0f
            val cy = startY + i * gap + shift
            val path = Path().apply {
                if (up) {
                    moveTo(w * 0.18f, cy + dy); lineTo(w * 0.5f, cy - dy); lineTo(w * 0.82f, cy + dy)
                } else {
                    moveTo(w * 0.18f, cy - dy); lineTo(w * 0.5f, cy + dy); lineTo(w * 0.82f, cy - dy)
                }
            }
            drawPath(
                path, tint.copy(alpha = if (moving) 0.40f + 0.60f * wv else 1f),
                style = Stroke(width = h * 0.105f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

// ---------- هدر ----------

@Composable
fun Header(vm: MainViewModel, onConnect: () -> Unit, onRemote: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "hdr")
    val pulse by inf.animateFloat(
        0.35f, 1f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("پژو ۴۰۵", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color.White)
                Text("کنترل تعلیق پنوماتیک", fontSize = 12.sp, color = Steel)
            }
            // دکمه‌ی ریموت: ریموت دوباره از اینجا باز می‌شود
            val tint = when (vm.locked) {
                true -> Cyan
                false -> Gold
                null -> Steel
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Panel)
                    .border(1.dp, tint.copy(alpha = 0.6f), CircleShape)
                    .clickable { onRemote() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(22.dp)) {
                    drawPadlock(Offset(size.width / 2f, size.height / 2f), size.width * 0.62f, tint, vm.locked == false, 1.8.dp.toPx())
                }
            }
            Spacer(Modifier.width(10.dp))
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Panel)
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(50))
                    .clickable { onConnect() }.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(8.dp).graphicsLayer { alpha = if (vm.connected) pulse else 1f }
                        .clip(CircleShape)
                        .background(if (vm.connected) Color(0xFF4ADE80) else Color(0xFFF87171))
                )
                Spacer(Modifier.width(8.dp))
                Text(if (vm.connected) "متصل" else "اتصال", fontSize = 13.sp, color = Color.White)
            }
        }
        val statusText = when {
            vm.unlocking -> "ماکت در حال باز شدن…"
            vm.locked == true -> "ماکت قفل است"
            else -> vm.status
        }
        val statusColor = when {
            vm.unlocking -> Gold
            vm.locked == true -> Cyan
            else -> Color(0xFF6B7385)
        }
        Text(
            statusText, fontSize = 11.sp, color = statusColor,
            modifier = Modifier.padding(top = 4.dp).graphicsLayer { alpha = if (vm.unlocking) pulse else 1f }
        )
    }
}

// ---------- پس‌زمینه‌ی زنده: دو هاله‌ی نور که آروم جابه‌جا میشن (طلایی وقتی میباخ روشنه) ----------

@Composable
private fun AuroraBackground(maybach: Boolean) {
    val inf = rememberInfiniteTransition(label = "aurora")
    val a by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "a")
    val gold by animateFloatAsState(if (maybach) 1f else 0f, tween(900), label = "gold")
    Canvas(Modifier.fillMaxSize()) {
        val ph = a * 2f * PI.toFloat()
        val r = size.maxDimension * 0.75f
        val c1 = Offset(size.width * (0.88f + 0.10f * sin(ph)), size.height * (0.10f + 0.06f * cos(ph)))
        val c2 = Offset(size.width * (0.10f + 0.10f * cos(ph)), size.height * (0.92f + 0.05f * sin(ph)))
        drawRect(Brush.radialGradient(listOf(Cyan.copy(alpha = 0.16f), Color.Transparent), center = c1, radius = r))
        drawRect(Brush.radialGradient(listOf(Violet.copy(alpha = 0.13f), Color.Transparent), center = c2, radius = r))
        if (gold > 0.01f) {
            drawRect(
                Brush.radialGradient(
                    listOf(Gold.copy(alpha = 0.14f * gold), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.5f), radius = r
                )
            )
        }
    }
}

// ---------- صفحه‌ی کنترل (فقط کنترل ، بدون عکس ماشین) ----------

@Composable
fun ControlScreen(vm: MainViewModel) {
    val cmds = vm.commands
    fun find(a: String): Cmd? = cmds.firstOrNull { it.action == a }
    val groups = cmds.filter { it.action == "NONE" }.groupBy { it.group }
    // درصد فقط وقتی عدد صحیحش عوض شود باعث ترکیب مجدد می‌شود
    val pctF by remember { derivedStateOf { (((vm.sim.dispF - 80f) / 32f) * 100f).roundToInt().coerceIn(0, 100) } }
    val pctB by remember { derivedStateOf { (((vm.sim.dispB - 84f) / 31f) * 100f).roundToInt().coerceIn(0, 100) } }
    // نفس کشیدنِ نور لبه‌ی کپسول‌ها
    val breathe by rememberInfiniteTransition(label = "breathe").animateFloat(
        0.55f, 1f,
        infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b"
    )

    Box(Modifier.fillMaxSize()) {
        AuroraBackground(vm.sim.maybachOn)
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val panelH = (maxHeight - 32.dp).coerceIn(400.dp, 580.dp)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    // چیدمان راست به چپ: جلو (سمت راست)، عقب، و ستون «هر دو» سمت چپ
                    Row(
                        Modifier.fillMaxWidth().height(panelH),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AxleCapsule(
                            "جلو", "FRONT", Cyan,
                            find("F_UP_S"), find("F_UP_F"), find("F_DN_S"), find("F_DN_F"),
                            { (vm.sim.dispF - 80f) / 32f }, pctF,
                            vm::press, vm::press, vm::pressUp,
                            { breathe },
                            Modifier.weight(1f).fillMaxHeight().staggerIn(0)
                        )
                        AxleCapsule(
                            "عقب", "REAR", Violet,
                            find("R_UP_S"), find("R_UP_F"), find("R_DN_S"), find("R_DN_F"),
                            { (vm.sim.dispB - 84f) / 31f }, pctB,
                            vm::press, vm::press, vm::pressUp,
                            { breathe },
                            Modifier.weight(1f).fillMaxHeight().staggerIn(1)
                        )
                        AllCapsule(
                            find("BOTH_UP"), find("BOTH_DN"), find("FACTORY"),
                            find("M_ON"), find("M_OFF"), vm.sim.maybachOn,
                            vm::press, vm::pressUp, vm::factory, vm::press,
                            { breathe },
                            Modifier.weight(0.9f).fillMaxHeight().staggerIn(2)
                        )
                    }
                }
                groups.forEach { (name, list) ->
                    item(key = "group_$name") { GroupCard(name, list, vm::press) }
                }
            }
        }
    }
}

// ---------- کپسول یک محور (جلو یا عقب) ----------

@Composable
private fun AxleButton(
    cmd: Cmd?, accent: Color, up: Boolean, fast: Boolean,
    onPress: (Cmd) -> Unit, onHoldStart: (Cmd) -> Unit, onHoldEnd: () -> Unit,
    modifier: Modifier
) {
    PressBox(
        modifier = modifier, accent = accent, filled = fast, enabled = cmd != null,
        // سریع: همان لحظه‌ی لمس یک‌بار فرمان می‌رود؛ آرام: تا وقتی دست روی دکمه است حرکت می‌کند
        onDown = { cmd?.let { if (fast) onPress(it) else onHoldStart(it) } },
        onUp = { if (!fast) onHoldEnd() }
    ) { _, bob ->
        val fg = if (fast) Ink else accent
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Chevrons(up, if (fast) 2 else 1, fg, 24.dp, bob)
            Spacer(Modifier.width(3.dp))
            Text(
                cmd?.label ?: "—", color = fg, fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false
            )
        }
    }
}

// نوار عمودی ارتفاع فعلی + درصد (نوار فقط دوباره‌کشی می‌شود، نه ترکیب مجدد)
@Composable
private fun VerticalLevel(level: () -> Float, percent: Int, accent: Color, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val trackW = 10.dp.toPx()
            val x = size.width / 2f - trackW / 2f
            val r = CornerRadius(trackW / 2f)
            drawRoundRect(accent.copy(alpha = 0.14f), Offset(x, 0f), Size(trackW, size.height), r)
            val fh = size.height * level().coerceIn(0f, 1f)
            if (fh > 0f) {
                drawRoundRect(
                    Brush.verticalGradient(
                        listOf(accent, accent.copy(alpha = 0.40f)),
                        startY = size.height - fh, endY = size.height
                    ),
                    Offset(x, size.height - fh), Size(trackW, fh), r
                )
                // نقطه‌ی درخشان بالای نوار
                val tip = Offset(size.width / 2f, size.height - fh)
                drawCircle(
                    brush = Brush.radialGradient(listOf(accent.copy(alpha = 0.65f), Color.Transparent), center = tip, radius = 16.dp.toPx()),
                    radius = 16.dp.toPx(), center = tip
                )
                drawCircle(Color.White, 3.dp.toPx(), tip)
            }
            for (i in 0..4) {
                val y = size.height * i / 4f
                drawLine(accent.copy(alpha = 0.28f), Offset(x - 9.dp.toPx(), y), Offset(x - 3.dp.toPx(), y), 1.dp.toPx())
                drawLine(accent.copy(alpha = 0.28f), Offset(x + trackW + 3.dp.toPx(), y), Offset(x + trackW + 9.dp.toPx(), y), 1.dp.toPx())
            }
        }
        Text(
            "$percent%", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Bg.copy(alpha = 0.8f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun AxleCapsule(
    title: String, tag: String, accent: Color,
    upSlow: Cmd?, upFast: Cmd?, downSlow: Cmd?, downFast: Cmd?,
    level: () -> Float, percent: Int,
    onPress: (Cmd) -> Unit, onHoldStart: (Cmd) -> Unit, onHoldEnd: () -> Unit,
    glow: () -> Float,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(36.dp)
    Column(
        modifier.clip(shape)
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.10f), Color(0xFF0A0B0E), Color(0xFF08090C))))
            .drawBehind {
                val g = glow()
                drawRoundRect(
                    Brush.verticalGradient(listOf(accent.copy(alpha = 0.85f * g), accent.copy(alpha = 0.10f))),
                    cornerRadius = CornerRadius(36.dp.toPx()), style = Stroke(1.3.dp.toPx())
                )
            }
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        Text(tag, fontSize = 9.sp, letterSpacing = 2.sp, color = accent.copy(alpha = 0.75f))
        val bm = Modifier.fillMaxWidth().height(50.dp)
        AxleButton(upFast, accent, true, true, onPress, onHoldStart, onHoldEnd, bm)
        AxleButton(upSlow, accent, true, false, onPress, onHoldStart, onHoldEnd, bm)
        VerticalLevel(level, percent, accent, Modifier.weight(1f).fillMaxWidth())
        AxleButton(downSlow, accent, false, false, onPress, onHoldStart, onHoldEnd, bm)
        AxleButton(downFast, accent, false, true, onPress, onHoldStart, onHoldEnd, bm)
    }
}

// ---------- ستون «هر دو»: بالا/پایین همزمان (W و D) ، میباخ ، فابریک ----------

@Composable
fun AllCapsule(
    bothUp: Cmd?, bothDown: Cmd?, factory: Cmd?, mOn: Cmd?, mOff: Cmd?, maybachOn: Boolean,
    onHoldStart: (Cmd) -> Unit, onHoldEnd: () -> Unit, onFactory: (Cmd) -> Unit, onPress: (Cmd) -> Unit,
    glow: () -> Float,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(36.dp)
    val inf = rememberInfiniteTransition(label = "mPulse")
    val ring by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearOutSlowInEasing)), label = "ring")
    Column(
        modifier.clip(shape)
            .background(Brush.verticalGradient(listOf(Silver.copy(alpha = 0.07f), Color(0xFF0A0B0E), Color(0xFF08090C))))
            .drawBehind {
                val g = glow()
                drawRoundRect(
                    Brush.verticalGradient(listOf(Silver.copy(alpha = 0.7f * g), Silver.copy(alpha = 0.08f))),
                    cornerRadius = CornerRadius(36.dp.toPx()), style = Stroke(1.3.dp.toPx())
                )
            }
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("هر دو", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            Text("SYNC", fontSize = 9.sp, letterSpacing = 2.sp, color = Silver.copy(alpha = 0.7f))
        }
        PressBox(
            Modifier.fillMaxWidth().height(70.dp), Silver, filled = true, enabled = bothUp != null,
            onDown = { bothUp?.let(onHoldStart) }, onUp = onHoldEnd
        ) { _, bob ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Chevrons(true, 2, Ink, 28.dp, bob)
                Text(bothUp?.label ?: "—", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
            }
        }
        // دکمه‌ی M (میباخ): شروع ، و وقتی فعال است توقف (با حلقه‌ی نور تپنده)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    if (maybachOn) {
                        drawCircle(
                            Gold.copy(alpha = (1f - ring) * 0.6f),
                            35.dp.toPx() + ring * 9.dp.toPx(), center, style = Stroke(2.dp.toPx())
                        )
                    }
                }
                PressBox(
                    Modifier.size(70.dp), Gold, filled = maybachOn, shape = CircleShape,
                    enabled = mOn != null,
                    onDown = { if (maybachOn) mOff?.let(onPress) else mOn?.let(onPress) }
                ) { _, _ ->
                    Text("M", color = if (maybachOn) Ink else Gold, fontWeight = FontWeight.Black, fontSize = 28.sp)
                }
            }
            Text(
                if (maybachOn) "توقف" else "میباخ", fontSize = 10.sp,
                color = Gold.copy(alpha = 0.85f), modifier = Modifier.padding(top = 1.dp)
            )
        }
        PressBox(
            Modifier.fillMaxWidth().height(70.dp), Silver, filled = true, enabled = bothDown != null,
            onDown = { bothDown?.let(onHoldStart) }, onUp = onHoldEnd
        ) { _, bob ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Chevrons(false, 2, Ink, 28.dp, bob)
                Text(bothDown?.label ?: "—", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
            }
        }
        PressBox(
            Modifier.fillMaxWidth().height(44.dp), Silver, enabled = factory != null,
            onDown = { factory?.let(onFactory) }
        ) { _, _ ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Home, null, tint = Silver, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("فابریک", color = Silver, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
            }
        }
    }
}
