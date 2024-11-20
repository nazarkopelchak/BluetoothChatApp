package com.nazarkopelchak.bluetoothchatapp.presentation.compontents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nazarkopelchak.bluetoothchatapp.domain.chat.BluetoothChatDevice
import com.nazarkopelchak.bluetoothchatapp.presentation.BluetoothUiState

@Composable
fun DeviceScreen(
    state: BluetoothUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceClick: (BluetoothChatDevice) -> Unit,
    onStartServerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        BluetoothDeviceList(
            pairedDevices = state.pairedDevices,
            scannedDevices = state.scannedDevices,
            onClick = onDeviceClick,
            modifier = Modifier.fillMaxWidth()
                .weight(1f)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {

            Button(
                onClick = onStartScan
            ) {
                Text(text = "Start Scan")
            }
            Button(
                onClick = onStopScan
            ) {
                Text(text = "Stop Scan")
            }
            Button(
                onClick = onStartServerClick
            ) {
                Text(text = "Start Server")
            }
        }
    }
}

@Composable
fun BluetoothDeviceList(
    pairedDevices: List<BluetoothChatDevice>,
    scannedDevices: List<BluetoothChatDevice>,
    onClick: (BluetoothChatDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
    ) {
        item {
            Text(
                text = "Paired Devices:",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(16.dp)
            )
        }

        items(pairedDevices) { pairedDevice ->
            Text(
                text = pairedDevice.name ?: "(No Name)",
                modifier = Modifier.fillMaxWidth()
                    .padding(16.dp)
                    .clickable {
                        onClick(pairedDevice)
                    }
                )
        }
        item {
            Text(
                text = "Scanned Devices:",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(16.dp)
            )
        }

        items(scannedDevices) { scannedDevice ->
            Text(
                text = scannedDevice.name ?: "(No Name)",
                modifier = Modifier.fillMaxWidth()
                    .padding(16.dp)
                    .clickable {
                        onClick(scannedDevice)
                    }
            )
        }
    }
}