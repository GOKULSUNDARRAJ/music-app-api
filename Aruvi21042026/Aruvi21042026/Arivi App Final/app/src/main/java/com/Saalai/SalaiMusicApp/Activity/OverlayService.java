package com.Saalai.SalaiMusicApp.Activity;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.Saalai.SalaiMusicApp.R;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private View overlayView;
    private ExoPlayer player;
    private PlayerView playerView;
    private WindowManager.LayoutParams params;
    private float initialTouchX, initialTouchY;
    private int initialX, initialY;
    private ProgressBar progressBar;
    private String currentVideoUrl;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 10;
    private static final long RETRY_DELAY_MS = 3000;
    private boolean isExpanded = false;
    private boolean isClosing = false;
    private static final int CLOSE_THRESHOLD_DP = 60;
    private int closeThresholdPx;
    ImageView openbutton;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        closeThresholdPx = (int) (CLOSE_THRESHOLD_DP * getResources().getDisplayMetrics().density);
        initializePlayer();
        setupOverlayView();
        setupPlayerListener();
        setupDragAndDrop();
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
    }

    private void setupOverlayView() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        overlayView = inflater.inflate(R.layout.overlay_layout, null);

        // Make sure the overlay has a solid background
        overlayView.setBackgroundColor(0xFF000000); // Solid black background

        playerView = overlayView.findViewById(R.id.overlay_player_view);
        playerView.setPlayer(player);
        playerView.setUseController(false);

        ImageView btnClose = overlayView.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> stopSelf());
        btnClose.setVisibility(View.GONE);

        ImageView expandButton = overlayView.findViewById(R.id.exo_play34);
        expandButton.setOnClickListener(v -> {
            if (!isExpanded) {
                expandOverlay();
            } else {
                collapseOverlay();
            }
        });


        openbutton = overlayView.findViewById(R.id.exo_play343);
        openbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMainActivity();
            }
        });
        openbutton.setVisibility(View.GONE);

        progressBar = overlayView.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int initialWidth = (int) (200 * getResources().getDisplayMetrics().density);
        int initialHeight = (int) (120 * getResources().getDisplayMetrics().density);

        params = new WindowManager.LayoutParams(
                initialWidth,
                initialHeight,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        windowManager.addView(overlayView, params);
    }

    private void expandOverlay() {
        params.width = (int) (300 * getResources().getDisplayMetrics().density);
        params.height = (int) (200 * getResources().getDisplayMetrics().density);
        isExpanded = true;

        ImageView btnClose = overlayView.findViewById(R.id.btnClose);
        btnClose.setVisibility(View.VISIBLE);
        openbutton.setVisibility(View.VISIBLE);
        playerView.setUseController(true);

        windowManager.updateViewLayout(overlayView, params);
    }

    private void collapseOverlay() {
        // Set to your original/minimized size
        params.width = (int) (200 * getResources().getDisplayMetrics().density);
        params.height = (int) (120 * getResources().getDisplayMetrics().density);
        isExpanded = false;

        ImageView btnClose = overlayView.findViewById(R.id.btnClose);
        btnClose.setVisibility(View.GONE);
        openbutton.setVisibility(View.GONE);
        playerView.setUseController(false);

        windowManager.updateViewLayout(overlayView, params);
    }


    private void setupPlayerListener() {
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    progressBar.setVisibility(View.GONE);
                } else if (state == Player.STATE_BUFFERING) {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(OverlayService.this, "Error playing video! Retrying...", Toast.LENGTH_SHORT).show();

                if (retryCount < MAX_RETRIES) {
                    retryCount++;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> retryPlayback(), RETRY_DELAY_MS);
                } else {
                    Toast.makeText(OverlayService.this, "Failed after " + MAX_RETRIES + " attempts", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupDragAndDrop() {
        playerView.setOnTouchListener(new View.OnTouchListener() {
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        isDragging = true;
                        int newX = initialX + (int) (event.getRawX() - initialTouchX);
                        int newY = initialY + (int) (event.getRawY() - initialTouchY);

                        params.x = newX;
                        params.y = newY;
                        windowManager.updateViewLayout(overlayView, params);

                        checkProximityToCloseButton();
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!isDragging && !isExpanded) {
                            expandOverlay();
                        }
                        isDragging = false;
                        return true;
                }
                return false;
            }
        });
    }

    private void checkProximityToCloseButton() {
        if (!isExpanded) return;

        ImageView btnClose = overlayView.findViewById(R.id.btnClose);
        int[] closeButtonPos = new int[2];
        btnClose.getLocationOnScreen(closeButtonPos);

        int[] overlayPos = new int[2];
        overlayView.getLocationOnScreen(overlayPos);

        int overlayCenterX = overlayPos[0] + overlayView.getWidth()/2;
        int overlayCenterY = overlayPos[1] + overlayView.getHeight()/2;
        int closeButtonCenterX = closeButtonPos[0] + btnClose.getWidth()/2;
        int closeButtonCenterY = closeButtonPos[1] + btnClose.getHeight()/2;

        double distance = Math.sqrt(
                Math.pow(overlayCenterX - closeButtonCenterX, 2) +
                        Math.pow(overlayCenterY - closeButtonCenterY, 2)
        );

        if (distance < closeThresholdPx) {
            btnClose.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100).start();
            if (!isClosing) {
                isClosing = true;
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isClosing) {
                        stopSelf();
                    }
                }, 300);
            }
        } else {
            btnClose.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            isClosing = false;
        }
    }

    private void openMainActivity() {
        stopSelf();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("FROM_OVERLAY", true);

        if (player != null && player.getCurrentMediaItem() != null) {
            String videoUrl = player.getCurrentMediaItem().playbackProperties.uri.toString();
            intent.putExtra("VIDEO_URL", videoUrl);
        }

        startActivity(intent);
    }

    private void retryPlayback() {
        if (currentVideoUrl != null && player != null) {
            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(currentVideoUrl));
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("VIDEO_URL")) {
            currentVideoUrl = intent.getStringExtra("VIDEO_URL");
            retryCount = 0;

            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(currentVideoUrl));
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
    }
}