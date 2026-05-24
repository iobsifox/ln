package com.example.data

enum class ConnectionMode {
    DOH_DIRECT,
    DOH_WORKER,
    PSIPHON,
    PSIPHON_DOH_DIRECT,
    PSIPHON_DOH_WORKER
}

data class AppSettings(
    val connectionMode: ConnectionMode = ConnectionMode.DOH_DIRECT,
    val dohDirectProvider: String = "cloudflare",
    val dohWorkerUrl: String = "",
    val dohWorkerUpstream: String = "cloudflare",
    val localHttpProxyEnabled: Boolean = true,
    val localSocks5Enabled: Boolean = true,
    val lanShareEnabled: Boolean = false,
    val vpnEnabled: Boolean = false,
    val externalCoresEnabled: Boolean = false,
    val xrayEnabled: Boolean = false,
    val singBoxEnabled: Boolean = false,
    val psiphonEnabled: Boolean = false,
    val selectedTunnelApps: Set<String> = emptySet(),
    val selectedBypassApps: Set<String> = emptySet()
)
