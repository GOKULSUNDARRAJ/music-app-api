package com.Saalai.SalaiMusicApp.Adapters;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.AudioDownloadManager;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class AudioAdapterfordownload extends RecyclerView.Adapter<AudioAdapterfordownload.AudioViewHolder> {

    private ArrayList<AudioModel> audioList;
    private Context context;
    private OnItemClickListener listener;
    private OnSongDeletedListener songDeletedListener;
    private RecyclerView recyclerView;
    private static final String TAG = "AudioAdapterfordownload";

    // Interface for drag handle clicks
    public interface OnDragHandleListener {
        void onDragHandleClick(RecyclerView.ViewHolder viewHolder);
    }

    private OnDragHandleListener dragHandleListener;

    public interface OnItemClickListener {
        void onItemClick(AudioModel audioModel, ProgressBar progressBar);
    }

    public interface OnSongDeletedListener {
        void onSongDeleted(AudioModel deletedAudio, int position);
    }

    public AudioAdapterfordownload(ArrayList<AudioModel> audioList, Context context, OnItemClickListener listener) {
        this.audioList = audioList;
        this.context = context;
        this.listener = listener;
    }

    public void setOnDragHandleListener(OnDragHandleListener listener) {
        this.dragHandleListener = listener;
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
    public AudioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_audio_for_download, parent, false);
        return new AudioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AudioViewHolder holder, int position) {
        AudioModel audio = audioList.get(position);
        holder.tvAudioName.setText(audio.getAudioName());

        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        boolean isCurrent = currentAudio != null && currentAudio.getAudioUrl() != null &&
                currentAudio.getAudioUrl().equals(audio.getAudioUrl());

        // Set default
        holder.bufferProgress.setVisibility(View.GONE);
        stopWaveAnimation(holder);

        if (isCurrent && PlayerManager.isPlaying()) {
            holder.playButton.setImageResource(R.drawable.pause);
            holder.tvAudioName.setTextColor(context.getResources().getColor(R.color.green));
            startWaveAnimation(holder);
        } else {
            holder.playButton.setImageResource(R.drawable.play_player);
            holder.tvAudioName.setTextColor(context.getResources().getColor(R.color.white));
        }

        // Load image
        String imageUrl = audio.getImageUrl();

        Log.d("AudioAdapter", "Image URL: " + imageUrl);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.video_placholder)
                    .error(R.drawable.video_placholder)
                    .into(holder.imgAudioArt, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d("AudioAdapter", "Image loaded successfully: " + imageUrl);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("AudioAdapter", "Image loading failed: " + imageUrl, e);
                        }
                    });

        } else {
            Log.w("AudioAdapter", "Image URL is empty. Loading placeholder.");
            holder.imgAudioArt.setImageResource(R.drawable.video_placholder);
        }

        // Set up drag handle click listener
        if (holder.dragHandle != null) {
            holder.dragHandle.setOnClickListener(v -> {
                if (dragHandleListener != null) {
                    dragHandleListener.onDragHandleClick(holder);
                }
            });

            // Also support long press on the drag handle for better UX
            holder.dragHandle.setOnLongClickListener(v -> {
                if (dragHandleListener != null) {
                    dragHandleListener.onDragHandleClick(holder);
                    return true;
                }
                return false;
            });
        }

        // Set up item click listener for playing
        holder.itemView.setOnClickListener(v -> {
            if (isCurrent) {
                if (PlayerManager.isPlaying()) {
                    PlayerManager.pausePlayback();
                    stopWaveAnimation(holder);
                    holder.playButton.setImageResource(R.drawable.play_player);
                    holder.tvAudioName.setTextColor(context.getResources().getColor(R.color.white));
                } else {
                    PlayerManager.resumePlayback();
                    startWaveAnimation(holder);
                    holder.playButton.setImageResource(R.drawable.pause);
                    holder.tvAudioName.setTextColor(context.getResources().getColor(R.color.green));
                }
            } else {
                holder.bufferProgress.setVisibility(View.VISIBLE);

                // 🔴 FOR OFFLINE SONGS: DON'T SAVE CATEGORY ID
                // Clear any existing category ID from SharedPreferences
                android.content.SharedPreferences prefs = context.getSharedPreferences("SavedAudio", Context.MODE_PRIVATE);
                prefs.edit().remove("audioCategoryId").apply();

                // Save audio without category ID
                saveAudioLocally(audio);
                saveFullPlaylist();
                PlayerManager.setAudioList(audioList, PlayerManager.PlaylistType.OFFLINE);

                // Play offline audio
                PlayerManager.playOfflineAudio(audio, () -> {
                    holder.bufferProgress.setVisibility(View.GONE);
                    startWaveAnimation(holder);
                    holder.playButton.setImageResource(R.drawable.pause);
                    holder.tvAudioName.setTextColor(context.getResources().getColor(R.color.green));
                });

                if (listener != null) {
                    listener.onItemClick(audio, holder.bufferProgress);
                }
            }
        });

        holder.menu.setOnClickListener(v -> {
            showCustomMenu(v, position, audio);
        });

        logSongList();
    }

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
            boolean isCurrent = currentAudio != null && currentAudio.getAudioUrl() != null &&
                    currentAudio.getAudioUrl().equals(audio.getAudioUrl());

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
                        notifyItemChanged(position);
                    } else {
                        PlayerManager.resumePlayback();
                        notifyItemChanged(position);
                    }
                } else {
                    // Play this song from the beginning
                    AudioViewHolder viewHolder = findViewHolderForPosition(position);

                    if (viewHolder != null) {
                        viewHolder.bufferProgress.setVisibility(View.VISIBLE);
                        startWaveAnimation(viewHolder);
                        viewHolder.playButton.setImageResource(R.drawable.pause);
                        viewHolder.tvAudioName.setTextColor(context.getResources().getColor(R.color.green));

                        // 🔴 FOR OFFLINE SONGS: DON'T SAVE CATEGORY ID
                        // Clear category ID from SharedPreferences
                        android.content.SharedPreferences prefs = context.getSharedPreferences("SavedAudio", Context.MODE_PRIVATE);
                        prefs.edit().remove("audioCategoryId").apply();

                        saveAudioLocally(audio);
                        saveFullPlaylist();
                        PlayerManager.setAudioList(audioList, PlayerManager.PlaylistType.OFFLINE);

                        PlayerManager.playOfflineAudio(audio, () -> {
                            viewHolder.bufferProgress.setVisibility(View.GONE);
                        });

                        if (listener != null) {
                            listener.onItemClick(audio, viewHolder.bufferProgress);
                        }
                    } else {
                        // If ViewHolder not found
                        Log.w(TAG, "ViewHolder not found for position " + position);

                        // 🔴 FOR OFFLINE SONGS: DON'T SAVE CATEGORY ID
                        android.content.SharedPreferences prefs = context.getSharedPreferences("SavedAudio", Context.MODE_PRIVATE);
                        prefs.edit().remove("audioCategoryId").apply();

                        saveAudioLocally(audio);
                        saveFullPlaylist();
                        PlayerManager.setAudioList(audioList, PlayerManager.PlaylistType.OFFLINE);

                        PlayerManager.playOfflineAudio(audio, null);

                        if (listener != null) {
                            listener.onItemClick(audio, null);
                        }
                    }

                    notifyDataSetChanged();
                }
            });

            // Handle Delete click
            menuDeleteLayout.setOnClickListener(v -> {
                popupWindow.dismiss();
                deleteDownloadedSong(audio);
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
    private AudioViewHolder findViewHolderForPosition(int position) {
        if (recyclerView != null) {
            try {
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(position);
                if (viewHolder instanceof AudioViewHolder) {
                    return (AudioViewHolder) viewHolder;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error finding ViewHolder for position: " + position, e);
            }
        } else {
            Log.w(TAG, "RecyclerView is null, cannot find ViewHolder");
        }
        return null;
    }

    private void deleteDownloadedSong(AudioModel audio) {
        Log.d(TAG, "Attempting to delete downloaded song: " + audio.getAudioName());

        // Initialize download manager
        AudioDownloadManager downloadManager = new AudioDownloadManager(context);

        // Delete the song
        boolean isDeleted = downloadManager.deleteDownloadedSong(audio);

        if (isDeleted) {
            Log.d(TAG, "Song deleted successfully: " + audio.getAudioName());
            showToast("Song deleted successfully");

            // Get position before removing
            int position = audioList.indexOf(audio);

            // Remove from the current list
            if (position != -1) {
                audioList.remove(position);
                notifyItemRemoved(position);

                // Notify listener
                if (songDeletedListener != null) {
                    songDeletedListener.onSongDeleted(audio, position);
                }

                // Broadcast updates
                broadcastDownloadUpdate();
                broadcastSongChanged();
            }
        } else {
            Log.e(TAG, "Failed to delete song: " + audio.getAudioName());
            showToast("Failed to delete song");
        }
    }

    private void broadcastDownloadUpdate() {
        Log.d(TAG, "Broadcasting download update");
        if (context != null) {
            Intent downloadUpdateIntent = new Intent("DOWNLOAD_UPDATE");
            downloadUpdateIntent.setPackage(context.getPackageName());
            context.sendBroadcast(downloadUpdateIntent);
            Log.d(TAG, "DOWNLOAD_UPDATE broadcast sent");
        }
    }

    private void broadcastSongChanged() {
        Log.d(TAG, "Broadcasting song changed");
        if (context != null) {
            Intent songChangedIntent = new Intent("SONG_CHANGED");
            songChangedIntent.setPackage(context.getPackageName());
            context.sendBroadcast(songChangedIntent);
            Log.d(TAG, "SONG_CHANGED broadcast sent");
        }
    }

    private void showToast(String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return audioList.size();
    }

    static class AudioViewHolder extends RecyclerView.ViewHolder {
        TextView tvAudioName;
        ImageView playButton, imgAudioArt, menu, dragHandle;
        ProgressBar bufferProgress;
        View bar1, bar2, bar3;
        View waveContainer;

        public AudioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAudioName = itemView.findViewById(R.id.audioTitle);
            playButton = itemView.findViewById(R.id.playButton);
            imgAudioArt = itemView.findViewById(R.id.imgAudioArt);
            menu = itemView.findViewById(R.id.menu);
            bufferProgress = itemView.findViewById(R.id.imageProgress);
            waveContainer = itemView.findViewById(R.id.waveContainer);
            bar1 = itemView.findViewById(R.id.bar1);
            bar2 = itemView.findViewById(R.id.bar2);
            bar3 = itemView.findViewById(R.id.bar3);

            // Initialize drag handle
            dragHandle = itemView.findViewById(R.id.dragHandle);
        }
    }

    private void startWaveAnimation(AudioViewHolder holder) {
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

    private void stopWaveAnimation(AudioViewHolder holder) {
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

    // In AudioAdapterfordownload.java, update the saveAudioLocally method:
    private void saveAudioLocally(AudioModel audio) {
        if (context != null) {
            android.content.SharedPreferences prefs = context.getSharedPreferences("SavedAudio", Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.putString("audioName", audio.getAudioName());
            editor.putString("audioUrl", audio.getAudioUrl());
            editor.putString("imageUrl", audio.getImageUrl());
            editor.putString("audioArtist", audio.getcategoryName());

            // 🔴 DON'T SAVE CATEGORY ID FOR OFFLINE SONGS
            editor.remove("audioCategoryId"); // Remove category ID for offline songs

            editor.apply();

            Log.d("AudioAdapter", "Saved OFFLINE audio (no category ID): " + audio.getAudioName() +
                    ", Artist: " + audio.getcategoryName());
        }
    }

    private void saveFullPlaylist() {
        if (context != null && audioList != null && !audioList.isEmpty()) {
            android.content.SharedPreferences prefs = context.getSharedPreferences("SavedPlaylist", Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();

            // Clear existing data
            editor.clear();

            // Save playlist size
            editor.putInt("playlistSize", audioList.size());
            editor.putString("playlistType", PlayerManager.PlaylistType.OFFLINE.name());

            // Save each song in the playlist
            for (int i = 0; i < audioList.size(); i++) {
                AudioModel song = audioList.get(i);
                editor.putString("songName_" + i, song.getAudioName());
                editor.putString("songUrl_" + i, song.getAudioUrl());
                editor.putString("songImage_" + i, song.getImageUrl());
                editor.putString("songArtist_" + i, song.getcategoryName());
                editor.putString("songId_" + i, song.getSongId());

                // Save download info
                if (song.getDownloadPath() != null && !song.getDownloadPath().isEmpty()) {
                    editor.putString("songDownloadPath_" + i, song.getDownloadPath());
                    editor.putBoolean("songDownloaded_" + i, true);
                }

                // 🔴 DON'T SAVE CATEGORY ID FOR OFFLINE SONGS
                editor.remove("songCategoryId_" + i);
            }

            editor.apply();
            Log.d("AudioAdapter", "Saved full offline playlist with " + audioList.size() + " songs");
        }
    }

    public void logSongList() {
        Log.d(TAG, "Current song list (" + audioList.size() + " songs):");
        for (int i = 0; i < audioList.size(); i++) {
            AudioModel song = audioList.get(i);
            Log.d(TAG, "[" + i + "] " + song.getAudioName() +
                    " | Artist: " + song.getcategoryName() +
                    " | Image: " + song.getImageUrl());
        }
    }

    /**
     * Method to update the playlist and notify adapter
     * @param newList The reordered playlist
     */
    public void updatePlaylist(ArrayList<AudioModel> newList) {
        this.audioList = newList;
        notifyDataSetChanged();
    }
}