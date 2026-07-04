package com.portscanpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.portscanpro.ui.screens.*
import com.portscanpro.ui.theme.Background
import com.portscanpro.ui.theme.PrimaryGreen
import com.portscanpro.ui.theme.PortScanProTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PortScanProTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onNavigate = { route ->
                when (route) {
                    "network" -> navController.navigate("network")
                    "devices" -> navController.navigate("devices")
                    "webvuln" -> navController.navigate("webvuln")
                    "reports" -> navController.navigate("reports")
                    "ble" -> navController.navigate("ble")
                    "osint" -> navController.navigate("osint")
                    "capture" -> navController.navigate("capture")
                }
            })
        }
        composable("network") {
            NetworkScanScreen(onBack = { navController.popBackStack() })
        }
        composable("devices") {
            DeviceDiscoveryScreen(onBack = { navController.popBackStack() })
        }
        composable("webvuln") {
            WebVulnScreen(onBack = { navController.popBackStack() })
        }
        composable("reports") {
            ReportScreen(onBack = { navController.popBackStack() })
        }
        composable("ble") {
            BleScannerScreen(onBack = { navController.popBackStack() })
        }
        composable("osint") {
            OsintScreen(onBack = { navController.popBackStack() })
        }
        composable("capture") {
            PacketCaptureScreen(onBack = { navController.popBackStack() })
        }
    }
}
