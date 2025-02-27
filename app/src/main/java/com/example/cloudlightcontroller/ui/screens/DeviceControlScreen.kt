package com.example.cloudlightcontroller.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cloudlightcontroller.ui.theme.*
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

@Composable
fun DeviceControlScreen(
    deviceId: String,
    viewModel: DeviceControlViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    // Collect states from the ViewModel
    val isPowerOn by viewModel.isPowerOn.collectAsStateWithLifecycle()
    val selectedEffect by viewModel.selectedEffect.collectAsStateWithLifecycle()
    val brightness by viewModel.brightness.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val currentColorRgb by viewModel.currentColor.collectAsStateWithLifecycle()
    
    // Convert brightness (0-255) to float (0-1) for the slider
    val brightnessFloat = remember(brightness) { brightness / 255f }
    
    // Convert RGB to compose Color
    val currentColor = remember(currentColorRgb) {
        Color(currentColorRgb.first, currentColorRgb.second, currentColorRgb.third)
    }
    
    val colorPickerController = rememberColorPickerController()
    
    // Effects list
    val effectsList = listOf(
        "solid" to "Solid Color",
        "rainbow" to "Rainbow",
        "breathing" to "Breathing",
        "storm" to "Storm",
        "sunrise" to "Sunrise",
        "sparkle" to "Sparkle",
        "confetti" to "Confetti",
        "fire" to "Fire"
    )
    
    // Handle connection status
    LaunchedEffect(isConnected) {
        if (!isConnected) {
            // Try to connect if not already connected
            viewModel.connectToDevice()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Cloud Light Control")
                        Text(
                            text = if (isConnected) "Connected" else "Connecting...",
                            style = MaterialTheme.typography.caption,
                            color = if (isConnected) Color.Green else Color.Yellow
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (!isConnected) {
            // Show connecting screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Connecting to your Cloud Light...")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Device ID: $deviceId",
                        style = MaterialTheme.typography.caption
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.connectToDevice() }) {
                        Text("Retry Connection")
                    }
                }
            }
        } else {
            // Connected - show control UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Power button
                PowerButton(
                    isOn = isPowerOn,
                    onToggle = { viewModel.setPower(it) }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Brightness slider
                BrightnessControl(
                    brightness = brightnessFloat,
                    onBrightnessChange = { viewModel.setBrightness((it * 255).toInt()) },
                    enabled = isPowerOn
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Color picker
                if (isPowerOn && selectedEffect == "solid") {
                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    HsvColorPicker(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(10.dp),
                        controller = colorPickerController,
                        onColorChanged = { envelope ->
                            viewModel.setComposeColor(envelope.color)
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Effects selector
                if (isPowerOn) {
                    Text(
                        text = "Effects",
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    EffectsGrid(
                        effects = effectsList,
                        selectedEffect = selectedEffect,
                        onEffectSelected = { viewModel.setEffect(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun PowerButton(
    isOn: Boolean,
    onToggle: (Boolean) -> Unit
) {
    OutlinedButton(
        onClick = { onToggle(!isOn) },
        modifier = Modifier.size(80.dp),
        shape = CircleShape,
        border = BorderStroke(2.dp, MaterialTheme.colors.primary),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = if (isOn) MaterialTheme.colors.primary else Color.Transparent,
            contentColor = if (isOn) MaterialTheme.colors.onPrimary else MaterialTheme.colors.primary
        )
    ) {
        Icon(
            imageVector = Icons.Default.Power,
            contentDescription = if (isOn) "Turn Off" else "Turn On",
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun BrightnessControl(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Brightness",
            style = MaterialTheme.typography.h6
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.BrightnessLow,
                contentDescription = "Low Brightness",
                tint = if (enabled) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
            )
            
            Slider(
                value = brightness,
                onValueChange = onBrightnessChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                enabled = enabled
            )
            
            Icon(
                imageVector = Icons.Default.BrightnessHigh,
                contentDescription = "High Brightness",
                tint = if (enabled) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
            )
        }
    }
}

@Composable
fun EffectsGrid(
    effects: List<Pair<String, String>>,
    selectedEffect: String,
    onEffectSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        effects.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (effectId, effectName) ->
                    EffectButton(
                        name = effectName,
                        selected = selectedEffect == effectId,
                        onClick = { onEffectSelected(effectId) },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // If odd number of effects in the row, add spacer
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun EffectButton(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
            contentColor = if (selected) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface
        )
    ) {
        Text(
            text = name,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
} 