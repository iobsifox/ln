package com.example.profiles

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProxyProfile(
    val id: String,
    val name: String,
    val type: String,
    val server: String,
    val port: Int,
    val isSelected: Boolean = false
)

object ProfileManager {
    private val defaultProfiles = listOf(
        ProxyProfile("1", "Default Cloudflare Warp", "WARP", "engage.cloudflareclient.com", 2408, true),
        ProxyProfile("2", "US East Gateway", "VLESS", "us-east.lastnight-edge.net", 443, false),
        ProxyProfile("3", "Europe Retail Node", "VMESS", "eu-retail.lastnight-edge.net", 853, false),
        ProxyProfile("4", "Psiphon Default Edge", "PSIPHON", "psiphon-anycast.com", 1080, false)
    )

    private val _profiles = MutableStateFlow<List<ProxyProfile>>(defaultProfiles)
    val profiles: StateFlow<List<ProxyProfile>> = _profiles.asStateFlow()

    fun selectProfile(id: String) {
        val updated = _profiles.value.map {
            it.copy(isSelected = (it.id == id))
        }
        _profiles.value = updated
    }

    fun addProfile(name: String, type: String, server: String, port: Int) {
        val newProfile = ProxyProfile(
            id = System.currentTimeMillis().toString(),
            name = name,
            type = type.uppercase(),
            server = server,
            port = port,
            isSelected = false
        )
        _profiles.value = _profiles.value + newProfile
    }

    fun deleteProfile(id: String) {
        // Prevent deleting selected profile or re-select first
        val list = _profiles.value.filterNot { it.id == id }
        if (list.none { it.isSelected } && list.isNotEmpty()) {
            _profiles.value = list.mapIndexed { idx, p -> p.copy(isSelected = (idx == 0)) }
        } else {
            _profiles.value = list
        }
    }
}
