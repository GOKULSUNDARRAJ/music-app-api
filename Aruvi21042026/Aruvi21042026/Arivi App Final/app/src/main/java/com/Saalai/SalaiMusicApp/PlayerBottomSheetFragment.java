package com.Saalai.SalaiMusicApp;

import android.app.Dialog;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.squareup.picasso.Picasso;

import java.lang.reflect.Method;
import java.util.Set;

public class PlayerBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String TAG = "FullAudioPlayerBS";

    private TextView tvSongName, tvSongArtist, tvCurrentTime, tvDuration, tvBluetoothDeviceName;
    private ImageView btnPlayPause, btnNext, btnPrev, imgAlbumArt, close, menu, imgBluetoothIcon, btnTimer;
    private ImageView btnClipMode;
    private ProgressBar progressBar;
    private SeekBar seekBar;
    private LinearLayout bluetoothInfoLayout;

    private Handler handler = new Handler();
    private Runnable updateSeekBarRunnable;

    private View rootView;

    // Audio Manager for Bluetooth detection
    private AudioManager audioManager;

    // Track Bluetooth connection state and device name
    private boolean isBluetoothConnected = false;
    private String connectedBluetoothDeviceName = "";

    // Handler for delayed name fetching
    private Handler nameFetchHandler = new Handler(Looper.getMainLooper());

    // Clip Mode - Now using PlayerManager's instance
    private ClipModeController clipModeController;
    private boolean isClipModeActive = false;

    // Broadcast receiver for play/pause updates
    private final BroadcastReceiver playerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                Log.d(TAG, "Received broadcast: " + action);
                if ("UPDATE_MINI_PLAYER".equals(action) ||
                        "UPDATE_AUDIO_ADAPTER".equals(action)) {
                    safeUpdateUI();
                    updatePlayPauseButton();
                }
            }
        }
    };

    // Timer update receiver
    private final BroadcastReceiver timerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && "UPDATE_TIMER_STATUS".equals(action)) {
                updateTimerIcon();
            }
        }
    };

    // Clip Mode state receiver
    // Clip Mode state receiver
    private final BroadcastReceiver clipModeStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && "CLIP_MODE_STATE_CHANGED".equals(action)) {
                boolean isActive = intent.getBooleanExtra("isActive", false);
                boolean isCleared = intent.getBooleanExtra("isCleared", false);

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        updateClipModeUI(isActive);

                        if (isCleared) {
                            // Reset any clip mode related UI elements
                            seekBar.setSecondaryProgress(0);
                            tvCurrentTime.setText(formatTime((int)PlayerManager.getCurrentPosition()));

                            // Get fresh clip mode controller
                            clipModeController = PlayerManager.getClipModeController();
                            if (clipModeController != null) {
                                clipModeController.setMediaPlayer(PlayerManager.getPlayer());
                            }
                        }
                    });
                }
            }
        }
    };

    // Bluetooth state receiver - Gets actual device name
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            Log.d(TAG, "📡 Bluetooth Broadcast: " + action);

            switch (action) {
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    BluetoothDevice connectedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String connectedName = getBluetoothDeviceFriendlyName(connectedDevice);
                    Log.d(TAG, "✅ Bluetooth CONNECTED - Device: " + connectedName);
                    handleBluetoothConnected(connectedName);
                    break;

                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    BluetoothDevice disconnectedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String disconnectedName = getBluetoothDeviceFriendlyName(disconnectedDevice);
                    Log.d(TAG, "❌ Bluetooth DISCONNECTED - Device: " + disconnectedName);
                    handleBluetoothDisconnected();
                    break;

                case BluetoothDevice.ACTION_NAME_CHANGED:
                    BluetoothDevice nameChangedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String newName = getBluetoothDeviceFriendlyName(nameChangedDevice);
                    Log.d(TAG, "📝 Bluetooth name changed to: " + newName);
                    if (isBluetoothConnected && newName != null && !newName.isEmpty()) {
                        connectedBluetoothDeviceName = newName;
                        safeUpdateUI();
                    }
                    break;

                case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                    Log.d(TAG, "🔊 Audio becoming noisy");
                    boolean isNowConnected = isBluetoothDeviceConnected();
                    if (isBluetoothConnected && !isNowConnected) {
                        handleBluetoothDisconnected();
                    }
                    break;
            }
        }
    };

    // Bluetooth list receiver
    private final BroadcastReceiver bluetoothListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            if ("BLUETOOTH_DEVICE_CONNECTED".equals(action)) {
                String deviceName = intent.getStringExtra("device_name");
                handleBluetoothConnected(deviceName);
            } else if ("BLUETOOTH_DEVICE_DISCONNECTED".equals(action)) {
                handleBluetoothDisconnected();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottomsheet_audio_player, container, false);
        rootView = view;
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize AudioManager
        if (getContext() != null) {
            audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        }

        // Initialize views
        tvSongName = view.findViewById(R.id.tvFullPlayerSongName);
        tvSongArtist = view.findViewById(R.id.tvFullPlayerSongArtist);
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvDuration = view.findViewById(R.id.tvDuration);
        tvBluetoothDeviceName = view.findViewById(R.id.tvBluetoothDeviceName);
        bluetoothInfoLayout = view.findViewById(R.id.bluetoothInfoLayout);

        // Clip Mode button
        btnClipMode = view.findViewById(R.id.btnClipMode);

        btnPlayPause = view.findViewById(R.id.btnFullPlayPause);
        btnNext = view.findViewById(R.id.btnFullNext);
        btnPrev = view.findViewById(R.id.btnFullPrev);
        imgAlbumArt = view.findViewById(R.id.imgAlbumArt);
        close = view.findViewById(R.id.close);
        menu = view.findViewById(R.id.menu);
        btnTimer = view.findViewById(R.id.btnTimer);
        progressBar = view.findViewById(R.id.progressBarLoading);
        seekBar = view.findViewById(R.id.seekBar);

        // Initialize Bluetooth icon
        imgBluetoothIcon = view.findViewById(R.id.imgbluetooth_bottomsheet);

        // Hide Bluetooth elements initially
        hideBluetoothElements();

        close.setOnClickListener(v -> dismiss());

        // Setup timer button
        setupTimerButton();

        // Setup clip mode
        setupClipMode();

        // Check initial Bluetooth state
        checkInitialBluetoothState();

        setupUI();
        applyEdgeToEdge();
        setupBluetoothIcon();
    }

    private void setupClipMode() {
        if (btnClipMode == null) return;

        // Get or create ClipModeController from PlayerManager
        clipModeController = PlayerManager.getClipModeController();
        if (clipModeController == null) {
            clipModeController = new ClipModeController();
            PlayerManager.setClipModeController(clipModeController);
        }

        // Load saved clip duration from SettingsFragment
        int savedDuration = getSavedClipDuration();
        clipModeController.setClipDuration(savedDuration);

        clipModeController.setMediaPlayer(PlayerManager.getPlayer());

        // Check if clip mode was already active
        boolean wasActive = PlayerManager.isClipModeActive();
        if (wasActive && clipModeController.isClipModeActive()) {
            isClipModeActive = true;
            updateClipModeUI(true);
        }

        // Set listener
        clipModeController.setClipModeListener(new ClipModeController.ClipModeListener() {
            @Override
            public void onClipModeStarted() {
                PlayerManager.setClipModeActive(true);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        isClipModeActive = true;
                        updateClipModeUI(true);
                        // Update seek bar colors to indicate clip mode
                        seekBar.setProgressTintList(getResources().getColorStateList(R.color.yellow));
                        seekBar.setSecondaryProgressTintList(getResources().getColorStateList(R.color.white));

                        int duration = clipModeController.getClipDurationSeconds();
                        showToast("Clip mode activated - " + duration + "s loop");
                    });
                }
            }

            @Override
            public void onClipModeStopped() {
                PlayerManager.setClipModeActive(false);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        isClipModeActive = false;
                        updateClipModeUI(false);
                        // Restore normal seek bar colors
                        seekBar.setProgressTintList(getResources().getColorStateList(R.color.yellow));
                        seekBar.setSecondaryProgressTintList(getResources().getColorStateList(R.color.gray));
                    });
                }
            }

            @Override
            public void onClipLoopRestart() {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        // Flash effect on seek bar when loop restarts
                        seekBar.animate().alpha(0.5f).setDuration(100)
                                .withEndAction(() -> seekBar.animate().alpha(1f).setDuration(200).start());
                    });
                }
            }

            @Override
            public void onClipTimeUpdate(int currentPosition, int clipPosition, int clipProgress) {
                // This is handled by the seek bar updater
            }

            @Override
            public void onClipModePaused() {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        updateClipModeUI(false);
                    });
                }
            }

            @Override
            public void onClipModeResumed() {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        updateClipModeUI(true);
                    });
                }
            }

            @Override
            public void onClipDurationChanged(int durationSeconds) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        // Update any duration display if needed
                        Log.d(TAG, "Clip duration changed to: " + durationSeconds + "s");
                    });
                }
            }
        });

        // Set click listener
        btnClipMode.setOnClickListener(v -> toggleClipMode());

        // Long press to show quick duration selection (optional)
        btnClipMode.setOnLongClickListener(v -> {
            if (isAdded()) {
                showQuickDurationDialog();
            }
            return true;
        });
    }

    private void toggleClipMode() {
        if (PlayerManager.getPlayer() == null || PlayerManager.getCurrentAudio() == null) {
            showToast("No audio playing");
            return;
        }

        if (!PlayerManager.isClipModeActive()) {
            // Start clip mode at current position or from beginning
            int currentPosition = (int) PlayerManager.getCurrentPosition();
            int duration = (int) PlayerManager.getDuration();

            // Get current clip duration from controller
            int clipDuration = clipModeController.getClipDurationSeconds() * 1000;

            // If near the end, start from beginning
            if (duration - currentPosition < clipDuration) {
                currentPosition = 0;
            }

            clipModeController.startClipMode(currentPosition);
        } else {
            clipModeController.stopClipMode();
        }
    }

    private void updateClipModeUI(boolean active) {
        if (btnClipMode == null || !isAdded()) return;

        if (active) {
            btnClipMode.setImageResource(R.drawable.ic_clip_mode_active);
            btnClipMode.setColorFilter(getResources().getColor(R.color.yellow));
        } else {
            btnClipMode.setImageResource(R.drawable.ic_clip_mode);
            btnClipMode.setColorFilter(getResources().getColor(R.color.white));
        }
    }

    private void setupBluetoothIcon() {
        if (bluetoothInfoLayout != null) {
            bluetoothInfoLayout.setOnClickListener(v -> {
                openBluetoothDeviceList();
            });
        }
    }

    private void openBluetoothDeviceList() {
        BluetoothDeviceListBottomSheet bluetoothSheet = new BluetoothDeviceListBottomSheet();
        bluetoothSheet.setConnectionListener(new BluetoothDeviceListBottomSheet.BluetoothConnectionListener() {
            @Override
            public void onDeviceConnected(String deviceName) {
                // Update UI with connected device
                isBluetoothConnected = true;
                connectedBluetoothDeviceName = deviceName;
                safeUpdateUI();
                showToast("Connected to " + deviceName);
            }

            @Override
            public void onDeviceDisconnected() {
                // Update UI for disconnection
                handleBluetoothDisconnected();
                showToast("Bluetooth disconnected");
            }
        });
        bluetoothSheet.show(getParentFragmentManager(), "BluetoothDeviceList");
    }

    private void setupTimerButton() {
        if (btnTimer != null) {
            btnTimer.setOnClickListener(v -> {
                openSleepTimer();
            });

            // Update timer icon based on current state
            updateTimerIcon();
        }
    }

    private void openSleepTimer() {
        Log.d(TAG, "Opening sleep timer bottom sheet from player");

        // Show sleep timer bottom sheet
        SleepTimerBottomSheet timerSheet = new SleepTimerBottomSheet();
        timerSheet.show(getParentFragmentManager(), "SleepTimer");

        // Set listener to update UI when timer changes
        SleepTimerBottomSheet.setTimerListener(new SleepTimerBottomSheet.TimerListener() {
            @Override
            public void onTimerStarted(int minutes) {
                updateTimerIcon();
                showToast("Sleep timer set for " + minutes + " minutes");
            }

            @Override
            public void onTimerFinished() {
                updateTimerIcon();
                showToast("Sleep timer finished");

                // Pause clip mode if active
                if (clipModeController != null && clipModeController.isClipModeActive()) {
                    clipModeController.pauseClipMode();
                }
            }

            @Override
            public void onTimerCancelled() {
                updateTimerIcon();
                showToast("Sleep timer cancelled");

                // Resume clip mode if it was paused
                if (clipModeController != null && clipModeController.isClipModeActive() &&
                        clipModeController.isPaused()) {
                    clipModeController.resumeClipMode();
                }
            }
        });
    }

    private void updateTimerIcon() {
        if (!isAdded() || btnTimer == null) return;

        requireActivity().runOnUiThread(() -> {
            boolean isTimerActive = SleepTimerBottomSheet.isTimerActive();

            if (isTimerActive) {
                // Timer is active - show active state with yellow color
                btnTimer.setImageResource(R.drawable.stopwatch);
                btnTimer.setColorFilter(getResources().getColor(R.color.yellow));

                // Optional: Show remaining time as toast or in a tooltip
                long timeRemaining = SleepTimerBottomSheet.getTimeRemaining();
                long minutes = (timeRemaining / 1000) / 60;
                long seconds = (timeRemaining / 1000) % 60;

                // Set content description for accessibility
                btnTimer.setContentDescription(String.format("Sleep timer active with %d minutes %d seconds remaining", minutes, seconds));

                Log.d(TAG, "Timer icon set to ACTIVE - Time remaining: " + minutes + ":" + String.format("%02d", seconds));
            } else {
                // Timer not active - show default state
                btnTimer.setImageResource(R.drawable.stopwatch);
                btnTimer.setColorFilter(getResources().getColor(R.color.white));
                btnTimer.setContentDescription("Sleep timer");
                Log.d(TAG, "Timer icon set to INACTIVE");
            }
        });
    }

    /**
     * Get Bluetooth device friendly name - Prioritizes human-readable names
     */
    private String getBluetoothDeviceFriendlyName(BluetoothDevice device) {
        if (device == null) return null;

        try {
            // Method 1: Direct getName() - This gives the friendly Bluetooth name
            String name = device.getName();
            if (name != null && !name.isEmpty()) {
                // Check if it looks like a model number (all digits or short)
                if (!name.matches("^[0-9]+$") && name.length() > 3) {
                    Log.d(TAG, "Got friendly device name: " + name);
                    return name;
                } else {
                    Log.d(TAG, "Skipping model number: " + name);
                }
            }

            // Method 2: Try to get alias if available (API 5+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    String alias = device.getAlias();
                    if (alias != null && !alias.isEmpty() && !alias.matches("^[0-9]+$")) {
                        Log.d(TAG, "Got device alias: " + alias);
                        return alias;
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "Security exception getting alias: " + e.getMessage());
                }
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting device name: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get currently connected Bluetooth device name - Prioritizes friendly name over model number
     */
    private String getCurrentConnectedDeviceName() {
        if (audioManager == null || !isBluetoothConnected) return null;

        String friendlyName = null;
        String modelNumber = null;

        try {
            // For Android 6.0+, check AudioDeviceInfo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.media.AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
                for (android.media.AudioDeviceInfo device : devices) {
                    int type = device.getType();
                    if (type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {

                        // First try to get address and find bonded device with friendly name
                        String address = getDeviceAddress(device);
                        if (address != null) {
                            String nameFromBonded = getFriendlyNameFromAddress(address);
                            if (nameFromBonded != null && !nameFromBonded.matches("^[0-9]+$")) {
                                friendlyName = nameFromBonded;
                                Log.d(TAG, "Got friendly name from bonded device: " + friendlyName);
                                return friendlyName;
                            }
                        }

                        // If no friendly name found, store model number as fallback
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            CharSequence productName = device.getProductName();
                            if (productName != null && productName.length() > 0) {
                                modelNumber = productName.toString();
                                Log.d(TAG, "Got product name (model number): " + modelNumber);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting device from AudioManager: " + e.getMessage());
        }

        // Only return model number if it's the only thing available
        return modelNumber;
    }

    /**
     * Get friendly name from device address by checking bonded devices
     */
    private String getFriendlyNameFromAddress(String address) {
        if (address == null) return null;

        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) {
                Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice device : bondedDevices) {
                    if (address.equals(device.getAddress())) {
                        return getBluetoothDeviceFriendlyName(device);
                    }
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
        }
        return null;
    }

    /**
     * Get device address from AudioDeviceInfo using reflection
     */
    private String getDeviceAddress(android.media.AudioDeviceInfo device) {
        try {
            Method method = device.getClass().getMethod("getAddress");
            Object result = method.invoke(device);
            if (result instanceof String) {
                return (String) result;
            }
        } catch (Exception e) {
            // Ignore - method might not exist
        }
        return null;
    }

    /**
     * Hide all Bluetooth UI elements
     */
    private void hideBluetoothElements() {
        if (imgBluetoothIcon != null) {
            imgBluetoothIcon.setVisibility(View.GONE);
        }
        if (tvBluetoothDeviceName != null) {
            tvBluetoothDeviceName.setVisibility(View.GONE);
        }
        if (bluetoothInfoLayout != null) {
            bluetoothInfoLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Check initial Bluetooth connection state - Shows only friendly names
     */
    private void checkInitialBluetoothState() {
        try {
            isBluetoothConnected = isBluetoothDeviceConnected();
            if (isBluetoothConnected) {
                // First show a placeholder immediately
                connectedBluetoothDeviceName = "Connecting...";
                safeUpdateUI();

                // Then try to get the real friendly name after a short delay
                nameFetchHandler.postDelayed(() -> {
                    if (isAdded() && isBluetoothConnected) {
                        String realName = getCurrentConnectedDeviceName();

                        // Only use the name if it's not a model number (not all digits)
                        if (realName != null && !realName.isEmpty() && !realName.matches("^[0-9]+$")) {
                            connectedBluetoothDeviceName = realName;
                            Log.d(TAG, "Updated to real device name: " + realName);
                        } else {
                            // Try to get from bonded devices directly
                            try {
                                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                                if (adapter != null) {
                                    Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
                                    for (BluetoothDevice device : bondedDevices) {
                                        if (isDeviceConnected(device)) {
                                            String name = getBluetoothDeviceFriendlyName(device);
                                            if (name != null && !name.isEmpty() && !name.matches("^[0-9]+$")) {
                                                connectedBluetoothDeviceName = name;
                                                Log.d(TAG, "Got friendly name from bonded device: " + name);
                                                break;
                                            }
                                        }
                                    }
                                }
                            } catch (SecurityException e) {
                                Log.e(TAG, "Security exception: " + e.getMessage());
                            }
                        }

                        // If still no friendly name, show a generic message instead of model number
                        if (connectedBluetoothDeviceName == null ||
                                connectedBluetoothDeviceName.isEmpty() ||
                                connectedBluetoothDeviceName.matches("^[0-9]+$")) {
                            connectedBluetoothDeviceName = "Bluetooth Audio";
                            Log.d(TAG, "Using generic name instead of model number");
                        }

                        safeUpdateUI();
                    }
                }, 800); // 800ms delay to ensure connection is fully established
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking initial Bluetooth state: " + e.getMessage());
            isBluetoothConnected = false;
            connectedBluetoothDeviceName = "";
        }
    }

    /**
     * Check if a specific Bluetooth device is connected
     */
    private boolean isDeviceConnected(BluetoothDevice device) {
        if (device == null) return false;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                BluetoothManager bluetoothManager = (BluetoothManager) requireContext()
                        .getSystemService(Context.BLUETOOTH_SERVICE);
                if (bluetoothManager != null) {
                    // Check A2DP profile connection
                    if (bluetoothManager.getConnectedDevices(BluetoothProfile.A2DP).contains(device)) {
                        return true;
                    }
                    // Check HEADSET profile as fallback
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        if (bluetoothManager.getConnectedDevices(BluetoothProfile.HEADSET).contains(device)) {
                            return true;
                        }
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Profile not supported: " + e.getMessage());
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception checking connection: " + e.getMessage());
        }

        return false;
    }

    /**
     * Check if any Bluetooth device is currently connected
     */
    private boolean isBluetoothDeviceConnected() {
        if (audioManager == null) return false;

        // Quick check
        if (audioManager.isBluetoothA2dpOn()) {
            return true;
        }

        // For Android 6.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                android.media.AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
                for (android.media.AudioDeviceInfo device : devices) {
                    int type = device.getType();
                    if (type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                        return true;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking Bluetooth: " + e.getMessage());
            }
        }

        return false;
    }

    /**
     * Handle Bluetooth connected with device name - Only stores friendly names
     */
    private void handleBluetoothConnected(String deviceName) {
        isBluetoothConnected = true;

        // Use provided name if it's friendly (not all digits)
        if (deviceName != null && !deviceName.isEmpty() && !deviceName.matches("^[0-9]+$")) {
            connectedBluetoothDeviceName = deviceName;
        } else {
            // Try to get friendly name from bonded devices
            String friendlyName = null;
            try {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter != null) {
                    Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
                    for (BluetoothDevice device : bondedDevices) {
                        if (isDeviceConnected(device)) {
                            String name = getBluetoothDeviceFriendlyName(device);
                            if (name != null && !name.isEmpty() && !name.matches("^[0-9]+$")) {
                                friendlyName = name;
                                break;
                            }
                        }
                    }
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception: " + e.getMessage());
            }

            if (friendlyName != null) {
                connectedBluetoothDeviceName = friendlyName;
            } else {
                connectedBluetoothDeviceName = "Bluetooth Audio";
            }
        }

        Log.d(TAG, "🔵 Connected - Device Name: " + connectedBluetoothDeviceName);
        safeUpdateUI();
    }

    /**
     * Handle Bluetooth disconnected - ONE attempt only
     */
    private void handleBluetoothDisconnected() {
        Log.d(TAG, "🔴 Processing DISCONNECT - hiding UI immediately");

        isBluetoothConnected = false;
        connectedBluetoothDeviceName = "";

        requireActivity().runOnUiThread(() -> {
            // Hide immediately - no animations
            if (bluetoothInfoLayout != null) {
                bluetoothInfoLayout.setVisibility(View.GONE);
            }
            if (imgBluetoothIcon != null) {
                imgBluetoothIcon.setVisibility(View.GONE);
            }
            if (tvBluetoothDeviceName != null) {
                tvBluetoothDeviceName.setVisibility(View.GONE);
            }

            // Cancel any pending animations
            if (bluetoothInfoLayout != null) bluetoothInfoLayout.animate().cancel();
            if (imgBluetoothIcon != null) imgBluetoothIcon.animate().cancel();
            if (tvBluetoothDeviceName != null) tvBluetoothDeviceName.animate().cancel();

            Log.d(TAG, "🔵 Bluetooth icon HIDDEN");
        });
    }

    /**
     * Update Bluetooth icon visibility - Shows only friendly names
     */
    private void updateBluetoothIcon() {
        if (!isAdded()) return;

        requireActivity().runOnUiThread(() -> {
            if (isBluetoothConnected) {
                // Update device name text - ensure we never show model numbers
                if (tvBluetoothDeviceName != null) {
                    String displayName = connectedBluetoothDeviceName;
                    // If it's a model number (all digits), show generic name instead
                    if (displayName != null && displayName.matches("^[0-9]+$")) {
                        displayName = "Bluetooth Audio";
                    }
                    tvBluetoothDeviceName.setText(displayName);
                }

                // Show with animation
                if (bluetoothInfoLayout != null && bluetoothInfoLayout.getVisibility() != View.VISIBLE) {
                    bluetoothInfoLayout.setVisibility(View.VISIBLE);
                    bluetoothInfoLayout.setAlpha(0f);
                    bluetoothInfoLayout.animate().alpha(1f).setDuration(300).start();
                }

                if (imgBluetoothIcon != null && imgBluetoothIcon.getVisibility() != View.VISIBLE) {
                    imgBluetoothIcon.setVisibility(View.VISIBLE);
                    imgBluetoothIcon.setAlpha(0f);
                    imgBluetoothIcon.animate().alpha(1f).setDuration(300).start();
                }

                if (tvBluetoothDeviceName != null && tvBluetoothDeviceName.getVisibility() != View.VISIBLE) {
                    tvBluetoothDeviceName.setVisibility(View.VISIBLE);
                    tvBluetoothDeviceName.setAlpha(0f);
                    tvBluetoothDeviceName.animate().alpha(1f).setDuration(300).start();
                }

                Log.d(TAG, "🔵 Bluetooth SHOWN - Name: " + connectedBluetoothDeviceName);
            } else {
                // Hide immediately - no animation
                if (bluetoothInfoLayout != null) {
                    bluetoothInfoLayout.setVisibility(View.GONE);
                }
                if (imgBluetoothIcon != null) {
                    imgBluetoothIcon.setVisibility(View.GONE);
                }
                if (tvBluetoothDeviceName != null) {
                    tvBluetoothDeviceName.setVisibility(View.GONE);
                }
            }
        });
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

    private void setupUI() {
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio == null) return;

        safeUpdateUI();

        // Play/Pause button
        btnPlayPause.setOnClickListener(v -> {
            if (PlayerManager.getPlayer() == null) return;

            if (PlayerManager.isPlaying()) {
                PlayerManager.pausePlayback();
                btnPlayPause.setImageResource(R.drawable.playradio);

                // Pause clip mode if active
                if (clipModeController != null && clipModeController.isClipModeActive()) {
                    clipModeController.pauseClipMode();
                }
            } else {
                PlayerManager.getPlayer().start();
                btnPlayPause.setImageResource(R.drawable.pauseradio);

                // Resume clip mode if active
                if (clipModeController != null && clipModeController.isClipModeActive() &&
                        clipModeController.isPaused()) {
                    clipModeController.resumeClipMode();
                }
            }

            updatePlayPauseButton();
            broadcastUpdate();
        });

        // Next / Previous buttons
        btnNext.setOnClickListener(v -> PlayerManager.playNext(() -> {
            safeUpdateUI();
            restartSeekBarUpdater();

            // Update clip mode with new song
            if (clipModeController != null && clipModeController.isClipModeActive()) {
                clipModeController.setMediaPlayer(PlayerManager.getPlayer());
                clipModeController.startClipMode(0);
            }
        }));

        btnPrev.setOnClickListener(v -> PlayerManager.playPrevious(() -> {
            safeUpdateUI();
            restartSeekBarUpdater();

            // Update clip mode with new song
            if (clipModeController != null && clipModeController.isClipModeActive()) {
                clipModeController.setMediaPlayer(PlayerManager.getPlayer());
                clipModeController.startClipMode(0);
            }
        }));

        // Setup SeekBar
        setupSeekBar();

        // Load album art
        loadAlbumArtWithPalette(currentAudio);

        // Listen for audio changes
        PlayerManager.getInstance().setOnAudioChangedListener(newAudio -> {
            safeUpdateUI();
            restartSeekBarUpdater();
        });

        if (PlayerManager.getPlayer() != null) {
            PlayerManager.getPlayer().setOnBufferingUpdateListener((mp, percent) -> {
                if (percent < 100) showLoading();
                else hideLoading();
            });

            PlayerManager.getPlayer().setOnErrorListener((mp, what, extra) -> {
                hideLoading();
                if (isAdded()) Toast.makeText(getContext(), "Playback error", Toast.LENGTH_SHORT).show();
                return true;
            });

            PlayerManager.getPlayer().setOnPreparedListener(mp -> {
                hideLoading();
                seekBar.setMax(mp.getDuration());
                tvDuration.setText(formatTime(mp.getDuration()));
                updatePlayPauseButton();
                restartSeekBarUpdater();
            });

            PlayerManager.getPlayer().setOnCompletionListener(mp -> {
                PlayerManager.playNext(() -> {
                    safeUpdateUI();
                    restartSeekBarUpdater();
                });
            });
        }

        menu.setOnClickListener(v -> {
            if (getContext() instanceof AppCompatActivity) {
                AppCompatActivity activity = (AppCompatActivity) getContext();
                MenuBottomSheetFragment bottomSheetFragment = new MenuBottomSheetFragment();

                Bundle args = new Bundle();
                args.putString("audioName", PlayerManager.getCurrentAudio().getAudioName());
                args.putString("audioUrl", PlayerManager.getCurrentAudio().getAudioUrl());
                args.putString("artistName", PlayerManager.getCurrentAudio().getcategoryName());
                args.putString("imageUrl", PlayerManager.getCurrentAudio().getImageUrl());
                bottomSheetFragment.setArguments(args);

                bottomSheetFragment.show(activity.getSupportFragmentManager(), "MenuBottomSheet");
            }
        });
    }

    private void updatePlayPauseButton() {
        if (!isAdded()) return;

        requireActivity().runOnUiThread(() -> {
            if (PlayerManager.getPlayer() != null) {
                btnPlayPause.setImageResource(
                        PlayerManager.isPlaying() ?
                                R.drawable.pauseradio :
                                R.drawable.playradio
                );
            }
        });
    }

    private void restartSeekBarUpdater() {
        if (updateSeekBarRunnable != null) handler.removeCallbacks(updateSeekBarRunnable);
        setupSeekBar();
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        btnPlayPause.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        btnPlayPause.setVisibility(View.VISIBLE);
    }

    private void safeUpdateUI() {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            updateUIForCurrentAudio();
            updateBluetoothIcon();
            updateTimerIcon();
        });
    }

    private void updateUIForCurrentAudio() {
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio == null) return;

        tvSongName.setText(currentAudio.getAudioName());
        tvSongArtist.setText(currentAudio.getcategoryName() != null ? currentAudio.getcategoryName() : "Unknown Artist");

        updatePlayPauseButton();

        if (PlayerManager.getPlayer() != null) {
            seekBar.setMax(PlayerManager.getPlayer().getDuration());
            tvDuration.setText(formatTime(PlayerManager.getPlayer().getDuration()));
        }

        loadAlbumArt(currentAudio);
        loadAlbumArtWithPalette(currentAudio);
    }

    private void loadAlbumArt(AudioModel audio) {
        if (audio.getImageUrl() != null && !audio.getImageUrl().isEmpty()) {
            Glide.with(requireContext())
                    .load(audio.getImageUrl())
                    .placeholder(R.drawable.video_placholder)
                    .into(imgAlbumArt);
        } else {
            imgAlbumArt.setImageResource(R.drawable.video_placholder);
        }
    }

    private void loadAlbumArtWithPalette(AudioModel audio) {
        if (audio.getImageUrl() != null && !audio.getImageUrl().isEmpty()) {
            Picasso.get()
                    .load(audio.getImageUrl())
                    .placeholder(R.drawable.video_placholder)
                    .into(imgAlbumArt, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Bitmap bitmap = ((android.graphics.drawable.BitmapDrawable) imgAlbumArt.getDrawable()).getBitmap();
                            if (bitmap != null) {
                                Palette.from(bitmap).generate(palette -> {
                                    int darkColor = palette.getDarkVibrantColor(0xFF000000);
                                    imgAlbumArt.setBackground(new android.graphics.drawable.GradientDrawable(
                                            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                                            new int[]{darkColor, 0x00000000}));
                                    View rootView = getView();
                                    if (rootView != null) {
                                        rootView.setBackground(new android.graphics.drawable.GradientDrawable(
                                                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                                                new int[]{darkColor, 0xFF121212}));
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            imgAlbumArt.setImageResource(R.drawable.video_placholder);
                        }
                    });
        } else {
            imgAlbumArt.setImageResource(R.drawable.video_placholder);
        }
    }

    private void setupSeekBar() {
        if (PlayerManager.getPlayer() == null) return;

        int currentPosition = (int) PlayerManager.getCurrentPosition();
        int duration = PlayerManager.getPlayer().getDuration();

        seekBar.setMax(duration);
        seekBar.setProgress(currentPosition);
        tvCurrentTime.setText(formatTime(currentPosition));
        tvDuration.setText(formatTime(duration));

        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;
                if (PlayerManager.getPlayer() != null) {
                    int currentPos = PlayerManager.getPlayer().getCurrentPosition();

                    // Use PlayerManager's clip mode controller
                    ClipModeController controller = PlayerManager.getClipModeController();

                    if (controller != null && controller.isClipModeActive()) {
                        int constrainedPos = controller.getConstrainedPosition(currentPos);

                        // Only update if position changed significantly
                        if (Math.abs(seekBar.getProgress() - constrainedPos) > 100) {
                            seekBar.setProgress(constrainedPos);
                        }

                        // Format time to show clip position within the loop
                        int clipPosition = constrainedPos - controller.getClipStartPosition();
                        int clipDurationSec = controller.getClipDurationSeconds();
                        tvCurrentTime.setText(formatTime(clipPosition) + " / 0:" + String.format("%02d", clipDurationSec));

                        // Update secondary progress to show clip boundaries
                        int clipEnd = controller.getClipStartPosition() + (clipDurationSec * 1000);
                        seekBar.setSecondaryProgress(clipEnd);
                    } else {
                        seekBar.setProgress(currentPos);
                        tvCurrentTime.setText(formatTime(currentPos));
                        seekBar.setSecondaryProgress(0);
                    }
                }
                handler.postDelayed(this, 500);
            }
        };
        handler.post(updateSeekBarRunnable);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && PlayerManager.getPlayer() != null) {
                    // Constrain progress if clip mode is active
                    ClipModeController controller = PlayerManager.getClipModeController();
                    if (controller != null && controller.isClipModeActive()) {
                        int constrainedProgress = controller.getConstrainedPosition(progress);
                        PlayerManager.getPlayer().seekTo(constrainedProgress);

                        // Update time display with clip format
                        int clipPosition = constrainedProgress - controller.getClipStartPosition();
                        int clipDurationSec = controller.getClipDurationSeconds();
                        tvCurrentTime.setText(formatTime(clipPosition) + " / 0:" + String.format("%02d", clipDurationSec));
                    } else {
                        PlayerManager.getPlayer().seekTo(progress);
                        tvCurrentTime.setText(formatTime(progress));
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }


    private int getSavedClipDuration() {
        if (getContext() == null) return 30; // Default 30 seconds

        SharedPreferences prefs = getContext().getSharedPreferences("ClipSettings", Context.MODE_PRIVATE);
        return prefs.getInt("clip_duration", 30);
    }

    private void showQuickDurationDialog() {
        if (!isAdded() || getContext() == null) return;

        String[] durations = {"5s", "10s", "15s", "30s", "45s", "60s", "90s", "100s"};

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Clip Duration")
                .setItems(durations, (dialog, which) -> {
                    int seconds;
                    switch (which) {
                        case 0: seconds = 5; break;
                        case 1: seconds = 10; break;
                        case 2: seconds = 15; break;
                        case 3: seconds = 30; break;
                        case 4: seconds = 45; break;
                        case 5: seconds = 60; break;
                        case 6: seconds = 90; break;
                        case 7: seconds = 100; break;
                        default: seconds = 30;
                    }

                    if (clipModeController != null) {
                        clipModeController.setClipDuration(seconds);
                        showToast("Clip duration set to " + seconds + " seconds");

                        // Save to SharedPreferences
                        SharedPreferences prefs = requireContext().getSharedPreferences("ClipSettings", Context.MODE_PRIVATE);
                        prefs.edit().putInt("clip_duration", seconds).apply();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void broadcastUpdate() {
        if (getActivity() != null) {
            String packageName = getActivity().getPackageName();

            Intent miniPlayerIntent = new Intent("UPDATE_MINI_PLAYER");
            Intent audioAdapterIntent = new Intent("UPDATE_AUDIO_ADAPTER");

            miniPlayerIntent.setPackage(packageName);
            audioAdapterIntent.setPackage(packageName);

            getActivity().sendBroadcast(miniPlayerIntent);
            getActivity().sendBroadcast(audioAdapterIntent);
        }
    }

    private String formatTime(int millis) {
        int minutes = (millis / 1000) / 60;
        int seconds = (millis / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register broadcast receivers
        IntentFilter playerFilter = new IntentFilter();
        playerFilter.addAction("UPDATE_MINI_PLAYER");
        playerFilter.addAction("UPDATE_AUDIO_ADAPTER");

        IntentFilter bluetoothFilter = new IntentFilter();
        bluetoothFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        bluetoothFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        bluetoothFilter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        bluetoothFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

        // Timer filter
        IntentFilter timerFilter = new IntentFilter("UPDATE_TIMER_STATUS");

        // Clip mode filter
        IntentFilter clipModeFilter = new IntentFilter("CLIP_MODE_STATE_CHANGED");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(playerUpdateReceiver, playerFilter, Context.RECEIVER_NOT_EXPORTED);
            requireContext().registerReceiver(bluetoothReceiver, bluetoothFilter, Context.RECEIVER_EXPORTED);
            requireContext().registerReceiver(timerUpdateReceiver, timerFilter, Context.RECEIVER_NOT_EXPORTED);
            requireContext().registerReceiver(clipModeStateReceiver, clipModeFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(playerUpdateReceiver, playerFilter);
            requireContext().registerReceiver(bluetoothReceiver, bluetoothFilter);
            requireContext().registerReceiver(timerUpdateReceiver, timerFilter);
            requireContext().registerReceiver(clipModeStateReceiver, clipModeFilter);
        }

        IntentFilter bluetoothListFilter = new IntentFilter();
        bluetoothListFilter.addAction("BLUETOOTH_DEVICE_CONNECTED");
        bluetoothListFilter.addAction("BLUETOOTH_DEVICE_DISCONNECTED");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(bluetoothListReceiver, bluetoothListFilter,
                    Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(bluetoothListReceiver, bluetoothListFilter);
        }

        // Update UI
        safeUpdateUI();
        checkInitialBluetoothState();
        updateTimerIcon();

        // Update clip mode with current player
        clipModeController = PlayerManager.getClipModeController();
        if (clipModeController != null) {
            clipModeController.setMediaPlayer(PlayerManager.getPlayer());

            // Sync UI with current clip mode state
            boolean wasActive = PlayerManager.isClipModeActive();
            if (wasActive && clipModeController.isClipModeActive()) {
                isClipModeActive = true;
                updateClipModeUI(true);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            requireContext().unregisterReceiver(playerUpdateReceiver);
            requireContext().unregisterReceiver(bluetoothReceiver);
            requireContext().unregisterReceiver(timerUpdateReceiver);
            requireContext().unregisterReceiver(bluetoothListReceiver);
            requireContext().unregisterReceiver(clipModeStateReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver not registered", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(updateSeekBarRunnable);
        nameFetchHandler.removeCallbacksAndMessages(null);

        // Don't release clipModeController here - it's now managed by PlayerManager
        // Just clear the reference
        clipModeController = null;
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