package com.mamad.peugeot405

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refreshDevices() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 31) permLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = darkColorScheme()) { AppRoot(vm) }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: MainViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    var showConnect by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("405 Control"); Text(vm.status, style = MaterialTheme.typography.labelSmall) } },
                actions = {
                    TextButton(onClick = { vm.refreshDevices(); showConnect = true }) {
                        Text(if (vm.connected) "تغییر" else "اتصال")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Home, null) }, label = { Text("کنترل") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Settings, null) }, label = { Text("فرمان‌ها") })
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) { if (tab == 0) ControlScreen(vm) else SettingsScreen(vm) }
    }
    if (showConnect) {
        AlertDialog(
            onDismissRequest = { showConnect = false },
            confirmButton = { TextButton(onClick = { showConnect = false }) { Text("بستن") } },
            dismissButton = { if (vm.connected) TextButton(onClick = { vm.disconnect(); showConnect = false }) { Text("قطع اتصال") } },
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
fun ControlScreen(vm: MainViewModel) {
    val groups = vm.commands.groupBy { it.group }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        groups.forEach { (g, list) ->
            item { Text(g, style = MaterialTheme.typography.titleMedium) }
            items(list.chunked(2)) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { c ->
                        Button(onClick = { vm.send(c.command) }, modifier = Modifier.weight(1f).height(64.dp)) { Text(c.label) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
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
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(c.label)
                        Text("${c.group}  •  ${c.command}", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { editing = c }) { Icon(Icons.Default.Edit, null) }
                    IconButton(onClick = { vm.delete(c.id) }) { Icon(Icons.Default.Delete, null) }
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
                    OutlinedTextField(label, { label = it }, label = { Text("برچسب دکمه") }, singleLine = true)
                    OutlinedTextField(command, { command = it }, label = { Text("فرمان ارسالی") }, singleLine = true)
                    OutlinedTextField(group, { group = it }, label = { Text("گروه") }, singleLine = true)
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
