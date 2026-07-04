package com.portscanpro.ui.screens

import android.content.Intent
import android.net.VpnService
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
import com.portscanpro.engine.PacketCaptureEngine
import com.portscanpro.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PacketCaptureScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val engine = remember { PacketCaptureEngine(context) }
    val state by engine.state.collectAsState()
    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            engine.startCapture()
        }
    }

    fun startCapture() {
        val intent = VpnService.prepare(context)
        if (intent != null) {
            vpnLauncher.launch(intent)
        } else {
            engine.startCapture()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (state.isCapturing) engine.stopCapture()
                onBack()
            }) {
                Icon(Icons.Default.ArrowBack, "Back", tint = WarningOrange)
            }
            Text("Packet Capture", color = WarningOrange, fontSize = 20.sp,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (state.isCapturing) engine.stopCapture() else startCapture()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isCapturing) ErrorRed else WarningOrange,
                    contentColor = if (state.isCapturing) Color.White else Color.Black
                ),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(
                    if (state.isCapturing) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.isCapturing) "Stop Capture" else "Start Capture",
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                )
            }
            Button(
                onClick = {
                    val file = engine.saveCapture()
                    if (file != null) {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(android.net.Uri.fromFile(file), "application/octet-stream")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try { context.startActivity(intent) } catch (_: Exception) {}
                    }
                },
                enabled = !state.isCapturing && state.packetCount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Save", fontFamily = FontFamily.Monospace)
            }
        }
        Spacer(Modifier.height(12.dp))

        // Stats cards
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Packets", "${state.packetCount}", WarningOrange, Modifier.weight(1f))
            StatCard("Duration", formatDuration(state.duration), AccentBlue, Modifier.weight(1f))
            StatCard("Size", formatBytes(state.bytesCaptured), PrimaryGreen, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(state.message, color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

        if (state.isCapturing) {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = WarningOrange, trackColor = Color.DarkGray
            )
        }

        // Protocol stats
        if (state.protocolStats.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Protocols:", color = Color.Gray, fontSize = 11.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                state.protocolStats.forEach { (proto, count) ->
                    Text("$proto: $count", color = WarningOrange, fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Packet list
        if (state.recentPackets.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Recent Packets (${state.recentPackets.size}):", color = Color.Gray,
                fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(top = 4.dp)) {
            items(state.recentPackets.take(20)) { packet ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(CardBg).padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(packet.protocol, color = WarningOrange, fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                    Text("${packet.sourceIp}:${packet.sourcePort}",
                        color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                        modifier = Modifier.weight(1f))
                    Text("→", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text("${packet.destIp}:${packet.destPort}",
                        color = AccentBlue, fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                        modifier = Modifier.weight(1f))
                    Text("${packet.length}B", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = color, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, fontSize = 16.sp)
            Text(label, color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        else -> "${"%.1f".format(bytes / (1024 * 1024.0))} MB"
    }
}
