package com.example.snapsort.ui

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.snapsort.R
import coil.compose.rememberAsyncImagePainter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagesTransferConfiguration(
    navController: NavController,
) {
    val context = LocalContext.current
    var selectedStartDate by remember { mutableStateOf(getFormattedDate(System.currentTimeMillis())) }
    var selectedEndDate by remember { mutableStateOf(getFormattedDate(System.currentTimeMillis())) }
    var sliderValue by remember { mutableStateOf(0f) }
    var imageUris by remember { mutableStateOf(listOf<Uri>()) }
    var selectedFolder by remember { mutableStateOf("Choisir le sous dossier (d'images)") }

    LaunchedEffect(sliderValue) {
        imageUris = getImagesFromGallery(context, sliderValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_book),
            contentDescription = "Download Icon",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Precisez la periode de transfert",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedButton(
            onClick = {
                // Implement folder selection logic here
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Folder, contentDescription = "Folder")
            Spacer(Modifier.width(8.dp))
            Text(selectedFolder)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(selectedStartDate)
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                modifier = Modifier.weight(1f)
            )
            Text(selectedEndDate)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            imageUris.take(2).forEach { uri ->
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = "Image Preview",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Button(
            onClick = {
                // Implement transfer logic here
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Transferer")
        }

        TextButton(
            onClick = {
                val tutorialUrl = "YOUR_TUTORIAL_URL_HERE"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tutorialUrl))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("Comment ça marche ?", color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun getImagesFromGallery(context: android.content.Context, sliderValue: Float): List<Uri> {
    val imageUris = mutableListOf<Uri>()
    val contentResolver = context.contentResolver
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_TAKEN)
    val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

    val cursor = contentResolver.query(collection, projection, null, null, sortOrder)
    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

        val totalImages = it.count
        val imageIndex = (sliderValue * (totalImages - 1)).toInt()

        if (imageIndex >= 0 && imageIndex < totalImages && it.moveToPosition(imageIndex)) {

            val id = it.getLong(idColumn)
            val contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
            imageUris.add(contentUri)

            if(imageIndex + 1 < totalImages && it.moveToPosition(imageIndex+1)){
                val id2 = it.getLong(idColumn)
                val contentUri2 = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id2.toString())
                imageUris.add(contentUri2)
            }

        }

    }
    return imageUris
}

private fun getFormattedDate(timeInMillis: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(timeInMillis))
}