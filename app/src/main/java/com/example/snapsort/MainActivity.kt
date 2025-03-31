package com.example.snapsort

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.snapsort.ui.HomeScreen
import com.example.snapsort.ui.HotSpotConnection
import com.example.snapsort.ui.ImageDCMILoader
import com.example.snapsort.ui.ImagesTransferConfiguration
import com.example.snapsort.ui.TutorialSwipeableScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavigation()
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("HotSpotConnection") { HotSpotConnection(navController)}
        composable("ImagesTransferConfiguration") { ImagesTransferConfiguration(navController) }
        composable("TutorialSwipeableScreen") { TutorialSwipeableScreen(navController) }
        composable("ImageDCMI") { ImageDCMILoader(navController) }

    }
}

