package com.mamad.peugeot405

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** App-wide language state. English=false means Persian; persisted by MainActivity. */
object AppLanguage {
    var english by mutableStateOf(false)
}

fun tr(persian: String, english: String): String = if (AppLanguage.english) english else persian
