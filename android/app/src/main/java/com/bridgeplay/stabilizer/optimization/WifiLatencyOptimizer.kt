package com.bridgeplay.stabilizer.optimization

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.util.Log
import java.net.DatagramSocket

/**
 * Hardware and Kernel Optimizer for Samsung Galaxy A53 (One UI)
 * Forces Wi-Fi chip out of 802.11 Power Save Polling and acquires low latency locks.
 */
class WifiLatencyOptimizer(private val context: Context) {

    private val tag = "WifiLatencyOptimizer"
    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    fun enableAggressiveGamingMode() {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                // Android 10+ (API 29+) WIFI_MODE_FULL_LOW_LATENCY disables Wi-Fi power saving
                wifiLock = wifiManager.createWifiLock(
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY,
                    "BridgePlay:GameLowLatencyLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire()
                }
                Log.i(tag, "Wi-Fi Full Low Latency Lock acquired successfully.")
            }

            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "BridgePlay:CpuWakeLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire(10 * 60 * 1000L /* 10 minutes session lock */)
                }
                Log.i(tag, "Partial CPU WakeLock acquired to eliminate background thread throttling.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to apply low-latency hardware locks: ${e.message}")
        }
    }

    fun applyDSCPPriority(socket: DatagramSocket) {
        try {
            // DSCP 46 (Expedited Forwarding) -> ToS byte = 0xB8 (46 << 2)
            // Maps directly to 802.11e WMM Access Category Voice (AC_VO) in Wi-Fi routers
            socket.trafficClass = 0xB8
        } catch (e: Exception) {
            Log.w(tag, "Could not set DSCP EF on socket: ${e.message}")
        }
    }

    fun release() {
        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
                Log.i(tag, "Wi-Fi lock released.")
            }
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.i(tag, "CPU wake lock released.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error releasing locks: ${e.message}")
        }
    }
}
