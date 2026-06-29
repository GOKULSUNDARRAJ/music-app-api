package com.Saalai.SalaiMusicApp;


import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
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
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.Saalai.SalaiMusicApp.Adapters.AlbumArtAdapter;
import com.bumptech.glide.Glide;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class PlayerBottomSheetFragmentwithslider extends BottomSheetDialogFragment {

    private static final String TAG = "FullAudioPlayerBS";

    private TextView tvSongName, tvSongArtist, tvCurrentTime, tvDuration;
    private ImageView btnPlayPause, btnNext, btnPrev, close, menu, btnRepeat, btnShuffle;
    private ProgressBar progressBar;
    private SeekBar seekBar;
    private ViewPager2 albumArtViewPager;
    private LinearLayout pageIndicator;

    private AlbumArtAdapter albumArtAdapter;
    private List<AudioModel> audioList = new ArrayList<>();
    private int currentViewPagerPosition = 0;
    private PlayerManager.PlaylistType currentPlaylistType;

    private Handler handler = new Handler();
    private Runnable updateSeekBarRunnable;

    private View rootView;
    private boolean isViewPagerInitialized = false;

    private boolean isViewPagerChangingByUser = true;
    private boolean shouldAutoPlayFromViewPager = true;

    // Broadcast receiver for play/pause updates
    private final BroadcastReceiver playerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Broadcast received: " + action);

            if ("UPDATE_MINI_PLAYER".equals(action) ||
                    "UPDATE_AUDIO_ADAPTER".equals(action)) {
                safeUpdateUI();
                updatePlayPauseButton();
                updatePlaylistData();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottomsheet_audio_player_slider, container, false);
        rootView = view;
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        initViews(view);

        // Setup UI components
        setupUI();

        // Setup ViewPager
        setupViewPager();

        // Apply edge-to-edge design
        applyEdgeToEdge();

        // Register broadcast receiver
        registerBroadcastReceiver();

        // Log current playing info
        PlayerManager.logCurrentPlayingInfo();
    }

    private void initViews(View view) {
        tvSongName = view.findViewById(R.id.tvFullPlayerSongName);
        tvSongArtist = view.findViewById(R.id.tvFullPlayerSongArtist);
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvDuration = view.findViewById(R.id.tvDuration);

        btnPlayPause = view.findViewById(R.id.btnFullPlayPause);
        btnNext = view.findViewById(R.id.btnFullNext);
        btnPrev = view.findViewById(R.id.btnFullPrev);
        close = view.findViewById(R.id.close);
        menu = view.findViewById(R.id.menu);
        progressBar = view.findViewById(R.id.progressBarLoading);
        seekBar = view.findViewById(R.id.seekBar);

        albumArtViewPager = view.findViewById(R.id.albumArtViewPager);
        pageIndicator = view.findViewById(R.id.pageIndicator);

        // Optional buttons (if they exist in your layout)
        btnRepeat = view.findViewById(R.id.btnRepeat);
        btnShuffle = view.findViewById(R.id.btnShuffle);

        // Set close button listener
        close.setOnClickListener(v -> dismiss());
    }

    private void setupViewPager() {
        // Get current playlist type
        currentPlaylistType = PlayerManager.getCurrentPlaylistType();
        Log.d(TAG, "Current playlist type: " + currentPlaylistType);

        // Get the current playlist based on type
        if (currentPlaylistType == PlayerManager.PlaylistType.ONLINE) {
            audioList = PlayerManager.getInstance().getOnlinePlaylist();
        } else if (currentPlaylistType == PlayerManager.PlaylistType.OFFLINE) {
            audioList = PlayerManager.getInstance().getOfflinePlaylist();
        }

        if (audioList == null || audioList.isEmpty()) {
            Log.w(TAG, "No audio list available for ViewPager");

            // Try to get the current playlist from PlayerManager
            List<AudioModel> currentPlaylist = PlayerManager.getCurrentPlaylist();
            if (currentPlaylist != null && !currentPlaylist.isEmpty()) {
                audioList = currentPlaylist;
                Log.d(TAG, "Using current playlist from PlayerManager with " + audioList.size() + " items");
            } else {
                // Create a single item list with current audio
                AudioModel currentAudio = PlayerManager.getCurrentAudio();
                if (currentAudio != null) {
                    audioList = new ArrayList<>();
                    audioList.add(currentAudio);
                    Log.d(TAG, "Created single item list with current audio");
                } else {
                    Log.e(TAG, "No audio available for ViewPager");
                    return;
                }
            }
        }

        // Find current position
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        currentViewPagerPosition = 0; // Default to first position

        if (currentAudio != null && audioList != null && !audioList.isEmpty()) {
            // Try to find the exact match
            for (int i = 0; i < audioList.size(); i++) {
                AudioModel audio = audioList.get(i);
                if (audio.getAudioUrl().equals(currentAudio.getAudioUrl()) ||
                        (audio.getDownloadPath() != null && currentAudio.getDownloadPath() != null &&
                                audio.getDownloadPath().equals(currentAudio.getDownloadPath()))) {
                    currentViewPagerPosition = i;
                    break;
                }
            }
            Log.d(TAG, "Found current audio at position: " + currentViewPagerPosition);
        }

        Log.d(TAG, "Setting up ViewPager with " + audioList.size() + " items, current position: " + currentViewPagerPosition);

        // Debug: Log all songs in the playlist
        for (int i = 0; i < audioList.size(); i++) {
            AudioModel audio = audioList.get(i);
            Log.d(TAG, "Song [" + i + "]: " + audio.getAudioName() +
                    " | Downloaded: " + audio.isDownloaded() +
                    " | URL: " + (audio.getAudioUrl() != null ? audio.getAudioUrl().substring(0, Math.min(50, audio.getAudioUrl().length())) : "null"));
        }

        // Setup adapter
        albumArtAdapter = new AlbumArtAdapter(audioList, currentViewPagerPosition);
        albumArtViewPager.setAdapter(albumArtAdapter);

        // Set current position
        albumArtViewPager.setCurrentItem(currentViewPagerPosition, false);

        // Add page transformer for better UX
        CompositePageTransformer transformer = new CompositePageTransformer();
        transformer.addTransformer(new MarginPageTransformer(dpToPx(16)));
        transformer.addTransformer((page, position) -> {
            float r = 1 - Math.abs(position);
            page.setScaleY(0.85f + r * 0.15f);
        });
        albumArtViewPager.setPageTransformer(transformer);

        // Set orientation to horizontal
        albumArtViewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

        // Reduce offscreen page limit for better performance
        albumArtViewPager.setOffscreenPageLimit(2);

        // Enable user input
        albumArtViewPager.setUserInputEnabled(true);

        // Setup page change callback
        albumArtViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (audioList == null || position >= audioList.size()) return;

                Log.d(TAG, "ViewPager page selected: " + position);

                // Update current position
                currentViewPagerPosition = position;

                // Update indicators
                updatePageIndicators(position);

                // Only auto-play if triggered by user swipe
                if (shouldAutoPlayFromViewPager && isViewPagerChangingByUser) {
                    // Get the selected audio
                    AudioModel selectedAudio = audioList.get(position);
                    AudioModel currentAudio = PlayerManager.getCurrentAudio();

                    // Check if this is a different audio than what's currently playing
                    if (currentAudio == null ||
                            !selectedAudio.getAudioUrl().equals(currentAudio.getAudioUrl()) &&
                                    !(selectedAudio.getDownloadPath() != null && currentAudio.getDownloadPath() != null &&
                                            selectedAudio.getDownloadPath().equals(currentAudio.getDownloadPath()))) {

                        Log.d(TAG, "Playing new audio from ViewPager: " + selectedAudio.getAudioName());
                        Log.d(TAG, "Selected audio download status: " + selectedAudio.isDownloaded());

                        // Show loading
                        showLoading();

                        // Determine how to play based on download status
                        if (selectedAudio.isDownloaded() && selectedAudio.getDownloadPath() != null) {
                            // Play offline audio
                            PlayerManager.playOfflineAudio(selectedAudio, new Runnable() {
                                @Override
                                public void run() {
                                    hideLoading();
                                    safeUpdateUI();
                                    restartSeekBarUpdater();
                                    broadcastUpdate();
                                }
                            });
                        } else {
                            // Play online audio
                            PlayerManager.playAudio(selectedAudio, new Runnable() {
                                @Override
                                public void run() {
                                    hideLoading();
                                    safeUpdateUI();
                                    restartSeekBarUpdater();
                                    broadcastUpdate();
                                }
                            });
                        }
                    } else {
                        Log.d(TAG, "Same audio selected, no action needed");
                    }
                }

                // Reset the flag
                isViewPagerChangingByUser = true;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                // Detect if user is scrolling
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    isViewPagerChangingByUser = true;
                } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    // ViewPager has settled
                }
            }
        });


        // Setup page indicators
        setupPageIndicators();

        isViewPagerInitialized = true;
    }

    private void updatePlaylistData() {
        // Update the audio list based on current playlist type
        PlayerManager.PlaylistType newType = PlayerManager.getCurrentPlaylistType();

        if (newType != currentPlaylistType) {
            currentPlaylistType = newType;
            Log.d(TAG, "Playlist type changed to: " + currentPlaylistType);

            // Get new playlist
            if (currentPlaylistType == PlayerManager.PlaylistType.ONLINE) {
                audioList = PlayerManager.getInstance().getOnlinePlaylist();
            } else {
                audioList = PlayerManager.getInstance().getOfflinePlaylist();
            }

            // Update adapter
            if (albumArtAdapter != null && audioList != null) {
                albumArtAdapter.setAudioList(audioList);
                setupPageIndicators();

                // Update current position
                AudioModel currentAudio = PlayerManager.getCurrentAudio();
                if (currentAudio != null && audioList != null) {
                    int newPosition = -1;
                    for (int i = 0; i < audioList.size(); i++) {
                        AudioModel audio = audioList.get(i);
                        if (audio.getAudioUrl().equals(currentAudio.getAudioUrl()) ||
                                (audio.getDownloadPath() != null && currentAudio.getDownloadPath() != null &&
                                        audio.getDownloadPath().equals(currentAudio.getDownloadPath()))) {
                            newPosition = i;
                            break;
                        }
                    }

                    if (newPosition >= 0 && newPosition != currentViewPagerPosition) {
                        currentViewPagerPosition = newPosition;
                        if (albumArtViewPager != null) {
                            albumArtViewPager.setCurrentItem(newPosition, false);
                        }
                        updatePageIndicators(newPosition);
                    }
                }
            }
        }
    }

    private void setupPageIndicators() {
        if (audioList == null || audioList.isEmpty() || pageIndicator == null) return;

        pageIndicator.removeAllViews();

        for (int i = 0; i < audioList.size(); i++) {
            ImageView dot = new ImageView(requireContext());
            dot.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.indicator_dot));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dpToPx(8), dpToPx(8)
            );
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            dot.setLayoutParams(params);

            pageIndicator.addView(dot);
        }

        updatePageIndicators(currentViewPagerPosition);
    }

    private void updatePageIndicators(int selectedPosition) {
        if (pageIndicator == null || pageIndicator.getChildCount() == 0) return;

        for (int i = 0; i < pageIndicator.getChildCount(); i++) {
            ImageView dot = (ImageView) pageIndicator.getChildAt(i);
            if (i == selectedPosition) {
                dot.setAlpha(1.0f);
                dot.setScaleX(1.2f);
                dot.setScaleY(1.2f);
                dot.setColorFilter(ContextCompat.getColor(requireContext(), R.color.yellow));
            } else {
                dot.setAlpha(0.5f);
                dot.setScaleX(1.0f);
                dot.setScaleY(1.0f);
                dot.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
            }
        }
    }

    private int dpToPx(int dp) {
        if (getContext() == null) return dp;
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void setupUI() {
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio == null) {
            Log.w(TAG, "No current audio found in setupUI");
            return;
        }

        // Initial UI update
        safeUpdateUI();

        // Play/Pause button
        btnPlayPause.setOnClickListener(v -> {
            PlayerManager.handlePlayPause();
            updatePlayPauseButton();
            broadcastUpdate();
        });

        // Next button
        btnNext.setOnClickListener(v -> {
            Log.d(TAG, "Next button clicked");
            showLoading();

            PlayerManager.playNext(new Runnable() {
                @Override
                public void run() {
                    hideLoading();
                    safeUpdateUI();
                    restartSeekBarUpdater();
                    broadcastUpdate();
                    updateViewPagerToCurrentAudio();
                }
            });
        });

        // Previous button
        btnPrev.setOnClickListener(v -> {
            Log.d(TAG, "Previous button clicked");
            showLoading();

            PlayerManager.playPrevious(new Runnable() {
                @Override
                public void run() {
                    hideLoading();
                    safeUpdateUI();
                    restartSeekBarUpdater();
                    broadcastUpdate();
                    updateViewPagerToCurrentAudio();
                }
            });
        });

        // Setup SeekBar
        setupSeekBar();

        // Listen for audio changes from PlayerManager
        PlayerManager.getInstance().setOnAudioChangedListener(newAudio -> {
            Log.d(TAG, "Audio changed in PlayerManager: " + (newAudio != null ? newAudio.getAudioName() : "null"));

            // Update ViewPager position when audio changes externally
            updateViewPagerToCurrentAudio();

            safeUpdateUI();
            restartSeekBarUpdater();
        });

        // Setup media player listeners
        if (PlayerManager.getPlayer() != null) {
            PlayerManager.getPlayer().setOnBufferingUpdateListener((mp, percent) -> {
                Log.d(TAG, "Buffering: " + percent + "%");
                if (percent < 100) {
                    showLoading();
                } else {
                    hideLoading();
                }
            });

            PlayerManager.getPlayer().setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "Playback error - what: " + what + ", extra: " + extra);
                hideLoading();
                if (isAdded()) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Playback error occurred", Toast.LENGTH_SHORT).show()
                    );
                }
                return true;
            });

            PlayerManager.getPlayer().setOnPreparedListener(mp -> {
                Log.d(TAG, "MediaPlayer prepared");
                hideLoading();
                seekBar.setMax(mp.getDuration());
                tvDuration.setText(formatTime(mp.getDuration()));
                updatePlayPauseButton();
                restartSeekBarUpdater();

                // Start playback if it was playing before
                if (PlayerManager.isPlaying()) {
                    mp.start();
                }
            });

            // FIX: Add a flag to prevent multiple completion triggers
            PlayerManager.getPlayer().setOnCompletionListener(mp -> {
                Log.d(TAG, "Playback completed, moving to next");

                // Add a small delay and check if we're still at the end
                new Handler().postDelayed(() -> {
                    if (PlayerManager.getPlayer() != null &&
                            !PlayerManager.getPlayer().isPlaying() &&
                            PlayerManager.getPlayer().getCurrentPosition() >= PlayerManager.getPlayer().getDuration() - 100) {

                        Log.d(TAG, "Confirming playback completed, playing next song");
                        PlayerManager.playNext(() -> {
                            safeUpdateUI();
                            restartSeekBarUpdater();
                            broadcastUpdate();
                            updateViewPagerToCurrentAudio();
                        });
                    }
                }, 100);
            });
        }

        // Menu button
        menu.setOnClickListener(v -> {
            if (getContext() instanceof AppCompatActivity) {
                AppCompatActivity activity = (AppCompatActivity) getContext();
                AudioModel current = PlayerManager.getCurrentAudio();

                if (current != null) {
                    MenuBottomSheetFragment bottomSheetFragment = new MenuBottomSheetFragment();

                    Bundle args = new Bundle();
                    args.putString("audioName", current.getAudioName());
                    args.putString("audioUrl", current.getAudioUrl());
                    args.putString("artistName", current.getcategoryName());
                    args.putString("imageUrl", current.getImageUrl());
                    args.putString("downloadPath", current.getDownloadPath());
                    args.putBoolean("isDownloaded", current.isDownloaded());
                    bottomSheetFragment.setArguments(args);

                    bottomSheetFragment.show(activity.getSupportFragmentManager(), "MenuBottomSheet");
                }
            }
        });

        // Optional: Repeat button
        if (btnRepeat != null) {
            btnRepeat.setOnClickListener(v -> {
                // Implement repeat functionality
                Toast.makeText(getContext(), "Repeat toggled", Toast.LENGTH_SHORT).show();
            });
        }

        // Optional: Shuffle button
        if (btnShuffle != null) {
            btnShuffle.setOnClickListener(v -> {
                // Implement shuffle functionality
                Toast.makeText(getContext(), "Shuffle toggled", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void updateViewPagerToCurrentAudio() {
        if (!isViewPagerInitialized || audioList == null || audioList.isEmpty()) return;

        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio == null) return;

        // Find the current audio in the list
        int newPosition = -1;
        for (int i = 0; i < audioList.size(); i++) {
            AudioModel audio = audioList.get(i);
            if (audio.getAudioUrl().equals(currentAudio.getAudioUrl()) ||
                    (audio.getDownloadPath() != null && currentAudio.getDownloadPath() != null &&
                            audio.getDownloadPath().equals(currentAudio.getDownloadPath()))) {
                newPosition = i;
                break;
            }
        }

        if (newPosition >= 0 && newPosition != currentViewPagerPosition) {
            // Temporarily disable auto-play when updating programmatically
            shouldAutoPlayFromViewPager = false;
            currentViewPagerPosition = newPosition;
            if (albumArtViewPager != null) {
                albumArtViewPager.setCurrentItem(newPosition, false);
            }
            updatePageIndicators(newPosition);
            Log.d(TAG, "ViewPager updated to position: " + newPosition);

            // Re-enable auto-play after a short delay
            new Handler().postDelayed(() -> {
                shouldAutoPlayFromViewPager = true;
            }, 300);
        }
    }

    // Helper method to update play/pause button
    private void updatePlayPauseButton() {
        if (!isAdded() || getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (PlayerManager.getPlayer() != null) {
                int iconRes = PlayerManager.isPlaying() ? R.drawable.pause : R.drawable.play_player;
                btnPlayPause.setImageResource(iconRes);
            }
        });
    }

    private void restartSeekBarUpdater() {
        if (updateSeekBarRunnable != null) {
            handler.removeCallbacks(updateSeekBarRunnable);
        }
        setupSeekBar();
    }

    private void showLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (btnPlayPause != null) {
            btnPlayPause.setVisibility(View.GONE);
        }
    }

    private void hideLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (btnPlayPause != null) {
            btnPlayPause.setVisibility(View.VISIBLE);
        }
    }

    private void safeUpdateUI() {
        if (!isAdded() || getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            try {
                updateUIForCurrentAudio();
            } catch (Exception e) {
                Log.e(TAG, "Error updating UI", e);
            }
        });
    }

    private void updateUIForCurrentAudio() {
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio == null) {
            Log.w(TAG, "No current audio to update UI");
            return;
        }

        Log.d(TAG, "Updating UI for: " + currentAudio.getAudioName());

        if (tvSongName != null) {
            tvSongName.setText(currentAudio.getAudioName());
        }

        if (tvSongArtist != null) {
            tvSongArtist.setText(currentAudio.getcategoryName() != null ?
                    currentAudio.getcategoryName() : "Unknown Artist");
        }

        updatePlayPauseButton();

        if (PlayerManager.getPlayer() != null) {
            if (seekBar != null) {
                seekBar.setMax(PlayerManager.getPlayer().getDuration());
            }
            if (tvDuration != null) {
                tvDuration.setText(formatTime(PlayerManager.getPlayer().getDuration()));
            }
        }

        loadAlbumArtWithPalette(currentAudio);

        // Update playlist type indicator
        currentPlaylistType = PlayerManager.getCurrentPlaylistType();
        Log.d(TAG, "Current playlist type in UI: " + currentPlaylistType);
    }

    private void loadAlbumArtWithPalette(AudioModel audio) {
        if (audio.getImageUrl() != null && !audio.getImageUrl().isEmpty() && rootView != null) {
            Picasso.get()
                    .load(audio.getImageUrl())
                    .into(new com.squareup.picasso.Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                            if (bitmap != null && rootView != null) {
                                Palette.from(bitmap).generate(palette -> {
                                    int darkColor = palette.getDarkVibrantColor(0xFF000000);
                                    if (rootView != null) {
                                        rootView.setBackground(new android.graphics.drawable.GradientDrawable(
                                                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                                                new int[]{darkColor, 0xFF121212}));
                                    }
                                });
                            }
                        }

                        @Override
                        public void onBitmapFailed(Exception e, android.graphics.drawable.Drawable errorDrawable) {
                            Log.e(TAG, "Failed to load album art for palette", e);
                        }

                        @Override
                        public void onPrepareLoad(android.graphics.drawable.Drawable placeHolderDrawable) {
                            // Do nothing
                        }
                    });
        }
    }

    private void setupSeekBar() {
        if (PlayerManager.getPlayer() == null) return;

        int currentPosition = PlayerManager.getPlayer().getCurrentPosition();
        int duration = PlayerManager.getPlayer().getDuration();

        if (seekBar != null) {
            seekBar.setMax(duration);
            seekBar.setProgress(currentPosition);
        }

        if (tvCurrentTime != null) {
            tvCurrentTime.setText(formatTime(currentPosition));
        }

        if (tvDuration != null) {
            tvDuration.setText(formatTime(duration));
        }

        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || PlayerManager.getPlayer() == null) return;

                try {
                    int currentPos = PlayerManager.getPlayer().getCurrentPosition();
                    if (seekBar != null) {
                        seekBar.setProgress(currentPos);
                    }
                    if (tvCurrentTime != null) {
                        tvCurrentTime.setText(formatTime(currentPos));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating seekbar", e);
                }

                handler.postDelayed(this, 500);
            }
        };
        handler.post(updateSeekBarRunnable);

        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && PlayerManager.getPlayer() != null) {
                        PlayerManager.getPlayer().seekTo(progress);
                        if (tvCurrentTime != null) {
                            tvCurrentTime.setText(formatTime(progress));
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // Pause seekbar updates while user is dragging
                    if (updateSeekBarRunnable != null) {
                        handler.removeCallbacks(updateSeekBarRunnable);
                    }
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // Resume seekbar updates
                    if (updateSeekBarRunnable != null) {
                        handler.post(updateSeekBarRunnable);
                    }
                }
            });
        }
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

    private void applyEdgeToEdge() {
        if (getDialog() != null && getDialog().getWindow() != null) {
            Window window = getDialog().getWindow();

            // Enable edge-to-edge
            WindowCompat.setDecorFitsSystemWindows(window, false);

            // Set transparent status bar
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(requireContext(), android.R.color.transparent));

            // Set transparent navigation bar
            window.setNavigationBarColor(ContextCompat.getColor(requireContext(), android.R.color.transparent));

            // For Android 8.0+ - set navigation bar icon colors
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int flags = window.getDecorView().getSystemUiVisibility();
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                window.getDecorView().setSystemUiVisibility(flags);
            }

            // Handle insets manually
            if (rootView != null) {
                ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                    int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                    int navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

                    // Apply padding to avoid system bars
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

    private void registerBroadcastReceiver() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireContext().registerReceiver(playerUpdateReceiver,
                        new IntentFilter("UPDATE_MINI_PLAYER"),
                        Context.RECEIVER_NOT_EXPORTED);
                requireContext().registerReceiver(playerUpdateReceiver,
                        new IntentFilter("UPDATE_AUDIO_ADAPTER"),
                        Context.RECEIVER_NOT_EXPORTED);
            } else {
                requireContext().registerReceiver(playerUpdateReceiver,
                        new IntentFilter("UPDATE_MINI_PLAYER"));
                requireContext().registerReceiver(playerUpdateReceiver,
                        new IntentFilter("UPDATE_AUDIO_ADAPTER"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error registering broadcast receiver", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume called");

        // Update playlist data
        updatePlaylistData();

        // Safe UI update
        safeUpdateUI();

        // Restart seekbar updater
        restartSeekBarUpdater();

        // Debug info
        PlayerManager.debugAllPlaylistInfo();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            requireContext().unregisterReceiver(playerUpdateReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver not registered", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called");

        if (updateSeekBarRunnable != null) {
            handler.removeCallbacks(updateSeekBarRunnable);
            handler.removeCallbacksAndMessages(null); // Remove all callbacks
        }

        // Clear all listeners
        if (PlayerManager.getPlayer() != null) {
            PlayerManager.getPlayer().setOnCompletionListener(null);
            PlayerManager.getPlayer().setOnPreparedListener(null);
            PlayerManager.getPlayer().setOnErrorListener(null);
            PlayerManager.getPlayer().setOnBufferingUpdateListener(null);
        }

        PlayerManager.getInstance().setOnAudioChangedListener(null);

        isViewPagerInitialized = false;
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

    private void checkAndFixPlayback() {
        if (PlayerManager.getPlayer() != null) {
            try {
                int currentPos = PlayerManager.getPlayer().getCurrentPosition();
                int duration = PlayerManager.getPlayer().getDuration();

                // If playback is stuck at the end
                if (currentPos >= duration - 100 && duration > 0) {
                    Log.d(TAG, "Playback stuck at end, resetting");
                    PlayerManager.getPlayer().seekTo(0);

                    // If it should be playing, restart it
                    if (PlayerManager.isPlaying()) {
                        PlayerManager.getPlayer().start();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking playback", e);
            }
        }
    }

    // Call this periodically or when you detect issues
    private void startPlaybackMonitor() {
        Handler monitorHandler = new Handler();
        Runnable monitorRunnable = new Runnable() {
            @Override
            public void run() {
                checkAndFixPlayback();
                monitorHandler.postDelayed(this, 1000); // Check every second
            }
        };
        monitorHandler.postDelayed(monitorRunnable, 1000);
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