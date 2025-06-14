package com.example.snapsort.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ErrorOutline
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.snapsort.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class ImageInfo(
    val uri: Uri,
    val dateTaken: Long,
    val path: String,
    val name: String,
    val size: Long
)

@Composable
fun ImagesTransferConfiguration(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tag = "ImagesTransfer"

    // État des permissions
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

    // Images trouvées et sélectionnées
    var allImages by remember { mutableStateOf<List<ImageInfo>>(emptyList()) }
    var filteredImages by remember { mutableStateOf<List<ImageInfo>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // État pour les filtres de date
    var dateRange by remember { mutableStateOf(0f..1f) }
    var minDate by remember { mutableStateOf(0L) }
    var maxDate by remember { mutableStateOf(System.currentTimeMillis()) }

    // Formatage pour afficher les dates
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Dossiers disponibles
    var availableFolders by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedFolder by remember { mutableStateOf("ALL") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // État du transfert - AMÉLIORÉ
    var isTransferring by remember { mutableStateOf(false) }
    var transferProgress by remember { mutableStateOf(0f) }
    var transferredCount by remember { mutableStateOf(0) }
    var totalToTransfer by remember { mutableStateOf(0) }
    var isLoadingImages by remember { mutableStateOf(false) }
    var transferError by remember { mutableStateOf<String?>(null) }
    var transferStatus by remember { mutableStateOf("") }

    // Fonction pour filtrer les images en fonction de la plage de dates
    fun filterImagesByDateRange() {
        if (allImages.isNotEmpty() && minDate < maxDate) {
            val range = maxDate - minDate
            val startDate = minDate + (range * dateRange.start).toLong()
            val endDate = minDate + (range * dateRange.endInclusive).toLong()
            
            filteredImages = allImages.filter { it.dateTaken in startDate..endDate }
            Log.d(tag, "Images filtrées: ${filteredImages.size} sur ${allImages.size}")
        } else {
            filteredImages = allImages
        }
    }

    // Fonction pour mettre à jour le range des dates
    fun updateDateRange(images: List<ImageInfo>) {
        if (images.isNotEmpty()) {
            minDate = images.minByOrNull { it.dateTaken }?.dateTaken ?: System.currentTimeMillis()
            maxDate = images.maxByOrNull { it.dateTaken }?.dateTaken ?: System.currentTimeMillis()
            dateRange = 0f..1f
            filterImagesByDateRange()
        } else {
            filteredImages = emptyList()
        }
    }

    // Fonction pour charger les images - AMÉLIORÉE
    fun loadImagesFromFolder(folder: String) {
        errorMessage = null
        isLoadingImages = true
        
        coroutineScope.launch {
            try {
                val loadedImages = withContext(Dispatchers.IO) {
                    getImagesFromFolder(context, folder)
                }
                
                allImages = loadedImages
                updateDateRange(loadedImages)
                isLoadingImages = false
                Log.d(tag, "Images chargées: ${loadedImages.size}")
            } catch (e: Exception) {
                Log.e(tag, "Erreur lors du chargement des images", e)
                errorMessage = "Erreur: ${e.message}"
                isLoadingImages = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            coroutineScope.launch {
                availableFolders = getDCIMFolders(context)
                loadImagesFromFolder(selectedFolder)
            }
        }
    }

    // Charger les dossiers disponibles au démarrage
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            try {
                availableFolders = getDCIMFolders(context)
                loadImagesFromFolder(selectedFolder)
            } catch (e: Exception) {
                errorMessage = "Erreur lors du chargement des dossiers: ${e.message}"
            }
        } else {
            permissionLauncher.launch(permission)
        }
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
            contentDescription = "Transfer Icon",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Sélection des photos à transférer",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        if (!hasPermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Permission d'accès aux images nécessaire",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { permissionLauncher.launch(permission) }
                    ) {
                        Text("Demander la permission")
                    }
                }
            }
        } else {
            // Menu déroulant pour les dossiers
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { dropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Folder, contentDescription = "Folder")
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedFolder == "ALL") "Toutes les images" else selectedFolder)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    availableFolders.forEach { folder ->
                        DropdownMenuItem(
                            text = {
                                Text(if (folder == "ALL") "Toutes les images" else folder)
                            },
                            onClick = {
                                if (selectedFolder != folder) {
                                    selectedFolder = folder
                                    loadImagesFromFolder(folder)
                                }
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Affichage du chargement
            if (isLoadingImages) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Recherche des images...")
                    }
                }
            }

            // Slider pour la sélection de plage de dates
            if (allImages.isNotEmpty() && minDate < maxDate) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Sélectionner une plage de dates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Affichage des dates sélectionnées
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val range = maxDate - minDate
                            val startDate = minDate + (range * dateRange.start).toLong()
                            val endDate = minDate + (range * dateRange.endInclusive).toLong()
                            
                            Text(
                                text = "De: ${dateFormat.format(Date(startDate))}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "À: ${dateFormat.format(Date(endDate))}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        RangeSlider(
                            value = dateRange,
                            onValueChange = { range ->
                                dateRange = range
                                filterImagesByDateRange()
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Nombre d'images sélectionnées avec taille totale
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${filteredImages.size} images sélectionnées",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (allImages.isNotEmpty()) {
                        Text(
                            text = "sur ${allImages.size} disponibles",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Afficher la taille totale estimée
                    if (filteredImages.isNotEmpty()) {
                        val totalSizeMB = filteredImages.sumOf { it.size } / (1024 * 1024)
                        Text(
                            text = "Taille estimée: ${totalSizeMB} MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Message d'erreur
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Aperçu des images
            if (filteredImages.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Première image (plus récente)
                    ImagePreview(
                        uri = filteredImages.first().uri,
                        label = "Plus récente"
                    )

                    // Dernière image (plus ancienne) si différente
                    if (filteredImages.size > 1) {
                        ImagePreview(
                            uri = filteredImages.last().uri,
                            label = "Plus ancienne"
                        )
                    }
                }
            }

            // Indicateur de progression et status du transfert - AMÉLIORÉ
            if (isTransferring) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = transferStatus,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LinearProgressIndicator(
                            progress = transferProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "$transferredCount / $totalToTransfer images",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Erreur de transfert
            if (transferError != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Erreur de transfert",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = transferError ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { transferError = null }
                        ) {
                            Text("OK")
                        }
                    }
                }
            }

            // État pour l'alerte de connexion
            var showHotspotAlert by remember { mutableStateOf(false) }

            // Alerte de connexion au hotspot
            if (showHotspotAlert) {
                AlertDialog(
                    onDismissRequest = { showHotspotAlert = false },
                    title = { Text("Connexion requise") },
                    text = { 
                        Text("Vous n'êtes pas connecté à un hotspot. Veuillez activer le hotspot ou consulter le tutoriel.")
                    },
                    confirmButton = {
                        TextButton(onClick = { showHotspotAlert = false }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showHotspotAlert = false
                            navController.navigate("TutorialSwipeableScreen")
                        }) {
                            Text("Voir le tutoriel")
                        }
                    }
                )
            }

            // Bouton de transfert - AMÉLIORÉ
            Button(
                onClick = {
                    if (filteredImages.isNotEmpty()) {
                        if (!isConnectedToHotspot(context)) {
                            showHotspotAlert = true
                        } else {
                            // Réinitialiser les états d'erreur
                            transferError = null
                            
                            coroutineScope.launch {
                                try {
                                    isTransferring = true
                                    totalToTransfer = filteredImages.size
                                    transferredCount = 0
                                    transferProgress = 0f
                                    transferStatus = "Initialisation du transfert..."

                                    val success = transferImagesOptimized(
                                        context = context,
                                        images = filteredImages,
                                        onProgressUpdate = { progress, count, status ->
                                            transferProgress = progress
                                            transferredCount = count
                                            transferStatus = status
                                        }
                                    )

                                    // Notification de fin
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            if (success) "Transfert réussi! $transferredCount images transférées" 
                                            else "Transfert partiellement réussi: $transferredCount/$totalToTransfer images",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    
                                } catch (e: Exception) {
                                    Log.e(tag, "Erreur lors du transfert", e)
                                    transferError = e.message ?: "Erreur inconnue"
                                } finally {
                                    isTransferring = false
                                    transferStatus = ""
                                }
                            }
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Aucune image à transférer",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = filteredImages.isNotEmpty() && !isTransferring
            ) {
                if (isTransferring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    if (isTransferring) "Transfert en cours..." 
                    else "Transférer ${filteredImages.size} photo(s)"
                )
            }
        }
    }
}

@Composable
fun ImagePreview(uri: Uri, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            label, 
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

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

// Fonctions utilitaires améliorées
private fun getImagesFromFolder(context: Context, folderPath: String): List<ImageInfo> {
    val imageInfoList = mutableListOf<ImageInfo>()
    val contentResolver = context.contentResolver

    try {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE
        )

        val selection = if (folderPath == "ALL") {
            null
        } else {
            "${MediaStore.Images.Media.DATA} LIKE ?"
        }

        val selectionArgs = if (folderPath == "ALL") {
            null
        } else {
            arrayOf("%$folderPath%")
        }

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        cursor?.use { c ->
            val idColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dataColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val nameColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            while (c.moveToNext()) {
                val id = c.getLong(idColumn)
                val dateTaken = if (c.isNull(dateColumn)) {
                    System.currentTimeMillis()
                } else {
                    c.getLong(dateColumn)
                }
                val path = c.getString(dataColumn) ?: ""
                val name = c.getString(nameColumn) ?: "image_$id"
                val size = if (c.isNull(sizeColumn)) 0L else c.getLong(sizeColumn)

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                imageInfoList.add(ImageInfo(contentUri, dateTaken, path, name, size))
            }
        }
    } catch (e: Exception) {
        Log.e("ImagesTransfer", "Erreur lors de la récupération des images", e)
    }

    return imageInfoList
}

private fun getDCIMFolders(context: Context): List<String> {
    val folders = mutableListOf("ALL", "DCIM", "Camera")
    
    try {
        val projection = arrayOf(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} ASC"
        )

        cursor?.use {
            val bucketColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            while (it.moveToNext()) {
                val bucketName = it.getString(bucketColumn)
                if (bucketName != null && !folders.contains(bucketName)) {
                    folders.add(bucketName)
                }
            }
        }
    } catch (e: Exception) {
        Log.e("ImagesTransfer", "Erreur lors de la récupération des dossiers", e)
    }

    return folders.distinct()
}

// NOUVELLE FONCTION DE TRANSFERT OPTIMISÉE POUR ÉVITER LES CRASHES
private suspend fun transferImagesOptimized(
    context: Context,
    images: List<ImageInfo>,
    onProgressUpdate: (Float, Int, String) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    Log.d("ImagesTransfer", "Début du transfert de ${images.size} images")

    try {
        // 1. Trouver le serveur
        withContext(Dispatchers.Main) {
            onProgressUpdate(0f, 0, "Recherche du serveur...")
        }
        
        val serverIp = findServerOptimized(context) 
            ?: throw Exception("Serveur non trouvé sur le réseau")
        
        Log.d("ImagesTransfer", "Serveur trouvé: $serverIp")

        // 2. Transférer par petits lots pour éviter les problèmes de mémoire
        val batchSize = 15 // Lots de 5 images
        val batches = images.chunked(batchSize)
        var totalTransferred = 0

        for ((batchIndex, batch) in batches.withIndex()) {
            try {
                withContext(Dispatchers.Main) {
                    onProgressUpdate(
                        totalTransferred.toFloat() / images.size,
                        totalTransferred,
                        "Transfert du lot ${batchIndex + 1}/${batches.size}..."
                    )
                }

                // Transfert d'un lot
                val batchSuccess = transferBatch(context, serverIp, batch) { transferredInBatch ->
                    val currentTotal = totalTransferred + transferredInBatch
                    val progress = currentTotal.toFloat() / images.size
                    
                    // Mise à jour UI sur le thread principal
                    kotlinx.coroutines.runBlocking(Dispatchers.Main) {
                        onProgressUpdate(progress, currentTotal, "Image ${currentTotal}/${images.size}")
                    }
                }

                if (batchSuccess) {
                    totalTransferred += batch.size
                } else {
                    Log.w("ImagesTransfer", "Échec du lot ${batchIndex + 1}")
                    // Continuer avec le lot suivant
                }

                // Petite pause entre les lots
                delay(100) // 100 ms pour éviter la saturation du réseau

            } catch (e: Exception) {
                Log.e("ImagesTransfer", "Erreur lors du transfert du lot ${batchIndex + 1}", e)
                // Continuer avec le lot suivant
            }
        }

        // 3. Résultat final
        withContext(Dispatchers.Main) {
            onProgressUpdate(1f, totalTransferred, "Transfert terminé")
        }
        
        Log.d("ImagesTransfer", "Transfert terminé: $totalTransferred/${images.size} images")
        return@withContext totalTransferred == images.size

    } catch (e: Exception) {
        Log.e("ImagesTransfer", "Erreur globale lors du transfert", e)
        withContext(Dispatchers.Main) {
            onProgressUpdate(0f, 0, "Erreur: ${e.message}")
        }
        throw e
    }
}

// Fonction pour transférer un lot d'images
private suspend fun transferBatch(
    context: Context,
    serverIp: String,
    images: List<ImageInfo>,
    onBatchProgress: (Int) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    var outputStream: BufferedOutputStream? = null
    var writer: BufferedWriter? = null

    try {
        val url = URL("http://$serverIp:8080")
        connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10000
            readTimeout = 60000   // 1 minute par lot 
            setRequestProperty("Content-Type", "application/octet-stream")
            setChunkedStreamingMode(8192) // Streaming par chunks
        }
        
        connection.connect()
        
        outputStream = BufferedOutputStream(connection.outputStream, 8192)
        writer = BufferedWriter(OutputStreamWriter(outputStream), 8192)

        // Envoyer le nombre d'images dans ce lot
        writer.write(images.size.toString())
        writer.write("\n")
        writer.flush()

        // Transférer chaque image du lot
        images.forEachIndexed { index, imageInfo ->
            try {
                transferSingleImageOptimized(context, imageInfo, outputStream, writer)
                onBatchProgress(index + 1)
                
                // Petit délai pour éviter la saturation
                if (index < images.size - 1) {
                    delay(50)
                }
            } catch (e: Exception) {
                Log.e("ImagesTransfer", "Erreur lors du transfert de l'image ${imageInfo.name}", e)
                // Continuer avec l'image suivante
                throw e // Arrêter ce lot en cas d'erreur
            }
        }

        writer.flush()
        outputStream.flush()

        // Vérifier la réponse
        val responseCode = connection.responseCode
        Log.d("ImagesTransfer", "Code de réponse: $responseCode")
        
        return@withContext responseCode == HttpURLConnection.HTTP_OK

    } catch (e: Exception) {
        Log.e("ImagesTransfer", "Erreur lors du transfert du lot", e)
        return@withContext false
    } finally {
        // Nettoyage des ressources
        try {
            writer?.close()
            outputStream?.close()
            connection?.disconnect()
        } catch (e: Exception) {
            Log.e("ImagesTransfer", "Erreur lors de la fermeture des ressources", e)
        }
    }
}

// Fonction optimisée pour transférer une image unique
private fun transferSingleImageOptimized(
    context: Context,
    imageInfo: ImageInfo,
    outputStream: BufferedOutputStream,
    writer: BufferedWriter
) {
    context.contentResolver.openInputStream(imageInfo.uri)?.use { inputStream ->
        try {
            // Nettoyer le nom de fichier
            val fileName = imageInfo.name
                .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                .replace("__+".toRegex(), "_")

            // Envoyer les métadonnées
            writer.write(fileName)
            writer.write("\n")

            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val dateString = dateFormat.format(Date(imageInfo.dateTaken))
            writer.write(dateString)
            writer.write("\n")

            // Calculer la taille réelle du stream
            val fileSize = inputStream.available()
            writer.write(fileSize.toString())
            writer.write("\n")
            writer.flush()

            // Transférer le contenu par chunks pour éviter les problèmes de mémoire
            val buffer = ByteArray(4096) // Buffer plus petit
            var bytesRead: Int
            var totalRead = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                
                // Flush périodique pour éviter l'accumulation en mémoire
                if (totalRead % 32768 == 0) { // Tous les 32KB
                    outputStream.flush()
                }
            }
            
            outputStream.flush()
            Log.d("ImagesTransfer", "Image transférée: $fileName ($totalRead octets)")
            
        } catch (e: Exception) {
            Log.e("ImagesTransfer", "Erreur lors du transfert de l'image ${imageInfo.name}", e)
            throw e
        }
    } ?: throw Exception("Impossible d'ouvrir l'image ${imageInfo.name}")
}

// Fonction optimisée de recherche de serveur
private suspend fun findServerOptimized(context: Context): String? = withContext(Dispatchers.IO) {
    try {
        // D'abord essayer localhost
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress("127.0.0.1", 8080), 1000)
            socket.close()
            return@withContext "127.0.0.1"
        } catch (e: Exception) {
            // Localhost ne fonctionne pas, continuer
        }

        // Obtenir l'IP locale
        val localIp = getWifiIpAddress(context) ?: return@withContext null
        val networkPrefix = localIp.substringBeforeLast(".")
        
        // Essayer les adresses les plus communes
        val commonAddresses = listOf("1", "100", "101", "254", "192")
        
        for (lastOctet in commonAddresses) {
            val serverIp = "$networkPrefix.$lastOctet"
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(serverIp, 8080), 500)
                socket.close()
                return@withContext serverIp
            } catch (e: Exception) {
                // Continuer avec l'adresse suivante
            }
        }
        
        null
    } catch (e: Exception) {
        Log.e("ImagesTransfer", "Erreur lors de la recherche du serveur", e)
        null
    }
}

private fun isConnectedToHotspot(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

private fun getWifiIpAddress(context: Context): String? {
    try {
        val networkInterfaces = NetworkInterface.getNetworkInterfaces()
        while (networkInterfaces.hasMoreElements()) {
            val networkInterface = networkInterfaces.nextElement()
            val addresses = networkInterface.inetAddresses
            
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (!address.isLoopbackAddress && address is Inet4Address) {
                    return address.hostAddress
                }
            }
        }
    } catch (e: Exception) {
        Log.e("ImagesTransfer", "Erreur lors de la récupération de l'IP", e)
    }
    return null
}