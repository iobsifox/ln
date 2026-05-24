package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cores.CoreManager
import com.example.data.AppSettings
import com.example.data.ConnectionMode
import com.example.data.SettingsRepository
import com.example.dns.DohClient
import com.example.logs.LogEntry
import com.example.logs.LogRepository
import com.example.profiles.ProfileManager
import com.example.profiles.ProxyProfile
import com.example.vpn.AppRoutingManager
import com.example.vpn.AppItem
import com.example.vpn.LastNightVpnService
import com.example.vpn.TunEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(private val context: Context) : ViewModel() {

    private val repository = SettingsRepository(context)

    // Expose local settings state
    val settingsState: StateFlow<AppSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    // Vpn Service active status
    val isVpnActive: StateFlow<Boolean> = LastNightVpnService.isRunningFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Download / Upload speeds
    val downloadSpeed: StateFlow<Long> = TunEngine.downloadSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val uploadSpeed: StateFlow<Long> = TunEngine.uploadSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Profile configurations
    val profiles: StateFlow<List<ProxyProfile>> = ProfileManager.profiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live terminal log files
    private val _logsState = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsState: StateFlow<List<LogEntry>> = _logsState.asStateFlow()

    // Package applications list
    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps.asStateFlow()

    // DNS test query field output
    private val _dnsTestResult = MutableStateFlow<String>("")
    val dnsTestResult: StateFlow<String> = _dnsTestResult.asStateFlow()

    private val _isResolvingDns = MutableStateFlow(false)
    val isResolvingDns: StateFlow<Boolean> = _isResolvingDns.asStateFlow()

    // Cores operational status
    private val _xrayRunning = MutableStateFlow(false)
    val xrayRunning: StateFlow<Boolean> = _xrayRunning.asStateFlow()

    private val _singBoxRunning = MutableStateFlow(false)
    val singBoxRunning: StateFlow<Boolean> = _singBoxRunning.asStateFlow()

    private val _psiphonRunning = MutableStateFlow(false)
    val psiphonRunning: StateFlow<Boolean> = _psiphonRunning.asStateFlow()

    init {
        // Collect logs reactively
        viewModelScope.launch {
            LogRepository.logsFlow.collect {
                _logsState.value = it
            }
        }
        // Initialize logs
        _logsState.value = LogRepository.getLogs()

        // Fetch installed applications on a background IO thread
        viewModelScope.launch(Dispatchers.IO) {
            _installedApps.value = AppRoutingManager.getInstalledApps(context)
        }

        // Poll core running status
        viewModelScope.launch {
            while (true) {
                _xrayRunning.value = CoreManager.xray.isRunning()
                _singBoxRunning.value = CoreManager.singBox.isRunning()
                _psiphonRunning.value = CoreManager.psiphon.isRunning()
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    // Settings modifiers
    fun setConnectionMode(mode: ConnectionMode) = viewModelScope.launch {
        repository.updateConnectionMode(mode)
    }

    fun setDohProvider(provider: String) = viewModelScope.launch {
        repository.updateDohDirectProvider(provider)
    }

    fun setDohWorkerUrl(url: String) = viewModelScope.launch {
        repository.updateDohWorkerUrl(url)
    }

    fun setDohWorkerUpstream(upstream: String) = viewModelScope.launch {
        repository.updateDohWorkerUpstream(upstream)
    }

    fun toggleHttpProxy(enabled: Boolean) = viewModelScope.launch {
        repository.updateLocalHttpProxyEnabled(enabled)
    }

    fun toggleSocksProxy(enabled: Boolean) = viewModelScope.launch {
        repository.updateLocalSocks5Enabled(enabled)
    }

    fun toggleLanSharing(enabled: Boolean) = viewModelScope.launch {
        repository.updateLanShareEnabled(enabled)
    }

    fun toggleVpnEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateVpnEnabled(enabled)
    }

    fun toggleExternalCores(enabled: Boolean) = viewModelScope.launch {
        repository.updateExternalCoresEnabled(enabled)
    }

    fun toggleXrayAdapter(enabled: Boolean) = viewModelScope.launch {
        repository.updateXrayEnabled(enabled)
        if (enabled) {
            CoreManager.startCore("xray")
        } else {
            CoreManager.stopCore("xray")
        }
    }

    fun toggleSingBoxAdapter(enabled: Boolean) = viewModelScope.launch {
        repository.updateSingBoxEnabled(enabled)
        if (enabled) {
            CoreManager.startCore("singbox")
        } else {
            CoreManager.stopCore("singbox")
        }
    }

    fun togglePsiphonAdapter(enabled: Boolean) = viewModelScope.launch {
        repository.updatePsiphonEnabled(enabled)
        if (enabled) {
            CoreManager.startCore("psiphon")
        } else {
            CoreManager.stopCore("psiphon")
        }
    }

    // Per-app split routing operations
    fun toggleAppTunnel(packageName: String) = viewModelScope.launch {
        val currentSet = settingsState.value.selectedTunnelApps.toMutableSet()
        if (currentSet.contains(packageName)) {
            currentSet.remove(packageName)
        } else {
            currentSet.add(packageName)
            // Remove from bypass list if added here
            removeFromBypassList(packageName)
        }
        repository.updateSelectedTunnelApps(currentSet)
    }

    fun toggleAppBypass(packageName: String) = viewModelScope.launch {
        val currentSet = settingsState.value.selectedBypassApps.toMutableSet()
        if (currentSet.contains(packageName)) {
            currentSet.remove(packageName)
        } else {
            currentSet.add(packageName)
            // Remove from tunnel list if added here
            removeFromTunnelList(packageName)
        }
        repository.updateSelectedBypassApps(currentSet)
    }

    private suspend fun removeFromTunnelList(packageName: String) {
        val currentSet = settingsState.value.selectedTunnelApps.toMutableSet()
        if (currentSet.contains(packageName)) {
            currentSet.remove(packageName)
            repository.updateSelectedTunnelApps(currentSet)
        }
    }

    private suspend fun removeFromBypassList(packageName: String) {
        val currentSet = settingsState.value.selectedBypassApps.toMutableSet()
        if (currentSet.contains(packageName)) {
            currentSet.remove(packageName)
            repository.updateSelectedBypassApps(currentSet)
        }
    }

    // Profiles operation
    fun selectProfile(id: String) {
        ProfileManager.selectProfile(id)
        LogRepository.i("Model", "Proxy profile configuration swapped.")
    }

    fun addProfile(name: String, type: String, server: String, port: Int) {
        ProfileManager.addProfile(name, type, server, port)
        LogRepository.i("Model", "Manually generated proxy profile added: $name ($type)")
    }

    fun deleteProfile(id: String) {
        ProfileManager.deleteProfile(id)
        LogRepository.w("Model", "Removed custom proxy configuration profile.")
    }

    // Live VPN launch methods
    fun toggleVpnServiceState() {
        val active = isVpnActive.value
        val intent = Intent(context, LastNightVpnService::class.java)
        if (active) {
            intent.action = LastNightVpnService.ACTION_STOP
            context.startService(intent)
        } else {
            intent.action = LastNightVpnService.ACTION_START
            // Start foreground requires service binding
            context.startService(intent)
        }
    }

    // Quick active DNS test resolve
    fun executeDnsTest(domain: String) = viewModelScope.launch {
        if (domain.isBlank()) {
            _dnsTestResult.value = "Please input a valid domain host address (e.g. google.com)"
            return@launch
        }
        _isResolvingDns.value = true
        _dnsTestResult.value = "Resolving query domain '$domain'..."

        val settings = settingsState.value
        withContext(Dispatchers.IO) {
            try {
                val results = if (settings.connectionMode == ConnectionMode.DOH_WORKER) {
                    DohClient.resolveWorker(
                        domain = domain,
                        workerUrl = settings.dohWorkerUrl,
                        upstreamKey = settings.dohWorkerUpstream
                    )
                } else {
                    DohClient.resolveDirect(
                        domain = domain,
                        providerKey = settings.dohDirectProvider
                    )
                }
                
                if (results.isNotEmpty()) {
                    _dnsTestResult.value = "SUCCESS: Found ${results.size} IP answers:\n" + 
                        results.joinToString("\n") { " -> $it" }
                } else {
                    _dnsTestResult.value = "RESOLVED COMPLETE: No record addresses returned."
                }
            } catch (e: Exception) {
                _dnsTestResult.value = "RESOLVING FAILED: ${e.message}"
            } finally {
                _isResolvingDns.value = false
            }
        }
    }

    fun clearLiveLogs() {
        LogRepository.clear()
    }
}
