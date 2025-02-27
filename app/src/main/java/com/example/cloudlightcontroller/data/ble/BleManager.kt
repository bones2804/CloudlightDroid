package com.example.cloudlightcontroller.data.ble

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "BleManager"
        
        // UUIDs matching the ESP32 BLE service and characteristics
        val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val COLOR_CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
        val EFFECT_CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a9")
        val POWER_CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26aa")
        val BRIGHTNESS_CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26ab")
        val STATUS_CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26ac")
    }
    
    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    
    private val _scanResults = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanResults: StateFlow<List<ScanResult>> = _scanResults
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    
    private var bluetoothGatt: BluetoothGatt? = null
    private var isScanning = false
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val devices = _scanResults.value.toMutableList()
            val existingIndex = devices.indexOfFirst { it.device.address == result.device.address }
            
            if (existingIndex >= 0) {
                devices[existingIndex] = result
            } else {
                devices.add(result)
            }
            
            _scanResults.value = devices
            Log.d(TAG, "Device found: ${result.device.name ?: "Unknown"} (${result.device.address}) RSSI: ${result.rssi}")
        }
        
        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            Log.e(TAG, "Scan failed with error code: $errorCode")
        }
    }
    
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server.")
                    _isConnected.value = true
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server.")
                    _isConnected.value = false
                    bluetoothGatt = null
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")
                // Subscribe to notifications if needed
            } else {
                Log.w(TAG, "Service discovery failed with status: $status")
            }
        }
        
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Read characteristic: ${characteristic.uuid}")
                when (characteristic.uuid) {
                    // Handle read responses
                }
            }
        }
        
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.d(TAG, "Characteristic changed: ${characteristic.uuid}")
            when (characteristic.uuid) {
                // Handle notifications
            }
        }
    }
    
    fun startScan() {
        Log.d(TAG, "startScan called")
        
        if (isScanning) {
            Log.d(TAG, "Already scanning, ignoring startScan request")
            return
        }
        
        // Check if device supports Bluetooth LE
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Device does not support Bluetooth")
            return
        }
        
        // Check if Bluetooth is enabled
        if (bluetoothAdapter.isEnabled.not()) {
            Log.e(TAG, "Bluetooth is not enabled")
            return
        }
        
        // Check if we have scanning permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Missing BLUETOOTH_SCAN permission")
                return
            }
        } else {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Missing ACCESS_FINE_LOCATION permission")
                return
            }
        }
        
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BLE Scanner is null, can't scan")
            return
        }
        
        // Create a scan filter for CloudLight devices based on service UUID
        val cloudLightFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        
        // Apply the filter to only scan for CloudLight devices
        val filters = listOf(cloudLightFilter)
        
        Log.d(TAG, "Using scan filter for CloudLight service UUID: $SERVICE_UUID")
        
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        _scanResults.value = emptyList()
        isScanning = true
        
        try {
            Log.d(TAG, "Starting BLE scan with CloudLight service filter")
            bluetoothLeScanner.startScan(filters, settings, scanCallback)
            Log.d(TAG, "Scan started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start scan: ${e.message}", e)
            isScanning = false
        }
    }
    
    fun stopScan() {
        Log.d(TAG, "stopScan called")
        if (!isScanning) {
            Log.d(TAG, "Not scanning, ignoring stopScan request")
            return
        }
        
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BLE Scanner is null, can't stop scan")
            isScanning = false
            return
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Missing BLUETOOTH_SCAN permission")
                    isScanning = false
                    return
                }
            }
            
            bluetoothLeScanner.stopScan(scanCallback)
            isScanning = false
            Log.d(TAG, "Scan stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop scan: ${e.message}", e)
            isScanning = false
        }
    }
    
    // Connect to a device by address
    fun connectToDevice(deviceAddress: String) {
        Log.d(TAG, "Connecting to device: $deviceAddress")
        
        if (bluetoothAdapter == null) {
            Log.e(TAG, "BluetoothAdapter not initialized")
            return
        }
        
        try {
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Missing BLUETOOTH_CONNECT permission")
                    return
                }
            }
            
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
            Log.d(TAG, "Connection request sent to device")
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device: ${e.message}", e)
        }
    }
    
    // Disconnect from current device
    fun disconnect() {
        if (bluetoothGatt != null) {
            Log.d(TAG, "Disconnecting from device")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Missing BLUETOOTH_CONNECT permission")
                    return
                }
            }
            bluetoothGatt?.disconnect()
        }
    }
    
    // Methods for controlling the Cloud Light
    
    fun setColor(red: Int, green: Int, blue: Int) {
        val service = bluetoothGatt?.getService(SERVICE_UUID) ?: return
        val characteristic = service.getCharacteristic(COLOR_CHAR_UUID) ?: return
        
        characteristic.value = byteArrayOf(red.toByte(), green.toByte(), blue.toByte())
        bluetoothGatt?.writeCharacteristic(characteristic)
        Log.d(TAG, "Setting color to RGB($red, $green, $blue)")
    }
    
    fun setEffect(effect: String) {
        val service = bluetoothGatt?.getService(SERVICE_UUID) ?: return
        val characteristic = service.getCharacteristic(EFFECT_CHAR_UUID) ?: return
        
        characteristic.value = effect.toByteArray()
        bluetoothGatt?.writeCharacteristic(characteristic)
        Log.d(TAG, "Setting effect to: $effect")
    }
    
    fun setPower(isOn: Boolean) {
        val service = bluetoothGatt?.getService(SERVICE_UUID) ?: return
        val characteristic = service.getCharacteristic(POWER_CHAR_UUID) ?: return
        
        characteristic.value = byteArrayOf(if (isOn) 1 else 0)
        bluetoothGatt?.writeCharacteristic(characteristic)
        Log.d(TAG, "Setting power: ${if (isOn) "ON" else "OFF"}")
    }
    
    fun setBrightness(brightness: Int) {
        val service = bluetoothGatt?.getService(SERVICE_UUID) ?: return
        val characteristic = service.getCharacteristic(BRIGHTNESS_CHAR_UUID) ?: return
        
        characteristic.value = byteArrayOf(brightness.toByte())
        bluetoothGatt?.writeCharacteristic(characteristic)
        Log.d(TAG, "Setting brightness to: $brightness")
    }
} 