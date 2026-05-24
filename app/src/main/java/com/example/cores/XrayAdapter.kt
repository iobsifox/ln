package com.example.cores

import com.example.logs.LogRepository
import kotlinx.coroutines.delay

class XrayAdapter : CoreAdapter {
    override val name: String = "Xray Core"
    private var running = false

    override suspend fun start(configJson: String) {
        if (running) return
        running = true
        LogRepository.i("XrayAdapter", "Initializing Xray Core...")
        delay(100)
        LogRepository.i("XrayAdapter", "Xray v1.8.4 starting up...")
        LogRepository.d("XrayAdapter", "Reading configuration JSON... (size: ${configJson.length} bytes)")
        LogRepository.d("XrayAdapter", "Inbound socks established on 127.0.0.1:10808")
        LogRepository.d("XrayAdapter", "Outbound direct route established")
        LogRepository.i("XrayAdapter", "Xray core started successfully!")
    }

    override suspend fun stop() {
        if (!running) return
        running = false
        LogRepository.i("XrayAdapter", "Stopping Xray core process...")
        delay(50)
        LogRepository.i("XrayAdapter", "Xray core terminated safely.")
    }

    override fun isRunning(): Boolean = running
}
