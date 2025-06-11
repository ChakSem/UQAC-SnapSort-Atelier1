package com.example.snapsort.domain.model

import java.util.Date

data class Image(
    val id: String,
    val name: String,
    val path: String,
    val dateTaken: Date,
    val sizeInBytes: Long,
    val mimeType: String
) {
    val sizeInMB: Float
        get() = sizeInBytes / (1024f * 1024f)
}

data class Folder(
    val name: String,
    val displayName: String,
    val imageCount: Int
)

data class NetworkConnection(
    val ssid: String,
    val isConnected: Boolean,
    val isHotspot: Boolean,
    val ipAddress: String?
)

data class TransferProgress(
    val currentImageName: String,
    val transferredCount: Int,
    val totalCount: Int,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val speed: Float, // MB/s
    val estimatedTimeRemaining: Long // seconds
) {
    val progressPercentage: Float
        get() = if (totalCount > 0) transferredCount.toFloat() / totalCount else 0f
        
    val bytesProgressPercentage: Float
        get() = if (totalBytes > 0) bytesTransferred.toFloat() / totalBytes else 0f
}