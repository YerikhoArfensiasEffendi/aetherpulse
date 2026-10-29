package com.bridgeplay.stabilizer.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import java.net.DatagramSocket

/**
 * Manages simultaneous bindings to Wi-Fi and Cellular physical radios on Android.
 */
class DualNetworkBinder(private val context: Context) {

    private val tag = "DualNetworkBinder"
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Volatile
    var wifiNetwork: Network? = null
        private set

    @Volatile
    var cellularNetwork: Network? = null
        private set

    private var wifiCallback: ConnectivityManager.NetworkCallback? = null
    private var cellularCallback: ConnectivityManager.NetworkCallback? = null

    fun startListening() {
        val wifiRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        wifiCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                wifiNetwork = network
                Log.i(tag, "Wi-Fi interface active and bound: $network")
            }

            override fun onLost(network: Network) {
                if (wifiNetwork == network) {
                    wifiNetwork = null
                    Log.w(tag, "Wi-Fi interface lost")
                }
            }
        }
        connectivityManager.registerNetworkCallback(wifiRequest, wifiCallback!!)

        val cellularRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cellularCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                cellularNetwork = network
                Log.i(tag, "Cellular (4G/5G) interface active and bound: $network")
            }

            override fun onLost(network: Network) {
                if (cellularNetwork == network) {
                    cellularNetwork = null
                    Log.w(tag, "Cellular interface lost")
                }
            }
        }
        connectivityManager.registerNetworkCallback(cellularRequest, cellularCallback!!)
    }

    fun bindSocketToWifi(socket: DatagramSocket): Boolean {
        val net = wifiNetwork ?: return false
        return try {
            net.bindSocket(socket)
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to bind socket to Wi-Fi: ${e.message}")
            false
        }
    }

    fun bindSocketToCellular(socket: DatagramSocket): Boolean {
        val net = cellularNetwork ?: return false
        return try {
            net.bindSocket(socket)
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to bind socket to Cellular: ${e.message}")
            false
        }
    }

    fun stopListening() {
        try {
            wifiCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
            cellularCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
        } catch (e: Exception) {
            Log.e(tag, "Error unregistering network callbacks: ${e.message}")
        }
    }
}
