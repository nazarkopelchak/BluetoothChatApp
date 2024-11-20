package com.nazarkopelchak.bluetoothchatapp.data.chat

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import com.nazarkopelchak.bluetoothchatapp.domain.chat.BluetoothChatDevice
import com.nazarkopelchak.bluetoothchatapp.domain.chat.BluetoothController
import com.nazarkopelchak.bluetoothchatapp.domain.chat.BluetoothMessage
import com.nazarkopelchak.bluetoothchatapp.domain.chat.ConnectionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

const val TAG = "BLUETOOTH_CONTROLLER"

class AndroidBluetoothController(
    private val context: Context
): BluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private var dataTransferService: BluetoothDataTransferService? = null

    private val _scannedDevices = MutableStateFlow<List<BluetoothChatDevice>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothChatDevice>>
        get() =_scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothChatDevice>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothChatDevice>>
        get() = _pairedDevices.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    private val foundDeviceReceiver = FoundDeviceReceiver { device ->
        _scannedDevices.update { devices ->
            val newDevice = device.toBluetoothChatDevice()
            if (newDevice in devices) devices else devices + newDevice
        }
    }

    @SuppressLint("MissingPermission")
    private val bluetoothStateReceiver = BluetoothStateReceiver{ isConnected, bluetoothDevice ->
        // Permission is checked in updatePairedDevices
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            if (bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == true) {
                println("HERE")
                _isConnected.update { isConnected }
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    _errors.emit("Can't connect to a non-paired device")
                }
            }
        }
        else {
            println("Failed")
        }
    }

    private var currentServerSocket: BluetoothServerSocket? = null
    private var currentClientSocket: BluetoothSocket? = null

    init {
        updatePairedDevices()
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
            )
    }

    override fun startSearch() {
        if (!checkPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }

        // TRYIT try moving it to init block
        context.registerReceiver(
            foundDeviceReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )

        updatePairedDevices()
        bluetoothAdapter?.startDiscovery()
    }

    override fun stopSearch() {
        if (!checkPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }

        bluetoothAdapter?.cancelDiscovery()
    }

    override fun startBluetoothServer(): Flow<ConnectionResult> {
        return flow {
            if (!checkPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                throw SecurityException("Bluetooth permission has not been granted")
            }

            currentServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                "Bluetooth Chat Server",
                UUID.fromString(SERVER_UUID)
            )

            var isLoopActive = true
            while (isLoopActive) {
                currentClientSocket = try {
                    //emit(ConnectionResult.ConnectionEstablished)
                    currentServerSocket?.accept()
                } catch (e: IOException) {
                    isLoopActive = false
                    //emit(ConnectionResult.Error("Could not run the server"))
                    null
                }
                emit(ConnectionResult.ConnectionEstablished)
                println("HERE2")
                currentClientSocket?.let { bluetoothSocket ->
                    currentServerSocket?.close()
                    val service = BluetoothDataTransferService(bluetoothSocket)
                    dataTransferService = service

                    emitAll(
                        service.listenForIncomingMessages()
                            .map {
                                ConnectionResult.TransferSucceeded(it)
                            }
                    )

                }
            }
        }.onCompletion {
            Log.d(TAG, "COmpleted")
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override fun connectToDevice(device: BluetoothChatDevice): Flow<ConnectionResult> {
        return flow {
            if (!checkPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                throw SecurityException("Bluetooth permission has not been granted")
            }

            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)

            currentClientSocket = bluetoothDevice
                ?.createRfcommSocketToServiceRecord(UUID.fromString(SERVER_UUID))

            stopSearch()

            currentClientSocket?.let { socket ->
                try {
                    socket.connect()
                    emit(ConnectionResult.ConnectionEstablished)

                    BluetoothDataTransferService(socket).also { transferService ->
                        dataTransferService = transferService
                        emitAll(
                            transferService.listenForIncomingMessages()
                                .map {
                                    ConnectionResult.TransferSucceeded(it)
                                }
                        )
                    }
                } catch (e: IOException) {
                    socket.close()
                    currentClientSocket = null
                    emit(ConnectionResult.Error("Connection was interrupted"))
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun trySendMessage(message: String): BluetoothMessage? {
        if (!checkPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            return null
        }

        if (dataTransferService == null) {
            return null
        }

        val bluetoothMessage = BluetoothMessage(
            message = message,
            senderName = bluetoothAdapter?.name ?: "Unknown name",
            isFromLocalUser = true
        )

        dataTransferService?.sendMessage(bluetoothMessage.toByteArray())

        return bluetoothMessage
    }

    override fun closeConnection() {
        currentClientSocket?.close()
        currentServerSocket?.close()
        currentClientSocket = null
        currentServerSocket = null
    }

    // TRYIT check when this function is called
    override fun release() {
        context.unregisterReceiver(foundDeviceReceiver)
        context.unregisterReceiver(bluetoothStateReceiver)
        closeConnection()
    }

    private fun updatePairedDevices() {
        if (!checkPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        bluetoothAdapter?.bondedDevices
            ?.map { it.toBluetoothChatDevice() }
            ?.also { devices ->
                _pairedDevices.update { devices }
            }
    }

    private fun checkPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val SERVER_UUID = "351953b1-be93-4240-8519-648cfa5b7553"
    }
}