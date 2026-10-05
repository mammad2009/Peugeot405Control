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

    // وضعیت انیمیشن ماشین: 0 = پایین، 0.5 = حالت عادی، 1 = بالا
    var frontTarget by mutableStateOf(0.5f); private set
    var rearTarget by mutableStateOf(0.5f); private set
    var frontMs by mutableStateOf(SLOW_MS); private set
    var rearMs by mutableStateOf(SLOW_MS); private set
    var maybach by mutableStateOf(false); private set

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

    // فشردن یک دکمه: هم فرمان می‌فرستد، هم انیمیشن ماشین را اجرا می‌کند (حتی بدون اتصال)
    fun press(c: Cmd) {
        send(c.command)
        when (c.action) {
            "F_UP_S" -> { frontTarget = 1f; frontMs = SLOW_MS }
            "F_DN_S" -> { frontTarget = 0f; frontMs = SLOW_MS }
            "F_UP_F" -> { frontTarget = 1f; frontMs = FAST_MS }
            "F_DN_F" -> { frontTarget = 0f; frontMs = FAST_MS }
            "R_UP_S" -> { rearTarget = 1f; rearMs = SLOW_MS }
            "R_DN_S" -> { rearTarget = 0f; rearMs = SLOW_MS }
            "R_UP_F" -> { rearTarget = 1f; rearMs = FAST_MS }
            "R_DN_F" -> { rearTarget = 0f; rearMs = FAST_MS }
            "M_ON" -> maybach = true
            "M_OFF" -> maybach = false
        }
    }

    fun upsert(c: Cmd) {
        val i = commands.indexOfFirst { it.id == c.id }
        commands = if (i >= 0) commands.toMutableList().also { it[i] = c } else commands + c
        store.save(commands)
    }

    fun delete(id: String) { commands = commands.filter { it.id != id }; store.save(commands) }

    fun resetDefaults() { store.reset(); commands = DEFAULT_COMMANDS }

    override fun onCleared() { bt.disconnect() }

    companion object {
        const val SLOW_MS = 1500
        const val FAST_MS = 450
    }
}
