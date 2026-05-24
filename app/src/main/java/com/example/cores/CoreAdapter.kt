package com.example.cores

interface CoreAdapter {
    val name: String
    suspend fun start(configJson: String)
    suspend fun stop()
    fun isRunning(): Boolean
}
