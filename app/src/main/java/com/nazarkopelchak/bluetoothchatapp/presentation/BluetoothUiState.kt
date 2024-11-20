package com.nazarkopelchak.bluetoothchatapp.presentation

import com.nazarkopelchak.bluetoothchatapp.domain.chat.BluetoothChatDevice
import com.nazarkopelchak.bluetoothchatapp.domain.chat.BluetoothMessage

data class BluetoothUiState(
    val scannedDevices: List<BluetoothChatDevice> = emptyList(),
    val pairedDevices: List<BluetoothChatDevice> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val chatMessages: List<BluetoothMessage> = emptyList()
)
