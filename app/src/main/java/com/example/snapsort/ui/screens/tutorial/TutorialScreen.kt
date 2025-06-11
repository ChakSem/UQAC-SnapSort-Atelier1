package com.example.snapsort.ui.screens.tutorial

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class TutorialPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val accentColor: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TutorialScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

    val tutorialPages = remember {
        listOf(
            TutorialPage(
                title = "Bienvenue sur SnapSort !",
                description = "Découvrez comment organiser facilement vos photos grâce à notre application intuitive qui vous aide à trier, importer et partager vos images.",
                icon = Icons.Default.PhotoLibrary,
                backgroundColor = Color(0xFF6750A4),
                accentColor = Color(0xFFEADDFF)
            ),
            TutorialPage(
                title = "Connexion WiFi Simple",
                description = "Connectez-vous rapidement à vos appareils via WiFi. Scannez le QR code ou sélectionnez votre réseau dans la liste des périphériques détectés.",
                icon = Icons.Outlined.Wifi,
                backgroundColor = Color(0xFF00796B),
                accentColor = Color(0xFFE0F2F1)
            ),
            TutorialPage(
                title = "Importation Rapide",
                description = "Importez vos photos en quelques secondes. Prévisualisez, sélectionnez par date ou événement et transférez directement depuis votre appareil.",
                icon = Icons.Outlined.CloudUpload,
                backgroundColor = Color(0xFF1976D2),
                accentColor = Color(0xFFE3F2FD)
            ),
            TutorialPage(
                title = "Organisation Intelligente",
                description = "Organisez vos photos par dossiers, dates et événements. Notre système de tri automatique vous fait gagner un temps précieux.",
                icon = Icons.Outlined.FolderOpen,
                backgroundColor = Color(0xFFE65100),
                accentColor = Color(0xFFFFF3E0)
            ),
            TutorialPage(
                title = "Transfert Sécurisé",
                description = "Tous vos transferts sont sécurisés et s'effectuent directement entre vos appareils. Vos photos restent privées et ne transitent jamais par nos serveurs.",
                icon = Icons.Outlined.Security,
                backgroundColor = Color(0xFF2E7D32),
                accentColor = Color(0xFFE8F5E8)
            ),
            TutorialPage(
                title = "Prêt à commencer ?",
                description = "Vous avez maintenant toutes les clés en main pour utiliser SnapSort efficacement. Commencez dès maintenant à organiser vos précieux souvenirs !",
                icon = Icons.Outlined.CheckCircle,
                backgroundColor = Color(0xFF7B1FA2),
                accentColor = Color(0xFFF3E5F5)
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { tutorialPages.size })
    val isLastPage = pagerState.currentPage == tutorialPages.size - 1

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(tutorialPages[pagerState.currentPage].backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top bar
            TutorialTopBar(
                canGoBack = pagerState.currentPage > 0,
                onNavigateBack = {
                    if (pagerState.currentPage > 0) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    } else {
                        onNavigateBack()
                    }
                },
                onSkip = onNavigateToHome
            )

            // Main content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                TutorialPageContent(
                    page = tutorialPages[page],
                    isActive = pagerState.currentPage == page
                )
            }

            // Bottom section
            TutorialBottomSection(
                pagerState = pagerState,
                pageCount = tutorialPages.size,
                isLastPage = isLastPage,
                accentColor = tutorialPages[pagerState.currentPage].accentColor,
                onNext = {
                    coroutineScope.launch {
                        if (isLastPage) {
                            onNavigateToHome()
                        } else {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                onSkip = onNavigateToHome
            )
        }
    }
}

@Composable
private fun TutorialTopBar(
    canGoBack: Boolean,
    onNavigateBack: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        AnimatedVisibility(
            visible = canGoBack,
            enter = fadeIn() + slideInHorizontally { -it },
            exit = fadeOut() + slideOutHorizontally { -it }
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Précédent",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Skip button
        TextButton(
            onClick = onSkip,
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Ignorer",
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TutorialPageContent(
    page: TutorialPage,
    isActive: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "page_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 16.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon section
        Card(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = page.accentColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = page.backgroundColor
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Text content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TutorialBottomSection(
    pagerState: PagerState,
    pageCount: Int,
    isLastPage: Boolean,
    accentColor: Color,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Page indicators
        PageIndicators(
            pageCount = pageCount,
            currentPage = pagerState.currentPage,
            activeColor = accentColor,
            inactiveColor = Color.White.copy(alpha = 0.3f)
        )

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isLastPage) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(
                        2.dp,
                        Color.White.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Passer",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Button(
                onClick = onNext,
                modifier = Modifier.weight(if (isLastPage) 1f else 1.5f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = if (isLastPage) "Commencer" else "Suivant",
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (!isLastPage) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIndicators(
    pageCount: Int,
    currentPage: Int,
    activeColor: Color,
    inactiveColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isActive = currentPage == index
            
            val width by animateDpAsState(
                targetValue = if (isActive) 32.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "indicator_width"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(8.dp)
                    .width(width)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isActive) activeColor else inactiveColor)
            )
        }
    }
}