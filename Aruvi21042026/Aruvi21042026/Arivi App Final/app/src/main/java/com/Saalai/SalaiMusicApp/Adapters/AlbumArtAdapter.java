package com.Saalai.SalaiMusicApp.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.R;
import com.bumptech.glide.Glide;
import java.util.List;

public class AlbumArtAdapter extends RecyclerView.Adapter<AlbumArtAdapter.ViewHolder> {

    private List<AudioModel> audioList;
    private int currentPosition;

    public AlbumArtAdapter(List<AudioModel> audioList, int currentPosition) {
        this.audioList = audioList;
        this.currentPosition = currentPosition;
    }

    public void setCurrentPosition(int position) {
        this.currentPosition = position;
        notifyDataSetChanged();
    }

    public void setAudioList(List<AudioModel> audioList) {
        this.audioList = audioList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album_art, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (audioList == null || position >= audioList.size()) return;

        AudioModel audio = audioList.get(position);

        if (audio.getImageUrl() != null && !audio.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(audio.getImageUrl())
                    .into(holder.imgAlbumArt);
        } else {

        }
    }

    @Override
    public int getItemCount() {
        return audioList != null ? audioList.size() : 0;
    }

    public AudioModel getAudioAtPosition(int position) {
        if (audioList != null && position >= 0 && position < audioList.size()) {
            return audioList.get(position);
        }
        return null;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAlbumArt;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAlbumArt = itemView.findViewById(R.id.imgAlbumArt);
        }
    }
}