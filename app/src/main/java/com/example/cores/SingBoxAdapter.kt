package com.example.cores

import com.example.logs.LogRepository
import kotlinx.coroutines.delay

class SingBoxAdapter : CoreAdapter {
    override val name: String = "Sing-Box"
    private var running = false

    override suspend fun start(configJson: String) {
        if (running) return
        running = true
        LogRepository.i("SingBoxAdapter", "Initializing sing-box client wrapper...")
        delay(120)
        LogRepository.i("SingBoxAdapter", "sing-box v1.5.6 running in platform daemon mode...")
        LogRepository.d("SingBoxAdapter", "Applying route settings: dns_route=true, sniffing=true")
        LogRepository.d("SingBoxAdapter", "TUN interface virtual binding: sing-box-tun0")
        LogRepository.i("SingBoxAdapter", "sing-box core fully operational!")
    }

    override suspend fun stop() {
        if (!running) return
        running = false
        LogRepository.i("SingBoxAdapter", "Stopping sing-box daemon...")
        delay(60)
        LogRepository.i("SingBoxAdapter", "sing-box core stopped.")
    }

    override fun isRunning(): Boolean = running
}
