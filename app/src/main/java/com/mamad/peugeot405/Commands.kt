package com.mamad.peugeot405

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Cmd(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val command: String,
    val group: String
)

val DEFAULT_COMMANDS = listOf(
    Cmd("fu", "جلو بالا", "F", "تعلیق جلو"),
    Cmd("fd", "جلو پایین", "B", "تعلیق جلو"),
    Cmd("ru", "عقب بالا", "R", "تعلیق عقب"),
    Cmd("rd", "عقب پایین", "L", "تعلیق عقب"),
    Cmd("ms", "شروع میباخ", "M", "میباخ"),
    Cmd("mx", "توقف میباخ", "N", "میباخ"),
    Cmd("c1", "فرمان 1", "1", "سایر"),
    Cmd("c2", "فرمان 2", "2", "سایر"),
    Cmd("cz", "فرمان Z", "Z", "سایر"),
    Cmd("cx", "فرمان X", "X", "سایر"),
    Cmd("co", "فرمان o", "o", "سایر"),
    Cmd("c0", "فرمان 0", "0", "سایر")
)

class CommandStore(context: Context) {
    private val prefs = context.getSharedPreferences("commands", Context.MODE_PRIVATE)

    fun load(): List<Cmd> {
        val raw = prefs.getString("list", null) ?: return DEFAULT_COMMANDS
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Cmd(o.getString("id"), o.getString("label"), o.getString("command"), o.getString("group"))
            }
        }.getOrDefault(DEFAULT_COMMANDS)
    }

    fun save(list: List<Cmd>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("label", it.label).put("command", it.command).put("group", it.group))
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }

    fun reset() { prefs.edit().remove("list").apply() }
}
