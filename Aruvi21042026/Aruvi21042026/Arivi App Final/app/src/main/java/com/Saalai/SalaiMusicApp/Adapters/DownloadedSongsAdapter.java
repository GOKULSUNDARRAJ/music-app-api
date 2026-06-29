package com.Saalai.SalaiMusicApp.Adapters;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.AudioDownloadManager;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class DownloadedSongsAdapter extends RecyclerView.Adapter<DownloadedSongsAdapter.SongViewHolder> {

    private ArrayList<AudioModel> songsList;
    private Context context;
    private OnItemClickListener listener;
    private OnSongDeletedListener songDeletedListener;
    private RecyclerView recyclerView;
    private static final String TAG = "DownloadedSongsAdapter";

    private int preparingPosition = -1;
    private int currentPlayingPosition = -1;

    public interface OnItemClickListener {
        void onItemClick(AudioModel audioModel, ProgressBar progressBar);
    }

    public interface OnSongDeletedListener {
        void onSongDeleted(AudioModel deletedAudio, int position);
    }

    public DownloadedSongsAdapter(ArrayList<AudioModel> songsList, Context context, OnItemClickListener listener) {
        this.songsList = songsList;
        this.context = context;
        this.listener = listener;
    }

    // Method to set the RecyclerView reference
    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    public void setOnSongDeletedListener(OnSongDeletedListener listener) {
        this.songDeletedListener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_downloaded_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        AudioModel song = songsList.get(position);

        // Set song details
        holder.tvAudioName.setText(song.getAudioName() != null ? song.getAudioName() : "Unknown Song");
        holder.tvcategoryName.setText(song.getcategoryName() != null ? song.getcategoryName() : "Unknown Artist");

        // Set download status
        if (song.isDownloaded() && song.getDownloadPath() != null) {
            holder.tvDownloadStatus.setText("Downloaded");
            holder.tvDownloadStatus.setTextColor(context.getResources().getColor(R.color.green));
            holder.ivDownloadStatus.setColorFilter(context.getResources().getColor(R.color.green));
        } else {
            holder.tvDownloadStatus.setText("Not Downloaded");
            holder.tvDownloadStatus.setTextColor(context.getResources().getColor(R.color.gray));
            holder.ivDownloadStatus.setColorFilter(context.getResources().getColor(R.color.gray));
        }

        // Load image
        if (song.getImageUrl() != null && !song.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(song.getImageUrl())
                    .placeholder(R.drawable.video_placholder)
                    .error(R.drawable.video_placholder)
                    .into(holder.imgAudioArt);
        } else {
            holder.imgAudioArt.setImageResource(R.drawable.video_placholder);
        }

        // Update current playing position
        updateCurrentPlayingPosition();

        // Check if this is the current playing song
        boolean isCurrent = (position == currentPlayingPosition);

        // Check if this position is preparing (showing progress bar)
        boolean isPreparing = (position == preparingPosition);

        // Handle progress bar visibility
        if (isPreparing) {
            holder.bufferProgress.setVisibility(View.VISIBLE);
            Log.d(TAG, "Showing progress on position: " + position + " - " + song.getAudioName());
        } else {
            holder.bufferProgress.setVisibility(View.GONE);
        }

        // Handle wave animation and play button
        if (isCurrent && PlayerManager.isPlaying()) {
            holder.playButton.setImageResource(R.drawable.pause);
            holder.tvAudioName.setTextColor(context.getResources().getColor(R.color.green));
            startWaveAnimation(holder);
        } else {
            holder.playButton.setImageResource(R.drawable.play_player);
            holder.tvAudioName.setTextColor(context.getResources().getColor(R.color.white));
            stopWaveAnimation(holder);
        }

        // Whole item click listener
        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Item clicked at position " + position + ": " + song.getAudioName());

            // ALWAYS play from start
            holder.bufferProgress.setVisibility(View.VISIBLE);
            saveAudioLocally(song);
            saveFullPlaylist();
            PlayerManager.setAudioList(songsList, PlayerManager.PlaylistType.OFFLINE);

            PlayerManager.playOfflineAudio(song, () -> {
                holder.bufferProgress.setVisibility(View.GONE);
                // Update UI
                updateCurrentPlayingPosition();
                preparingPosition = -1;
                notifyDataSetChanged();
            });

            if (listener != null) {
                listener.onItemClick(song, holder.bufferProgress);
            }
        });

        // Menu click listener
        holder.menu.setOnClickListener(v -> {
            showCustomMenu(v, position, song);
        });
    }

    // ADD THIS METHOD (same as in AudioAdapterfordownload)
    private void showCustomMenu(View anchorView, int position, AudioModel audio) {
        if (context != null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View popupView = inflater.inflate(R.layout.popup_message_menu, null);

            // Find the layouts
            View menuPlayPauseLayout = popupView.findViewById(R.id.menuPlayPause);
            View menuDeleteLayout = popupView.findViewById(R.id.menuDelete);
            ImageView playPauseIcon = popupView.findViewById(R.id.playPauseIcon);
            TextView playPauseText = popupView.findViewById(R.id.playPauseText);

            // Check if this audio is currently playing
            AudioModel currentAudio = PlayerManager.getCurrentAudio();
            boolean isCurrent = currentAudio != null && currentAudio.getAudioUrl().equals(audio.getAudioUrl());

            // Set initial play/pause state
            if (isCurrent && PlayerManager.isPlaying()) {
                playPauseIcon.setImageResource(R.drawable.pause);
                playPauseText.setText("Pause");
            } else {
                playPauseIcon.setImageResource(R.drawable.play_player);
                playPauseText.setText("Play");
            }

            // Get the exact width from the inflated view
            popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int widthInPx = popupView.getMeasuredWidth();
            int heightInPx = popupView.getMeasuredHeight();

            // Create the popup window
            PopupWindow popupWindow = new PopupWindow(
                    popupView,
                    widthInPx,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );

            // Configure popup window
            popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            popupWindow.setOutsideTouchable(true);
            popupWindow.setElevation(16f);
            popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);

            // Handle Play/Pause click
            menuPlayPauseLayout.setOnClickListener(v -> {
                popupWindow.dismiss();

                if (isCurrent) {
                    if (PlayerManager.isPlaying()) {
                        PlayerManager.pausePlayback();
                        // Update the specific item's UI
                        notifyItemChanged(position);
                    } else {
                        PlayerManager.resumePlayback();
                        // Update the specific item's UI
                        notifyItemChanged(position);
                    }
                } else {
                    // Play this song from the beginning
                    // Try to find the ViewHolder
                    SongViewHolder viewHolder = findViewHolderForPosition(position);

                    if (viewHolder != null) {
                        // Use the found ViewHolder
                        viewHolder.bufferProgress.setVisibility(View.VISIBLE);
                        startWaveAnimation(viewHolder);
                        viewHolder.playButton.setImageResource(R.drawable.pause);
                        viewHolder.tvAudioName.setTextColor(context.getResources().getColor(R.color.green));

                        // Save audio and playlist
                        saveAudioLocally(audio);
                        saveFullPlaylist();
                        PlayerManager.setAudioList(songsList, PlayerManager.PlaylistType.OFFLINE);

                        // Use playOfflineAudio
                        PlayerManager.playOfflineAudio(audio, () -> {
                            viewHolder.bufferProgress.setVisibility(View.GONE);
                        });

                        // Call listener WITH the progress bar
                        if (listener != null) {
                            listener.onItemClick(audio, viewHolder.bufferProgress);
                        }
                    } else {
                        // If ViewHolder not found, use fallback without progress bar
                        Log.w(TAG, "ViewHolder not found for position " + position + ", using fallback playback");

                        // Save audio and playlist
                        saveAudioLocally(audio);
                        saveFullPlaylist();
                        PlayerManager.setAudioList(songsList, PlayerManager.PlaylistType.OFFLINE);

                        // Use playOfflineAudio
                        PlayerManager.playOfflineAudio(audio, null);

                        // Call listener WITHOUT progress bar (pass null)
                        if (listener != null) {
                            listener.onItemClick(audio, null);
                        }
                    }

                    // Update all items to reflect the new current song
                    notifyDataSetChanged();
                }
            });

            // Handle Delete click
            menuDeleteLayout.setOnClickListener(v -> {
                popupWindow.dismiss();
                deleteSong(audio);
            });

            // Get the anchor view location
            int[] location = new int[2];
            anchorView.getLocationOnScreen(location);

            // Calculate position to show popup aligned to right edge and vertically centered
            int xPos = location[0] + anchorView.getWidth() - widthInPx;
            int yPos = location[1] - (heightInPx / 2) + (anchorView.getHeight() / 2);

            // Adjust if goes off screen
            if (yPos < 0) yPos = 0;

            // Also check if popup goes off bottom of screen
            int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
            if (yPos + heightInPx > screenHeight) {
                yPos = screenHeight - heightInPx - 50; // 50dp margin from bottom
            }

            // Show popup at calculated position
            popupWindow.showAtLocation(
                    anchorView,
                    Gravity.NO_GRAVITY,
                    xPos,
                    yPos
            );

            // Close popup when clicked outside
            popupWindow.setTouchInterceptor((v1, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_OUTSIDE) {
                    popupWindow.dismiss();
                    return true;
                }
                return false;
            });
        }
    }

    // Helper method to find ViewHolder for a position
    private SongViewHolder findViewHolderForPosition(int position) {
        if (recyclerView != null) {
            try {
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(position);
                if (viewHolder instanceof SongViewHolder) {
                    return (SongViewHolder) viewHolder;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error finding ViewHolder for position: " + position, e);
            }
        } else {
            Log.w(TAG, "RecyclerView is null, cannot find ViewHolder");
        }
        return null;
    }

    private void deleteSong(AudioModel audio) {
        Log.d(TAG, "Attempting to delete song: " + audio.getAudioName());

        // Get position before removing
        int position = songsList.indexOf(audio);

        if (songDeletedListener != null) {
            songDeletedListener.onSongDeleted(audio, position);
        }
    }

    @Override
    public int getItemCount() {
        return songsList != null ? songsList.size() : 0;
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView tvAudioName, tvcategoryName, tvDownloadStatus;
        ImageView playButton, imgAudioArt, menu, ivDownloadStatus;
        ProgressBar bufferProgress;
        View bar1, bar2, bar3;
        View waveContainer;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAudioName = itemView.findViewById(R.id.audioTitle);
            tvcategoryName = itemView.findViewById(R.id.audioArtist);
            playButton = itemView.findViewById(R.id.playButton);
            imgAudioArt = itemView.findViewById(R.id.imgAudioArt);
            menu = itemView.findViewById(R.id.menu);
            bufferProgress = itemView.findViewById(R.id.imageProgress);
            waveContainer = itemView.findViewById(R.id.waveContainer);
            bar1 = itemView.findViewById(R.id.bar1);
            bar2 = itemView.findViewById(R.id.bar2);
            bar3 = itemView.findViewById(R.id.bar3);
            ivDownloadStatus = itemView.findViewById(R.id.ivDownloadStatus);
            tvDownloadStatus = itemView.findViewById(R.id.tvDownloadStatus);
        }
    }

    private void startWaveAnimation(SongViewHolder holder) {
        holder.waveContainer.setVisibility(View.VISIBLE);

        // Create a gentle wave animation that flows from left to right
        ObjectAnimator anim1 = ObjectAnimator.ofFloat(holder.bar1, "scaleY", 1.0f, 1.2f, 1.0f);
        anim1.setDuration(800);
        anim1.setRepeatCount(ValueAnimator.INFINITE);
        anim1.setInterpolator(new LinearInterpolator());
        anim1.start();

        ObjectAnimator anim2 = ObjectAnimator.ofFloat(holder.bar2, "scaleY", 1.0f, 1.4f, 1.0f);
        anim2.setDuration(800);
        anim2.setRepeatCount(ValueAnimator.INFINITE);
        anim2.setInterpolator(new LinearInterpolator());
        anim2.start();

        ObjectAnimator anim3 = ObjectAnimator.ofFloat(holder.bar3, "scaleY", 1.0f, 1.6f, 1.0f);
        anim3.setDuration(800);
        anim3.setRepeatCount(ValueAnimator.INFINITE);
        anim3.setInterpolator(new LinearInterpolator());
        anim3.start();

        // Stagger the animations to create a flowing wave effect
        anim1.setStartDelay(0);
        anim2.setStartDelay(200);
        anim3.setStartDelay(400);

        // Save animators as tags for later stop
        holder.bar1.setTag(anim1);
        holder.bar2.setTag(anim2);
        holder.bar3.setTag(anim3);

        Log.d(TAG, "Wave animation started");
    }

    private void stopWaveAnimation(SongViewHolder holder) {
        holder.waveContainer.setVisibility(View.GONE);

        // Cancel all animations
        if (holder.bar1.getTag() instanceof ObjectAnimator) {
            ((ObjectAnimator) holder.bar1.getTag()).cancel();
        }
        if (holder.bar2.getTag() instanceof ObjectAnimator) {
            ((ObjectAnimator) holder.bar2.getTag()).cancel();
        }
        if (holder.bar3.getTag() instanceof ObjectAnimator) {
            ((ObjectAnimator) holder.bar3.getTag()).cancel();
        }

        // Reset to original heights
        holder.bar1.setScaleY(1f);
        holder.bar2.setScaleY(1f);
        holder.bar3.setScaleY(1f);

        holder.bar1.setTag(null);
        holder.bar2.setTag(null);
        holder.bar3.setTag(null);
    }


    private void saveAudioLocally(AudioModel audio) {
        if (context != null) {
            android.content.SharedPreferences prefs = context.getSharedPreferences("SavedAudio", Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.putString("audioName", audio.getAudioName());
            editor.putString("audioUrl", audio.getAudioUrl());
            editor.putString("imageUrl", audio.getImageUrl());
            editor.putString("audioArtist", audio.getcategoryName());
            editor.apply();

            Log.d(TAG, "Saved current audio: " + audio.getAudioName() + ", Artist: " + audio.getcategoryName());
        }
    }

    private void saveFullPlaylist() {
        if (context != null && songsList != null && !songsList.isEmpty()) {
            android.content.SharedPreferences prefs = context.getSharedPreferences("SavedPlaylist", Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();

            // Save playlist size
            editor.putInt("playlistSize", songsList.size());

            // Save each song in the playlist
            for (int i = 0; i < songsList.size(); i++) {
                AudioModel song = songsList.get(i);
                editor.putString("songName_" + i, song.getAudioName());
                editor.putString("songUrl_" + i, song.getAudioUrl());
                editor.putString("songImage_" + i, song.getImageUrl());
                editor.putString("songArtist_" + i, song.getcategoryName());
            }

            editor.apply();
            Log.d(TAG, "Saved full playlist with " + songsList.size() + " songs");
        }
    }

    public void showNextItemProgress(int nextIndex) {
        preparingPosition = nextIndex;
        notifyDataSetChanged();
        Log.d(TAG, "Showing progress on next item at position: " + nextIndex);
    }

    /**
     * Show progress bar on the previous item (when previous is called from PlayerManager)
     */
    public void showPreviousItemProgress(int prevIndex) {
        preparingPosition = prevIndex;
        notifyDataSetChanged();
        Log.d(TAG, "Showing progress on previous item at position: " + prevIndex);
    }

    public void clearPreparingProgress() {
        preparingPosition = -1;
        notifyDataSetChanged();
    }

    public void updatePlayingIndicators() {
        updateCurrentPlayingPosition();
        preparingPosition = -1;
        notifyDataSetChanged();
    }

    private void updateCurrentPlayingPosition() {
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio != null && songsList != null) {
            for (int i = 0; i < songsList.size(); i++) {
                AudioModel song = songsList.get(i);
                if (currentAudio.getAudioUrl() != null &&
                        currentAudio.getAudioUrl().equals(song.getAudioUrl())) {
                    currentPlayingPosition = i;
                    return;
                }
            }
        }
        currentPlayingPosition = -1;
    }

    public int getCurrentPlayingPosition() {
        return currentPlayingPosition;
    }
}