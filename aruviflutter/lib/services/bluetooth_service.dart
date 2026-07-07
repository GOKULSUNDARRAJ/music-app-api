import 'package:flutter/services.dart';

class BluetoothService {
  static const MethodChannel _channel = MethodChannel('com.saalai.salaimusicapp/bluetooth');

  Future<Map<String, String>?> getConnectedDevice() async {
    try {
      final dynamic result = await _channel.invokeMethod('getConnectedDevice');
      if (result != null) {
        return Map<String, String>.from(result as Map);
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  Future<List<Map<String, String>>> getPairedDevices() async {
    try {
      final List<dynamic> devices = await _channel.invokeMethod('getPairedDevices');
      return devices.map((e) => Map<String, String>.from(e as Map)).toList();
    } catch (e) {
      return [];
    }
  }

  Future<void> startScan() async {
    try {
      await _channel.invokeMethod('startScan');
    } catch (e) {
      // Ignore
    }
  }

  Future<List<Map<String, String>>> getScannedDevices() async {
    try {
      final List<dynamic> devices = await _channel.invokeMethod('getScannedDevices');
      return devices.map((e) => Map<String, String>.from(e as Map)).toList();
    } catch (e) {
      return [];
    }
  }

  Future<void> openBluetoothSettings() async {
    try {
      await _channel.invokeMethod('openBluetoothSettings');
    } catch (e) {
      // Ignore
    }
  }
}
