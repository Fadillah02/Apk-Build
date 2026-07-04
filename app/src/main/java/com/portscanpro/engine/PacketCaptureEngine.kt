package com.portscanpro.engine

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class PacketCaptureEngine(private val context: Context) {

    private val _state = MutableStateFlow(CaptureState())
    val state: StateFlow<CaptureState> = _state

    data class CapturedPacket(
        val timestamp: Long = System.currentTimeMillis(),
        val sourceIp: String = "",
        val destIp: String = "",
        val sourcePort: Int = 0,
        val destPort: Int = 0,
        val protocol: String = "",
        val length: Int = 0,
        val payloadPreview: String = ""
    )

    data class CaptureState(
        val isCapturing: Boolean = false,
        val packetCount: Long = 0,
        val bytesCaptured: Long = 0,
        val duration: Long = 0,
        val recentPackets: List<CapturedPacket> = emptyList(),
        val message: String = "Ready to capture",
        val protocolStats: Map<String, Long> = emptyMap()
    )

    private val isCapturing = AtomicBoolean(false)
    private val packetCount = AtomicLong(0)
    private val bytesCaptured = AtomicLong(0)
    private var captureJob: Job? = null
    private var startTime: Long = 0
    private val recentPackets = mutableListOf<CapturedPacket>()
    private val protocolStats = mutableMapOf<String, Long>()

    fun startCapture() {
        if (isCapturing.get()) return
        isCapturing.set(true)
        packetCount.set(0)
        bytesCaptured.set(0)
        startTime = System.currentTimeMillis()
        recentPackets.clear()
        protocolStats.clear()

        _state.value = CaptureState(isCapturing = true, message = "Capturing...")

        captureJob = CoroutineScope(Dispatchers.IO).launch {
            while (isCapturing.get()) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - startTime
                _state.value = _state.value.copy(
                    duration = elapsed,
                    message = "Capturing... ${packetCount.get()} packets, ${formatBytes(bytesCaptured.get())}"
                )
            }
        }
    }

    fun stopCapture() {
        isCapturing.set(false)
        captureJob?.cancel()
        _state.value = _state.value.copy(
            isCapturing = false,
            message = "Capture stopped. ${packetCount.get()} packets captured."
        )
    }

    fun addPacket(packet: CapturedPacket) {
        if (!isCapturing.get()) return

        packetCount.incrementAndGet()
        bytesCaptured.addAndGet(packet.length.toLong())

        synchronized(protocolStats) {
            protocolStats[packet.protocol] = (protocolStats[packet.protocol] ?: 0) + 1
        }

        synchronized(recentPackets) {
            recentPackets.add(0, packet)
            if (recentPackets.size > 50) recentPackets.removeAt(recentPackets.lastIndex)
        }

        val elapsed = System.currentTimeMillis() - startTime
        _state.value = _state.value.copy(
            packetCount = packetCount.get(),
            bytesCaptured = bytesCaptured.get(),
            duration = elapsed,
            recentPackets = recentPackets.toList(),
            protocolStats = protocolStats.toMap()
        )
    }

    fun saveCapture(): File? {
        return try {
            val dir = File(context.getExternalFilesDir("captures"), "pcap")
            dir.mkdirs()

            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val fileName = "capture_${dateFormat.format(Date())}.pcap"
            val file = File(dir, fileName)

            FileOutputStream(file).use { fos ->
                val header = ByteArray(24)
                val bb = ByteBuffer.wrap(header)
                bb.putInt(0xa1b2c3d4.toInt())
                bb.putShort(2)
                bb.putShort(4)
                bb.putInt(0)
                bb.putInt(0)
                bb.putInt(65535)
                bb.putInt(1) // link type: Ethernet
                fos.write(header)
            }

            _state.value = _state.value.copy(message = "Saved to ${file.absolutePath}")
            file
        } catch (e: Exception) {
            _state.value = _state.value.copy(message = "Save failed: ${e.message}")
            null
        }
    }

    fun reset() {
        stopCapture()
        packetCount.set(0)
        bytesCaptured.set(0)
        recentPackets.clear()
        protocolStats.clear()
        _state.value = CaptureState()
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}
