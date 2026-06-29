package com.Saalai.SalaiMusicApp.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Models.PlaylistSection;
import com.Saalai.SalaiMusicApp.R;

import java.util.ArrayList;
import java.util.List;

public class YourLikedPlaylistSectionAdapter extends RecyclerView.Adapter<YourLikedPlaylistSectionAdapter.SectionViewHolder> {

    private List<PlaylistSection> sectionList;
    private OnCategoryClickListener onCategoryClickListener;
    private Context context;

    // Track all view holders for refreshing
    private List<SectionViewHolder> viewHolders = new ArrayList<>();

    // Layout type constants
    public static final int LAYOUT_HORIZONTAL = 1;
    public static final int LAYOUT_VERTICAL = 2;
    public static final int LAYOUT_GRID = 3;
    public static final int LAYOUT_GRID_3 = 4;  // New constant for 3-column grid

    // Interface for category click events
    public interface OnCategoryClickListener {
        void onCategoryClick(String categoryName, List<com.Saalai.SalaiMusicApp.Models.AudioModel> songs,
                             String categoryImageUrl, String categoryId);
    }

    public YourLikedPlaylistSectionAdapter(List<PlaylistSection> sectionList, OnCategoryClickListener listener) {
        this.sectionList = sectionList;
        this.onCategoryClickListener = listener;
    }

    @NonNull
    @Override
    public SectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_section_likes, parent, false);
        SectionViewHolder holder = new SectionViewHolder(view);
        viewHolders.add(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull SectionViewHolder holder, int position) {
        PlaylistSection section = sectionList.get(position);
        holder.bind(section);
    }

    @Override
    public int getItemCount() {
        return sectionList.size();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        viewHolders.clear();
    }

    public void updateData(List<PlaylistSection> newSectionList) {
        this.sectionList = newSectionList;
        notifyDataSetChanged();
    }

    public void refreshPlayingState() {
        // Refresh all nested artist adapters
        for (SectionViewHolder holder : viewHolders) {
            if (holder.artistAdapter != null) {
                holder.artistAdapter.refreshPlayingState();
            }
        }
    }

    class SectionViewHolder extends RecyclerView.ViewHolder {
        private TextView sectionTitle;
        private RecyclerView artistsRecyclerView;
        private YourLikedPlaylistArtistAdapter artistAdapter;

        public SectionViewHolder(@NonNull View itemView) {
            super(itemView);
            sectionTitle = itemView.findViewById(R.id.sectionTitle);
            artistsRecyclerView = itemView.findViewById(R.id.artistsRecyclerView);
        }

        public void bind(PlaylistSection section) {
            sectionTitle.setText(section.getSectionName());

            // Create adapter with the section's artists and the click listener
            artistAdapter = new YourLikedPlaylistArtistAdapter(
                    section.getArtists(),
                    (artistName, songs, artistImageUrl) -> {
                        // Find categoryId from the first song
                        String categoryId = null;
                        if (songs != null && !songs.isEmpty()) {
                            categoryId = songs.get(0).getCategoryId();
                        }
                        // Pass to the fragment's click listener
                        if (onCategoryClickListener != null) {
                            onCategoryClickListener.onCategoryClick(artistName, songs, artistImageUrl, categoryId);
                        }
                    }
            );

            // Set layout manager based on section type
            switch (section.getLayoutType()) {
                case LAYOUT_VERTICAL:
                    artistsRecyclerView.setLayoutManager(new LinearLayoutManager(
                            itemView.getContext(),
                            LinearLayoutManager.VERTICAL,
                            false
                    ));
                    break;

                case LAYOUT_GRID:
                    // 2-column grid (original)
                    artistsRecyclerView.setLayoutManager(new GridLayoutManager(
                            itemView.getContext(),
                            2
                    ));
                    break;

                case LAYOUT_GRID_3:
                    // 3-column grid
                    artistsRecyclerView.setLayoutManager(new GridLayoutManager(
                            itemView.getContext(),
                            3
                    ));
                    break;

                case LAYOUT_HORIZONTAL:
                default:
                    artistsRecyclerView.setLayoutManager(new LinearLayoutManager(
                            itemView.getContext(),
                            LinearLayoutManager.HORIZONTAL,
                            false
                    ));
                    break;
            }

            artistsRecyclerView.setAdapter(artistAdapter);
        }
    }
}