package com.nazarkopelchak.bluetoothchatapp.domain.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {

    val isConnected: StateFlow<Boolean>
    val scannedDevices: StateFlow<List<BluetoothChatDevice>>
    val pairedDevices: StateFlow<List<BluetoothChatDevice>>
    val errors: SharedFlow<String>

    fun startSearch()

    fun stopSearch()

    fun startBluetoothServer(): Flow<ConnectionResult>

    fun connectToDevice(device: BluetoothChatDevice): Flow<ConnectionResult>

    suspend fun trySendMessage(message: String): BluetoothMessage?

    fun closeConnection()

    fun release()
}