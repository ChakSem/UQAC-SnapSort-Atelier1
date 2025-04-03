    package com.example.snapsort.ui
    
    import android.Manifest
    import android.content.ContentUris
    import android.content.Context
    import android.content.pm.PackageManager
    import android.net.Uri
    import android.net.wifi.WifiManager
    import android.os.Build
    import android.os.Environment
    import android.provider.MediaStore
    import android.util.Log
    import kotlinx.coroutines.isActive
    import android.widget.Toast
    import androidx.activity.compose.rememberLauncherForActivityResult
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.annotation.RequiresApi
    import androidx.compose.foundation.Image
    import androidx.compose.material3.RangeSlider
    import androidx.compose.material3.SliderDefaults
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
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.layout.ContentScale
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.res.painterResource
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import androidx.core.content.ContextCompat
    import androidx.navigation.NavController
    import coil.compose.AsyncImagePainter
    import coil.compose.rememberAsyncImagePainter
    import coil.request.ImageRequest
    import com.example.snapsort.R
    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.Job
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext
    import java.io.File
    import java.io.OutputStream
    import java.net.InetSocketAddress
    import java.net.Socket
    import java.net.SocketTimeoutException
    import java.text.SimpleDateFormat
    import java.util.*
    import java.io.BufferedWriter
    import java.io.OutputStreamWriter
    import java.net.HttpURLConnection
    import java.net.Inet4Address
    import java.net.NetworkInterface
    import java.net.URL
    
    
    data class DateRange(
        val startDate: Long,
        val endDate: Long
    )
    
    data class ImageInfo(
        val uri: Uri,
        val dateTaken: Long,
        val path: String
    )
    
    @RequiresApi(Build.VERSION_CODES.R)
    @Composable
    fun ImagesTransferConfiguration(
        navController: NavController,
    ) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val tag = "ImagesTransfer"
    
        // État des permissions
        var hasPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED ||
                        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                ) == PackageManager.PERMISSION_GRANTED)
            )
        }
    
        // Images trouvées et sélectionnées
        var allImages by remember { mutableStateOf<List<ImageInfo>>(emptyList()) }
        var filteredImages by remember { mutableStateOf<List<ImageInfo>>(emptyList()) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
    
        // État pour le Range Slider de dates
        var dateRange by remember { mutableStateOf(0f..1f) }
        var minDate by remember { mutableStateOf(0L) }
        var maxDate by remember { mutableStateOf(System.currentTimeMillis()) }
    
        // Formatage pour afficher les dates
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
        // Dossiers disponibles
        var availableFolders by remember { mutableStateOf<List<String>>(emptyList()) }
        var selectedFolder by remember { mutableStateOf<String>("ALL") }
        var dropdownExpanded by remember { mutableStateOf(false) }
    
        // État du transfert
        var isTransferring by remember { mutableStateOf(false) }
        var transferProgress by remember { mutableStateOf(0f) }
        var transferredCount by remember { mutableStateOf(0) }
        var totalToTransfer by remember { mutableStateOf(0) }
    
        // État pour afficher le scanning des images
        var isLoadingImages by remember { mutableStateOf(false) }
    
        // Job pour pouvoir annuler les coroutines précédentes
        var currentLoadingJob by remember { mutableStateOf<Job?>(null) }
    
        // Fonction pour filtrer les images en fonction de la plage de dates
        fun filterImagesByDateRange(
            images: List<ImageInfo>,
            start: Float,
            end: Float,
            min: Long,
            max: Long
        ) {
            if (images.isNotEmpty() && min < max) {
                // Calculer les dates réelles à partir des valeurs normalisées du slider
                // Inverser le calcul pour que 0 corresponde à la date la plus récente (max)
                // et 1 corresponde à la date la plus ancienne (min)
                val startDate = max - ((max - min) * start).toLong()
                val endDate = max - ((max - min) * end).toLong()

                // Filtrer les images dont la date est dans la plage
                // Notez que comme nous avons inversé l'échelle, nous filtrons différemment
                filteredImages = images.filter { it.dateTaken in endDate..startDate }
                Log.d(
                    tag,
                    "Images filtrées: ${filteredImages.size} sur ${images.size} (${
                        dateFormat.format(Date(startDate))
                    } à ${dateFormat.format(Date(endDate))})"
                )
            } else {
                filteredImages = images
            }
        }
    
        // Fonction pour mettre à jour le range des dates en fonction des images chargées
        fun updateDateRange(images: List<ImageInfo>) {
            if (images.isNotEmpty()) {
                // Récupérer les dates min et max des images
                minDate = images.minByOrNull { it.dateTaken }?.dateTaken ?: System.currentTimeMillis()
                maxDate = images.maxByOrNull { it.dateTaken }?.dateTaken ?: System.currentTimeMillis()
    
                // Réinitialiser le slider
                dateRange = 0f..1f
    
                // Appliquer le filtre initial
                filterImagesByDateRange(images, 0f, 1f, minDate, maxDate)
            } else {
                // Si pas d'images, reset
                filteredImages = emptyList()
            }
        }
    
        // Fonction pour charger les images depuis un dossier spécifique
        fun loadImagesFromFolder(folder: String) {
            // Annuler tout job précédent
            currentLoadingJob?.cancel()
    
            // Réinitialiser les états
            allImages = emptyList()
            filteredImages = emptyList()
            errorMessage = null
            isLoadingImages = true
    
            // Créer un nouveau job pour le chargement des images
            currentLoadingJob = coroutineScope.launch {
                try {
                    Log.d(tag, "Début du chargement des images pour le dossier: $folder")
    
                    val loadedImages = withContext(Dispatchers.IO) {
                        getImagesFromFolder(context, folder)
                    }
    
                    // Vérifier si le job a été annulé pendant le chargement
                    if (!isActive) {
                        Log.d(tag, "Chargement des images annulé")
                        return@launch
                    }
    
                    Log.d(tag, "Images chargées: ${loadedImages.size}")
    
                    allImages = loadedImages
                    updateDateRange(loadedImages)
                    isLoadingImages = false
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e(tag, "Erreur lors du chargement des images", e)
                        errorMessage = "Erreur: ${e.message}"
                        isLoadingImages = false
                    }
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
                    if (availableFolders.isNotEmpty()) {
                        loadImagesFromFolder(selectedFolder)
                    }
                }
            }
        }
    
        // Charger les dossiers disponibles au démarrage si l'autorisation est déjà accordée
        LaunchedEffect(hasPermission) {
            if (hasPermission) {
                try {
                    availableFolders = getDCIMFolders(context)
                    if (availableFolders.isNotEmpty()) {
                        selectedFolder = "ALL"
                        loadImagesFromFolder(selectedFolder)
                    } else {
                        errorMessage = "Aucun dossier d'images trouvé"
                    }
                } catch (e: Exception) {
                    errorMessage = "Erreur lors du chargement des dossiers: ${e.message}"
                    Log.e(tag, "Erreur lors du chargement des dossiers", e)
                }
            } else {
                // Demander l'autorisation si nécessaire
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    
        // Nettoyer les ressources quand le composable est détruit
        DisposableEffect(Unit) {
            onDispose {
                // Annuler les jobs en cours quand le composant est détruit
                currentLoadingJob?.cancel()
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
    
            // Afficher un indicateur de chargement pendant le scan des images
            if (isLoadingImages) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Recherche des images dans ${if (selectedFolder == "ALL") "tous les dossiers" else selectedFolder}...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Ajout du double slider pour la sélection de plage de dates
            if (allImages.isNotEmpty() && minDate < maxDate) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Sélectionner une plage de dates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Affichage des dates sélectionnées
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Date de début (plus récente)
                            val startDate = maxDate - ((maxDate - minDate) * dateRange.start).toLong()
                            Text(
                                text = "Début: ${dateFormat.format(Date(startDate))}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            // Date de fin (plus ancienne)
                            val endDate = maxDate - ((maxDate - minDate) * dateRange.endInclusive).toLong()
                            Text(
                                text = "Fin: ${dateFormat.format(Date(endDate))}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Le slider double pour sélectionner la plage
                        RangeSlider(
                            value = dateRange,
                            onValueChange = { range ->
                                dateRange = range
                                filterImagesByDateRange(
                                    allImages,
                                    range.start,
                                    range.endInclusive,
                                    minDate,
                                    maxDate
                                )
                            },
                            valueRange = 0f..1f,
                            steps = 100,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
    
            // Nombre d'images chargées et sélectionnées
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${filteredImages.size} images sélectionnées sur ${allImages.size} disponibles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
    
            // Message d'erreur si nécessaire
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0))
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
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color.Red
                        )
                    }
                }
            }
    
            // Aperçu des images (première et dernière de la sélection)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (filteredImages.isNotEmpty()) {
                    // Première image (la plus récente)
                    ImagePreview(
                        uri = filteredImages.first().uri,
                        date = filteredImages.first().dateTaken,
                        label = "Plus récente"
                    )
    
                    // Dernière image (la plus ancienne)
                    if (filteredImages.size > 1) {
                        ImagePreview(
                            uri = filteredImages.last().uri,
                            date = filteredImages.last().dateTaken,
                            label = "Plus ancienne"
                        )
                    }
                }
            }
    
            // Indicateur de progression si transfert en cours
            if (isTransferring) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = transferProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Transfert en cours: $transferredCount/$totalToTransfer",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
    
            // Bouton de transfert
            Button(
                onClick = {
                    if (filteredImages.isNotEmpty()) {
                        coroutineScope.launch {
                            isTransferring = true
                            totalToTransfer = filteredImages.size
                            transferredCount = 0
    
                            try {
                                val success = transferImages(
                                    context,
                                    filteredImages,
                                    onProgressUpdate = { progress, count ->
                                        transferProgress = progress
                                        transferredCount = count
                                    }
                                )
    
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        if (success) "Transfert réussi!" else "Échec du transfert",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Log.e(tag, "Erreur lors du transfert", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Erreur: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } finally {
                                isTransferring = false
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
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isTransferring) "Transfert en cours..." else "Transférer ${filteredImages.size} photo(s)")
            }
    
            TextButton(
                onClick = {
                    navController.navigate("TutorialSwipeableScreen") {
                        popUpTo("TutorialSwipeableScreen") { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Comment ça marche ?", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
    
    // Fonction optimisée pour récupérer les images d'un dossier spécifique
    private fun getImagesFromFolder(context: Context, folderPath: String): List<ImageInfo> {
        val imageInfoList = mutableListOf<ImageInfo>()
        val contentResolver = context.contentResolver
        val tag = "ImagesTransfer"
    
        Log.d(tag, "Recherche d'images dans le dossier: $folderPath")
    
        try {
            // Projection pour récupérer les données nécessaires
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATA
            )
    
            // Construire la requête en fonction du dossier sélectionné
            val selection = if (folderPath == "ALL") {
                null
            } else {
                // Utiliser LIKE pour le chemin du dossier
                "${MediaStore.Images.Media.DATA} LIKE ?"
            }
    
            val selectionArgs = if (folderPath == "ALL") {
                null
            } else {
                val searchPath = if (folderPath.startsWith("DCIM/")) {
                    // Chemin pour les sous-dossiers DCIM
                    "%/${folderPath}/%"
                } else if (folderPath == "DCIM") {
                    // Chemin pour le dossier DCIM principal
                    "%/DCIM/%"
                } else {
                    // Autre chemin
                    "%$folderPath%"
                }
                arrayOf(searchPath)
            }
    
            // Trier par date de prise de vue (du plus récent au plus ancien)
            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
    
            val cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
    
            cursor?.use { c ->
                Log.d(tag, "Nombre d'images trouvées: ${c.count}")
    
                val idColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val dataColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
    
                while (c.moveToNext()) {
                    val id = c.getLong(idColumn)
                    val dateTaken = if (c.isNull(dateColumn)) {
                        System.currentTimeMillis()
                    } else {
                        c.getLong(dateColumn)
                    }
                    val path = c.getString(dataColumn)
    
                    // Création de l'URI du contenu
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
    
                    // Vérifier si l'URI est valide
                    try {
                        contentResolver.openInputStream(contentUri)?.close()
                        imageInfoList.add(ImageInfo(contentUri, dateTaken, path))
                        Log.d(tag, "Image trouvée: Path=$path")
                    } catch (e: Exception) {
                        Log.e(tag, "URI invalide: $contentUri - ${e.message}")
                    }
                }
            }
    
            // Si la liste est vide, essayer la méthode de secours
            if (imageInfoList.isEmpty() && folderPath != "ALL") {
                Log.d(tag, "Tentative de récupération directe des fichiers dans $folderPath")
                val dirPath = when {
                    folderPath.startsWith("DCIM/") -> {
                        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                            folderPath.substring(5))
                    }
                    folderPath == "DCIM" -> {
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    }
                    else -> {
                        File(folderPath)
                    }
                }
    
                // Vérifier si le dossier existe
                if (dirPath.exists() && dirPath.isDirectory) {
                    val files = dirPath.listFiles { file ->
                        file.isFile && (file.name.endsWith(".jpg", ignoreCase = true) ||
                                file.name.endsWith(".jpeg", ignoreCase = true) ||
                                file.name.endsWith(".png", ignoreCase = true))
                    }
    
                    files?.forEach { file ->
                        val uri = Uri.fromFile(file)
                        val dateTaken = file.lastModified()
    
                        try {
                            imageInfoList.add(ImageInfo(uri, dateTaken, file.absolutePath))
                            Log.d(tag, "Image trouvée directement: ${file.absolutePath}")
                        } catch (e: Exception) {
                            Log.e(tag, "Erreur avec l'image: ${file.absolutePath} - ${e.message}")
                        }
                    }
                }
            }
    
        } catch (e: Exception) {
            Log.e(tag, "Erreur lors de la récupération des images", e)
        }
    
        Log.d(tag, "Total d'images trouvées: ${imageInfoList.size}")
        return imageInfoList
    }
    
    // Fonction pour charger les images depuis un dossier spécifique
    private fun loadImagesFromFolder(
        context: Context,
        folder: String,
        coroutineScope: CoroutineScope,
        callback: (List<ImageInfo>, String?) -> Unit
    ) {
        coroutineScope.launch {
            try {
                Log.d("ImagesTransfer", "Chargement des images du dossier: $folder")
    
                // Utiliser withContext pour effectuer la requête en arrière-plan
                val loadedImages = withContext(Dispatchers.IO) {
                    getImagesFromFolder(context, folder)
                }
    
                Log.d("ImagesTransfer", "Nombre d'images trouvées: ${loadedImages.size}")
                callback(loadedImages, null)
            } catch (e: Exception) {
                val errorMsg = "Erreur lors du chargement des images: ${e.message}"
                Log.e("ImagesTransfer", errorMsg, e)
                callback(emptyList(), errorMsg)
            }
        }
    }
    
    // Fonction pour obtenir les dossiers disponibles - Simplifiée pour se concentrer sur DCIM
    private fun getDCIMFolders(context: Context): List<String> {
        val folders = mutableListOf<String>()
        val tag = "ImagesTransfer"
    
        // Ajouter une option "Toutes les images"
        folders.add("ALL")
    
        // Ajouter directement DCIM et ses sous-dossiers
        try {
            val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            if (dcimDir.exists() && dcimDir.isDirectory) {
                if (!folders.contains("DCIM")) folders.add("DCIM")
    
                dcimDir.listFiles()?.forEach { file ->
                    if (file.isDirectory && !folders.contains("DCIM/${file.name}")) {
                        folders.add("DCIM/${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Erreur lors de la récupération des dossiers DCIM", e)
        }
    
        return folders.distinct()
    }
    
    
    
    
    @Composable
    fun ImagePreview(uri: Uri, date: Long, label: String) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
    
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = "Image Preview",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
    
            Text(
                getFormattedDate(date),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
    @RequiresApi(Build.VERSION_CODES.R)
    fun checkPermissions(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Environment.isExternalStorageManager() ||
                    context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
        return true
    }
    
    private suspend fun transferImages(
        context: Context,
        images: List<ImageInfo>,
        onProgressUpdate: (Float, Int) -> Unit
    ): Boolean {
        return try {
            // Obtenir l'adresse IP du réseau WiFi
            val wifiIp = getWifiIpAddress(context) ?: throw Exception("Impossible d'obtenir l'adresse IP")
            Log.d("ImagesTransfer", "Adresse IP locale: $wifiIp")
    
            // Extraire le préfixe du réseau
            val networkPrefix = wifiIp.substringBeforeLast(".")
    
            // Port du serveur
            val serverPort = 8080
    
            // Chercher le serveur sur le réseau
            val serverIp = findServer(networkPrefix, serverPort) ?: throw Exception("Serveur non trouvé sur le réseau")
            Log.d("ImagesTransfer", "Serveur trouvé à l'adresse: $serverIp")
    
            withContext(Dispatchers.IO) {
                // Créer une connexion HTTP
                val url = URL("http://$serverIp:$serverPort")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 10000 // 10 seconds
                connection.readTimeout = 60000   // 60 seconds
                connection.setRequestProperty("Content-Type", "application/octet-stream")
                connection.connect()
    
                val outputStream = connection.outputStream
                val writer = BufferedWriter(OutputStreamWriter(outputStream))
    
                try {
                    // Envoyer le nombre total d'images comme première ligne
                    writer.write(images.size.toString())
                    writer.write("\n")
                    writer.flush()
    
                    // Transférer chaque image
                    images.forEachIndexed { index, imageInfo ->
                        try {
                            transferSingleImage(context, imageInfo, outputStream, writer)
                            
                            // Mettre à jour la progression
                            val progress = (index + 1).toFloat() / images.size
                            withContext(Dispatchers.Main) {
                                onProgressUpdate(progress, index + 1)
                            }
                        } catch (e: Exception) {
                            Log.e("ImagesTransfer", "Erreur lors du transfert de l'image ${index + 1}", e)
                            // Continuer avec l'image suivante plutôt que d'arrêter tout le processus
                        }
                    }
    
                    // Lire la réponse du serveur
                    val responseCode = connection.responseCode
                    val responseMessage = connection.responseMessage
                    Log.d("ImagesTransfer", "Réponse du serveur: $responseCode $responseMessage")
    
                    responseCode == HttpURLConnection.HTTP_OK
                } finally {
                    try {
                        outputStream.close()
                        writer.close()
                        connection.disconnect()
                    } catch (e: Exception) {
                        Log.e("ImagesTransfer", "Erreur lors de la fermeture des ressources", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ImagesTransfer", "Erreur lors du transfert", e)
            throw e
        }
    }
    private fun transferSingleImage(
        context: Context,
        imageInfo: ImageInfo,
        outputStream: OutputStream,
        writer: BufferedWriter
    ) {
        context.contentResolver.openInputStream(imageInfo.uri)?.use { inputStream ->
            try {
                // Obtenir un nom de fichier significatif
                val file = File(imageInfo.path)
                val fileName = file.name
    
                // Envoyer le nom du fichier sur une ligne
                writer.write(fileName)
                writer.write("\n")
    
                // Formatter la date au format attendu par le serveur (YYYYMMDD_HHmmss)
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val dateString = dateFormat.format(Date(imageInfo.dateTaken))
                writer.write(dateString)
                writer.write("\n")
    
                // Récupérer et envoyer la taille du fichier
                val fileSize = inputStream.available()
                writer.write(fileSize.toString())
                writer.write("\n")
                writer.flush()
    
                // Transférer le contenu du fichier
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var totalRead = 0
    
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    outputStream.flush() // Flush plus fréquent pour éviter les blocages
                }
    
                Log.d("ImagesTransfer", "Image transférée: $fileName ($totalRead octets)")
            } catch (e: Exception) {
                Log.e("ImagesTransfer", "Erreur lors du transfert de l'image ${imageInfo.path}", e)
                throw e
            }
        } ?: throw Exception("Impossible d'ouvrir l'image ${imageInfo.path}")
    }
    private fun getWifiIpAddress(context: Context): String? {
        try {
            // D'abord, essayer via le WifiManager
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress
            
            // Convertir l'entier en adresse IP
            if (ipAddress != 0) {
                return String.format(
                    "%d.%d.%d.%d",
                    ipAddress and 0xff,
                    ipAddress shr 8 and 0xff,
                    ipAddress shr 16 and 0xff,
                    ipAddress shr 24 and 0xff
                )
            }
            
            // Si WifiManager échoue, essayer via les interfaces réseau (fonctionne en hotspot aussi)
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
            Log.e("ImagesTransfer", "Erreur lors de la récupération de l'adresse IP", e)
        }
        
        return null
    }
    private suspend fun findServer(networkPrefix: String, port: Int): String? = withContext(Dispatchers.IO) {
        // Adresses les plus probables à essayer en premier
        val priorityAddresses = listOf(
            "1",    // Souvent utilisé par les routeurs
            "100",  // Adresse commune pour les points d'accès
            "101",  // Adresse commune secondaire
            "254"   // Dernière adresse souvent utilisée par les hôtes
        )
        
        // D'abord, tester si le serveur est sur notre propre machine
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress("127.0.0.1", port), 300)
            socket.close()
            Log.d("ImagesTransfer", "Serveur trouvé sur localhost")
            return@withContext "127.0.0.1"
        } catch (e: Exception) {
            // Ignorer et continuer
        }
        
        // Ensuite, essayer les adresses prioritaires
        for (lastOctet in priorityAddresses) {
            val potentialServer = "$networkPrefix.$lastOctet"
            try {
                val socket = Socket()
                socket.soTimeout = 500
                socket.connect(InetSocketAddress(potentialServer, port), 500)
                
                // Vérifier si c'est bien notre serveur en envoyant une requête GET
                try {
                    val urlConnection = URL("http://$potentialServer:$port").openConnection() as HttpURLConnection
                    urlConnection.requestMethod = "GET"
                    urlConnection.connectTimeout = 500
                    urlConnection.readTimeout = 500
                    val responseCode = urlConnection.responseCode
                    
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        socket.close()
                        Log.d("ImagesTransfer", "Serveur vérifié à l'adresse $potentialServer")
                        return@withContext potentialServer
                    }
                } catch (e: Exception) {
                    // Si la connexion TCP a réussi mais la requête HTTP a échoué,
                    // considérons quand même que c'est peut-être notre serveur
                    socket.close()
                    Log.d("ImagesTransfer", "Serveur potentiel trouvé à $potentialServer (sans vérification HTTP)")
                    return@withContext potentialServer
                }
                
                socket.close()
            } catch (e: Exception) {
                // Ignorer et essayer l'adresse suivante
            }
        }
        
        // Scan progressif du réseau, en commençant par les adresses plus susceptibles d'être assignées
        // aux serveurs ou PC (2-20, 100-110, 200-254)
        val scanRanges = listOf(2..20, 100..110, 200..254, 21..99, 111..199)
        
        for (range in scanRanges) {
            for (i in range) {
                val potentialServer = "$networkPrefix.$i"
                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(potentialServer, port), 300)
                    socket.close()
                    Log.d("ImagesTransfer", "Serveur trouvé à l'adresse $potentialServer")
                    return@withContext potentialServer
                } catch (e: Exception) {
                    // Ignorer les échecs et passer à l'adresse suivante
                    continue
                }
            }
        }
        
        null // Aucun serveur trouvé
    }
    private fun getFormattedDate(date: Long): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(date))
    }
    
    private suspend fun verifyServerAvailability(serverIp: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$serverIp:$port")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            
            val responseCode = connection.responseCode
            return@withContext responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            Log.e("ImagesTransfer", "Erreur lors de la vérification du serveur", e)
            return@withContext false
        }
    }