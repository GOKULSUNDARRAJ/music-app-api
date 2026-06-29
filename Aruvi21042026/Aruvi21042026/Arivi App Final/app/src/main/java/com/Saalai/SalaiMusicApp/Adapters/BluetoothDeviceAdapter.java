package com.Saalai.SalaiMusicApp.Adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Models.BluetoothDeviceModel;
import com.Saalai.SalaiMusicApp.R;

import java.util.List;

public class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceAdapter.ViewHolder> {

    private List<BluetoothDeviceModel> deviceList;
    private Context context;
    private OnDeviceClickListener listener;
    private String connectedDeviceAddress;

    public interface OnDeviceClickListener {
        void onDeviceClick(BluetoothDeviceModel device);
    }

    public BluetoothDeviceAdapter(Context context, List<BluetoothDeviceModel> deviceList,
                                  OnDeviceClickListener listener) {
        this.context = context;
        this.deviceList = deviceList;
        this.listener = listener;
    }

    public void setConnectedDeviceAddress(String address) {
        this.connectedDeviceAddress = address;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_bluetooth_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BluetoothDeviceModel device = deviceList.get(position);

        // Set device name or fallback to "Unknown Device"
        String deviceName = device.getName();
        if (deviceName == null || deviceName.isEmpty()) {
            deviceName = "Unknown Device";
        }
        holder.tvDeviceName.setText(deviceName);

        // Set device address
        holder.tvDeviceAddress.setText(device.getAddress());

        // Show signal strength indicator
        int rssi = device.getRssi();
        if (rssi != 0) {
            int signalStrength = getSignalStrength(rssi);
            holder.ivSignalStrength.setImageLevel(signalStrength);
        }

        // Check if this device is currently connected
        boolean isConnected = device.getAddress().equals(connectedDeviceAddress);
        device.setConnected(isConnected);

        if (isConnected) {
            holder.tvConnectionStatus.setText("Connected");
            holder.tvConnectionStatus.setTextColor(ContextCompat.getColor(context, R.color.green));
            holder.ivDeviceIcon.setImageResource(R.drawable.headphones1);
            // Green tint for connected devices
            holder.ivDeviceIcon.setColorFilter(ContextCompat.getColor(context, R.color.green), android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (device.isBonded()) {
            holder.tvConnectionStatus.setText("Paired");
            holder.tvConnectionStatus.setTextColor(ContextCompat.getColor(context, R.color.yellow));
            holder.ivDeviceIcon.setImageResource(R.drawable.headphones1);
            // Yellow tint for paired devices
            holder.ivDeviceIcon.setColorFilter(ContextCompat.getColor(context, R.color.yellow), android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            holder.tvConnectionStatus.setText("Available");
            holder.tvConnectionStatus.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.ivDeviceIcon.setImageResource(R.drawable.headphones1);
            // White tint for available devices
            holder.ivDeviceIcon.setColorFilter(ContextCompat.getColor(context, R.color.white), android.graphics.PorterDuff.Mode.SRC_IN);
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeviceClick(device);
            }
        });
    }

    private int getSignalStrength(int rssi) {
        if (rssi >= -50) return 3; // Strong
        else if (rssi >= -65) return 2; // Medium
        else if (rssi >= -85) return 1; // Weak
        else return 0; // Very weak
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public void updateDeviceList(List<BluetoothDeviceModel> newList) {
        this.deviceList = newList;
        notifyDataSetChanged();
    }

    public void updateDeviceRssi(String address, int rssi) {
        for (int i = 0; i < deviceList.size(); i++) {
            if (deviceList.get(i).getAddress().equals(address)) {
                deviceList.get(i).setRssi(rssi);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName, tvDeviceAddress, tvConnectionStatus;
        ImageView ivDeviceIcon, ivSignalStrength;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvDeviceAddress = itemView.findViewById(R.id.tvDeviceAddress);
            tvConnectionStatus = itemView.findViewById(R.id.tvConnectionStatus);
            ivDeviceIcon = itemView.findViewById(R.id.ivDeviceIcon);
            ivSignalStrength = itemView.findViewById(R.id.ivSignalStrength);
        }
    }
}