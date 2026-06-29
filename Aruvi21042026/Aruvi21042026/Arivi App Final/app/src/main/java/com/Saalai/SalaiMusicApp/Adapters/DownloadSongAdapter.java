package com.Saalai.SalaiMusicApp.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.Locale;

public class DownloadSongAdapter extends RecyclerView.Adapter<DownloadSongAdapter.ViewHolder> {

    private static final String TAG = "DownloadSongAdapter";
    private ArrayList<AudioModel> songs;
    private Context context;

    // REMOVE the listener interface for now
    // We'll handle clicks in Fragment directly

    public DownloadSongAdapter(ArrayList<AudioModel> songs) {
        this.songs = songs != null ? songs : new ArrayList<>();
        Log.d(TAG, "Adapter created with " + this.songs.size() + " songs");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song_downlod, parent, false);

        ViewHolder holder = new ViewHolder(view);

        // DEBUG: Make absolutely sure views are clickable
        holder.itemView.setClickable(true);
        holder.itemView.setFocusable(true);
        holder.itemView.setFocusableInTouchMode(true);

        if (holder.btnOptions != null) {
            holder.btnOptions.setClickable(true);
            holder.btnOptions.setFocusable(true);
            holder.btnOptions.setFocusableInTouchMode(true);
        }

        Log.d(TAG, "onCreateViewHolder: holder created");
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= songs.size()) {
            Log.e(TAG, "Invalid position: " + position);
            return;
        }

        AudioModel song = songs.get(position);

        if (song == null) {
            holder.tvTitle.setText("Unknown Song");
            holder.tvArtist.setText("Unknown Artist");
            holder.tvDuration.setText("--:--");
            return;
        }

        // Set song title
        String title = song.getAudioName() != null ? song.getAudioName() : "Unknown Song";
        holder.tvTitle.setText(title);

        // Set artist
        String artist = song.getcategoryName() != null ? song.getcategoryName() : "Unknown Artist";
        holder.tvArtist.setText(artist);

        // Set duration
        String durationText = formatDuration(song);
        holder.tvDuration.setText(durationText);

        // Show download indicator
        if (song.isDownloaded()) {
            holder.tvDuration.setText("✓ " + durationText);
            holder.tvDuration.setTextColor(context.getResources().getColor(R.color.green));
        }

        // Load album art
        if (song.getImageUrl() != null && !song.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(song.getImageUrl())
                    .apply(new RequestOptions()
                            .centerCrop())
                    .into(holder.ivAlbumArt);
        }

        // IMPORTANT: NO CLICK LISTENERS HERE
        // We'll handle clicks via RecyclerView touch listener

        Log.d(TAG, "Bound position " + position + ": " + title);
    }

    private String formatDuration(AudioModel song) {
        try {
            long durationMillis = song.getDurationInMillis();
            if (durationMillis > 0) {
                long seconds = (durationMillis / 1000) % 60;
                long minutes = (durationMillis / (1000 * 60)) % 60;
                return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting duration: " + e.getMessage());
        }
        return "--:--";
    }

    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAlbumArt, btnOptions;
        TextView tvTitle, tvArtist, tvDuration;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Find views
            ivAlbumArt = itemView.findViewById(R.id.ivAlbumArt);
            btnOptions = itemView.findViewById(R.id.btnOptions);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvDuration = itemView.findViewById(R.id.tvDuration);

            // Make absolutely sure
            itemView.setClickable(true);
            itemView.setFocusable(true);
            itemView.setFocusableInTouchMode(true);

            Log.d("ViewHolder", "ViewHolder created - all views found");
        }
    }
}