package com.Saalai.SalaiMusicApp.Adapters;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.MenuBottomSheetFragment;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class OfflinePlaylistAudioAdapter extends RecyclerView.Adapter<OfflinePlaylistAudioAdapter.OfflineAudioViewHolder> {

    private ArrayList<AudioModel> audioList;
    private Context context;
    private OnItemClickListener listener;
    private boolean isOfflineMode;

    public interface OnItemClickListener {
        void onItemClick(AudioModel audioModel, ProgressBar progressBar);
    }

    // Constructor for offline mode
    public OfflinePlaylistAudioAdapter(ArrayList<AudioModel> audioList, Context context, boolean isOfflineMode, OnItemClickListener listener) {
        this.audioList = audioList;
        this.context = context;
        this.isOfflineMode = isOfflineMode;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OfflineAudioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_audiooffline, parent, false);
        return new OfflineAudioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OfflineAudioViewHolder holder, int position) {
        AudioModel audio = audioList.get(position);
        holder.tvAudioName.setText(audio.getAudioName());

        // Check if this is the currently playing song
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        boolean isCurrent = currentAudio != null &&
                (currentAudio.getAudioUrl().equals(audio.getAudioUrl()) ||
                        (audio.getDownloadPath() != null && audio.getDownloadPath().equals(currentAudio.getDownloadPath())));

        // Set default state
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

        // Load image - check if song has local image path
        String imageUrl = audio.getImageUrl();
        String localImagePath = audio.getDownloadPath();

        // Try to load local image if available
        if (localImagePath != null && !localImagePath.isEmpty()) {
            // Extract image path from audio path (assuming same directory)
            String imagePath = localImagePath.replace(".mp3", ".jpg").replace(".m4a", ".jpg");

            try {
                Picasso.get()
                        .load(new java.io.File(imagePath))
                        .placeholder(R.drawable.video_placholder)
                        .error(R.drawable.video_placholder)
                        .into(holder.imgAudioArt);
            } catch (Exception e) {
                // Fallback to online image
                loadOnlineImage(imageUrl, holder);
            }
        } else {
            // Load online image
            loadOnlineImage(imageUrl, holder);
        }

        // Set click listeners
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

                // For offline mode, use local file path
                if (isOfflineMode && audio.getDownloadPath() != null) {
                    // Play from local file
                    PlayerManager.setAudioList(audioList, PlayerManager.PlaylistType.OFFLINE);
                    PlayerManager.playOfflineAudio(audio, () -> {
                        holder.bufferProgress.setVisibility(View.GONE);
                        startWaveAnimation(holder);
                        holder.playButton.setImageResource(R.drawable.pause);
                        holder.tvAudioName.setTextColor(context.getResources().getColor(R.color.green));
                    });
                } else {
                    // Fallback to online playback
                    PlayerManager.setAudioList(audioList, PlayerManager.PlaylistType.ONLINE);
                    PlayerManager.playAudio(audio, () -> {
                        holder.bufferProgress.setVisibility(View.GONE);
                        startWaveAnimation(holder);
                        holder.playButton.setImageResource(R.drawable.pause);
                        holder.tvAudioName.setTextColor(context.getResources().getColor(R.color.green));
                    });
                }

                if (listener != null) {
                    listener.onItemClick(audio, holder.bufferProgress);
                }
            }
        });

        // Menu button click
        holder.menu.setOnClickListener(v -> {
            if (context instanceof AppCompatActivity) {
                AppCompatActivity activity = (AppCompatActivity) context;
                MenuBottomSheetFragment bottomSheetFragment = new MenuBottomSheetFragment();

                Bundle args = new Bundle();
                args.putString("audioName", audio.getAudioName());
                args.putString("audioUrl", audio.getAudioUrl());
                args.putString("artistName", audio.getcategoryName());
                args.putString("imageUrl", audio.getImageUrl());

                // Add download path for offline songs
                if (audio.getDownloadPath() != null) {
                    args.putString("downloadPath", audio.getDownloadPath());
                }

                bottomSheetFragment.setArguments(args);
                bottomSheetFragment.show(activity.getSupportFragmentManager(), "OfflineMenuBottomSheet");
            }
        });

        // Show offline indicator
        if (audio.isDownloaded() || audio.getDownloadPath() != null) {
            holder.offlineIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.offlineIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return audioList.size();
    }

    // Helper method to load online image
    private void loadOnlineImage(String imageUrl, OfflineAudioViewHolder holder) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.video_placholder)
                    .error(R.drawable.video_placholder)
                    .into(holder.imgAudioArt, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d("OfflinePlaylistAudioAdapter", "Image loaded: " + imageUrl);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("OfflinePlaylistAudioAdapter", "Image loading failed: " + imageUrl, e);
                        }
                    });
        } else {
            holder.imgAudioArt.setImageResource(R.drawable.video_placholder);
        }
    }

    static class OfflineAudioViewHolder extends RecyclerView.ViewHolder {
        TextView tvAudioName;
        ImageView playButton, imgAudioArt, menu, offlineIndicator;
        ProgressBar bufferProgress;
        View bar1, bar2, bar3;
        View waveContainer;

        public OfflineAudioViewHolder(@NonNull View itemView) {
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

            // Add offline indicator (you need to add this in your item_audio.xml)
            offlineIndicator = itemView.findViewById(R.id.offlineIndicator);
        }
    }

    private void startWaveAnimation(OfflineAudioViewHolder holder) {
        holder.waveContainer.setVisibility(View.VISIBLE);

        // First bar - small wave
        ObjectAnimator anim1 = ObjectAnimator.ofFloat(holder.bar1, "scaleY", 0.7f, 1.4f);
        anim1.setDuration(300);
        anim1.setRepeatCount(ValueAnimator.INFINITE);
        anim1.setRepeatMode(ValueAnimator.REVERSE);
        anim1.setInterpolator(new LinearInterpolator());
        anim1.start();

        // Second bar - medium wave
        ObjectAnimator anim2 = ObjectAnimator.ofFloat(holder.bar2, "scaleY", 0.8f, 1.8f);
        anim2.setDuration(400);
        anim2.setRepeatCount(ValueAnimator.INFINITE);
        anim2.setRepeatMode(ValueAnimator.REVERSE);
        anim2.setInterpolator(new LinearInterpolator());
        anim2.start();

        // Third bar - big wave
        ObjectAnimator anim3 = ObjectAnimator.ofFloat(holder.bar3, "scaleY", 0.9f, 2.3f);
        anim3.setDuration(350);
        anim3.setRepeatCount(ValueAnimator.INFINITE);
        anim3.setRepeatMode(ValueAnimator.REVERSE);
        anim3.setInterpolator(new LinearInterpolator());
        anim3.start();

        // Save animators as tags for later stop
        holder.bar1.setTag(anim1);
        holder.bar2.setTag(anim2);
        holder.bar3.setTag(anim3);
    }

    private void stopWaveAnimation(OfflineAudioViewHolder holder) {
        holder.waveContainer.setVisibility(View.GONE);

        if (holder.bar1.getTag() instanceof ObjectAnimator) ((ObjectAnimator) holder.bar1.getTag()).cancel();
        if (holder.bar2.getTag() instanceof ObjectAnimator) ((ObjectAnimator) holder.bar2.getTag()).cancel();
        if (holder.bar3.getTag() instanceof ObjectAnimator) ((ObjectAnimator) holder.bar3.getTag()).cancel();

        holder.bar1.setScaleY(1f);
        holder.bar2.setScaleY(1f);
        holder.bar3.setScaleY(1f);
    }

    public void updateList(ArrayList<AudioModel> newList) {
        audioList.clear();
        audioList.addAll(newList);
        notifyDataSetChanged();
    }

    // Get song at position
    public AudioModel getSongAt(int position) {
        if (position >= 0 && position < audioList.size()) {
            return audioList.get(position);
        }
        return null;
    }
}