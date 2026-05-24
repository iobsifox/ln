package com.example.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.cores.CoreManager
import com.example.data.AppSettings
import com.example.data.ConnectionMode
import com.example.data.SettingsRepository
import com.example.logs.LogRepository
import com.example.proxy.HttpProxyServer
import com.example.proxy.Socks5ProxyServer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

class LastNightVpnService : VpnService() {

    companion object {
        private const val TAG = "VpnService"
        private const val CHANNEL_ID = "last_night_vpn_channel"
        private const val NOTIFICATION_ID = 114514

        const val ACTION_START = "com.example.vpn.START"
        const val ACTION_STOP = "com.example.vpn.STOP"

        private val _isRunningFlow = MutableStateFlow(false)
        val isRunningFlow: StateFlow<Boolean> = _isRunningFlow
    }

    private var httpProxy: HttpProxyServer? = null
    private var socksProxy: Socks5ProxyServer? = null
    private var serviceJob = Job()
    private var serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var pfd: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        startVpn()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }

    private fun startVpn() {
        if (_isRunningFlow.value) return
        _isRunningFlow.value = true

        serviceJob = Job()
        serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

        // Show foreground notification immediately to comply with Android foreground requirements
        startForeground(NOTIFICATION_ID, buildNotification("Establishing Last Night connection..."))

        LogRepository.i(TAG, "Starting VPN service...")

        serviceScope.launch {
            try {
                // Read fresh settings from DataStore Preferences
                val repository = SettingsRepository(applicationContext)
                val settings = repository.settingsFlow.first()

                LogRepository.i(TAG, "Initializing interface builder for Mode: ${settings.connectionMode}")

                val builder = Builder()
                    .setSession("Last Night")
                    .setMtu(1500)
                    .addAddress("10.0.0.2", 24)
                    .addRoute("0.0.0.0", 0)

                // Configure DNS server addresses
                when (settings.connectionMode) {
                    ConnectionMode.DOH_DIRECT -> {
                        // Bind local virtual DNS or direct provider resolving
                        builder.addDnsServer("1.1.1.1")
                        builder.addDnsServer("8.8.8.8")
                    }
                    ConnectionMode.DOH_WORKER -> {
                        builder.addDnsServer("9.9.9.9")
                    }
                    else -> {
                        // Fallback
                        builder.addDnsServer("8.8.8.8")
                    }
                }

                // Apply dynamic per-app split tunnel rules
                var rulesApplied = 0
                if (settings.selectedTunnelApps.isNotEmpty()) {
                    LogRepository.i(TAG, "Configuring Allowed Tunnel app guidelines:")
                    for (pkg in settings.selectedTunnelApps) {
                        try {
                            builder.addAllowedApplication(pkg)
                            rulesApplied++
                            LogRepository.d(TAG, "  + Allowed App: $pkg")
                        } catch (e: Exception) {
                            LogRepository.e(TAG, "Skipped allowed app $pkg: ${e.message}")
                        }
                    }
                } else if (settings.selectedBypassApps.isNotEmpty()) {
                    LogRepository.i(TAG, "Configuring Disallowed / Bypass app guidelines:")
                    for (pkg in settings.selectedBypassApps) {
                        try {
                            builder.addDisallowedApplication(pkg)
                            rulesApplied++
                            LogRepository.d(TAG, "  - Disallowed App: $pkg")
                        } catch (e: Exception) {
                            LogRepository.e(TAG, "Skipped bypass app $pkg: ${e.message}")
                        }
                    }
                }

                LogRepository.i(TAG, "VPN Split tunnels setup completed. Applied $rulesApplied per-app boundaries.")

                // Create the system TUN interface (requires VpnService system confirmation)
                pfd = builder.establish()
                if (pfd == null) {
                    LogRepository.e(TAG, "Android VPN interface establishment failed (pfd is null). Please grant VPN system access permissions.")
                    stopSelf()
                    return@launch
                }

                LogRepository.i(TAG, "Virtual TUN interface established (IPv4: 10.0.0.2/24)")

                // Engage background packet engine
                TunEngine.start(pfd)

                // Launch internal socks/HTTP proxies if enabled
                if (settings.localHttpProxyEnabled) {
                    httpProxy = HttpProxyServer(8080, settings.lanShareEnabled)
                    httpProxy?.start()
                }

                if (settings.localSocks5Enabled) {
                    socksProxy = Socks5ProxyServer(1080, settings.lanShareEnabled)
                    socksProxy?.start()
                }

                // Start external proxy core adapters if mode requests it
                startExternalCores(settings)

                // Update notification text to connected status
                val mng = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                mng.notify(NOTIFICATION_ID, buildNotification("Last Night VPN Connected | Mode: ${settings.connectionMode}"))

                LogRepository.i(TAG, "VPN tunnel is fully active and routing traffic.")

            } catch (e: Exception) {
                LogRepository.e(TAG, "Fatal VPN initialization error: ${e.message}")
                stopVpn()
            }
        }
    }

    private suspend fun startExternalCores(settings: AppSettings) {
        val mode = settings.connectionMode
        if (mode == ConnectionMode.PSIPHON || 
            mode == ConnectionMode.PSIPHON_DOH_DIRECT || 
            mode == ConnectionMode.PSIPHON_DOH_WORKER) {
            CoreManager.startCore("psiphon")
        }

        if (settings.externalCoresEnabled) {
            if (settings.xrayEnabled) {
                CoreManager.startCore("xray")
            }
            if (settings.singBoxEnabled) {
                CoreManager.startCore("singbox")
            }
        }
    }

    private fun stopVpn() {
        if (!_isRunningFlow.value) return
        _isRunningFlow.value = false

        LogRepository.i(TAG, "Shutting down VPN tunnel...")

        serviceJob.cancel()
        
        // Stop components
        TunEngine.stop()
        
        httpProxy?.stop()
        httpProxy = null
        
        socksProxy?.stop()
        socksProxy = null

        CoroutineScope(Dispatchers.IO).launch {
            CoreManager.stopAll()
        }

        try {
            pfd?.close()
        } catch (e: Exception) {
            // ignore
        }
        pfd = null

        stopForeground(true)
        stopSelf()
        LogRepository.i(TAG, "VPN service safely stopped.")
    }

    private fun buildNotification(text: String): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_share) // fallback
            .setContentTitle("Last Night")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Last Night VPN Tunnel"
            val descriptionText = "Virtual active interface status logs"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
