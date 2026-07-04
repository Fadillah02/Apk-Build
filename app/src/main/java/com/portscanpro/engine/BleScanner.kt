package com.portscanpro.engine

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BleScanner(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter = bluetoothManager?.adapter
    private var bleScanner: BluetoothLeScanner? = null

    private val _state = MutableStateFlow(BleScanState())
    val state: StateFlow<BleScanState> = _state

    data class BleDevice(
        val name: String,
        val address: String,
        val rssi: Int,
        val txPower: Int = 0,
        val serviceUuids: List<String> = emptyList(),
        val isConnectable: Boolean = true,
        val manufacturerData: ByteArray? = null,
        val deviceType: String = ""
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BleDevice) return false
            return address == other.address
        }
        override fun hashCode(): Int = address.hashCode()
    }

    data class BleScanState(
        val isScanning: Boolean = false,
        val devices: List<BleDevice> = emptyList(),
        val message: String = "Ready to scan",
        val deviceCount: Int = 0
    )

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: "Unknown"
            val address = device.address
            val rssi = result.rssi

            val scanRecord = result.scanRecord
            val serviceUuids = scanRecord?.serviceUuids?.map { it.uuid.toString() } ?: emptyList()
            val txPower = scanRecord?.txPowerLevel ?: 0
            val manufacturerData = scanRecord?.getManufacturerSpecificData(0)

            val deviceType = when {
                device.name?.contains("AirPods", true) == true -> "AirPods"
                device.name?.contains("Galaxy Buds", true) == true -> "Galaxy Buds"
                device.name?.contains("Sony", true) == true -> "Sony Headphones"
                device.name?.contains("JBL", true) == true -> "JBL Speaker"
                device.name?.contains("Mi Band", true) == true -> "Mi Band"
                device.name?.contains("Fitbit", true) == true -> "Fitbit"
                device.name?.contains("Apple Watch", true) == true -> "Apple Watch"
                device.name?.contains("Samsung", true) == true -> "Samsung Device"
                device.name?.contains("HUAWEI", true) == true -> "Huawei Device"
                device.name?.contains("Xiaomi", true) == true -> "Xiaomi Device"
                device.name?.contains("Smart", true) == true -> "Smart Device"
                device.name?.contains("BLE", true) == true -> "BLE Device"
                device.name?.contains("Beacon", true) == true -> "Beacon"
                else -> ""
            }

            val bleDevice = BleDevice(
                name = name,
                address = address,
                rssi = rssi,
                txPower = txPower,
                serviceUuids = serviceUuids,
                manufacturerData = manufacturerData,
                deviceType = deviceType
            )

            val currentDevices = _state.value.devices.toMutableList()
            val existingIndex = currentDevices.indexOfFirst { it.address == address }
            if (existingIndex >= 0) {
                currentDevices[existingIndex] = bleDevice
            } else {
                currentDevices.add(bleDevice)
            }

            _state.value = _state.value.copy(
                devices = currentDevices.sortedByDescending { it.rssi },
                deviceCount = currentDevices.size,
                message = "Found ${currentDevices.size} devices"
            )
        }

        override fun onScanFailed(errorCode: Int) {
            val errorMsg = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scan already in progress"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE not supported"
                else -> "Scan failed (code: $errorCode)"
            }
            _state.value = _state.value.copy(isScanning = false, message = errorMsg)
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (bluetoothAdapter?.isEnabled != true) {
            _state.value = _state.value.copy(message = "Bluetooth is not enabled")
            return
        }

        bleScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bleScanner == null) {
            _state.value = _state.value.copy(message = "BLE scanner not available")
            return
        }

        _state.value = BleScanState(isScanning = true, message = "Scanning BLE devices...")

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        bleScanner?.startScan(null, settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        bleScanner?.stopScan(scanCallback)
        _state.value = _state.value.copy(
            isScanning = false,
            message = "Scan stopped. ${_state.value.devices.size} devices found."
        )
    }

    fun reset() {
        _state.value = BleScanState()
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
}
