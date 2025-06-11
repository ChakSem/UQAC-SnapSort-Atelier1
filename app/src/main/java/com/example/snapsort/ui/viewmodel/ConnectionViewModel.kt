package com.example.snapsort.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snapsort.domain.model.NetworkConnection
import com.example.snapsort.domain.usecase.ConnectToWifiUseCase
import com.example.snapsort.domain.usecase.FindServerUseCase
import com.example.snapsort.domain.usecase.GetNetworkStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val getNetworkStatusUseCase: GetNetworkStatusUseCase,
    private val connectToWifiUseCase: ConnectToWifiUseCase,
    private val findServerUseCase: FindServerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectionUiState())
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    init {
        // Start observing network status immediately
        observeNetworkStatus()
    }

    fun checkPermissionsAndStartNetworkObservation() {
        // This method can be used to trigger permission checks and start network observation
        // For now, we'll just ensure network observation is running
        observeNetworkStatus()
    }

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            getNetworkStatusUseCase()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Erreur réseau inconnue"
                    )
                }
                .collect { connection ->
                    _uiState.value = _uiState.value.copy(
                        networkConnection = connection,
                        error = null
                    )
                }
        }
    }

    fun connectToWifi(ssid: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true, error = null)

            connectToWifiUseCase(ssid, password)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        connectionMessage = "Connexion réussie à $ssid"
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        error = e.message ?: "Échec de la connexion"
                    )
                }
        }
    }

    fun handleQrWifiData(qrData: String) {
        // Parse WiFi QR code data
        // Format: WIFI:T:WPA;S:NetworkName;P:password;;
        try {
            if (qrData.startsWith("WIFI:")) {
                val parts = qrData.removePrefix("WIFI:").split(";")
                var ssid = ""
                var password = ""
                var security = ""

                for (part in parts) {
                    when {
                        part.startsWith("S:") -> ssid = part.removePrefix("S:")
                        part.startsWith("P:") -> password = part.removePrefix("P:")
                        part.startsWith("T:") -> security = part.removePrefix("T:")
                    }
                }

                if (ssid.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        connectionMessage = "QR code scanné: $ssid"
                    )

                    // Attempt to connect to the WiFi network
                    connectToWifi(ssid, password)
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "QR code WiFi invalide"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "Ce n'est pas un QR code WiFi valide"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Erreur lors du traitement du QR code: ${e.message}"
            )
        }
    }

    fun findServer() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFindingServer = true, error = null)

            findServerUseCase()
                .onSuccess { serverAddress ->
                    _uiState.value = _uiState.value.copy(
                        isFindingServer = false,
                        serverAddress = serverAddress,
                        connectionMessage = "Serveur trouvé à $serverAddress"
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isFindingServer = false,
                        error = e.message ?: "Serveur non trouvé"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(connectionMessage = null)
    }
}

data class ConnectionUiState(
    val networkConnection: NetworkConnection? = null,
    val isConnecting: Boolean = false,
    val isFindingServer: Boolean = false,
    val serverAddress: String? = null,
    val connectionMessage: String? = null,
    val error: String? = null
)