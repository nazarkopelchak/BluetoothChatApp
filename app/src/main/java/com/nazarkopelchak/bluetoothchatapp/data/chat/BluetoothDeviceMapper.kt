package com.nazarkopelchak.bluetoothchatapp.data.chat

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.nazarkopelchak.bluetoothchatapp.domain.chat.BluetoothChatDevice

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothChatDevice(): BluetoothChatDevice {

    return BluetoothChatDevice(
        name = name,
        address = address
    )
}