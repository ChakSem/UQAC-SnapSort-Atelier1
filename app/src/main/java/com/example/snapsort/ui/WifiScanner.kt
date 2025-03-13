package com.example.snapsort.ui

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.NetworkInterface

data class NetworkDevice(
    val hostname: String,
    val ipAddress: String,
    val macAddress: String,
    val signalStrength: Int = 0
)

sealed class ScanResult {
    data class Success(val devices: List<NetworkDevice>) : ScanResult()
    data class Error(val message: String) : ScanResult()
}

object WifiScanner {
    private const val SCAN_TIMEOUT = 1000L  // Durée maximale pour un scan en ms
    private const val PARALLEL_SCANS = 16 // C'est le nombre d'adresses IP à scanner en parallèle

    suspend fun startScan(context: Context): ScanResult = withContext(Dispatchers.IO) {
        try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            
            if (!wifiManager.isWifiEnabled) {
                return@withContext ScanResult.Error("Le WiFi est désactivé")
            }

            val connectionInfo = wifiManager.connectionInfo
            if (connectionInfo == null) {
                return@withContext ScanResult.Error("Non connecté au réseau WiFi")
            }

            val devices = scanNetwork(wifiManager)
            ScanResult.Success(devices.sortedByDescending { it.signalStrength })
            
        } catch (e: Exception) {
            ScanResult.Error("Erreur de scan : ${e.localizedMessage}")
        }
    }

    private suspend fun scanNetwork(wifiManager: WifiManager): List<NetworkDevice> = 
        coroutineScope {
            val dhcpInfo = wifiManager.dhcpInfo
            val subnet = getSubnet(dhcpInfo.ipAddress)
            val currentRssi = wifiManager.connectionInfo.rssi

            (1..254).chunked(PARALLEL_SCANS)
                .flatMap { chunk ->
                    chunk.map { i ->
                        async {
                            scanAddress("$subnet.$i", currentRssi)
                        }
                    }.awaitAll()
                }
                .filterNotNull()
        }

    private suspend fun scanAddress(ip: String, rssi: Int): NetworkDevice? =
        withContext(Dispatchers.IO) {
            try {
                val address = InetAddress.getByName(ip)
                if (address.isReachable(SCAN_TIMEOUT.toInt())) {
                    NetworkDevice(
                        hostname = address.hostName.takeIf { it != ip } ?: "Appareil $ip",
                        ipAddress = ip,
                        macAddress = getMacFromArpCache(ip),
                        signalStrength = rssi
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }

    private fun getMacFromArpCache(ip: String): String {
        return try {
            val process = Runtime.getRuntime().exec("arp -a $ip")
            val result = process.inputStream.bufferedReader().readText()
            val macPattern = "[0-9A-Fa-f]{2}[-:][0-9A-Fa-f]{2}[-:][0-9A-Fa-f]{2}[-:]" +
                           "[0-9A-Fa-f]{2}[-:][0-9A-Fa-f]{2}[-:][0-9A-Fa-f]{2}"
            
            result.lineSequence()
                .filter { it.contains(ip) }
                .mapNotNull { line ->
                    Regex(macPattern).find(line)?.value
                }
                .firstOrNull() ?: "Non disponible"
        } catch (e: Exception) {
            "Non disponible"
        }
    }

    private fun getSubnet(ip: Int): String =
        "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}"
}
