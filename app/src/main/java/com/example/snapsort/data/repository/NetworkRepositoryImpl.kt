package com.example.snapsort.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import com.example.snapsort.domain.model.NetworkConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

interface NetworkRepository {
    fun getNetworkStatus(): Flow<NetworkConnection>
    suspend fun connectToWifi(ssid: String, password: String): Result<Unit>
    suspend fun findServerAddress(port: Int): Result<String>
}

@Singleton
class NetworkRepositoryImpl @Inject constructor(
    private val context: Context
) : NetworkRepository {

    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val wifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    override fun getNetworkStatus(): Flow<NetworkConnection> = flow {
        while (true) {
            val connection = getCurrentNetworkConnection()
            emit(connection)
            kotlinx.coroutines.delay(2000) // Check every 2 seconds
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun connectToWifi(ssid: String, password: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    connectToWifiAndroid10Plus(ssid, password)
                } else {
                    connectToWifiLegacy(ssid, password)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun findServerAddress(port: Int): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val localIp = getCurrentIpAddress()
                    ?: return@withContext Result.failure(Exception("Impossible d'obtenir l'IP locale"))

                val networkPrefix = localIp.substringBeforeLast(".")
                val priorityAddresses = listOf(1, 100, 101, 254)

                // Test priority addresses first
                for (lastOctet in priorityAddresses) {
                    val serverAddress = "$networkPrefix.$lastOctet"
                    if (testConnection(serverAddress, port)) {
                        return@withContext Result.success(serverAddress)
                    }
                }

                // Scan remaining addresses
                for (i in 2..253) {
                    if (i !in priorityAddresses) {
                        val serverAddress = "$networkPrefix.$i"
                        if (testConnection(serverAddress, port)) {
                            return@withContext Result.success(serverAddress)
                        }
                    }
                }

                Result.failure(Exception("Serveur non trouvé sur le réseau"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun getCurrentNetworkConnection(): NetworkConnection {
        val ssid = getCurrentSsid()
        val isConnected = isConnectedToWifi()
        val isHotspot = isConnectedToHotspot()
        val ipAddress = getCurrentIpAddress()

        return NetworkConnection(
            ssid = ssid,
            isConnected = isConnected,
            isHotspot = isHotspot,
            ipAddress = ipAddress
        )
    }

    private fun isConnectedToWifi(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun isConnectedToHotspot(): Boolean {
        if (!isConnectedToWifi()) return false
        val ipAddress = getCurrentIpAddress()
        return ipAddress?.startsWith("192.168.137.") == true ||
                ipAddress?.startsWith("192.168.43.") == true
    }

    private fun getCurrentSsid(): String {
        return try {
            val wifiInfo = wifiManager.connectionInfo
            wifiInfo.ssid?.removeSurrounding("\"") ?: "Non connecté"
        } catch (e: Exception) {
            "Non connecté"
        }
    }

    private fun getCurrentIpAddress(): String? {
        return try {
            val ipAddress = wifiManager.connectionInfo.ipAddress
            if (ipAddress != 0) {
                String.format(
                    "%d.%d.%d.%d",
                    ipAddress and 0xff,
                    ipAddress shr 8 and 0xff,
                    ipAddress shr 16 and 0xff,
                    ipAddress shr 24 and 0xff
                )
            } else {
                getIpAddressFromNetworkInterface()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getIpAddressFromNetworkInterface(): String? {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val addresses = networkInterface.inetAddresses

                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            // Handle exception
        }
        return null
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun connectToWifiAndroid10Plus(ssid: String, password: String) {
        val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .apply {
                if (password.isNotEmpty()) {
                    setWpa2Passphrase(password)
                }
            }
            .build()

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(wifiNetworkSpecifier)
            .build()

        // Implementation would require callback handling
        // This is a simplified version
    }

    private fun connectToWifiLegacy(ssid: String, password: String) {
        // Legacy implementation for older Android versions
        throw UnsupportedOperationException("Legacy WiFi connection not implemented")
    }

    private suspend fun testConnection(address: String, port: Int): Boolean {
        return try {
            val socket = Socket()
            socket.connect(java.net.InetSocketAddress(address, port), 500)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}