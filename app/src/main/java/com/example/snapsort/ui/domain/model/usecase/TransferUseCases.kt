// domain/usecase/TransferUseCases.kt
package com.example.snapsort.domain.usecase

import com.example.snapsort.data.repository.TransferRepository
import com.example.snapsort.domain.model.Image
import com.example.snapsort.domain.model.TransferProgress
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TransferImagesUseCase @Inject constructor(
    private val transferRepository: TransferRepository
) {
    operator fun invoke(
        images: List<Image>,
        serverAddress: String,
        port: Int = 8080
    ): Flow<TransferProgress> {
        return transferRepository.transferImages(images, serverAddress, port)
    }
}

class ValidateImagesForTransferUseCase @Inject constructor() {
    operator fun invoke(images: List<Image>): Result<Unit> {
        return when {
            images.isEmpty() -> Result.failure(Exception("Aucune image sélectionnée"))
            images.sumOf { it.sizeInBytes } > MAX_TRANSFER_SIZE -> 
                Result.failure(Exception("Taille totale trop importante"))
            else -> Result.success(Unit)
        }
    }
    
    companion object {
        private const val MAX_TRANSFER_SIZE = 5L * 1024 * 1024 * 1024 // 5GB
    }
}