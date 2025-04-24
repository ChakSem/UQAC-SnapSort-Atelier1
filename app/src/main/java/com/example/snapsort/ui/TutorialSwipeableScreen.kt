package com.example.snapsort.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.snapsort.R
import kotlinx.coroutines.launch


data class TutorialPage(
    val title: String,
    val description: String,
    val imageResId: Int
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TutorialSwipeableScreen(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    val tutorialPages = listOf(
        TutorialPage(
            "Bienvenue sur SnapSort !",
            "Découvrez comment organiser facilement vos photos",
            R.drawable.logo_snapsort
        ),
        TutorialPage(
            "Scan WiFi",
            "Connectez-vous à vos appareils photo via WiFi",
            R.drawable.logo_snapsort
        ),
        TutorialPage(
            "Importation",
            "Importez vos photos rapidement et simplement",
            R.drawable.logo_snapsort
        ),
        TutorialPage(
            "Tri Automatique",
            "Laissez l'IA trier vos photos par catégories",
            R.drawable.logo_snapsort
        ),
        TutorialPage(
            "Organisation",
            "Organisez vos photos dans des dossiers personnalisés",
            R.drawable.logo_snapsort
        ),
        TutorialPage(
            "Partage",
            "Partagez facilement vos photos avec vos proches",
            R.drawable.logo_snapsort
        ),
        TutorialPage(
            "Sauvegarde",
            "Vos photos sont automatiquement sauvegardées",
            R.drawable.logo_snapsort
        ),
        TutorialPage(
            "C'est parti !",
            "Commencez à utiliser SnapSort dès maintenant",
            R.drawable.logo_snapsort
        )
    )

    val pagerState = rememberPagerState(pageCount = { tutorialPages.size })

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Barre supérieure
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = { navController.navigate("home") },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Contenu principal
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                TutorialPage(tutorialPages[page])
            }

            // Indicateurs et bouton
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Indicateurs de page
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(tutorialPages.size) { index ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                        )
                    }
                }

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
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage < tutorialPages.size - 1) "Suivant" else "Commencer",
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TutorialPage(page: TutorialPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Image(
            painter = painterResource(id = page.imageResId),
            contentDescription = page.title,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp),
            contentScale = ContentScale.Fit
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = page.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = page.description,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}