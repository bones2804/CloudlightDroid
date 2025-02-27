package com.example.cloudlightcontroller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.cloudlightcontroller.ui.screens.DeviceDiscoveryScreen
import com.example.cloudlightcontroller.ui.screens.DeviceControlScreen
import com.example.cloudlightcontroller.ui.theme.CloudLightControllerTheme

@Composable
fun CloudLightApp() {
    CloudLightControllerTheme {
        val navController = rememberNavController()
        
        NavHost(
            navController = navController,
            startDestination = "discovery"
        ) {
            composable("discovery") {
                DeviceDiscoveryScreen(
                    onDeviceSelected = { deviceId ->
                        // Navigate to control screen with the selected device ID
                        navController.navigate("control/$deviceId")
                    }
                )
            }
            
            composable("control/{deviceId}") { backStackEntry ->
                val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
                DeviceControlScreen(
                    deviceId = deviceId,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
} 