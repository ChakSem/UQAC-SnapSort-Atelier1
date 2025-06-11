// domain/usecase/NetworkUseCases.kt
package com.example.snapsort.domain.usecase

import com.example.snapsort.data.repository.NetworkRepository
import com.example.snapsort.domain.model.NetworkConnection
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNetworkStatusUseCase @Inject constructor(
    private val networkRepository: NetworkRepository
) {
    operator fun invoke(): Flow<NetworkConnection> {
        return networkRepository.getNetworkStatus()
    }
}

class ConnectToWifiUseCase @Inject constructor(
    private val networkRepository: NetworkRepository
) {
    suspend operator fun invoke(ssid: String, password: String): Result<Unit> {
        return networkRepository.connectToWifi(ssid, password)
    }
}

class FindServerUseCase @Inject constructor(
    private val networkRepository: NetworkRepository
) {
    suspend operator fun invoke(port: Int = 8080): Result<String> {
        return networkRepository.findServerAddress(port)
    }
}
