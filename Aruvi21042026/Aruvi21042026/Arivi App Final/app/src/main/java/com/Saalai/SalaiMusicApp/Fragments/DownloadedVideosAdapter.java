package com.Saalai.SalaiMusicApp.Fragments;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.VideoDownloadManager;

import java.io.File;
import java.util.List;

// Adapter class
public class DownloadedVideosAdapter extends RecyclerView.Adapter<DownloadedVideosAdapter.ViewHolder> {

    private Context context;
    private List<VideoDownloadManager.DownloadedVideo> videos;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(VideoDownloadManager.DownloadedVideo video);
        void onItemLongClick(VideoDownloadManager.DownloadedVideo video, int position);
    }

    public DownloadedVideosAdapter(Context context, List<VideoDownloadManager.DownloadedVideo> videos, OnItemClickListener listener) {
        this.context = context;
        this.videos = videos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_downloaded_video, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VideoDownloadManager.DownloadedVideo video = videos.get(position);

        holder.tvTitle.setText(video.getTitle());
        holder.tvSize.setText(getFileSize(video.getFilePath()));
        holder.tvDate.setText(video.getFormattedDownloadTime());

        // Load thumbnail if available
        if (video.getThumbnailUrl() != null && !video.getThumbnailUrl().isEmpty()) {
            Glide.with(context)
                    .load(video.getThumbnailUrl())
                    .apply(new RequestOptions())
                    .into(holder.ivThumbnail);
        } else {

        }

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(video);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onItemLongClick(video, position);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    private String getFileSize(String filePath) {
        try {
            File file = new File(filePath);
            long size = file.length();

            if (size < 1024) {
                return size + " B";
            } else if (size < 1024 * 1024) {
                return String.format("%.1f KB", size / 1024.0);
            } else if (size < 1024 * 1024 * 1024) {
                return String.format("%.1f MB", size / (1024.0 * 1024.0));
            } else {
                return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
            }
        } catch (Exception e) {
            return "Unknown size";
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvTitle;
        TextView tvSize;
        TextView tvDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSize = itemView.findViewById(R.id.tvSize);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}