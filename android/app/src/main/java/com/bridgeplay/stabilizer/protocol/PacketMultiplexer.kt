package com.bridgeplay.stabilizer.protocol

import com.bridgeplay.stabilizer.network.DualNetworkBinder
import com.bridgeplay.stabilizer.optimization.WifiLatencyOptimizer
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Encapsulates game packets into BridgePlay protocol frames and transmits
 * duplicate copies simultaneously across Wi-Fi and Cellular interfaces.
 */
class PacketMultiplexer(
    private val networkBinder: DualNetworkBinder,
    private val optimizer: WifiLatencyOptimizer,
    private val serverAddress: InetAddress,
    private val serverPort: Int
) {
    private val magicHeader: Short = 0x4250 // "BP"
    private val typeData: Byte = 0x01
    private val typeFec: Byte = 0x02

    private val seqCounter = AtomicInteger(1)
    private val fecEncoder = FecEncoder(groupSize = 4)
    private val ioExecutor: ExecutorService = Executors.newFixedThreadPool(2)

    private var wifiSocket: DatagramSocket? = null
    private var cellularSocket: DatagramSocket? = null

    fun initializeSockets() {
        wifiSocket = DatagramSocket().also {
            optimizer.applyDSCPPriority(it)
            networkBinder.bindSocketToWifi(it)
        }

        cellularSocket = DatagramSocket().also {
            optimizer.applyDSCPPriority(it)
            networkBinder.bindSocketToCellular(it)
        }
    }

    fun dispatchGamePacket(payload: ByteArray) {
        val seq = seqCounter.getAndIncrement()
        val frame = buildFrame(typeData, flags = 0, seq = seq, payload = payload)

        // Parallel non-blocking dual transmission
        sendOverDualPath(frame)

        // Check if FEC parity packet is ready
        val parity = fecEncoder.onNewPacket(payload)
        if (parity != null) {
            val paritySeq = seqCounter.getAndIncrement()
            val parityFrame = buildFrame(typeFec, flags = 0, seq = paritySeq, payload = parity)
            sendOverDualPath(parityFrame)
        }
    }

    private fun sendOverDualPath(data: ByteArray) {
        // Send over Wi-Fi
        ioExecutor.execute {
            try {
                val sock = wifiSocket
                if (sock != null && !sock.isClosed) {
                    val packet = DatagramPacket(data, data.size, serverAddress, serverPort)
                    sock.send(packet)
                }
            } catch (_: Exception) {}
        }

        // Send over Cellular simultaneously
        ioExecutor.execute {
            try {
                val sock = cellularSocket
                if (sock != null && !sock.isClosed) {
                    val packet = DatagramPacket(data, data.size, serverAddress, serverPort)
                    sock.send(packet)
                }
            } catch (_: Exception) {}
        }
    }

    private fun buildFrame(type: Byte, flags: Byte, seq: Int, payload: ByteArray): ByteArray {
        val buffer = ByteBuffer.allocate(10 + payload.size)
        buffer.putShort(magicHeader)
        buffer.put(type)
        buffer.put(flags)
        buffer.putShort(payload.size.toShort())
        buffer.putInt(seq)
        buffer.put(payload)
        return buffer.array()
    }

    fun close() {
        try {
            wifiSocket?.close()
            cellularSocket?.close()
            ioExecutor.shutdownNow()
        } catch (_: Exception) {}
    }
}
