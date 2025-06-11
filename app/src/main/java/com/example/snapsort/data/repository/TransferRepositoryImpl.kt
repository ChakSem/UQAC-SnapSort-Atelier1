package com.example.snapsort.data.repository

import android.content.Context
import com.example.snapsort.domain.model.Image
import com.example.snapsort.domain.model.TransferProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferRepositoryImpl @Inject constructor(
    private val context: Context
) : TransferRepository {

    override fun transferImages(
        images: List<Image>,
        serverAddress: String,
        port: Int
    ): Flow<TransferProgress> = flow {
        val batchSize = 10
        val batches = images.chunked(batchSize)
        var totalTransferred = 0
        val totalBytes = images.sumOf { it.sizeInBytes }
        var bytesTransferred = 0L
        val startTime = System.currentTimeMillis()

        for ((batchIndex, batch) in batches.withIndex()) {
            try {
                val batchBytes = transferBatch(batch, serverAddress, port)
                bytesTransferred += batchBytes
                totalTransferred += batch.size
                
                // Calculate transfer statistics
                val currentTime = System.currentTimeMillis()
                val elapsedSeconds = (currentTime - startTime) / 1000.0
                val speed = if (elapsedSeconds > 0) {
                    (bytesTransferred / (1024.0 * 1024.0)) / elapsedSeconds
                } else 0.0
                
                val remainingBytes = totalBytes - bytesTransferred
                val estimatedTimeRemaining = if (speed > 0) {
                    (remainingBytes / (1024.0 * 1024.0)) / speed
                } else 0.0

                emit(
                    TransferProgress(
                        currentImageName = batch.lastOrNull()?.name ?: "",
                        transferredCount = totalTransferred,
                        totalCount = images.size,
                        bytesTransferred = bytesTransferred,
                        totalBytes = totalBytes,
                        speed = speed.toFloat(),
                        estimatedTimeRemaining = estimatedTimeRemaining.toLong()
                    )
                )
                
                // Pause between batches
                if (batchIndex < batches.size - 1) {
                    delay(1000)
                }
                
            } catch (e: Exception) {
                throw e
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun transferBatch(
        images: List<Image>,
        serverAddress: String,
        port: Int
    ): Long = withContext(Dispatchers.IO) {
        val url = URL("http://$serverAddress:$port")
        val connection = url.openConnection() as HttpURLConnection

        connection.apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 30000
            readTimeout = 180000
            setRequestProperty("Content-Type", "application/octet-stream")
        }

        connection.connect()
        var totalBytes = 0L

        try {
            connection.outputStream.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                    writer.write("${images.size}\n")
                    writer.flush()

                    images.forEach { image ->
                        transferSingleImage(image, outputStream, writer)
                        totalBytes += image.sizeInBytes
                        delay(50)
                    }
                }
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Server responded with error: ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
        
        totalBytes
    }

    private suspend fun transferSingleImage(
        image: Image,
        outputStream: java.io.OutputStream,
        writer: BufferedWriter
    ) = withContext(Dispatchers.IO) {
        val uri = android.net.Uri.parse(image.path)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val fileName = normalizeFileName(image.name)
            writer.write("$fileName\n")

            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val dateString = dateFormat.format(image.dateTaken)
            writer.write("$dateString\n")

            val fileSize = image.sizeInBytes
            writer.write("$fileSize\n")
            writer.flush()

            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                if (bytesRead % (buffer.size * 10) == 0) {
                    outputStream.flush()
                }
            }
            outputStream.flush()
        } ?: throw Exception("Cannot open image: ${image.name}")
    }

    private fun normalizeFileName(originalName: String): String {
        return originalName
            .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            .replace("__+".toRegex(), "_")
            .trim('_')
            .takeIf { it.isNotEmpty() } ?: "image_${System.currentTimeMillis()}"
    }
}