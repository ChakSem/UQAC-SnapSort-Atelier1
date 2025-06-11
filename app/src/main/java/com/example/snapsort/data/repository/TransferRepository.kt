package com.example.snapsort.data.repository

import com.example.snapsort.domain.model.Image
import com.example.snapsort.domain.model.TransferProgress
import kotlinx.coroutines.flow.Flow

interface TransferRepository {
    fun transferImages(
        images: List<Image>,
        serverAddress: String,
        port: Int
    ): Flow<TransferProgress>
}