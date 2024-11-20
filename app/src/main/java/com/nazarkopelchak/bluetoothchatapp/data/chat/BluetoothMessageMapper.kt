package com.nazarkopelchak.bluetoothchatapp.data.chat

import com.nazarkopelchak.bluetoothchatapp.domain.chat.BluetoothMessage

const val DELIMITER = "-##+"    // Cheap version of serialization

fun String.toBluetoothMessage(isFromLocalUser: Boolean): BluetoothMessage {
    val name = substringBeforeLast(DELIMITER)
    val message = substringAfter(DELIMITER)
    return BluetoothMessage(
        message = message,
        senderName = name,
        isFromLocalUser = isFromLocalUser
    )
}

fun BluetoothMessage.toByteArray(): ByteArray {
    return "$senderName$DELIMITER$message".toByteArray()
}