package com.mamad.peugeot405

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothClient(context: Context) {
    private val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var socket: BluetoothSocket? = null
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun pairedDevices(): List<BluetoothDevice> =
        runCatching { adapter?.bondedDevices?.toList() ?: emptyList() }.getOrDefault(emptyList())

    suspend fun connect(device: BluetoothDevice): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            disconnect()
            adapter.cancelDiscovery()
            val s = device.createRfcommSocketToServiceRecord(sppUuid)
            s.connect()
            socket = s
        }
    }

    suspend fun send(text: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val out = socket!!.outputStream
            out.write(text.toByteArray())
            out.flush()
        }
    }

    fun disconnect() {
        runCatching { socket?.close() }
        socket = null
    }
}
