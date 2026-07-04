package com.portscanpro.engine

import com.portscanpro.model.VulnResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class WebVulnScanner {

    private var isCancelled = false

    private val _state = MutableStateFlow(WebScanProgress())
    val state: StateFlow<WebScanProgress> = _state

    data class WebScanProgress(
        val isScanning: Boolean = false,
        val progress: Float = 0f,
        val currentPath: String = "",
        val results: List<VulnResult> = emptyList(),
        val message: String = ""
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val defaultWordlist = listOf(
        "admin", "login", "wp-admin", "wp-login", "administrator",
        "backup", "backups", "db_backup", "database", "sql",
        "phpmyadmin", "pma", "mysql", "phpPgAdmin",
        "config", "configuration", "config.php", "config.xml",
        ".env", ".git", ".svn", ".htaccess", ".htpasswd",
        "robots.txt", "sitemap.xml", "crossdomain.xml",
        "api", "api/v1", "api/v2", "graphql",
        "upload", "uploads", "files", "download",
        "test", "testing", "dev", "development", "staging",
        "vendor", "node_modules", "composer.json", "package.json",
        "server-status", "server-info", "phpinfo.php", "info.php",
        "wp-content", "wp-includes", "wp-json",
        "shell", "cmd", "exec", "cgi-bin",
        "manager", "manager/html", "console",
        "swagger", "swagger-ui", "api-doc", "api/docs",
        "static", "assets", "public", "private",
        "logs", "log", "error.log", "access.log",
        "temp", "tmp", "cache",
        "install", "setup", "wizard",
        "xmlrpc.php", "wp-cron.php", "wp-config.php",
        "dashboard", "panel", "cpanel", "webmail",
        "mail", "email", "smtp", "pop3",
        "proxy", "socks", "vpn",
        "index.php", "index.html", "default.php",
        "readme.html", "readme", "changelog.txt", "LICENSE",
        "export", "import", "csv", "xls",
        "user", "users", "customer", "clients",
        "order", "orders", "cart", "checkout",
        "search", "query", "ajax",
        "rss", "feed", "atom",
        "sso", "oauth", "auth", "authenticate",
        ".well-known", ".well-known/security.txt"
    )

    fun cancel() {
        isCancelled = true
    }

    fun reset() {
        isCancelled = false
        _state.value = WebScanProgress()
    }

    suspend fun scanDirectories(url: String, customWordlist: List<String>? = null): List<VulnResult> = withContext(Dispatchers.IO) {
        isCancelled = false
        val baseUrl = url.trimEnd('/')
        val wordlist = customWordlist ?: defaultWordlist
        val results = mutableListOf<VulnResult>()
        _state.value = WebScanProgress(isScanning = true, message = "Starting directory scan")

        for ((index, path) in wordlist.withIndex()) {
            if (isCancelled) break
            val progress = index.toFloat() / wordlist.size
            _state.value = WebScanProgress(
                isScanning = true,
                progress = progress,
                currentPath = path,
                results = results.toList(),
                message = "Checking /$path..."
            )

            try {
                val fullUrl = "$baseUrl/$path"
                val request = Request.Builder().url(fullUrl).get().build()
                val response = client.newCall(request).execute()

                val code = response.code
                val contentType = response.header("Content-Type") ?: ""
                val contentLength = response.body?.contentLength() ?: (response.header("Content-Length") ?: "0").toLongOrNull() ?: 0
                val bodyPreview = response.body?.string()?.take(2000) ?: ""
                val title = extractTitle(bodyPreview)

                val isInteresting = when {
                    code in 200..299 -> true
                    code == 401 || code == 403 -> true
                    code == 500 -> true
                    path.endsWith(".git") || path.endsWith(".env") || path == "phpinfo.php" -> true
                    title.contains("admin", true) || title.contains("login", true) || title.contains("dashboard", true) -> true
                    else -> false
                }

                if (isInteresting) {
                    results.add(VulnResult(
                        url = fullUrl,
                        statusCode = code,
                        contentType = contentType,
                        contentLength = contentLength,
                        isInteresting = isInteresting,
                        title = title
                    ))
                }

                _state.value = WebScanProgress(
                    isScanning = true,
                    progress = progress,
                    currentPath = path,
                    results = results.toList(),
                    message = "Found ${results.size} interesting paths"
                )
            } catch (_: Exception) {
                // connection error, skip path
            }
        }

        _state.value = WebScanProgress(
            isScanning = false,
            progress = 1f,
            results = results,
            message = "Scan complete. ${results.size} paths found."
        )
        results
    }

    suspend fun checkBasicSecurity(url: String): Map<String, Boolean> = withContext(Dispatchers.IO) {
        val baseUrl = url.trimEnd('/')
        val checks = mutableMapOf<String, Boolean>()
        try {
            val response = client.newCall(Request.Builder().url(baseUrl).head().build()).execute()
            val headers = response.headers
            checks["missing_security_headers"] = !headers.names().any { name ->
                name.equals("X-Frame-Options", ignoreCase = true) ||
                name.equals("X-Content-Type-Options", ignoreCase = true) ||
                name.equals("X-XSS-Protection", ignoreCase = true) ||
                name.equals("Content-Security-Policy", ignoreCase = true) ||
                name.equals("Strict-Transport-Security", ignoreCase = true)
            }
        } catch (_: Exception) {
            checks["error"] = true
        }
        checks
    }

    private fun extractTitle(html: String): String {
        val titleRegex = Regex("<title[^>]*>([^<]+)</title>", RegexOption.IGNORE_CASE)
        return titleRegex.find(html)?.groupValues?.getOrNull(1)?.trim() ?: ""
    }
}
