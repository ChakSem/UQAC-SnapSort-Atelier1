package com.example.snapsort.ui.components

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Modèles de données
data class ImageItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateTaken: Long,
    val size: Long,
    val path: String
)

data class FolderItem(
    val name: String,
    val path: String,
    val imageCount: Int,
    val isSubfolder: Boolean = false,
    val parentFolder: String? = null
)

// États UI
sealed class LoadingState {
    object Idle : LoadingState()
    object LoadingFolders : LoadingState()
    object LoadingImages : LoadingState()
    data class Error(val message: String) : LoadingState()
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun ImprovedImageSelector(
    onImagesSelected: (List<ImageItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // États
    var loadingState by remember { mutableStateOf<LoadingState>(LoadingState.Idle) }
    var folders by remember { mutableStateOf<List<FolderItem>>(emptyList()) }
    var selectedFolder by remember { mutableStateOf<FolderItem?>(null) }
    var allImages by remember { mutableStateOf<List<ImageItem>>(emptyList()) }
    var filteredImages by remember { mutableStateOf<List<ImageItem>>(emptyList()) }
    var selectedImages by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // États pour les filtres
    var showOnlySelected by remember { mutableStateOf(false) }
    var dateRange by remember { mutableStateOf<Pair<Date, Date>?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Permission
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            loadFolders(context, coroutineScope) { result ->
                when (result) {
                    is Result.Success -> {
                        folders = result.data
                        selectedFolder = result.data.firstOrNull()
                        loadingState = LoadingState.Idle
                    }
                    is Result.Error -> {
                        loadingState = LoadingState.Error(result.message)
                    }
                }
            }
        }
    }

    // Charger les dossiers au démarrage
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            loadingState = LoadingState.LoadingFolders
            loadFolders(context, coroutineScope) { result ->
                when (result) {
                    is Result.Success -> {
                        folders = result.data
                        selectedFolder = result.data.firstOrNull()
                        loadingState = LoadingState.Idle
                    }
                    is Result.Error -> {
                        loadingState = LoadingState.Error(result.message)
                    }
                }
            }
        } else {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            permissionLauncher.launch(permission)
        }
    }

    // Charger les images quand le dossier change
    LaunchedEffect(selectedFolder) {
        selectedFolder?.let { folder ->
            loadingState = LoadingState.LoadingImages
            loadImagesFromFolder(context, folder, coroutineScope) { result ->
                when (result) {
                    is Result.Success -> {
                        allImages = result.data
                        filteredImages = result.data
                        selectedImages = emptySet()

                        // Calculer la plage de dates
                        if (result.data.isNotEmpty()) {
                            val minDate = Date(result.data.minByOrNull { it.dateTaken }?.dateTaken ?: 0)
                            val maxDate = Date(result.data.maxByOrNull { it.dateTaken }?.dateTaken ?: 0)
                            dateRange = Pair(minDate, maxDate)
                        }

                        loadingState = LoadingState.Idle
                    }
                    is Result.Error -> {
                        loadingState = LoadingState.Error(result.message)
                    }
                }
            }
        }
    }

    // Filtrer les images
    LaunchedEffect(allImages, showOnlySelected, searchQuery, dateRange) {
        filteredImages = filterImages(allImages, showOnlySelected, selectedImages, searchQuery, dateRange)
    }

    if (!hasPermission) {
        PermissionRequestCard(
            onRequestPermission = {
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                permissionLauncher.launch(permission)
            }
        )
        return
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Sélecteur de dossier
        FolderSelector(
            folders = folders,
            selectedFolder = selectedFolder,
            onFolderSelected = { selectedFolder = it },
            isLoading = loadingState is LoadingState.LoadingFolders
        )

        // Barre d'outils et filtres
        FilterToolbar(
            selectedCount = selectedImages.size,
            totalCount = filteredImages.size,
            showOnlySelected = showOnlySelected,
            onToggleShowSelected = { showOnlySelected = !showOnlySelected },
            searchQuery = searchQuery,
            onSearchQueryChanged = { searchQuery = it },
            onSelectAll = {
                selectedImages = if (selectedImages.size == filteredImages.size) {
                    emptySet()
                } else {
                    filteredImages.map { it.id }.toSet()
                }
            },
            onConfirmSelection = {
                val selectedImagesList = filteredImages.filter { it.id in selectedImages }
                onImagesSelected(selectedImagesList)
            }
        )

        // Contenu principal
        Box(modifier = Modifier.weight(1f)) {
            when (loadingState) {
                is LoadingState.LoadingFolders -> {
                    LoadingIndicator("Chargement des dossiers...")
                }
                is LoadingState.LoadingImages -> {
                    LoadingIndicator("Chargement des images...")
                }
                is LoadingState.Error -> {
                    ErrorCard(
                        message = loadingState.message,
                        onRetry = {
                            selectedFolder?.let { folder ->
                                loadingState = LoadingState.LoadingImages
                                loadImagesFromFolder(context, folder, coroutineScope) { result ->
                                    when (result) {
                                        is Result.Success -> {
                                            allImages = result.data
                                            filteredImages = result.data
                                            loadingState = LoadingState.Idle
                                        }
                                        is Result.Error -> {
                                            loadingState = LoadingState.Error(result.message)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
                else -> {
                    if (filteredImages.isEmpty()) {
                        EmptyStateCard()
                    } else {
                        ImageGrid(
                            images = filteredImages,
                            selectedImages = selectedImages,
                            onImageToggled = { imageId ->
                                selectedImages = if (imageId in selectedImages) {
                                    selectedImages - imageId
                                } else {
                                    selectedImages + imageId
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderSelector(
    folders: List<FolderItem>,
    selectedFolder: FolderItem?,
    onFolderSelected: (FolderItem) -> Unit,
    isLoading: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dossier sélectionné",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selectedFolder?.name ?: "Aucun dossier",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    selectedFolder?.let {
                        Text(
                            text = "${it.imageCount} images",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Changer de dossier"
                        )
                    }
                }
            }

            // Menu déroulant des dossiers
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                folders.forEach { folder ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (folder.isSubfolder)
                                        Icons.Outlined.SubdirectoryArrowRight
                                    else
                                        Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = folder.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${folder.imageCount} images",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onClick = {
                            onFolderSelected(folder)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterToolbar(
    selectedCount: Int,
    totalCount: Int,
    showOnlySelected: Boolean,
    onToggleShowSelected: () -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onSelectAll: () -> Unit,
    onConfirmSelection: () -> Unit
) {
    var showSearchBar by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Première ligne : statistiques et actions principales
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "$selectedCount sélectionnées",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "sur $totalCount images",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                Row {
                    IconButton(onClick = { showSearchBar = !showSearchBar }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Rechercher",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    IconButton(onClick = onSelectAll) {
                        Icon(
                            imageVector = if (selectedCount == totalCount)
                                Icons.Default.SelectAll
                            else
                                Icons.Outlined.SelectAll,
                            contentDescription = "Tout sélectionner",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Barre de recherche (si affichée)
            AnimatedVisibility(
                visible = showSearchBar,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        placeholder = { Text("Rechercher des images...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Effacer")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }

            // Deuxième ligne : filtres et bouton de confirmation
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilterChip(
                    onClick = onToggleShowSelected,
                    label = { Text("Sélectionnées uniquement") },
                    selected = showOnlySelected,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                Button(
                    onClick = onConfirmSelection,
                    enabled = selectedCount > 0,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Confirmer ($selectedCount)")
                }
            }
        }
    }
}

@Composable
private fun ImageGrid(
    images: List<ImageItem>,
    selectedImages: Set<Long>,
    onImageToggled: (Long) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(images) { image ->
            ImageGridItem(
                image = image,
                isSelected = image.id in selectedImages,
                onToggleSelected = { onImageToggled(image.id) }
            )
        }
    }
}

@Composable
private fun ImageGridItem(
    image: ImageItem,
    isSelected: Boolean,
    onToggleSelected: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onToggleSelected() }
            .graphicsLayer(scaleX = animatedScale, scaleY = animatedScale),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        border = if (isSelected) {
            BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Box {
            AsyncImage(
                model = image.uri,
                contentDescription = image.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Overlay de sélection
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Sélectionnée",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(
                                Color.White,
                                CircleShape
                            )
                            .padding(4.dp)
                    )
                }
            }

            // Informations sur l'image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            ) {
                Text(
                    text = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(image.dateTaken)),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Fonctions utilitaires
private fun filterImages(
    images: List<ImageItem>,
    showOnlySelected: Boolean,
    selectedImages: Set<Long>,
    searchQuery: String,
    dateRange: Pair<Date, Date>?
): List<ImageItem> {
    return images.filter { image ->
        // Filtre par sélection
        if (showOnlySelected && image.id !in selectedImages) return@filter false

        // Filtre par recherche
        if (searchQuery.isNotEmpty() && !image.name.contains(searchQuery, ignoreCase = true)) {
            return@filter false
        }

        // Filtre par date (si défini)
        dateRange?.let { (startDate, endDate) ->
            val imageDate = Date(image.dateTaken)
            if (imageDate.before(startDate) || imageDate.after(endDate)) {
                return@filter false
            }
        }

        true
    }
}

// Classes résultat
sealed class Result<T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error<T>(val message: String) : Result<T>()
}

// Fonctions de chargement
private fun loadFolders(
    context: Context,
    coroutineScope: CoroutineScope,
    onResult: (Result<List<FolderItem>>) -> Unit
) {
    coroutineScope.launch(Dispatchers.IO) {
        try {
            val folders = mutableListOf<FolderItem>()

            // Ajouter le dossier "Toutes les images"
            val totalImageCount = getTotalImageCount(context)
            folders.add(
                FolderItem(
                    name = "Toutes les images",
                    path = "ALL",
                    imageCount = totalImageCount
                )
            )

            // Récupérer les dossiers via MediaStore
            val projection = arrayOf(
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.DATA
            )

            val folderCountMap = mutableMapOf<String, Int>()

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

                while (cursor.moveToNext()) {
                    val bucketName = cursor.getString(bucketColumn)
                    val filePath = cursor.getString(dataColumn)

                    if (bucketName != null && filePath != null) {
                        folderCountMap[bucketName] = folderCountMap.getOrDefault(bucketName, 0) + 1
                    }
                }
            }

            // Convertir en FolderItem et trier
            folderCountMap.forEach { (folderName, count) ->
                val isSubfolder = folderName.contains("/") ||
                        folderName.startsWith("DCIM") ||
                        folderName != "Camera" && folderName != "DCIM"

                folders.add(
                    FolderItem(
                        name = folderName,
                        path = folderName,
                        imageCount = count,
                        isSubfolder = isSubfolder
                    )
                )
            }

            withContext(Dispatchers.Main) {
                onResult(Result.Success(folders.sortedBy { it.name }))
            }

        } catch (e: Exception) {
            Log.e("ImageSelector", "Erreur lors du chargement des dossiers", e)
            withContext(Dispatchers.Main) {
                onResult(Result.Error("Erreur lors du chargement des dossiers: ${e.message}"))
            }
        }
    }
}

private fun loadImagesFromFolder(
    context: Context,
    folder: FolderItem,
    coroutineScope: CoroutineScope,
    onResult: (Result<List<ImageItem>>) -> Unit
) {
    coroutineScope.launch(Dispatchers.IO) {
        try {
            val images = mutableListOf<ImageItem>()

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATA
            )

            val selection = if (folder.path == "ALL") {
                null
            } else {
                "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
            }

            val selectionArgs = if (folder.path == "ALL") {
                null
            } else {
                arrayOf(folder.name)
            }

            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Image"
                    val dateTaken = cursor.getLong(dateColumn)
                    val size = cursor.getLong(sizeColumn)
                    val path = cursor.getString(dataColumn) ?: ""

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    images.add(
                        ImageItem(
                            id = id,
                            uri = contentUri,
                            name = name,
                            dateTaken = dateTaken,
                            size = size,
                            path = path
                        )
                    )
                }
            }

            withContext(Dispatchers.Main) {
                onResult(Result.Success(images))
            }

        } catch (e: Exception) {
            Log.e("ImageSelector", "Erreur lors du chargement des images", e)
            withContext(Dispatchers.Main) {
                onResult(Result.Error("Erreur lors du chargement des images: ${e.message}"))
            }
        }
    }
}

private fun getTotalImageCount(context: Context): Int {
    return try {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            null,
            null,
            null
        )?.use { cursor ->
            cursor.count
        } ?: 0
    } catch (e: Exception) {
        0
    }
}

// Composants utilitaires
@Composable
private fun LoadingIndicator(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Réessayer")
            }
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = "Aucune image trouvée",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Essayez de sélectionner un autre dossier",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PermissionRequestCard(
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Accès aux photos requis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Pour sélectionner et transférer vos photos, l'application a besoin d'accéder à votre galerie.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Autoriser l'accès")
            }
        }
    }
}