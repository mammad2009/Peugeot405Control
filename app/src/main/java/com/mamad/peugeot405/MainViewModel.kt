package com.mamad.peugeot405

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
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
    private val prefs = app.getSharedPreferences("bt", Context.MODE_PRIVATE)

    // شبیه‌ساز کد آردوینو؛ انیمیشن ماشین از روی آن می‌آید
    val sim = SketchSim()

    var commands by mutableStateOf(store.load()); private set
    var devices by mutableStateOf<List<BluetoothDevice>>(emptyList()); private set
    var connected by mutableStateOf(false); private set
    var connecting by mutableStateOf(false); private set
    // وضعیت قفل ماکت از روی آخرین دکمه‌ی ریموت (null = هنوز چیزی نفرستاده‌ایم)
    var locked by mutableStateOf<Boolean?>(null); private set
    var status by mutableStateOf("متصل نیست"); private set
    var btSupported by mutableStateOf(true); private set
    var btPermission by mutableStateOf(true); private set
    var btEnabled by mutableStateOf(true); private set
    var lastAddress by mutableStateOf(prefs.getString("last", "") ?: ""); private set

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
        refreshDevices()
    }

    fun refreshDevices() {
        btSupported = bt.isSupported
        btPermission = bt.hasPermission()
        btEnabled = bt.isEnabled
        devices = bt.pairedDevices().sortedByDescending { it.address == lastAddress }
    }

    private fun friendly(e: Throwable): String {
        val m = e.message ?: e.javaClass.simpleName
        return when {
            e is SecurityException -> "اجازه‌ی بلوتوث داده نشده"
            m.contains("read failed", true) || m.contains("timeout", true) || m.contains("closed", true) ->
                "HC-05 جواب نداد. روشن بودنش را چک کن و اینکه به گوشی دیگری وصل نباشد ($m)"
            else -> m
        }
    }

    fun connect(d: BluetoothDevice) = viewModelScope.launch {
        if (connecting) return@launch
        connecting = true
        status = "در حال اتصال..."
        bt.connect(d)
            .onSuccess {
                connected = true
                locked = null
                status = "متصل به ${d.name}"
                lastAddress = d.address
                prefs.edit().putString("last", d.address).apply()
            }
            .onFailure { connected = false; status = "اتصال ناموفق: ${friendly(it)}" }
        connecting = false
    }

    // اتصال سریع به آخرین دستگاه (یا تنها دستگاه جفت‌شده). اگر نشد false برمی‌گرداند تا لیست نمایش داده شود
    fun connectQuick(): Boolean {
        refreshDevices()
        if (!btSupported || !btPermission || !btEnabled) return false
        val d = devices.firstOrNull { it.address == lastAddress } ?: devices.singleOrNull() ?: return false
        connect(d)
        return true
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

    // فشردن یک دکمه: هم فرمان می‌فرستد، هم همان را در شبیه‌ساز اجرا می‌کند (حتی بدون اتصال)
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

    private fun cmdFor(action: String): Cmd? = commands.firstOrNull { it.action == action }

    // فرمان‌های چندبایتی (مثل «هر دو بالا»: هر بایت جدا و با فاصله‌ی کوتاه فرستاده می‌شود)
    fun pressMulti(c: Cmd) {
        viewModelScope.launch {
            for (ch in c.command) {
                send(ch.toString())
                sim.receive(ch)
                delay(40)
            }
        }
    }

    // ارتفاع فابریک = وضعیت استراحتِ سرووها (جلو 80، عقب 84). اگر میباخ روشن است با فرمان توقفش برمی‌گردد
    fun factory(c: Cmd) {
        if (sim.maybachOn) cmdFor("M_OFF")?.let { press(it) } else pressMulti(c)
    }

    // ریموت پژو
    fun remoteLock() {
        send(cmdFor("LOCK")?.command ?: "K")
        if (connected) locked = true
    }

    fun remoteUnlock() {
        send(cmdFor("UNLOCK")?.command ?: "U")
        if (connected) locked = false
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
