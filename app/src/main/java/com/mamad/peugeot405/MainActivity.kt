package com.mamad.peugeot405

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refreshDevices() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 31) permLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, background = Bg, surface = Panel)) {
                    AppRoot(vm)
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun AppRoot(vm: MainViewModel) {
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
        AlertDialog(
            onDismissRequest = { showConnect = false },
            confirmButton = { TextButton(onClick = { showConnect = false }) { Text("بستن") } },
            dismissButton = {
                if (vm.connected) TextButton(onClick = { vm.disconnect(); showConnect = false }) { Text("قطع اتصال") }
            },
            title = { Text("دستگاه‌های جفت‌شده") },
            text = {
                if (vm.devices.isEmpty()) Text("دستگاهی پیدا نشد. اول HC-05 را از تنظیمات بلوتوث گوشی جفت کن (رمز معمولاً 1234 یا 0000).")
                else LazyColumn {
                    items(vm.devices) { d ->
                        TextButton(onClick = { vm.connect(d); showConnect = false }) { Text("${d.name}  (${d.address})") }
                    }
                }
            }
        )
    }
}

@Composable
fun Header(vm: MainViewModel, onConnect: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("پژو ۴۰۵", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color.White)
                Text("کنترل تعلیق پنوماتیک", fontSize = 12.sp, color = Color(0xFF8B93A5))
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
    val extras = cmds.filter { it.action == "NONE" }
    Column(Modifier.fillMaxSize()) {
        CarStage(
            vm.frontTarget, vm.rearTarget, vm.frontMs, vm.rearMs, vm.maybach,
            Modifier.fillMaxWidth().height(210.dp)
        )
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AxleCard("جلو", "FRONT", Cyan,
                    find("F_UP_S"), find("F_UP_F"), find("F_DN_S"), find("F_DN_F"), vm::press)
            }
            item {
                AxleCard("عقب", "REAR", Violet,
                    find("R_UP_S"), find("R_UP_F"), find("R_DN_S"), find("R_DN_F"), vm::press)
            }
            item { MaybachCard(find("M_ON"), find("M_OFF"), vm.maybach, vm::press) }
            if (extras.isNotEmpty()) item { ExtrasCard(extras, vm::press) }
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

@Composable
fun PillButton(
    label: String,
    accent: Color,
    filled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    leading: @Composable () -> Unit = {}
) {
    val shape = RoundedCornerShape(18.dp)
    val fg = if (filled) Color(0xFF0B0D12) else accent
    Row(
        modifier.height(height).clip(shape)
            .background(if (filled) accent else accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.55f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        leading()
        Text(label, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
    }
}

@Composable
fun Arrows(up: Boolean, count: Int, tint: Color) {
    val icon = if (up) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        if (count == 2) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp).offset(y = (-5).dp))
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp).offset(y = 5.dp))
        } else {
            Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun AxleButton(cmd: Cmd?, accent: Color, up: Boolean, fast: Boolean, onPress: (Cmd) -> Unit, modifier: Modifier) {
    val fg = if (fast) Color(0xFF0B0D12) else accent
    PillButton(
        label = cmd?.label ?: "—",
        accent = accent,
        filled = fast,
        enabled = cmd != null,
        onClick = { cmd?.let(onPress) },
        modifier = modifier
    ) {
        Arrows(up, if (fast) 2 else 1, fg)
        Spacer(Modifier.width(6.dp))
    }
}

@Composable
fun AxleCard(
    title: String, tag: String, accent: Color,
    upSlow: Cmd?, upFast: Cmd?, downSlow: Cmd?, downFast: Cmd?,
    onPress: (Cmd) -> Unit
) {
    CardBox(accent) {
        CardTitle(title, tag, accent)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AxleButton(upSlow, accent, true, false, onPress, Modifier.weight(1f))
            AxleButton(upFast, accent, true, true, onPress, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AxleButton(downSlow, accent, false, false, onPress, Modifier.weight(1f))
            AxleButton(downFast, accent, false, true, onPress, Modifier.weight(1f))
        }
    }
}

@Composable
fun MaybachCard(start: Cmd?, stop: Cmd?, active: Boolean, onPress: (Cmd) -> Unit) {
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
                onClick = { start?.let(onPress) }, modifier = Modifier.weight(1.4f)
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF0B0D12), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(6.dp))
            }
            PillButton(
                label = stop?.label ?: "—", accent = Gold, filled = false, enabled = stop != null,
                onClick = { stop?.let(onPress) }, modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Close, null, tint = Gold, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
            }
        }
    }
}

@Composable
fun ExtrasCard(extras: List<Cmd>, onPress: (Cmd) -> Unit) {
    val accent = Color(0xFF8B93A5)
    CardBox(accent) {
        CardTitle("فرمان‌های دیگر", "EXTRA", accent)
        extras.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { c ->
                    PillButton(
                        label = c.label, accent = accent, filled = false, enabled = true,
                        onClick = { onPress(c) }, modifier = Modifier.weight(1f), height = 48.dp
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
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
                        Text("فرمان ارسالی: ${c.command}", fontSize = 11.sp, color = Color(0xFF8B93A5))
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
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("ویرایش فرمان") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(label, { label = it }, label = { Text("نام دکمه") }, singleLine = true)
                    OutlinedTextField(command, { command = it }, label = { Text("فرمان ارسالی") }, singleLine = true)
                    if (cmd.action == "NONE") {
                        OutlinedTextField(group, { group = it }, label = { Text("گروه") }, singleLine = true)
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
