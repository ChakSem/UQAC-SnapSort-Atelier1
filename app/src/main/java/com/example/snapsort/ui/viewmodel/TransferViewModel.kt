// ui/viewmodel/TransferViewModel.kt
package com.example.snapsort.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snapsort.domain.model.Image
import com.example.snapsort.domain.model.TransferProgress
import com.example.snapsort.domain.usecase.TransferImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val transferImagesUseCase: TransferImagesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    fun startTransfer(images: List<Image>, serverAddress: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isTransferring = true,
                error = null,
                isCompleted = false
            )
            
            transferImagesUseCase(images, serverAddress)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isTransferring = false,
                        error = e.message ?: "Erreur lors du transfert"
                    )
                }
                .collect { progress ->
                    _uiState.value = _uiState.value.copy(
                        transferProgress = progress,
                        isCompleted = progress.transferredCount == progress.totalCount
                    )
                    
                    if (progress.transferredCount == progress.totalCount) {
                        _uiState.value = _uiState.value.copy(isTransferring = false)
                    }
                }
        }
    }

    fun cancelTransfer() {
        // Implementation for cancelling transfer
        _uiState.value = _uiState.value.copy(
            isTransferring = false,
            error = "Transfert annulé"
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetTransfer() {
        _uiState.value = TransferUiState()
    }
}

data class TransferUiState(
    val isTransferring: Boolean = false,
    val isCompleted: Boolean = false,
    val transferProgress: TransferProgress? = null,
    val error: String? = null
)