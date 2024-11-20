package com.nazarkopelchak.bluetoothchatapp.presentation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.nazarkopelchak.bluetoothchatapp.presentation.compontents.ChatScreen
import com.nazarkopelchak.bluetoothchatapp.presentation.compontents.DeviceScreen
import com.nazarkopelchak.bluetoothchatapp.ui.theme.BluetoothChatAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val bluetoothManager by lazy {
        getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }
    private val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val enableBluetoothLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // Can be used to check if the user enabled the permissions after showing the dialog
        }
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        //intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        //enableBluetoothLauncher.launch(intent)

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            val canEnableBluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                perms[Manifest.permission.BLUETOOTH_CONNECT] == true
            } else true

            if (canEnableBluetooth && !isBluetoothEnabled) {
                enableBluetoothLauncher.launch(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                )
            )
        }

        enableEdgeToEdge()
        setContent {
            BluetoothChatAppTheme {
                val viewModel = hiltViewModel<BluetoothChatViewModel>()
                val state by viewModel.state.collectAsState()

                LaunchedEffect(key1 = state.errorMessage) {
                    state.errorMessage?.let {
                        Toast.makeText(
                            applicationContext,
                            it,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                LaunchedEffect(key1 = state.isConnected) {
                    Toast.makeText(
                        applicationContext,
                        "You are connected!",
                        Toast.LENGTH_LONG
                    ).show()
                }

                Scaffold(
                    modifier = Modifier
                ) { innerPadding ->
                    when {
                        state.isConnecting -> {
                            Column (
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Text(text = "Connecting...")
                            }
                        }
                        state.isConnected -> {
                            ChatScreen(
                                state = state,
                                onDisconnect = viewModel::disconnectFromDevice,
                                onSendMessage = viewModel::sendMessage,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        else -> {
                            DeviceScreen(
                                state = state,
                                onStartScan = viewModel::startScan,
                                onStopScan = viewModel::stopScan,
                                onDeviceClick = viewModel::connectToDevice,
                                onStartServerClick = { viewModel.waitForIncomingConnections { enableBluetoothLauncher.launch(intent) }},
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}