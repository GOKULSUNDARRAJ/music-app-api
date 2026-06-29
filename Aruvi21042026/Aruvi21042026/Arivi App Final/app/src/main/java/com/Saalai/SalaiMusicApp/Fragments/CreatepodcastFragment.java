package com.Saalai.SalaiMusicApp.Fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.Saalai.SalaiMusicApp.Custom.WaveformSeekBar;
import com.Saalai.SalaiMusicApp.Custom.WaveformView;
import com.Saalai.SalaiMusicApp.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CreatepodcastFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_STORAGE_PERMISSION = 201;

    // TAG for logging
    private static final String TAG = "CreatePodcastFragment";

    // Views
    private EditText editTextPodcastName;
    private CardView uploadContainer;
    private ImageView imageViewPreview;
    private ImageView imageViewUploadIcon;
    private ConstraintLayout imguploadlayout;
    private TextView textViewTimer;
    private ImageView buttonDeleteImage; // For image deletion
    private ImageView deletebtn; // For recording deletion in recording layout
    private ImageView buttonRecord1, buttonRecord;
    private LinearLayout recordlayout1, recordlayout2;
    private ImageView buttonStop;
    private TextView textViewRecordingStatus;

    // Player layout views
    private LinearLayout playerLayout;
    private ImageView buttonPlayPauseout;
    private ImageView buttonDeletePlayer; // New delete button in player layout
    private WaveformSeekBar waveformSeekBar;
    private TextView playingTime;
    private TextView totalTime;

    // Add to podcasts button and progress bar
    private ConstraintLayout llsignup;
    private LinearLayout btnlayout;
    private ProgressBar progressBar;
    private TextView addToPodcastsText;

    // Recording variables
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String recordedFilePath;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private boolean isTimerRunning = false;
    private CountDownTimer timer;
    private long recordingTime = 0;
    private Handler waveformHandler = new Handler();
    private Runnable waveformRunnable;
    private Handler seekBarHandler = new Handler();
    private Runnable seekBarRunnable;

    private boolean isRecordingPaused = false;

    // Flag to track if we're pausing for image picker
    private boolean isPausingForImagePicker = false;

    // Permissions
    private String[] permissions = {Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    private Uri selectedImageUri;
    private WaveformView waveformView;

    private List<String> recordedSegments = new ArrayList<>();
    private String currentSegmentPath = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Creating view for CreatePodcastFragment");

        try {
            View view = inflater.inflate(R.layout.fragment_createprodcast, container, false);
            Log.d(TAG, "onCreateView: Layout inflated successfully");

            initViews(view);
            setupListeners();
            setupBackButtonHandler(view);
            checkPermissions();

            return view;
        } catch (Exception e) {
            Log.e(TAG, "onCreateView: Error inflating layout", e);
            Toast.makeText(getContext(), "Error loading create podcast screen", Toast.LENGTH_SHORT).show();
            throw e;
        }
    }

    private void initViews(View view) {
        Log.d(TAG, "initViews: Initializing views");

        try {
            waveformView = view.findViewById(R.id.waveformView);
            editTextPodcastName = view.findViewById(R.id.editTextPodcastName);
            uploadContainer = view.findViewById(R.id.uploadContainer);
            imageViewPreview = view.findViewById(R.id.imageViewPreview);
            imageViewUploadIcon = view.findViewById(R.id.imageViewUploadIcon);
            imguploadlayout = view.findViewById(R.id.imguploadlayout);
            buttonDeleteImage = view.findViewById(R.id.buttonDeleteImage);
            textViewTimer = view.findViewById(R.id.textViewTimer);
            deletebtn = view.findViewById(R.id.deletebtn);
            buttonRecord = view.findViewById(R.id.buttonRecord);
            buttonRecord1 = view.findViewById(R.id.buttonRecord1);
            recordlayout1 = view.findViewById(R.id.recordlayout1);
            recordlayout2 = view.findViewById(R.id.recordlayout2);
            buttonStop = view.findViewById(R.id.buttonStop);
            textViewRecordingStatus = view.findViewById(R.id.textViewRecordingStatus);

            // Initialize the player layout and its controls
            playerLayout = view.findViewById(R.id.player);
            buttonPlayPauseout = view.findViewById(R.id.buttonPlayPauseout);
            buttonDeletePlayer = view.findViewById(R.id.buttonDeletePlayer); // New delete button in player
            waveformSeekBar = view.findViewById(R.id.waveformSeekBar);
            playingTime = view.findViewById(R.id.playingtime);
            totalTime = view.findViewById(R.id.totaltime);

            // Initialize Add to podcasts button and progress bar
            llsignup = view.findViewById(R.id.llsignup);
            btnlayout = view.findViewById(R.id.btnlayout);
            progressBar = view.findViewById(R.id.progressBar);

            // Find the text view inside btnlayout
            if (btnlayout != null) {
                for (int i = 0; i < btnlayout.getChildCount(); i++) {
                    View child = btnlayout.getChildAt(i);
                    if (child instanceof TextView) {
                        addToPodcastsText = (TextView) child;
                        break;
                    }
                }
            }

            Log.d(TAG, "initViews: Player layout found: " + (playerLayout != null));
            Log.d(TAG, "initViews: PlayPauseout found: " + (buttonPlayPauseout != null));
            Log.d(TAG, "initViews: Delete Player button found: " + (buttonDeletePlayer != null));
            Log.d(TAG, "initViews: WaveformSeekBar found: " + (waveformSeekBar != null));
            Log.d(TAG, "initViews: Playing time found: " + (playingTime != null));
            Log.d(TAG, "initViews: Total time found: " + (totalTime != null));
            Log.d(TAG, "initViews: Add to podcasts button found: " + (llsignup != null));
            Log.d(TAG, "initViews: Progress bar found: " + (progressBar != null));

            // Initially hide the add to podcasts button
            if (llsignup != null) {
                llsignup.setVisibility(View.GONE);
            }

            Log.d(TAG, "initViews: All views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "initViews: Error initializing views", e);
        }
    }

    private void setupListeners() {
        Log.d(TAG, "setupListeners: Setting up click listeners");

        // Upload picture
        if (uploadContainer != null) {
            uploadContainer.setOnClickListener(v -> {
                Log.d(TAG, "uploadContainer clicked");
                openImagePicker();
            });
        }

        // Delete image button (for uploaded picture)
        if (buttonDeleteImage != null) {
            buttonDeleteImage.setOnClickListener(v -> {
                Log.d(TAG, "buttonDeleteImage clicked");
                deleteSelectedImage();
                checkAndShowAddButton();
            });
        }

        // Delete button for recording (in recording layout)
        if (deletebtn != null) {
            deletebtn.setOnClickListener(v -> {
                Log.d(TAG, "deletebtn clicked - deleting recording");
                deleteRecording();
                checkAndShowAddButton();
            });
        }

        // New delete button in player layout
        if (buttonDeletePlayer != null) {
            buttonDeletePlayer.setOnClickListener(v -> {
                Log.d(TAG, "buttonDeletePlayer clicked - deleting recording from player");
                deleteRecording();
                checkAndShowAddButton();
            });
        }

        // Record button (second layout) - controls recording
        if (buttonRecord != null) {
            buttonRecord.setOnClickListener(v -> {
                Log.d(TAG, "buttonRecord clicked, isRecording: " + isRecording + ", isRecordingPaused: " + isRecordingPaused);

                if (isRecording) {
                    pauseRecording();
                } else if (isRecordingPaused) {
                    resumeRecording();
                } else {
                    startRecording();
                }
            });
        }

        // Record button (first layout) - switches to recording layout
        if (buttonRecord1 != null) {
            buttonRecord1.setOnClickListener(v -> {
                Log.d(TAG, "buttonRecord1 clicked, switching layouts");
                if (recordlayout2 != null && recordlayout1 != null) {
                    recordlayout2.setVisibility(View.VISIBLE);
                    recordlayout1.setVisibility(View.GONE);
                }

                // If we already have a recording file, just show it
                if (recordedFilePath != null && new File(recordedFilePath).exists()) {
                    updateUIAfterRecording();
                } else {
                    // Otherwise start a new recording
                    if (!isRecording && !isRecordingPaused) {
                        startRecording();
                    }
                }
            });
        }

        // Play/Pause button (player layout)
        if (buttonPlayPauseout != null) {
            buttonPlayPauseout.setOnClickListener(v -> {
                Log.d(TAG, "buttonPlayPauseout clicked, isPlaying: " + isPlaying);
                handlePlayPauseClick();
            });
        }

        // Stop/Finish button
        if (buttonStop != null) {
            buttonStop.setOnClickListener(v -> {
                Log.d(TAG, "buttonStop clicked");
                onFinishButtonClicked();
            });
        }

        // WaveformSeekBar listener
        if (waveformSeekBar != null) {
            waveformSeekBar.setOnSeekBarChangeListener(new WaveformSeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(WaveformSeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && mediaPlayer != null) {
                        mediaPlayer.seekTo(progress);
                        updatePlayingTime(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(WaveformSeekBar seekBar) {
                    stopSeekBarUpdate();
                }

                @Override
                public void onStopTrackingTouch(WaveformSeekBar seekBar) {
                    if (isPlaying) {
                        startSeekBarUpdate();
                    }
                }
            });
        }

        // Add to podcasts button click listener
        if (llsignup != null) {
            llsignup.setOnClickListener(v -> {
                Log.d(TAG, "Add to podcasts button clicked");
                addToPodcasts();
            });
        }
    }

    private void checkAndShowAddButton() {
        boolean hasImage = (selectedImageUri != null);
        boolean hasAudio = (recordedFilePath != null && new File(recordedFilePath).exists());

        Log.d(TAG, "checkAndShowAddButton: hasImage=" + hasImage + ", hasAudio=" + hasAudio);

        if (hasImage && hasAudio && llsignup != null) {
            llsignup.setVisibility(View.VISIBLE);
        } else if (llsignup != null) {
            llsignup.setVisibility(View.GONE);
        }
    }

    private void addToPodcasts() {
        Log.d(TAG, "addToPodcasts: Starting add to podcasts process");

        String podcastName = editTextPodcastName.getText().toString().trim();

        if (podcastName.isEmpty()) {
            Log.w(TAG, "addToPodcasts: Podcast name is empty");
            editTextPodcastName.setError("Please enter podcast name");
            return;
        }

        // Show progress bar and hide text
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (addToPodcastsText != null) {
            addToPodcastsText.setVisibility(View.GONE);
        }
        if (btnlayout != null) {
            btnlayout.setEnabled(false);
        }

        // Simulate upload process
        new Handler().postDelayed(() -> {
            performAddToPodcasts(podcastName);
        }, 2000);
    }

    private void performAddToPodcasts(String podcastName) {
        Log.d(TAG, "performAddToPodcasts: Adding podcast: " + podcastName);

        // Here you would implement actual upload to server
        boolean success = true;

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (success) {
            Toast.makeText(requireContext(), "Podcast added successfully!", Toast.LENGTH_LONG).show();
            resetForm();
        } else {
            if (addToPodcastsText != null) {
                addToPodcastsText.setVisibility(View.VISIBLE);
            }
            if (btnlayout != null) {
                btnlayout.setEnabled(true);
            }
            Toast.makeText(requireContext(), "Failed to add podcast. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetForm() {
        // Clear podcast name
        if (editTextPodcastName != null) {
            editTextPodcastName.setText("");
        }

        // Delete image
        deleteSelectedImage();

        // Delete recording
        deleteRecording();

        // Hide add button
        if (llsignup != null) {
            llsignup.setVisibility(View.GONE);
        }

        // Reset button state
        if (addToPodcastsText != null) {
            addToPodcastsText.setVisibility(View.VISIBLE);
        }
        if (btnlayout != null) {
            btnlayout.setEnabled(true);
        }

        // Ensure we're on first layout
        if (recordlayout1 != null && recordlayout2 != null) {
            recordlayout1.setVisibility(View.VISIBLE);
            recordlayout2.setVisibility(View.GONE);
        }
    }

    private void deleteRecording() {
        Log.d(TAG, "deleteRecording: Deleting current recording");

        // Stop any ongoing recording or playback
        if (isRecording) {
            stopRecording();
        }
        if (isPlaying) {
            stopPlaying();
        }

        // Delete the recorded file if it exists
        if (recordedFilePath != null) {
            File file = new File(recordedFilePath);
            if (file.exists()) {
                boolean deleted = file.delete();
                Log.d(TAG, "deleteRecording: Recorded file deleted: " + deleted);
            }
            recordedFilePath = null;
        }

        // Delete all segment files
        for (String segmentPath : recordedSegments) {
            File segmentFile = new File(segmentPath);
            if (segmentFile.exists()) {
                segmentFile.delete();
            }
        }
        recordedSegments.clear();

        // Reset recording state
        recordingTime = 0;
        if (textViewTimer != null) {
            textViewTimer.setText("00:00:00");
        }
        if (textViewRecordingStatus != null) {
            textViewRecordingStatus.setText("Ready to record");
            textViewRecordingStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
        }

        // Hide player layout
        if (playerLayout != null) {
            playerLayout.setVisibility(View.GONE);
        }

        // Reset time displays
        if (playingTime != null) {
            playingTime.setText("0:00");
        }
        if (totalTime != null) {
            totalTime.setText("0:00");
        }

        // Reset waveform
        if (waveformSeekBar != null) {
            waveformSeekBar.generateRandomWaveform(50);
            waveformSeekBar.setProgress(0);
        }

        // Update UI
        updateUIAfterRecording();

        Toast.makeText(requireContext(), "Recording deleted", Toast.LENGTH_SHORT).show();
    }

    private void resetAllForNewRecording() {
        Log.d(TAG, "resetAllForNewRecording: Resetting everything for new recording");

        // Stop all activities
        if (isRecording) {
            stopRecording();
        }
        if (isPlaying) {
            stopPlaying();
        }

        // Clear recording data
        recordedFilePath = null;
        recordedSegments.clear();
        recordingTime = 0;

        // Clear image
        selectedImageUri = null;
        if (imageViewPreview != null) {
            imageViewPreview.setImageResource(android.R.color.transparent);
            imageViewPreview.setVisibility(View.GONE);
        }
        if (imguploadlayout != null) {
            imguploadlayout.setVisibility(View.VISIBLE);
        }
        if (buttonDeleteImage != null) {
            buttonDeleteImage.setVisibility(View.GONE);
        }

        // Reset UI
        if (textViewTimer != null) {
            textViewTimer.setText("00:00:00");
        }
        if (textViewRecordingStatus != null) {
            textViewRecordingStatus.setText("Ready to record");
            textViewRecordingStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
        }
        if (editTextPodcastName != null) {
            editTextPodcastName.setText("");
        }

        // Hide player and add button
        if (playerLayout != null) {
            playerLayout.setVisibility(View.GONE);
        }
        if (llsignup != null) {
            llsignup.setVisibility(View.GONE);
        }

        // Reset to first layout
        if (recordlayout1 != null && recordlayout2 != null) {
            recordlayout1.setVisibility(View.VISIBLE);
            recordlayout2.setVisibility(View.GONE);
        }

        // Reset time displays
        if (playingTime != null) {
            playingTime.setText("0:00");
        }
        if (totalTime != null) {
            totalTime.setText("0:00");
        }

        // Reset waveform
        if (waveformSeekBar != null) {
            waveformSeekBar.generateRandomWaveform(50);
            waveformSeekBar.setProgress(0);
        }

        // Reset states
        isRecording = false;
        isPlaying = false;
        isRecordingPaused = false;
    }

    private void handlePlayPauseClick() {
        if (isRecordingPaused && !recordedSegments.isEmpty()) {
            Log.d(TAG, "Recording is paused with segments. Stopping recording first...");
            stopRecording();
            updateUIAfterRecording();

            new Handler().postDelayed(() -> {
                tryPlaying();
            }, 500);
            return;
        }

        if (recordedFilePath == null) {
            Log.w(TAG, "No recording file to play");
            Toast.makeText(requireContext(), "Please record audio first", Toast.LENGTH_SHORT).show();
            return;
        }

        File recordingFile = new File(recordedFilePath);
        if (!recordingFile.exists()) {
            Log.w(TAG, "Recording file doesn't exist: " + recordedFilePath);
            Toast.makeText(requireContext(), "Recording file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isPlaying) {
            pausePlaying();
        } else {
            startPlaying();
        }
    }

    private void tryPlaying() {
        if (recordedFilePath == null) {
            Log.w(TAG, "tryPlaying: recordedFilePath is still null after stopping");
            Toast.makeText(requireContext(), "No recording available to play", Toast.LENGTH_SHORT).show();
            return;
        }

        File recordingFile = new File(recordedFilePath);
        if (!recordingFile.exists()) {
            Log.w(TAG, "tryPlaying: File still doesn't exist: " + recordedFilePath);
            Toast.makeText(requireContext(), "Recording file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isPlaying) {
            pausePlaying();
        } else {
            startPlaying();
        }
    }

    private void onFinishButtonClicked() {
        Log.d(TAG, "onFinishButtonClicked: Handling finish button click");

        stopRecording();
        stopPlaying();

        if (recordedFilePath != null && new File(recordedFilePath).exists()) {
            if (playerLayout != null) {
                playerLayout.setVisibility(View.VISIBLE);
            }

            setupSeekBar();

            File recordingFile = new File(recordedFilePath);
            if (textViewRecordingStatus != null) {
                textViewRecordingStatus.setText("Recording saved (" + recordingFile.length() + " bytes). Ready to play.");
            }

            switchToFirstLayout();
            checkAndShowAddButton();
        } else {
            if (textViewRecordingStatus != null) {
                textViewRecordingStatus.setText("No recording available");
            }
            if (playerLayout != null) {
                playerLayout.setVisibility(View.GONE);
            }
        }

        if (textViewRecordingStatus != null) {
            textViewRecordingStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));
        }
    }

    private void setupSeekBar() {
        if (recordedFilePath != null && waveformSeekBar != null) {
            try {
                MediaPlayer tempPlayer = new MediaPlayer();
                tempPlayer.setDataSource(recordedFilePath);
                tempPlayer.prepare();
                int duration = tempPlayer.getDuration();
                waveformSeekBar.setMaxProgress(duration);

                String totalTimeStr = formatTime(duration);
                if (totalTime != null) {
                    totalTime.setText(totalTimeStr);
                }

                if (playingTime != null) {
                    playingTime.setText("0:00");
                }

                generateWaveformFromAudio(recordedFilePath);

                tempPlayer.release();
            } catch (IOException e) {
                Log.e(TAG, "setupSeekBar: Error getting audio duration", e);
            }
        }
    }

    private void generateWaveformFromAudio(String filePath) {
        if (waveformSeekBar != null) {
            waveformSeekBar.generateRandomWaveform(100);
        }
    }

    private String formatTime(int milliseconds) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(minutes);
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    private void updatePlayingTime(int currentPosition) {
        if (playingTime != null) {
            playingTime.setText(formatTime(currentPosition));
        }
    }

    private void startSeekBarUpdate() {
        if (waveformSeekBar == null) return;

        stopSeekBarUpdate();

        seekBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    waveformSeekBar.setProgress(currentPosition);
                    updatePlayingTime(currentPosition);
                    seekBarHandler.postDelayed(this, 100);
                }
            }
        };
        seekBarHandler.post(seekBarRunnable);
    }

    private void stopSeekBarUpdate() {
        if (seekBarHandler != null && seekBarRunnable != null) {
            seekBarHandler.removeCallbacks(seekBarRunnable);
        }
    }

    private void deleteSelectedImage() {
        Log.d(TAG, "deleteSelectedImage: Deleting selected image");

        if (selectedImageUri != null) {
            selectedImageUri = null;

            if (imageViewPreview != null) {
                imageViewPreview.setImageResource(android.R.color.transparent);
                imageViewPreview.setVisibility(View.GONE);
            }

            if (imguploadlayout != null) {
                imguploadlayout.setVisibility(View.VISIBLE);
            }

            if (buttonDeleteImage != null) {
                buttonDeleteImage.setVisibility(View.GONE);
            }

            Log.d(TAG, "deleteSelectedImage: Image deleted successfully");
            Toast.makeText(requireContext(), "Image removed", Toast.LENGTH_SHORT).show();
        } else {
            Log.w(TAG, "deleteSelectedImage: No image to delete");
            Toast.makeText(requireContext(), "No image to delete", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBackButtonHandler(View view) {
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                if (recordlayout2 != null && recordlayout2.getVisibility() == View.VISIBLE) {
                    // If in recording layout, just switch to first layout
                    switchToFirstLayout();
                    return true;
                } else {
                    // If in main layout, reset everything for new recording and go back
                    resetAllForNewRecording();
                    // Let the system handle the back press to close fragment
                    return false;
                }
            }
            return false;
        });
    }

    private void switchToFirstLayout() {
        Log.d(TAG, "switchToFirstLayout: Switching to first layout");

        if (isRecording) {
            stopRecording();
        }

        if (isPlaying) {
            stopPlaying();
        }

        if (recordlayout2 != null && recordlayout1 != null) {
            recordlayout2.setVisibility(View.GONE);
            recordlayout1.setVisibility(View.VISIBLE);
        }

        updateUIAfterRecording();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
            }
        }
    }

    private boolean checkAllPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
            return false;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
                return false;
            }
        }

        return true;
    }

    private void startRecording() {
        Log.d(TAG, "startRecording: Attempting to start recording");

        if (!checkAllPermissions()) {
            Toast.makeText(requireContext(), "Please grant all permissions to record", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isRecordingPaused && recordedSegments.isEmpty()) {
            recordedFilePath = null;
        }

        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC);

            if (storageDir == null) {
                storageDir = requireContext().getFilesDir();
            }

            if (storageDir == null) {
                Toast.makeText(requireContext(), "Cannot access storage", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            currentSegmentPath = new File(storageDir, "segment_" + timeStamp + ".mp3").getAbsolutePath();

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(96000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(currentSegmentPath);

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Recording setup failed", Toast.LENGTH_SHORT).show();
                cleanupAfterFailedRecording();
                return;
            }

            isRecording = true;
            isRecordingPaused = false;
            updateUIForRecording();
            startTimer();
            startWaveformAnimation();

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Recording failed to start", Toast.LENGTH_SHORT).show();
            cleanupAfterFailedRecording();
        }
    }

    private void pauseRecording() {
        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;

                if (currentSegmentPath != null) {
                    recordedSegments.add(currentSegmentPath);
                    currentSegmentPath = null;
                }

                isRecording = false;
                isRecordingPaused = true;

                stopTimer();
                stopWaveformAnimation();

                updateUIForPausedRecording();
                Toast.makeText(requireContext(), "Recording paused", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Toast.makeText(requireContext(), "Error pausing recording", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void resumeRecording() {
        isRecordingPaused = false;
        startRecording();
    }

    private void updateUIForPausedRecording() {
        if (buttonRecord != null) {
            buttonRecord.setImageResource(R.drawable.micwhite);
        }

        if (textViewRecordingStatus != null) {
            textViewRecordingStatus.setText("Recording paused. Tap to resume.");
            textViewRecordingStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.bgred));
        }

        if (buttonPlayPauseout != null) {
            buttonPlayPauseout.setEnabled(true);
        }

        if (buttonRecord1 != null) {
            buttonRecord1.setImageResource(R.drawable.micwhite);
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;

                if (currentSegmentPath != null) {
                    recordedSegments.add(currentSegmentPath);
                    currentSegmentPath = null;
                }

            } catch (Exception e) {
                Log.e(TAG, "stopRecording: Error stopping recording", e);
            }
        }

        combineRecordedSegments();

        isRecording = false;
        isRecordingPaused = false;
        stopTimer();
        stopWaveformAnimation();

        updateUIAfterRecording();
    }

    private void combineRecordedSegments() {
        if (recordedSegments.isEmpty()) {
            recordedFilePath = null;
            return;
        }

        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            File outputFile = new File(storageDir, "final_recording_" + timeStamp + ".mp3");
            recordedFilePath = outputFile.getAbsolutePath();

            if (recordedSegments.size() == 1) {
                File segmentFile = new File(recordedSegments.get(0));
                if (segmentFile.renameTo(outputFile)) {
                    recordedSegments.clear();
                } else {
                    copyFile(segmentFile, outputFile);
                    recordedSegments.clear();
                }
                return;
            }

            File firstSegment = new File(recordedSegments.get(0));
            if (firstSegment.exists()) {
                copyFile(firstSegment, outputFile);
            }

            for (String segmentPath : recordedSegments) {
                File segmentFile = new File(segmentPath);
                if (segmentFile.exists() && !segmentFile.getAbsolutePath().equals(outputFile.getAbsolutePath())) {
                    segmentFile.delete();
                }
            }

            recordedSegments.clear();

        } catch (Exception e) {
            Log.e(TAG, "combineRecordedSegments: Error", e);
            Toast.makeText(requireContext(), "Error combining recordings", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyFile(File source, File dest) throws IOException {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    private void cleanupAfterFailedRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "cleanupAfterFailedRecording: Error releasing mediaRecorder", e);
            }
            mediaRecorder = null;
        }
        isRecording = false;
        stopTimer();
        stopWaveformAnimation();
        updateUIAfterRecording();
    }

    private void startPlaying() {
        if (recordedFilePath == null) {
            Toast.makeText(requireContext(), "No recording to play", Toast.LENGTH_SHORT).show();
            return;
        }

        File recordingFile = new File(recordedFilePath);
        if (!recordingFile.exists() || recordingFile.length() == 0) {
            Toast.makeText(requireContext(), "No valid recording to play", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            mediaPlayer = new MediaPlayer();

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                isPlaying = false;
                updateUIAfterPlaying();
                playWithAudioAttributes();
                return true;
            });

            mediaPlayer.setDataSource(recordedFilePath);

            mediaPlayer.setOnPreparedListener(mp -> {
                if (waveformSeekBar != null) {
                    waveformSeekBar.setMaxProgress(mp.getDuration());
                }

                if (totalTime != null) {
                    totalTime.setText(formatTime(mp.getDuration()));
                }

                mp.start();
                isPlaying = true;
                updateUIForPlaying();
                startSeekBarUpdate();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                updateUIAfterPlaying();
                stopSeekBarUpdate();
                if (waveformSeekBar != null) {
                    waveformSeekBar.setProgress(0);
                }
                if (playingTime != null) {
                    playingTime.setText("0:00");
                }
                if (buttonPlayPauseout != null) {
                    buttonPlayPauseout.setImageResource(R.drawable.playwhite);
                }
            });

            mediaPlayer.prepareAsync();

        } catch (IOException e) {
            Toast.makeText(requireContext(), "Failed to play recording", Toast.LENGTH_SHORT).show();
            isPlaying = false;
            updateUIAfterPlaying();
        }
    }

    private void playWithAudioAttributes() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            mediaPlayer = new MediaPlayer();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                android.media.AudioAttributes audioAttributes = new android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                mediaPlayer.setAudioAttributes(audioAttributes);
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }

            mediaPlayer.setDataSource(recordedFilePath);

            mediaPlayer.setOnPreparedListener(mp -> {
                if (waveformSeekBar != null) {
                    waveformSeekBar.setMaxProgress(mp.getDuration());
                }
                if (totalTime != null) {
                    totalTime.setText(formatTime(mp.getDuration()));
                }
                mp.start();
                isPlaying = true;
                updateUIForPlaying();
                startSeekBarUpdate();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                updateUIAfterPlaying();
                stopSeekBarUpdate();
                if (waveformSeekBar != null) {
                    waveformSeekBar.setProgress(0);
                }
                if (playingTime != null) {
                    playingTime.setText("0:00");
                }
            });

            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Audio playback failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void pausePlaying() {
        if (mediaPlayer != null && isPlaying) {
            try {
                mediaPlayer.pause();
                isPlaying = false;
                stopSeekBarUpdate();
                if (buttonPlayPauseout != null) {
                    buttonPlayPauseout.setImageResource(R.drawable.playwhite);
                }
            } catch (Exception e) {
                Log.e(TAG, "pausePlaying: Error pausing playback", e);
            }
        }
    }

    private void stopPlaying() {
        if (mediaPlayer != null) {
            if (isPlaying) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            isPlaying = false;
            stopSeekBarUpdate();
            if (waveformSeekBar != null) {
                waveformSeekBar.setProgress(0);
            }
            if (playingTime != null) {
                playingTime.setText("0:00");
            }
            updateUIAfterPlaying();
        }
    }

    private void startTimer() {
        stopTimer();

        timer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                recordingTime += 1000;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
            }
        }.start();

        isTimerRunning = true;
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        isTimerRunning = false;
    }

    private void updateTimerText() {
        long hours = TimeUnit.MILLISECONDS.toHours(recordingTime);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(recordingTime) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(recordingTime) % 60;

        String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        if (textViewTimer != null) {
            textViewTimer.setText(time);
        }
    }

    private void updateUIForRecording() {
        if (buttonRecord != null) {
            buttonRecord.setImageResource(R.drawable.pause);
        }

        if (textViewRecordingStatus != null) {
            textViewRecordingStatus.setText("Recording...");
            textViewRecordingStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
        }

        if (buttonPlayPauseout != null) {
            buttonPlayPauseout.setEnabled(true);
        }

        if (buttonStop != null) {
            buttonStop.setEnabled(true);
        }

        if (buttonRecord1 != null) {
            buttonRecord1.setImageResource(R.drawable.pause);
        }
    }

    private void updateUIAfterRecording() {
        if (buttonRecord != null) {
            buttonRecord.setImageResource(R.drawable.micwhite);
        }

        if (textViewRecordingStatus != null) {
            if (recordedFilePath != null && new File(recordedFilePath).exists()) {
                File file = new File(recordedFilePath);
                textViewRecordingStatus.setText("Recording saved (" + file.length() + " bytes). Ready to play.");
            } else {
                textViewRecordingStatus.setText("Ready to record");
            }
            textViewRecordingStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));
        }

        if (buttonPlayPauseout != null) {
            if (recordedFilePath != null && new File(recordedFilePath).exists()) {
                buttonPlayPauseout.setEnabled(true);
                buttonPlayPauseout.setAlpha(1.0f);
            } else {
                buttonPlayPauseout.setEnabled(false);
                buttonPlayPauseout.setAlpha(0.5f);
            }
        }

        if (buttonRecord1 != null) {
            buttonRecord1.setImageResource(R.drawable.micwhite);
        }

        isRecordingPaused = false;
    }

    private void updateUIForPlaying() {
        if (buttonPlayPauseout != null) {
            buttonPlayPauseout.setImageResource(R.drawable.pause);
        }

        if (textViewRecordingStatus != null) {
            textViewRecordingStatus.setText("Playing...");
            textViewRecordingStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue));
        }

        if (buttonRecord != null) {
            buttonRecord.setEnabled(false);
        }

        if (buttonRecord1 != null) {
            buttonRecord1.setEnabled(false);
        }
    }

    private void updateUIAfterPlaying() {
        if (buttonPlayPauseout != null) {
            buttonPlayPauseout.setImageResource(R.drawable.playwhite);
        }

        if (textViewRecordingStatus != null) {
            textViewRecordingStatus.setText("Ready to record");
            textViewRecordingStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
        }

        if (buttonRecord != null) {
            buttonRecord.setEnabled(true);
        }

        if (buttonRecord1 != null) {
            buttonRecord1.setEnabled(true);
        }
    }

    private void openImagePicker() {
        try {
            isPausingForImagePicker = true;

            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Cannot open image picker", Toast.LENGTH_SHORT).show();
            isPausingForImagePicker = false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK
                && data != null && data.getData() != null) {
            selectedImageUri = data.getData();

            if (imageViewPreview != null) {
                imageViewPreview.setImageURI(selectedImageUri);
                imageViewPreview.setVisibility(View.VISIBLE);
            }

            if (imguploadlayout != null) {
                imguploadlayout.setVisibility(View.GONE);
            }

            if (buttonDeleteImage != null) {
                buttonDeleteImage.setVisibility(View.VISIBLE);
            }

            checkAndShowAddButton();
        }

        isPausingForImagePicker = false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Audio permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Audio permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Storage permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startWaveformAnimation() {
        stopWaveformAnimation();

        waveformRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    animateWaveform();
                    waveformHandler.postDelayed(this, 100);
                }
            }
        };

        waveformHandler.post(waveformRunnable);
    }

    private void stopWaveformAnimation() {
        if (waveformHandler != null && waveformRunnable != null) {
            waveformHandler.removeCallbacks(waveformRunnable);
        }
    }

    private void animateWaveform() {
        if (waveformView != null && isRecording) {
            float amplitude = (float) (Math.random() * 50);
            waveformView.addAmplitude(amplitude);

            if (waveformSeekBar != null) {
                waveformSeekBar.addAmplitude(amplitude);
            }
        }
    }

    private void cleanupResources(boolean fullCleanup) {
        stopTimer();

        if (isRecording) {
            stopRecording();
        }

        if (fullCleanup && isPlaying) {
            stopPlaying();
        }

        stopWaveformAnimation();
        stopSeekBarUpdate();

        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "cleanupResources: Error releasing mediaRecorder", e);
            }
            mediaRecorder = null;
        }

        if (fullCleanup && mediaPlayer != null) {
            try {
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                Log.e(TAG, "cleanupResources: Error releasing mediaPlayer", e);
            }
        }

        isRecordingPaused = false;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (isPausingForImagePicker) {
            cleanupResources(false);
        } else {
            cleanupResources(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        isPausingForImagePicker = false;

        if (isPlaying) {
            updateUIForPlaying();
        } else if (recordedFilePath != null && new File(recordedFilePath).exists()) {
            updateUIAfterRecording();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (!isPausingForImagePicker) {
            cleanupResources(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanupResources(true);
    }
}