package com.example.snapsort.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snapsort.domain.model.Folder
import com.example.snapsort.domain.model.Image
import com.example.snapsort.domain.usecase.FilterImagesByDateRangeUseCase
import com.example.snapsort.domain.usecase.GetAvailableFoldersUseCase
import com.example.snapsort.domain.usecase.GetImagesFromFolderUseCase
import com.example.snapsort.domain.usecase.ValidateImagesForTransferUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ImageSelectionViewModel @Inject constructor(
    private val getAvailableFoldersUseCase: GetAvailableFoldersUseCase,
    private val getImagesFromFolderUseCase: GetImagesFromFolderUseCase,
    private val filterImagesByDateRangeUseCase: FilterImagesByDateRangeUseCase,
    private val validateImagesForTransferUseCase: ValidateImagesForTransferUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageSelectionUiState())
    val uiState: StateFlow<ImageSelectionUiState> = _uiState.asStateFlow()

    init {
        loadFolders()
    }

    fun loadFolders() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            getAvailableFoldersUseCase()
                .onSuccess { folders ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        availableFolders = folders,
                        selectedFolder = folders.firstOrNull()
                    )
                    // Load images from first folder
                    folders.firstOrNull()?.let { loadImagesFromFolder(it.name) }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Erreur lors du chargement des dossiers"
                    )
                }
        }
    }

    fun selectFolder(folder: Folder) {
        _uiState.value = _uiState.value.copy(selectedFolder = folder)
        loadImagesFromFolder(folder.name)
    }

    fun updateDateRange(startDate: Date, endDate: Date) {
        val currentState = _uiState.value
        val filteredImages = filterImagesByDateRangeUseCase(
            currentState.allImages,
            startDate,
            endDate
        )
        
        _uiState.value = currentState.copy(
            filteredImages = filteredImages,
            dateRange = Pair(startDate, endDate)
        )
    }

    fun validateForTransfer(): Result<Unit> {
        return validateImagesForTransferUseCase(_uiState.value.filteredImages)
    }

    private fun loadImagesFromFolder(folderName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingImages = true, error = null)
            
            getImagesFromFolderUseCase(folderName)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingImages = false,
                        error = e.message ?: "Erreur lors du chargement des images"
                    )
                }
                .collect { images ->
                    val dateRange = if (images.isNotEmpty()) {
                        val minDate = images.minByOrNull { it.dateTaken }?.dateTaken
                        val maxDate = images.maxByOrNull { it.dateTaken }?.dateTaken
                        if (minDate != null && maxDate != null) Pair(maxDate, minDate) else null
                    } else null
                    
                    _uiState.value = _uiState.value.copy(
                        isLoadingImages = false,
                        allImages = images,
                        filteredImages = images,
                        dateRange = dateRange
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class ImageSelectionUiState(
    val isLoading: Boolean = false,
    val isLoadingImages: Boolean = false,
    val availableFolders: List<Folder> = emptyList(),
    val selectedFolder: Folder? = null,
    val allImages: List<Image> = emptyList(),
    val filteredImages: List<Image> = emptyList(),
    val dateRange: Pair<Date, Date>? = null,
    val error: String? = null
)