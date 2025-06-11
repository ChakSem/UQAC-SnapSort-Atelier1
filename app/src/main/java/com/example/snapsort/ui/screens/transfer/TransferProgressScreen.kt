package com.example.snapsort.ui.screens.transfer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.snapsort.ui.components.common.ErrorCard
import com.example.snapsort.ui.components.common.TransferProgressCard
import com.example.snapsort.ui.viewmodel.TransferViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferProgressScreen(
    viewModel: TransferViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onTransferComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showCancelDialog by remember { mutableStateOf(false) }

    // Handle transfer completion
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            kotlinx.coroutines.delay(2000) // Show success for 2 seconds
            onTransferComplete()
        }
    }

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
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        // Header
        TransferHeader(
            onNavigateBack = if (uiState.isTransferring) null else onNavigateBack,
            onCancelTransfer = if (uiState.isTransferring) {
                { showCancelDialog = true }
            } else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            uiState.isTransferring -> {
                uiState.transferProgress?.let { progress ->
                    TransferInProgressContent(
                        progress = progress,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            uiState.isCompleted -> {
                TransferCompletedContent(
                    transferredCount = uiState.transferProgress?.totalCount ?: 0,
                    modifier = Modifier.weight(1f)
                )
            }

            uiState.error != null -> {
                val errorMessage = uiState.error ?: "Une erreur inconnue s'est produite"
                ErrorCard(
                    message = errorMessage,
                    onRetry = { viewModel.clearError() },
                    modifier = Modifier.weight(1f)
                )
            }

            else -> {
                // Initial state - shouldn't happen normally
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Préparation du transfert...")
                }
            }
        }
    }

    // Cancel confirmation dialog
    if (showCancelDialog) {
        CancelTransferDialog(
            onConfirm = {
                showCancelDialog = false
                viewModel.cancelTransfer()
                onNavigateBack()
            },
            onDismiss = { showCancelDialog = false }
        )
    }
}

@Composable
private fun TransferHeader(
    onNavigateBack: (() -> Unit)?,
    onCancelTransfer: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
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
            onNavigateBack?.let {
                IconButton(
                    onClick = it,
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
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Transfert des photos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Envoi vers votre ordinateur",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            onCancelTransfer?.let {
                IconButton(
                    onClick = it,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Annuler",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferInProgressContent(
    progress: com.example.snapsort.domain.model.TransferProgress,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Progress animation
        val animatedProgress by animateFloatAsState(
            targetValue = progress.progressPercentage,
            animationSpec = tween(300),
            label = "progress"
        )

        // Main progress card
        TransferProgressCard(
            progress = progress,
            modifier = Modifier.fillMaxWidth()
        )

        // Additional details
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Détails du transfert",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TransferDetailRow(
                    label = "Image actuelle",
                    value = progress.currentImageName
                )

                TransferDetailRow(
                    label = "Progression",
                    value = "${progress.transferredCount} / ${progress.totalCount}"
                )

                TransferDetailRow(
                    label = "Vitesse",
                    value = "${String.format("%.1f", progress.speed)} MB/s"
                )

                if (progress.estimatedTimeRemaining > 0) {
                    val minutes = progress.estimatedTimeRemaining / 60
                    val seconds = progress.estimatedTimeRemaining % 60
                    TransferDetailRow(
                        label = "Temps restant",
                        value = "${minutes}:${seconds.toString().padStart(2, '0')}"
                    )
                }
            }
        }

        // Tips card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Gardez l'application ouverte et restez connecté au réseau WiFi pendant le transfert.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun TransferDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun TransferCompletedContent(
    transferredCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success animation
        val scale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "success_scale"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Transfert terminé !",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = "$transferredCount photos ont été transférées avec succès vers votre ordinateur.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )

                Text(
                    text = "Vous pouvez maintenant retrouver vos photos sur votre ordinateur.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun CancelTransferDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Annuler le transfert",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text("Êtes-vous sûr de vouloir annuler le transfert en cours ? Les images déjà transférées ne seront pas supprimées.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Annuler le transfert")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Continuer")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}