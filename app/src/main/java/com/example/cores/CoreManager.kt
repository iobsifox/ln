package com.example.cores

import com.example.logs.LogRepository

object CoreManager {
    val xray = XrayAdapter()
    val singBox = SingBoxAdapter()
    val psiphon = PsiphonAdapter()

    suspend fun startCore(coreName: String, configJson: String = "") {
        LogRepository.i("CoreManager", "Instruction received to start core: $coreName")
        when (coreName.lowercase()) {
            "xray" -> xray.start(configJson)
            "singbox", "sing-box" -> singBox.start(configJson)
            "psiphon" -> psiphon.start(configJson)
            else -> LogRepository.e("CoreManager", "Unknown core name requested: $coreName")
        }
    }

    suspend fun stopCore(coreName: String) {
        LogRepository.i("CoreManager", "Instruction received to stop core: $coreName")
        when (coreName.lowercase()) {
            "xray" -> xray.stop()
            "singbox", "sing-box" -> singBox.stop()
            "psiphon" -> psiphon.stop()
            else -> LogRepository.e("CoreManager", "Unknown core name requested: $coreName")
        }
    }

    suspend fun stopAll() {
        LogRepository.i("CoreManager", "Stopping all running external engine cores...")
        if (xray.isRunning()) xray.stop()
        if (singBox.isRunning()) singBox.stop()
        if (psiphon.isRunning()) psiphon.stop()
    }
}
