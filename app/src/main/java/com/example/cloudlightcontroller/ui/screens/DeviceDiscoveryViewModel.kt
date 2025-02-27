package com.example.cloudlightcontroller.ui.screens

import android.bluetooth.le.ScanResult
import android.util.Log
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
class DeviceDiscoveryViewModel @Inject constructor(
    private val bleManager: BleManager
) : ViewModel() {
    
    private val TAG = "DeviceDiscoveryViewModel"
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    val scanResults = bleManager.scanResults
    
    init {
        Log.d(TAG, "ViewModel initialized")
        viewModelScope.launch {
            bleManager.scanResults.collect { results ->
                Log.d(TAG, "Scan results updated: ${results.size} devices")
            }
        }
    }
    
    fun startScan() {
        Log.d(TAG, "Starting scan")
        bleManager.startScan()
        _isScanning.value = true
    }
    
    fun stopScan() {
        Log.d(TAG, "Stopping scan")
        bleManager.stopScan()
        _isScanning.value = false
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared, stopping scan")
        bleManager.stopScan()
    }
} 