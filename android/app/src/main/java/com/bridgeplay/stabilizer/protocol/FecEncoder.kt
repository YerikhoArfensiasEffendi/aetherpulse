package com.bridgeplay.stabilizer.protocol

import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

/**
 * Client-Side Forward Error Correction (FEC) Encoder.
 * Generates 1 XOR parity packet for every K data packets.
 */
class FecEncoder(private val groupSize: Int = 4) {

    private val packetBuffer = ArrayList<ByteArray>()
    private val counter = AtomicInteger(0)

    @Synchronized
    fun onNewPacket(payload: ByteArray): ByteArray? {
        packetBuffer.add(payload)
        if (packetBuffer.size >= groupSize) {
            val parity = computeXorParity(packetBuffer)
            packetBuffer.clear()
            return parity
        }
        return null
    }

    private fun computeXorParity(packets: List<ByteArray>): ByteArray {
        var maxLen = 0
        for (p in packets) {
            maxLen = max(maxLen, p.size)
        }

        val parity = ByteArray(maxLen)
        for (p in packets) {
            for (i in p.indices) {
                parity[i] = (parity[i].toInt() xor p[i].toInt()).toByte()
            }
        }
        return parity
    }

    @Synchronized
    fun reset() {
        packetBuffer.clear()
    }
}
