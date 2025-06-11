// utils/NetworkUtils.kt
package com.example.snapsort.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object NetworkUtils {
    
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    suspend fun testConnection(host: String, port: Int, timeoutMs: Int = 5000): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                    true
                }
            } catch (e: Exception) {
                false
            }
        }
    }
    
    fun parseWifiQrCode(qrContent: String): Triple<String, String, String>? {
        if (!qrContent.startsWith("WIFI:")) return null
        
        val regex = """WIFI:S:([^;]*);P:([^;]*);T:([^;]*);""".toRegex()
        val matchResult = regex.find(qrContent) ?: return null
        
        val ssid = matchResult.groupValues.getOrNull(1) ?: ""
        val password = matchResult.groupValues.getOrNull(2) ?: ""
        val encryption = matchResult.groupValues.getOrNull(3) ?: ""
        
        return if (ssid.isNotEmpty()) Triple(ssid, password, encryption) else null
    }
}