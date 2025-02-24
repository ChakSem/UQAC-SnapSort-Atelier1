
// HomeScreen.kt
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.snapsort.R

@OptIn(ExperimentalMaterial3Api::class)
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Image(
                painter = painterResource(id = R.drawable.logo_snapsort),
                contentDescription = "Logo SnapSort",
                modifier = Modifier
                    .width(200.dp)
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Commencer à trier votre téléphone",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = termsChecked,
                    onCheckedChange = {
                        termsChecked = it
                        enableButton = it
                    }
                )
                Text(
                    text = "J'accepte les ",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "conditions d'utilisation",
                    fontSize = 14.sp,
                    color = Color.Blue,
                    modifier = Modifier.clickable { showTermsDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { navController.navigate("WifiResultsScreen") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (enableButton) Color(0xFF6200EE) else Color.Gray,
                    contentColor = Color.White
                ),
                enabled = enableButton
            ) {
                Text(text = "Activer", color = Color.White)
            }

            Button(
                onClick = { /* Gérer l'action du bouton Tutoriel */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray,
                    contentColor = Color.Black
                )
            ) {
                Text(text = "Tutoriel", color = Color.Black)
            }
        }

        if (showTermsDialog) {
            AlertDialog(
                onDismissRequest = { showTermsDialog = false },
                title = { Text("Conditions d'utilisation") },
                text = {
                    Column(modifier = Modifier.verticalScroll(scrollState)) {
                        Text(
                            text = "TODO : Insérer ici les conditions d'utilisation.\n\n".repeat(20),
                            fontSize = 14.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showTermsDialog = false
                            termsChecked = true
                            enableButton = true
                        },
                        enabled = scrollState.value == scrollState.maxValue
                    ) {
                        Text("Continuer")
                    }
                },
                dismissButton = {
                    Button(onClick = { showTermsDialog = false }) {
                        Text("Annuler")
                    }
                }
            )
        }
    }
}
