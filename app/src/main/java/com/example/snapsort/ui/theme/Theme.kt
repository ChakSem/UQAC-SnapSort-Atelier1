package com.example.snapsort.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// === SCHÉMA DE COULEURS CLAIR - SNAPSORT ===
private val SnapSortLightColorScheme = lightColorScheme(
    // Couleurs principales
    primary = SnapSortBlue700,
    onPrimary = Color.White,
    primaryContainer = SnapSortBlue100,
    onPrimaryContainer = SnapSortBlue900,
    
    // Couleurs secondaires
    secondary = SnapSortOrange600,
    onSecondary = Color.White,
    secondaryContainer = SnapSortOrange100,
    onSecondaryContainer = SnapSortOrange900,
    
    // Couleurs tertiaires
    tertiary = SnapSortGreen600,
    onTertiary = Color.White,
    tertiaryContainer = SnapSortGreen100,
    onTertiaryContainer = SnapSortGreen900,
    
    // Couleurs d'erreur
    error = SnapSortError,
    onError = Color.White,
    errorContainer = Color(0xFFFFEDEA),
    onErrorContainer = Color(0xFF410E0B),
    
    // Couleurs de surface et arrière-plan
    background = SnapSortLightBackground,
    onBackground = SnapSortLightOnBackground,
    surface = SnapSortLightSurface,
    onSurface = SnapSortLightOnSurface,
    surfaceVariant = SnapSortLightSurfaceVariant,
    onSurfaceVariant = SnapSortGray700,
    
    // Couleurs d'outline et autres
    outline = SnapSortGray400,
    outlineVariant = SnapSortGray200,
    scrim = Color.Black.copy(alpha = 0.32f),
    inverseSurface = SnapSortGray800,
    inverseOnSurface = SnapSortGray100,
    inversePrimary = SnapSortBlue200
)

// === SCHÉMA DE COULEURS SOMBRE - SNAPSORT ===
private val SnapSortDarkColorScheme = darkColorScheme(
    // Couleurs principales
    primary = SnapSortBlue300,
    onPrimary = SnapSortBlue900,
    primaryContainer = SnapSortBlue800,
    onPrimaryContainer = SnapSortBlue100,
    
    // Couleurs secondaires
    secondary = SnapSortOrange400,
    onSecondary = SnapSortOrange900,
    secondaryContainer = SnapSortOrange800,
    onSecondaryContainer = SnapSortOrange100,
    
    // Couleurs tertiaires
    tertiary = SnapSortGreen400,
    onTertiary = SnapSortGreen900,
    tertiaryContainer = SnapSortGreen800,
    onTertiaryContainer = SnapSortGreen100,
    
    // Couleurs d'erreur
    error = Color(0xFFFF5449),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    // Couleurs de surface et arrière-plan
    background = SnapSortDarkBackground,
    onBackground = SnapSortDarkOnBackground,
    surface = SnapSortDarkSurface,
    onSurface = SnapSortDarkOnSurface,
    surfaceVariant = SnapSortDarkSurfaceVariant,
    onSurfaceVariant = SnapSortGray400,
    
    // Couleurs d'outline et autres
    outline = SnapSortGray600,
    outlineVariant = SnapSortGray700,
    scrim = Color.Black.copy(alpha = 0.48f),
    inverseSurface = SnapSortGray100,
    inverseOnSurface = SnapSortGray800,
    inversePrimary = SnapSortBlue700
)

// === ANCIEN SCHÉMA (compatibilité) ===
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * Thème principal de l'application SnapSort
 * Inclut la gestion automatique des couleurs dynamiques et des thèmes sombre/clair
 */
@Composable
fun SnapSortTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color est disponible sur Android 12+
    dynamicColor: Boolean = false, // Désactivé par défaut pour garder l'identité visuelle
    // Nouveau paramètre pour forcer le thème professionnel
    useCustomTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Utiliser les couleurs dynamiques du système si activées
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        
        // Utiliser le thème personnalisé SnapSort (recommandé)
        useCustomTheme -> {
            if (darkTheme) SnapSortDarkColorScheme else SnapSortLightColorScheme
        }
        
        // Fallback vers l'ancien thème
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SnapSortTypography,
        content = content
    )
}

/**
 * Thème spécialement conçu pour les écrans de photographie
 * Utilise un arrière-plan plus sombre pour mettre en valeur les images
 */
@Composable
fun SnapSortPhotoTheme(
    content: @Composable () -> Unit
) {
    val photoColorScheme = SnapSortDarkColorScheme.copy(
        background = Color.Black,
        surface = Color(0xFF1A1A1A),
        surfaceVariant = Color(0xFF2A2A2A)
    )

    MaterialTheme(
        colorScheme = photoColorScheme,
        typography = SnapSortTypography,
        content = content
    )
}

/**
 * Thème pour les écrans de paramètres et configuration
 * Plus clair et aéré pour la lisibilité
 */
@Composable
fun SnapSortSettingsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val settingsColorScheme = if (darkTheme) {
        SnapSortDarkColorScheme.copy(
            surface = SnapSortGray900,
            surfaceVariant = SnapSortGray800
        )
    } else {
        SnapSortLightColorScheme.copy(
            surface = SnapSortGray50,
            surfaceVariant = Color.White
        )
    }

    MaterialTheme(
        colorScheme = settingsColorScheme,
        typography = SnapSortTypography,
        content = content
    )
}