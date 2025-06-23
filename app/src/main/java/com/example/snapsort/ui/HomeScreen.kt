// HomeScreen.kt avec le nouveau thème SnapSort
package com.example.snapsort.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.snapsort.R
import com.example.snapsort.ui.theme.*

@Composable
fun HomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var showTermsDialog by remember { mutableStateOf(false) }
    var termsChecked by remember { mutableStateOf(false) }
    var enableButton by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // === SECTION LOGO ET BIENVENUE ===
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo de l'application
                    Image(
                        painter = painterResource(id = R.drawable.logo_snapsort),
                        contentDescription = "Logo SnapSort",
                        modifier = Modifier
                            .size(120.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Titre principal avec le nouveau style
                    Text(
                        text = "SnapSort",
                        style = SnapSortScreenTitle,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    // Sous-titre descriptif
                    Text(
                        text = "Organisez et transférez vos photos facilement",
                        style = SnapSortSubtitle,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // === SECTION PRINCIPALE ===
            Text(
                text = "Commencer à trier votre téléphone",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // === SECTION CONDITIONS D'UTILISATION ===
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = termsChecked,
                        onCheckedChange = {
                            termsChecked = it
                            enableButton = it
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    
                    Text(
                        text = "J'accepte les ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "conditions d'utilisation",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { showTermsDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // === SECTION BOUTONS D'ACTION ===
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bouton principal - Activer
                Button(
                    onClick = { navController.navigate("HotSpotConnection") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (enableButton) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.outline,
                        contentColor = if (enableButton)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    enabled = enableButton,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (enableButton) 6.dp else 0.dp
                    )
                ) {
                    Text(
                        text = "Commencer le transfert",
                        style = SnapSortButtonText
                    )
                }

                // Bouton secondaire - Tutoriel
                OutlinedButton(
                    onClick = { navController.navigate("TutorialSwipeableScreen") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 2.dp
                    )
                ) {
                    Text(
                        text = "Voir le tutoriel",
                        style = SnapSortButtonTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // === SECTION INFORMATIONS ADDITIONNELLES ===
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "💡 Conseil",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Assurez-vous que votre ordinateur et téléphone sont connectés au même réseau WiFi pour un transfert optimal.",
                        style = SnapSortHelpText,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // === DIALOG CONDITIONS D'UTILISATION ===
        if (showTermsDialog) {
            AlertDialog(
                onDismissRequest = { showTermsDialog = false },
                title = { 
                    Text(
                        "Conditions d'utilisation",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(scrollState)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = buildString {
                                    append("Bienvenue dans SnapSort !\n\n")
                                    append("En utilisant cette application, vous acceptez :\n\n")
                                    append("1. Respect de la vie privée\n")
                                    append("• Vos photos restent sur vos appareils\n")
                                    append("• Aucune donnée n'est envoyée vers des serveurs externes\n\n")
                                    append("2. Utilisation responsable\n")
                                    append("• L'application est destinée à l'organisation de photos personnelles\n")
                                    append("• Respectez les droits d'auteur des images\n\n")
                                    append("3. Limitations de responsabilité\n")
                                    append("• Sauvegardez vos photos importantes avant utilisation\n")
                                    append("• L'application est fournie en l'état\n\n")
                                    append("4. Conditions techniques\n")
                                    append("• Connexion WiFi requise pour le transfert\n")
                                    append("• Permissions d'accès aux médias nécessaires\n\n")
                                    append("Merci de faire confiance à SnapSort pour organiser vos souvenirs !")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showTermsDialog = false
                            termsChecked = true
                            enableButton = true
                        },
                        enabled = scrollState.value >= scrollState.maxValue * 0.8f,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Accepter et continuer")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showTermsDialog = false },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Annuler")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}