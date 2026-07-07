import 'dart:async';
import 'package:flutter/material.dart';
import '../services/bluetooth_service.dart';

class BluetoothDevicesSheet extends StatefulWidget {
  const BluetoothDevicesSheet({super.key});

  @override
  State<BluetoothDevicesSheet> createState() => _BluetoothDevicesSheetState();
}

class _BluetoothDevicesSheetState extends State<BluetoothDevicesSheet> {
  final BluetoothService _bluetoothService = BluetoothService();
  bool _isLoading = true;
  List<Map<String, String>> _pairedDevices = [];
  List<Map<String, String>> _scannedDevices = [];
  String? _connectedDeviceName;
  Timer? _scanTimer;

  @override
  void initState() {
    super.initState();
    _loadDevices();
  }

  @override
  void dispose() {
    _scanTimer?.cancel();
    super.dispose();
  }

  Future<void> _loadDevices() async {
    setState(() => _isLoading = true);
    
    final connected = await _bluetoothService.getConnectedDevice();
    final paired = await _bluetoothService.getPairedDevices();
    
    // Start scanning for available devices
    await _bluetoothService.startScan();
    
    setState(() {
      _connectedDeviceName = connected;
      _pairedDevices = paired;
      _isLoading = false;
    });

    // Poll for scanned devices every 2 seconds
    _scanTimer?.cancel();
    _scanTimer = Timer.periodic(const Duration(seconds: 2), (timer) async {
      final scanned = await _bluetoothService.getScannedDevices();
      if (mounted) {
        setState(() {
          // Filter out devices that are already in the paired list
          _scannedDevices = scanned.where((s) => !_pairedDevices.any((p) => p['address'] == s['address'])).toList();
        });
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      height: MediaQuery.of(context).size.height * 0.7,
      decoration: const BoxDecoration(
        color: Color(0xFF1E1E24), // Dark background matching the screenshot
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Row(
              children: [
                IconButton(
                  icon: const Icon(Icons.close, color: Colors.white),
                  onPressed: () => Navigator.pop(context),
                ),
                const Expanded(
                  child: Text(
                    'Bluetooth Devices',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.refresh, color: Colors.white),
                  onPressed: _loadDevices,
                ),
              ],
            ),
          ),
          
          if (_isLoading)
            const Padding(
              padding: EdgeInsets.all(16.0),
              child: Text(
                'Scanning for devices...',
                style: TextStyle(color: Colors.white70, fontSize: 14),
              ),
            )
          else ...[
            Expanded(
              child: ListView(
                children: [
                  if (_pairedDevices.isNotEmpty) ...[
                    const Padding(
                      padding: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                      child: Text(
                        'Paired Devices',
                        style: TextStyle(color: Colors.white54, fontSize: 12, fontWeight: FontWeight.bold),
                      ),
                    ),
                    ..._pairedDevices.map((device) {
                      final isConnected = device['name'] == _connectedDeviceName;
                      return _buildDeviceTile(device, isConnected: isConnected, isPaired: true);
                    }).toList(),
                  ],
                  if (_scannedDevices.isNotEmpty) ...[
                    const Padding(
                      padding: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                      child: Text(
                        'Available Devices',
                        style: TextStyle(color: Colors.white54, fontSize: 12, fontWeight: FontWeight.bold),
                      ),
                    ),
                    ..._scannedDevices.map((device) {
                      return _buildDeviceTile(device, isConnected: false, isPaired: false);
                    }).toList(),
                  ],
                  if (_pairedDevices.isEmpty && _scannedDevices.isEmpty)
                    const Padding(
                      padding: EdgeInsets.all(16.0),
                      child: Center(
                        child: Text(
                          'No devices found',
                          style: TextStyle(color: Colors.white54),
                        ),
                      ),
                    )
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildDeviceTile(Map<String, String> device, {required bool isConnected, required bool isPaired}) {
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      leading: Icon(
        isPaired ? Icons.headset : Icons.headset_mic_outlined,
        color: isPaired ? Colors.red : Colors.white54,
        size: 32,
      ),
      title: Text(
        device['name'] ?? 'Unknown Device',
        style: const TextStyle(
          color: Colors.white,
          fontSize: 16,
          fontWeight: FontWeight.w600,
        ),
      ),
      subtitle: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            device['address'] ?? '',
            style: const TextStyle(color: Colors.white54, fontSize: 12),
          ),
          Text(
            isConnected ? 'Connected' : (isPaired ? 'Paired' : 'Available'),
            style: TextStyle(
              color: isConnected ? Colors.green : (isPaired ? Colors.red : Colors.white54),
              fontSize: 12,
            ),
          ),
        ],
      ),
      onTap: () {
        Navigator.pop(context);
        _bluetoothService.openBluetoothSettings();
      },
    );
  }
}
