package com.example.snapsort.ui.components.image

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.snapsort.domain.model.Image

@Composable
fun ImageGrid(
    images: List<Image>,
    modifier: Modifier = Modifier,
    columns: Int = 3,
    onImageClick: ((Image) -> Unit)? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(images) { image ->
            ImageGridItem(
                image = image,
                onClick = onImageClick
            )
        }
    }
}

@Composable
private fun ImageGridItem(
    image: Image,
    onClick: ((Image) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .let { baseModifier -> // ✅ CORRECTION: Utiliser let pour chaîner les modifiers
                if (onClick != null) {
                    baseModifier.clickable { onClick(image) } // ✅ CORRECTION: Import clickable ajouté
                } else {
                    baseModifier
                }
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        AsyncImage(
            model = image.path,
            contentDescription = image.name,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    }
}