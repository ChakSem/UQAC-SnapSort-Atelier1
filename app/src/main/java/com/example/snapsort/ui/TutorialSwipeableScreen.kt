package com.example.snapsort.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.WifiFind
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.snapsort.R
import kotlinx.coroutines.launch

/**
 * Représente une page du tutoriel avec toutes les informations nécessaires
 */
data class TutorialPage(
    val title: String,
    val description: String,
    val imageResId: Int,
    val icon: ImageVector? = null,
    val backgroundColor: Color = Color(0xFF2C3E50),
    val primaryColor: Color = Color(0xFF3498DB)
)

/**
 * Écran de tutoriel avec animation et transitions fluides
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TutorialSwipeableScreen(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val tutorialPages = listOf(
        TutorialPage(
            "Bienvenue sur SnapSort !",
            "Découvrez comment organiser facilement vos photos grâce à notre application intuitive qui vous aide à trier, importer et partager vos images.",
            R.drawable.logo_snapsort,
            backgroundColor = Color(0xFF2C3E50),
            primaryColor = Color(0xFF3498DB)
        ),
        TutorialPage(
            "Connexion WiFi Intelligente",
            "Connectez-vous rapidement à vos appareils photo via WiFi. Scannez simplement le QR code ou sélectionnez votre appareil dans la liste des périphériques détectés.",
            R.drawable.logo_snapsort,
            icon = Icons.Outlined.WifiFind,
            backgroundColor = Color(0xFF27AE60),
            primaryColor = Color(0xFFE8F8F5)
        ),
        TutorialPage(
            "Importation Rapide",
            "Importez vos photos en quelques secondes. Prévisualisez, sélectionnez par date ou événement et transférez directement depuis votre appareil photo ou carte mémoire.",
            R.drawable.logo_snapsort,
            icon = Icons.Outlined.CloudUpload,
            backgroundColor = Color(0xFF8E44AD),
            primaryColor = Color(0xFFF5EEF8)
        ),
        TutorialPage(
            "Tri Automatique par IA",
            "Notre technologie avancée d'intelligence artificielle analyse et trie automatiquement vos photos par catégories : paysages, portraits, événements, et bien plus encore.",
            R.drawable.logo_snapsort,
            icon = Icons.Outlined.Sort,
            backgroundColor = Color(0xFFD35400),
            primaryColor = Color(0xFFFDF2E9)
        ),
        TutorialPage(
            "Organisation Personnalisée",
            "Créez des dossiers personnalisés, ajoutez des étiquettes et organisez vos photos selon vos préférences. Retrouvez facilement vos images grâce à notre système de recherche avancé.",
            R.drawable.logo_snapsort,
            icon = Icons.Outlined.Folder,
            backgroundColor = Color(0xFF2980B9),
            primaryColor = Color(0xFFEBF5FB)
        ),
        TutorialPage(
            "Partage Facile",
            "Partagez vos photos avec vos proches en quelques clics. Envoyez des albums entiers par e-mail, message ou sur vos réseaux sociaux préférés.",
            R.drawable.logo_snapsort,
            icon = Icons.Outlined.Share,
            backgroundColor = Color(0xFF16A085),
            primaryColor = Color(0xFFE8F8F5)
        ),
        TutorialPage(
            "Prêt à commencer ?",
            "Vous êtes maintenant prêt à utiliser SnapSort ! Commencez dès aujourd'hui à organiser vos précieux souvenirs de manière simple et efficace.",
            R.drawable.logo_snapsort,
            icon = Icons.Outlined.CheckCircle,
            backgroundColor = Color(0xFF1ABC9C),
            primaryColor = Color(0xFFE8F8F5)
        )
    )

    val pagerState = rememberPagerState(pageCount = { tutorialPages.size })

    // Paramètre pour savoir si nous sommes sur la dernière page
    val isLastPage = pagerState.currentPage == tutorialPages.size - 1

    // Animation pour le bouton flottant qui apparaît sur la dernière page
    val fabOffset by animateDpAsState(
        targetValue = if (isLastPage) 0.dp else 100.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "FAB Animation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tutorialPages[pagerState.currentPage].backgroundColor)
    ) {
        // Fond avec dégradé
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            tutorialPages[pagerState.currentPage].backgroundColor,
                            tutorialPages[pagerState.currentPage].backgroundColor.copy(alpha = 0.7f),
                            tutorialPages[pagerState.currentPage].backgroundColor.copy(alpha = 0.5f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Barre supérieure avec boutons de navigation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Bouton retour (visible seulement si pas sur la première page)
                // Correct version
                androidx.compose.animation.AnimatedVisibility(
                    visible = pagerState.currentPage > 0,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White
                        )
                    }
                }

                // Bouton pour fermer/ignorer le tutoriel
                IconButton(
                    onClick = {
                        navController.navigate("home") {
                            popUpTo("tutorialSwipeableScreen") { inclusive = true }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Ignorer",
                        tint = Color.White
                    )
                }
            }

            // Contenu principal - le pager horizontal
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                TutorialPageContent(tutorialPages[page], page, pagerState)
            }

            // Navigation et indicateurs de page
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Indicateurs de page (points)
                PageIndicators(
                    pageCount = tutorialPages.size,
                    currentPage = pagerState.currentPage,
                    selectedColor = tutorialPages[pagerState.currentPage].primaryColor,
                    unselectedColor = tutorialPages[pagerState.currentPage].primaryColor.copy(alpha = 0.3f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Bouton Suivant/Commencer
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage < tutorialPages.size - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                navController.navigate("home") {
                                    popUpTo("tutorialSwipeableScreen") { inclusive = true }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tutorialPages[pagerState.currentPage].primaryColor,
                        contentColor = tutorialPages[pagerState.currentPage].backgroundColor
                    )
                ) {
                    Text(
                        text = if (pagerState.currentPage < tutorialPages.size - 1) "Suivant" else "Commencer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (pagerState.currentPage < tutorialPages.size - 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null
                        )
                    }
                }

                // Bouton pour ignorer le tutoriel
                if (!isLastPage) {
                    TextButton(
                        onClick = {
                            navController.navigate("home") {
                                popUpTo("tutorialSwipeableScreen") { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Ignorer le tutoriel",
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // Bouton flottant sur la dernière page
        AnimatedVisibility(
            visible = isLastPage,
            enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(y = fabOffset)
                .padding(24.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    navController.navigate("home") {
                        popUpTo("tutorialSwipeableScreen") { inclusive = true }
                    }
                },
                containerColor = tutorialPages[pagerState.currentPage].primaryColor,
                contentColor = tutorialPages[pagerState.currentPage].backgroundColor
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Commencer",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * Affiche le contenu d'une page de tutoriel
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TutorialPageContent(
    page: TutorialPage,
    pageIndex: Int,
    pagerState: PagerState
) {
    // Détection du changement de page pour l'animation
    val isCurrentPage = pageIndex == pagerState.currentPage
    val isFirstAppear = remember { mutableStateOf(true) }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == pageIndex) {
            isFirstAppear.value = false
        }
    }

    // Animation pour les éléments entrants
    val animateOffset by animateDpAsState(
        targetValue = if (isCurrentPage || !isFirstAppear.value) 0.dp else 100.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "Content Animation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .offset(y = animateOffset),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Section de l'image ou icône
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (page.icon != null && pageIndex > 0) {
                // Afficher l'icône pour les pages non initiales
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(180.dp),
                    tint = page.primaryColor
                )
            } else {
                // Afficher l'image pour la première page
                Image(
                    painter = painterResource(id = page.imageResId),
                    contentDescription = page.title,
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Section du texte
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = page.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = page.description,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 24.sp
            )
        }
    }
}

/**
 * Affiche les indicateurs de page (points)
 */
@Composable
fun PageIndicators(
    pageCount: Int,
    currentPage: Int,
    selectedColor: Color,
    unselectedColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { index ->
            val size = animateDpAsState(
                targetValue = if (currentPage == index) 10.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "Indicator Size Animation"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(size.value)
                    .clip(CircleShape)
                    .background(
                        if (currentPage == index) selectedColor else unselectedColor
                    )
            )
        }
    }
}