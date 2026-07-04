package com.portscanpro.engine

import com.portscanpro.model.PortInfo
import com.portscanpro.model.SERVICE_PORT_MAP
import com.portscanpro.model.ScanTarget
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.Collections

class NetworkScanner {

    private var isCancelled = false

    private val _state = MutableStateFlow(ScanProgress())
    val state: StateFlow<ScanProgress> = _state

    data class ScanProgress(
        val isScanning: Boolean = false,
        val progress: Float = 0f,
        val currentTarget: String = "",
        val discovered: List<ScanTarget> = emptyList(),
        val message: String = ""
    )

    fun cancel() {
        isCancelled = true
    }

    fun reset() {
        isCancelled = false
        _state.value = ScanProgress()
    }

    suspend fun pingHost(ip: String): ScanTarget = withContext(Dispatchers.IO) {
        try {
            val inet = InetAddress.getByName(ip)
            val start = System.nanoTime()
            val reachable = inet.isReachable(2000)
            val elapsed = (System.nanoTime() - start) / 1_000_000
            if (reachable) {
                val hostname = try { inet.getCanonicalHostName() ?: "" } catch (_: Exception) { "" }
                ScanTarget(ip = ip, hostname = hostname, isAlive = true, pingTime = elapsed)
            } else {
                ScanTarget(ip = ip, isAlive = false)
            }
        } catch (_: Exception) {
            ScanTarget(ip = ip, isAlive = false)
        }
    }

    suspend fun scanSubnet(subnet: String, ports: List<Int> = listOf(22, 80, 443, 3306, 3389, 8080, 8443)): List<ScanTarget> = withContext(Dispatchers.IO) {
        isCancelled = false
        _state.value = ScanProgress(isScanning = true, message = "Resolving subnet...")

        val baseIp = subnet.substringBeforeLast(".")
        val startHost = try { subnet.substringAfterLast(".").split("/")[0].split("-")[0].toInt() } catch (_: Exception) { 1 }
        val endHost = try {
            val parts = subnet.substringAfterLast(".")
            if (parts.contains("/")) {
                val cidr = parts.split("/")[1].toInt()
                (1 shl (32 - cidr)) - 2
            } else if (parts.contains("-")) {
                parts.split("-")[1].toInt()
            } else {
                254
            }
        } catch (_: Exception) { 254 }

        val hosts = (startHost..endHost).map { "$baseIp.$it" }
        val aliveHosts = mutableListOf<ScanTarget>()
        var processed = 0

        for ((index, ip) in hosts.withIndex()) {
            if (isCancelled) break
            _state.value = _state.value.copy(
                currentTarget = ip,
                progress = processed.toFloat() / hosts.size,
                message = "Pinging $ip..."
            )
            val target = pingHost(ip)
            if (target.isAlive) {
                aliveHosts.add(target)
            }
            processed++
        }

        if (isCancelled) {
            _state.value = ScanProgress(isScanning = false, message = "Cancelled")
            return@withContext aliveHosts
        }

        _state.value = _state.value.copy(
            message = "Scanning ports on ${aliveHosts.size} hosts..."
        )

        val results = mutableListOf<ScanTarget>()
        var portProgress = 0f
        for ((index, host) in aliveHosts.withIndex()) {
            if (isCancelled) break
            portProgress = index.toFloat() / aliveHosts.size
            _state.value = _state.value.copy(
                progress = portProgress,
                currentTarget = host.ip,
                message = "Scanning ports on ${host.ip}..."
            )
            val openPorts = quickPortScan(host.ip, ports)
            results.add(host.copy(openPorts = openPorts, osGuess = guessOs(openPorts)))
        }

        _state.value = ScanProgress(
            isScanning = false,
            progress = 1f,
            discovered = results,
            message = "Scan complete. ${results.size} hosts found."
        )

        results
    }

    suspend fun scanPorts(ip: String, ports: IntRange = 1..1024): List<PortInfo> = coroutineScope {
        val openPorts = mutableListOf<PortInfo>()
        val semaphore = Semaphore(50)
        val jobs = ports.map { port ->
            async(Dispatchers.IO) {
                if (isCancelled) return@async
                semaphore.withPermit {
                    try {
                        val socket = Socket()
                        socket.connect(InetSocketAddress(ip, port), 300)
                        if (socket.isConnected) {
                            val service = SERVICE_PORT_MAP[port] ?: guessServiceFromBanner(ip, port, socket)
                            synchronized(openPorts) {
                                openPorts.add(PortInfo(port = port, state = "open", service = service))
                            }
                        }
                        socket.close()
                    } catch (_: Exception) {}
                }
            }
        }
        jobs.joinAll()
        openPorts.sortedBy { it.port }
    }

    suspend fun quickPortScan(ip: String, ports: List<Int> = listOf(21, 22, 23, 25, 53, 80, 110, 111, 135, 139, 143, 389, 443, 445, 993, 995, 1433, 1521, 2049, 3306, 3389, 5432, 5900, 6379, 8080, 8443, 9090, 27017)): List<PortInfo> = withContext(Dispatchers.IO) {
        val openPorts = mutableListOf<PortInfo>()
        for (port in ports) {
            if (isCancelled) break
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, port), 500)
                if (socket.isConnected) {
                    val service = SERVICE_PORT_MAP[port] ?: guessServiceFromBanner(ip, port, socket)
                    openPorts.add(PortInfo(port = port, state = "open", service = service))
                }
                socket.close()
            } catch (_: Exception) {}
        }
        openPorts.sortedBy { it.port }
    }

    private fun guessServiceFromBanner(ip: String, port: Int, socket: Socket): String {
        return try {
            socket.soTimeout = 1000
            val reader = socket.getInputStream().bufferedReader()
            val banner = reader.readText().take(100).trim()
            when {
                banner.contains("SSH", true) -> "SSH"
                banner.contains("FTP", true) -> "FTP"
                banner.contains("SMTP", true) -> "SMTP"
                banner.contains("POP3", true) -> "POP3"
                banner.contains("IMAP", true) -> "IMAP"
                banner.contains("HTTP", true) -> "HTTP"
                banner.contains("MySQL", true) -> "MySQL"
                banner.contains("MariaDB", true) -> "MariaDB"
                banner.contains("PostgreSQL", true) -> "PostgreSQL"
                banner.contains("Apache", true) -> "Apache HTTP"
                banner.contains("nginx", true) -> "nginx"
                banner.contains("Microsoft-IIS", true) -> "IIS"
                banner.contains("OpenSSH", true) -> "OpenSSH"
                banner.contains("vsftpd", true) -> "vsftpd"
                banner.contains("ProFTPD", true) -> "ProFTPD"
                banner.contains("Pure-FTPd", true) -> "Pure-FTPd"
                banner.contains("Exim", true) -> "Exim"
                banner.contains("Postfix", true) -> "Postfix"
                banner.contains("Dovecot", true) -> "Dovecot"
                banner.contains("Courier", true) -> "Courier"
                banner.contains("OpenSSL", true) -> "OpenSSL"
                else -> if (banner.isNotBlank()) banner.take(40) else "unknown"
            }
        } catch (_: Exception) {
            SERVICE_PORT_MAP[port] ?: "unknown"
        }
    }

    private fun guessOs(ports: List<PortInfo>): String {
        val serviceSet = ports.map { it.service }.toSet()
        return when {
            ports.any { it.port == 3389 } && ports.any { it.port == 445 } -> "Windows"
            ports.any { it.port == 22 } && ports.any { it.port == 111 } -> "Linux/Unix"
            ports.any { it.port == 22 } && ports.any { it.port == 443 } && ports.any { it.port == 8080 } -> "Linux (likely)"
            ports.any { it.port == 22 } -> "Unix-like"
            ports.any { it.port == 1433 } -> "Windows (MSSQL)"
            ports.any { it.port == 5900 } || ports.any { it.port == 5901 } -> "VNC Host"
            ports.isEmpty() -> "Unknown"
            else -> "Unknown"
        }
    }

    fun getLocalIp(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress ?: ""
                    }
                }
            }
            ""
        } catch (_: Exception) { "" }
    }

    fun getLocalSubnet(): String {
        val ip = getLocalIp()
        return if (ip.isNotBlank()) "${ip.substringBeforeLast(".")}.0/24" else "192.168.1.0/24"
    }
}
