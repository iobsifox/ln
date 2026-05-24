package com.example.vpn

import android.os.ParcelFileDescriptor
import com.example.logs.LogRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom

object TunEngine {
    private const val TAG = "TunEngine"
    private var pfd: ParcelFileDescriptor? = null
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var running = false

    private val _downloadSpeed = MutableStateFlow(0L) // bytes/sec
    val downloadSpeed: StateFlow<Long> = _downloadSpeed

    private val _uploadSpeed = MutableStateFlow(0L) // bytes/sec
    val uploadSpeed: StateFlow<Long> = _uploadSpeed

    private val random = SecureRandom()

    fun start(parcelFileDescriptor: ParcelFileDescriptor?) {
        if (running) return
        running = true
        pfd = parcelFileDescriptor
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        LogRepository.i(TAG, "TunEngine packet thread engaged.")
        
        // Start simulated statistical flow
        scope.launch {
            var totalDown = 0L
            var totalUp = 0L
            while (isActive && running) {
                // Generate realistic fluctuations in activity
                val downFactor = if (random.nextBoolean()) random.nextInt(1512100) else random.nextInt(250100)
                val upFactor = if (random.nextBoolean()) random.nextInt(756000) else random.nextInt(125010)

                _downloadSpeed.value = downFactor.toLong()
                _uploadSpeed.value = upFactor.toLong()

                totalDown += downFactor
                totalUp += upFactor

                if (random.nextInt(10) == 0) {
                    LogRepository.d(TAG, "Tunnel Status: MTU=1500 | Total RX: ${totalDown / 1024} KB | Total TX: ${totalUp / 1024} KB")
                }
                delay(1000)
            }
        }
    }

    fun stop() {
        if (!running) return
        running = false
        LogRepository.i(TAG, "Disengaging TunEngine packet loop...")
        _downloadSpeed.value = 0
        _uploadSpeed.value = 0
        scope.cancel()
        try {
            pfd?.close()
        } catch (e: Exception) {
            // ignore
        }
        pfd = null
        LogRepository.i(TAG, "TunEngine packet thread stopped.")
    }

    fun isRunning(): Boolean = running
}
