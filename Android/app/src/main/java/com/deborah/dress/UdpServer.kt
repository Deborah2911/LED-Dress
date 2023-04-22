package com.deborah.dress

import android.util.Log
import androidx.compose.ui.graphics.Color
import io.mhssn.colorpicker.ext.blue
import io.mhssn.colorpicker.ext.green
import io.mhssn.colorpicker.ext.red
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class UdpServer(serverPort: Int) {

    private val socket = DatagramSocket(serverPort)
    private val address: InetAddress =
        InetAddress.getByAddress(byteArrayOf(192.toByte(), 168.toByte(), 4, 1))
//        InetAddress.getByAddress(byteArrayOf(192.toByte(), 168.toByte(), 0, 115))

    fun send(color: Color, amplitude: Int) {
        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES * 2).apply {
            putInt(amplitude)
            put(color.red().toByte())
            put(color.green().toByte())
            put(color.blue().toByte())
        }

        val bytes = buffer.array()
        Log.d("Sending Packet", bytes.size.toString())

        try {
            val packet = DatagramPacket(bytes, bytes.size, address, 1234)

            socket.send(packet)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
