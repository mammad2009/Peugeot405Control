package com.mamad.peugeot405

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = CommandStore(app)
    private val bt = BluetoothClient(app)

    // شبیه‌ساز کد آردوینو؛ انیمیشن ماشین از روی آن می‌آید
    val sim = SketchSim()

    var commands by mutableStateOf(store.load()); private set
    var devices by mutableStateOf<List<BluetoothDevice>>(emptyList()); private set
    var connected by mutableStateOf(false); private set
    var status by mutableStateOf("متصل نیست"); private set

    // صف ارسال: بایت‌ها دقیقاً به ترتیب فشردن/رها کردن فرستاده می‌شوند
    private val outbox = Channel<String>(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            for (t in outbox) {
                bt.send(t).onFailure { connected = false; status = "ارسال ناموفق بود، اتصال قطع شد" }
            }
        }
        viewModelScope.launch {
            while (true) {
                sim.tick(SystemClock.uptimeMillis())
                delay(10)
            }
        }
    }

    fun refreshDevices() { devices = bt.pairedDevices() }

    fun connect(d: BluetoothDevice) = viewModelScope.launch {
        status = "در حال اتصال..."
        bt.connect(d)
            .onSuccess { connected = true; status = "متصل به ${d.name}" }
            .onFailure { connected = false; status = "اتصال ناموفق بود" }
    }

    fun disconnect() { bt.disconnect(); connected = false; status = "متصل نیست" }

    fun send(text: String) {
        if (!connected) { status = "اول به HC-05 وصل شو"; return }
        outbox.trySend(text)
    }

    // کاراکتری که شبیه‌ساز باید ببیند (مطابق کد آردوینو، مستقل از فرمانی که کاربر ویرایش کرده)
    private fun canon(c: Cmd): Char = when (c.action) {
        "F_UP_S" -> 'F'
        "F_DN_S" -> 'B'
        "R_UP_S" -> 'R'
        "R_DN_S" -> 'L'
        "F_UP_F" -> '2'
        "F_DN_F" -> '1'
        "R_UP_F" -> 'Z'
        "R_DN_F" -> 'X'
        "M_ON" -> 'M'
        "M_OFF" -> 'N'
        "STOP" -> '0'
        else -> c.command.firstOrNull() ?: '\u0000'
    }

    // فشردن دکمه: هم فرمان می‌فرستد، هم همان را در شبیه‌ساز اجرا می‌کند (حتی بدون اتصال)
    fun press(c: Cmd) {
        send(c.command)
        sim.receive(canon(c))
    }

    // رها کردن دکمه‌ی حرکت آرام: فرمان توقف (در کد آردوینو هر کاراکتر دیگری حرکت را متوقف می‌کند)
    fun pressUp() {
        val stop = commands.firstOrNull { it.action == "STOP" }
        send(stop?.command ?: "0")
        sim.receive('0')
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
