package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val CONNECTION_MODE = stringPreferencesKey("connection_mode")
        private val DOH_DIRECT_PROVIDER = stringPreferencesKey("doh_direct_provider")
        private val DOH_WORKER_URL = stringPreferencesKey("doh_worker_url")
        private val DOH_WORKER_UPSTREAM = stringPreferencesKey("doh_worker_upstream")
        private val LOCAL_HTTP_PROXY_ENABLED = booleanPreferencesKey("local_http_proxy_enabled")
        private val LOCAL_SOCKS5_ENABLED = booleanPreferencesKey("local_socks5_enabled")
        private val LAN_SHARE_ENABLED = booleanPreferencesKey("lan_share_enabled")
        private val VPN_ENABLED = booleanPreferencesKey("vpn_enabled")
        private val EXTERNAL_CORES_ENABLED = booleanPreferencesKey("external_cores_enabled")
        private val XRAY_ENABLED = booleanPreferencesKey("xray_enabled")
        private val SING_BOX_ENABLED = booleanPreferencesKey("sing_box_enabled")
        private val PSIPHON_ENABLED = booleanPreferencesKey("psiphon_enabled")
        private val SELECTED_TUNNEL_APPS = stringSetPreferencesKey("selected_tunnel_apps")
        private val SELECTED_BYPASS_APPS = stringSetPreferencesKey("selected_bypass_apps")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            connectionMode = ConnectionMode.valueOf(
                preferences[CONNECTION_MODE] ?: ConnectionMode.DOH_DIRECT.name
            ),
            dohDirectProvider = preferences[DOH_DIRECT_PROVIDER] ?: "cloudflare",
            dohWorkerUrl = preferences[DOH_WORKER_URL] ?: "",
            dohWorkerUpstream = preferences[DOH_WORKER_UPSTREAM] ?: "cloudflare",
            localHttpProxyEnabled = preferences[LOCAL_HTTP_PROXY_ENABLED] ?: true,
            localSocks5Enabled = preferences[LOCAL_SOCKS5_ENABLED] ?: true,
            lanShareEnabled = preferences[LAN_SHARE_ENABLED] ?: false,
            vpnEnabled = preferences[VPN_ENABLED] ?: false,
            externalCoresEnabled = preferences[EXTERNAL_CORES_ENABLED] ?: false,
            xrayEnabled = preferences[XRAY_ENABLED] ?: false,
            singBoxEnabled = preferences[SING_BOX_ENABLED] ?: false,
            psiphonEnabled = preferences[PSIPHON_ENABLED] ?: false,
            selectedTunnelApps = preferences[SELECTED_TUNNEL_APPS] ?: emptySet(),
            selectedBypassApps = preferences[SELECTED_BYPASS_APPS] ?: emptySet()
        )
    }

    suspend fun updateConnectionMode(mode: ConnectionMode) {
        context.dataStore.edit { preferences ->
            preferences[CONNECTION_MODE] = mode.name
        }
    }

    suspend fun updateDohDirectProvider(provider: String) {
        context.dataStore.edit { preferences ->
            preferences[DOH_DIRECT_PROVIDER] = provider
        }
    }

    suspend fun updateDohWorkerUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[DOH_WORKER_URL] = url
        }
    }

    suspend fun updateDohWorkerUpstream(upstream: String) {
        context.dataStore.edit { preferences ->
            preferences[DOH_WORKER_UPSTREAM] = upstream
        }
    }

    suspend fun updateLocalHttpProxyEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LOCAL_HTTP_PROXY_ENABLED] = enabled
        }
    }

    suspend fun updateLocalSocks5Enabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LOCAL_SOCKS5_ENABLED] = enabled
        }
    }

    suspend fun updateLanShareEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LAN_SHARE_ENABLED] = enabled
        }
    }

    suspend fun updateVpnEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VPN_ENABLED] = enabled
        }
    }

    suspend fun updateExternalCoresEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[EXTERNAL_CORES_ENABLED] = enabled
        }
    }

    suspend fun updateXrayEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[XRAY_ENABLED] = enabled
        }
    }

    suspend fun updateSingBoxEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SING_BOX_ENABLED] = enabled
        }
    }

    suspend fun updatePsiphonEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PSIPHON_ENABLED] = enabled
        }
    }

    suspend fun updateSelectedTunnelApps(appPackageNames: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_TUNNEL_APPS] = appPackageNames
        }
    }

    suspend fun updateSelectedBypassApps(appPackageNames: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_BYPASS_APPS] = appPackageNames
        }
    }
}
