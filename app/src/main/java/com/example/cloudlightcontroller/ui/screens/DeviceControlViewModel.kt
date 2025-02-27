package com.example.cloudlightcontroller.ui.screens

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloudlightcontroller.data.ble.BleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceControlViewModel @Inject constructor(
    private val bleManager: BleManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val TAG = "DeviceControlViewModel"
    
    // Get the device ID from the navigation arguments
    private val deviceId: String = checkNotNull(savedStateHandle["deviceId"])
    
    // State for the UI
    private val _isPowerOn = MutableStateFlow(true)
    val isPowerOn: StateFlow<Boolean> = _isPowerOn.asStateFlow()
    
    private val _brightness = MutableStateFlow(80) // 0-255
    val brightness: StateFlow<Int> = _brightness.asStateFlow()
    
    private val _selectedEffect = MutableStateFlow("solid")
    val selectedEffect: StateFlow<String> = _selectedEffect.asStateFlow()
    
    private val _currentColor = MutableStateFlow(Triple(255, 255, 255)) // RGB
    val currentColor: StateFlow<Triple<Int, Int, Int>> = _currentColor.asStateFlow()
    
    // Connection status from BLE Manager
    val isConnected = bleManager.isConnected
    
    init {
        Log.d(TAG, "Initializing with device ID: $deviceId")
        connectToDevice()
    }
    
    fun connectToDevice() {
        Log.d(TAG, "Connecting to device: $deviceId")
        bleManager.connectToDevice(deviceId)
    }
    
    fun disconnectFromDevice() {
        Log.d(TAG, "Disconnecting from device")
        bleManager.disconnect()
    }
    
    fun setPower(isOn: Boolean) {
        _isPowerOn.value = isOn
        Log.d(TAG, "Setting power: $isOn")
        bleManager.setPower(isOn)
    }
    
    fun setBrightness(value: Int) {
        _brightness.value = value
        Log.d(TAG, "Setting brightness: $value")
        bleManager.setBrightness(value)
    }
    
    fun setEffect(effect: String) {
        _selectedEffect.value = effect
        Log.d(TAG, "Setting effect: $effect")
        bleManager.setEffect(effect)
    }
    
    fun setColor(red: Int, green: Int, blue: Int) {
        _currentColor.value = Triple(red, green, blue)
        Log.d(TAG, "Setting color: RGB($red, $green, $blue)")
        bleManager.setColor(red, green, blue)
    }
    
    // Helper method to handle Compose Color
    fun setComposeColor(color: androidx.compose.ui.graphics.Color) {
        val red = (color.red * 255).toInt()
        val green = (color.green * 255).toInt()
        val blue = (color.blue * 255).toInt()
        setColor(red, green, blue)
    }
    
    override fun onCleared() {
        super.onCleared()
        // Disconnect when the ViewModel is cleared
        disconnectFromDevice()
    }
} 