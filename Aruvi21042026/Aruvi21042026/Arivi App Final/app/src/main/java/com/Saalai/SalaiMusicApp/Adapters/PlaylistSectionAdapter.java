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

public class PlaylistSectionAdapter extends RecyclerView.Adapter<PlaylistSectionAdapter.SectionViewHolder> {

    private List<PlaylistSection> sectionList;
    private ArtistAdapter.OnArtistClickListener onArtistClickListener;
    private Context context;

    // Track all view holders for refreshing
    private List<SectionViewHolder> viewHolders = new ArrayList<>();

    // Layout type constants
    public static final int LAYOUT_HORIZONTAL = 1;
    public static final int LAYOUT_VERTICAL = 2;
    public static final int LAYOUT_GRID = 3;

    public PlaylistSectionAdapter(List<PlaylistSection> sectionList, ArtistAdapter.OnArtistClickListener listener) {
        this.sectionList = sectionList;
        this.onArtistClickListener = listener;
    }

    @NonNull
    @Override
    public SectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_section, parent, false);
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
        private ArtistAdapter artistAdapter;

        public SectionViewHolder(@NonNull View itemView) {
            super(itemView);
            sectionTitle = itemView.findViewById(R.id.sectionTitle);
            artistsRecyclerView = itemView.findViewById(R.id.artistsRecyclerView);
        }

        public void bind(PlaylistSection section) {
            sectionTitle.setText(section.getSectionName());

            artistAdapter = new ArtistAdapter(section.getArtists(), onArtistClickListener);

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
                    // Use 2 columns for grid layout
                    artistsRecyclerView.setLayoutManager(new GridLayoutManager(
                            itemView.getContext(),
                            2 // Number of columns
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