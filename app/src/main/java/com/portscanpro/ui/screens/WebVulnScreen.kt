package com.portscanpro.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portscanpro.engine.WebVulnScanner
import com.portscanpro.model.VulnResult
import com.portscanpro.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun WebVulnScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val scanner = remember { WebVulnScanner() }
    val scanState by scanner.state.collectAsState()

    var targetUrl by remember { mutableStateOf("https://example.com") }
    var results by remember { mutableStateOf<List<VulnResult>>(emptyList()) }

    fun startScan() {
        if (targetUrl.isBlank()) return
        results = emptyList()
        scanner.reset()
        scope.launch {
            results = scanner.scanDirectories(targetUrl)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = WarningOrange)
            }
            Text("Web Vuln Scanner", color = WarningOrange, fontSize = 20.sp,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = targetUrl,
            onValueChange = { targetUrl = it },
            label = { Text("Target URL") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WarningOrange, unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = WarningOrange, cursorColor = WarningOrange,
                focusedTextColor = Color.White, unfocusedTextColor = Color.White
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { startScan() },
            enabled = !scanState.isScanning && targetUrl.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = WarningOrange, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (scanState.isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Icon(Icons.Default.Security, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (scanState.isScanning) "Scanning..." else "Start Directory Scan",
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        if (scanState.isScanning) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { scanState.progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = WarningOrange,
                trackColor = Color.DarkGray
            )
            Spacer(Modifier.height(4.dp))
            Text(scanState.message, color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(Modifier.height(12.dp))

        if (results.isNotEmpty()) {
            Text("${results.size} interesting path(s) found", color = WarningOrange, fontSize = 13.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(results) { result ->
                VulnResultCard(result = result)
            }
        }
    }
}

@Composable
private fun VulnResultCard(result: VulnResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when {
                    result.statusCode in 200..299 -> Icons.Default.CheckCircle
                    result.statusCode in 300..399 -> Icons.Default.Redirect
                    result.statusCode == 401 || result.statusCode == 403 -> Icons.Default.Lock
                    result.statusCode == 500 -> Icons.Default.Error
                    else -> Icons.Default.Info
                }
                val iconColor = when {
                    result.statusCode in 200..299 -> SuccessGreen
                    result.statusCode == 401 || result.statusCode == 403 -> WarningOrange
                    result.statusCode == 500 -> ErrorRed
                    else -> Color.Gray
                }
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("${result.statusCode}", color = iconColor, fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(result.url, color = AccentBlue, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            if (result.title.isNotBlank()) {
                Text("Title: ${result.title}", color = Color.Gray, fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp, modifier = Modifier.padding(start = 24.dp))
            }
            Row(modifier = Modifier.padding(start = 24.dp)) {
                Text(result.contentType, color = Color.DarkGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                if (result.contentLength > 0) {
                    Text(" • ${result.contentLength} bytes", color = Color.DarkGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }
        }
    }
}
