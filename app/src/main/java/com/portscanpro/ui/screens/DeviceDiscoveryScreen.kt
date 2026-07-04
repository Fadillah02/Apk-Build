package com.portscanpro.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portscanpro.engine.DeviceDiscoverer
import com.portscanpro.model.DeviceInfo
import com.portscanpro.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DeviceDiscoveryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val discoverer = remember { DeviceDiscoverer(context) }

    var devices by remember { mutableStateOf<List<DeviceInfo>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var scanMode by remember { mutableStateOf("arp") }
    var message by remember { mutableStateOf("Ready to scan") }

    fun startScan() {
        isScanning = true
        message = "Scanning..."
        scope.launch {
            devices = when (scanMode) {
                "wifi" -> discoverer.scanWifiNetworks()
                else -> discoverer.discoverDevices(discoverer.getWifiSubnet())
            }
            message = "Found ${devices.size} devices"
            isScanning = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = PrimaryGreen)
            }
            Text("Device Discovery", color = PrimaryGreen, fontSize = 20.sp,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("arp" to "ARP Scan", "wifi" to "WiFi Scan").forEach { (value, label) ->
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
            enabled = !isScanning,
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isScanning) "Scanning..." else "Scan Network",
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(8.dp))
        Text(message, color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)

        Spacer(Modifier.height(12.dp))

        if (devices.isNotEmpty()) {
            Text("${devices.size} device(s) found", color = PrimaryGreen, fontSize = 13.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(devices) { device ->
                DeviceCard(device = device)
            }
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DeviceHub,
                contentDescription = null,
                tint = if (device.isOnline) SuccessGreen else Color.Gray,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (device.hostname.isNotBlank()) device.hostname else device.ip,
                    color = Color.White, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, fontSize = 14.sp
                )
                if (device.ip.isNotBlank()) {
                    Text(device.ip, color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
                if (device.mac.isNotBlank()) {
                    Row {
                        Text(device.mac, color = AccentBlue, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        if (device.vendor.isNotBlank()) {
                            Spacer(Modifier.width(4.dp))
                            Text("(${device.vendor})", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
                if (device.rssi != 0) {
                    Text("Signal: ${device.rssi} dBm", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
            Icon(
                if (device.isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = null,
                tint = if (device.isOnline) SuccessGreen else Color.DarkGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
