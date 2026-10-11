package com.mamad.peugeot405

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// action: نقش دکمه در انیمیشن ماشین (ثابت می‌ماند حتی اگر فرمان ارسالی را عوض کنی)
data class Cmd(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val command: String,
    val group: String,
    val action: String = "NONE"
)

val DEFAULT_COMMANDS = listOf(
    Cmd("f_up_s", "خیز آرام", "F", "جلو", "F_UP_S"),
    Cmd("f_dn_s", "نشست آرام", "B", "جلو", "F_DN_S"),
    Cmd("f_up_f", "خیز سریع", "2", "جلو", "F_UP_F"),
    Cmd("f_dn_f", "نشست سریع", "1", "جلو", "F_DN_F"),
    Cmd("r_up_s", "خیز آرام", "R", "عقب", "R_UP_S"),
    Cmd("r_dn_s", "نشست آرام", "L", "عقب", "R_DN_S"),
    Cmd("r_up_f", "خیز سریع", "Z", "عقب", "R_UP_F"),
    Cmd("r_dn_f", "نشست سریع", "X", "عقب", "R_DN_F"),
    Cmd("m_on", "شروع میباخ", "M", "میباخ", "M_ON"),
    Cmd("m_off", "توقف", "N", "میباخ", "M_OFF"),
    Cmd("stop", "رها کردن دکمه (توقف)", "0", "سیستمی", "STOP")
)

class CommandStore(context: Context) {
    private val prefs = context.getSharedPreferences("commands", Context.MODE_PRIVATE)
    private val key = "list_v3"

    fun load(): List<Cmd> {
        val raw = prefs.getString(key, null) ?: return DEFAULT_COMMANDS
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Cmd(
                    o.getString("id"), o.getString("label"), o.getString("command"),
                    o.getString("group"), o.optString("action", "NONE")
                )
            }
        }.getOrDefault(DEFAULT_COMMANDS)
    }

    fun save(list: List<Cmd>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("label", it.label)
                    .put("command", it.command).put("group", it.group).put("action", it.action)
            )
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun reset() { prefs.edit().remove(key).apply() }
}
