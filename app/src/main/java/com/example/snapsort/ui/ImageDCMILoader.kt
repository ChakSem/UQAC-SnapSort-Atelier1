package com.example.snapsort.ui

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

@Composable
fun ImageDCMILoader(navController: NavController) {
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

    // Gestion des permissions selon la version Android
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
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
                    Text(
                        "Permission d'accès aux images nécessaire",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(permission)
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Chargement des images...")
                    }
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
                    Text(
                        "Erreur: $errorMessage",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
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
                            Text(
                                "Dossier: ", 
                                modifier = Modifier.padding(end = 8.dp),
                                style = MaterialTheme.typography.labelLarge
                            )

                            Box {
                                OutlinedButton(
                                    onClick = { folderMenuExpanded = true }
                                ) {
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
                            text = "Images trouvées: ${images.size} (limitées à 10 pour l'aperçu)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (images.isEmpty() && !isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Aucune image trouvée dans ce dossier",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Grille d'images
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(4.dp),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(images) { item ->
                                Card(
                                    modifier = Modifier
                                        .aspectRatio(1f),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = item.uri.toUri(),
                                            onError = { 
                                                println("Erreur de chargement de l'image: ${item.uri}")
                                            }
                                        ),
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
                val selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} LIKE 'DCIM%' OR " +
                        "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = 'Camera'"

                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} ASC"
                )?.use { cursor ->
                    val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                    while (cursor.moveToNext()) {
                        val bucketName = cursor.getString(bucketColumn)
                        if (bucketName != null && !foldersList.contains(bucketName)) {
                            foldersList.add(bucketName)
                        }
                    }
                }

                foldersList.distinct()
            }

            onComplete(folders)
        } catch (e: Exception) {
            println("Erreur lors du chargement des dossiers: ${e.message}")
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

private suspend fun loadImagesFromDCIM(
    context: android.content.Context, 
    folderName: String?
): List<ImageItem> {
    return withContext(Dispatchers.IO) 
    {
        // Liste pour stocker les images
        val images = mutableListOf<ImageItem>()

        try {
            // Projections des colonnes nécessaires
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            )

            // Sélection variable selon le dossier choisi
            val selection: String?
            val selectionArgs: Array<String>?

            if (folderName == null) {
                // Dossier DCIM racine - inclure tous les dossiers DCIM
                selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} LIKE ?"
                selectionArgs = arrayOf("DCIM%")
            } else {
                // Sous-dossier spécifique
                selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
                selectionArgs = arrayOf(folderName)
            }

            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

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

                // Limiter manuellement à 10 éléments pour l'aperçu
                var count = 0
                while (it.moveToNext() && count < 10) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn) ?: "image_$id"
                    val dateTaken = if (it.isNull(dateTakenColumn)) {
                        System.currentTimeMillis()
                    } else {
                        it.getLong(dateTakenColumn)
                    }

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
        } catch (e: Exception) {
            println("Erreur lors du chargement des images: ${e.message}")
            e.printStackTrace()
        }

        images
    }
}