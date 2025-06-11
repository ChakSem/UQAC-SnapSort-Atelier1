package com.example.snapsort.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.snapsort.ui.screens.HomeScreen
import com.example.snapsort.ui.screens.connection.HotspotConnectionScreen
import com.example.snapsort.ui.screens.transfer.ImageSelectionScreen
import com.example.snapsort.ui.screens.transfer.TransferProgressScreen
import com.example.snapsort.ui.screens.tutorial.TutorialScreen

@Composable
fun SnapSortNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { 300 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -300 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -300 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { 300 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToConnection = {
                    navController.navigate(Screen.Connection.route)
                },
                onNavigateToTutorial = {
                    navController.navigate(Screen.Tutorial.route)
                }
            )
        }

        composable(Screen.Tutorial.route) {
            TutorialScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Connection.route) {
            HotspotConnectionScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToImageSelection = {
                    navController.navigate(Screen.ImageSelection.route)
                }
            )
        }

        composable(Screen.ImageSelection.route) {
            ImageSelectionScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTransfer = { selectedImages ->
                    navController.navigate(Screen.TransferProgress.route)
                }
            )
        }

        composable(Screen.TransferProgress.route) {
            TransferProgressScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onTransferComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}