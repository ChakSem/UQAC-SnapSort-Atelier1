package com.example.snapsort.ui

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ImageItem(
    val id: Long,
    val uri: String,
    val name: String,
    val dateTaken: Long
)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun ImageDCMILoader(
    navController: NavController,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var images by remember { mutableStateOf<List<ImageItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Liste de sous-dossiers
    var subfolders by remember { mutableStateOf<List<String>>(emptyList()) }
    // Dossier actuellement sélectionné (null = DCIM racine)
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    // État du menu déroulant
    var folderMenuExpanded by remember { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            // Charger d'abord les sous-dossiers
            loadSubfolders(coroutineScope, context) { folders ->
                subfolders = folders
            }
            // Puis charger les images (dossier racine par défaut)
            loadImages(coroutineScope, context, selectedFolder) { loadedImages, error ->
                images = loadedImages
                errorMessage = error
                isLoading = false
            }
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            errorMessage = null

            // Charger les sous-dossiers
            loadSubfolders(coroutineScope, context) { folders ->
                subfolders = folders
            }

            // Charger les images
            loadImages(coroutineScope, context, selectedFolder) { loadedImages, error ->
                images = loadedImages
                errorMessage = error
                isLoading = false
            }
        }
    }

    // Recharger les images quand le dossier sélectionné change
    LaunchedEffect(selectedFolder) {
        if (hasPermission) {
            isLoading = true
            loadImages(coroutineScope, context, selectedFolder) { loadedImages, error ->
                images = loadedImages
                errorMessage = error
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !hasPermission -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Permission d'accès aux images nécessaire")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        }
                    ) {
                        Text("Demander la permission")
                    }
                }
            }
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Erreur: $errorMessage")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            loadImages(coroutineScope, context, selectedFolder) { loadedImages, error ->
                                images = loadedImages
                                errorMessage = error
                                isLoading = false
                            }
                        }
                    ) {
                        Text("Réessayer")
                    }
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Sélecteur de dossier
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Dossier: ", modifier = Modifier.padding(end = 8.dp))

                            Box {
                                Button(onClick = { folderMenuExpanded = true }) {
                                    Text(selectedFolder ?: "DCIM (racine)")
                                }

                                DropdownMenu(
                                    expanded = folderMenuExpanded,
                                    onDismissRequest = { folderMenuExpanded = false }
                                ) {
                                    // Option pour le dossier racine
                                    DropdownMenuItem(
                                        text = { Text("DCIM (racine)") },
                                        onClick = {
                                            selectedFolder = null
                                            folderMenuExpanded = false
                                        }
                                    )

                                    // Options pour les sous-dossiers
                                    subfolders.forEach { folder ->
                                        DropdownMenuItem(
                                            text = { Text(folder) },
                                            onClick = {
                                                selectedFolder = folder
                                                folderMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Images: ${images.size} (limitées à 5)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Grille d'images
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(images) { item ->
                            Card(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .aspectRatio(1f)
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(item.uri.toUri()),
                                    contentDescription = item.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Fonction pour charger les sous-dossiers DCIM
private fun loadSubfolders(
    coroutineScope: CoroutineScope,
    context: android.content.Context,
    onComplete: (List<String>) -> Unit
) {
    coroutineScope.launch {
        try {
            val folders = withContext(Dispatchers.IO) {
                val foldersList = mutableListOf<String>()

                val projection = arrayOf(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} LIKE 'DCIM/%' OR ${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = 'DCIM'"

                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    null
                )?.use { cursor ->
                    val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                    while (cursor.moveToNext()) {
                        val bucketName = cursor.getString(bucketColumn)
                        if (bucketName != "DCIM" && !foldersList.contains(bucketName)) {
                            foldersList.add(bucketName)
                        }
                    }
                }

                foldersList
            }

            onComplete(folders)
        } catch (e: Exception) {
            onComplete(emptyList())
        }
    }
}

// Fonction modifiée pour charger les images d'un dossier spécifique
private fun loadImages(
    coroutineScope: CoroutineScope,
    context: android.content.Context,
    folderName: String?,
    onComplete: (List<ImageItem>, String?) -> Unit
) {
    coroutineScope.launch {
        try {
            val loadedImages = loadImagesFromDCIM(context, folderName)
            onComplete(loadedImages, null)
        } catch (e: Exception) {
            onComplete(emptyList(), "Erreur lors du chargement des images: ${e.message}")
        }
    }
}
suspend fun loadImagesFromDCIM(context: android.content.Context, folderName: String?): List<ImageItem> {
    return withContext(Dispatchers.IO) {
        val images = mutableListOf<ImageItem>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN
        )

        // Sélection variable selon le dossier choisi
        val selection: String
        val selectionArgs: Array<String>

        if (folderName == null) {
            // Dossier DCIM racine
            selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
            selectionArgs = arrayOf("DCIM")
        } else {
            // Sous-dossier spécifique
            selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
            selectionArgs = arrayOf(folderName)
        }

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        // Ne pas ajouter LIMIT dans la requête SQL - cela cause l'erreur

        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateTakenColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

            // Limiter manuellement à 5 éléments
            var count = 0
            while (it.moveToNext() && count < 5) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val dateTaken = it.getLong(dateTakenColumn)

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                images.add(
                    ImageItem(
                        id = id,
                        uri = contentUri.toString(),
                        name = name,
                        dateTaken = dateTaken
                    )
                )

                count++
            }
        }

        images
    }
}