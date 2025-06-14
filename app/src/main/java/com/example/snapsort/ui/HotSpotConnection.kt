package com.example.snapsort.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.NetworkRequest
import android.net.Network
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import android.util.Size
import android.view.ViewGroup
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HotSpotConnection(navController: NavController) {
    val context = LocalContext.current
    val wifiManager = remember { 
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager 
    }
    val connectivityManager = remember { 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager 
    }
    val coroutineScope = rememberCoroutineScope()

    // État pour les dialogues
    var showQrScannerDialog by remember { mutableStateOf(false) }
    var showNetworkConfirmDialog by remember { mutableStateOf(false) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    var currentSsid by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("") }

    // Permissions
    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    )
    
    val locationPermissionState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_FINE_LOCATION
    )

    // Fonction pour obtenir le SSID actuel
    fun getCurrentSsid(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val networkInfo = connectivityManager.activeNetwork
                if (networkInfo != null) {
                    val networkCapabilities = connectivityManager.getNetworkCapabilities(networkInfo)
                    if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                        // Pour Android 10+, on ne peut plus récupérer directement le SSID
                        "Réseau WiFi connecté"
                    } else {
                        "Non connecté"
                    }
                } else {
                    "Non connecté"
                }
            } else {
                // Pour Android 9 et inférieur
                @Suppress("DEPRECATION")
                val wifiInfo = wifiManager.connectionInfo
                wifiInfo.ssid?.removeSurrounding("\"") ?: "Non connecté"
            }
        } catch (e: Exception) {
            Log.e("HotSpotConnection", "Erreur lors de la récupération du SSID", e)
            "Non connecté"
        }
    }

    // Fonction pour extraire les informations WiFi du QR code
    fun parseWifiQRCode(qrContent: String): Triple<String, String, String> {
        var ssid = ""
        var password = ""
        var security = "WPA"

        if (qrContent.startsWith("WIFI:")) {
            val parts = qrContent.removePrefix("WIFI:").removeSuffix(";;").split(";")
            parts.forEach { part ->
                when {
                    part.startsWith("S:") -> ssid = part.removePrefix("S:")
                    part.startsWith("P:") -> password = part.removePrefix("P:")
                    part.startsWith("T:") -> security = part.removePrefix("T:")
                }
            }
        }
        return Triple(ssid, password, security)
    }

    // Fonction pour se connecter au WiFi
    fun connectToWifi(ssid: String, password: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Pour les versions antérieures, ouvrir les paramètres WiFi
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            context.startActivity(intent)
            return
        }

        isConnecting = true
        connectionStatus = "Connexion en cours à $ssid..."

        try {
            val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .apply {
                    if (password.isNotEmpty()) {
                        setWpa2Passphrase(password)
                    }
                }
                .build()

            val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(wifiNetworkSpecifier)
                .build()

            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    connectivityManager.bindProcessToNetwork(network)
                    isConnecting = false
                    connectionStatus = "Connecté à $ssid"
                    currentSsid = ssid

                    coroutineScope.launch {
                        navController.navigate("ImagesTransferConfiguration")
                    }
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    isConnecting = false
                    connectionStatus = "Échec de connexion à $ssid"
                    
                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                    context.startActivity(intent)
                }
            }

            connectivityManager.requestNetwork(networkRequest, networkCallback)
        } catch (e: Exception) {
            Log.e("HotSpotConnection", "Erreur lors de la connexion WiFi", e)
            isConnecting = false
            connectionStatus = "Erreur: ${e.message}"
            
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            context.startActivity(intent)
        }
    }

    // Récupérer le SSID actuel au démarrage
    LaunchedEffect(Unit) {
        currentSsid = getCurrentSsid()
    }

    val wifiSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        currentSsid = getCurrentSsid()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = "Wifi Icon",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Connectez-vous au Hotspot",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Text(
                    text = "Pour transférer vos images, connectez-vous au réseau WiFi partagé par votre ordinateur.",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Connection status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = if (currentSsid != "Non connecté")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Réseau actuel",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentSsid,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                        wifiSettingsLauncher.launch(intent)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Paramètres WiFi"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WiFi")
                }
            }
        }

        // Status de connexion
        AnimatedVisibility(
            visible = isConnecting,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = connectionStatus,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action buttons
        Button(
            onClick = {
                showNetworkConfirmDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Continuer avec ce réseau", modifier = Modifier.padding(vertical = 4.dp))
        }

        OutlinedButton(
            onClick = {
                when {
                    !cameraPermissionState.status.isGranted -> {
                        if (cameraPermissionState.status.shouldShowRationale) {
                            showPermissionRationaleDialog = true
                        } else {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !locationPermissionState.status.isGranted -> {
                        locationPermissionState.launchPermissionRequest()
                    }
                    else -> {
                        showQrScannerDialog = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scanner le code QR du réseau")
        }

        Spacer(modifier = Modifier.weight(1f))
    }

    // QR Code Scanner Dialog
    if (showQrScannerDialog) {
        QrScannerDialog(
            onDismiss = { showQrScannerDialog = false },
            onQrCodeScanned = { qrContent ->
                showQrScannerDialog = false
                val (ssid, password, _) = parseWifiQRCode(qrContent)
                
                if (ssid.isNotEmpty()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
                        locationPermissionState.status.isGranted) {
                        connectToWifi(ssid, password)
                    } else {
                        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                        wifiSettingsLauncher.launch(intent)
                    }
                } else {
                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                    wifiSettingsLauncher.launch(intent)
                }
            }
        )
    }

    // Network Confirmation Dialog
    if (showNetworkConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showNetworkConfirmDialog = false },
            title = { Text("Confirmer la connexion") },
            text = {
                Column {
                    Text("Vous êtes connecté au réseau:")
                    Text(
                        text = currentSsid,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text("Voulez-vous continuer avec ce réseau?")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNetworkConfirmDialog = false
                        navController.navigate("ImagesTransferConfiguration")
                    }
                ) {
                    Text("Confirmer")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNetworkConfirmDialog = false }
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    // Permission Rationale Dialog
    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRationaleDialog = false },
            title = { Text("Permission de caméra requise") },
            text = { 
                Text("Pour scanner un code QR WiFi, l'application a besoin d'accéder à votre caméra.") 
            },
            confirmButton = {
                Button(onClick = {
                    showPermissionRationaleDialog = false
                    cameraPermissionState.launchPermissionRequest()
                }) {
                    Text("Autoriser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationaleDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit,
    onQrCodeScanned: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var scanning by remember { mutableStateOf(true) }
    var hasDetectedQR by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Scanner le code QR WiFi",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Fermer",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Camera Preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()

                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setTargetResolution(Size(1280, 720))
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                val executor = Executors.newSingleThreadExecutor()

                                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                    if (scanning && !hasDetectedQR) {
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val image = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )

                                            val scanner = BarcodeScanning.getClient()
                                            scanner.process(image)
                                                .addOnSuccessListener { barcodes ->
                                                    for (barcode in barcodes) {
                                                        val rawValue = barcode.rawValue
                                                        if (barcode.valueType == Barcode.TYPE_WIFI) {
                                                            val ssid = barcode.wifi?.ssid ?: ""
                                                            val password = barcode.wifi?.password ?: ""
                                                            val encryptionType = barcode.wifi?.encryptionType ?: 0

                                                            if (ssid.isNotEmpty()) {
                                                                hasDetectedQR = true
                                                                scanning = false
                                                                onQrCodeScanned("WIFI:S:$ssid;P:$password;T:$encryptionType;;")
                                                            }
                                                        } else if (rawValue?.startsWith("WIFI:") == true) {
                                                            hasDetectedQR = true
                                                            scanning = false
                                                            onQrCodeScanned(rawValue)
                                                        }
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    Log.e("QRScanner", "Erreur de scan: ${it.message}")
                                                }
                                                .addOnCompleteListener {
                                                    imageProxy.close()
                                                }
                                        } else {
                                            imageProxy.close()
                                        }
                                    } else {
                                        imageProxy.close()
                                    }
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                } catch (e: Exception) {
                                    Log.e("QRScanner", "Erreur d'initialisation de la caméra", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // QR Frame Overlay
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .align(Alignment.Center)
                            .border(
                                width = 3.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(24.dp)
                            )
                    )
                }

                // Instructions
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 20.dp)
                ) {
                    Text(
                        text = "Placez le code QR du réseau WiFi dans le cadre pour le scanner",
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}