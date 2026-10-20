package com.mamad.peugeot405

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlin.math.roundToInt

private val Ink = Color(0xFF07080B)

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
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, tween(90), label = "pressScale")
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
            .clip(shape)
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = top), accent.copy(alpha = bottom))))
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
fun Chevrons(up: Boolean, count: Int, tint: Color, size: Dp = 30.dp) {
    Canvas(Modifier.size(size, size)) {
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
fun Header(vm: MainViewModel, onConnect: () -> Unit, onRemote: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("AIR CONTROL", fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 1.5.sp, color = Color.White)
                Text("کنترل هوشمند تعلیق", fontSize = 12.sp, color = Steel)
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
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (vm.connected) Color(0xFF4ADE80) else Color(0xFFF87171)))
                Spacer(Modifier.width(8.dp))
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
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "control_overview") {
                ControlOverviewCard(vm, pctF, pctB)
            }
            item {
                // چیدمان راست به چپ: جلو (سمت راست مثل ماشین)، عقب، و ستون «هر دو» سمت چپ
                Row(
                    Modifier.fillMaxWidth().height(392.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AxleCapsule(
                        "جلو", "FRONT", Cyan,
                        find("F_UP_S"), find("F_UP_F"), find("F_DN_S"), find("F_DN_F"),
                        { (vm.sim.dispF - 80f) / 32f }, pctF,
                        vm::press, vm::press, vm::pressUp,
                        Modifier.weight(1f).fillMaxHeight()
                    )
                    AxleCapsule(
                        "عقب", "REAR", Violet,
                        find("R_UP_S"), find("R_UP_F"), find("R_DN_S"), find("R_DN_F"),
                        { (vm.sim.dispB - 84f) / 31f }, pctB,
                        vm::press, vm::press, vm::pressUp,
                        Modifier.weight(1f).fillMaxHeight()
                    )
                    AllCapsule(
                        find("BOTH_UP"), find("BOTH_DN"), find("FACTORY"),
                        find("M_ON"), find("M_OFF"), vm.sim.maybachOn,
                        vm::pressMulti, vm::factory, vm::press,
                        Modifier.weight(0.9f).fillMaxHeight()
                    )
                }
            }
            groups.forEach { (name, list) ->
                item(key = "group_$name") { GroupCard(name, list, vm::press) }
            }
        }
    }
}

// ---------- کارت وضعیت مدرن؛ بدون تصویر خودرو ----------

@Composable
private fun ControlOverviewCard(vm: MainViewModel, frontPercent: Int, rearPercent: Int) {
    val pulse by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (vm.connected) 1f else 0.35f,
        animationSpec = tween(650),
        label = "connectionPulse"
    )
    val frontLevel by androidx.compose.animation.core.animateFloatAsState(
        targetValue = frontPercent / 100f, animationSpec = tween(420), label = "frontLevel"
    )
    val rearLevel by androidx.compose.animation.core.animateFloatAsState(
        targetValue = rearPercent / 100f, animationSpec = tween(420), label = "rearLevel"
    )
    val shape = RoundedCornerShape(26.dp)
    Row(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF121C2A), Color(0xFF0B0D13), Color(0xFF171326))))
            .border(1.dp, Cyan.copy(alpha = 0.30f), shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Cyan.copy(alpha = 0.25f), Violet.copy(alpha = 0.18f)))),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize().padding(7.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                drawCircle(Cyan.copy(alpha = 0.18f + 0.25f * pulse), size.minDimension * 0.43f, Offset(cx, cy))
                drawCircle(Cyan.copy(alpha = 0.95f), size.minDimension * 0.28f, Offset(cx, cy), style = Stroke(2.dp.toPx()))
                drawLine(Color.White.copy(alpha = 0.9f), Offset(cx, cy - size.height * 0.22f), Offset(cx, cy + size.height * 0.22f), 2.dp.toPx(), StrokeCap.Round)
                drawLine(Color.White.copy(alpha = 0.9f), Offset(cx - size.width * 0.22f, cy), Offset(cx + size.width * 0.22f, cy), 2.dp.toPx(), StrokeCap.Round)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SUSPENSION SYSTEM", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(7.dp).clip(CircleShape).background(
                    (if (vm.connected) Color(0xFF4ADE80) else Color(0xFFFF6B7A)).copy(alpha = pulse)
                ))
            }
            Text(if (vm.connected) "اتصال بلوتوث فعال" else "آماده‌ی کنترل • بلوتوث متصل نیست",
                color = Steel, fontSize = 11.sp, maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniLevel("جلو", frontLevel, Cyan, frontPercent, Modifier.weight(1f))
                MiniLevel("عقب", rearLevel, Violet, rearPercent, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniLevel(label: String, level: Float, accent: Color, percent: Int, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Steel, fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
            Text("$percent%", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f))) {
            Box(Modifier.fillMaxWidth(level.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.55f), accent))))
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
    ) { _ ->
        val fg = if (fast) Ink else accent
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Chevrons(up, if (fast) 2 else 1, fg, 24.dp)
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

// ---------- ستون «هر دو»: بالا/پایین همزمان، میباخ، فابریک ----------

@Composable
fun AllCapsule(
    bothUp: Cmd?, bothDown: Cmd?, factory: Cmd?, mOn: Cmd?, mOff: Cmd?, maybachOn: Boolean,
    onMulti: (Cmd) -> Unit, onFactory: (Cmd) -> Unit, onPress: (Cmd) -> Unit,
    modifier: Modifier = Modifier
) {
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
            Text("هر دو", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            Text("SYNC", fontSize = 9.sp, letterSpacing = 2.sp, color = Silver.copy(alpha = 0.7f))
        }
        PressBox(
            Modifier.fillMaxWidth().height(70.dp), Silver, filled = true, enabled = bothUp != null,
            onDown = { bothUp?.let(onMulti) }
        ) { _ ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Chevrons(true, 2, Ink, 28.dp)
                Text(bothUp?.label ?: "—", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
            }
        }
        // دکمه‌ی M (میباخ): شروع، و وقتی فعال است توقف
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PressBox(
                Modifier.size(70.dp), Gold, filled = maybachOn, shape = CircleShape,
                enabled = mOn != null,
                onDown = { if (maybachOn) mOff?.let(onPress) else mOn?.let(onPress) }
            ) { _ ->
                Text("M", color = if (maybachOn) Ink else Gold, fontWeight = FontWeight.Black, fontSize = 28.sp)
            }
            Text(
                if (maybachOn) "توقف" else "میباخ", fontSize = 10.sp,
                color = Gold.copy(alpha = 0.85f), modifier = Modifier.padding(top = 3.dp)
            )
        }
        PressBox(
            Modifier.fillMaxWidth().height(70.dp), Silver, filled = true, enabled = bothDown != null,
            onDown = { bothDown?.let(onMulti) }
        ) { _ ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Chevrons(false, 2, Ink, 28.dp)
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
