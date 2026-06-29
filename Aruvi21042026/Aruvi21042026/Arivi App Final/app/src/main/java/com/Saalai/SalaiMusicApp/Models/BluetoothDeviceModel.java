package com.Saalai.SalaiMusicApp.Models;


import android.bluetooth.BluetoothDevice;

public class BluetoothDeviceModel {
    private String name;
    private String address;
    private BluetoothDevice device;
    private boolean isConnected;
    private int bondState;
    private int rssi; // Signal strength

    public BluetoothDeviceModel(String name, String address, BluetoothDevice device) {
        this.name = name;
        this.address = address;
        this.device = device;
        this.isConnected = false;
        if (device != null) {
            this.bondState = device.getBondState();
        }
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public BluetoothDevice getDevice() { return device; }
    public void setDevice(BluetoothDevice device) { this.device = device; }

    public boolean isConnected() { return isConnected; }
    public void setConnected(boolean connected) { isConnected = connected; }

    public int getBondState() { return bondState; }
    public void setBondState(int bondState) { this.bondState = bondState; }

    public int getRssi() { return rssi; }
    public void setRssi(int rssi) { this.rssi = rssi; }

    public boolean isBonded() {
        return bondState == BluetoothDevice.BOND_BONDED;
    }
}