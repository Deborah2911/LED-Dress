package com.deborah.dress

import androidx.compose.ui.graphics.Color
import io.mhssn.colorpicker.ext.blue
import io.mhssn.colorpicker.ext.green
import io.mhssn.colorpicker.ext.red
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class UdpServer(serverPort: Int) {

    private val socket = DatagramSocket(serverPort)
    private val address: InetAddress =
        InetAddress.getByName("192.168.4.1")

    fun send(color: Color, amplitude: Int, algorithm: LedAlgorithm) {
        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES + Byte.SIZE_BYTES * 4).apply {
            putInt(amplitude)
            put(color.red().toByte())
            put(color.green().toByte())
            put(color.blue().toByte())
            put(algorithm.toByte())
        }

        val bytes = buffer.array()

        try {
            val packet = DatagramPacket(bytes, bytes.size, address, 1234)

            socket.send(packet)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun close() {
        socket.close()
    }
}
