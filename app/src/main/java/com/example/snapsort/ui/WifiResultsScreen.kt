package com.example.snapsort.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.snapsort.ui.ScanResult
import com.example.snapsort.ui.WifiScanner
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiResultsScreen(
    navController: NavController,
) {
    var isScanning by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<NetworkDevice>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isScanning = true
        val result = WifiScanner.startScan(context)
        isScanning = false
        when (result) {
            is ScanResult.Success -> {
                devices = result.devices
                errorMessage = null
            }
            is ScanResult.Error -> {
                errorMessage = result.message
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appareils détectés") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!isScanning) {
                                scope.launch {
                                    isScanning = true
                                    val result = WifiScanner.startScan(context)
                                    isScanning = false
                                    when (result) {
                                        is ScanResult.Success -> {
                                            devices = result.devices
                                            errorMessage = null
                                        }
                                        is ScanResult.Error -> {
                                            errorMessage = result.message
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isScanning
                    ) {
                        Icon(Icons.Filled.Refresh, "Rafraîchir")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isScanning -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Recherche des appareils en cours...")
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color.Red,
                            fontSize = 16.sp
                        )
                    }
                }
                devices.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Aucun appareil trouvé",
                            fontSize = 16.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        items(devices) { device ->
                            DeviceCard(device = device)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceCard(device: NetworkDevice) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.hostname,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "IP: ${device.ipAddress}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "MAC: ${device.macAddress}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            // Indicateur de force du signal
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                val signalColor = when {
                    device.signalStrength > -50 -> Color.Green
                    device.signalStrength > -70 -> Color.Yellow
                    else -> Color.Red
                }
                
//                Icon(
//                    imageVector = Icons.Default.Wifi,
//                    contentDescription = "Force du signal",
//                    tint = signalColor,
//                    modifier = Modifier.size(24.dp)
//                )
            }
        }
    }
}