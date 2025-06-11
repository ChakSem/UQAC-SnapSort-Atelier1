// domain/usecase/ImageUseCases.kt
package com.example.snapsort.domain.usecase

import com.example.snapsort.data.repository.ImageRepositoryImpl
import com.example.snapsort.domain.model.Folder
import com.example.snapsort.domain.model.Image
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject

class GetAvailableFoldersUseCase @Inject constructor(
    private val imageRepository: ImageRepositoryImpl
) {
    suspend operator fun invoke(): Result<List<Folder>> {
        return imageRepository.getAvailableFolders()
    }
}

class GetImagesFromFolderUseCase @Inject constructor(
    private val imageRepository: ImageRepositoryImpl
) {
    operator fun invoke(
        folderName: String,
        limit: Int = 1000,
        offset: Int = 0
    ): Flow<List<Image>> {
        return imageRepository.getImagesFromFolder(folderName, limit, offset)
    }
}

class FilterImagesByDateRangeUseCase @Inject constructor() {
    operator fun invoke(
        images: List<Image>,
        startDate: Date,
        endDate: Date
    ): List<Image> {
        return images.filter { image ->
            image.dateTaken.after(endDate) && image.dateTaken.before(startDate)
        }
    }
}