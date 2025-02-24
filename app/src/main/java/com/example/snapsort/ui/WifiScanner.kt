package com.example.snapsort.ui

import android.content.Context
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.Executors

data class NetworkDevice(
    val hostname: String,
    val ipAddress: String,
    val macAddress: String
)

sealed class ScanResult {
    data class Success(val devices: List<NetworkDevice>) : ScanResult()
    data class Error(val message: String) : ScanResult()
}

object WifiScanner {
    fun startScan(context: Context, onResult: (ScanResult) -> Unit) {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (!wifiManager.isWifiEnabled) {
            onResult(ScanResult.Error("Veuillez activer le WiFi"))
            return
        }

        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                val ipAddress = wifiManager.connectionInfo.ipAddress
                val ip = String.format(
                    "%d.%d.%d",
                    ipAddress and 0xff,
                    (ipAddress shr 8) and 0xff,
                    (ipAddress shr 16) and 0xff
                )

                val devices = mutableListOf<NetworkDevice>()

                for (i in 1..254) {
                    val testIp = "$ip.$i"
                    val address = InetAddress.getByName(testIp)

                    if (address.isReachable(1000)) {
                        val hostname = address.hostName
                        val macAddress = getMacAddress(testIp, context)
                        devices.add(
                            NetworkDevice(
                                hostname = hostname,
                                ipAddress = testIp,
                                macAddress = macAddress
                            )
                        )
                    }
                }

                onResult(ScanResult.Success(devices))
            } catch (e: Exception) {
                onResult(ScanResult.Error("Erreur lors du scan: ${e.message}"))
            }
        }
    }

    private fun getMacAddress(ip: String, context: Context): String {
        try {
            // Essayer d'abord d'obtenir l'adresse MAC via l'interface réseau
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress == ip) {
                        val mac = networkInterface.hardwareAddress
                        if (mac != null) {
                            return mac.joinToString(":") { byte -> "%02X".format(byte) }
                        }
                    }
                }
            }

            // Si l'adresse MAC n'est pas trouvée via l'interface réseau, essayer ARP
            val runtime = Runtime.getRuntime()
            val process = runtime.exec("ip neigh show $ip")
            val output = process.inputStream.bufferedReader().readText()

            // Chercher l'adresse MAC dans la sortie de la commande ARP
            val macMatch = Regex("[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}").find(output)
            if (macMatch != null) {
                return macMatch.value.uppercase()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return "Non disponible"
    }
}
