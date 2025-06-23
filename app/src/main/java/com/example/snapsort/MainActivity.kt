package com.example.snapsort

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.snapsort.ui.HomeScreen
import com.example.snapsort.ui.HotSpotConnection
import com.example.snapsort.ui.ImageDCMILoader
import com.example.snapsort.ui.ImagesTransferConfiguration
import com.example.snapsort.ui.TutorialSwipeableScreen
import com.example.snapsort.ui.theme.SnapSortTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            // Utilisation du nouveau thème SnapSort professionnel
            SnapSortTheme(
                // Force l'utilisation du thème personnalisé (recommandé)
                useCustomTheme = true,
                // Les couleurs dynamiques sont désactivées pour préserver l'identité visuelle
                dynamicColor = false
            ) {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        NavHost(
            navController = navController, 
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") { 
                HomeScreen(navController) 
            }
            composable("HotSpotConnection") { 
                HotSpotConnection(navController) 
            }
            composable("ImagesTransferConfiguration") { 
                ImagesTransferConfiguration(navController) 
            }
            composable("TutorialSwipeableScreen") { 
                TutorialSwipeableScreen(navController) 
            }
            composable("ImageDCMI") { 
                ImageDCMILoader(navController) 
            }
        }
    }
}