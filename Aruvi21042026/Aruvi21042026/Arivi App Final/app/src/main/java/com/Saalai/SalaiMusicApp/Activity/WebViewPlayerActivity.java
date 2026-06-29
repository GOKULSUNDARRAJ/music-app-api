package com.Saalai.SalaiMusicApp.Activity;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.Saalai.SalaiMusicApp.R;
public class WebViewPlayerActivity extends AppCompatActivity implements Player.Listener {

    private PlayerView playerView;
    private ExoPlayer player;
    private String streamUrl = "http://88.150.197.10:1935/star-live/suneuzx789.stream/playlist.m3u8";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view_player);
        makeStatusBarTransparent();
        playerView = findViewById(R.id.player_view);
        initializePlayer();
    }

    private void initializePlayer() {
        // Create player instance
        player = new ExoPlayer.Builder(this).build();

        // Set player to view
        playerView.setPlayer(player);

        // Create media item
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(streamUrl));

        // Set live playback configuration
        MediaItem.LiveConfiguration liveConfig = new MediaItem.LiveConfiguration.Builder()
                .setTargetOffsetMs(5000) // 5 seconds from live edge
                .setMinPlaybackSpeed(0.98f)
                .setMaxPlaybackSpeed(1.02f)
                .build();

        mediaItem = mediaItem.buildUpon()
                .setLiveConfiguration(liveConfig)
                .build();

        // Set media item and prepare
        player.setMediaItem(mediaItem);
        player.prepare();
        player.addListener(this);

        // Start playback
        player.setPlayWhenReady(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    // Handle player errors, including behind live window
    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onPlayerError(PlaybackException error) {
        if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
            // Re-initialize player at the live edge
            player.seekToDefaultPosition();
            player.prepare();
        } else {
            // Handle other errors
            Log.e("LivePlayer", "Playback error: " + error.getMessage());
        }
    }

    // Monitor live playback state
    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        if (isPlaying) {
            // Active playback
            Log.d("LivePlayer", "Playback started");
        }
    }

    // Respond to timeline changes (important for live streams)
    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onTimelineChanged(Timeline timeline, int reason) {
        if (player.isCurrentWindowLive()) {
            long liveOffset = player.getCurrentLiveOffset();
            Log.d("LivePlayer", "Live offset: " + liveOffset + " ms");
        }
    }

    private void makeStatusBarTransparent() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }

}