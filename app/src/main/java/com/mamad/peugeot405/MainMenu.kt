package com.mamad.peugeot405

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val Ink = Color(0xFF07080B)

// ---------- پس‌زمینه‌ی متحرک (نورهای رنگی که آرام جابه‌جا می‌شوند) ----------

@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "amb")
    val a by inf.animateFloat(
        0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "ambA"
    )
    val b by inf.animateFloat(
        0f, 1f, infiniteRepeatable(tween(13000, easing = LinearEasing), RepeatMode.Reverse), label = "ambB"
    )
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(Bg)
        val c1 = Offset(w * (0.9f - 0.6f * a), h * (0.10f + 0.25f * b))
        drawCircle(
            Brush.radialGradient(listOf(Cyan.copy(alpha = 0.20f), Color.Transparent), center = c1, radius = w * 0.95f),
            radius = w * 0.95f, center = c1
        )
        val c2 = Offset(w * (0.05f + 0.7f * b), h * (0.88f - 0.35f * a))
        drawCircle(
            Brush.radialGradient(listOf(Violet.copy(alpha = 0.18f), Color.Transparent), center = c2, radius = w * 0.9f),
            radius = w * 0.9f, center = c2
        )
    }
}

// ---------- ورود تدریجی (هر کپسول با کمی تأخیر از پایین بالا می‌آید) ----------

@Composable
private fun Modifier.staggerIn(delayMs: Int): Modifier {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        shown = true
    }
    val p by animateFloatAsState(
        if (shown) 1f else 0f, spring(dampingRatio = 0.72f, stiffness = 110f), label = "stagger"
    )
    return this.graphicsLayer {
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * 110.dp.toPx()
        val sc = 0.9f + 0.1f * p
        scaleX = sc
        scaleY = sc
    }
}

// ---------- قاب کپسول: گرادیان + نور متحرکی که دور لبه می‌چرخد ----------

@Composable
private fun Modifier.capsuleFrame(accent: Color, top: Color, bottom: Color): Modifier {
    val shape = RoundedCornerShape(36.dp)
    val inf = rememberInfiniteTransition(label = "capsule")
    val sweep by inf.animateFloat(
        0f, 1f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "capSweep"
    )
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, bottom)))
        .border(1.dp, accent.copy(alpha = 0.22f), shape)
        .drawWithContent {
            drawContent()
            val h = size.height
            val y0 = -h * 0.7f + sweep * h * 2.4f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to accent.copy(alpha = 0.10f),
                        0.5f to accent.copy(alpha = 0.95f),
                        1f to accent.copy(alpha = 0.10f)
                    ),
                    startY = y0, endY = y0 + h * 0.7f
                ),
                cornerRadius = CornerRadius(36.dp.toPx()),
                style = Stroke(2.5.dp.toPx())
            )
        }
}

// ---------- دکمه‌ی پایه: فشردن = onDown ، رها کردن = onUp ----------

@Composable
fun PressBox(
    modifier: Modifier,
    accent: Color,
    filled: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(50),
    onDown: () -> Unit = {},
    onUp: () -> Unit = {},
    content: @Composable BoxScope.(Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val down by rememberUpdatedState(onDown)
    val up by rememberUpdatedState(onUp)
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        if (pressed) 0.93f else 1f, spring(dampingRatio = 0.45f, stiffness = 700f), label = "pressScale"
    )
    val glow by animateFloatAsState(if (pressed) 1f else 0f, tween(150), label = "pressGlow")
    val top = when {
        pressed -> 0.60f
        filled -> 0.92f
        else -> 0.16f
    }
    val bottom = when {
        pressed -> 0.32f
        filled -> 0.62f
        else -> 0.04f
    }
    val gloss = if (filled) {
        Brush.verticalGradient(0f to Color.White.copy(alpha = 0.22f), 0.5f to Color.Transparent)
    } else {
        Brush.verticalGradient(0f to Color.White.copy(alpha = 0.05f), 0.5f to Color.Transparent)
    }
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow((18f * glow).dp, shape, clip = false, ambientColor = accent, spotColor = accent)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = top), accent.copy(alpha = bottom))))
            .background(gloss)
            .border(1.dp, accent.copy(alpha = if (pressed) 1f else 0.55f), shape)
            .pointerInput(enabled) {
                detectTapGestures(onPress = {
                    if (enabled) {
                        pressed = true
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
        content(pressed)
    }
}

@Composable
fun Chevrons(up: Boolean, count: Int, tint: Color, size: Dp = 30.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size, size)) {
        val w = this.size.width
        val h = this.size.height
        val gap = h * 0.30f
        val dy = h * 0.17f
        val startY = h / 2f - gap * (count - 1) / 2f
        for (i in 0 until count) {
            val cy = startY + i * gap
            val path = Path().apply {
                if (up) {
                    moveTo(w * 0.18f, cy + dy); lineTo(w * 0.5f, cy - dy); lineTo(w * 0.82f, cy + dy)
                } else {
                    moveTo(w * 0.18f, cy - dy); lineTo(w * 0.5f, cy + dy); lineTo(w * 0.82f, cy - dy)
                }
            }
            drawPath(
                path, tint,
                style = Stroke(width = h * 0.105f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

// ---------- هدر ----------

@Composable
private fun StatusDot(connected: Boolean) {
    val inf = rememberInfiniteTransition(label = "dot")
    val p by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "dotP")
    val col = if (connected) Color(0xFF4ADE80) else Color(0xFFF87171)
    Canvas(Modifier.size(18.dp)) {
        if (connected) drawCircle(col.copy(alpha = (1f - p) * 0.5f), 4.dp.toPx() + p * 5.dp.toPx())
        drawCircle(col, 4.dp.toPx())
    }
}

@Composable
fun Header(vm: MainViewModel, onConnect: () -> Unit, onRemote: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "پژو ۴۰۵",
                    style = TextStyle(
                        brush = Brush.horizontalGradient(listOf(Color.White, Cyan)),
                        fontSize = 28.sp, fontWeight = FontWeight.Black
                    )
                )
                Text("کنترل تعلیق پنوماتیک", fontSize = 12.sp, color = Steel)
            }
            // دکمه‌ی ریموت: ریموت دوباره از اینجا باز می‌شود
            val tint by animateColorAsState(
                when (vm.locked) {
                    true -> Cyan
                    false -> Gold
                    null -> Steel
                },
                tween(450), label = "lockTint"
            )
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(Panel.copy(alpha = 0.8f))
                    .border(1.dp, tint.copy(alpha = 0.7f), CircleShape)
                    .clickable { onRemote() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(22.dp)) {
                    drawPadlock(Offset(size.width / 2f, size.height / 2f), size.width * 0.62f, tint, vm.locked == false, 1.8.dp.toPx())
                }
            }
            Spacer(Modifier.width(10.dp))
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Panel.copy(alpha = 0.8f))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(50))
                    .clickable { onConnect() }.padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusDot(vm.connected)
                Spacer(Modifier.width(4.dp))
                Text(if (vm.connected) "متصل" else "اتصال", fontSize = 13.sp, color = Color.White)
            }
        }
        Text(vm.status, fontSize = 11.sp, color = Color(0xFF6B7385), modifier = Modifier.padding(top = 4.dp))
    }
}

// ---------- صفحه‌ی کنترل ----------

@Composable
fun ControlScreen(vm: MainViewModel) {
    val cmds = vm.commands
    fun find(a: String): Cmd? = cmds.firstOrNull { it.action == a }
    val groups = cmds.filter { it.action == "NONE" }.groupBy { it.group }
    // درصد فقط وقتی عدد صحیحش عوض شود باعث ترکیب مجدد می‌شود
    val pctF by remember { derivedStateOf { (((vm.sim.dispF - 80f) / 32f) * 100f).roundToInt().coerceIn(0, 100) } }
    val pctB by remember { derivedStateOf { (((vm.sim.dispB - 84f) / 31f) * 100f).roundToInt().coerceIn(0, 100) } }
    // عکس ماشین حذف شد؛ کپسول‌ها کل ارتفاع صفحه را می‌گیرند
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val rowH = (maxHeight - 32.dp).coerceAtLeast(440.dp)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                // چیدمان راست به چپ: جلو (سمت راست)، عقب، و ستون «هر دو» سمت چپ
                Row(
                    Modifier.fillMaxWidth().height(rowH),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AxleCapsule(
                        "جلو", "FRONT", Cyan,
                        find("F_UP_S"), find("F_UP_F"), find("F_DN_S"), find("F_DN_F"),
                        { (vm.sim.dispF - 80f) / 32f }, pctF,
                        vm::press, vm::press, vm::pressUp,
                        Modifier.weight(1f).fillMaxHeight().staggerIn(0)
                    )
                    AxleCapsule(
                        "عقب", "REAR", Violet,
                        find("R_UP_S"), find("R_UP_F"), find("R_DN_S"), find("R_DN_F"),
                        { (vm.sim.dispB - 84f) / 31f }, pctB,
                        vm::press, vm::press, vm::pressUp,
                        Modifier.weight(1f).fillMaxHeight().staggerIn(110)
                    )
                    AllCapsule(
                        find("BOTH_UP"), find("BOTH_DN"), find("FACTORY"),
                        find("M_ON"), find("M_OFF"), vm.sim.maybachOn,
                        vm::press, vm::pressUp, vm::factory,
                        Modifier.weight(0.9f).fillMaxHeight().staggerIn(220)
                    )
                }
            }
            groups.forEach { (name, list) ->
                item(key = "group_$name") { GroupCard(name, list, vm::press) }
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
    ) { pressed ->
        val fg = if (fast) Ink else accent
        val nudge by animateFloatAsState(
            if (pressed) (if (up) -5f else 5f) else 0f,
            spring(dampingRatio = 0.4f, stiffness = 500f), label = "nudge"
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Chevrons(
                up, if (fast) 2 else 1, fg, 24.dp,
                Modifier.graphicsLayer { translationY = nudge.dp.toPx() }
            )
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
    val inf = rememberInfiniteTransition(label = "level")
    val pulse by inf.animateFloat(
        0.4f, 1f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "lvPulse"
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val trackW = 12.dp.toPx()
            val x = size.width / 2f - trackW / 2f
            val r = CornerRadius(trackW / 2f)
            drawRoundRect(accent.copy(alpha = 0.14f), Offset(x, 0f), Size(trackW, size.height), r)
            val fh = size.height * level().coerceIn(0f, 1f)
            if (fh > 0f) {
                val topY = size.height - fh
                drawRoundRect(
                    Brush.verticalGradient(
                        listOf(accent, accent.copy(alpha = 0.40f)),
                        startY = topY, endY = size.height
                    ),
                    Offset(x, topY), Size(trackW, fh), r
                )
                // سر نوار: هاله‌ی نورانی که نفس می‌کشد
                val head = Offset(size.width / 2f, topY.coerceIn(6.dp.toPx(), size.height - 6.dp.toPx()))
                drawCircle(accent.copy(alpha = 0.30f * pulse), 15.dp.toPx(), head)
                drawCircle(accent.copy(alpha = 0.55f), 8.dp.toPx(), head)
                drawCircle(Color.White, 3.5.dp.toPx(), head)
            }
            for (i in 0..4) {
                val y = size.height * i / 4f
                drawLine(accent.copy(alpha = 0.28f), Offset(x - 11.dp.toPx(), y), Offset(x - 4.dp.toPx(), y), 1.dp.toPx())
                drawLine(accent.copy(alpha = 0.28f), Offset(x + trackW + 4.dp.toPx(), y), Offset(x + trackW + 11.dp.toPx(), y), 1.dp.toPx())
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier.capsuleFrame(accent, Color(0xE6161A22), Color(0xE60A0B0F))
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

// ---------- ستون «هر دو»: بالا/پایین همزمان (W/D)، میباخ، فابریک ----------

@Composable
fun AllCapsule(
    bothUp: Cmd?, bothDown: Cmd?, factory: Cmd?, mOn: Cmd?, mOff: Cmd?, maybachOn: Boolean,
    onPress: (Cmd) -> Unit, onHoldEnd: () -> Unit, onFactory: (Cmd) -> Unit,
    modifier: Modifier = Modifier
) {
    val inf = rememberInfiniteTransition(label = "mInf")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "mSpin")
    val pulse by inf.animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "mPulse"
    )
    val on by animateFloatAsState(if (maybachOn) 1f else 0f, tween(500), label = "mOn")

    Column(
        modifier.capsuleFrame(Silver, Color(0xE6181B21), Color(0xE60A0B0F))
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("هر دو", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            Text("SYNC", fontSize = 9.sp, letterSpacing = 2.sp, color = Silver.copy(alpha = 0.7f))
        }
        // هر دو بالا = W (تا وقتی دست روی دکمه است حرکت می‌کند، رها کنی می‌ایستد)
        PressBox(
            Modifier.fillMaxWidth().height(70.dp), Silver, filled = true, enabled = bothUp != null,
            onDown = { bothUp?.let(onPress) },
            onUp = onHoldEnd
        ) { pressed ->
            val nudge by animateFloatAsState(
                if (pressed) -5f else 0f, spring(dampingRatio = 0.4f, stiffness = 500f), label = "bothUpNudge"
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Chevrons(true, 2, Ink, 28.dp, Modifier.graphicsLayer { translationY = nudge.dp.toPx() })
                Text(bothUp?.label ?: "—", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
            }
        }
        // دکمه‌ی M (میباخ): شروع، و وقتی فعال است توقف؛ هنگام میباخ حلقه‌ی طلایی می‌چرخد
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    if (on > 0.01f) {
                        drawCircle(
                            Brush.radialGradient(
                                listOf(Gold.copy(alpha = 0.40f * on * pulse), Color.Transparent),
                                radius = size.minDimension / 2f
                            ),
                            radius = size.minDimension / 2f
                        )
                        rotate(spin) {
                            drawArc(
                                brush = Brush.sweepGradient(listOf(Color.Transparent, Gold)),
                                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                                topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                                size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
                                alpha = on,
                                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }
                PressBox(
                    Modifier.size(70.dp), Gold, filled = maybachOn, shape = CircleShape,
                    enabled = mOn != null,
                    onDown = { if (maybachOn) mOff?.let(onPress) else mOn?.let(onPress) }
                ) { _ ->
                    Text("M", color = if (maybachOn) Ink else Gold, fontWeight = FontWeight.Black, fontSize = 28.sp)
                }
            }
            Text(
                if (maybachOn) "توقف" else "میباخ", fontSize = 10.sp,
                color = Gold.copy(alpha = 0.85f), modifier = Modifier.padding(top = 1.dp)
            )
        }
        // هر دو پایین = D
        PressBox(
            Modifier.fillMaxWidth().height(70.dp), Silver, filled = true, enabled = bothDown != null,
            onDown = { bothDown?.let(onPress) },
            onUp = onHoldEnd
        ) { pressed ->
            val nudge by animateFloatAsState(
                if (pressed) 5f else 0f, spring(dampingRatio = 0.4f, stiffness = 500f), label = "bothDnNudge"
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Chevrons(false, 2, Ink, 28.dp, Modifier.graphicsLayer { translationY = nudge.dp.toPx() })
                Text(bothDown?.label ?: "—", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
            }
        }
        PressBox(
            Modifier.fillMaxWidth().height(44.dp), Silver, enabled = factory != null,
            onDown = { factory?.let(onFactory) }
        ) { _ ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Home, null, tint = Silver, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("فابریک", color = Silver, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
            }
        }
    }
}
