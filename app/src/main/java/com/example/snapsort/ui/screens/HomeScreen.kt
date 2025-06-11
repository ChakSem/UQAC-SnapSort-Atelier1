package com.example.snapsort.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snapsort.R
import com.example.snapsort.ui.theme.SnapSortTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToConnection: () -> Unit,
    onNavigateToTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTermsDialog by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }
    var showWelcomeAnimation by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        showWelcomeAnimation = true
    }

    SnapSortTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                // Header Section with Logo
                AnimatedVisibility(
                    visible = showWelcomeAnimation,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn()
                ) {
                    HeaderSection()
                }

                // Main Content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    WelcomeCard()

                    TermsAcceptanceCard(
                        termsAccepted = termsAccepted,
                        onShowTerms = { showTermsDialog = true }
                    )
                }

                // Action Buttons
                ActionButtonsSection(
                    termsAccepted = termsAccepted,
                    onStartTransfer = onNavigateToConnection,
                    onShowTutorial = onNavigateToTutorial
                )
            }
        }

        if (showTermsDialog) {
            TermsDialog(
                onDismiss = { showTermsDialog = false },
                onAccept = {
                    termsAccepted = true
                    showTermsDialog = false
                }
            )
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 32.dp)
    ) {
        Card(
            modifier = Modifier.size(180.dp),
            shape = CircleShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_snapsort),
                    contentDescription = "Logo SnapSort",
                    modifier = Modifier.size(120.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SnapSort",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Organisez vos photos intelligemment",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WelcomeCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Prêt à commencer ?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Connectez-vous à votre ordinateur et transférez vos photos en quelques étapes simples.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TermsAcceptanceCard(
    termsAccepted: Boolean,
    onShowTerms: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (termsAccepted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { if (!termsAccepted) onShowTerms() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = termsAccepted,
                onCheckedChange = { if (!termsAccepted) onShowTerms() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Conditions d'utilisation",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "J'accepte les conditions d'utilisation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Plus d'informations",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ActionButtonsSection(
    termsAccepted: Boolean,
    onStartTransfer: () -> Unit,
    onShowTutorial: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onStartTransfer,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = termsAccepted,
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = if (termsAccepted) 4.dp else 0.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Commencer le transfert",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = onShowTutorial,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.School,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Voir le tutoriel",
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@Composable
private fun TermsDialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    val scrollState = rememberScrollState()
    val canAccept by remember {
        derivedStateOf { scrollState.value >= scrollState.maxValue - 50 }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Conditions d'utilisation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = buildTermsText(),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                enabled = canAccept,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Accepter")
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

private fun buildTermsText(): String {
    return """
        Bienvenue dans SnapSort !
        
        En utilisant cette application, vous acceptez les conditions suivantes :
        
        1. UTILISATION DE L'APPLICATION
        SnapSort est conçu pour vous aider à organiser et transférer vos photos de manière simple et efficace.
        
        2. CONFIDENTIALITÉ
        Vos photos restent privées et ne sont jamais stockées sur nos serveurs. Tous les transferts s'effectuent directement entre vos appareils.
        
        3. AUTORISATIONS
        L'application demande l'accès à vos photos uniquement pour les fonctionnalités de transfert et d'organisation.
        
        4. CONNEXION RÉSEAU
        L'application utilise votre connexion WiFi locale pour communiquer avec vos autres appareils.
        
        5. RESPONSABILITÉ
        Vous êtes responsable de la sauvegarde de vos photos avant utilisation.
        
        6. MODIFICATIONS
        Ces conditions peuvent être mises à jour. Les changements importants vous seront notifiés.
        
        Faites défiler jusqu'en bas pour accepter ces conditions.
    """.trimIndent()
}