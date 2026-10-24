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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
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
            // جهت صفحه و متن‌ها با زبان انتخاب‌شده عوض می‌شود
            val fa = vm.isFa
            val strings = remember(fa) { Strings(fa) }
            CompositionLocalProvider(
                LocalLayoutDirection provides (if (fa) LayoutDirection.Rtl else LayoutDirection.Ltr),
                LocalStrings provides strings
            ) {
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


// مراحل ورود: 0 = آرم پژو ، 1 = دکمه‌ی اتصال ، 2 = ریموت پژو ، 3 = منوی اصلی
@Composable
fun AppRoot(
    vm: MainViewModel,
    onRequestPermission: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var phase by rememberSaveable { mutableStateOf(if (vm.connected) 3 else 0) }
    var tab by remember { mutableIntStateOf(0) }
    var showConnect by remember { mutableStateOf(false) }
    var showRemote by remember { mutableStateOf(false) }
    var transition by remember { mutableStateOf(false) }

    // بعد از اتصال موفق: دکمه محو می‌شود و بعدش ریموت ظاهر می‌شود
    LaunchedEffect(vm.connected) {
        if (vm.connected && phase == 1) {
            delay(1050)
            if (phase == 1) phase = 2
        }
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        if (phase == 3 || transition) {
            MenuLayer(
                vm, tab, { tab = it },
                { vm.refreshDevices(); showConnect = true },
                { showRemote = true },
                { transition = false; phase = 0 }
            )
        }
        if (phase == 1) {
            ConnectScreen(vm, onPick = { vm.refreshDevices(); showConnect = true }, onSkip = { phase = 3 })
        }
        if (phase == 2) {
            RemoteIntro(vm, onLeaving = { transition = true }, onDone = { phase = 3; transition = false })
        }
        if (phase == 0) {
            SplashScreen(onDone = { phase = if (vm.connected) 2 else 1 })
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
    onRemote: () -> Unit,
    onReplayIntro: () -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(700), label = "menuIn")
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = a }) {
        AmbientBackground(accent = if (vm.sim.maybachOn) Gold else Cyan)
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            bottomBar = { BottomNav(tab, onTab) }
        ) { pad ->
            Column(Modifier.padding(pad).fillMaxSize()) {
                Header(vm, onConnect, onRemote)
                // تعویض تب‌ها با اسلاید و محو شدن
                AnimatedContent(
                    targetState = tab,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { full -> full * dir / 3 } +
                            fadeIn(tween(420))) togetherWith
                            (slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { full -> -full * dir / 3 } +
                                fadeOut(tween(200)))
                    },
                    label = "tabs"
                ) { t ->
                    when (t) {
                        0 -> ControlScreen(vm)
                        1 -> CommandsScreen(vm)
                        else -> SettingsScreen(vm, onConnect, onRemote, onReplayIntro)
                    }
                }
            }
        }
    }
}

// ---------- نوار پایین با نشانگر لغزنده ----------

@Composable
fun BottomNav(tab: Int, onTab: (Int) -> Unit) {
    val S = LocalStrings.current
    val labels = listOf(S.navControl, S.navCommands, S.navSettings)
    val icons = listOf(Icons.Default.Home, Icons.Default.Edit, Icons.Default.Settings)
    val pos by animateFloatAsState(tab.toFloat(), spring(dampingRatio = 0.7f, stiffness = 260f), label = "navPos")
    BoxWithConstraints(Modifier.fillMaxWidth().background(Panel)) {
        val itemW = maxWidth / 3
        // نور پشت آیتم انتخاب‌شده
        Box(
            Modifier.offset(x = itemW * pos).width(itemW).height(62.dp)
                .background(Brush.verticalGradient(listOf(Cyan.copy(alpha = 0.14f), Color.Transparent)))
        )
        // خط نورانی بالای آیتم انتخاب‌شده
        Box(Modifier.offset(x = itemW * pos).width(itemW).height(62.dp), contentAlignment = Alignment.TopCenter) {
            Box(
                Modifier.width(36.dp).height(3.dp).clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(PeugeotBlue, Cyan)))
            )
        }
        Row(Modifier.fillMaxWidth().height(62.dp)) {
            for (i in 0..2) {
                val sel = tab == i
                val sc by animateFloatAsState(if (sel) 1.2f else 1f, spring(dampingRatio = 0.45f, stiffness = 400f), label = "navSc$i")
                Column(
                    Modifier.weight(1f).fillMaxHeight()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTab(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        icons[i], null, tint = if (sel) Cyan else Grey,
                        modifier = Modifier.size(24.dp).graphicsLayer { scaleX = sc; scaleY = sc }
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(labels[i], fontSize = 11.sp, color = if (sel) Cyan else Grey)
                }
            }
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
    val S = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(S.close) } },
        dismissButton = {
            if (vm.connected) TextButton(onClick = { vm.disconnect(); onDismiss() }) { Text(S.disconnect) }
        },
        title = { Text(S.dlgTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    !vm.btSupported -> Text(S.noBluetooth)
                    !vm.btPermission -> {
                        Text(S.noPermission)
                        Button(onClick = onRequestPermission) { Text(S.grantPermission) }
                        TextButton(onClick = onOpenSettings) { Text(S.openAppSettings) }
                    }
                    !vm.btEnabled -> {
                        Text(S.btOff)
                        Button(onClick = onEnableBluetooth) { Text(S.turnOnBt) }
                    }
                    vm.devices.isEmpty() -> Text(S.noPaired)
                    else -> {
                        LazyColumn(Modifier.heightIn(max = 260.dp)) {
                            items(vm.devices) { d ->
                                TextButton(onClick = { vm.connect(d); onDismiss() }) {
                                    Text("${d.name ?: S.unnamed}  (${d.address})")
                                }
                            }
                        }
                        Text(S.connectTip, fontSize = 11.sp, color = Grey)
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
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF131720), Panel)))
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
    val fg = if (filled) Color(0xFF0B0D12) else accent
    PressBox(
        modifier.height(height), accent, filled = filled, enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        onDown = onDown,
        onUp = onUp,
        onTap = onClick
    ) { _ ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            leading()
            Text(label, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
        }
    }
}

@Composable
fun ExtraButtons(extras: List<Cmd>, accent: Color, onPress: (Cmd) -> Unit) {
    val S = LocalStrings.current
    extras.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { c ->
                PillButton(
                    label = S.cmdLabel(c), accent = accent, filled = false, enabled = true,
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
    val S = LocalStrings.current
    val accent = Color(0xFF7DD3C0)
    CardBox(accent) {
        CardTitle(S.group(name), "CUSTOM", accent)
        ExtraButtons(list, accent, onPress)
    }
}

// ---------- تب فرمان‌ها (ویرایش دکمه‌ها و فرمان‌ها) ----------

@Composable
fun CommandsScreen(vm: MainViewModel) {
    val S = LocalStrings.current
    var editing by remember { mutableStateOf<Cmd?>(null) }
    var raw by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(raw, { raw = it }, label = { Text(S.customCommand) },
                    modifier = Modifier.weight(1f), singleLine = true)
                Spacer(Modifier.width(8.dp))
                Button(onClick = { if (raw.isNotEmpty()) vm.send(raw) }) { Text(S.send) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { editing = Cmd(label = "", command = "", group = "سایر") }) {
                    Icon(Icons.Default.Add, null); Text(S.newCommand)
                }
                OutlinedButton(onClick = { vm.resetDefaults() }) { Text(S.defaults) }
            }
        }
        itemsIndexed(vm.commands, key = { _, c -> c.id }) { i, c ->
            Card(
                Modifier.fillMaxWidth().reveal(minOf(i, 8) * 45, 24.dp),
                colors = CardDefaults.cardColors(containerColor = Panel)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${S.group(c.group)} • ${S.cmdLabel(c)}")
                        Text("${S.sentCommand}: ${c.command}", fontSize = 11.sp, color = Grey)
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
        var label by remember(cmd.id) { mutableStateOf(S.cmdLabel(cmd)) }
        var command by remember(cmd.id) { mutableStateOf(cmd.command) }
        var group by remember(cmd.id) { mutableStateOf(S.group(cmd.group)) }
        val groups = (listOf("جلو", "عقب", "میباخ", "سایر") + vm.commands.map { it.group })
            .filter { it != "سیستمی" }.distinct()
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(S.editCommand) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(label, { label = it }, label = { Text(S.buttonName) }, singleLine = true)
                    OutlinedTextField(command, { command = it }, label = { Text(S.sentCommand) }, singleLine = true)
                    if (cmd.action == "NONE") {
                        OutlinedTextField(group, { group = it }, label = { Text(S.groupName) }, singleLine = true)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            groups.forEach { g ->
                                val shown = S.group(g)
                                Text(
                                    shown, fontSize = 12.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(50))
                                        .background(if (group.trim() == shown) Cyan.copy(alpha = 0.3f) else Color(0xFF222B3A))
                                        .clickable { group = shown }
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
                        val newGroup = if (cmd.action == "NONE") S.groupKey(group.trim()).ifBlank { "سایر" } else cmd.group
                        vm.upsert(cmd.copy(label = label.trim(), command = command, group = newGroup))
                        editing = null
                    }
                }) { Text(S.save) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(S.cancel) } }
        )
    }
}

// ---------- تب تنظیمات (زبان، اتصال، صدا) ----------

@Composable
fun SettingsScreen(vm: MainViewModel, onConnect: () -> Unit, onRemote: () -> Unit, onReplayIntro: () -> Unit) {
    val S = LocalStrings.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(Modifier.reveal(0)) {
                CardBox(Cyan) {
                    CardTitle(S.language, "LANGUAGE", Cyan)
                    Text(S.languageHint, color = Grey, fontSize = 12.sp)
                    LangToggle(vm.isFa, { vm.setLang(it) })
                }
            }
        }
        item {
            Box(Modifier.reveal(90)) {
                CardBox(PeugeotBlue) {
                    CardTitle(S.connection, "BLUETOOTH", PeugeotBlue)
                    Text(vm.status, color = Grey, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PillButton(
                            if (vm.connected) S.disconnect else S.headerConnect, Cyan, true, true,
                            Modifier.weight(1f), 48.dp,
                            onClick = { if (vm.connected) vm.disconnect() else onConnect() }
                        )
                        PillButton(S.showRemote, Gold, false, true, Modifier.weight(1f), 48.dp, onClick = onRemote)
                    }
                }
            }
        }
        item {
            Box(Modifier.reveal(180)) {
                CardBox(Gold) {
                    CardTitle(S.remoteSound, "SOUND", Gold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(S.remoteSound, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = vm.soundOn, onCheckedChange = { vm.setSound(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PeugeotBlue)
                        )
                    }
                    PillButton(S.replayIntro, Silver, false, true, Modifier.fillMaxWidth(), 48.dp, onClick = onReplayIntro)
                }
            }
        }
        item {
            Box(Modifier.reveal(270)) {
                CardBox(Silver) {
                    CardTitle(S.about, "v2.0", Silver)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LionLogo(44.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("PEUGEOT 405", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Text(S.aboutText, color = Grey, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
