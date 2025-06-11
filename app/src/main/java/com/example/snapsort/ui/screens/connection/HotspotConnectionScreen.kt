package com.example.snapsort.ui.screens.connection

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.snapsort.ui.components.common.ErrorCard
import com.example.snapsort.ui.components.common.QrCodeScanner
import com.example.snapsort.ui.viewmodel.ConnectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotspotConnectionScreen(
    viewModel: ConnectionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToImageSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showQrScanner by remember { mutableStateOf(false) }
    var showNetworkConfirmDialog by remember { mutableStateOf(false) }

    // Request permissions on first launch
    LaunchedEffect(Unit) {
        // Trigger permission requests and network status check
        viewModel.checkPermissionsAndStartNetworkObservation()
    }

    if (showQrScanner) {
        QrCodeScanner(
            onQrCodeScanned = { qrData ->
                showQrScanner = false
                // Parse QR data for WiFi credentials
                viewModel.handleQrWifiData(qrData)
            },
            onDismiss = { showQrScanner = false }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // Header
            ConnectionHeader(onNavigateBack = onNavigateBack)

            // Connection Status
            uiState.networkConnection?.let { connection ->
                ConnectionStatusCard(
                    connection = connection,
                    isConnecting = uiState.isConnecting,
                    connectionMessage = uiState.connectionMessage
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            ActionButtons(
                isConnected = uiState.networkConnection?.isConnected == true,
                isConnecting = uiState.isConnecting,
                onContinueClick = { showNetworkConfirmDialog = true },
                onScanQrClick = { showQrScanner = true }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Instructions
            InstructionsCard()

            // Error handling
            uiState.error?.let { error ->
                ErrorCard(
                    message = error,
                    onRetry = { viewModel.clearError() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Dialogs
    if (showNetworkConfirmDialog) {
        NetworkConfirmationDialog(
            currentSsid = uiState.networkConnection?.ssid ?: "",
            onConfirm = {
                showNetworkConfirmDialog = false
                onNavigateToImageSelection()
            },
            onDismiss = { showNetworkConfirmDialog = false }
        )
    }
}

@Composable
private fun ConnectionHeader(onNavigateBack: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Retour",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Connexion au Hotspot",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Connectez-vous au réseau WiFi de votre ordinateur",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connection: com.example.snapsort.domain.model.NetworkConnection,
    isConnecting: Boolean,
    connectionMessage: String?
) {
    val indicatorColor by animateColorAsState(
        targetValue = when {
            isConnecting -> MaterialTheme.colorScheme.tertiary
            connection.isConnected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.error
        },
        animationSpec = tween(300),
        label = "Connection Indicator"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(indicatorColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Réseau actuel",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = connection.ssid,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Status message
            AnimatedVisibility(
                visible = isConnecting || connectionMessage != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Text(
                            text = connectionMessage ?: "Connexion en cours...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(
    isConnected: Boolean,
    isConnecting: Boolean,
    onContinueClick: () -> Unit,
    onScanQrClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isConnected && !isConnecting,
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Continuer avec ce réseau",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }

        OutlinedButton(
            onClick = onScanQrClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !isConnecting,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(imageVector = Icons.Outlined.QrCodeScanner, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scanner le code QR WiFi")
        }
    }
}

@Composable
private fun InstructionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Instructions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "1. Assurez-vous que votre ordinateur partage son réseau WiFi\n" +
                        "2. Connectez votre téléphone à ce réseau\n" +
                        "3. Ou scannez le QR code affiché sur votre ordinateur",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NetworkConfirmationDialog(
    currentSsid: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Confirmer la connexion",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text("Vous êtes connecté au réseau :")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentSsid,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Voulez-vous continuer avec ce réseau ?")
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirmer")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Annuler")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}