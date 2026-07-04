package com.portscanpro.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

class OsintEngine {

    private val _state = MutableStateFlow(OsintState())
    val state: StateFlow<OsintState> = _state

    data class WhoisResult(
        val domain: String = "",
        val registrar: String = "",
        val creationDate: String = "",
        val expiryDate: String = "",
        val nameServers: List<String> = emptyList(),
        val registrant: String = "",
        val country: String = "",
        val raw: String = ""
    )

    data class DnsResult(
        val domain: String = "",
        val aRecords: List<String> = emptyList(),
        val aaaaRecords: List<String> = emptyList(),
        val mxRecords: List<String> = emptyList(),
        val nsRecords: List<String> = emptyList(),
        val txtRecords: List<String> = emptyList(),
        val cnameRecord: String = "",
        val soaRecord: String = ""
    )

    data class UsernameResult(
        val platform: String = "",
        val url: String = "",
        val status: String = "",
        val found: Boolean = false
    )

    data class OsintState(
        val isRunning: Boolean = false,
        val currentTool: String = "",
        val whoisResult: WhoisResult? = null,
        val dnsResult: DnsResult? = null,
        val usernameResults: List<UsernameResult> = emptyList(),
        val message: String = "Ready"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun whoisLookup(domain: String): WhoisResult = withContext(Dispatchers.IO) {
        _state.value = OsintState(isRunning = true, currentTool = "WHOIS", message = "Looking up $domain...")

        try {
            val cleanDomain = domain.replace(Regex("^https?://"), "").replace(Regex("/.*$"), "").trim()
            val socket = Socket("whois.iana.org", 43)
            socket.soTimeout = 10000

            val writer = socket.getOutputStream().bufferedWriter()
            writer.write("$cleanDomain\r\n")
            writer.flush()

            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val raw = reader.readText()
            socket.close()

            val registrar = extractWhoisField(raw, listOf("Registrar:", "Registrant Organization:", "Registrant:"))
            val creationDate = extractWhoisField(raw, listOf("Creation Date:", "created:", "Created:"))
            val expiryDate = extractWhoisField(raw, listOf("Expiry Date:", "Registry Expiry Date:", "expires:"))
            val nameServers = extractWhoisList(raw, listOf("Name Server:", "nserver:"))
            val registrant = extractWhoisField(raw, listOf("Registrant Organization:", "Registrant Name:"))
            val country = extractWhoisField(raw, listOf("Registrant Country:", "Country:"))

            val result = WhoisResult(
                domain = cleanDomain,
                registrar = registrar,
                creationDate = creationDate,
                expiryDate = expiryDate,
                nameServers = nameServers,
                registrant = registrant,
                country = country,
                raw = raw
            )

            _state.value = OsintState(isRunning = false, currentTool = "WHOIS", whoisResult = result, message = "WHOIS complete for $cleanDomain")
            result
        } catch (e: Exception) {
            val errorResult = WhoisResult(domain = domain, raw = "Error: ${e.message}")
            _state.value = OsintState(isRunning = false, currentTool = "WHOIS", whoisResult = errorResult, message = "WHOIS failed: ${e.message}")
            errorResult
        }
    }

    suspend fun dnsLookup(domain: String): DnsResult = withContext(Dispatchers.IO) {
        _state.value = OsintState(isRunning = true, currentTool = "DNS", message = "Resolving DNS for $domain...")

        try {
            val cleanDomain = domain.replace(Regex("^https?://"), "").replace(Regex("/.*$"), "").trim()

            val inet = InetAddress.getByName(cleanDomain)
            val aRecords = listOf(inet.hostAddress ?: "")

            val aaaaRecords = try {
                val process = Runtime.getRuntime().exec(arrayOf("nslookup", "-type=AAAA", cleanDomain))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = reader.readText()
                process.waitFor()
                Regex("Address:\\s*(\\S+)").findAll(output).map { it.groupValues[1] }.filter { !it.contains(":") }.toList()
            } catch (_: Exception) { emptyList() }

            val mxRecords = try {
                val process = Runtime.getRuntime().exec(arrayOf("nslookup", "-type=MX", cleanDomain))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = reader.readText()
                process.waitFor()
                Regex("mail exchanger = (\\S+)").findAll(output).map { it.groupValues[1] }.toList()
            } catch (_: Exception) { emptyList() }

            val nsRecords = try {
                val process = Runtime.getRuntime().exec(arrayOf("nslookup", "-type=NS", cleanDomain))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = reader.readText()
                process.waitFor()
                Regex("nameserver = (\\S+)").findAll(output).map { it.groupValues[1] }.toList()
            } catch (_: Exception) { emptyList() }

            val txtRecords = try {
                val process = Runtime.getRuntime().exec(arrayOf("nslookup", "-type=TXT", cleanDomain))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = reader.readText()
                process.waitFor()
                Regex("text = \"([^\"]+)\"").findAll(output).map { it.groupValues[1] }.toList()
            } catch (_: Exception) { emptyList() }

            val cnameRecord = try {
                val process = Runtime.getRuntime().exec(arrayOf("nslookup", "-type=CNAME", cleanDomain))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = reader.readText()
                process.waitFor()
                Regex("canonical name = (\\S+)").findAll(output).map { it.groupValues[1] }.firstOrNull() ?: ""
            } catch (_: Exception) { "" }

            val result = DnsResult(
                domain = cleanDomain,
                aRecords = aRecords,
                aaaaRecords = aaaaRecords,
                mxRecords = mxRecords,
                nsRecords = nsRecords,
                txtRecords = txtRecords,
                cnameRecord = cnameRecord
            )

            _state.value = OsintState(isRunning = false, currentTool = "DNS", dnsResult = result, message = "DNS lookup complete for $cleanDomain")
            result
        } catch (e: Exception) {
            val errorResult = DnsResult(domain = domain)
            _state.value = OsintState(isRunning = false, currentTool = "DNS", dnsResult = errorResult, message = "DNS lookup failed: ${e.message}")
            errorResult
        }
    }

    suspend fun usernameSearch(username: String): List<UsernameResult> = withContext(Dispatchers.IO) {
        _state.value = OsintState(isRunning = true, currentTool = "USERNAME", message = "Searching for @$username...")

        val platforms = listOf(
            "GitHub" to "https://github.com/$username",
            "Twitter" to "https://x.com/$username",
            "Instagram" to "https://www.instagram.com/$username/",
            "Reddit" to "https://www.reddit.com/user/$username",
            "LinkedIn" to "https://www.linkedin.com/in/$username",
            "YouTube" to "https://www.youtube.com/@$username",
            "TikTok" to "https://www.tiktok.com/@$username",
            "Pinterest" to "https://www.pinterest.com/$username",
            "Twitch" to "https://www.twitch.tv/$username",
            "Steam" to "https://steamcommunity.com/id/$username",
            "Spotify" to "https://open.spotify.com/user/$username",
            "Medium" to "https://medium.com/@$username",
            "Dev.to" to "https://dev.to/$username",
            "HackerNews" to "https://news.ycombinator.com/user?id=$username",
            "Keybase" to "https://keybase.io/$username",
            "GitLab" to "https://gitlab.com/$username",
            "BitBucket" to "https://bitbucket.org/$username/",
            "Docker Hub" to "https://hub.docker.com/u/$username",
            "npm" to "https://www.npmjs.com/~$username",
            "Patreon" to "https://www.patreon.com/$username",
            "Flickr" to "https://www.flickr.com/people/$username",
            "Behance" to "https://www.behance.net/$username",
            "Dribbble" to "https://dribbble.com/$username",
            "Vimeo" to "https://vimeo.com/$username",
            "SoundCloud" to "https://soundcloud.com/$username",
            "Telegram" to "https://t.me/$username"
        )

        val results = mutableListOf<UsernameResult>()
        var count = 0

        for ((platform, url) in platforms) {
            try {
                val request = Request.Builder().url(url).head().build()
                val response = client.newCall(request).execute()
                val code = response.code
                val found = code in 200..299 || code == 302

                results.add(UsernameResult(
                    platform = platform,
                    url = url,
                    status = "$code",
                    found = found
                ))

                count++
                _state.value = OsintState(
                    isRunning = true,
                    currentTool = "USERNAME",
                    usernameResults = results,
                    message = "Searched $count/${platforms.size} platforms..."
                )
            } catch (_: Exception) {
                results.add(UsernameResult(platform = platform, url = url, status = "Error", found = false))
                count++
            }
        }

        _state.value = OsintState(
            isRunning = false,
            currentTool = "USERNAME",
            usernameResults = results,
            message = "Username search complete. Found on ${results.count { it.found }} platforms."
        )
        results
    }

    private fun extractWhoisField(raw: String, keys: List<String>): String {
        for (line in raw.lines()) {
            val trimmed = line.trim()
            for (key in keys) {
                if (trimmed.startsWith(key, ignoreCase = true)) {
                    return trimmed.substringAfter(key).trim()
                }
            }
        }
        return ""
    }

    private fun extractWhoisList(raw: String, keys: List<String>): List<String> {
        return raw.lines().filter { line ->
            keys.any { key -> line.trim().startsWith(key, ignoreCase = true) }
        }.map { line ->
            keys.firstOrNull { line.trim().startsWith(it, ignoreCase = true) }
                ?.let { line.trim().substringAfter(it).trim() } ?: ""
        }.filter { it.isNotBlank() }
    }
}
