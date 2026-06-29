package com.Saalai.SalaiMusicApp;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class ClipModeController {
    private static final String TAG = "ClipModeController";
    public static final int MAX_CLIP_DURATION = 100000; // 100 seconds in milliseconds
    private static final int DEFAULT_CLIP_DURATION = 30000; // 30 seconds default
    private static final int MIN_CLIP_DURATION = 5000; // 5 seconds minimum
    private static final int FADE_DURATION = 1000; // 1 second fade
    private static final int UPDATE_INTERVAL = 50; // 50ms updates

    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Handler fadeHandler;
    private ClipModeListener listener;
    private boolean isClipModeActive = false;
    private int clipStartPosition = 0;
    private int clipEndPosition = 0;
    private int originalDuration;
    private boolean isFading = false;
    private boolean isPaused = false;
    private boolean isLooping = false;

    // New variable for user-selected duration
    private int selectedClipDuration = DEFAULT_CLIP_DURATION;

    // Fade variables
    private float currentVolume = 1.0f;
    private Runnable fadeRunnable;
    private Runnable clipMonitorRunnable;

    public interface ClipModeListener {
        void onClipModeStarted();
        void onClipModeStopped();
        void onClipLoopRestart();
        void onClipTimeUpdate(int currentPosition, int clipPosition, int clipProgress);
        void onClipModePaused();
        void onClipModeResumed();
        void onClipDurationChanged(int durationSeconds); // New callback
    }

    public ClipModeController() {
        this.handler = new Handler(Looper.getMainLooper());
        this.fadeHandler = new Handler(Looper.getMainLooper());
    }

    // New method to set clip duration from SeekBar
    public void setClipDuration(int durationSeconds) {
        // Convert seconds to milliseconds
        int durationMillis = durationSeconds * 1000;

        // Clamp to valid range
        if (durationMillis < MIN_CLIP_DURATION) {
            durationMillis = MIN_CLIP_DURATION;
        } else if (durationMillis > MAX_CLIP_DURATION) {
            durationMillis = MAX_CLIP_DURATION;
        }

        this.selectedClipDuration = durationMillis;

        Log.d(TAG, "Clip duration set to: " + durationSeconds + " seconds (" + durationMillis + "ms)");

        // If clip mode is active, update the clip end position
        if (isClipModeActive && mediaPlayer != null) {
            updateClipEndPosition();

            // Notify listener
            if (listener != null) {
                listener.onClipDurationChanged(durationSeconds);
            }
        }
    }

    private void updateClipEndPosition() {
        if (mediaPlayer == null) return;

        originalDuration = mediaPlayer.getDuration();
        clipEndPosition = Math.min(clipStartPosition + selectedClipDuration, originalDuration);

        Log.d(TAG, "Updated clip end position: " + clipEndPosition + "ms (duration: " +
                (clipEndPosition - clipStartPosition) + "ms)");
    }

    // Get current clip duration in seconds
    public int getClipDurationSeconds() {
        return selectedClipDuration / 1000;
    }

    // Get max clip duration in seconds
    public int getMaxClipDurationSeconds() {
        return MAX_CLIP_DURATION / 1000;
    }

    public void setMediaPlayer(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }

    public void setClipModeListener(ClipModeListener listener) {
        this.listener = listener;
    }

    public void startClipMode(int startPosition) {
        if (mediaPlayer == null) {
            Log.e(TAG, "Cannot start clip mode: mediaPlayer is null");
            return;
        }

        if (isClipModeActive) {
            return;
        }

        isClipModeActive = true;
        isPaused = false;
        isLooping = true;
        clipStartPosition = startPosition;
        originalDuration = mediaPlayer.getDuration();

        // Use the selected clip duration
        clipEndPosition = Math.min(clipStartPosition + selectedClipDuration, originalDuration);

        // Ensure valid start position
        if (clipStartPosition < 0) clipStartPosition = 0;
        if (clipStartPosition >= originalDuration - MIN_CLIP_DURATION) clipStartPosition = 0;

        Log.d(TAG, "Starting clip mode at: " + clipStartPosition + "ms, end at: " + clipEndPosition +
                "ms, duration: " + (clipEndPosition - clipStartPosition) + "ms");

        // Seek to start position
        mediaPlayer.seekTo(clipStartPosition);

        // Start with fade in
        startFadeIn();

        // Start monitoring after a short delay
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isClipModeActive && !isPaused) {
                    startClipMonitoring();
                }
            }
        }, 500);

        if (listener != null) {
            listener.onClipModeStarted();
            listener.onClipDurationChanged(selectedClipDuration / 1000);
        }
    }

    public void stopClipMode() {
        if (!isClipModeActive) return;

        Log.d(TAG, "Stopping clip mode");

        isClipModeActive = false;
        isLooping = false;
        isFading = false;
        isPaused = false;

        // Stop monitoring
        if (clipMonitorRunnable != null) {
            handler.removeCallbacks(clipMonitorRunnable);
            clipMonitorRunnable = null;
        }

        // Stop fade
        if (fadeRunnable != null) {
            fadeHandler.removeCallbacks(fadeRunnable);
            fadeRunnable = null;
        }

        // Restore full volume
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(1.0f, 1.0f);
        }

        if (listener != null) {
            listener.onClipModeStopped();
        }
    }

    // Rest of your existing methods remain the same...
    public void pauseClipMode() {
        if (!isClipModeActive || isPaused) return;

        Log.d(TAG, "Pausing clip mode");

        isPaused = true;
        isLooping = false;

        if (clipMonitorRunnable != null) {
            handler.removeCallbacks(clipMonitorRunnable);
            clipMonitorRunnable = null;
        }

        if (fadeRunnable != null) {
            fadeHandler.removeCallbacks(fadeRunnable);
            fadeRunnable = null;
            isFading = false;
        }

        if (listener != null) {
            listener.onClipModePaused();
        }
    }

    public void resumeClipMode() {
        if (!isClipModeActive || !isPaused) return;

        Log.d(TAG, "Resuming clip mode");

        isPaused = false;
        isLooping = true;

        // Make sure we're at a valid position
        if (mediaPlayer != null) {
            int currentPos = mediaPlayer.getCurrentPosition();
            if (currentPos > clipEndPosition || currentPos < clipStartPosition) {
                mediaPlayer.seekTo(clipStartPosition);
            }
        }

        startClipMonitoring();
        startFadeIn();

        if (listener != null) {
            listener.onClipModeResumed();
        }
    }

    public boolean isClipModeActive() {
        return isClipModeActive;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public int getClipStartPosition() {
        return clipStartPosition;
    }

    public int getClipEndPosition() {
        return clipEndPosition;
    }

    public int getConstrainedPosition(int position) {
        if (!isClipModeActive) return position;

        if (position < clipStartPosition) return clipStartPosition;
        if (position > clipEndPosition) return clipEndPosition;
        return position;
    }

    // Your existing monitoring and fade methods remain the same...
    private void startClipMonitoring() {
        if (clipMonitorRunnable != null) {
            handler.removeCallbacks(clipMonitorRunnable);
        }

        clipMonitorRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isClipModeActive || isPaused || mediaPlayer == null || !isLooping) {
                    return;
                }

                try {
                    int currentPosition = mediaPlayer.getCurrentPosition();

                    // Update clip position
                    int clipPosition = currentPosition - clipStartPosition;
                    int clipProgress = clipPosition;

                    // Send time update to listener
                    if (listener != null) {
                        listener.onClipTimeUpdate(currentPosition, clipPosition, clipProgress);
                    }

                    // Check if we've reached the end of the clip
                    if (currentPosition >= clipEndPosition || currentPosition >= originalDuration - 100) {
                        Log.d(TAG, "Reached clip end at " + currentPosition + "ms, looping back to " + clipStartPosition + "ms");

                        // Stop monitoring temporarily
                        handler.removeCallbacks(this);

                        // Start fade out and loop
                        startFadeOut(new Runnable() {
                            @Override
                            public void run() {
                                if (!isClipModeActive || isPaused || !isLooping) return;

                                // Seek back to start
                                mediaPlayer.seekTo(clipStartPosition);

                                // Notify loop restart
                                if (listener != null) {
                                    listener.onClipLoopRestart();
                                }

                                // Start fade in
                                startFadeIn();

                                // Restart monitoring after a short delay
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (isClipModeActive && !isPaused && isLooping) {
                                            startClipMonitoring();
                                        }
                                    }
                                }, FADE_DURATION / 2);
                            }
                        });
                    } else {
                        // Continue monitoring
                        handler.postDelayed(this, UPDATE_INTERVAL);
                    }
                } catch (IllegalStateException e) {
                    Log.e(TAG, "MediaPlayer state error: " + e.getMessage());
                    // Try to restart monitoring after error
                    handler.postDelayed(this, UPDATE_INTERVAL);
                }
            }
        };

        handler.post(clipMonitorRunnable);
    }

    private void startFadeIn() {
        if (isFading || mediaPlayer == null) return;

        Log.d(TAG, "Starting fade in");

        isFading = true;
        currentVolume = 0.0f;
        mediaPlayer.setVolume(0.0f, 0.0f);

        final long startTime = System.currentTimeMillis();

        fadeRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isClipModeActive || isPaused || mediaPlayer == null) {
                    isFading = false;
                    return;
                }

                try {
                    long elapsed = System.currentTimeMillis() - startTime;
                    float progress = Math.min(1.0f, (float) elapsed / FADE_DURATION);
                    float volume = interpolate(progress);
                    currentVolume = volume;

                    mediaPlayer.setVolume(volume, volume);

                    if (progress < 1.0f) {
                        fadeHandler.postDelayed(this, 20);
                    } else {
                        Log.d(TAG, "Fade in complete");
                        isFading = false;
                    }
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Error during fade in: " + e.getMessage());
                    isFading = false;
                }
            }
        };

        fadeHandler.post(fadeRunnable);
    }

    private void startFadeOut(final Runnable onComplete) {
        if (isFading || mediaPlayer == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        Log.d(TAG, "Starting fade out");

        isFading = true;
        final float startVolume = currentVolume;
        final long startTime = System.currentTimeMillis();

        fadeRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isClipModeActive || isPaused || mediaPlayer == null) {
                    isFading = false;
                    if (onComplete != null) onComplete.run();
                    return;
                }

                try {
                    long elapsed = System.currentTimeMillis() - startTime;
                    float progress = Math.min(1.0f, (float) elapsed / FADE_DURATION);
                    float volume = startVolume * (1.0f - progress);

                    mediaPlayer.setVolume(volume, volume);

                    if (progress < 1.0f) {
                        fadeHandler.postDelayed(this, 20);
                    } else {
                        Log.d(TAG, "Fade out complete");
                        isFading = false;
                        currentVolume = 0.0f;
                        if (onComplete != null) onComplete.run();
                    }
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Error during fade out: " + e.getMessage());
                    isFading = false;
                    if (onComplete != null) onComplete.run();
                }
            }
        };

        fadeHandler.post(fadeRunnable);
    }

    private float interpolate(float t) {
        // Smooth step interpolation
        return t * t * (3 - 2 * t);
    }

    public void release() {
        Log.d(TAG, "Releasing ClipModeController");

        isClipModeActive = false;
        isLooping = false;
        isFading = false;

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (fadeHandler != null) {
            fadeHandler.removeCallbacksAndMessages(null);
        }

        mediaPlayer = null;
        listener = null;
    }
}