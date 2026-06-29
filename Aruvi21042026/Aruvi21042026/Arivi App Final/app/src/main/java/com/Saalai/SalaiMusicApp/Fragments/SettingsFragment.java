package com.Saalai.SalaiMusicApp.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.Saalai.SalaiMusicApp.ClipModeController;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";
    private static final String PREFS_NAME = "ClipSettings";
    private static final String KEY_CLIP_DURATION = "clip_duration";

    private SeekBar clipDurationSeekBar;
    private TextView tvClipDurationValue;
    private ImageView backButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize views
        initViews(view);

        // Setup click listeners
        setupClickListeners();

        // Setup clip duration seek bar
        setupClipDurationSeekBar();

        // Load saved settings
        loadSavedSettings();

        return view;
    }

    private void initViews(View view) {
        clipDurationSeekBar = view.findViewById(R.id.clipDurationSeekBar);
        tvClipDurationValue = view.findViewById(R.id.tvClipDurationValue);
        backButton = view.findViewById(R.id.backbtn);
    }

    private void setupClickListeners() {
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }
    }

    private void setupClipDurationSeekBar() {
        if (clipDurationSeekBar == null) return;

        // Set max to 100 seconds
        clipDurationSeekBar.setMax(100);

        // Set minimum progress to 5 seconds programmatically
        clipDurationSeekBar.setProgress(30); // Default 30 seconds

        clipDurationSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // Ensure minimum of 5 seconds
                    if (progress < 5) {
                        progress = 5;
                        seekBar.setProgress(progress);
                    }

                    // Update the displayed value
                    tvClipDurationValue.setText(progress + "s");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Optional: Add haptic feedback
                if (getContext() != null) {
                    seekBar.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int duration = seekBar.getProgress();

                // Save the setting
                saveClipDuration(duration);

                // Update ClipModeController if it exists
                updateClipModeController(duration);

                // Show confirmation
                showToast("Clip duration set to " + duration + " seconds");

                Log.d(TAG, "Clip duration saved: " + duration + "s");
            }
        });
    }

    private void loadSavedSettings() {
        if (getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int savedDuration = prefs.getInt(KEY_CLIP_DURATION, 30); // Default 30 seconds

        // Update UI
        if (clipDurationSeekBar != null) {
            clipDurationSeekBar.setProgress(savedDuration);
        }
        if (tvClipDurationValue != null) {
            tvClipDurationValue.setText(savedDuration + "s");
        }

        // Update ClipModeController
        updateClipModeController(savedDuration);

        Log.d(TAG, "Loaded saved clip duration: " + savedDuration + "s");
    }

    private void saveClipDuration(int durationSeconds) {
        if (getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_CLIP_DURATION, durationSeconds);
        editor.apply();
    }

    private void updateClipModeController(int durationSeconds) {
        try {
            ClipModeController controller = PlayerManager.getClipModeController();
            if (controller != null) {
                controller.setClipDuration(durationSeconds);
                Log.d(TAG, "Updated ClipModeController with duration: " + durationSeconds + "s");
            } else {
                Log.d(TAG, "ClipModeController not available yet");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating ClipModeController: " + e.getMessage());
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    // Static method to get the saved clip duration from anywhere
    public static int getSavedClipDuration(Context context) {
        if (context == null) return 30; // Default

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_CLIP_DURATION, 30);
    }
}