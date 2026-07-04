package com.portscanpro.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portscanpro.engine.NetworkScanner
import com.portscanpro.model.PortInfo
import com.portscanpro.model.ScanTarget
import com.portscanpro.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScanScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scanner = remember { NetworkScanner() }
    val scanState by scanner.state.collectAsState()

    var targetIp by remember { mutableStateOf("") }
    var scanMode by remember { mutableStateOf("quick") }
    var results by remember { mutableStateOf<List<ScanTarget>>(emptyList()) }
    var expandedHost by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        targetIp = scanner.getLocalSubnet()
    }

    fun startScan() {
        if (targetIp.isBlank()) return
        results = emptyList()
        scanner.reset()
        scope.launch {
            when (scanMode) {
                "ping" -> {
                    val result = scanner.pingHost(targetIp)
                    results = listOf(result)
                }
                "subnet" -> {
                    results = scanner.scanSubnet(targetIp)
                }
                "quick" -> {
                    val host = scanner.pingHost(targetIp)
                    if (host.isAlive) {
                        val ports = scanner.quickPortScan(targetIp)
                        results = listOf(host.copy(openPorts = ports))
                    } else {
                        results = listOf(host)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = PrimaryGreen)
            }
            Text("Network Scanner", color = PrimaryGreen, fontSize = 20.sp,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = targetIp,
            onValueChange = { targetIp = it },
            label = { Text("Target IP / Subnet") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen, unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = PrimaryGreen, cursorColor = PrimaryGreen,
                focusedTextColor = Color.White, unfocusedTextColor = Color.White
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("quick" to "Quick Scan", "subnet" to "Subnet Scan", "ping" to "Ping").forEach { (value, label) ->
                FilterChip(
                    selected = scanMode == value,
                    onClick = { scanMode = value },
                    label = { Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                        selectedLabelColor = PrimaryGreen
                    )
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { startScan() },
            enabled = !scanState.isScanning && targetIp.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (scanState.isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (scanState.isScanning) "Scanning..." else "Start Scan",
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        if (scanState.isScanning) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { scanState.progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = PrimaryGreen,
                trackColor = Color.DarkGray
            )
            Spacer(Modifier.height(4.dp))
            Text(scanState.message, color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(Modifier.height(12.dp))

        if (results.isNotEmpty()) {
            Text("${results.size} host(s) found", color = PrimaryGreen, fontSize = 13.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results) { host ->
                HostCard(host = host, isExpanded = expandedHost == host.ip,
                    onToggle = { expandedHost = if (expandedHost == host.ip) null else host.ip })
            }
        }
    }
}

@Composable
private fun HostCard(host: ScanTarget, isExpanded: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        onClick = onToggle
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (host.isAlive) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (host.isAlive) SuccessGreen else ErrorRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(host.ip, color = Color.White, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Text("${host.openPorts.size} open", color = if (host.openPorts.isNotEmpty()) WarningOrange else Color.Gray,
                    fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                if (host.pingTime > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text("${host.pingTime}ms", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
            if (host.hostname.isNotBlank()) {
                Text(host.hostname, color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 24.dp))
            }
            if (host.osGuess.isNotBlank()) {
                Text("OS: ${host.osGuess}", color = AccentBlue, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 24.dp))
            }

            if (isExpanded && host.openPorts.isNotEmpty()) {
                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))
                host.openPorts.forEach { port ->
                    Row(modifier = Modifier.padding(start = 24.dp, top = 2.dp)) {
                        Text("${port.port}/tcp", color = WarningOrange, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(port.service, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        if (port.version.isNotBlank()) {
                            Spacer(Modifier.width(4.dp))
                            Text(port.version, color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
