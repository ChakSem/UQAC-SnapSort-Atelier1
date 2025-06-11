package com.example.snapsort.ui.components

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import kotlin.math.abs

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScannerDialog(
    onQrCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // États pour le scanner
    var scanning by remember { mutableStateOf(true) }
    var hasDetectedQR by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var camera: Camera? by remember { mutableStateOf(null) }

    // Permission de caméra
    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    )

    // Demander la permission si nécessaire
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (!cameraPermissionState.status.isGranted) {
        // Dialogue de demande de permission
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Permission requise") },
            text = { Text("L'accès à la caméra est nécessaire pour scanner le QR code WiFi.") },
            confirmButton = {
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Autoriser")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Annuler")
                }
            }
        )
        return
    }

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
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Prévisualisation de la caméra
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        this.scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder()
                                .build()
                                .also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(android.util.Size(1280, 720))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            val executor = Executors.newSingleThreadExecutor()

                            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                if (scanning && !hasDetectedQR) {
                                    processImageProxy(imageProxy) { qrContent ->
                                        if (qrContent.isNotEmpty()) {
                                            hasDetectedQR = true
                                            scanning = false
                                            onQrCodeScanned(qrContent)
                                        }
                                    }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (exc: Exception) {
                                Log.e("QRScanner", "Erreur de liaison de la caméra", exc)
                            }

                        } catch (exc: Exception) {
                            Log.e("QRScanner", "Erreur d'initialisation de la caméra", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay avec cadre de scan
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Barre supérieure
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                RoundedCornerShape(24.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            camera?.let {
                                torchEnabled = !torchEnabled
                                it.cameraControl.enableTorch(torchEnabled)
                            }
                        },
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                RoundedCornerShape(24.dp)
                            )
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Flash",
                            tint = Color.White
                        )
                    }
                }

                // Zone centrale avec cadre de scan
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Cadre de scan avec coins
                    QrScanFrame()
                }

                // Instructions en bas
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Scanner le QR Code WiFi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Placez le QR code dans le cadre pour le scanner automatiquement",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QrScanFrame() {
    val scanLinePosition by remember {
        androidx.compose.animation.core.animateFloatAsState(
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(2000),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "scan_line"
        )
    }

    Box(
        modifier = Modifier.size(280.dp)
    ) {
        // Cadre avec coins
        Canvas(modifier = Modifier.fillMaxSize()) { /* Dessiner les coins du cadre */ }

        // Version simplifiée avec bordure
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 3.dp,
                    color = Color.White,
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            // Ligne de scan animée
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.Red)
                    .align(Alignment.TopStart)
                    .offset(y = (260.dp * scanLinePosition))
            )
        }
    }
}

private fun processImageProxy(
    imageProxy: ImageProxy,
    onQrDetected: (String) -> Unit
) {
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
                    when (barcode.valueType) {
                        Barcode.TYPE_WIFI -> {
                            // QR Code WiFi natif
                            val ssid = barcode.wifi?.ssid ?: ""
                            val password = barcode.wifi?.password ?: ""
                            val encryptionType = when (barcode.wifi?.encryptionType) {
                                Barcode.WiFi.TYPE_WPA -> "WPA"
                                Barcode.WiFi.TYPE_WEP -> "WEP"
                                else -> "NONE"
                            }

                            if (ssid.isNotEmpty()) {
                                val wifiString = "WIFI:T:$encryptionType;S:$ssid;P:$password;;"
                                onQrDetected(wifiString)
                                return@addOnSuccessListener
                            }
                        }
                        Barcode.TYPE_TEXT -> {
                            // QR Code texte qui pourrait être un WiFi
                            val text = barcode.displayValue ?: ""
                            if (text.startsWith("WIFI:")) {
                                onQrDetected(text)
                                return@addOnSuccessListener
                            }
                        }
                        else -> {
                            // Autres types de codes
                            val rawValue = barcode.rawValue ?: ""
                            if (rawValue.startsWith("WIFI:")) {
                                onQrDetected(rawValue)
                                return@addOnSuccessListener
                            }
                        }
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
}

// Fonction utilitaire pour parser les QR codes WiFi
data class WifiCredentials(
    val ssid: String,
    val password: String,
    val encryption: String
)

fun parseWifiQrCode(qrContent: String): WifiCredentials? {
    if (!qrContent.startsWith("WIFI:")) return null

    try {
        val content = qrContent.removePrefix("WIFI:")
        val parts = content.split(";")

        var ssid = ""
        var password = ""
        var encryption = "NONE"

        for (part in parts) {
            when {
                part.startsWith("S:") -> ssid = part.removePrefix("S:")
                part.startsWith("P:") -> password = part.removePrefix("P:")
                part.startsWith("T:") -> encryption = part.removePrefix("T:")
            }
        }

        return if (ssid.isNotEmpty()) {
            WifiCredentials(ssid, password, encryption)
        } else null

    } catch (e: Exception) {
        Log.e("QRParser", "Erreur de parsing: ${e.message}")
        return null
    }
}