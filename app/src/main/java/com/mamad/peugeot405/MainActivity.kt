package com.mamad.peugeot405

import android.app.Application
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Grey = Color(0xFF8B93A5)

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
            AppLanguage.english = getSharedPreferences("ui", MODE_PRIVATE).getBoolean("english", false)
            CompositionLocalProvider(LocalLayoutDirection provides if (AppLanguage.english) LayoutDirection.Ltr else LayoutDirection.Rtl) {
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


// مراحل ورود: 0 = دکمه‌ی اتصال ، 1 = ریموت پژو ، 2 = منوی اصلی
@Composable
fun AppRoot(
    vm: MainViewModel,
    onRequestPermission: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var phase by rememberSaveable { mutableStateOf(if (vm.connected) 2 else 0) }
    var tab by remember { mutableIntStateOf(0) }
    var showConnect by remember { mutableStateOf(false) }
    var showRemote by remember { mutableStateOf(false) }
    var transition by remember { mutableStateOf(false) }

    // بعد از اتصال موفق: دکمه محو می‌شود و بعدش ریموت ظاهر می‌شود
    LaunchedEffect(vm.connected) {
        if (vm.connected && phase == 0) {
            delay(1050)
            if (phase == 0) phase = 1
        }
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        if (phase == 2 || transition) {
            MenuLayer(
                vm, tab, { tab = it },
                { vm.refreshDevices(); showConnect = true },
                { showRemote = true }
            )
        }
        if (phase == 0) {
            ConnectScreen(vm, onPick = { vm.refreshDevices(); showConnect = true }, onSkip = { phase = 2 })
        }
        if (phase == 1) {
            RemoteIntro(vm, onLeaving = { transition = true }, onDone = { phase = 2; transition = false })
        }
        RemoteOverlay(vm, showRemote) { showRemote = false }
    }
    if (showConnect) {
        ConnectDialog(vm, { showConnect = false }, onRequestPermission, onEnableBluetooth, onOpenSettings)
    }
}

@Composable
fun MenuLayer(
    vm: MainViewModel,
    tab: Int,
    onTab: (Int) -> Unit,
    onConnect: () -> Unit,
    onRemote: () -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(700), label = "menuIn")
    Scaffold(
        modifier = Modifier.graphicsLayer { alpha = a },
        containerColor = Bg,
        contentColor = Color.White,
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                val colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF1A2230),
                    selectedIconColor = Cyan, selectedTextColor = Cyan,
                    unselectedIconColor = Grey, unselectedTextColor = Grey
                )
                NavigationBarItem(selected = tab == 0, onClick = { onTab(0) }, colors = colors,
                    icon = { Icon(Icons.Default.Home, null) }, label = { Text(tr("کنترل", "Control")) })
                NavigationBarItem(selected = tab == 1, onClick = { onTab(1) }, colors = colors,
                    icon = { Icon(Icons.Default.Settings, null) }, label = { Text(tr("فرمان‌ها", "Commands")) })
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Header(vm, onConnect, onRemote)
            if (tab == 0) ControlScreen(vm) else SettingsScreen(vm)
        }
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
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("زبان برنامه", "App language"), fontWeight = FontWeight.Bold)
                        Text(tr("فارسی / English", "English / Persian"), fontSize = 12.sp, color = Grey)
                    }
                    Button(onClick = {
                        AppLanguage.english = !AppLanguage.english
                        (vm.getApplication<Application>().getSharedPreferences("ui", android.content.Context.MODE_PRIVATE))
                            .edit().putBoolean("english", AppLanguage.english).apply()
                    }) { Text(if (AppLanguage.english) "فارسی" else "English") }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(raw, { raw = it }, label = { Text(tr("ارسال فرمان دلخواه", "Send custom command")) },
                    modifier = Modifier.weight(1f), singleLine = true)
                Spacer(Modifier.width(8.dp))
                Button(onClick = { if (raw.isNotEmpty()) vm.send(raw) }) { Text(tr("ارسال", "Send")) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { editing = Cmd(label = "", command = "", group = "سایر") }) {
                    Icon(Icons.Default.Add, null); Text(tr("فرمان جدید", "New command"))
                }
                OutlinedButton(onClick = { vm.resetDefaults() }) { Text(tr("پیش‌فرض", "Defaults")) }
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
            title = { Text(tr("ویرایش فرمان", "Edit command")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(label, { label = it }, label = { Text(tr("نام دکمه", "Button label")) }, singleLine = true)
                    OutlinedTextField(command, { command = it }, label = { Text(tr("فرمان ارسالی", "Command to send")) }, singleLine = true)
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
                }) { Text(tr("ذخیره", "Save")) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(tr("لغو", "Cancel")) } }
        )
    }
}
