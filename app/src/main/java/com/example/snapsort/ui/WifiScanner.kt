package com.example.snapsort.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.FileReader
import java.net.InetAddress
import java.net.Inet4Address
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
    private const val PARALLEL_SCANS = 16 // Nombre d'adresses IP à scanner en parallèle

    suspend fun startScan(context: Context): ScanResult = withContext(Dispatchers.IO) {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            
            // Vérification que le WiFi est actif et connecté
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            if (network == null || capabilities == null || 
                !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return@withContext ScanResult.Error("Non connecté au réseau WiFi")
            }
            
            // Obtenir l'adresse IP de l'appareil et le subnet
            val deviceIpAddress = getDeviceIpAddress()
            if (deviceIpAddress.isNullOrEmpty()) {
                return@withContext ScanResult.Error("Impossible d'obtenir l'adresse IP")
            }
            
            // Obtenir une estimation de la force du signal
            // Méthode moderne, indépendante des API dépréciées
            val rssi = getWifiSignalStrength(context)
            
            // Scanner le réseau
            val devices = scanNetwork(deviceIpAddress, rssi)
            if (devices.isEmpty()) {
                return@withContext ScanResult.Error("Aucun appareil trouvé sur le réseau")
            }
            
            ScanResult.Success(devices.sortedByDescending { it.signalStrength })
            
        } catch (e: Exception) {
            ScanResult.Error("Erreur de scan : ${e.message}")
        }
    }

    private fun getWifiSignalStrength(context: Context): Int {
        // Utiliser la méthode non dépréciée pour Android 10+
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return -80
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return -80
        
        // À partir d'Android 13, on peut utiliser cette méthode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return capabilities.signalStrength
        }
        
        // Pour les versions antérieures, on utilise une estimation standard
        // Une valeur typique pour un réseau WiFi en bon état
        return -65
    }

    private fun getDeviceIpAddress(): String? {
        // Méthode moderne et non dépréciée pour obtenir l'adresse IP
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            
            for (networkInterface in networkInterfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                
                val interfaceName = networkInterface.name
                // Généralement l'interface WiFi s'appelle wlan0
                if (interfaceName.startsWith("wlan")) {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (address is Inet4Address && !address.isLoopbackAddress) {
                            return address.hostAddress
                        }
                    }
                }
            }
            
            // Si on n'a pas trouvé d'interface WiFi spécifique, essayer toutes les interfaces
            val networkIterator = NetworkInterface.getNetworkInterfaces()
            while (networkIterator.hasMoreElements()) {
                val networkInterface = networkIterator.nextElement()
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            // Ignorer l'exception
        }
        return null
    }

    private suspend fun scanNetwork(deviceIp: String, rssi: Int): List<NetworkDevice> = 
        coroutineScope {
            val subnet = deviceIp.substringBeforeLast(".")
            
            (1..254).chunked(PARALLEL_SCANS)
                .flatMap { chunk ->
                    chunk.map { i ->
                        async {
                            val ipToScan = "$subnet.$i"
                            // Ne pas scanner sa propre adresse IP
                            if (ipToScan != deviceIp) {
                                scanAddress(ipToScan, rssi)
                            } else {
                                null
                            }
                        }
                    }.awaitAll()
                }
                .filterNotNull()
        }

    private suspend fun scanAddress(ip: String, rssi: Int): NetworkDevice? =
        withContext(Dispatchers.IO) {
            try {
                // Utiliser un timeout pour éviter que le scan ne prenne trop de temps
                val reachable = withTimeoutOrNull(SCAN_TIMEOUT) {
                    val address = InetAddress.getByName(ip)
                    address.isReachable(SCAN_TIMEOUT.toInt())
                } ?: false
                
                if (reachable) {
                    val address = InetAddress.getByName(ip)
                    val mac = getMacFromArpCache(ip)
                    NetworkDevice(
                        hostname = try { 
                            address.hostName.takeIf { it != ip && it.isNotEmpty() } ?: "Appareil $ip"
                        } catch (e: Exception) { 
                            "Appareil $ip" 
                        },
                        ipAddress = ip,
                        macAddress = mac,
                        signalStrength = rssi
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }

    private fun getMacFromArpCache(ip: String): String {
        return try {
            // Essayer de lire directement du fichier ARP cache
            val reader = BufferedReader(FileReader("/proc/net/arp"))
            var line: String?
            var macAddress = "Non disponible"
            
            reader.readLine() // Skip header
            while (reader.readLine().also { line = it } != null) {
                if (line?.contains(ip) == true) {
                    val parts = line?.split("\\s+".toRegex())
                    if (parts != null && parts.size >= 4) {
                        macAddress = parts[3]
                        if (macAddress != "00:00:00:00:00:00") break
                    }
                }
            }
            reader.close()
            
            // Si pas trouvé, essayer avec la commande IP
            if (macAddress == "Non disponible") {
                val process = Runtime.getRuntime().exec("ip neigh show $ip")
                val result = process.inputStream.bufferedReader().readText()
                val macPattern = "[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:" +
                               "[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}"
                
                val foundMac = result.lineSequence()
                    .mapNotNull { line ->
                        Regex(macPattern).find(line)?.value
                    }
                    .firstOrNull()
                
                if (!foundMac.isNullOrEmpty()) {
                    macAddress = foundMac
                }
            }
            
            macAddress
        } catch (e: Exception) {
            "Non disponible"
        }
    }
}