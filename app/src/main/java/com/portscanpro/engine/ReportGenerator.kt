package com.portscanpro.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.portscanpro.model.ScanTarget
import com.portscanpro.model.VulnResult
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportGenerator(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun generatePdfReport(
        scanType: String,
        target: String,
        hosts: List<ScanTarget> = emptyList(),
        vulnResults: List<VulnResult> = emptyList()
    ): File? {
        return try {
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()
            val titlePaint = Paint().apply { textSize = 24f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#00E676") }
            val headerPaint = Paint().apply { textSize = 16f; isFakeBoldText = true; color = android.graphics.Color.WHITE }
            val textPaint = Paint().apply { textSize = 12f; color = android.graphics.Color.LTGRAY }
            val labelPaint = Paint().apply { textSize = 11f; color = android.graphics.Color.GRAY }

            var y = 40f

            canvas.drawText("PortScanPro - Scan Report", 40f, y, titlePaint)
            y += 30f
            canvas.drawText("Date: ${dateFormat.format(Date())}", 40f, y, textPaint)
            y += 18f
            canvas.drawText("Scan Type: $scanType", 40f, y, textPaint)
            y += 18f
            canvas.drawText("Target: $target", 40f, y, textPaint)
            y += 30f

            // Hosts section
            if (hosts.isNotEmpty()) {
                canvas.drawText("=== Host Discovery Results ===", 40f, y, headerPaint)
                y += 22f
                for (host in hosts) {
                    if (y > 800) break
                    val status = if (host.isAlive) "ONLINE" else "OFFLINE"
                    canvas.drawText("${host.ip} ($status) - ${host.openPorts.size} open ports", 40f, y, textPaint)
                    y += 16f
                    if (host.hostname.isNotBlank()) {
                        canvas.drawText("  Hostname: ${host.hostname}", 50f, y, labelPaint); y += 14f
                    }
                    if (host.mac.isNotBlank()) {
                        canvas.drawText("  MAC: ${host.mac} (${host.vendor})", 50f, y, labelPaint); y += 14f
                    }
                    if (host.osGuess.isNotBlank()) {
                        canvas.drawText("  OS: ${host.osGuess}", 50f, y, labelPaint); y += 14f
                    }
                    if (host.openPorts.isNotEmpty()) {
                        canvas.drawText("  Open Ports:", 50f, y, labelPaint); y += 14f
                        for (port in host.openPorts.take(20)) {
                            if (y > 800) break
                            canvas.drawText("    Port ${port.port}/${port.protocol} - ${port.service}", 60f, y, labelPaint)
                            y += 13f
                        }
                        if (host.openPorts.size > 20) {
                            canvas.drawText("    ... and ${host.openPorts.size - 20} more", 60f, y, labelPaint); y += 13f
                        }
                    }
                    y += 6f
                }
            }

            // Vuln results section
            if (vulnResults.isNotEmpty()) {
                if (y > 600) {
                    doc.finishPage(page)
                    val page2Info = PdfDocument.PageInfo.Builder(595, 842, 2).create()
                    val page2 = doc.startPage(page2Info)
                    val canvas2 = page2.canvas
                    y = 40f
                    canvas2.drawText("=== Web Vulnerability Results ===", 40f, y, headerPaint); y += 22f
                    for (r in vulnResults) {
                        if (y > 800) break
                        canvas2.drawText("${r.url}", 40f, y, textPaint); y += 14f
                        canvas2.drawText("  Status: ${r.statusCode} | Size: ${r.contentLength}", 50f, y, labelPaint); y += 14f
                        if (r.title.isNotBlank()) {
                            canvas2.drawText("  Title: ${r.title}", 50f, y, labelPaint); y += 14f
                        }
                        y += 4f
                    }
                    doc.finishPage(page2)
                } else {
                    canvas.drawText("=== Web Vulnerability Results ===", 40f, y, headerPaint); y += 22f
                    for (r in vulnResults) {
                        if (y > 800) {
                            doc.finishPage(page)
                            val page2Info = PdfDocument.PageInfo.Builder(595, 842, 2).create()
                            val page2 = doc.startPage(page2Info)
                            val canvas2 = page2.canvas
                            y = 40f
                        }
                        val targetCanvas = if (y > 40) canvas else { doc.finishPage(page); canvas }
                        targetCanvas.drawText("${r.url}", 40f, y, textPaint); y += 14f
                        targetCanvas.drawText("  Status: ${r.statusCode} | Size: ${r.contentLength}", 50f, y, labelPaint); y += 14f
                        if (r.title.isNotBlank()) {
                            targetCanvas.drawText("  Title: ${r.title}", 50f, y, labelPaint); y += 14f
                        }
                        y += 4f
                    }
                }
            } else {
                canvas.drawText("No web vulnerability results.", 40f, y, textPaint)
            }

            doc.finishPage(page)

            val dir = File(context.getExternalFilesDir("reports"), "PDF")
            dir.mkdirs()
            val file = File(dir, "scan_${fileDateFormat.format(Date())}.pdf")
            FileOutputStream(file).use { doc.writeTo(it) }
            doc.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateHtmlReport(
        scanType: String,
        target: String,
        hosts: List<ScanTarget> = emptyList(),
        vulnResults: List<VulnResult> = emptyList()
    ): File? {
        return try {
            val sb = StringBuilder()
            sb.append("""
            <!DOCTYPE html>
            <html><head>
            <meta charset="utf-8">
            <title>PortScanPro - Scan Report</title>
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { font-family: 'Courier New', monospace; background: #0d0d0d; color: #e0e0e0; padding: 20px; }
                h1 { color: #00e676; border-bottom: 2px solid #00e676; padding-bottom: 10px; margin-bottom: 20px; }
                h2 { color: #00e676; margin: 20px 0 10px; }
                .meta { color: #888; font-size: 14px; margin-bottom: 20px; }
                .host { background: #1a1a1a; border-left: 3px solid #00e676; margin: 10px 0; padding: 10px; }
                .host .ip { color: #00e676; font-weight: bold; font-size: 16px; }
                .host .detail { color: #aaa; font-size: 13px; margin-left: 10px; }
                .port { color: #ffb300; margin-left: 20px; font-size: 13px; }
                .vuln { background: #1a1a1a; border-left: 3px solid #ffb300; margin: 10px 0; padding: 10px; }
                .vuln .url { color: #64b5f6; }
                .vuln .status { color: #ffb300; }
                .vuln .info { color: #aaa; font-size: 13px; }
                .footer { margin-top: 30px; color: #555; font-size: 12px; text-align: center; }
                .online { color: #00e676; }
                .offline { color: #cf6679; }
            </style>
            </head><body>
            <h1>PortScanPro Scan Report</h1>
            <div class="meta">
                <p>Date: ${dateFormat.format(Date())}</p>
                <p>Scan Type: $scanType</p>
                <p>Target: $target</p>
            </div>
            """.trimIndent())

            if (hosts.isNotEmpty()) {
                sb.append("<h2>Host Discovery Results</h2>")
                for (host in hosts) {
                    val statusClass = if (host.isAlive) "online" else "offline"
                    sb.append("""
                    <div class="host">
                        <div class="ip"><span class="$statusClass">●</span> ${host.ip}</div>
                        <div class="detail">Hostname: ${host.hostname.ifBlank { "-" }}</div>
                        <div class="detail">MAC: ${host.mac.ifBlank { "-" }} ${if (host.vendor.isNotBlank()) "(${host.vendor})" else ""}</div>
                        <div class="detail">OS Guess: ${host.osGuess.ifBlank { "-" }}</div>
                        <div class="detail">Ping: ${if (host.pingTime > 0) "${host.pingTime}ms" else "-"}</div>
                        <div class="detail">Open Ports: ${host.openPorts.size}</div>
                        ${host.openPorts.joinToString("") { "<div class=\"port\">Port ${it.port}/${it.protocol} → ${it.service}</div>" }}
                    </div>
                    """.trimIndent())
                }
            }

            if (vulnResults.isNotEmpty()) {
                sb.append("<h2>Web Vulnerability Results</h2>")
                for (r in vulnResults) {
                    sb.append("""
                    <div class="vuln">
                        <div class="url">${r.url}</div>
                        <div class="status">Status: ${r.statusCode}</div>
                        <div class="info">Content-Type: ${r.contentType.ifBlank { "-" }}</div>
                        <div class="info">Size: ${r.contentLength} bytes</div>
                        ${if (r.title.isNotBlank()) "<div class=\"info\">Title: ${r.title}</div>" else ""}
                    </div>
                    """.trimIndent())
                }
            }

            sb.append("""
            <div class="footer">
                <p>Generated by PortScanPro v1.0.0</p>
                <p>${dateFormat.format(Date())}</p>
            </div>
            </body></html>
            """.trimIndent())

            val dir = File(context.getExternalFilesDir("reports"), "HTML")
            dir.mkdirs()
            val file = File(dir, "scan_${fileDateFormat.format(Date())}.html")
            file.writeText(sb.toString())
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
