package com.portscanpro.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.portscanpro.data.database.AppDatabase
import com.portscanpro.data.database.ScanEntity
import com.portscanpro.engine.ReportGenerator
import com.portscanpro.model.ScanTarget
import com.portscanpro.model.VulnResult
import com.portscanpro.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val reportGen = remember { ReportGenerator(context) }

    var scans by remember { mutableStateOf<List<ScanEntity>>(emptyList()) }
    var selectedScan by remember { mutableStateOf<ScanEntity?>(null) }
    var exportMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scans = db.scanDao().getAllScans()
    }

    fun saveScan(type: String, target: String, hosts: List<ScanTarget>, vulns: List<VulnResult>) {
        scope.launch {
            val json = Gson().toJson(mapOf("hosts" to hosts, "vulns" to vulns))
            db.scanDao().insertScan(ScanEntity(
                scanType = type, target = target,
                hostsFound = hosts.size,
                portsFound = hosts.sumOf { it.openPorts.size },
                resultsJson = json
            ))
            scans = db.scanDao().getAllScans()
        }
    }

    fun exportPdf(scan: ScanEntity) {
        scope.launch {
            exportMessage = "Generating PDF..."
            val data = parseResults(scan.resultsJson)
            val file = withContext(Dispatchers.IO) {
                reportGen.generatePdfReport(scan.scanType, scan.target, data.first, data.second)
            }
            if (file != null) {
                openFile(context, file)
                exportMessage = "PDF exported: ${file.name}"
            } else {
                exportMessage = "Failed to generate PDF"
            }
        }
    }

    fun exportHtml(scan: ScanEntity) {
        scope.launch {
            exportMessage = "Generating HTML..."
            val data = parseResults(scan.resultsJson)
            val file = withContext(Dispatchers.IO) {
                reportGen.generateHtmlReport(scan.scanType, scan.target, data.first, data.second)
            }
            if (file != null) {
                openFile(context, file)
                exportMessage = "HTML exported: ${file.name}"
            } else {
                exportMessage = "Failed to generate HTML"
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
            Text("Reports & History", color = PrimaryGreen, fontSize = 20.sp,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        if (exportMessage.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(exportMessage, color = PrimaryGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(Modifier.height(12.dp))

        if (scans.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No scan history yet", color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                    Text("Run a scan first, then come back here", color = Color.DarkGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(scans) { scan ->
                    ScanHistoryCard(
                        scan = scan,
                        isSelected = selectedScan?.id == scan.id,
                        onSelect = { selectedScan = if (selectedScan?.id == scan.id) null else scan },
                        onExportPdf = { exportPdf(scan) },
                        onExportHtml = { exportHtml(scan) },
                        onDelete = {
                            scope.launch {
                                db.scanDao().deleteScan(scan)
                                scans = db.scanDao().getAllScans()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanHistoryCard(
    scan: ScanEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onExportPdf: () -> Unit,
    onExportHtml: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) CardBg.copy(alpha = 0.8f) else CardBg)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when (scan.scanType) {
                        "subnet" -> Icons.Default.WifiFind
                        "quick" -> Icons.Default.NetworkCheck
                        "webvuln" -> Icons.Default.Security
                        else -> Icons.Default.Search
                    },
                    contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(scan.scanType.uppercase(), color = PrimaryGreen,
                        fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(scan.target, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Text(dateFormat.format(Date(scan.timestamp)), color = Color.Gray,
                        fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                Text("${scan.hostsFound} hosts", color = AccentBlue, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }

            if (isSelected) {
                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onExportPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PDF", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                    Button(
                        onClick = onExportHtml,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("HTML", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

private fun parseResults(json: String): Pair<List<ScanTarget>, List<VulnResult>> {
    return try {
        val gson = Gson()
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val map: Map<String, Any> = gson.fromJson(json, type)
        val hosts = gson.fromJson(gson.toJson(map["hosts"]), object : TypeToken<List<ScanTarget>>() {}.type) as? List<ScanTarget> ?: emptyList()
        val vulns = gson.fromJson(gson.toJson(map["vulns"]), object : TypeToken<List<VulnResult>>() {}.type) as? List<VulnResult> ?: emptyList()
        Pair(hosts, vulns)
    } catch (_: Exception) {
        Pair(emptyList(), emptyList())
    }
}

private fun openFile(context: Context, file: File) {
    try {
        val uri = Uri.fromFile(file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, when {
                file.name.endsWith(".pdf") -> "application/pdf"
                file.name.endsWith(".html") -> "text/html"
                else -> "*/*"
            })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
