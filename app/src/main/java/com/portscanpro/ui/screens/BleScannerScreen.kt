package com.portscanpro.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import com.portscanpro.engine.BleScanner
import com.portscanpro.ui.theme.*
import java.util.UUID

@Composable
fun BleScannerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scanner = remember { BleScanner(context) }
    val scanState by scanner.state.collectAsState()
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        hasPermission = perms[Manifest.permission.BLUETOOTH_SCAN] == true || Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasPermission || !hasLocationPermission) {
            val perms = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) perms.add(Manifest.permission.BLUETOOTH_SCAN)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permLauncher.launch(perms.toTypedArray())
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (scanState.isScanning) scanner.stopScan()
                onBack()
            }) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color(0xFF9C27B0))
            }
            Text("BLE Scanner", color = Color(0xFF9C27B0), fontSize = 20.sp,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(8.dp))

        if (!hasPermission) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3E0000)),
                modifier = Modifier.fillMaxWidth()) {
                Text("Bluetooth permission required. Tap to grant.",
                    color = ErrorRed, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (scanState.isScanning) scanner.stopScan() else scanner.startScan()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (scanState.isScanning) ErrorRed else Color(0xFF9C27B0),
                    contentColor = Color.White
                ),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(
                    if (scanState.isScanning) Icons.Default.Stop else Icons.Default.BluetoothSearching,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (scanState.isScanning) "Stop Scan" else "Start BLE Scan",
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(scanState.message, color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)

        if (scanState.isScanning) {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = Color(0xFF9C27B0),
                trackColor = Color.DarkGray
            )
        }

        Spacer(Modifier.height(12.dp))

        if (scanState.devices.isNotEmpty()) {
            Text("${scanState.devices.size} BLE device(s) found", color = Color(0xFF9C27B0),
                fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(scanState.devices) { device ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null,
                                tint = Color(0xFF9C27B0), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(device.name, color = Color.White, fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                                Text(device.address, color = Color.Gray,
                                    fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                if (device.deviceType.isNotBlank()) {
                                    Text(device.deviceType, color = Color(0xFFCE93D8),
                                        fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                val rssiColor = when {
                                    device.rssi > -50 -> SuccessGreen
                                    device.rssi > -70 -> WarningOrange
                                    else -> ErrorRed
                                }
                                Text("${device.rssi} dBm", color = rssiColor,
                                    fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (device.serviceUuids.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text("Services:", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            device.serviceUuids.take(5).forEach { uuid ->
                                val serviceName = resolveUuid(uuid)
                                Text("  $uuid${if (serviceName.isNotBlank()) " ($serviceName)" else ""}",
                                    color = AccentBlue, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                            if (device.serviceUuids.size > 5) {
                                Text("  ...and ${device.serviceUuids.size - 5} more",
                                    color = Color.DarkGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun resolveUuid(uuid: String): String {
    return when {
        uuid.startsWith("00001800") -> "Generic Access"
        uuid.startsWith("00001801") -> "Generic Attribute"
        uuid.startsWith("0000180A") -> "Device Information"
        uuid.startsWith("0000180F") -> "Battery Service"
        uuid.startsWith("0000180D") -> "Heart Rate"
        uuid.startsWith("00001802") -> "Alert Notification"
        uuid.startsWith("00001803") -> "Tx Power"
        uuid.startsWith("00001804") -> "User Data"
        uuid.startsWith("00001805") -> "Current Time"
        uuid.startsWith("00001806") -> "Reference Time Update"
        uuid.startsWith("00001807") -> "Next DST Change"
        uuid.startsWith("00001808") -> "Glucose"
        uuid.startsWith("00001809") -> "Health Thermometer"
        uuid.startsWith("0000180C") -> "User Data"
        uuid.startsWith("00001810") -> "Blood Pressure"
        uuid.startsWith("00001811") -> "Alert Notification"
        uuid.startsWith("00001812") -> "Human Interface Device"
        uuid.startsWith("00001813") -> "Scan Parameters"
        uuid.startsWith("00001814") -> "Running Speed and Cadence"
        uuid.startsWith("00001816") -> "Cycling Speed and Cadence"
        uuid.startsWith("00001818") -> "Cycling Power"
        uuid.startsWith("0000181C") -> "User Data"
        uuid.startsWith("0000FE95") -> "Xiaomi"
        uuid.startsWith("0000FE2C") -> "Google"
        uuid.startsWith("0000FEE0") -> "Xiaomi Mi Band"
        uuid.startsWith("0000FD6F") -> "Eddystone"
        uuid.startsWith("0000FFF0") -> "Custom Service"
        else -> ""
    }
}
