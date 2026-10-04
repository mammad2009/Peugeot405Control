package com.mamad.peugeot405

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = CommandStore(app)
    private val bt = BluetoothClient(app)

    var commands by mutableStateOf(store.load()); private set
    var devices by mutableStateOf<List<BluetoothDevice>>(emptyList()); private set
    var connected by mutableStateOf(false); private set
    var status by mutableStateOf("متصل نیست"); private set

    fun refreshDevices() { devices = bt.pairedDevices() }

    fun connect(d: BluetoothDevice) = viewModelScope.launch {
        status = "در حال اتصال..."
        bt.connect(d)
            .onSuccess { connected = true; status = "متصل به ${d.name}" }
            .onFailure { connected = false; status = "اتصال ناموفق بود" }
    }

    fun disconnect() { bt.disconnect(); connected = false; status = "متصل نیست" }

    fun send(text: String) = viewModelScope.launch {
        if (!connected) { status = "اول به HC-05 وصل شو"; return@launch }
        bt.send(text).onFailure { connected = false; status = "ارسال ناموفق بود، اتصال قطع شد" }
    }

    fun upsert(c: Cmd) {
        val i = commands.indexOfFirst { it.id == c.id }
        commands = if (i >= 0) commands.toMutableList().also { it[i] = c } else commands + c
        store.save(commands)
    }

    fun delete(id: String) { commands = commands.filter { it.id != id }; store.save(commands) }

    fun resetDefaults() { store.reset(); commands = DEFAULT_COMMANDS }

    override fun onCleared() { bt.disconnect() }
}
