package com.example.aruviflutter

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import android.media.AudioManager
import android.media.AudioDeviceInfo

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.saalai.salaimusicapp/bluetooth"
    private val scannedDevices = mutableSetOf<Map<String, String>>()
    private var isReceiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device != null) {
                    try {
                        val deviceMap = mapOf(
                            "name" to (device.name ?: "Unknown Device"),
                            "address" to device.address
                        )
                        scannedDevices.add(deviceMap)
                    } catch (e: SecurityException) {
                        // Ignore
                    }
                }
            }
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "getConnectedDevice" -> {
                    result.success(getConnectedBluetoothDevice())
                }
                "getPairedDevices" -> {
                    result.success(getPairedBluetoothDevices())
                }
                "startScan" -> {
                    startBluetoothScan()
                    result.success(null)
                }
                "getScannedDevices" -> {
                    result.success(scannedDevices.toList())
                }
                "openBluetoothSettings" -> {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    startActivity(intent)
                    result.success(null)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    private fun startBluetoothScan() {
        try {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter
            if (adapter != null) {
                scannedDevices.clear()
                if (!isReceiverRegistered) {
                    val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                    registerReceiver(receiver, filter)
                    isReceiverRegistered = true
                }
                if (adapter.isDiscovering) {
                    adapter.cancelDiscovery()
                }
                adapter.startDiscovery()
            }
        } catch (e: SecurityException) {
            // Missing permissions
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            unregisterReceiver(receiver)
            isReceiverRegistered = false
        }
    }

    private fun getConnectedBluetoothDevice(): Map<String, String>? {
        // Try BluetoothManager first
        try {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val a2dpDevices = bluetoothManager.getConnectedDevices(android.bluetooth.BluetoothProfile.A2DP)
            if (a2dpDevices.isNotEmpty()) {
                return mapOf(
                    "name" to (a2dpDevices[0].name ?: "Unknown Device"),
                    "address" to a2dpDevices[0].address
                )
            }
            val headsetDevices = bluetoothManager.getConnectedDevices(android.bluetooth.BluetoothProfile.HEADSET)
            if (headsetDevices.isNotEmpty()) {
                return mapOf(
                    "name" to (headsetDevices[0].name ?: "Unknown Device"),
                    "address" to headsetDevices[0].address
                )
            }
        } catch (e: Exception) {
            // Ignore
        }

        // Fallback to AudioManager
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in devices) {
                if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    device.type == AudioDeviceInfo.TYPE_BLE_HEADSET) {
                    
                    var address = ""
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        try {
                            val method = device.javaClass.getMethod("getAddress")
                            val result = method.invoke(device)
                            if (result is String) {
                                address = result
                            }
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                    return mapOf(
                        "name" to device.productName.toString(),
                        "address" to address
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore exceptions
        }
        return null
    }

    private fun getPairedBluetoothDevices(): List<Map<String, String>> {
        val pairedList = mutableListOf<Map<String, String>>()
        try {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter
            if (adapter != null) {
                val pairedDevices = adapter.bondedDevices
                for (device in pairedDevices) {
                    val deviceMap = mapOf(
                        "name" to (device.name ?: "Unknown Device"),
                        "address" to device.address
                    )
                    pairedList.add(deviceMap)
                }
            }
        } catch (e: SecurityException) {
            // Missing permissions, return what we can or empty
        } catch (e: Exception) {
            // Ignore other exceptions
        }
        return pairedList
    }
}
