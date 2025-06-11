// data/repository/ImageRepositoryImpl.kt
package com.example.snapsort.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.snapsort.domain.model.Folder
import com.example.snapsort.domain.model.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

interface ImageRepository {
    suspend fun getAvailableFolders(): Result<List<Folder>>
    fun getImagesFromFolder(folderName: String, limit: Int, offset: Int): Flow<List<Image>>
}

@Singleton
class ImageRepositoryImpl @Inject constructor(
    private val context: Context
) : ImageRepository {

    override suspend fun getAvailableFolders(): Result<List<Folder>> {
        return withContext(Dispatchers.IO) {
            try {
                val folders = mutableListOf<Folder>()
                folders.add(Folder("ALL", "Toutes les images", 0))

                val projection = arrayOf(
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                    "COUNT(*) as count"
                )

                val cursor = context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Images.Media.BUCKET_DISPLAY_NAME),
                    null,
                    null,
                    "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} ASC"
                )

                cursor?.use {
                    val bucketColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    val folderCounts = mutableMapOf<String, Int>()

                    while (it.moveToNext()) {
                        val bucketName = it.getString(bucketColumn)
                        if (!bucketName.isNullOrEmpty()) {
                            folderCounts[bucketName] = folderCounts.getOrDefault(bucketName, 0) + 1
                        }
                    }

                    folderCounts.forEach { (name, count) ->
                        folders.add(Folder(name, name, count))
                    }
                }

                Result.success(folders)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun getImagesFromFolder(
        folderName: String,
        limit: Int,
        offset: Int
    ): Flow<List<Image>> = flow {
        val images = loadImagesFromStorage(folderName, limit, offset)
        emit(images)
    }.flowOn(Dispatchers.IO)

    private suspend fun loadImagesFromStorage(
        folderName: String,
        limit: Int,
        offset: Int
    ): List<Image> = withContext(Dispatchers.IO) {
        val images = mutableListOf<Image>()

        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE
            )

            val (selection, selectionArgs) = buildSelectionQuery(folderName)
            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use { c ->
                val idColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val pathColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val mimeColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

                var count = 0
                var skipped = 0

                while (c.moveToNext() && count < limit) {
                    if (skipped < offset) {
                        skipped++
                        continue
                    }

                    try {
                        val id = c.getLong(idColumn)
                        val name = c.getString(nameColumn) ?: "unknown"
                        val dateTaken = if (c.isNull(dateColumn)) {
                            System.currentTimeMillis()
                        } else {
                            c.getLong(dateColumn)
                        }
                        val path = c.getString(pathColumn) ?: ""
                        val size = if (c.isNull(sizeColumn)) 0L else c.getLong(sizeColumn)
                        val mimeType = c.getString(mimeColumn) ?: "image/jpeg"

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )

                        if (isUriAccessible(contentUri)) {
                            images.add(
                                Image(
                                    id = id.toString(),
                                    name = name,
                                    path = contentUri.toString(),
                                    dateTaken = Date(dateTaken),
                                    sizeInBytes = size,
                                    mimeType = mimeType
                                )
                            )
                            count++
                        }
                    } catch (e: Exception) {
                        // Log error but continue
                    }
                }
            }
        } catch (e: Exception) {
            // Log error
        }

        images
    }

    private fun buildSelectionQuery(folderName: String): Pair<String?, Array<String>?> {
        return when (folderName) {
            "ALL" -> null to null
            else -> "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?" to arrayOf(folderName)
        }
    }

    private fun isUriAccessible(uri: android.net.Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
