package com.example.logs

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: String,
    val level: String,
    val tag: String,
    val message: String
)

object LogRepository {
    private val _logs = mutableListOf<LogEntry>()
    private val _logsFlow = MutableSharedFlow<List<LogEntry>>(replay = 1)
    
    val logsFlow: SharedFlow<List<LogEntry>> = _logsFlow.asSharedFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun log(level: String, tag: String, message: String) {
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            level = level,
            tag = tag,
            message = message
        )
        synchronized(_logs) {
            _logs.add(entry)
            if (_logs.size > 500) {
                _logs.removeAt(0)
            }
            _logsFlow.tryEmit(ArrayList(_logs))
        }
    }

    fun d(tag: String, message: String) = log("DEBUG", tag, message)
    fun i(tag: String, message: String) = log("INFO", tag, message)
    fun w(tag: String, message: String) = log("WARN", tag, message)
    fun e(tag: String, message: String) = log("ERROR", tag, message)

    fun clear() {
        synchronized(_logs) {
            _logs.clear()
            _logsFlow.tryEmit(emptyList())
        }
    }

    fun getLogs(): List<LogEntry> {
        return synchronized(_logs) { ArrayList(_logs) }
    }
}
