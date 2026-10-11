package com.mamad.peugeot405

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Grey = Color(0xFF8B93A5)
private val BuiltInGroups = setOf("جلو", "عقب", "میباخ")

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refreshDevices() }
    private val enableLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { vm.refreshDevices() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBtPermission()
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, background = Bg, surface = Panel)) {
                    AppRoot(vm, this@MainActivity::requestBtPermission, this@MainActivity::enableBluetooth, this@MainActivity::openAppSettings)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshDevices()
    }

    private fun requestBtPermission() {
        if (Build.VERSION.SDK_INT >= 31) permLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }

    private fun enableBluetooth() {
        enableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    private fun openAppSettings() {
        startActivity(
            Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
        )
    }
}

@Composable
fun AppRoot(
    vm: MainViewModel,
    onRequestPermission: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var showConnect by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = Bg,
        contentColor = Color.White,
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Home, null) }, label = { Text("کنترل") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Settings, null) }, label = { Text("فرمان‌ها") })
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Header(vm) { vm.refreshDevices(); showConnect = true }
            if (tab == 0) ControlScreen(vm) else SettingsScreen(vm)
        }
    }
    if (showConnect) {
        ConnectDialog(vm, { showConnect = false }, onRequestPermission, onEnableBluetooth, onOpenSettings)
    }
}

@SuppressLint("MissingPermission")
@Composable
fun ConnectDialog(
    vm: MainViewModel,
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } },
        dismissButton = {
            if (vm.connected) TextButton(onClick = { vm.disconnect(); onDismiss() }) { Text("قطع اتصال") }
        },
        title = { Text("اتصال به HC-05") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    !vm.btSupported -> Text("این گوشی بلوتوث ندارد.")
                    !vm.btPermission -> {
                        Text("برنامه اجازه‌ی «دستگاه‌های نزدیک» (بلوتوث) را ندارد.")
                        Button(onClick = onRequestPermission) { Text("دادن اجازه") }
                        TextButton(onClick = onOpenSettings) { Text("باز کردن تنظیمات برنامه") }
                    }
                    !vm.btEnabled -> {
                        Text("بلوتوث گوشی خاموش است.")
                        Button(onClick = onEnableBluetooth) { Text("روشن کردن بلوتوث") }
                    }
                    vm.devices.isEmpty() ->
                        Text("دستگاه جفت‌شده‌ای نیست. اول از تنظیمات بلوتوث گوشی، HC-05 را جفت کن (رمز 1234 یا 0000) و برگرد.")
                    else -> {
                        LazyColumn(Modifier.heightIn(max = 260.dp)) {
                            items(vm.devices) { d ->
                                TextButton(onClick = { vm.connect(d); onDismiss() }) {
                                    Text("${d.name ?: "بدون نام"}  (${d.address})")
                                }
                            }
                        }
                        Text(
                            "اگر وصل نشد: چراغ HC-05 باید تند چشمک بزند و به گوشی دیگری وصل نباشد.",
                            fontSize = 11.sp, color = Grey
                        )
                    }
                }
                Text(vm.status, fontSize = 11.sp, color = Grey)
            }
        }
    )
}

@Composable
fun Header(vm: MainViewModel, onConnect: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("پژو ۴۰۵", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color.White)
                Text("کنترل تعلیق پنوماتیک", fontSize = 12.sp, color = Grey)
            }
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

@Composable
fun ControlScreen(vm: MainViewModel) {
    val cmds = vm.commands
    fun find(a: String): Cmd? = cmds.firstOrNull { it.action == a }
    val custom = cmds.filter { it.action == "NONE" }
    val otherGroups = custom.filter { it.group !in BuiltInGroups }.groupBy { it.group }
    Column(Modifier.fillMaxSize()) {
        CarStage({ vm.sim.dispF }, { vm.sim.dispB }, vm.sim.maybachOn)
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AxleCard(
                    "جلو", "FRONT", Cyan,
                    find("F_UP_S"), find("F_UP_F"), find("F_DN_S"), find("F_DN_F"),
                    custom.filter { it.group == "جلو" },
                    { (vm.sim.dispF - 80f) / 32f },
                    vm::press, vm::press, vm::pressUp
                )
            }
            item {
                AxleCard(
                    "عقب", "REAR", Violet,
                    find("R_UP_S"), find("R_UP_F"), find("R_DN_S"), find("R_DN_F"),
                    custom.filter { it.group == "عقب" },
                    { (vm.sim.dispB - 84f) / 31f },
                    vm::press, vm::press, vm::pressUp
                )
            }
            item { MaybachCard(find("M_ON"), find("M_OFF"), vm.sim.maybachOn, custom.filter { it.group == "میباخ" }, vm::press) }
            otherGroups.forEach { (name, list) ->
                item(key = "group_$name") { GroupCard(name, list, vm::press) }
            }
        }
    }
}

@Composable
fun CardBox(accent: Color, active: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Panel)
            .border(1.dp, accent.copy(alpha = if (active) 0.9f else 0.25f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun CardTitle(title: String, tag: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 4.dp, height = 18.dp).clip(RoundedCornerShape(2.dp)).background(accent))
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        Spacer(Modifier.weight(1f))
        Text(tag, fontSize = 11.sp, color = accent.copy(alpha = 0.7f))
    }
}

// نوار باریک که ارتفاع فعلی محور را نشان می‌دهد (فقط دوباره‌کشی می‌شود، نه ترکیب مجدد)
@Composable
fun Gauge(value: () -> Float, accent: Color) {
    Canvas(Modifier.fillMaxWidth().height(6.dp)) {
        val r = CornerRadius(size.height / 2f)
        drawRoundRect(accent.copy(alpha = 0.15f), size = size, cornerRadius = r)
        val w = size.width * value().coerceIn(0f, 1f)
        if (w > 0f) {
            val x0 = if (layoutDirection == LayoutDirection.Rtl) size.width - w else 0f
            drawRoundRect(accent, topLeft = Offset(x0, 0f), size = Size(w, size.height), cornerRadius = r)
        }
    }
}

@Composable
fun PillButton(
    label: String,
    accent: Color,
    filled: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    onDown: () -> Unit = {},
    onUp: () -> Unit = {},
    onClick: () -> Unit = {},
    leading: @Composable () -> Unit = {}
) {
    val shape = RoundedCornerShape(18.dp)
    var pressed by remember { mutableStateOf(false) }
    val fg = if (filled) Color(0xFF0B0D12) else accent
    val down by rememberUpdatedState(onDown)
    val up by rememberUpdatedState(onUp)
    val click by rememberUpdatedState(onClick)
    val bg = if (filled) accent.copy(alpha = if (pressed) 0.72f else 1f)
             else accent.copy(alpha = if (pressed) 0.38f else 0.14f)
    Row(
        modifier.height(height).clip(shape).background(bg)
            .border(1.dp, accent.copy(alpha = 0.55f), shape)
            .pointerInput(enabled) {
                detectTapGestures(onPress = {
                    if (enabled) {
                        pressed = true
                        down()
                        var released = false
                        try {
                            released = tryAwaitRelease()
                        } finally {
                            pressed = false
                            up()
                        }
                        if (released) click()
                    }
                })
            }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        leading()
        Text(label, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
    }
}

// ---------- دکمه‌های اسپرت تعلیق: متوازی‌الاضلاع با فلش ----------

private val SlantShape = GenericShape { size, _ ->
    val k = size.height * 0.28f
    moveTo(k, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width - k, size.height)
    lineTo(0f, size.height)
    close()
}

@Composable
fun Chevrons(up: Boolean, count: Int, tint: Color) {
    Canvas(Modifier.size(30.dp, 30.dp)) {
        val w = size.width
        val h = size.height
        val gap = 9.dp.toPx()
        val dy = 5.dp.toPx()
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
            drawPath(path, tint, style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun SportButton(
    label: String,
    accent: Color,
    up: Boolean,
    fast: Boolean,
    enabled: Boolean,
    onDown: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val down by rememberUpdatedState(onDown)
    val upCb by rememberUpdatedState(onUp)
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(80), label = "press")
    val fg = if (fast) Color(0xFF0B0D12) else accent
    val fill = if (fast) {
        Brush.horizontalGradient(listOf(accent.copy(alpha = if (pressed) 0.85f else 1f), accent.copy(alpha = if (pressed) 0.6f else 0.72f)))
    } else {
        Brush.horizontalGradient(
            listOf(accent.copy(alpha = if (pressed) 0.42f else 0.16f), accent.copy(alpha = if (pressed) 0.28f else 0.05f))
        )
    }
    Row(
        modifier.height(66.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(SlantShape).background(fill)
            .border(1.2.dp, accent.copy(alpha = if (pressed) 1f else 0.6f), SlantShape)
            .pointerInput(enabled) {
                detectTapGestures(onPress = {
                    if (enabled) {
                        pressed = true
                        down()
                        try {
                            tryAwaitRelease()
                        } finally {
                            pressed = false
                            upCb()
                        }
                    }
                })
            }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Chevrons(up, if (fast) 2 else 1, fg)
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
    }
}

@Composable
fun AxleSport(
    cmd: Cmd?, accent: Color, up: Boolean, fast: Boolean,
    onPress: (Cmd) -> Unit, onHoldStart: (Cmd) -> Unit, onHoldEnd: () -> Unit,
    modifier: Modifier
) {
    SportButton(
        label = cmd?.label ?: "—",
        accent = accent, up = up, fast = fast, enabled = cmd != null,
        // آرام: تا زمانی که دست روی دکمه است حرکت می‌کند، با رها کردن می‌ایستد
        // سریع: همان لحظه‌ی لمس یک‌بار فرمان می‌رود
        onDown = { cmd?.let { if (fast) onPress(it) else onHoldStart(it) } },
        onUp = { if (!fast) onHoldEnd() },
        modifier = modifier
    )
}

@Composable
fun ExtraButtons(extras: List<Cmd>, accent: Color, onPress: (Cmd) -> Unit) {
    extras.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { c ->
                PillButton(
                    label = c.label, accent = accent, filled = false, enabled = true,
                    modifier = Modifier.weight(1f), height = 46.dp, onClick = { onPress(c) }
                )
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
fun AxleCard(
    title: String, tag: String, accent: Color,
    upSlow: Cmd?, upFast: Cmd?, downSlow: Cmd?, downFast: Cmd?,
    extras: List<Cmd>, gauge: () -> Float,
    onPress: (Cmd) -> Unit, onHoldStart: (Cmd) -> Unit, onHoldEnd: () -> Unit
) {
    CardBox(accent) {
        CardTitle(title, tag, accent)
        Gauge(gauge, accent)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AxleSport(upSlow, accent, true, false, onPress, onHoldStart, onHoldEnd, Modifier.weight(1f))
            AxleSport(upFast, accent, true, true, onPress, onHoldStart, onHoldEnd, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AxleSport(downSlow, accent, false, false, onPress, onHoldStart, onHoldEnd, Modifier.weight(1f))
            AxleSport(downFast, accent, false, true, onPress, onHoldStart, onHoldEnd, Modifier.weight(1f))
        }
        ExtraButtons(extras, accent, onPress)
    }
}

@Composable
fun MaybachCard(start: Cmd?, stop: Cmd?, active: Boolean, extras: List<Cmd>, onPress: (Cmd) -> Unit) {
    CardBox(Gold, active) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 4.dp, height = 18.dp).clip(RoundedCornerShape(2.dp)).background(Gold))
            Spacer(Modifier.width(8.dp))
            Text("میباخ", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            Spacer(Modifier.weight(1f))
            Text(if (active) "در حال اجرا" else "MAYBACH MODE", fontSize = 11.sp, color = Gold.copy(alpha = 0.8f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(
                label = start?.label ?: "—", accent = Gold, filled = true, enabled = start != null,
                modifier = Modifier.weight(1.4f), onClick = { start?.let(onPress) }
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF0B0D12), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(6.dp))
            }
            PillButton(
                label = stop?.label ?: "—", accent = Gold, filled = false, enabled = stop != null,
                modifier = Modifier.weight(1f), onClick = { stop?.let(onPress) }
            ) {
                Icon(Icons.Default.Close, null, tint = Gold, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
            }
        }
        ExtraButtons(extras, Gold, onPress)
    }
}

// هر گروه دلخواه یک کارت جدا با اسم خودش
@Composable
fun GroupCard(name: String, list: List<Cmd>, onPress: (Cmd) -> Unit) {
    val accent = Color(0xFF7DD3C0)
    CardBox(accent) {
        CardTitle(name, "CUSTOM", accent)
        ExtraButtons(list, accent, onPress)
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel) {
    var editing by remember { mutableStateOf<Cmd?>(null) }
    var raw by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(raw, { raw = it }, label = { Text("ارسال فرمان دلخواه") },
                    modifier = Modifier.weight(1f), singleLine = true)
                Spacer(Modifier.width(8.dp))
                Button(onClick = { if (raw.isNotEmpty()) vm.send(raw) }) { Text("ارسال") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { editing = Cmd(label = "", command = "", group = "سایر") }) {
                    Icon(Icons.Default.Add, null); Text("فرمان جدید")
                }
                OutlinedButton(onClick = { vm.resetDefaults() }) { Text("پیش‌فرض") }
            }
        }
        items(vm.commands, key = { it.id }) { c ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${c.group} • ${c.label}")
                        Text("فرمان ارسالی: ${c.command}", fontSize = 11.sp, color = Grey)
                    }
                    IconButton(onClick = { editing = c }) { Icon(Icons.Default.Edit, null) }
                    if (c.action == "NONE") {
                        IconButton(onClick = { vm.delete(c.id) }) { Icon(Icons.Default.Delete, null) }
                    }
                }
            }
        }
    }
    editing?.let { cmd ->
        var label by remember(cmd.id) { mutableStateOf(cmd.label) }
        var command by remember(cmd.id) { mutableStateOf(cmd.command) }
        var group by remember(cmd.id) { mutableStateOf(cmd.group) }
        val groups = (listOf("جلو", "عقب", "میباخ", "سایر") + vm.commands.map { it.group })
            .filter { it != "سیستمی" }.distinct()
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("ویرایش فرمان") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(label, { label = it }, label = { Text("نام دکمه") }, singleLine = true)
                    OutlinedTextField(command, { command = it }, label = { Text("فرمان ارسالی") }, singleLine = true)
                    if (cmd.action == "NONE") {
                        OutlinedTextField(group, { group = it }, label = { Text("اسم گروه (جدید بنویس یا انتخاب کن)") }, singleLine = true)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            groups.forEach { g ->
                                Text(
                                    g, fontSize = 12.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(50))
                                        .background(if (group.trim() == g) Cyan.copy(alpha = 0.3f) else Color(0xFF222B3A))
                                        .clickable { group = g }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (label.isNotBlank() && command.isNotEmpty()) {
                        vm.upsert(cmd.copy(label = label.trim(), command = command, group = group.ifBlank { "سایر" }.trim()))
                        editing = null
                    }
                }) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("لغو") } }
        )
    }
}
