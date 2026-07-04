package com.portscanpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portscanpro.ui.theme.*

data class ToolCard(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

private val tools = listOf(
    ToolCard("network", "Network\nScanner", "Ping sweep, port scan, service detection", Icons.Default.WifiFind, PrimaryGreen),
    ToolCard("devices", "Device\nDiscovery", "Find hosts, MAC vendor lookup, WiFi scan", Icons.Default.Devices, AccentBlue),
    ToolCard("webvuln", "Web Vuln\nScanner", "Directory brute force, security header check", Icons.Default.Security, WarningOrange),
    ToolCard("reports", "Reports\n& History", "Export PDF/HTML, view scan history", Icons.Default.Assessment, PrimaryGreen),
)

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp)
    ) {
        Text(
            text = "PortScanPro",
            color = PrimaryGreen,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Ethical Hacking Toolkit",
            color = Color.Gray,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tools) { tool ->
                ToolCardView(tool = tool, onClick = { onNavigate(tool.id) })
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "v1.0.0 • No Root Required",
            color = Color.DarkGray,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun ToolCardView(tool: ToolCard, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .background(tool.color.copy(alpha = 0.15f), MaterialTheme.shapes.small)
                    .padding(8.dp)
            ) {
                Icon(tool.icon, contentDescription = null, tint = tool.color, modifier = Modifier.size(28.dp))
            }
            Column {
                Text(
                    text = tool.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = tool.description,
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 13.sp,
                    maxLines = 2
                )
            }
        }
    }
}
