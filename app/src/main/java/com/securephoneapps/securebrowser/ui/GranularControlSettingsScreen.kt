package com.securephoneapps.securebrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securephoneapps.securebrowser.viewmodel.BrowserStateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GranularControlSettingsScreen(
    viewModel: BrowserStateViewModel,
    onBack: () -> Unit
) {
    val telemetry by viewModel.shieldTelemetry.collectAsState()
    val jsEnabled by viewModel.javascriptEnabled.collectAsState()
    val blockThirdParty by viewModel.blockThirdPartyCookies.collectAsState()
    val rtcPrivacy by viewModel.webRtcPrivacyEnabled.collectAsState()
    val deGoogling by viewModel.deGooglingTelemetryEnabled.collectAsState()
    val forcedDark by viewModel.forcedDarkModeEnabled.collectAsState()
    val httpsOnly by viewModel.httpsOnlyMode.collectAsState()
    val searchEngineVal by viewModel.searchEngine.collectAsState()
    val customSearchEngineUrlVal by viewModel.customSearchEngineUrl.collectAsState()
    val userAgentVal by viewModel.selectedUserAgent.collectAsState()
    
    val selectedDohProvider by viewModel.selectedDohProvider.collectAsState()
    val proxyEnabled by viewModel.proxyEnabled.collectAsState()
    val proxyHost by viewModel.proxyHost.collectAsState()
    val proxyPort by viewModel.proxyPort.collectAsState()
    val proxyType by viewModel.proxyType.collectAsState()
    val biometricEnabled by viewModel.isBiometricLockEnabled.collectAsState()
    val isHardwareShutterActive by viewModel.isHardwareShutterActive.collectAsState()

    var showUaDropdown by remember { mutableStateOf(false) }
    var exceptionInput by remember { mutableStateOf("") }
    val exceptionList = remember { mutableStateListOf<String>("duckduckgo.com", "brave.com") }

    val userAgents = listOf(
        "Android Mobile" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
        "Windows Desktop" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "MacOS Desktop" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "iOS Mobile Safari" to "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Hardening Panel", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF0F172A), // Slate 900
                    navigationIconContentColor = Color(0xFF475569) // Slate 600
                ),
                modifier = Modifier.border(width = 1.dp, color = Color(0xFFE2E8F0))
            )
        },
        containerColor = Color(0xFFF8FAFC) // Slate 50 background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // -- SECTION 1: Shield Telemetry Dashboard --
            Text(
                text = "SHIELD SYSTEM TELEMETRY",
                fontSize = 11.sp,
                color = Color(0xFF64748B), // Slate 500
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TelemetryRow(
                        icon = Icons.Default.Block,
                        title = "Ad & Tracker Interceptions",
                        count = telemetry?.trackersBlockedGlobal ?: 0L,
                        color = Color(0xFFEF4444) // Red 500
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TelemetryRow(
                        icon = Icons.Default.Fingerprint,
                        title = "Canvas Fingerprint Fakes",
                        count = telemetry?.canvasFakesTriggered ?: 0L,
                        color = Color(0xFF10B981) // Emerald 500
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TelemetryRow(
                        icon = Icons.Default.Security,
                        title = "Fingerprint Profilers Distorted",
                        count = telemetry?.fingerprintMocksTriggered ?: 0L,
                        color = Color(0xFF8B5CF6) // Violet 500
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TelemetryRow(
                        icon = Icons.Default.SettingsBackupRestore,
                        title = "Google Telemetry Drops",
                        count = telemetry?.telemetryPingsNeutralized ?: 0L,
                        color = Color(0xFF3B82F6) // Blue 500
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // -- SECTION 2: Shield Firewall Settings --
            Text(
                text = "SHIELD FIREWALL CONTROLS",
                fontSize = 11.sp,
                color = Color(0xFF64748B), // Slate 500
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingToggleRow(
                        title = "Network Shield (Brave-Interception)",
                        subtitle = "Regex network filter to drop trackers and crypto-miners",
                        checked = jsEnabled, // Coupled/integrated toggles
                        onCheckedChange = { viewModel.toggleSetting("javascript_enabled", it) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingToggleRow(
                        title = "Anti-Fingerprinting Virtualization",
                        subtitle = "Brave-style script injection to mock WebGL, Audio and Canvas variables",
                        checked = deGoogling,
                        onCheckedChange = { viewModel.toggleSetting("de_googling_telemetry_enabled", it) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingToggleRow(
                        title = "Forced Layout Dark Mode Engine",
                        subtitle = "Forcefully invert webpage canvas rendering for midnight browsing",
                        checked = forcedDark,
                        onCheckedChange = { viewModel.toggleForcedDarkMode(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // -- SECTION 3.5: Data Portability & Backup --
            Text(
                text = "DATA PORTABILITY & BACKUP",
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val context = viewModel.getApplication<android.app.Application>()
                                val export = viewModel.exportBookmarksToEncryptedJson(context)
                                if (export.isNotBlank()) {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Encrypted Backup", export)
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "Encrypted backup copied to clipboard", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, tint = Color(0xFF2563EB))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Export Encrypted Backup", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Copies encrypted bookmarks JSON to clipboard", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    var showImportDialog by remember { mutableStateOf(false) }
                    var importText by remember { mutableStateOf("") }
                    
                    if (showImportDialog) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showImportDialog = false },
                            title = { Text("Import Encrypted Backup") },
                            text = {
                                Column {
                                    Text("Paste the encrypted JSON string below:", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = importText,
                                        onValueChange = { importText = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Paste here...") }
                                    )
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    viewModel.importBookmarksFromEncryptedJson(viewModel.getApplication(), importText)
                                    showImportDialog = false
                                    importText = ""
                                }) { Text("Import") }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showImportDialog = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, tint = Color(0xFF059669))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Import Encrypted Backup", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Restore bookmarks from encrypted profile string", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // -- SECTION 3.6: Advanced Network Routing --
            Text(
                text = "ADVANCED NETWORK ROUTING",
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Encrypted DoH Selector
                    Text("Encrypted DoH DNS Provider", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    var showDohDropdown by remember { mutableStateOf(false) }
                    val dohProviders = listOf(
                        "Default" to "Default",
                        "Cloudflare" to "https://cloudflare-dns.com",
                        "Quad9" to "https://dns.quad9.net",
                        "AdGuard DNS" to "https://dns.adguard-dns.com"
                    )
                    
                    ExposedDropdownMenuBox(
                        expanded = showDohDropdown,
                        onExpandedChange = { showDohDropdown = !showDohDropdown }
                    ) {
                        val currentDohName = dohProviders.find { it.second == selectedDohProvider }?.first ?: "Default"
                        OutlinedTextField(
                            value = currentDohName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDohDropdown) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF475569),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showDohDropdown,
                            onDismissRequest = { showDohDropdown = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            dohProviders.forEach { (name, url) ->
                                DropdownMenuItem(
                                    text = { Text(name, color = Color(0xFF0F172A)) },
                                    onClick = {
                                        viewModel.updateDohProvider(url)
                                        showDohDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Proxy Settings Panel
                    SettingToggleRow(
                        title = "Enable Custom Proxy",
                        subtitle = "Route WebKit traffic through custom SOCKS5/HTTP proxy (e.g. Tor)",
                        checked = proxyEnabled,
                        onCheckedChange = { viewModel.setBrowserProxy(proxyHost, proxyPort, proxyType, it) }
                    )

                    if (proxyEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Proxy Host field
                            var hostInput by remember(proxyHost) { mutableStateOf(proxyHost) }
                            OutlinedTextField(
                                value = hostInput,
                                onValueChange = { 
                                    hostInput = it
                                    viewModel.setBrowserProxy(it, proxyPort, proxyType, proxyEnabled)
                                },
                                label = { Text("Proxy Host") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF475569),
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            // Proxy Port field
                            var portInput by remember(proxyPort) { mutableStateOf(proxyPort.toString()) }
                            OutlinedTextField(
                                value = portInput,
                                onValueChange = { 
                                    portInput = it
                                    val portInt = it.toIntOrNull() ?: proxyPort
                                    viewModel.setBrowserProxy(proxyHost, portInt, proxyType, proxyEnabled)
                                },
                                label = { Text("Proxy Port") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF475569),
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.width(100.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Proxy Type field (SOCKS vs HTTP toggle)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Proxy Type", color = Color(0xFF475569), fontSize = 14.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.setBrowserProxy(proxyHost, proxyPort, "SOCKS", proxyEnabled) }
                            ) {
                                Text("SOCKS5", color = if (proxyType == "SOCKS") Color(0xFF2563EB) else Color(0xFF94A3B8), fontWeight = if (proxyType == "SOCKS") FontWeight.Bold else FontWeight.Normal)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.setBrowserProxy(proxyHost, proxyPort, "HTTP", proxyEnabled) }
                            ) {
                                Text("HTTP", color = if (proxyType == "HTTP") Color(0xFF2563EB) else Color(0xFF94A3B8), fontWeight = if (proxyType == "HTTP") FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // -- SECTION 3.7: Security Access Configuration --
            Text(
                text = "SECURITY ACCESS CONFIGURATION",
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingToggleRow(
                        title = "Enforce Biometric Application Lock",
                        subtitle = "Request biometric fingerprint or secure device PIN before launching the browser context.",
                        checked = biometricEnabled,
                        onCheckedChange = { viewModel.toggleBiometricLock(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // -- SECTION 3.8: Hardware Peripheral Firewalls --
            Text(
                text = "HARDWARE PERIPHERAL FIREWALLS",
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingToggleRow(
                        title = "Enforce Camera & Microphone Hard Shutter",
                        subtitle = "Forcefully intercept and block all web configurations requesting access to camera and microphone hardware.",
                        checked = isHardwareShutterActive,
                        onCheckedChange = { viewModel.toggleHardwareShutter(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // -- SECTION 3: Cryptographic Privacy --
            Text(
                text = "CRYPTOGRAPHIC PRIVACY POLICY",
                fontSize = 11.sp,
                color = Color(0xFF64748B), // Slate 500
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingToggleRow(
                        title = "Enforce HTTPS-Only Connection",
                        subtitle = "Upgrade non-secure HTTP links; drop connection immediately if fail",
                        checked = httpsOnly,
                        onCheckedChange = { viewModel.toggleSetting("https_only_mode", it) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingToggleRow(
                        title = "WebRTC IP Leak Protection",
                        subtitle = "Prevent local/internal network IP exposure through WebRTC queries",
                        checked = rtcPrivacy,
                        onCheckedChange = { viewModel.toggleSetting("webrtc_privacy_enabled", it) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingToggleRow(
                        title = "Block Third-Party Cookies",
                        subtitle = "Limit cross-site trackers from saving identifiers on your storage",
                        checked = blockThirdParty,
                        onCheckedChange = { viewModel.toggleSetting("block_third_party_cookies", it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // User Agent Dropdown Selector
                    Text("Isolated User-Agent Profile", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    ExposedDropdownMenuBox(
                        expanded = showUaDropdown,
                        onExpandedChange = { showUaDropdown = !showUaDropdown }
                    ) {
                        val currentUaName = userAgents.find { it.second == userAgentVal }?.first ?: "Custom"
                        OutlinedTextField(
                            value = currentUaName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showUaDropdown) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF475569),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showUaDropdown,
                            onDismissRequest = { showUaDropdown = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            userAgents.forEach { (name, ua) ->
                                DropdownMenuItem(
                                    text = { Text(name, color = Color(0xFF0F172A)) },
                                    onClick = {
                                        viewModel.updateUserAgent(ua)
                                        showUaDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // -- SECTION 4: Exception and Bypass Managers --
            Text(
                text = "SHIELD BYPASS & EXCEPTIONS",
                fontSize = 11.sp,
                color = Color(0xFF64748B), // Slate 500
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Tracker Block Excluded Domains",
                        color = Color(0xFF0F172A), // Slate 900
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "These trusted domains bypass the network interception firewall.",
                        color = Color(0xFF64748B), // Slate 500
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = exceptionInput,
                            onValueChange = { exceptionInput = it },
                            placeholder = { Text("e.g. proton.me", color = Color(0xFF94A3B8)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF475569),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (exceptionInput.isNotBlank()) {
                                    exceptionList.add(exceptionInput.trim())
                                    exceptionInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)), // Blue 600
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Add", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Active Exceptions List
                    exceptionList.forEach { domain ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(domain, color = Color(0xFF0F172A), fontSize = 14.sp)
                            Text(
                                "Delete",
                                color = Color(0xFFEF4444), // Red 500
                                modifier = Modifier
                                    .clickable { exceptionList.remove(domain) }
                                    .padding(4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // -- SECTION 5: Search Engines & Cookie Stats --
            Text(
                text = "SEARCH ENGINE & COOKIES",
                fontSize = 11.sp,
                color = Color(0xFF64748B), // Slate 500
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Primary Search Engine", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        SearchEngineBadge(
                            name = "DuckDuckGo",
                            isSelected = searchEngineVal == "DuckDuckGo",
                            onClick = { viewModel.updateSearchEngine("DuckDuckGo") }
                        )
                        SearchEngineBadge(
                            name = "Brave Search",
                            isSelected = searchEngineVal == "Brave",
                            onClick = { viewModel.updateSearchEngine("Brave") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Custom Search Engine Query URL",
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customSearchEngineUrlVal,
                        onValueChange = { viewModel.updateCustomSearchEngine(it) },
                        placeholder = { Text("e.g., https://duckduckgo.com/?q=") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF475569),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_search_engine_input")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cookie, contentDescription = "Cookies", tint = Color(0xFF475569)) // Slate 600
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Stored Session Cookies", color = Color(0xFF0F172A), fontSize = 14.sp)
                                Text("Virtual sandbox isolation", color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                        }
                        Text("Active", color = Color(0xFF059669), fontSize = 12.sp, fontWeight = FontWeight.Bold) // Emerald 600
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // -- SECTION 6: Destructive Wiping Sequence --
            Button(
                onClick = { viewModel.closeSession() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)), // Red 500
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("destructive_wipe_button")
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = "Wipe Session", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("DESTRUCTIVE SESSION WIPE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun TelemetryRow(
    icon: ImageVector,
    title: String,
    count: Long,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, color = Color(0xFF0F172A), fontSize = 14.sp) // Slate 900
        }
        Text(count.toString(), color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 16.sp) // Slate 900
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold) // Slate 900
            Text(subtitle, color = Color(0xFF64748B), fontSize = 11.sp, lineHeight = 14.sp) // Slate 500
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF059669), // Emerald 600
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFCBD5E1) // Slate 300
            )
        )
    }
}

@Composable
fun SearchEngineBadge(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0) // Blue 600 or Slate 200
    val bgColor = if (isSelected) Color(0xFF2563EB).copy(alpha = 0.08f) else Color.Transparent

    Box(
        modifier = Modifier
            .width(140.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onClick() }
            .background(Color.Transparent)
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFF2563EB) else Color(0xFF94A3B8), // Blue 600 or Slate 400
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = name,
                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B), // Blue 600 or Slate 500
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
