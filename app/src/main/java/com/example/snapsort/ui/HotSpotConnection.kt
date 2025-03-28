package com.example.snapsort.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Wifi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotSpotConnection(
    navController: NavController,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Wifi,
            contentDescription = "Wifi Icon",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Connectez-vous au Hotspot de votre ordinateur",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Pour continuer, veuillez vous connecter au réseau Wi-Fi partagé par votre ordinateur.",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                // Ajoutez ici le code pour vérifier la connexion au hotspot si nécessaire
                // Par exemple, vérifiez si un certain SSID est connecté ou si un point d'accès spécifique est accessible.
                // Si la connexion est réussie, naviguez vers la prochaine étape.
                // Sinon, affichez un message d'erreur.
                // Pour l'instant on navigue directement.
                // TODO: Faire une pop up de confirmation avant de naviguer , en lui affichant le SSID du réseau connecté et lui dire de vérifier que c'est bien le bon réseau.
                navController.navigate("ImagesTransferConfiguration")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continuer")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            // TODO: Faire un slider qui permet de naviguer sur un tutotiel

            onClick = {
                val tutorialUrl = "YOUR_TUTORIAL_URL_HERE" // Replace with your tutorial URL
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tutorialUrl))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Info, contentDescription = "Tutorial")
            Spacer(Modifier.width(8.dp))
            Text("Besoin d'aide ? Tutoriel")
        }
    }
}