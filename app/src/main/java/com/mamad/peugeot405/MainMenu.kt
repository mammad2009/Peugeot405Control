package com.mamad.peugeot405

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private val Ink = Color(0xFF07080B)

// ---------- دکمه‌ی پایه: فشردن = onDown ، رها کردن = onUp (با فنر و موج نوری) ----------

@Composable
fun PressBox(
    modifier: Modifier,
    accent: Color,
    filled: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(50),
    onDown: () -> Unit = {},
    onUp: () -> Unit = {},
    onTap: () -> Unit = {},
    content: @Composable BoxScope.(Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }
    val wave = remember { Animatable(1f) }
    val down by rememberUpdatedState(onDown)
    val up by rememberUpdatedState(onUp)
    val tap by rememberUpdatedState(onTap)
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        if (pressed) 0.93f else 1f, spring(dampingRatio = 0.45f, stiffness = 500f), label = "pressScale"
    )
    LaunchedEffect(tick) {
        if (tick > 0) {
            wave.snapTo(0f)
            wave.animateTo(1f, tween(650, easing = LinearOutSlowInEasing))
        }
    }
    val top = when {
        pressed -> 0.55f
        filled -> 0.92f
        else -> 0.16f
    }
    val bottom = when {
        pressed -> 0.30f
        filled -> 0.62f
        else -> 0.04f
    }
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .drawBehind {
                val wv = wave.value
                if (wv < 1f) {
                    val d = wv * 14.dp.toPx()
                    drawRoundRect(
                        accent.copy(alpha = (1f - wv) * 0.7f), Offset(-d, -d),
                        Size(size.width + 2f * d, size.height + 2f * d),
                        CornerRadius(size.minDimension / 2f + d), style = Stroke(2.dp.toPx())
                    )
                }
            }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = top), accent.copy(alpha = bottom))))
            .border(1.dp, accent.copy(alpha = if (pressed) 1f else 0.55f), shape)
            .pointerInput(enabled) {
                detectTapGestures(onPress = {
                    if (enabled) {
                        pressed = true
                        tick++
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        down()
                        var released = false
                        try {
                            released = tryAwaitRelease()
                        } finally {
                            pressed = false
                            up()
                        }
                        // کلیک فقط وقتی انگشت واقعاً روی دکمه رها شده (نه وقتی اسکرول کرده)
                        if (released) tap()
                    }
                })
            },
        contentAlignment = Alignment.Center
    ) {
        content(pressed)
    }
}

// فلش‌های جهت؛ هنگام فشردن موج نور از فلش‌ها عبور می‌کند
@Composable
fun Chevrons(up: Boolean, count: Int, tint: Color, size: Dp = 30.dp, flow: () -> Float = { 0f }) {
    Canvas(Modifier.size(size, size)) {
        val f = flow()
        val w = this.size.width
        val h = this.size.height
        val gap = h * 0.30f
        val dy = h * 0.17f
        val startY = h / 2f - gap * (count - 1) / 2f
        for (i in 0 until count) {
            val cy = startY + i * gap
            val order = if (up) count - 1 - i else i
            val a = if (f <= 0f) 1f else {
                val p = ((f - order * 0.22f) + 1f) % 1f
                0.30f + 0.70f * sin(p * PI.toFloat())
            }
            val path = Path().apply {
                if (up) {
                    moveTo(w * 0.18f, cy + dy); lineTo(w * 0.5f, cy - dy); lineTo(w * 0.82f, cy + dy)
                } else {
                    moveTo(w * 0.18f, cy - dy); lineTo(w * 0.5f, cy + dy); lineTo(w * 0.82f, cy - dy)
                }
            }
            drawPath(
                path, tint.copy(alpha = tint.alpha * a),
                style = Stroke(width = h * 0.105f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

// ---------- هدر ----------

@Composable
fun Header(vm: MainViewModel, onConnect: () -> Unit, onRemote: () -> Unit) {
    val S = LocalStrings.current
    val inf = rememberInfiniteTransition(label = "hdr")
    val blink = inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "hdrBlink")
    Column(Modifier.fillMaxWidth().reveal(0, (-28).dp).padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LionLogo(34.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    S.appTitle, fontWeight = FontWeight.Black, color = Color.White,
                    fontSize = if (S.fa) 24.sp else 21.sp, letterSpacing = if (S.fa) 0.sp else 2.sp
                )
                Text(
                    S.appSubtitle, color = Steel,
                    fontSize = if (S.fa) 12.sp else 9.sp, letterSpacing = if (S.fa) 0.sp else 3.sp
                )
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
                    Modifier.size(8.dp).graphicsLayer { alpha = if (vm.connecting) blink.value else 1f }
                        .clip(CircleShape).background(if (vm.connected) Color(0xFF4ADE80) else Color(0xFFF87171))
                )
                Spacer(Modifier.width(8.dp))
                Text(if (vm.connected) S.headerConnected else S.headerConnect, fontSize = 13.sp, color = Color.White)
            }
        }
        Text(vm.status, fontSize = 11.sp, color = Color(0xFF6B7385), modifier = Modifier.padding(top = 4.dp))
    }
}

// ---------- صفحه‌ی کنترل ----------

@Composable
fun ControlScreen(vm: MainViewModel) {
    val S = LocalStrings.current
    val cmds = vm.commands
    fun find(a: String): Cmd? = cmds.firstOrNull { it.action == a }
    val groups = cmds.filter { it.action == "NONE" }.groupBy { it.group }
    // درصد فقط وقتی عدد صحیحش عوض شود باعث ترکیب مجدد می‌شود
    val pctF by remember { derivedStateOf { (((vm.sim.dispF - 80f) / 32f) * 100f).roundToInt().coerceIn(0, 100) } }
    val pctB by remember { derivedStateOf { (((vm.sim.dispB - 84f) / 31f) * 100f).roundToInt().coerceIn(0, 100) } }
    Column(Modifier.fillMaxSize()) {
        EmblemStage({ vm.sim.dispF }, { vm.sim.dispB }, vm.sim.maybachOn, Modifier.reveal(0, 20.dp))
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                // چیدمان: جلو (سمت شروع زبان)، عقب، و ستون «هر دو»
                Row(
                    Modifier.fillMaxWidth().height(392.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AxleCapsule(
                        S.front, "FRONT", Cyan,
                        find("F_UP_S"), find("F_UP_F"), find("F_DN_S"), find("F_DN_F"),
                        { (vm.sim.dispF - 80f) / 32f }, pctF,
                        vm::press, vm::press, vm::pressUp,
                        Modifier.weight(1f).fillMaxHeight().reveal(60)
                    )
                    AxleCapsule(
                        S.rear, "REAR", Violet,
                        find("R_UP_S"), find("R_UP_F"), find("R_DN_S"), find("R_DN_F"),
                        { (vm.sim.dispB - 84f) / 31f }, pctB,
                        vm::press, vm::press, vm::pressUp,
                        Modifier.weight(1f).fillMaxHeight().reveal(170)
                    )
                    AllCapsule(
                        find("BOTH_UP"), find("BOTH_DN"), find("FACTORY"),
                        find("M_ON"), find("M_OFF"), vm.sim.maybachOn,
                        vm::pressMulti, vm::factory, vm::press,
                        Modifier.weight(0.9f).fillMaxHeight().reveal(280)
                    )
                }
            }
            groups.entries.forEachIndexed { idx, (name, list) ->
                item(key = "group_$name") {
                    Box(Modifier.reveal(380 + idx * 90)) { GroupCard(name, list, vm::press) }
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
    val S = LocalStrings.current
    val flow = remember { Animatable(0f) }
    PressBox(
        modifier = modifier, accent = accent, filled = fast, enabled = cmd != null,
        // سریع: همان لحظه‌ی لمس یک‌بار فرمان می‌رود؛ آرام: تا وقتی دست روی دکمه است حرکت می‌کند
        onDown = { cmd?.let { if (fast) onPress(it) else onHoldStart(it) } },
        onUp = { if (!fast) onHoldEnd() }
    ) { pressed ->
        LaunchedEffect(pressed) {
            if (pressed) {
                while (true) {
                    flow.snapTo(0f)
                    flow.animateTo(1f, tween(700, easing = LinearEasing))
                }
            } else {
                flow.snapTo(0f)
            }
        }
        val fg = if (fast) Ink else accent
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Chevrons(up, if (fast) 2 else 1, fg, 24.dp, flow = { flow.value })
            Spacer(Modifier.width(3.dp))
            Text(
                cmd?.let { S.cmdLabel(it) } ?: "—", color = fg, fontSize = 10.5.sp,
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
                // درخشش سر نوار
                val tip = Offset(size.width / 2f, size.height - fh)
                drawCircle(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.9f), Color.Transparent), center = tip, radius = 16.dp.toPx()),
                    radius = 16.dp.toPx(), center = tip
                )
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
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(36.dp)
    Column(
        modifier.clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF15181E), Color(0xFF08090C))))
            .border(1.dp, Brush.verticalGradient(listOf(accent.copy(alpha = 0.75f), accent.copy(alpha = 0.10f))), shape)
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

// حلقه‌های طلایی که دور دکمه‌ی M می‌تپند (فقط وقتی میباخ روشن است)
@Composable
private fun MaybachPulse(modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "mPulse")
    val p = inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1700, easing = LinearOutSlowInEasing)), label = "mP")
    Canvas(modifier) {
        for (k in 0..1) {
            val q = (p.value + k * 0.5f) % 1f
            drawCircle(
                Gold.copy(alpha = (1f - q) * 0.6f), size.minDimension / 2f * (0.55f + 0.45f * q), center,
                style = Stroke(2.dp.toPx())
            )
        }
    }
}

// ---------- ستون «هر دو»: بالا/پایین همزمان، میباخ، فابریک ----------

@Composable
fun AllCapsule(
    bothUp: Cmd?, bothDown: Cmd?, factory: Cmd?, mOn: Cmd?, mOff: Cmd?, maybachOn: Boolean,
    onMulti: (Cmd) -> Unit, onFactory: (Cmd) -> Unit, onPress: (Cmd) -> Unit,
    modifier: Modifier = Modifier
) {
    val S = LocalStrings.current
    val shape = RoundedCornerShape(36.dp)
    Column(
        modifier.clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF16181D), Color(0xFF08090C))))
            .border(1.dp, Brush.verticalGradient(listOf(Silver.copy(alpha = 0.6f), Silver.copy(alpha = 0.08f))), shape)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(S.both, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            Text("SYNC", fontSize = 9.sp, letterSpacing = 2.sp, color = Silver.copy(alpha = 0.7f))
        }
        val flowUp = remember { Animatable(0f) }
        PressBox(
            Modifier.fillMaxWidth().height(70.dp), Silver, filled = true, enabled = bothUp != null,
            onDown = { bothUp?.let(onMulti) }
        ) { pressed ->
            LaunchedEffect(pressed) {
                if (pressed) {
                    while (true) {
                        flowUp.snapTo(0f)
                        flowUp.animateTo(1f, tween(700, easing = LinearEasing))
                    }
                } else {
                    flowUp.snapTo(0f)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Chevrons(true, 2, Ink, 28.dp, flow = { flowUp.value })
                Text(bothUp?.let { S.cmdLabel(it) } ?: "—", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
            }
        }
        // دکمه‌ی M (میباخ): شروع، و وقتی فعال است توقف
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                if (maybachOn) MaybachPulse(Modifier.requiredSize(92.dp))
                PressBox(
                    Modifier.size(70.dp), Gold, filled = maybachOn, shape = CircleShape,
                    enabled = mOn != null,
                    onDown = { if (maybachOn) mOff?.let(onPress) else mOn?.let(onPress) }
                ) { _ ->
                    Text("M", color = if (maybachOn) Ink else Gold, fontWeight = FontWeight.Black, fontSize = 28.sp)
                }
            }
            Text(
                if (maybachOn) S.stop else S.maybach, fontSize = 10.sp,
                color = Gold.copy(alpha = 0.85f), modifier = Modifier.padding(top = 3.dp)
            )
        }
        val flowDn = remember { Animatable(0f) }
        PressBox(
            Modifier.fillMaxWidth().height(70.dp), Silver, filled = true, enabled = bothDown != null,
            onDown = { bothDown?.let(onMulti) }
        ) { pressed ->
            LaunchedEffect(pressed) {
                if (pressed) {
                    while (true) {
                        flowDn.snapTo(0f)
                        flowDn.animateTo(1f, tween(700, easing = LinearEasing))
                    }
                } else {
                    flowDn.snapTo(0f)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Chevrons(false, 2, Ink, 28.dp, flow = { flowDn.value })
                Text(bothDown?.let { S.cmdLabel(it) } ?: "—", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
            }
        }
        PressBox(
            Modifier.fillMaxWidth().height(44.dp), Silver, enabled = factory != null,
            onDown = { factory?.let(onFactory) }
        ) { _ ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Home, null, tint = Silver, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(S.factory, color = Silver, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
            }
        }
    }
}
