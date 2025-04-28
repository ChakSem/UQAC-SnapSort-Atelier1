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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.ui.viewinterop.AndroidView
import java.util.concurrent.Executors
import android.util.Size
import android.view.ViewGroup
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.window.DialogProperties
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HotSpotConnection(
    navController: NavController,
) {
    val context = LocalContext.current
    val wifiManager = remember { context.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    val connectivityManager = remember { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    val coroutineScope = rememberCoroutineScope()

    // État pour les dialogues
    var showQrScannerDialog by remember { mutableStateOf(false) }
    var showNetworkConfirmDialog by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var currentSsid by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("") }
    var cameraPermissionRequested by remember { mutableStateOf(false) }
    var locationPermissionRequested by remember { mutableStateOf(false) }



    val locationPermissionState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_FINE_LOCATION,
        onPermissionResult = { isGranted ->
            locationPermissionRequested = true
            if (isGranted) {
                showQrScannerDialog = true
            } else {
                // On peut quand même montrer le scanner, mais on ne pourra pas se connecter automatiquement
                showQrScannerDialog = true
            }
        }
    )

    // Vérifier la permission de caméra
    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA,
        onPermissionResult = { isGranted ->
            cameraPermissionRequested = true
            if (isGranted) {
                // Si la permission de localisation est également nécessaire (Android 10+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    !locationPermissionState.status.isGranted &&
                    !locationPermissionRequested) {
                    locationPermissionState.launchPermissionRequest()
                } else {
                    // Sinon, on peut afficher le scanner directement
                    showQrScannerDialog = true
                }
            } else {
                showPermissionDeniedDialog = true
            }
        }
    )

    // Fonction pour extraire les informations WiFi du QR code
    fun parseWifiQRCode(qrContent: String): Triple<String, String, Int> {
        var ssid = ""
        var password = ""
        var encryptionType = 0 // 1=WEP, 2=WPA, 3=WPA2, 4=WPA2/WPA3, 0=None

        if (qrContent.startsWith("WIFI:")) {
            val regex = """S:([^;]*);P:([^;]*);T:([^;]*);""".toRegex()
            val matchResult = regex.find(qrContent)

            if (matchResult != null) {
                ssid = matchResult.groupValues[1]
                password = matchResult.groupValues[2]
                encryptionType = when (matchResult.groupValues[3].uppercase()) {
                    "WEP" -> 1
                    "WPA" -> 2
                    "WPA2" -> 3
                    "WPA2/WPA3" -> 4
                    else -> 0
                }
            }
        }

        return Triple(ssid, password, encryptionType)
    }

    // Fonction pour se connecter au WiFi
    fun connectToWifi(ssid: String, password: String, encryptionType: Int) {
        isConnecting = true
        connectionStatus = "Connexion en cours à $ssid..."

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Pour Android 10 et supérieur, utiliser NetworkRequest
                val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    .apply {
                        if (password.isNotEmpty()) {
                            when (encryptionType) {
                                0 -> { /* Pas de chiffrement */ }
                                else -> setWpa2Passphrase(password) // WPA2 est le plus couramment utilisé
                            }
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
                        // Forcer les connexions à utiliser ce réseau
                        connectivityManager.bindProcessToNetwork(network)
                        isConnecting = false
                        connectionStatus = "Connecté à $ssid"

                        // Mise à jour du SSID actuel
                        currentSsid = ssid

                        // Navigation vers l'écran suivant après connexion réussie
                        coroutineScope.launch {
                            navController.navigate("ImagesTransferConfiguration")
                        }
                    }

                    override fun onUnavailable() {
                        super.onUnavailable()
                        isConnecting = false
                        connectionStatus = "Échec de connexion à $ssid"

                        // Ouvrir les paramètres WiFi en cas d'échec
                        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                        context.startActivity(intent)
                    }
                }

                // Demande de connexion au réseau spécifié
                connectivityManager.requestNetwork(networkRequest, networkCallback)
            } else {
                // Pour Android 9 et inférieur, ouvrir les paramètres WiFi
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                context.startActivity(intent)
                isConnecting = false
            }
        } catch (e: Exception) {
            Log.e("HotSpotConnection", "Erreur lors de la connexion WiFi: ${e.message}")
            isConnecting = false
            connectionStatus = "Erreur: ${e.message}"

            // Ouvrir les paramètres WiFi en cas d'erreur
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            context.startActivity(intent)
        }
    }

    // Récupérer le SSID actuel
    LaunchedEffect(Unit) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 (API 29) and above
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkInfo = connectivityManager.activeNetwork
                if (networkInfo != null) {
                    val networkCapabilities =
                        connectivityManager.getNetworkCapabilities(networkInfo)
                    if (networkCapabilities != null &&
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                        currentSsid = try {
                            wifiManager.connectionInfo.ssid.removeSurrounding("\"")
                        } catch (e: Exception) {
                            "Non connecté"
                        }
                    } else {
                        currentSsid = "Non connecté"
                    }
                } else {
                    currentSsid = "Non connecté"
                }
            } else {
                // For Android 9 (API 28) and below
                try {
                    val wifiInfo = wifiManager.connectionInfo
                    currentSsid = wifiInfo.ssid.removeSurrounding("\"")
                } catch (e: Exception) {
                    currentSsid = "Non connecté"
                }
            }
        } catch (e: Exception) {
            currentSsid = "Non connecté"
        }
    }

    // Lancement de l'écran des paramètres WiFi
    val wifiSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Rafraîchir le SSID après retour des paramètres
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 (API 29) and above
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkInfo = connectivityManager.activeNetwork
                if (networkInfo != null) {
                    val networkCapabilities =
                        connectivityManager.getNetworkCapabilities(networkInfo)
                    if (networkCapabilities != null &&
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                        currentSsid = try {
                            wifiManager.connectionInfo.ssid.removeSurrounding("\"")
                        } catch (e: Exception) {
                            "Non connecté"
                        }
                    } else {
                        currentSsid = "Non connecté"
                    }
                } else {
                    currentSsid = "Non connecté"
                }
            } else {
                // For Android 9 (API 28) and below
                try {
                    val wifiInfo = wifiManager.connectionInfo
                    currentSsid = wifiInfo.ssid.removeSurrounding("\"")
                } catch (e: Exception) {
                    currentSsid = "Non connecté"
                }
            }
        } catch (e: Exception) {
            currentSsid = "Non connecté"
        }
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

        // Status de connexion (visible seulement pendant la connexion)
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
                // Réinitialiser les flags de demande de permission
                cameraPermissionRequested = false
                locationPermissionRequested = false

                // Demander d'abord la permission de caméra si nécessaire
                if (!cameraPermissionState.status.isGranted) {
                    cameraPermissionState.launchPermissionRequest()
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    !locationPermissionState.status.isGranted) {
                    // Ensuite la permission de localisation si nécessaire pour Android 10+
                    locationPermissionState.launchPermissionRequest()
                } else {
                    // Si toutes les permissions sont accordées, afficher le scanner
                    showQrScannerDialog = true
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
                // Traitement du code QR WiFi
                showQrScannerDialog = false

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    locationPermissionState.status.isGranted) {
                    // Avec Android 10+ et permission de localisation, on peut se connecter directement
                    val (ssid, password, encryptionType) = parseWifiQRCode(qrContent)
                    if (ssid.isNotEmpty()) {
                        connectToWifi(ssid, password, encryptionType)
                    } else {
                        // Si on ne peut pas extraire les informations WiFi, ouvrir les paramètres
                        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                        wifiSettingsLauncher.launch(intent)
                    }
                } else {
                    // Pour les versions antérieures ou sans permission de localisation
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
                        // Navigation vers l'écran suivant
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

    // Permission refusée
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Permission de caméra requise") },
            text = { Text("Pour scanner un code QR WiFi, l'application a besoin d'accéder à votre caméra.") },
            confirmButton = {
                Button(onClick = {
                    showPermissionDeniedDialog = false
                    cameraPermissionState.launchPermissionRequest()
                }) {
                    Text("Réessayer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
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

    // Variables pour le scanner
    var scanning by remember { mutableStateOf(true) }
    var hasDetectedQR by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false // Permet au dialogue d'utiliser la largeur maximale
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f) // Utilise 95% de la largeur de l'écran
                .fillMaxHeight(0.85f) // Utilise 85% de la hauteur de l'écran
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
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 22.sp
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
                                this.scaleType = PreviewView.ScaleType.FILL_CENTER
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
                                                        // Vérifier si c'est un code WiFi
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
                                                            // Format texte pour les QR codes WiFi
                                                            hasDetectedQR = true
                                                            scanning = false
                                                            onQrCodeScanned(rawValue)
                                                        }
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    // Gestion des erreurs
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
                                    Log.e("QRScanner", "Erreur d'initialisation de la caméra: ${e.message}")
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // QR Frame Overlay - Cadre plus grand
                    Box(
                        modifier = Modifier
                            .size(300.dp) // Taille augmentée pour un meilleur cadrage
                            .align(Alignment.Center)
                            .border(
                                width = 3.dp, // Bordure plus visible
                                color = Color(0xFF5D4BA8), // Couleur violette comme sur votre capture
                                shape = RoundedCornerShape(24.dp) // Coins plus arrondis
                            )
                    )

                    // Optionnel: Overlay semi-transparent pour mettre en évidence le cadre
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.5f),
                                        Color.Black.copy(alpha = 0.3f)
                                    )
                                )
                            )
                    )

                    // Trou dans l'overlay pour le cadre QR
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .align(Alignment.Center)
                            .background(Color.Transparent)
                            .clip(RoundedCornerShape(24.dp))
                    )
                }

                // Instructions
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(vertical = 20.dp)
                ) {
                    Text(
                        text = "Placez le code QR du réseau WiFi dans le cadre pour le scanner",
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}