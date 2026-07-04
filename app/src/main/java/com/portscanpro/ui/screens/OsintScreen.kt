package com.portscanpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.portscanpro.engine.OsintEngine
import com.portscanpro.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OsintScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val engine = remember { OsintEngine() }
    val state by engine.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = AccentBlue)
            }
            Text("OSINT Tools", color = AccentBlue, fontSize = 20.sp,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(8.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CardBg,
            contentColor = AccentBlue
        ) {
            Tab(selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Whois", fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp))
            }
            Tab(selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("DNS", fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp))
            }
            Tab(selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text("Username", fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp))
            }
        }
        Spacer(Modifier.height(12.dp))

        when (selectedTab) {
            0 -> WhoisTab(engine, state)
            1 -> DnsTab(engine, state)
            2 -> UsernameTab(engine, state)
        }
    }
}

@Composable
private fun WhoisTab(engine: OsintEngine, state: OsintEngine.OsintState) {
    val scope = rememberCoroutineScope()
    var domain by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = domain,
            onValueChange = { domain = it },
            label = { Text("Domain") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue, unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = AccentBlue, cursorColor = AccentBlue,
                focusedTextColor = Color.White, unfocusedTextColor = Color.White
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { scope.launch { engine.whoisLookup(domain) } },
            enabled = !state.isRunning && domain.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (state.isRunning && state.currentTool == "WHOIS") {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Lookup WHOIS", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(8.dp))
        if (state.message.isNotBlank() && state.currentTool == "WHOIS") {
            Text(state.message, color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        state.whoisResult?.let { result ->
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = CardBg), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                    whoisRow("Domain", result.domain)
                    whoisRow("Registrar", result.registrar)
                    whoisRow("Created", result.creationDate)
                    whoisRow("Expires", result.expiryDate)
                    whoisRow("Registrant", result.registrant)
                    whoisRow("Country", result.country)
                    if (result.nameServers.isNotEmpty()) {
                        Text("Name Servers:", color = Color.Gray, fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 8.dp))
                        result.nameServers.forEach { ns ->
                            Text("  $ns", color = AccentBlue, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun whoisRow(label: String, value: String) {
    if (value.isNotBlank()) {
        Row(modifier = Modifier.padding(vertical = 2.dp)) {
            Text("$label: ", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun DnsTab(engine: OsintEngine, state: OsintEngine.OsintState) {
    val scope = rememberCoroutineScope()
    var domain by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = domain,
            onValueChange = { domain = it },
            label = { Text("Domain") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen, unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = PrimaryGreen, cursorColor = PrimaryGreen,
                focusedTextColor = Color.White, unfocusedTextColor = Color.White
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { scope.launch { engine.dnsLookup(domain) } },
            enabled = !state.isRunning && domain.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (state.isRunning && state.currentTool == "DNS") {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Icon(Icons.Default.Dns, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("DNS Lookup", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(8.dp))
        if (state.message.isNotBlank() && state.currentTool == "DNS") {
            Text(state.message, color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        state.dnsResult?.let { result ->
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = CardBg), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    dnsRow("A Records", result.aRecords)
                    dnsRow("AAAA Records", result.aaaaRecords)
                    dnsRow("MX Records", result.mxRecords)
                    dnsRow("NS Records", result.nsRecords)
                    dnsRow("TXT Records", result.txtRecords)
                    if (result.cnameRecord.isNotBlank()) {
                        dnsRow("CNAME", listOf(result.cnameRecord))
                    }
                }
            }
        }
    }
}

@Composable
private fun dnsRow(label: String, records: List<String>) {
    if (records.isNotEmpty()) {
        Text("$label:", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 6.dp))
        records.forEach { record ->
            Text("  $record", color = PrimaryGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun UsernameTab(engine: OsintEngine, state: OsintEngine.OsintState) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WarningOrange, unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = WarningOrange, cursorColor = WarningOrange,
                focusedTextColor = Color.White, unfocusedTextColor = Color.White
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { scope.launch { engine.usernameSearch(username) } },
            enabled = !state.isRunning && username.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = WarningOrange, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (state.isRunning && state.currentTool == "USERNAME") {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Icon(Icons.Default.PersonSearch, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Search Username", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(8.dp))
        if (state.message.isNotBlank() && state.currentTool == "USERNAME") {
            Text(state.message, color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        if (state.usernameResults.isNotEmpty()) {
            val found = state.usernameResults.filter { it.found }
            val notFound = state.usernameResults.filter { !it.found }
            Text("Found: ${found.size} | Not found: ${notFound.size}",
                color = WarningOrange, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
            items(state.usernameResults.filter { it.found }) { result ->
                Card(colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.8f)),
                    modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null,
                            tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(result.platform, color = Color.White, fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(result.url, color = AccentBlue, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                }
            }
            items(state.usernameResults.filter { !it.found }) { result ->
                Card(colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cancel, contentDescription = null,
                            tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(result.platform, color = Color.DarkGray, fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
