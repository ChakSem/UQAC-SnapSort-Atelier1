package com.example.snapsort.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// === POLICES PERSONNALISÉES SNAPSORT ===
// Note: Vous devrez ajouter ces polices dans res/font/ si vous voulez les utiliser
// Pour l'instant, on utilise les polices système optimisées

/**
 * Police principale pour les titres - Moderne et impactante
 * Utilise la police système par défaut avec des poids optimisés
 */
val SnapSortDisplayFontFamily = FontFamily.Default

/**
 * Police pour le corps de texte - Lisible et claire
 * Optimisée pour la lecture sur mobile
 */
val SnapSortBodyFontFamily = FontFamily.Default

/**
 * Police pour les éléments UI spéciaux - Technique et précise
 * Utilisée pour les boutons, labels, etc.
 */
val SnapSortLabelFontFamily = FontFamily.Default

// === TYPOGRAPHIE SNAPSORT ===
val SnapSortTypography = Typography(
    // === STYLES DE DISPLAY (Très grands titres) ===
    displayLarge = TextStyle(
        fontFamily = SnapSortDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = SnapSortDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = SnapSortDisplayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),

    // === STYLES DE HEADLINES (Titres principaux) ===
    headlineLarge = TextStyle(
        fontFamily = SnapSortDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SnapSortDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SnapSortDisplayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // === STYLES DE TITLES (Titres de sections) ===
    titleLarge = TextStyle(
        fontFamily = SnapSortBodyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SnapSortBodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = SnapSortBodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // === STYLES DE BODY (Corps de texte) ===
    bodyLarge = TextStyle(
        fontFamily = SnapSortBodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SnapSortBodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = SnapSortBodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // === STYLES DE LABELS (Étiquettes et boutons) ===
    labelLarge = TextStyle(
        fontFamily = SnapSortLabelFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = SnapSortLabelFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SnapSortLabelFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// === STYLES PERSONNALISÉS SNAPSORT ===

/**
 * Style pour les titres d'écrans principaux
 */
val SnapSortScreenTitle = TextStyle(
    fontFamily = SnapSortDisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 36.sp,
    letterSpacing = 0.sp
)

/**
 * Style pour les sous-titres descriptifs
 */
val SnapSortSubtitle = TextStyle(
    fontFamily = SnapSortBodyFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.15.sp
)

/**
 * Style pour les textes de boutons principaux
 */
val SnapSortButtonText = TextStyle(
    fontFamily = SnapSortLabelFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.1.sp
)

/**
 * Style pour les textes de boutons secondaires
 */
val SnapSortButtonTextSecondary = TextStyle(
    fontFamily = SnapSortLabelFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.25.sp
)

/**
 * Style pour les métadonnées d'images (taille, date, etc.)
 */
val SnapSortImageMeta = TextStyle(
    fontFamily = SnapSortBodyFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.4.sp
)

/**
 * Style pour les messages d'erreur
 */
val SnapSortErrorText = TextStyle(
    fontFamily = SnapSortBodyFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.25.sp
)

/**
 * Style pour les messages de succès
 */
val SnapSortSuccessText = TextStyle(
    fontFamily = SnapSortBodyFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.25.sp
)

/**
 * Style pour les textes de navigation
 */
val SnapSortNavText = TextStyle(
    fontFamily = SnapSortLabelFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.1.sp
)

/**
 * Style pour les textes d'aide et instructions
 */
val SnapSortHelpText = TextStyle(
    fontFamily = SnapSortBodyFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.3.sp
)

// === ANCIENNE TYPOGRAPHIE (conservée pour compatibilité) ===
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)