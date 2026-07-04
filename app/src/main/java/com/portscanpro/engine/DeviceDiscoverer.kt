package com.portscanpro.engine

import android.content.Context
import android.net.wifi.WifiManager
import com.portscanpro.model.DeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DeviceDiscoverer(private val context: Context) {

    private val macVendorDb = MacVendorDB()

    suspend fun discoverDevices(subnetIp: String): List<DeviceInfo> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<DeviceInfo>()
        val baseIp = subnetIp.substringBeforeLast(".")
        val arpEntries = readArpCache()

        for (i in 1..254) {
            val ip = "$baseIp.$i"
            val inet = InetAddress.getByName(ip)
            val reachable = inet.isReachable(1000)
            val mac = arpEntries[ip] ?: ""
            val vendor = if (mac.isNotBlank()) macVendorDb.lookup(mac) else ""
            val hostname = try {
                inet.getCanonicalHostName()
            } catch (_: Exception) { "" }

            if (reachable || mac.isNotBlank()) {
                devices.add(DeviceInfo(
                    ip = ip,
                    hostname = if (hostname != ip) hostname else "",
                    mac = mac,
                    vendor = vendor,
                    isOnline = reachable
                ))
            }
        }
        devices
    }

    fun scanWifiNetworks(): List<DeviceInfo> {
        val devices = mutableListOf<DeviceInfo>()
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return devices
            val results = wifiManager.scanResults
            for (result in results) {
                val vendor = macVendorDb.lookup(result.BSSID)
                devices.add(DeviceInfo(
                    ip = "",
                    hostname = result.SSID,
                    mac = result.BSSID,
                    vendor = vendor,
                    rssi = result.level,
                    isOnline = true
                ))
            }
        } catch (_: Exception) {}
        return devices
    }

    private fun readArpCache(): Map<String, String> {
        val arpMap = mutableMapOf<String, String>()
        try {
            val process = Runtime.getRuntime().exec("cat /proc/net/arp")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine()
            reader.forEachLine { line ->
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 4) {
                    val ip = parts[0]
                    val mac = parts[3]
                    if (mac.matches(Regex("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}"))) {
                        arpMap[ip] = mac.uppercase()
                    }
                }
            }
            reader.close()
        } catch (_: Exception) {}
        return arpMap
    }

    fun getWifiIp(): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return ""
            val info = wifiManager.connectionInfo ?: return ""
            val ipInt = info.ipAddress
            val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
            buffer.putInt(ipInt)
            val bytes = buffer.array()
            InetAddress.getByAddress(bytes).hostAddress ?: ""
        } catch (_: Exception) { "" }
    }

    fun getWifiSubnet(): String {
        val ip = getWifiIp()
        return if (ip.isNotBlank()) "${ip.substringBeforeLast(".")}.0/24" else "192.168.1.0/24"
    }
}
