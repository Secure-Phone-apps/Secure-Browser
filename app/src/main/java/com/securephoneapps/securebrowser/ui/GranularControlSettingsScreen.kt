package com.securephoneapps.securebrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securephoneapps.securebrowser.viewmodel.BrowserStateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GranularControlSettingsScreen(viewModel: BrowserStateViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // State bindings
    val jsEnabled by viewModel.javascriptEnabled.collectAsState()
    val blockThirdPartyCookies by viewModel.blockThirdPartyCookies.collectAsState()
    val rtcPrivacy by viewModel.webRtcPrivacyEnabled.collectAsState()
    val deGoogling by viewModel.deGooglingTelemetryEnabled.collectAsState()
    val httpsOnly by viewModel.httpsOnlyMode.collectAsState()
    val clearOnExit by viewModel.clearOnExitEnabled.collectAsState()
    val stripTracking by viewModel.stripTrackingEnabled.collectAsState()
    val deAMP by viewModel.deAMPEnabled.collectAsState()
    val biometricEnabled by viewModel.isBiometricLockEnabled.collectAsState()
    val hardwareShutter by viewModel.isHardwareShutterActive.collectAsState()
    val restrictLocal by viewModel.restrictLocalSubnets.collectAsState()
    val forcedDark by viewModel.forcedDarkModeEnabled.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val themeColor by viewModel.themeColor.collectAsState()
    
    val searchEngine by viewModel.searchEngine.collectAsState()
    val customSearchUrl by viewModel.customSearchEngineUrl.collectAsState()
    val selectedUserAgent by viewModel.selectedUserAgent.collectAsState()
    
    val dohProvider by viewModel.selectedDohProvider.collectAsState()
    val proxyEnabled by viewModel.proxyEnabled.collectAsState()
    val proxyHost by viewModel.proxyHost.collectAsState()
    val proxyPort by viewModel.proxyPort.collectAsState()
    val proxyType by viewModel.proxyType.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(BrowserStateViewModel.Screen.Browser) }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            SettingsSectionTitle("Search Engine Configuration")
            SettingsDropdown(
                label = "Default Search Engine",
                options = listOf("DuckDuckGo", "Brave", "Google", "Bing", "Custom"),
                selectedOption = searchEngine,
                onOptionSelected = { viewModel.updateSearchEngine(it) }
            )
            
            if (searchEngine == "Custom") {
                Spacer(Modifier.height(8.dp))
                SettingsTextField(
                    label = "Custom Search Engine Query URL",
                    value = customSearchUrl,
                    onValueChange = { viewModel.updateCustomSearchEngine(it) },
                    placeholder = "https://duckduckgo.com/?q="
                )
            }

            Spacer(Modifier.height(24.dp))
            SettingsSectionTitle("Visual Interface & Theme")
            SettingsDropdown(
                label = "Application Theme",
                options = listOf("Light", "Dark"),
                selectedOption = appTheme,
                onOptionSelected = { viewModel.updateAppTheme(it) }
            )
            Spacer(Modifier.height(16.dp))
            SettingsDropdown(
                label = "Accent Color",
                options = listOf("Blue", "Green", "Purple", "Red", "Orange"),
                selectedOption = themeColor,
                onOptionSelected = { viewModel.updateThemeColor(it) }
            )
            
            Spacer(Modifier.height(24.dp))
            SettingsSectionTitle("Storage & Downloads")
            Button(
                onClick = { viewModel.navigateTo(BrowserStateViewModel.Screen.Downloads) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Download, null)
                Spacer(Modifier.width(8.dp))
                Text("Manage Secure Downloads")
            }
            Text(
                "View and export files from the isolated vault",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Spacer(Modifier.height(24.dp))
            SettingsSectionTitle("Identity & Spoofing")
            SettingsDropdown(
                label = "Browser User Agent string",
                options = listOf("Mobile (Android)", "Desktop (Windows)", "Desktop (MacOS)", "iOS (iPhone)"),
                selectedOption = when {
                    selectedUserAgent.contains("Android") -> "Mobile (Android)"
                    selectedUserAgent.contains("Windows") -> "Desktop (Windows)"
                    selectedUserAgent.contains("Macintosh") -> "Desktop (MacOS)"
                    selectedUserAgent.contains("iPhone") -> "iOS (iPhone)"
                    else -> "Mobile (Android)"
                },
                onOptionSelected = {
                    val ua = when(it) {
                        "Mobile (Android)" -> "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        "Desktop (Windows)" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        "Desktop (MacOS)" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        "iOS (iPhone)" -> "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
                        else -> "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    }
                    viewModel.updateUserAgent(ua)
                }
            )

            Spacer(Modifier.height(24.dp))
            SettingsSectionTitle("Advanced Network Routing")
            
            SettingsDropdown(
                label = "Encrypted DoH DNS Provider",
                options = listOf("Cloudflare", "Quad9", "AdGuard", "Default"),
                selectedOption = when(dohProvider) {
                    "https://cloudflare-dns.com" -> "Cloudflare"
                    "https://dns.quad9.net/dns-query" -> "Quad9"
                    "https://dns.adguard.com/dns-query" -> "AdGuard"
                    else -> "Default"
                },
                onOptionSelected = {
                    val url = when(it) {
                        "Cloudflare" -> "https://cloudflare-dns.com"
                        "Quad9" -> "https://dns.quad9.net/dns-query"
                        "AdGuard" -> "https://dns.adguard.com/dns-query"
                        else -> "Default"
                    }
                    viewModel.updateDohProvider(url)
                }
            )

            SettingsToggleItem(
                title = "Enable SOCKS5/HTTP Proxy",
                subtitle = "Route all traffic through a custom proxy",
                checked = proxyEnabled,
                onCheckedChange = { viewModel.setBrowserProxy(proxyHost, proxyPort, proxyType, it) }
            )
            
            if (proxyEnabled) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(2f)) {
                        SettingsTextField(
                            label = "Proxy Host",
                            value = proxyHost,
                            onValueChange = { viewModel.setBrowserProxy(it, proxyPort, proxyType, proxyEnabled) }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SettingsTextField(
                            label = "Port",
                            value = proxyPort.toString(),
                            onValueChange = { 
                                val port = it.toIntOrNull() ?: 0
                                viewModel.setBrowserProxy(proxyHost, port, proxyType, proxyEnabled)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            SettingsSectionTitle("Firewalls & Peripheral Shields")
            
            SettingsToggleItem("Enable JavaScript Execution", "Standard JS engine logic", jsEnabled) { viewModel.toggleSetting("javascript_enabled", it) }
            SettingsToggleItem("Block 3rd-Party Tracking Storage", "Prevents cross-site cookies", blockThirdPartyCookies) { viewModel.toggleSetting("block_third_party_cookies", it) }
            SettingsToggleItem("Enforce WebRTC Privacy Leak Shield", "Prevents local IP leakage via WebRTC", rtcPrivacy) { viewModel.toggleSetting("webrtc_privacy_enabled", it) }
            SettingsToggleItem("Enforce Absolute HTTPS-Only Mode", "Upgrades all insecure requests", httpsOnly) { viewModel.toggleSetting("https_only_mode", it) }
            SettingsToggleItem("Strip URL Tracking Parameters", "Removes UTM, fbclid, etc. from links", stripTracking) { viewModel.toggleSetting("strip_tracking_enabled", it) }
            SettingsToggleItem("Automated Google de-AMP Linker", "Redirects AMP links to canonical pages", deAMP) { viewModel.toggleSetting("deamp_enabled", it) }
            SettingsToggleItem("Automated Clear-Room Exit Policy", "Wipes session data on app close", clearOnExit) { viewModel.toggleSetting("clear_on_exit_enabled", it) }
            SettingsToggleItem("De-Googling Privacy Telemetry", "Strip Google specific tracking headers", deGoogling) { viewModel.toggleSetting("de_googling_telemetry_enabled", it) }
            SettingsToggleItem("Enforce Biometric Application Lock", "Requires auth to open browser", biometricEnabled) { viewModel.toggleBiometricLock(it) }
            SettingsToggleItem("Enforce Camera & Microphone Hard Shutter", "Zero-trust media request denial", hardwareShutter) { viewModel.toggleHardwareShutter(it) }
            SettingsToggleItem("Restrict Local Intranet Port Scanning", "Blocks private subnet probes", restrictLocal) { viewModel.toggleRestrictLocalSubnets(it) }
            SettingsToggleItem("Forced Layout Dark Mode Engine", "Native CSS inversion for all sites", forcedDark) { viewModel.toggleForcedDarkMode(it) }

            Spacer(Modifier.height(24.dp))
            SettingsSectionTitle("Data Portability & Backup")
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { viewModel.exportBookmarksToEncryptedJson(context) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export")
                }
                Button(
                    onClick = { /* Import logic trigger */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Import")
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.clearAllData() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.DeleteSweep, null)
                Spacer(Modifier.width(8.dp))
                Text("Clear All Browsing Data")
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.executeHardPanicPurge() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Warning, null)
                Spacer(Modifier.width(8.dp))
                Text("HARD PANIC PURGE", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsToggleItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTextField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String = "") {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdown(label: String, options: List<String>, selectedOption: String, onOptionSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
