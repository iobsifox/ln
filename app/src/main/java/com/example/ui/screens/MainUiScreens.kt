package com.example.ui.screens

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.data.AppSettings
import com.example.data.ConnectionMode
import com.example.logs.LogEntry
import com.example.logs.LogRepository
import com.example.profiles.ProxyProfile
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.vpn.AppItem
import kotlinx.coroutines.launch

@Composable
fun MainTabContent(
    currentScreen: String,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        when (currentScreen) {
            "home" -> HomeScreen(viewModel)
            "modes" -> ModesScreen(viewModel)
            "dns" -> DnsSettingsScreen(viewModel)
            "sharing" -> LocalSharingScreen(viewModel)
            "apps" -> VpnAppsAccessScreen(viewModel)
            "cores" -> CoresScreen(viewModel)
            "logs" -> LogsScreen(viewModel)
        }
    }
}

// ==========================================
// 1. HOME SCREEN Layout
// ==========================================
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val isRunning by viewModel.isVpnActive.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val dlSpeed by viewModel.downloadSpeed.collectAsState()
    val ulSpeed by viewModel.uploadSpeed.collectAsState()

    val connectionMode = settings.connectionMode

    // Pulsing circle scale animation when active
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRunning) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val neonColor by animateColorAsState(
        targetValue = if (isRunning) SuccessNeon else PurpleNeon,
        animationSpec = tween(500),
        label = "color"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Active Module
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background radial glow
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(neonColor.copy(alpha = 0.15f), Color.Transparent)
                            )
                        )
                )

                // Interactive Circle Hook
                Box(
                    modifier = Modifier
                        .size((160 * pulseScale).dp)
                        .shadow(
                            elevation = if (isRunning) 16.dp else 4.dp,
                            shape = CircleShape,
                            ambientColor = neonColor,
                            spotColor = neonColor
                        )
                        .border(
                            BorderStroke(3.dp, neonColor),
                            CircleShape
                        )
                        .clip(CircleShape)
                        .background(CardDark)
                        .clickable { viewModel.toggleVpnServiceState() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Toggle connection button",
                            tint = neonColor,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRunning) "CONNECTED" else "DISCONNECTED",
                            color = neonColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }
        }

        // Configuration State Summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Connection Status Indicators",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    StatusRow(label = "Connection Mode", value = connectionMode.name, icon = Icons.Default.SettingsInputComponent, iconColor = BlueNeon)
                    Divider(color = BorderDark, modifier = Modifier.padding(vertical = 8.dp))
                    
                    val dnsStateStr = if (connectionMode == ConnectionMode.DOH_WORKER) "Cloudflare Workers" else settings.dohDirectProvider.uppercase()
                    StatusRow(label = "DNS Crypt Resolve", value = dnsStateStr, icon = Icons.Default.Dns, iconColor = CyanNeon)
                }
            }
        }

        // Bitrate Gauge Module
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SpeedCard(
                    title = "DOWNLOAD RATE",
                    speedBytes = dlSpeed,
                    icon = Icons.Default.ArrowDownward,
                    color = SuccessNeon,
                    modifier = Modifier.weight(1f)
                )
                SpeedCard(
                    title = "UPLOAD RATE",
                    speedBytes = ulSpeed,
                    icon = Icons.Default.ArrowUpward,
                    color = BlueNeon,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Proxies Endpoints List
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Active Local Services Sharing",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val httpStatus = if (settings.localHttpProxyEnabled) "ACTIVE (127.0.0.1:8080)" else "DISABLED"
                    val socksStatus = if (settings.localSocks5Enabled) "ACTIVE (127.0.0.1:1080)" else "DISABLED"
                    val lanStatus = if (settings.lanShareEnabled) "ENABLED (0.0.0.0 ALLOW ALL)" else "LOCAL ONLY (127.0.0.1)"

                    StatusRow(
                        label = "HTTP Connecting Tunnel",
                        value = httpStatus,
                        icon = Icons.Default.Http,
                        iconColor = if (settings.localHttpProxyEnabled && isRunning) SuccessNeon else TextMuted
                    )
                    Divider(color = BorderDark, modifier = Modifier.padding(vertical = 8.dp))
                    
                    StatusRow(
                        label = "SOCKS5 Proxy Tunnel",
                        value = socksStatus,
                        icon = Icons.Default.AltRoute,
                        iconColor = if (settings.localSocks5Enabled && isRunning) SuccessNeon else TextMuted
                    )
                    Divider(color = BorderDark, modifier = Modifier.padding(vertical = 8.dp))
                    
                    StatusRow(
                        label = "LAN Endpoint Share",
                        value = lanStatus,
                        icon = Icons.Default.Share,
                        iconColor = if (settings.lanShareEnabled) CyanNeon else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun StatusRow(label: String, value: String, icon: ImageVector, iconColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, color = TextMuted, fontSize = 13.sp)
        }
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@Composable
fun SpeedCard(
    title: String,
    speedBytes: Long,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    val speedText = formatSpeed(speedBytes)
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, BorderDark),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = title, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = speedText,
                color = Color.White,
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black
            )
        }
    }
}

fun formatSpeed(bytes: Long): String {
    if (bytes < 1024) return "$bytes B/s"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB/s", kb)
    val mb = kb / 1024.0
    return String.format("%.1f MB/s", mb)
}

// ==========================================
// 2. MODES SCREEN & Profiles Configuration
// ==========================================
@Composable
fun ModesScreen(viewModel: MainViewModel) {
    val settings by viewModel.settingsState.collectAsState()
    val profiles by viewModel.profiles.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Connection Mode Selection",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // List connection modes
        items(ConnectionMode.values()) { mode ->
            val isSelected = settings.connectionMode == mode
            val borderTint by animateColorAsState(if (isSelected) PurpleNeon else BorderDark)
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) SurfaceDark else CardDark
                ),
                border = BorderStroke(1.5.dp, borderTint),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setConnectionMode(mode) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { viewModel.setConnectionMode(mode) },
                        colors = RadioButtonDefaults.colors(selectedColor = PurpleNeon)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = getModeTitle(mode),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = getModeDescription(mode),
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Server Profiles title row
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Server Configuration Profiles",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueNeon)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 12.sp)
                }
            }
        }

        // Server profiles list items
        if (profiles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No profiles available. Tap 'Add' to insert custom proxy server configs.", color = TextMuted, textAlign = TextAlign.Center, fontSize = 13.sp)
                }
            }
        } else {
            items(profiles) { profile ->
                ProfileItemRow(
                    profile = profile,
                    onSelect = { viewModel.selectProfile(profile.id) },
                    onDelete = { viewModel.deleteProfile(profile.id) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddProfileDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, type, server, port ->
                viewModel.addProfile(name, type, server, port)
                showAddDialog = false
            }
        )
    }
}

fun getModeTitle(mode: ConnectionMode): String {
    return when (mode) {
        ConnectionMode.DOH_DIRECT -> "1. DoH Direct Only"
        ConnectionMode.DOH_WORKER -> "2. DoH Workers Tunnel"
        ConnectionMode.PSIPHON -> "3. Psiphon Proxy Chain"
        ConnectionMode.PSIPHON_DOH_DIRECT -> "4. Psiphon + DoH Direct"
        ConnectionMode.PSIPHON_DOH_WORKER -> "5. Psiphon + DoH Worker"
    }
}

fun getModeDescription(mode: ConnectionMode): String {
    return when (mode) {
        ConnectionMode.DOH_DIRECT -> "Perform pure encrypted DNS-over-HTTPS requests over built-in providers."
        ConnectionMode.DOH_WORKER -> "Tunnel encrypted DNS operations using customizable Cloudflare Serverless Workers."
        ConnectionMode.PSIPHON -> "Establish secure censors-bypass SSH connections utilizing Psiphon Edge nodes."
        ConnectionMode.PSIPHON_DOH_DIRECT -> "Chain proxying where data routes via Psiphon, and DNS resolves direct over HTTPS."
        ConnectionMode.PSIPHON_DOH_WORKER -> "Perform Psiphon data routing and worker-side encrypted DNS resolutions."
    }
}

@Composable
fun ProfileItemRow(
    profile: ProxyProfile,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val highlightColor by animateColorAsState(
        targetValue = if (profile.isSelected) BlueNeon else BorderDark
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, highlightColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .background(if (profile.isSelected) BlueNeon.copy(alpha = 0.15f) else BorderDark, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = profile.type,
                        color = if (profile.isSelected) BlueNeon else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = profile.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${profile.server}:${profile.port}",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (profile.isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active Indicator",
                        tint = SuccessNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove setting profile",
                        tint = DangerNeon,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProfileDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("VLESS") }
    var server by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("443") }

    var expandedType by remember { mutableStateOf(false) }
    val types = listOf("VLESS", "VMESS", "TROJAN", "SHADOWSOCKS", "PSIPHON", "WARP")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Server Profile", color = Color.White) },
        containerColor = CardDark,
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Display Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BlueNeon, unfocusedLabelColor = TextMuted),
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Server Core Protocol") },
                        trailingIcon = {
                            IconButton(onClick = { expandedType = !expandedType }) {
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BlueNeon, unfocusedLabelColor = TextMuted),
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false },
                        modifier = Modifier.background(CardDark)
                    ) {
                        types.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t, color = Color.White) },
                                onClick = {
                                    type = t
                                    expandedType = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = server,
                    onValueChange = { server = it },
                    label = { Text("Server Gateway Host/IP") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BlueNeon, unfocusedLabelColor = TextMuted),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Access Port") },
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BlueNeon, unfocusedLabelColor = TextMuted),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val pNum = port.toIntOrNull() ?: 443
                    if (name.isNotBlank() && server.isNotBlank()) {
                        onAdd(name, type, server, pNum)
                    }
                }
            ) {
                Text("CREATE", color = SuccessNeon)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = DangerNeon)
            }
        }
    )
}

// ==========================================
// 3. DNS SETTINGS PANEL
// ==========================================
@Composable
fun DnsSettingsScreen(viewModel: MainViewModel) {
    val settings by viewModel.settingsState.collectAsState()
    val testOutput by viewModel.dnsTestResult.collectAsState()
    val isResolving by viewModel.isResolvingDns.collectAsState()

    var testDomain by remember { mutableStateOf("google.com") }
    var customWorkerUrl by remember { mutableStateOf(settings.dohWorkerUrl) }

    val providers = listOf("cloudflare", "google", "quad9", "adguard")
    val upstreams = listOf("cloudflare", "google", "quad9", "adguard")

    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "DNS Crypt Settings (DoH)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        // Direct Providers Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Built-In DNS Providers Only",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Select active provider for Direct DoH queries.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    providers.forEach { p ->
                        val isSel = settings.dohDirectProvider == p
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setDohProvider(p) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSel,
                                onClick = { viewModel.setDohProvider(p) },
                                colors = RadioButtonDefaults.colors(selectedColor = CyanNeon)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(p.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = if (p == "cloudflare") "https://cloudflare-dns.com/dns-query"
                                    else if (p == "google") "https://dns.google/dns-query"
                                    else if (p == "quad9") "https://dns.quad9.net/dns-query"
                                    else "https://dns.adguard-dns.com/dns-query",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // Custom Worker Configurations
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cloudflare Workers DNS Bypass",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bypasses standard geo-blocks routing DNS wire queries over customized worker nodes.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = customWorkerUrl,
                        onValueChange = {
                            customWorkerUrl = it
                            viewModel.setDohWorkerUrl(it)
                        },
                        label = { Text("Worker Gateway URL") },
                        placeholder = { Text("https://example.worker.dev/dns-query") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = BorderDark,
                            unfocusedLabelColor = TextMuted
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Worker DNS Upstream Resolver:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        upstreams.forEach { u ->
                            val isSel = settings.dohWorkerUpstream == u
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSel) CyanNeon.copy(alpha = 0.15f) else BorderDark,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSel) CyanNeon else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { viewModel.setDohWorkerUpstream(u) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = u.uppercase(),
                                    color = if (isSel) CyanNeon else TextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Diagnostic DNS Sandbox
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "DNS Crypt Sandbox", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Verify binary RFC 8484 packet serialization and parsing online.", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = testDomain,
                            onValueChange = { testDomain = it },
                            label = { Text("Domain to resolve") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = BorderDark,
                                unfocusedLabelColor = TextMuted
                            ),
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                keyboardController?.hide()
                                viewModel.executeDnsTest(testDomain)
                            })
                        )

                        Button(
                            onClick = {
                                keyboardController?.hide()
                                viewModel.executeDnsTest(testDomain)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                            modifier = Modifier.height(56.dp),
                            enabled = !isResolving
                        ) {
                            if (isResolving) {
                                CircularProgressIndicator(color = BgDark, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("QUERY", color = BgDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (testOutput.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BgDark, RoundedCornerShape(4.dp))
                                .border(1.dp, BorderDark, RoundedCornerShape(4.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = testOutput,
                                color = if (testOutput.startsWith("SUCCESS")) SuccessNeon else if (testOutput.startsWith("RESOLVED")) TextPrimary else DangerNeon,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. LOCAL SHARING ENDPOINTS CONFIG
// ==========================================
@Composable
fun LocalSharingScreen(viewModel: MainViewModel) {
    val settings by viewModel.settingsState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "Local Proxies & LAN Sharing", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        // HTTP Proxy configuration Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Local HTTP Tunnel Server", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Enables HTTP CONNECT proxies over local address.", color = TextMuted, fontSize = 12.sp)
                        }
                        Switch(
                            checked = settings.localHttpProxyEnabled,
                            onCheckedChange = { viewModel.toggleHttpProxy(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = SuccessNeon)
                        )
                    }

                    AnimatedVisibility(visible = settings.localHttpProxyEnabled) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = BorderDark)
                            Spacer(modifier = Modifier.height(12.dp))
                            InfoRow(label = "Bind Loopback Endpoint", value = "127.0.0.1:8080")
                            InfoRow(label = "Gateway Protocol", value = "HTTP 1.1 CONNECT")
                        }
                    }
                }
            }
        }

        // SOCKS5 config Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Local SOCKS5 Proxy Server", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Enables raw binary SOCKS5 sockets wrapping.", color = TextMuted, fontSize = 12.sp)
                        }
                        Switch(
                            checked = settings.localSocks5Enabled,
                            onCheckedChange = { viewModel.toggleSocksProxy(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = SuccessNeon)
                        )
                    }

                    AnimatedVisibility(visible = settings.localSocks5Enabled) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = BorderDark)
                            Spacer(modifier = Modifier.height(12.dp))
                            InfoRow(label = "Bind Loopback Endpoint", value = "127.0.0.1:1080")
                            InfoRow(label = "Gateway Auth Methods", value = "0x00 (NO AUTH REQUIRED)")
                        }
                    }
                }
            }
        }

        // LAN Network Access allow
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Expose Gateway to Local Area Network", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Binds proxy ports over 0.0.0.0 instead of loopback loop, allowing nearby Wi-Fi devices to share the active VPN tunnel directly.", color = TextMuted, fontSize = 12.sp)
                        }
                        Switch(
                            checked = settings.lanShareEnabled,
                            onCheckedChange = { viewModel.toggleLanSharing(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon)
                        )
                    }

                    AnimatedVisibility(visible = settings.lanShareEnabled) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = BorderDark)
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BgDark, RoundedCornerShape(4.dp))
                                    .border(1.dp, WarningColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .padding(12.dp)
                            ) {
                                Row {
                                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = WarningColor)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "By enabling LAN sharing, anyone on your local Wi-Fi router network segment can hook tunnels over your mobile interface. Disable when connected on public airports or networks.",
                                        color = WarningColor,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

val WarningColor = Color(0xFFFBBF24)

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

// ==========================================
// 5. VPN SPLIT TUNNELLING / APPS ACCESS PANEL
// ==========================================
@Composable
fun VpnAppsAccessScreen(viewModel: MainViewModel) {
    val settings by viewModel.settingsState.collectAsState()
    val appsList by viewModel.installedApps.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showOnlySystem by remember { mutableStateOf(false) }

    val filteredApps = remember(appsList, searchQuery, showOnlySystem) {
        appsList.filter { app ->
            val matchQuery = app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            val matchSystem = if (showOnlySystem) app.isSystemApp else !app.isSystemApp
            matchQuery && matchSystem
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "VPN Split Tunnelling Rules", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        // Tunnel Modes Explainer Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Architectural split rules", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Allows/excludes individual applications over the virtual VPN interface. System services can be filtered natively.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Only Apps Tunnel Set Size", color = TextMuted, fontSize = 11.sp)
                            Text("${settings.selectedTunnelApps.size} Apps allowed", color = BlueNeon, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bypass Apps Set Size", color = TextMuted, fontSize = 11.sp)
                            Text("${settings.selectedBypassApps.size} Apps bypassed", color = PurpleNeon, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Search app field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search package or app name...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = BlueNeon,
                    unfocusedBorderColor = BorderDark,
                    unfocusedLabelColor = TextMuted
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Toggle user vs system packages
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !showOnlySystem,
                    onClick = { showOnlySystem = false },
                    label = { Text("User Applications") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BlueNeon.copy(alpha = 0.15f),
                        selectedLabelColor = BlueNeon
                    )
                )
                FilterChip(
                    selected = showOnlySystem,
                    onClick = { showOnlySystem = true },
                    label = { Text("System Services") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurpleNeon.copy(alpha = 0.15f),
                        selectedLabelColor = PurpleNeon
                    )
                )
            }
        }

        // Applications list
        if (filteredApps.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text("No matching packages found.", color = TextMuted, fontSize = 13.sp)
                }
            }
        } else {
            items(filteredApps) { app ->
                AppRowLayout(
                    app = app,
                    isAllowed = settings.selectedTunnelApps.contains(app.packageName),
                    isDisallowed = settings.selectedBypassApps.contains(app.packageName),
                    onToggleAllowed = { viewModel.toggleAppTunnel(app.packageName) },
                    onToggleDisallowed = { viewModel.toggleAppBypass(app.packageName) }
                )
            }
        }
    }
}

@Composable
fun AppRowLayout(
    app: AppItem,
    isAllowed: Boolean,
    isDisallowed: Boolean,
    onToggleAllowed: () -> Unit,
    onToggleDisallowed: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, BorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon loader with fallback
            if (app.icon != null) {
                Image(
                    bitmap = app.icon.toBitmap(96, 96).asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(BorderDark, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Android, contentDescription = null, tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    text = app.packageName,
                    color = TextMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Allowed Tunnel Selection Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .border(
                        1.dp,
                        if (isAllowed) SuccessNeon else BorderDark,
                        RoundedCornerShape(4.dp)
                    )
                    .background(if (isAllowed) SuccessNeon.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { onToggleAllowed() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "TUNNEL",
                    color = if (isAllowed) SuccessNeon else TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Disallowed Bypass Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .border(
                        1.dp,
                        if (isDisallowed) DangerNeon else BorderDark,
                        RoundedCornerShape(4.dp)
                    )
                    .background(if (isDisallowed) DangerNeon.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { onToggleDisallowed() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "BYPASS",
                    color = if (isDisallowed) DangerNeon else TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==========================================
// 6. EXTERNAL CORES DIAGNOSTIC PANEL
// ==========================================
@Composable
fun CoresScreen(viewModel: MainViewModel) {
    val settings by viewModel.settingsState.collectAsState()
    
    val xrayRunning by viewModel.xrayRunning.collectAsState()
    val singBoxRunning by viewModel.singBoxRunning.collectAsState()
    val psiphonRunning by viewModel.psiphonRunning.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "External Daemon Cores (Vendored)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Deploy Pinned External Daemon Cores", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Allows executing underlying official JNI bin layers directly inside app storage limits.", color = TextMuted, fontSize = 12.sp)
                        }
                        Switch(
                            checked = settings.externalCoresEnabled,
                            onCheckedChange = { viewModel.toggleExternalCores(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = BlueNeon)
                        )
                    }

                    if (settings.externalCoresEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = BorderDark)
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SuccessNeon.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                .border(1.dp, SuccessNeon.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(10.dp)
                        ) {
                            Text("VERIFIED: Release bundles ARM64-V8A pinned binaries natively. Zero network core downloads required.", color = SuccessNeon, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        // 1. Xray Core configuration block
        item {
            CoreAdapterRowLayout(
                title = "Xray Core Adapter",
                ver = "v1.8.4-vendored",
                path = "/jniLibs/arm64-v8a/xray",
                isEnabled = settings.xrayEnabled,
                isRunning = xrayRunning,
                onCheckedChange = { viewModel.toggleXrayAdapter(it) },
                activeColor = PurpleNeon
            )
        }

        // 2. Sing-Box block
        item {
            CoreAdapterRowLayout(
                title = "Sing-Box Core Adapter",
                ver = "v1.5.6-vendored",
                path = "/jniLibs/arm64-v8a/sing-box",
                isEnabled = settings.singBoxEnabled,
                isRunning = singBoxRunning,
                onCheckedChange = { viewModel.toggleSingBoxAdapter(it) },
                activeColor = BlueNeon
            )
        }

        // 3. Psiphon Daemon block
        item {
            CoreAdapterRowLayout(
                title = "Psiphon Sponsor Daemon",
                ver = "v3.8.1-vendored",
                path = "/jniLibs/arm64-v8a/psiphon",
                isEnabled = settings.psiphonEnabled,
                isRunning = psiphonRunning,
                onCheckedChange = { viewModel.togglePsiphonAdapter(it) },
                activeColor = CyanNeon
            )
        }
    }
}

@Composable
fun CoreAdapterRowLayout(
    title: String,
    ver: String,
    path: String,
    isEnabled: Boolean,
    isRunning: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, if (isRunning) activeColor else BorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Bundled File: $ver", color = TextMuted, fontSize = 11.sp)
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = activeColor)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = BorderDark)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Active Status State", color = TextMuted, fontSize = 12.sp)
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isRunning) activeColor.copy(alpha = 0.15f) else BorderDark,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isRunning) "ACTIVE DEPLOYED" else "OFFLINE IDLE",
                        color = if (isRunning) activeColor else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Binary Mapping Route", color = TextMuted, fontSize = 12.sp)
                Text(text = path, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ==========================================
// 7. DIAGNOSTIC LIVE LOGS SCREEN
// ==========================================
@Composable
fun LogsScreen(viewModel: MainViewModel) {
    val logs by viewModel.logsState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "App Core Terminal logs", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        val fullLogText = logs.joinToString("\n") { "[${it.timestamp}] [${it.level}] [${it.tag}]: ${it.message}" }
                        // Copy to Clipboard
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Last Night System Trace", fullLogText)
                        clipboard.setPrimaryClip(clip)
                        LogRepository.i("Terminal", "Logged trace lines copied to device system clipboard safely.")
                    }
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy raw logs text", tint = BlueNeon)
                }
                IconButton(onClick = { viewModel.clearLiveLogs() }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear system lists", tint = DangerNeon)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Simulated Unix Terminal
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF04070F), RoundedCornerShape(8.dp))
                .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("$ root@last-night: logs terminal listening...", color = TextMuted, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "[${log.timestamp}]",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "[${log.level}]",
                                color = getLogLevelColor(log.level),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(52.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${log.tag}: ${log.message}",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getLogLevelColor(level: String): Color {
    return when (level) {
        "DEBUG" -> TextMuted
        "INFO" -> CyanNeon
        "WARN" -> WarningColor
        "ERROR" -> DangerNeon
        else -> TextPrimary
    }
}
