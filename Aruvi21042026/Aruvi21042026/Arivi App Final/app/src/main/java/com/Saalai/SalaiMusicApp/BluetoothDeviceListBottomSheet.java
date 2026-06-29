package com.Saalai.SalaiMusicApp;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Adapters.BluetoothDeviceAdapter;
import com.Saalai.SalaiMusicApp.Models.BluetoothDeviceModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothDeviceListBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "BluetoothDeviceList";
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1001;
    private static final long SCAN_PERIOD = 10000; // 10 seconds

    private RecyclerView recyclerView;
    private BluetoothDeviceAdapter adapter;
    private List<BluetoothDeviceModel> deviceList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvNoDevices, tvScanning, tvTitle;
    private ImageView btnClose, btnRefresh;
    private Button btnEnableBluetooth;
    private View rootView;

    private BluetoothAdapter bluetoothAdapter;
    private AudioManager audioManager;
    private Handler handler = new Handler();
    private boolean isScanning = false;
    private String connectedDeviceAddress = "";

    // Callback interface
    public interface BluetoothConnectionListener {
        void onDeviceConnected(String deviceName);
        void onDeviceDisconnected();
    }

    private BluetoothConnectionListener connectionListener;

    public void setConnectionListener(BluetoothConnectionListener listener) {
        this.connectionListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.FullExpandedBottomSheet);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.bottom_sheet_bluetooth_devices, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupBluetooth();
        checkPermissionsAndScan();
        applyEdgeToEdge();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewBluetooth);
        progressBar = view.findViewById(R.id.progressBar);
        tvNoDevices = view.findViewById(R.id.tvNoDevices);
        tvScanning = view.findViewById(R.id.tvScanning);
        tvTitle = view.findViewById(R.id.tvTitle);
        btnClose = view.findViewById(R.id.btnClose);
        btnRefresh = view.findViewById(R.id.btnRefresh);
        btnEnableBluetooth = view.findViewById(R.id.btnEnableBluetooth);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BluetoothDeviceAdapter(requireContext(), deviceList, device -> {
            connectToDevice(device);
        });
        recyclerView.setAdapter(adapter);

        btnClose.setOnClickListener(v -> dismiss());
        btnRefresh.setOnClickListener(v -> {
            startScan();
            showToast("Scanning for devices...");
        });
        btnEnableBluetooth.setOnClickListener(v -> enableBluetooth());
    }

    private void applyEdgeToEdge() {
        if (getDialog() != null && getDialog().getWindow() != null) {
            Window window = getDialog().getWindow();

            WindowCompat.setDecorFitsSystemWindows(window, false);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(requireContext(), android.R.color.transparent));
            window.setNavigationBarColor(ContextCompat.getColor(requireContext(), android.R.color.transparent));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int flags = window.getDecorView().getSystemUiVisibility();
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                window.getDecorView().setSystemUiVisibility(flags);
            }

            if (rootView != null) {
                ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                    int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                    int navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

                    v.setPadding(
                            v.getPaddingLeft(),
                            statusBarHeight,
                            v.getPaddingRight(),
                            navigationBarHeight
                    );

                    return WindowInsetsCompat.CONSUMED;
                });
            }
        }
    }

    private void setupBluetooth() {
        try {
            // Initialize BluetoothAdapter
            BluetoothManager bluetoothManager = (BluetoothManager) requireContext()
                    .getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                bluetoothAdapter = bluetoothManager.getAdapter();
            }

            // Initialize AudioManager
            audioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);

            // Get currently connected device
            checkConnectedDevice();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Bluetooth: " + e.getMessage());
        }
    }

    private void checkConnectedDevice() {
        if (bluetoothAdapter == null || !hasBluetoothPermissions()) return;

        try {
            // Method 1: Check via AudioManager (most reliable)
            if (audioManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.media.AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
                for (android.media.AudioDeviceInfo device : devices) {
                    int type = device.getType();
                    if (type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {

                        // Try to get the connected device address
                        String address = getDeviceAddress(device);
                        if (address != null) {
                            connectedDeviceAddress = address;
                            Log.d(TAG, "Connected device found via AudioManager: " + address);
                            return;
                        }
                    }
                }
            }

            // Method 2: Check via BluetoothManager with error handling
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                BluetoothManager bluetoothManager = (BluetoothManager) requireContext()
                        .getSystemService(Context.BLUETOOTH_SERVICE);

                if (bluetoothManager != null) {
                    // Try A2DP profile first
                    try {
                        List<BluetoothDevice> a2dpDevices = bluetoothManager
                                .getConnectedDevices(BluetoothProfile.A2DP);
                        if (a2dpDevices != null && !a2dpDevices.isEmpty()) {
                            connectedDeviceAddress = a2dpDevices.get(0).getAddress();
                            Log.d(TAG, "Connected device found via A2DP: " + connectedDeviceAddress);
                            return;
                        }
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "A2DP profile not supported: " + e.getMessage());
                    }

                    // Try HEADSET profile as fallback
                    try {
                        List<BluetoothDevice> headsetDevices = bluetoothManager
                                .getConnectedDevices(BluetoothProfile.HEADSET);
                        if (headsetDevices != null && !headsetDevices.isEmpty()) {
                            connectedDeviceAddress = headsetDevices.get(0).getAddress();
                            Log.d(TAG, "Connected device found via HEADSET: " + connectedDeviceAddress);
                            return;
                        }
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "HEADSET profile not supported: " + e.getMessage());
                    }
                }
            }

            // Method 3: Check via simple A2DP state
            if (audioManager != null && audioManager.isBluetoothA2dpOn()) {
                Log.d(TAG, "Bluetooth A2DP is on, but couldn't get device address");
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception checking connected device: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error checking connected device: " + e.getMessage());
        }
    }

    /**
     * Get device address from AudioDeviceInfo using reflection
     */
    private String getDeviceAddress(android.media.AudioDeviceInfo device) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Method method = device.getClass().getMethod("getAddress");
                Object result = method.invoke(device);
                if (result instanceof String) {
                    return (String) result;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting device address via reflection: " + e.getMessage());
        }
        return null;
    }

    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void checkPermissionsAndScan() {
        if (bluetoothAdapter == null) {
            tvNoDevices.setText("Bluetooth not supported");
            tvNoDevices.setVisibility(View.VISIBLE);
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            showBluetoothDisabledUI();
            return;
        }

        // Check and request permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, REQUEST_BLUETOOTH_PERMISSIONS);
                return;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, REQUEST_BLUETOOTH_PERMISSIONS);
                return;
            }
        }

        // Permissions granted, start scanning
        loadPairedDevices();
        registerReceivers();
        startScan();
    }

    private void showBluetoothDisabledUI() {
        progressBar.setVisibility(View.GONE);
        tvScanning.setVisibility(View.GONE);
        btnEnableBluetooth.setVisibility(View.VISIBLE);
        tvNoDevices.setVisibility(View.VISIBLE);
        tvNoDevices.setText("Bluetooth is disabled");
    }

    private void enableBluetooth() {
        if (bluetoothAdapter != null) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_BLUETOOTH_PERMISSIONS);
        } else {
            // Open Bluetooth settings as fallback
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (resultCode == RESULT_OK) {
                // Bluetooth enabled, start scanning
                btnEnableBluetooth.setVisibility(View.GONE);
                checkPermissionsAndScan();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                checkPermissionsAndScan();
            } else {
                Toast.makeText(getContext(), "Bluetooth permissions required",
                        Toast.LENGTH_SHORT).show();
                dismiss();
            }
        }
    }

    private void loadPairedDevices() {
        if (bluetoothAdapter == null) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            deviceList.clear();

            for (BluetoothDevice device : pairedDevices) {
                String name = getDeviceFriendlyName(device);
                BluetoothDeviceModel model = new BluetoothDeviceModel(name,
                        device.getAddress(), device);
                deviceList.add(model);
            }

            adapter.updateDeviceList(deviceList);
            adapter.setConnectedDeviceAddress(connectedDeviceAddress);

            if (deviceList.isEmpty()) {
                tvNoDevices.setVisibility(View.VISIBLE);
            } else {
                tvNoDevices.setVisibility(View.GONE);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception loading paired devices: " + e.getMessage());
        }
    }

    private String getDeviceFriendlyName(BluetoothDevice device) {
        try {
            String name = device.getName();
            if (name != null && !name.isEmpty() && !name.matches("^[0-9]+$")) {
                return name;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                String alias = device.getAlias();
                if (alias != null && !alias.isEmpty() && !alias.matches("^[0-9]+$")) {
                    return alias;
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting device name: " + e.getMessage());
        }
        return "Unknown Device";
    }

    private void registerReceivers() {
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireContext().registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                requireContext().registerReceiver(bluetoothReceiver, filter);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error registering receiver: " + e.getMessage());
        }
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            try {
                switch (action) {
                    case BluetoothDevice.ACTION_FOUND:
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);

                        if (device != null) {
                            addOrUpdateDevice(device, rssi);
                        }
                        break;

                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        isScanning = true;
                        tvScanning.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.VISIBLE);
                        break;

                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        isScanning = false;
                        tvScanning.setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                        break;

                    case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                        BluetoothDevice bondDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                        updateDeviceBondState(bondDevice, bondState);
                        break;

                    case BluetoothDevice.ACTION_ACL_CONNECTED:
                        BluetoothDevice connectedDev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        handleDeviceConnected(connectedDev);
                        break;

                    case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                        BluetoothDevice disconnectedDev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        handleDeviceDisconnected(disconnectedDev);
                        break;

                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        handleBluetoothStateChange(state);
                        break;
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception in receiver: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Error in Bluetooth receiver: " + e.getMessage());
            }
        }
    };

    private void addOrUpdateDevice(BluetoothDevice device, int rssi) {
        try {
            String address = device.getAddress();
            String name = getDeviceFriendlyName(device);

            // Check if device already exists
            for (BluetoothDeviceModel model : deviceList) {
                if (model.getAddress().equals(address)) {
                    model.setRssi(rssi);
                    adapter.updateDeviceRssi(address, rssi);
                    return;
                }
            }

            // Add new device
            BluetoothDeviceModel newModel = new BluetoothDeviceModel(name, address, device);
            newModel.setRssi(rssi);
            deviceList.add(newModel);
            adapter.updateDeviceList(deviceList);

            if (tvNoDevices.getVisibility() == View.VISIBLE) {
                tvNoDevices.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding/updating device: " + e.getMessage());
        }
    }

    private void updateDeviceBondState(BluetoothDevice device, int bondState) {
        if (device == null) return;

        for (BluetoothDeviceModel model : deviceList) {
            if (model.getAddress().equals(device.getAddress())) {
                model.setBondState(bondState);
                break;
            }
        }
        adapter.updateDeviceList(deviceList);
    }

    private void handleDeviceConnected(BluetoothDevice device) {
        if (device == null) return;

        connectedDeviceAddress = device.getAddress();
        String deviceName = getDeviceFriendlyName(device);

        adapter.setConnectedDeviceAddress(connectedDeviceAddress);

        if (connectionListener != null) {
            connectionListener.onDeviceConnected(deviceName);
        }

        // Update PlayerBottomSheetFragment
        try {
            Intent intent = new Intent("BLUETOOTH_DEVICE_CONNECTED");
            intent.putExtra("device_name", deviceName);
            requireContext().sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error sending broadcast: " + e.getMessage());
        }

        showToast("Connected to " + deviceName);
        dismiss();
    }

    private void handleDeviceDisconnected(BluetoothDevice device) {
        if (device != null && device.getAddress().equals(connectedDeviceAddress)) {
            connectedDeviceAddress = "";
            adapter.setConnectedDeviceAddress("");

            if (connectionListener != null) {
                connectionListener.onDeviceDisconnected();
            }

            // Update PlayerBottomSheetFragment
            try {
                Intent intent = new Intent("BLUETOOTH_DEVICE_DISCONNECTED");
                requireContext().sendBroadcast(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error sending broadcast: " + e.getMessage());
            }

            showToast("Device disconnected");
        }
    }

    private void handleBluetoothStateChange(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_ON:
                btnEnableBluetooth.setVisibility(View.GONE);
                startScan();
                break;
            case BluetoothAdapter.STATE_OFF:
                showBluetoothDisabledUI();
                break;
        }
    }

    private void startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) return;

        try {
            // Check permissions for Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }

            // Cancel any ongoing scan
            if (isScanning) {
                bluetoothAdapter.cancelDiscovery();
            }

            // Clear non-paired devices but keep paired ones
            List<BluetoothDeviceModel> pairedDevices = new ArrayList<>();
            for (BluetoothDeviceModel model : deviceList) {
                if (model.isBonded()) {
                    pairedDevices.add(model);
                }
            }
            deviceList.clear();
            deviceList.addAll(pairedDevices);
            adapter.updateDeviceList(deviceList);

            // Start new scan
            bluetoothAdapter.startDiscovery();

            // Stop scan after SCAN_PERIOD
            handler.postDelayed(() -> {
                if (isScanning && bluetoothAdapter != null) {
                    try {
                        bluetoothAdapter.cancelDiscovery();
                    } catch (SecurityException e) {
                        Log.e(TAG, "Security exception stopping scan: " + e.getMessage());
                    }
                }
            }, SCAN_PERIOD);

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception starting scan: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error starting scan: " + e.getMessage());
        }
    }

    private void connectToDevice(BluetoothDeviceModel deviceModel) {
        BluetoothDevice device = deviceModel.getDevice();
        if (device == null) return;

        try {
            // For Android 12+, need BLUETOOTH_CONNECT permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                            REQUEST_BLUETOOTH_PERMISSIONS);
                    return;
                }
            }

            // If device is already connected, just dismiss
            if (deviceModel.getAddress().equals(connectedDeviceAddress)) {
                showToast("Already connected to " + deviceModel.getName());
                dismiss();
                return;
            }

            // Show connecting message
            showToast("Connecting to " + deviceModel.getName() + "...");

            // Try to connect via A2DP
            boolean connected = connectA2DP(device);

            if (!connected) {
                // If A2DP connection fails, open Bluetooth settings for manual connection
                showToast("Please connect manually in Bluetooth settings");
                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(intent);
            }

            // Also try to pair if not already bonded
            if (!deviceModel.isBonded()) {
                createBond(device);
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception connecting: " + e.getMessage());
            showToast("Permission denied");
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to device: " + e.getMessage());
            showToast("Connection failed");
        }
    }

    private boolean connectA2DP(BluetoothDevice device) {
        try {
            // For Android 8.0+, we can try to use the profile proxy
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                BluetoothManager bluetoothManager = (BluetoothManager) requireContext()
                        .getSystemService(Context.BLUETOOTH_SERVICE);

                if (bluetoothManager != null) {
                    // Try to connect via reflection as a fallback
                    try {
                        // Method 1: Using connect() method via reflection
                        Method connectMethod = device.getClass().getMethod("connect");
                        if (connectMethod != null) {
                            boolean result = (boolean) connectMethod.invoke(device);
                            if (result) {
                                Log.d(TAG, "Connection initiated successfully");
                                return true;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Reflection connect failed: " + e.getMessage());
                    }

                    // Method 2: Try to connect via createBond first if not bonded
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        try {
                            Method bondMethod = device.getClass().getMethod("createBond");
                            bondMethod.invoke(device);
                        } catch (Exception e) {
                            Log.e(TAG, "Bond creation failed: " + e.getMessage());
                        }
                    }
                }
            }

            // For all versions, we can't directly connect programmatically on most devices
            // due to Android security restrictions. The best we can do is open settings.
            return false;

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception A2DP: " + e.getMessage());
            return false;
        }
    }

    private void createBond(BluetoothDevice device) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }

                Method method = device.getClass().getMethod("createBond");
                method.invoke(device);
                Log.d(TAG, "Bond creation initiated");
            }
        } catch (Exception e) {
            Log.e(TAG, "Bond creation failed: " + e.getMessage());
        }
    }

    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            requireContext().unregisterReceiver(bluetoothReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver not registered: " + e.getMessage());
        }

        // Stop scanning
        if (bluetoothAdapter != null && isScanning) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                        bluetoothAdapter.cancelDiscovery();
                    }
                } else {
                    bluetoothAdapter.cancelDiscovery();
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception stopping scan: " + e.getMessage());
            }
        }

        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onStart() {
        super.onStart();
        final View view = getView();
        if (view != null) {
            view.post(() -> {
                View parent = (View) view.getParent();
                if (parent != null) {
                    BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(parent);
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    behavior.setSkipCollapsed(true);
                    behavior.setDraggable(true);
                    ViewGroup.LayoutParams params = parent.getLayoutParams();
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    parent.setLayoutParams(params);
                }
            });
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dlg -> {
            BottomSheetDialog d = (BottomSheetDialog) dlg;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                behavior.setDraggable(true);
                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            }
        });
        return dialog;
    }

    @Override
    public int getTheme() {
        return R.style.FullExpandedBottomSheet;
    }
}