package com.nazarkopelchak.bluetoothchatapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nazarkopelchak.bluetoothchatapp.domain.chat.BluetoothChatDevice
import com.nazarkopelchak.bluetoothchatapp.domain.chat.BluetoothController
import com.nazarkopelchak.bluetoothchatapp.domain.chat.ConnectionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BluetoothChatViewModel @Inject constructor(
    private val bluetoothController: BluetoothController
): ViewModel() {

    private var deviceConnectionJob: Job? = null

    private val _state = MutableStateFlow(BluetoothUiState())
    val state = combine(
        bluetoothController.scannedDevices,
        bluetoothController.pairedDevices,
        _state
    ) { scannedDevices, pairedDevices, state ->
        state.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices,
            chatMessages = if (state.isConnected) state.chatMessages else emptyList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    init {
        bluetoothController.isConnected.onEach { isConnected ->
            println(isConnected)
            _state.update { it.copy(isConnected = isConnected) }
        }.launchIn(viewModelScope)

        bluetoothController.errors.onEach { error ->
            _state.update { it.copy(errorMessage = error) }
        }.launchIn(viewModelScope)
    }

    fun connectToDevice(device: BluetoothChatDevice) {
        _state.update { it.copy(isConnecting = true) }
        deviceConnectionJob = bluetoothController
            .connectToDevice(device)
            .listen()
    }

    fun disconnectFromDevice() {
        deviceConnectionJob?.cancel()
        bluetoothController.closeConnection()
        _state.update { it.copy(
            isConnecting = false,
            isConnected = false
        ) }
    }

    fun waitForIncomingConnections(bluetoothLauncher: () -> Unit) {
        bluetoothLauncher()
        _state.update { it.copy(
            isConnecting = true
        ) }
        deviceConnectionJob = bluetoothController
            .startBluetoothServer()
            .listen()
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            val bluetoothMessage = bluetoothController.trySendMessage(message)
            if (bluetoothMessage != null) {
                _state.update {
                    it.copy(
                        chatMessages = it.chatMessages + bluetoothMessage
                    )
                }
            }
        }
    }

    fun startScan() {
        bluetoothController.startSearch()
    }

    fun stopScan() {
        bluetoothController.stopSearch()
    }

    private fun Flow<ConnectionResult>.listen(): Job {
        return onEach { result ->
            when (result) {
                is ConnectionResult.ConnectionEstablished -> {
                    _state.update {
                        it.copy(
                            isConnected = true,
                            isConnecting = false,
                            errorMessage = null
                        )}
                }
                is ConnectionResult.Error -> {
                    _state.update { it.copy(
                        isConnected = false,
                        isConnecting = false,
                        errorMessage = result.message
                    ) }
                }

                is ConnectionResult.TransferSucceeded -> {
                    _state.update { it.copy(
                        chatMessages = it.chatMessages + result.message
                    ) }
                }
            }
        }
            .catch {
                bluetoothController.closeConnection()
                _state.update { it.copy(
                    isConnected = false,
                    isConnecting = false
                ) }
            }.launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }
}