package com.Saalai.SalaiMusicApp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import androidx.media.session.MediaButtonReceiver;

import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.Models.PlaylistModel;
import com.Saalai.SalaiMusicApp.Notification.NotificationHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlayerManager {

    private static final String TAG = "PlayerManager";
    private static PlayerManager instance;
    private static final Object lock = new Object();
    private static ClipModeController clipModeController;
    private static boolean isClipModeActive = false;
    private MediaPlayer mediaPlayer;
    private AudioModel currentAudio;
    private List<AudioModel> audioList;
    private int currentIndex = 0;
    private Context context;

    // Notification helper
    private static NotificationHelper notificationHelper;

    // Store both playlists
    private List<AudioModel> onlinePlaylist;
    private List<AudioModel> offlinePlaylist;
    private List<AudioModel> currentPlaylist; // The currently active playlist

    // Bluetooth Control Components
    private MediaSessionCompat mediaSession;
    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusListener;
    private ComponentName mediaButtonReceiverComponent;
    private Handler mainHandler;
    private BroadcastReceiver mediaButtonReceiver;
    private boolean isReceiverRegistered = false;
    private Handler positionHandler;
    private Runnable positionRunnable;
    private long currentPosition = 0;

    public static final String ACTION_PREPARE_NEXT = "PREPARE_NEXT";
    public static final String ACTION_PREPARE_PREVIOUS = "PREPARE_PREVIOUS";

    // Add these fields at the class level
    private static boolean isPlayingNext = false;
    private static boolean isPlayingPrevious = false;
    private static Handler playHandler = new Handler(Looper.getMainLooper());
    private static long lastNextCallTime = 0;
    private static long lastPrevCallTime = 0;
    private static final long CALL_DELAY = 1000; // 2 seconds between calls

    private String currentSongId = "";

    // Listener for UI updates
    public enum PlaylistType {
        ONLINE, OFFLINE
    }

    private PlaylistType currentPlaylistType = PlaylistType.ONLINE;

    public interface OnAudioChangedListener {
        void onAudioChanged(AudioModel newAudio);
    }

    private OnAudioChangedListener listener;

    public void setOnAudioChangedListener(OnAudioChangedListener listener) {
        this.listener = listener;
    }

    private void notifyAudioChanged() {
        if (listener != null && currentAudio != null) {
            listener.onAudioChanged(currentAudio);
        }
    }

    // Private constructor
    private PlayerManager(Context ctx) {
        this.context = ctx.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.positionHandler = new Handler(Looper.getMainLooper());

        // Initialize MediaPlayer
        mediaPlayer = new MediaPlayer();

        // Set audio attributes based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );
        } else {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }

        // Set completion listener
        mediaPlayer.setOnCompletionListener(mp -> {
            Log.d(TAG, "Song finished, playing next...");
            mainHandler.post(() -> {
                playNext(null);
            });
        });

        // Set error listener
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer error - what: " + what + ", extra: " + extra);

            if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                recreateMediaPlayer();
                return true;
            }

            if (what == MediaPlayer.MEDIA_ERROR_IO) {
                mainHandler.post(() -> playNext(null));
                return true;
            }

            return false;
        });

        // Initialize notification helper
        notificationHelper = new NotificationHelper(context);

        // Initialize Bluetooth components
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        initMediaSession();
        registerMediaButtonReceiver();
        initPositionTracking();

        Log.d(TAG, "PlayerManager initialized for Android " + Build.VERSION.SDK_INT);
    }

    // MediaSession initialization
    private void initMediaSession() {
        try {
            mediaSession = new MediaSessionCompat(context, "SalaiMusic");

            mediaSession.setCallback(new MediaSessionCompat.Callback() {
                @Override
                public void onPlay() {
                    Log.d(TAG, "MediaSession: onPlay");
                    handlePlayPause();
                }

                @Override
                public void onPause() {
                    Log.d(TAG, "MediaSession: onPause");
                    handlePlayPause();
                }

                @Override
                public void onSkipToNext() {
                    Log.d(TAG, "MediaSession: onSkipToNext");
                    playNext(null);
                }

                @Override
                public void onSkipToPrevious() {
                    Log.d(TAG, "MediaSession: onSkipToPrevious");
                    playPrevious(null);
                }

                @Override
                public void onStop() {
                    Log.d(TAG, "MediaSession: onStop");
                    stopPlayback();
                }

                @Override
                public void onSeekTo(long pos) {
                    Log.d(TAG, "MediaSession: onSeekTo " + pos);
                    if (mediaPlayer != null) {
                        mediaPlayer.seekTo((int) pos);
                        updatePlaybackState(isPlaying(), pos);
                    }
                }

                @Override
                public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                    KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                    if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                        handleKeyEvent(event);
                    }
                    return true;
                }
            });

            mediaSession.setFlags(
                    MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            );

            // Set initial playback state
            PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                    .setActions(
                            PlaybackStateCompat.ACTION_PLAY |
                                    PlaybackStateCompat.ACTION_PAUSE |
                                    PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                    PlaybackStateCompat.ACTION_STOP |
                                    PlaybackStateCompat.ACTION_SEEK_TO
                    )
                    .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f);

            mediaSession.setPlaybackState(stateBuilder.build());
            mediaSession.setActive(true);

            Log.d(TAG, "MediaSession initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MediaSession: " + e.getMessage());
        }
    }

    // Register media button receiver
    private void registerMediaButtonReceiver() {
        try {
            mediaButtonReceiverComponent = new ComponentName(context.getPackageName(),
                    "com.Saalai.SalaiMusicApp.MediaButtonReceiver");
            audioManager.registerMediaButtonEventReceiver(mediaButtonReceiverComponent);

            mediaButtonReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                        KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                        if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                            handleKeyEvent(event);
                            abortBroadcast();
                        }
                    }
                }
            };

            IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
            filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
            context.registerReceiver(mediaButtonReceiver, filter);
            isReceiverRegistered = true;

            Log.d(TAG, "Media button receiver registered");
        } catch (Exception e) {
            Log.e(TAG, "Error registering media button receiver: " + e.getMessage());
        }
    }

    // Public method for MediaButtonReceiver to call
    public void handleMediaButtonEvent(Intent intent) {
        KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
            handleKeyEvent(event);
        }
    }

    // Position tracking
    private void initPositionTracking() {
        positionRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying()) {
                    currentPosition = mediaPlayer.getCurrentPosition();
                    updatePlaybackState(true, currentPosition);
                    positionHandler.postDelayed(this, 1000);
                }
            }
        };
    }

    private void startPositionTracking() {
        positionHandler.removeCallbacks(positionRunnable);
        positionHandler.post(positionRunnable);
    }

    private void stopPositionTracking() {
        positionHandler.removeCallbacks(positionRunnable);
    }

    // Handle key events
    private void handleKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        Log.d(TAG, "Media button pressed: " + keyCode);

        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
                Log.d(TAG, "PLAY/PAUSE button pressed");
                mainHandler.post(() -> {
                    if (isPlaying()) {
                        pausePlayback();
                    } else {
                        resumePlayback();
                    }
                });
                break;

            case KeyEvent.KEYCODE_MEDIA_PLAY:
                Log.d(TAG, "PLAY button pressed");
                mainHandler.post(() -> {
                    if (!isPlaying()) {
                        resumePlayback();
                    }
                });
                break;

            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                Log.d(TAG, "PAUSE button pressed");
                mainHandler.post(() -> {
                    if (isPlaying()) {
                        pausePlayback();
                    }
                });
                break;

            case KeyEvent.KEYCODE_MEDIA_NEXT:
                Log.d(TAG, "NEXT button pressed");
                mainHandler.post(() -> playNext(null));
                break;

            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                Log.d(TAG, "PREVIOUS button pressed");
                mainHandler.post(() -> playPrevious(null));
                break;

            case KeyEvent.KEYCODE_MEDIA_STOP:
                Log.d(TAG, "STOP button pressed");
                mainHandler.post(() -> stopPlayback());
                break;

            default:
                Log.d(TAG, "Unhandled key code: " + keyCode);
                break;
        }
    }

    // Update playback state
    private void updatePlaybackState(boolean isPlaying, long position) {
        if (mediaSession == null) return;

        try {
            int state = isPlaying ?
                    PlaybackStateCompat.STATE_PLAYING :
                    PlaybackStateCompat.STATE_PAUSED;

            PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                    .setActions(
                            PlaybackStateCompat.ACTION_PLAY |
                                    PlaybackStateCompat.ACTION_PAUSE |
                                    PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                    PlaybackStateCompat.ACTION_STOP |
                                    PlaybackStateCompat.ACTION_SEEK_TO
                    )
                    .setState(state, position, 1.0f);

            mediaSession.setPlaybackState(stateBuilder.build());
        } catch (Exception e) {
            Log.e(TAG, "Error updating playback state: " + e.getMessage());
        }

        // Update notification
        if (currentAudio != null) {
            updateNotification(currentAudio, isPlaying);
        }
    }

    // Audio focus handling
    private boolean requestAudioFocus() {
        if (audioManager == null) return false;

        audioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        Log.d(TAG, "AudioFocus: GAIN");
                        // Resume playback or restore volume
                        if (mediaPlayer != null && !mediaPlayer.isPlaying() && currentAudio != null) {
                            // Don't auto-resume here - let user control
                        }
                        // Restore volume
                        if (mediaPlayer != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                mediaPlayer.setVolume(1.0f, 1.0f);
                            }
                        }
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS:
                        Log.d(TAG, "AudioFocus: LOSS");
                        // Lost focus for an unbounded amount of time
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            mediaPlayer.pause();
                            updatePlaybackState(false, mediaPlayer.getCurrentPosition());
                            stopPositionTracking();
                            updateNotification(currentAudio, false);
                        }
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        Log.d(TAG, "AudioFocus: LOSS_TRANSIENT");
                        // Lost focus temporarily
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            mediaPlayer.pause();
                            updatePlaybackState(false, mediaPlayer.getCurrentPosition());
                            stopPositionTracking();
                            updateNotification(currentAudio, false);
                        }
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        Log.d(TAG, "AudioFocus: LOSS_TRANSIENT_CAN_DUCK");
                        // Lost focus but can play at low volume
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                mediaPlayer.setVolume(0.1f, 0.1f);
                            }
                        }
                        break;
                }
            }
        };

        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.media.AudioFocusRequest focusRequest =
                    new android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                            .setAudioAttributes(
                                    new android.media.AudioAttributes.Builder()
                                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                                            .build()
                            )
                            .setOnAudioFocusChangeListener(audioFocusListener)
                            .build();
            result = audioManager.requestAudioFocus(focusRequest);
        } else {
            result = audioManager.requestAudioFocus(
                    audioFocusListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
            );
        }

        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void abandonAudioFocus() {
        if (audioManager != null && audioFocusListener != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.media.AudioFocusRequest focusRequest =
                        new android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                                .setOnAudioFocusChangeListener(audioFocusListener)
                                .build();
                audioManager.abandonAudioFocusRequest(focusRequest);
            } else {
                audioManager.abandonAudioFocus(audioFocusListener);
            }
        }
    }

    private void recreateMediaPlayer() {
        synchronized (lock) {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                );
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
        }
    }

    public static void init(Context ctx) {
        synchronized (lock) {
            if (instance == null) {
                instance = new PlayerManager(ctx);
            }
        }
    }

    public static PlayerManager getInstance() {
        synchronized (lock) {
            if (instance == null) {
                throw new IllegalStateException("PlayerManager not initialized. Call init() first.");
            }
            return instance;
        }
    }

    public static MediaPlayer getPlayer() {
        return getInstance().mediaPlayer;
    }

    public static AudioModel getCurrentAudio() {
        return getInstance().currentAudio;
    }

    public static boolean isPlaying() {
        PlayerManager manager = getInstance();
        return manager.mediaPlayer != null && manager.mediaPlayer.isPlaying();
    }

    public static void setAudioList(List<AudioModel> list, PlaylistType type) {
        PlayerManager manager = getInstance();

        Log.d(TAG, "setAudioList called - Type: " + type +
                ", New list size: " + (list != null ? list.size() : 0));

        if (type == PlaylistType.ONLINE) {
            manager.onlinePlaylist = list != null ? new ArrayList<>(list) : null;
            Log.d(TAG, "Updated ONLINE playlist with " + (list != null ? list.size() : 0) + " songs");
        } else if (type == PlaylistType.OFFLINE) {
            manager.offlinePlaylist = list != null ? new ArrayList<>(list) : null;
            Log.d(TAG, "Updated OFFLINE playlist with " + (list != null ? list.size() : 0) + " songs");

            if (manager.offlinePlaylist != null) {
                for (int i = 0; i < manager.offlinePlaylist.size(); i++) {
                    AudioModel song = manager.offlinePlaylist.get(i);
                    Log.d(TAG, "Offline playlist [" + i + "]: " + song.getAudioName() +
                            " | Downloaded: " + song.isDownloaded() +
                            " | Path: " + song.getDownloadPath());
                }
            }
        }

        if (manager.currentPlaylistType == type) {
            manager.currentPlaylist = list != null ? new ArrayList<>(list) : null;
            Log.d(TAG, "Updated current playlist to match type: " + type);
        }
    }

    public static PlaylistType getCurrentPlaylistType() {
        return getInstance().currentPlaylistType;
    }

    public static void switchToPlaylist(PlaylistType type) {
        PlayerManager manager = getInstance();
        manager.currentPlaylistType = type;

        if (type == PlaylistType.ONLINE && manager.onlinePlaylist != null) {
            manager.currentPlaylist = manager.onlinePlaylist;
        } else if (type == PlaylistType.OFFLINE && manager.offlinePlaylist != null) {
            manager.currentPlaylist = manager.offlinePlaylist;
        }

        if (manager.currentAudio != null && manager.currentPlaylist != null) {
            for (int i = 0; i < manager.currentPlaylist.size(); i++) {
                if (manager.currentPlaylist.get(i).getAudioUrl().equals(manager.currentAudio.getAudioUrl())) {
                    manager.currentIndex = i;
                    break;
                }
            }
        }
    }

    public static void playAudio(AudioModel audio, Runnable onPreparedCallback) {
        PlayerManager manager = getInstance();
        if (audio == null || audio.getAudioUrl() == null) return;

        // Request audio focus before playing
        if (!manager.requestAudioFocus()) {
            Log.e(TAG, "Could not get audio focus");
            return;
        }

        logCurrentPlayingInfo();
        boolean isOnlineSong = audio.getAudioUrl().startsWith("http");
        Log.d(TAG, "playAudio called for: " + audio.getAudioName() +
                ", URL type: " + (isOnlineSong ? "ONLINE" : "OFFLINE") +
                ", isDownloaded: " + audio.isDownloaded());

        manager.mediaPlayer.setOnErrorListener(null);

        PlaylistType expectedType = isOnlineSong ? PlaylistType.ONLINE : PlaylistType.OFFLINE;

        if (audio.isDownloaded() && audio.getDownloadPath() != null) {
            expectedType = PlaylistType.OFFLINE;
            Log.d(TAG, "Song is downloaded, treating as OFFLINE type");
        }

        List<AudioModel> targetPlaylist = expectedType == PlaylistType.ONLINE ? manager.onlinePlaylist : manager.offlinePlaylist;

        Log.d(TAG, "Target " + expectedType + " playlist: " +
                (targetPlaylist != null ? targetPlaylist.size() + " songs" : "null"));

        if (targetPlaylist == null || targetPlaylist.isEmpty()) {
            Log.e(TAG, "Target " + expectedType + " playlist is null or empty! Creating temporary playlist.");

            if (expectedType == PlaylistType.OFFLINE && manager.offlinePlaylist != null && !manager.offlinePlaylist.isEmpty()) {
                manager.currentPlaylist = new ArrayList<>(manager.offlinePlaylist);
                manager.currentPlaylistType = PlaylistType.OFFLINE;
                Log.d(TAG, "Using existing offline playlist with " + manager.offlinePlaylist.size() + " songs");
            } else {
                manager.currentPlaylist = new ArrayList<>();
                manager.currentPlaylist.add(audio);
                manager.currentPlaylistType = expectedType;
                Log.d(TAG, "Created new " + expectedType + " playlist with 1 song");
            }
            manager.currentIndex = 0;
        } else {
            int foundIndex = -1;
            for (int i = 0; i < targetPlaylist.size(); i++) {
                AudioModel song = targetPlaylist.get(i);
                if (song.getAudioUrl().equals(audio.getAudioUrl()) ||
                        (audio.getDownloadPath() != null && song.getDownloadPath() != null &&
                                song.getDownloadPath().equals(audio.getDownloadPath())) ||
                        song.getAudioName().equals(audio.getAudioName())) {
                    foundIndex = i;
                    break;
                }
            }

            if (foundIndex != -1) {
                manager.currentPlaylist = new ArrayList<>(targetPlaylist);
                manager.currentPlaylistType = expectedType;
                manager.currentIndex = foundIndex;
                Log.d(TAG, "Song found in " + expectedType + " playlist at index: " + foundIndex +
                        ", playlist size: " + targetPlaylist.size());
            } else {
                Log.e(TAG, "Song not found in " + expectedType + " playlist! Adding to playlist.");
                manager.currentPlaylist = new ArrayList<>(targetPlaylist);
                manager.currentPlaylist.add(audio);
                manager.currentPlaylistType = expectedType;
                manager.currentIndex = manager.currentPlaylist.size() - 1;
                Log.d(TAG, "Added song to " + expectedType + " playlist, new size: " + manager.currentPlaylist.size());
            }
        }

        if (expectedType == PlaylistType.OFFLINE && audio.isDownloaded() && audio.getDownloadPath() != null) {
            Log.d(TAG, "Playing downloaded song using offline audio method");
            manager.startOfflineAudio(audio, audio.getDownloadPath(), onPreparedCallback);
        } else {
            Log.d(TAG, "Playing song using regular audio method");
            manager.startAudio(audio, onPreparedCallback);
        }
    }

    private void startAudio(AudioModel audio, Runnable onPreparedCallback) {
        try {
            Log.d(TAG, "startAudio: " + audio.getAudioName());

            // Check if this is a different song
            checkForSongChange(audio);

            synchronized (lock) {
                if (mediaPlayer.isPlaying()) {
                    Log.d(TAG, "Stopping current playback");
                    mediaPlayer.stop();
                }

                mediaPlayer.reset();
                Log.d(TAG, "Setting data source: " + audio.getAudioUrl());
                mediaPlayer.setDataSource(audio.getAudioUrl());
            }

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error - what: " + what + ", extra: " + extra);
                abandonAudioFocus();
                return true;
            });

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "MediaPlayer prepared, starting playback");
                mp.start();
                currentAudio = audio;
                currentPosition = 0;

                // DON'T automatically restart clip mode for new song
                // Clip mode should be reset and user needs to activate it again

                // Update playback state for Bluetooth
                updatePlaybackState(true, 0);
                startPositionTracking();

                Log.d(TAG, "🎵 NOW PLAYING: " + audio.getAudioName());
                Log.d(TAG, "   Category ID: " + audio.getCategoryId());
                Log.d(TAG, "   Playlist ID: " + audio.getPlaylistId());
                Log.d(TAG, "   Is Downloaded: " + audio.isDownloaded());

                saveCurrentAudio();
                notifyAudioChanged();
                broadcastAudioChange();
                updateNotification(audio, true);

                mediaPlayer.setOnCompletionListener(m -> {
                    Log.d(TAG, "Song completed: " + audio.getAudioName());
                    playNext(null);
                });

                if (onPreparedCallback != null) onPreparedCallback.run();
            });

            Log.d(TAG, "Preparing async...");
            mediaPlayer.prepareAsync();

        } catch (IOException e) {
            Log.e(TAG, "IOException in startAudio: " + e.getMessage());
            abandonAudioFocus();
            e.printStackTrace();
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException in startAudio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void pausePlayback() {
        PlayerManager manager = getInstance();
        synchronized (lock) {
            MediaPlayer mp = manager.mediaPlayer;
            if (mp != null && mp.isPlaying()) {
                mp.pause();
                manager.currentPosition = mp.getCurrentPosition();

                // Pause clip mode if active
                if (clipModeController != null && clipModeController.isClipModeActive()) {
                    clipModeController.pauseClipMode();
                }

                // Update playback state for Bluetooth
                manager.updatePlaybackState(false, manager.currentPosition);
                manager.stopPositionTracking();
                manager.updateNotification(manager.currentAudio, false);

                Log.d(TAG, "Playback paused at position: " + manager.currentPosition);
            }
        }
        manager.broadcastAudioChange();
    }

    public static void resumePlayback() {
        PlayerManager manager = getInstance();
        synchronized (lock) {
            MediaPlayer mp = manager.mediaPlayer;
            if (mp != null && !mp.isPlaying() && manager.currentAudio != null) {
                // Don't request audio focus again - it should already have it
                mp.start();

                // Resume clip mode if active
                if (clipModeController != null && clipModeController.isClipModeActive() &&
                        clipModeController.isPaused()) {
                    clipModeController.resumeClipMode();
                }

                // Update playback state for Bluetooth
                manager.updatePlaybackState(true, manager.currentPosition);
                manager.startPositionTracking();
                manager.updateNotification(manager.currentAudio, true);

                Log.d(TAG, "Playback resumed");
            }
        }
        manager.broadcastAudioChange();
    }

    public static void stopPlayback() {
        PlayerManager manager = getInstance();
        synchronized (lock) {
            MediaPlayer mp = manager.mediaPlayer;
            if (mp != null) {
                if (mp.isPlaying()) mp.stop();
                mp.reset();

                // Stop clip mode
                if (clipModeController != null && clipModeController.isClipModeActive()) {
                    clipModeController.stopClipMode();
                }

                // Clean up audio focus and playback state
                manager.abandonAudioFocus();
                manager.updatePlaybackState(false, 0);
                manager.stopPositionTracking();

                manager.currentAudio = null;
                manager.broadcastAudioChange();
                manager.notifyAudioChanged();
                stopNotification();
            }
        }
    }

    public static void releasePlayer() {
        PlayerManager manager = getInstance();
        synchronized (lock) {
            // Release clip mode
            if (clipModeController != null) {
                clipModeController.release();
                clipModeController = null;
            }
            isClipModeActive = false;

            // Release MediaSession
            if (manager.mediaSession != null) {
                manager.mediaSession.setActive(false);
                manager.mediaSession.release();
                manager.mediaSession = null;
            }

            // Unregister receivers
            try {
                if (manager.isReceiverRegistered && manager.mediaButtonReceiver != null) {
                    manager.context.unregisterReceiver(manager.mediaButtonReceiver);
                    manager.isReceiverRegistered = false;
                }
                if (manager.mediaButtonReceiverComponent != null) {
                    manager.audioManager.unregisterMediaButtonEventReceiver(
                            manager.mediaButtonReceiverComponent
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver: " + e.getMessage());
            }

            // Abandon audio focus
            manager.abandonAudioFocus();

            // Stop position tracking
            manager.stopPositionTracking();

            // Release MediaPlayer
            MediaPlayer mp = manager.mediaPlayer;
            if (mp != null) {
                mp.release();
                manager.mediaPlayer = null;
                manager.currentAudio = null;
                stopNotification();
                instance = null;
                Log.d(TAG, "Player released");
            }
        }
    }

    public static void playNext(Runnable onPreparedCallback) {
        PlayerManager manager = getInstance();

        // Prevent multiple calls within the delay period
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNextCallTime < CALL_DELAY) {
            Log.d(TAG, "playNext called too soon, ignoring");
            return;
        }

        // Prevent concurrent execution
        if (isPlayingNext) {
            Log.d(TAG, "Already playing next, ignoring duplicate call");
            return;
        }

        lastNextCallTime = currentTime;
        isPlayingNext = true;

        // Reset the flag after a delay
        playHandler.postDelayed(() -> isPlayingNext = false, 3000);

        Log.d(TAG, "playNext called - currentPlaylistType: " + manager.currentPlaylistType +
                ", currentPlaylist size: " + (manager.currentPlaylist != null ? manager.currentPlaylist.size() : "null"));

        if (manager.currentPlaylist == null || manager.currentPlaylist.isEmpty()) {
            Log.e(TAG, "Cannot play next: current playlist is null or empty");
            stopPlayback();
            isPlayingNext = false;
            return;
        }

        // Calculate next index
        int nextIndex = manager.currentIndex + 1;
        if (nextIndex >= manager.currentPlaylist.size()) {
            nextIndex = 0; // Loop back to start
        }

        // Broadcast that we're about to play next (show progress on next item)
        Intent prepareIntent = new Intent(ACTION_PREPARE_NEXT);
        prepareIntent.putExtra("nextIndex", nextIndex);
        if (manager.context != null) {
            manager.context.sendBroadcast(prepareIntent);
        }

        // Now update the index and play
        manager.currentIndex = nextIndex;
        Log.d(TAG, "Next song index: " + manager.currentIndex +
                ", total songs: " + manager.currentPlaylist.size());

        AudioModel nextAudio = manager.currentPlaylist.get(manager.currentIndex);
        Log.d(TAG, "Attempting to play next: " + nextAudio.getAudioName() +
                ", Playlist type: " + manager.currentPlaylistType);

        // Clear clip mode before playing next song
        clearClipMode();

        playAudio(nextAudio, () -> {
            isPlayingNext = false;
            if (onPreparedCallback != null) onPreparedCallback.run();
        });
    }

    // Update playPrevious method
    public static void playPrevious(Runnable onPreparedCallback) {
        PlayerManager manager = getInstance();

        // Prevent multiple calls within the delay period
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPrevCallTime < CALL_DELAY) {
            Log.d(TAG, "playPrevious called too soon, ignoring");
            return;
        }

        // Prevent concurrent execution
        if (isPlayingPrevious) {
            Log.d(TAG, "Already playing previous, ignoring duplicate call");
            return;
        }

        lastPrevCallTime = currentTime;
        isPlayingPrevious = true;

        // Reset the flag after a delay
        playHandler.postDelayed(() -> isPlayingPrevious = false, 3000);

        if (manager.currentPlaylist == null || manager.currentPlaylist.isEmpty()) {
            isPlayingPrevious = false;
            return;
        }

        // Calculate previous index
        int prevIndex = manager.currentIndex - 1;
        if (prevIndex < 0) {
            prevIndex = manager.currentPlaylist.size() - 1; // Loop to end
        }

        // Broadcast that we're about to play previous (show progress on previous item)
        Intent prepareIntent = new Intent(ACTION_PREPARE_PREVIOUS);
        prepareIntent.putExtra("prevIndex", prevIndex);
        if (manager.context != null) {
            manager.context.sendBroadcast(prepareIntent);
        }

        // Now update the index and play
        manager.currentIndex = prevIndex;
        Log.d(TAG, "Previous song index: " + manager.currentIndex +
                ", total songs: " + manager.currentPlaylist.size());

        AudioModel prevAudio = manager.currentPlaylist.get(manager.currentIndex);
        Log.d(TAG, "Playing previous: " + prevAudio.getAudioName() +
                ", Playlist type: " + manager.currentPlaylistType);

        // Clear clip mode before playing previous song
        clearClipMode();

        playAudio(prevAudio, () -> {
            isPlayingPrevious = false;
            if (onPreparedCallback != null) onPreparedCallback.run();
        });
    }

    public static boolean isPlayingNext() {
        return isPlayingNext;
    }

    // Add this method to check if previous is currently being processed
    public static boolean isPlayingPrevious() {
        return isPlayingPrevious;
    }
    private void broadcastAudioChange() {
        if (context != null) {
            Intent intent = new Intent("UPDATE_MINI_PLAYER");
            intent.putExtra("isPlaying", isPlaying());
            if (currentAudio != null) {
                intent.putExtra("audioName", currentAudio.getAudioName());
                intent.putExtra("audioArtist", currentAudio.getcategoryName());
            }
            context.sendBroadcast(intent);

            context.sendBroadcast(new Intent("UPDATE_AUDIO_ADAPTER"));
        }
    }

    private void updateNotification(AudioModel audio, boolean isPlaying) {
        if (notificationHelper != null && audio != null) {
            notificationHelper.showNotification(audio, isPlaying);
        }
    }

    public static void updateNotification() {
        PlayerManager manager = getInstance();
        if (manager.currentAudio != null) {
            manager.updateNotification(manager.currentAudio, isPlaying());
        }
    }

    public static void stopNotification() {
        if (notificationHelper != null) {
            notificationHelper.cancelNotification();
        }
    }

    private void saveCurrentAudio() {
        if (currentAudio != null && context != null) {
            SharedPreferences prefs = context.getSharedPreferences("SavedAudio", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            editor.clear();

            editor.putString("audioName", currentAudio.getAudioName());
            editor.putString("audioUrl", currentAudio.getAudioUrl());
            editor.putString("imageUrl", currentAudio.getImageUrl());
            editor.putString("audioArtist", currentAudio.getcategoryName());
            editor.putString("songId", currentAudio.getSongId());
            editor.putLong("currentPosition", currentPosition);

            if (currentAudio.getDownloadPath() != null && !currentAudio.getDownloadPath().isEmpty()) {
                editor.putString("downloadPath", currentAudio.getDownloadPath());
                Log.d(TAG, "Saving download path: " + currentAudio.getDownloadPath());
            } else {
                editor.remove("downloadPath");
            }

            String categoryId = currentAudio.getCategoryId();
            if (categoryId != null && !categoryId.isEmpty()) {
                editor.putString("audioCategoryId", categoryId);
                Log.d(TAG, "Saved current audio with category ID: " + currentAudio.getAudioName() +
                        " | Category ID: " + categoryId);
            } else {
                editor.remove("audioCategoryId");
            }

            editor.apply();
            saveFullPlaylist();
        }
    }

    private void saveFullPlaylist() {
        if (currentPlaylist != null && !currentPlaylist.isEmpty() && context != null) {
            SharedPreferences prefs = context.getSharedPreferences("SavedPlaylist", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            editor.clear();
            editor.putInt("playlistSize", currentPlaylist.size());
            editor.putString("playlistType", currentPlaylistType.name());
            editor.putInt("currentIndex", currentIndex);

            for (int i = 0; i < currentPlaylist.size(); i++) {
                AudioModel song = currentPlaylist.get(i);
                editor.putString("songName_" + i, song.getAudioName());
                editor.putString("songUrl_" + i, song.getAudioUrl());
                editor.putString("songImage_" + i, song.getImageUrl());
                editor.putString("songArtist_" + i, song.getcategoryName());
                editor.putString("songId_" + i, song.getSongId());

                if (song.getDownloadPath() != null && !song.getDownloadPath().isEmpty()) {
                    editor.putString("songDownloadPath_" + i, song.getDownloadPath());
                    editor.putBoolean("songDownloaded_" + i, true);
                    Log.d(TAG, "Saving playlist song " + i + " download path: " + song.getDownloadPath());
                }

                if (song.getCategoryId() != null && !song.getCategoryId().isEmpty()) {
                    editor.putString("songCategoryId_" + i, song.getCategoryId());
                    Log.d(TAG, "Saving playlist song " + i + " category ID: " + song.getCategoryId());
                }
            }

            editor.apply();
            Log.d(TAG, "Saved full playlist with " + currentPlaylist.size() + " songs");
        }
    }

    public static void setCurrentAudio(AudioModel audio) {
        PlayerManager manager = getInstance();
        manager.currentAudio = audio;

        if (audio != null) {
            manager.saveCurrentAudio();
        }
    }

    public static void debugAllPlaylistInfo() {
        PlayerManager manager = getInstance();
        Log.d(TAG, "=== COMPLETE PLAYLIST DEBUG INFO ===");
        Log.d(TAG, "Current playlist type: " + manager.currentPlaylistType);
        Log.d(TAG, "Current playlist size: " +
                (manager.currentPlaylist != null ? manager.currentPlaylist.size() : "null"));
        Log.d(TAG, "Current index: " + manager.currentIndex);

        if (manager.currentPlaylist != null) {
            for (int i = 0; i < manager.currentPlaylist.size(); i++) {
                AudioModel song = manager.currentPlaylist.get(i);
                Log.d(TAG, "  [" + i + "] " + song.getAudioName() +
                        " | Downloaded: " + song.isDownloaded() +
                        " | URL: " + song.getAudioUrl());
            }
        }

        Log.d(TAG, "Online playlist size: " +
                (manager.onlinePlaylist != null ? manager.onlinePlaylist.size() : "null"));
        Log.d(TAG, "Offline playlist size: " +
                (manager.offlinePlaylist != null ? manager.offlinePlaylist.size() : "null"));

        Log.d(TAG, "Current audio: " +
                (manager.currentAudio != null ? manager.currentAudio.getAudioName() : "null"));
        Log.d(TAG, "Is playing: " + isPlaying());
        Log.d(TAG, "=== END DEBUG ===");
    }

    public static void playOfflineAudio(AudioModel audio, Runnable onPreparedCallback) {
        PlayerManager manager = getInstance();
        if (audio == null) {
            Log.e(TAG, "playOfflineAudio: audio is null");
            return;
        }

        String filePath = audio.getDownloadPath();
        if (filePath == null || filePath.isEmpty()) {
            Log.e(TAG, "playOfflineAudio: No download path for audio: " + audio.getAudioName());
            playAudio(audio, onPreparedCallback);
            return;
        }

        File audioFile = new File(filePath);
        if (!audioFile.exists()) {
            Log.e(TAG, "playOfflineAudio: File doesn't exist: " + filePath);
            playAudio(audio, onPreparedCallback);
            return;
        }

        Log.d(TAG, "playOfflineAudio: Playing from local file: " + filePath +
                ", File size: " + audioFile.length() + " bytes");

        Log.d(TAG, "🎵 ABOUT TO PLAY OFFLINE:");
        Log.d(TAG, "   Song: " + audio.getAudioName());
        Log.d(TAG, "   Category ID: " + audio.getCategoryId());
        Log.d(TAG, "   Original URL: " + audio.getAudioUrl());
        Log.d(TAG, "   Is Downloaded: " + audio.isDownloaded());

        AudioModel audioToPlay = new AudioModel(audio);
        manager.currentPlaylistType = PlaylistType.OFFLINE;

        if (manager.offlinePlaylist != null) {
            for (int i = 0; i < manager.offlinePlaylist.size(); i++) {
                AudioModel song = manager.offlinePlaylist.get(i);
                if (song.getAudioUrl().equals(audio.getAudioUrl()) ||
                        (song.getDownloadPath() != null && song.getDownloadPath().equals(filePath))) {
                    manager.currentIndex = i;
                    break;
                }
            }
            manager.currentPlaylist = manager.offlinePlaylist;
        }

        manager.startOfflineAudio(audioToPlay, filePath, onPreparedCallback);
    }

    private void startOfflineAudio(AudioModel audio, String filePath, Runnable onPreparedCallback) {
        try {
            Log.d(TAG, "startOfflineAudio: " + audio.getAudioName());

            // Check if this is a different song
            checkForSongChange(audio);

            synchronized (lock) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }

                mediaPlayer.reset();
                mediaPlayer.setDataSource(filePath);
            }

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error in offline playback - what: " + what + ", extra: " + extra);

                if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN || extra == MediaPlayer.MEDIA_ERROR_IO) {
                    Log.d(TAG, "Offline playback failed, trying online URL: " + audio.getAudioUrl());
                    try {
                        mp.reset();
                        mp.setDataSource(audio.getAudioUrl());
                        mp.prepareAsync();
                        return true;
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to fallback to online playback: " + e.getMessage());
                    }
                }
                return false;
            });

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "Offline audio prepared, starting playback");
                mp.start();
                currentAudio = audio;
                currentPosition = 0;

                // DON'T automatically restart clip mode for new song
                // Clip mode should be reset and user needs to activate it again

                // Update playback state for Bluetooth
                updatePlaybackState(true, 0);
                startPositionTracking();

                Log.d(TAG, "🎵 OFFLINE NOW PLAYING: " + audio.getAudioName());
                Log.d(TAG, "   Category ID: " + audio.getCategoryId());
                Log.d(TAG, "   Playlist ID: " + audio.getPlaylistId());
                Log.d(TAG, "   Is Downloaded: " + audio.isDownloaded());
                Log.d(TAG, "   Download Path: " + filePath);

                logCurrentPlayingInfo();
                saveCurrentAudio();
                notifyAudioChanged();
                broadcastAudioChange();
                updateNotification(audio, true);

                mp.setOnCompletionListener(m -> {
                    Log.d(TAG, "Offline song completed: " + audio.getAudioName());
                    playNext(null);
                });

                if (onPreparedCallback != null) {
                    onPreparedCallback.run();
                }
            });

            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            Log.e(TAG, "Error in startOfflineAudio: " + e.getMessage());
            abandonAudioFocus();
            e.printStackTrace();
            playAudio(audio, onPreparedCallback);
        }
    }

    public static boolean isSongDownloaded(AudioModel audio) {
        if (audio == null) return false;

        if (audio.getDownloadPath() != null && !audio.getDownloadPath().isEmpty()) {
            File file = new File(audio.getDownloadPath());
            return file.exists();
        }

        return false;
    }

    public static android.net.Uri getOfflineFileUri(AudioModel audio) {
        if (audio == null || audio.getDownloadPath() == null) return null;

        File file = new File(audio.getDownloadPath());
        if (!file.exists()) return null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return androidx.core.content.FileProvider.getUriForFile(
                    instance.context,
                    instance.context.getPackageName() + ".provider",
                    file
            );
        } else {
            return android.net.Uri.fromFile(file);
        }
    }

    public static void handlePlayPause() {
        PlayerManager manager = getInstance();

        if (manager.currentAudio == null) {
            Log.w(TAG, "handlePlayPause: No current audio to play/pause");
            return;
        }

        if (isPlaying()) {
            pausePlayback();
        } else {
            if (manager.mediaPlayer != null && manager.currentAudio != null) {
                resumePlayback();
            } else {
                playAudio(manager.currentAudio, null);
            }
        }
    }

    public static List<AudioModel> getCurrentPlaylist() {
        return getInstance().currentPlaylist;
    }

    public static String getCurrentPlaylistId() {
        if (getCurrentAudio() != null) {
            return getCurrentAudio().getPlaylistId();
        }
        return null;
    }

    public static void logCurrentPlayingInfo() {
        PlayerManager manager = getInstance();
        AudioModel currentAudio = manager.currentAudio;
        if (currentAudio != null) {
            Log.d(TAG, "🎵 Currently Playing:");
            Log.d(TAG, "  Song: " + currentAudio.getAudioName());
            Log.d(TAG, "  Song ID: " + currentAudio.getSongId());
            Log.d(TAG, "  Category ID: " + currentAudio.getCategoryId());
            Log.d(TAG, "  Category Name: " + currentAudio.getcategoryName());
            Log.d(TAG, "  Playlist ID: " + currentAudio.getPlaylistId());
            Log.d(TAG, "  Download Path: " + currentAudio.getDownloadPath());
            Log.d(TAG, "  Is Downloaded: " + currentAudio.isDownloaded());
            Log.d(TAG, "  Playlist Type: " + manager.currentPlaylistType);
            Log.d(TAG, "  Position: " + manager.currentPosition + "/" +
                    (manager.mediaPlayer != null ? manager.mediaPlayer.getDuration() : 0));
        } else {
            Log.d(TAG, "🎵 No audio currently playing");
        }
    }

    public List<AudioModel> getOnlinePlaylist() {
        return onlinePlaylist != null ? new ArrayList<>(onlinePlaylist) : new ArrayList<>();
    }

    public List<AudioModel> getOfflinePlaylist() {
        return offlinePlaylist != null ? new ArrayList<>(offlinePlaylist) : new ArrayList<>();
    }

    public List<AudioModel> getCurrentPlaylistForType(PlaylistType type) {
        if (type == PlaylistType.ONLINE) {
            return getOnlinePlaylist();
        } else {
            return getOfflinePlaylist();
        }
    }


    public static long getDuration() {
        PlayerManager manager = getInstance();
        if (manager.mediaPlayer != null && manager.currentAudio != null) {
            return manager.mediaPlayer.getDuration();
        }
        return 0;
    }


    public static void restoreState() {
        PlayerManager manager = getInstance();
        if (manager.context == null) return;

        SharedPreferences prefs = manager.context.getSharedPreferences("SavedAudio", Context.MODE_PRIVATE);
        String audioName = prefs.getString("audioName", null);

        if (audioName != null) {
            String audioUrl = prefs.getString("audioUrl", "");
            String imageUrl = prefs.getString("imageUrl", "");
            String audioArtist = prefs.getString("audioArtist", "");
            String songId = prefs.getString("songId", "");
            String downloadPath = prefs.getString("downloadPath", "");
            String audioCategoryId = prefs.getString("audioCategoryId", "");
            long savedPosition = prefs.getLong("currentPosition", 0);

            AudioModel savedAudio = new AudioModel();
            savedAudio.setAudioName(audioName);
            savedAudio.setAudioUrl(audioUrl);
            savedAudio.setImageUrl(imageUrl);
            savedAudio.setcategoryName(audioArtist);
            savedAudio.setSongId(songId);
            savedAudio.setDownloadPath(downloadPath);
            savedAudio.setCategoryId(audioCategoryId);
            savedAudio.setDownloaded(downloadPath != null && !downloadPath.isEmpty());

            manager.currentAudio = savedAudio;
            manager.currentPosition = savedPosition;

            Log.d(TAG, "Restored audio state: " + audioName);
        }
    }

    public MediaSessionCompat.Token getMediaSessionToken() {
        if (mediaSession != null) {
            return mediaSession.getSessionToken();
        }
        return null;
    }


    private void logBluetoothDebug(String action) {
        Log.d(TAG, "=== BLUETOOTH DEBUG ===");
        Log.d(TAG, "Action: " + action);
        Log.d(TAG, "Is playing: " + isPlaying());
        Log.d(TAG, "Current audio: " + (currentAudio != null ? currentAudio.getAudioName() : "null"));
        Log.d(TAG, "MediaSession active: " + (mediaSession != null && mediaSession.isActive()));
        Log.d(TAG, "======================");
    }

    // Clip Mode Methods
    public static void setClipModeController(ClipModeController controller) {
        clipModeController = controller;
    }

    public static ClipModeController getClipModeController() {
        return clipModeController;
    }

    public static boolean isClipModeActive() {
        return isClipModeActive;
    }

    public static void setClipModeActive(boolean active) {
        isClipModeActive = active;

        // Broadcast clip mode state change
        if (getInstance().context != null) {
            Intent intent = new Intent("CLIP_MODE_STATE_CHANGED");
            intent.putExtra("isActive", active);
            getInstance().context.sendBroadcast(intent);
        }
    }

    public static void seekTo(int position) {
        PlayerManager manager = getInstance();
        if (manager.mediaPlayer != null) {
            // If clip mode is active, constrain position to clip bounds
            if (clipModeController != null && clipModeController.isClipModeActive()) {
                position = clipModeController.getConstrainedPosition(position);
            }

            manager.mediaPlayer.seekTo(position);
            manager.currentPosition = position;
            manager.updatePlaybackState(isPlaying(), position);
        }
    }

    public static long getCurrentPosition() {
        PlayerManager manager = getInstance();
        if (manager.mediaPlayer != null) {
            int position = manager.mediaPlayer.getCurrentPosition();

            // If clip mode is active, return constrained position for display
            if (clipModeController != null && clipModeController.isClipModeActive()) {
                return clipModeController.getConstrainedPosition(position);
            }

            return position;
        }
        return manager.currentPosition;
    }

    private void checkForSongChange(AudioModel newAudio) {
        if (newAudio == null) return;

        String newSongId = newAudio.getSongId();
        if (!newSongId.equals(currentSongId)) {
            // Song has changed
            Log.d(TAG, "Song changed from " + currentSongId + " to " + newSongId);
            currentSongId = newSongId;

            // Reset clip mode for new song
            resetClipModeForNewSong();
        }
    }

    // Add this method to reset clip mode for new song
    private void resetClipModeForNewSong() {
        if (clipModeController != null) {
            if (clipModeController.isClipModeActive()) {
                Log.d(TAG, "Stopping clip mode for song change");
                clipModeController.stopClipMode();
            }
            // Reset the controller for new song
            clipModeController.setMediaPlayer(mediaPlayer);
        }
        isClipModeActive = false;
    }

    // Add this method to clear clip mode completely
    public static void clearClipMode() {
        if (clipModeController != null) {
            clipModeController.stopClipMode();
            clipModeController = null;
        }
        isClipModeActive = false;

        // Broadcast clip mode cleared
        if (getInstance().context != null) {
            Intent intent = new Intent("CLIP_MODE_STATE_CHANGED");
            intent.putExtra("isActive", false);
            intent.putExtra("isCleared", true);
            getInstance().context.sendBroadcast(intent);
        }
    }
}