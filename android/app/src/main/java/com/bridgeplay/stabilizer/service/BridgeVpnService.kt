package com.bridgeplay.stabilizer.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.bridgeplay.stabilizer.network.DualNetworkBinder
import com.bridgeplay.stabilizer.optimization.WifiLatencyOptimizer
import com.bridgeplay.stabilizer.protocol.PacketMultiplexer
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress

class BridgeVpnService : VpnService() {

    private val tag = "BridgeVpnService"
    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false

    private lateinit var networkBinder: DualNetworkBinder
    private lateinit var optimizer: WifiLatencyOptimizer
    private var multiplexer: PacketMultiplexer? = null
    private var workerThread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        networkBinder = DualNetworkBinder(this).apply { startListening() }
        optimizer = WifiLatencyOptimizer(this).apply { enableAggressiveGamingMode() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val serverIp = intent?.getStringExtra("SERVER_IP") ?: "127.0.0.1"
        val serverPort = intent?.getIntExtra("SERVER_PORT", 8910) ?: 8910

        if (!isRunning) {
            isRunning = true
            startVpnTunnel(serverIp, serverPort)
        }
        return START_STICKY
    }

    private fun startVpnTunnel(serverIp: String, serverPort: Int) {
        workerThread = Thread {
            try {
                val serverAddr = InetAddress.getByName(serverIp)
                multiplexer = PacketMultiplexer(networkBinder, optimizer, serverAddr, serverPort).apply {
                    initializeSockets()
                }

                val builder = Builder()
                    .setSession("BridgePlay Stabilizer")
                    .addAddress("10.89.0.2", 24)
                    .addRoute("0.0.0.0", 0)
                    .setMtu(1400)

                // Split tunneling: selectively route Mobile Legends package if present
                try {
                    builder.addAllowedApplication("com.mobile.legends")
                    Log.i(tag, "Split-tunneling enabled specifically for Mobile Legends (com.mobile.legends)")
                } catch (e: Exception) {
                    Log.w(tag, "Mobile Legends app not found or package restriction bypassed: ${e.message}")
                }

                vpnInterface = builder.establish()
                val fd = vpnInterface?.fileDescriptor ?: return@Thread

                val inStream = FileInputStream(fd)
                val outStream = FileOutputStream(fd)
                val buffer = ByteArray(32767)

                Log.i(tag, "BridgePlay TUN virtual interface active. Processing packets...")

                while (isRunning) {
                    val length = inStream.read(buffer)
                    if (length > 0) {
                        // Inspect IPv4 packet header
                        val version = (buffer[0].toInt() shr 4) and 0x0F
                        if (version == 4 && length >= 20) {
                            val protocol = buffer[9].toInt() and 0xFF
                            if (protocol == 17) { // UDP datagram
                                val ipHeaderLen = (buffer[0].toInt() and 0x0F) * 4
                                val udpPayloadOffset = ipHeaderLen + 8
                                if (length > udpPayloadOffset) {
                                    val payloadLen = length - udpPayloadOffset
                                    val gamePayload = ByteArray(payloadLen)
                                    System.arraycopy(buffer, udpPayloadOffset, gamePayload, 0, payloadLen)

                                    // Duplicate packet to both Wi-Fi and 4G LTE simultaneously
                                    multiplexer?.dispatchGamePacket(gamePayload)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "VPN Tunnel error: ${e.message}")
            } finally {
                cleanup()
            }
        }.apply { start() }
    }

    private fun cleanup() {
        isRunning = false
        multiplexer?.close()
        optimizer.release()
        networkBinder.stopListening()
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (_: Exception) {}
        Log.i(tag, "BridgeVpnService cleaned up successfully.")
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }
}
