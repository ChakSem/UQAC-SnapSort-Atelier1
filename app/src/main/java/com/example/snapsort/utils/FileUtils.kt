// utils/FileUtils.kt
package com.example.snapsort.utils

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import java.io.File
import java.io.InputStream
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

object FileUtils {
    
    private val decimalFormat = DecimalFormat("#,##0.#")
    
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        
        return "${decimalFormat.format(bytes / 1024.0.pow(digitGroups.toDouble()))} ${units[digitGroups]}"
    }
    
    fun getFileExtension(uri: Uri): String {
        return uri.toString().substringAfterLast('.', "")
    }
    
    fun isImageFile(mimeType: String): Boolean {
        return mimeType.startsWith("image/")
    }
    
    fun getFileSizeFromUri(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.available().toLong()
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    fun validateFileName(fileName: String): String {
        return fileName
            .replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
            .replace(Regex("_{2,}"), "_")
            .trim('_')
            .takeIf { it.isNotEmpty() } ?: "unknown_file"
    }
}