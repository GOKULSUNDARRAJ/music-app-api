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

import com.Saalai.SalaiMusicApp.MenuBottomSheetFragment;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class AudioAdapter extends RecyclerView.Adapter<AudioAdapter.AudioViewHolder> {

    private static final String TAG = "AudioAdapter";
    private ArrayList<AudioModel> audioList;
    private Context context;
    private OnItemClickListener listener;
    private RecyclerView recyclerView;

    // Single field for preparing position
    private int preparingPosition = -1;

    public interface OnItemClickListener {
        void onItemClick(AudioModel audioModel, ProgressBar progressBar);
    }

    public AudioAdapter(ArrayList<AudioModel> audioList, Context context, OnItemClickListener listener) {
        this.audioList = audioList;
        this.context = context;
        this.listener = listener;
    }

    // Method to set the RecyclerView reference
    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    // Show progress on next item
    public void showNextItemProgress(int nextIndex) {
        preparingPosition = nextIndex;
        notifyDataSetChanged();
        Log.d(TAG, "Showing progress on next item at index: " + nextIndex);

        // Scroll to make the next item visible
        if (recyclerView != null) {
            recyclerView.smoothScrollToPosition(nextIndex);
        }
    }

    // Show progress on previous item
    public void showPreviousItemProgress(int prevIndex) {
        preparingPosition = prevIndex;
        notifyDataSetChanged();
        Log.d(TAG, "Showing progress on previous item at index: " + prevIndex);

        // Scroll to make the previous item visible
        if (recyclerView != null) {
            recyclerView.smoothScrollToPosition(prevIndex);
        }
    }

    // Clear all preparing progress
    public void clearPreparingProgress() {
        preparingPosition = -1;
        notifyDataSetChanged();
    }

    // Generic method to show progress for any item
    public void showItemProgress(int position) {
        preparingPosition = position;
        notifyDataSetChanged();
        if (recyclerView != null && position >= 0 && position < audioList.size()) {
            recyclerView.smoothScrollToPosition(position);
        }
    }

    @NonNull
    @Override
    public AudioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_audio, parent, false);
        return new AudioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AudioViewHolder holder, int position) {
        AudioModel audio = audioList.get(position);
        holder.tvAudioName.setText(audio.getAudioName());

        // Check if this item is preparing
        boolean isPreparing = (position == preparingPosition);

        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        boolean isCurrent = false;

        // First check by song ID (most reliable)
        if (currentAudio != null && audio.getSongId() != null && currentAudio.getSongId() != null) {
            isCurrent = currentAudio.getSongId().equals(audio.getSongId());
        }

        // If song ID check fails, fall back to URL
        if (!isCurrent && currentAudio != null && audio.getAudioUrl() != null) {
            isCurrent = currentAudio.getAudioUrl().equals(audio.getAudioUrl());
        }

        // IMPORTANT FIX: Hide progress bar if this is the current playing song or if it's not preparing
        // This ensures progress is hidden when playback starts
        if (isCurrent && PlayerManager.isPlaying()) {
            holder.bufferProgress.setVisibility(View.GONE);
            holder.playButton.setImageResource(R.drawable.pause);
            holder.tvAudioName.setTextColor(context.getResources().getColor(R.color.green));
            startWaveAnimation(holder);
        } else if (isPreparing) {
            // Show progress only if preparing and not yet playing
            holder.bufferProgress.setVisibility(View.VISIBLE);
            holder.playButton.setImageResource(R.drawable.play_player);
            holder.tvAudioName.setTextColor(context.getResources().getColor(R.color.white));
            stopWaveAnimation(holder);
            Log.d(TAG, "Showing progress on position: " + position + " - " + audio.getAudioName());
        } else {
            // Normal state - no progress
            holder.bufferProgress.setVisibility(View.GONE);
            holder.playButton.setImageResource(R.drawable.play_player);
            holder.tvAudioName.setTextColor(context.getResources().getColor(R.color.white));
            stopWaveAnimation(holder);
        }

        // Load image
        String imageUrl = audio.getImageUrl();
        Log.d(TAG, "Image URL: " + imageUrl);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.video_placholder)
                    .error(R.drawable.video_placholder)
                    .into(holder.imgAudioArt, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Image loaded successfully: " + imageUrl);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Image loading failed: " + imageUrl, e);
                        }
                    });
        } else {
            Log.w(TAG, "Image URL is empty. Loading placeholder.");
            holder.imgAudioArt.setImageResource(R.drawable.video_placholder);
        }

        holder.itemView.setOnClickListener(v -> {
            // Show progress bar on clicked item
            holder.bufferProgress.setVisibility(View.VISIBLE);

            // Save current preparing position
            preparingPosition = position;
            notifyDataSetChanged(); // This will update other items

            // Save audio locally
            saveAudioLocally(audio);

            // Save the FULL playlist BEFORE setting it
            saveFullPlaylist();

            // Check if the clicked song is downloaded or online
            boolean isDownloaded = audio.getDownloadPath() != null && !audio.getDownloadPath().isEmpty();

            // Set the playlist with correct type
            if (isDownloaded) {
                PlayerManager.setAudioList(audioList, PlayerManager.PlaylistType.OFFLINE);
            } else {
                PlayerManager.setAudioList(audioList, PlayerManager.PlaylistType.ONLINE);
            }

            // Play the clicked song
            PlayerManager.playAudio(audio, () -> {
                // This callback runs when audio starts playing
                // IMPORTANT: Clear preparing position and update UI
                if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                    // Hide progress bar
                    holder.bufferProgress.setVisibility(View.GONE);

                    // Clear preparing position
                    preparingPosition = -1;

                    // Update UI
                    startWaveAnimation(holder);
                    holder.playButton.setImageResource(R.drawable.pause);
                    holder.tvAudioName.setTextColor(context.getResources().getColor(R.color.green));

                    // Update UI for all items
                    notifyDataSetChanged();

                    // Broadcast that song has changed
                    broadcastSongChanged();

                    Log.d(TAG, "Audio prepared and started: " + audio.getAudioName());
                }
            });

            if (listener != null) {
                listener.onItemClick(audio, holder.bufferProgress);
            }
        });

        // Menu click listener
        // Find this section in your AudioAdapter (around line 150-165)
        holder.menu.setOnClickListener(v -> {
            // Use the activity's fragment manager instead of child fragment manager
            if (context instanceof AppCompatActivity) {
                AppCompatActivity activity = (AppCompatActivity) context;
                MenuBottomSheetFragment bottomSheetFragment = new MenuBottomSheetFragment();

                // Pass data to the bottom sheet fragment if needed
                Bundle args = new Bundle();
                args.putString("audioName", audio.getAudioName());
                args.putString("audioUrl", audio.getAudioUrl());
                args.putString("artistName", audio.getcategoryName());
                args.putString("imageUrl", audio.getImageUrl());

                // ✅ Add song ID (like "song_001")
                String songId = audio.getSongId();
                args.putString("songId", songId);

                bottomSheetFragment.setArguments(args);
                bottomSheetFragment.show(activity.getSupportFragmentManager(), "MenuBottomSheet");
            }
        });

        logSongList();
    }

    // Show custom popup menu


    // Open menu bottom sheet
    private void openMenuBottomSheet(AudioModel audio) {
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            MenuBottomSheetFragment bottomSheetFragment = new MenuBottomSheetFragment();

            Bundle args = new Bundle();
            args.putString("audioName", audio.getAudioName());
            args.putString("audioUrl", audio.getAudioUrl());
            args.putString("artistName", audio.getcategoryName());
            args.putString("imageUrl", audio.getImageUrl());
            args.putString("songId", audio.getSongId());

            bottomSheetFragment.setArguments(args);
            bottomSheetFragment.show(activity.getSupportFragmentManager(), "MenuBottomSheet");
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
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return audioList.size();
    }

    static class AudioViewHolder extends RecyclerView.ViewHolder {
        TextView tvAudioName;
        ImageView playButton, imgAudioArt, menu;
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

        // Reset to original heights (based on your XML)
        holder.bar1.setScaleY(1f);  // 18dp height in XML
        holder.bar2.setScaleY(1f);  // 14dp height in XML
        holder.bar3.setScaleY(1f);  // match_parent height in XML

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

            if (audio.getDownloadPath() != null && !audio.getDownloadPath().isEmpty()) {
                editor.putString("downloadPath", audio.getDownloadPath());
                editor.putBoolean("isDownloaded", true);
                Log.d(TAG, "Saved OFFLINE audio: " + audio.getAudioName() +
                        ", Path: " + audio.getDownloadPath());
            } else {
                String categoryId = audio.getCategoryId();
                if (categoryId == null || categoryId.isEmpty()) {
                    AudioModel currentAudio = PlayerManager.getCurrentAudio();
                    if (currentAudio != null && currentAudio.getCategoryId() != null) {
                        categoryId = currentAudio.getCategoryId();
                        audio.setCategoryId(categoryId);
                    }
                }
                editor.putString("audioCategoryId", categoryId);
                editor.putBoolean("isDownloaded", false);
                Log.d(TAG, "Saved ONLINE audio: " + audio.getAudioName() +
                        ", Category ID: " + categoryId);
            }

            editor.apply();
        }
    }

    private void saveFullPlaylist() {
        if (context != null && audioList != null && !audioList.isEmpty()) {
            android.content.SharedPreferences prefs = context.getSharedPreferences("SavedPlaylist", Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();

            editor.putInt("playlistSize", audioList.size());

            boolean hasDownloadedSongs = false;
            for (AudioModel song : audioList) {
                if (song.getDownloadPath() != null && !song.getDownloadPath().isEmpty()) {
                    hasDownloadedSongs = true;
                    break;
                }
            }

            editor.putBoolean("playlistHasOfflineSongs", hasDownloadedSongs);

            for (int i = 0; i < audioList.size(); i++) {
                AudioModel song = audioList.get(i);
                editor.putString("songName_" + i, song.getAudioName());
                editor.putString("songUrl_" + i, song.getAudioUrl());
                editor.putString("songImage_" + i, song.getImageUrl());
                editor.putString("songArtist_" + i, song.getcategoryName());
                editor.putString("songCategoryId_" + i, song.getCategoryId());

                if (song.getDownloadPath() != null && !song.getDownloadPath().isEmpty()) {
                    editor.putString("songDownloadPath_" + i, song.getDownloadPath());
                }

                editor.putString("songId_" + i, song.getSongId());
            }

            editor.apply();
            Log.d(TAG, "Saved full playlist with " + audioList.size() +
                    " songs, has offline songs: " + hasDownloadedSongs);
        }
    }

    private void broadcastSongChanged() {
        if (context != null) {
            Intent intent = new Intent("SONG_CHANGED");
            intent.setPackage(context.getPackageName());
            context.sendBroadcast(intent);
        }
    }

    public void logSongList() {
        if (audioList != null) {
            Log.d(TAG, "=== SONG LIST ===");
            Log.d(TAG, "Total songs: " + audioList.size());

            for (int i = 0; i < audioList.size(); i++) {
                AudioModel song = audioList.get(i);
                Log.d(TAG, "Song #" + (i + 1) + ":");
                Log.d(TAG, "  - Name: " + song.getAudioName());
                Log.d(TAG, "  - URL: " + song.getAudioUrl());
                Log.d(TAG, "  - Song ID: " + song.getSongId());
                Log.d(TAG, "  - Image URL: " + song.getImageUrl());
                Log.d(TAG, "  - Category ID: " + song.getCategoryId());
                Log.d(TAG, "  - Category Name: " + song.getcategoryName());
                Log.d(TAG, "  - Download Path: " + song.getDownloadPath());
                Log.d(TAG, "  - Is Downloaded: " + (song.getDownloadPath() != null && !song.getDownloadPath().isEmpty()));
                Log.d(TAG, "  -----------------");
            }
            Log.d(TAG, "=== END SONG LIST ===");
        }
    }

    // Method to update the playlist
    public void updatePlaylist(ArrayList<AudioModel> newList) {
        this.audioList = newList;
        notifyDataSetChanged();
        logSongList();
    }
}