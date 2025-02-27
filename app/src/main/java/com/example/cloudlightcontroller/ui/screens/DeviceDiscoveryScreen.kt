package com.example.cloudlightcontroller.ui.screens

import android.bluetooth.le.ScanResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cloudlightcontroller.data.ble.BleManager
import androidx.compose.material.ExperimentalMaterialApi

data class BluetoothDevice(
    val id: String,
    val name: String,
    val signalStrength: Int // RSSI value
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DeviceDiscoveryScreen(
    viewModel: DeviceDiscoveryViewModel = hiltViewModel(),
    onDeviceSelected: (String) -> Unit
) {
    val scanResults by viewModel.scanResults.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    LaunchedEffect(key1 = Unit) {
        // Request any necessary permissions if not already granted
        // This is a backup in case the MainActivity permission flow failed
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Find Your Cloud Light",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Button(
            onClick = { 
                if (isScanning) {
                    viewModel.stopScan()
                } else {
                    viewModel.startScan()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Scan for devices",
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(text = if (isScanning) "Scanning..." else "Scan for CloudLight Devices")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isScanning) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        if (scanResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isScanning) 
                        "Searching for CloudLight devices..." 
                    else 
                        "No CloudLight devices found.\nTap Scan to search for Cloud Lights.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            Text(
                text = "Available CloudLight Devices",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(scanResults) { result ->
                    val device = result.device
                    val name = device.name ?: "Unknown Device"
                    val address = device.address
                    val rssi = result.rssi
                    
                    DeviceItem(
                        device = BluetoothDevice(
                            id = address,
                            name = name,
                            signalStrength = rssi
                        ),
                        onClick = { onDeviceSelected(address) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DeviceItem(
    device: BluetoothDevice,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Signal: ${getSignalDescription(device.signalStrength)}",
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }
            
            Text(
                text = "${device.signalStrength} dBm",
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun getSignalDescription(rssi: Int): String {
    return when {
        rssi > -60 -> "Excellent"
        rssi > -70 -> "Good"
        rssi > -80 -> "Fair"
        else -> "Poor"
    }
} 