package com.Saalai.SalaiMusicApp.ShimmerAdapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.R;
import com.facebook.shimmer.ShimmerFrameLayout;

public class ShimmerPlaylistSectionAdapter extends RecyclerView.Adapter<ShimmerPlaylistSectionAdapter.ShimmerSectionViewHolder> {

    private Context context;
    private int sectionCount = 3; // Default number of shimmer sections

    public ShimmerPlaylistSectionAdapter(Context context) {
        this.context = context;
    }

    public ShimmerPlaylistSectionAdapter(Context context, int sectionCount) {
        this.context = context;
        this.sectionCount = sectionCount;
    }

    @NonNull
    @Override
    public ShimmerSectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shimmer_section, parent, false);
        return new ShimmerSectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShimmerSectionViewHolder holder, int position) {
        // Start shimmer animation for section title
        holder.sectionTitleShimmer.startShimmer();

        // Setup recycler view based on section position
        int layoutType;
        int itemCount;

        switch (position) {
            case 0: // First section - horizontal with type 1 (square with radius)
                layoutType = 4;
                itemCount = 4; // Show 4 items horizontally
                holder.artistsRecyclerView.setLayoutManager(new GridLayoutManager(
                        context,
                        2
                ));

                break;
            case 1: // Second section - horizontal with type 2 (round)
                layoutType = 1;
                itemCount = 4; // Show 4 items horizontally
                holder.artistsRecyclerView.setLayoutManager(new LinearLayoutManager(
                        context,
                        LinearLayoutManager.HORIZONTAL,
                        false
                ));
                break;
            case 2: // Third section - vertical with type 4 (card layout)
                layoutType = 2;
                itemCount = 4; // Show 4 items horizontally
                holder.artistsRecyclerView.setLayoutManager(new LinearLayoutManager(
                        context,
                        LinearLayoutManager.HORIZONTAL,
                        false
                ));
                break;
            case 3: // Third section - vertical with type 4 (card layout)
                layoutType = 1;
                itemCount = 4; // Show 4 items horizontally
                holder.artistsRecyclerView.setLayoutManager(new LinearLayoutManager(
                        context,
                        LinearLayoutManager.HORIZONTAL,
                        false
                ));
                break;
            case 4: // Third section - vertical with type 4 (card layout)
                layoutType = 2;
                itemCount = 4; // Show 4 items horizontally
                holder.artistsRecyclerView.setLayoutManager(new LinearLayoutManager(
                        context,
                        LinearLayoutManager.HORIZONTAL,
                        false
                ));
                break;
            case 6: // Third section - vertical with type 4 (card layout)
                layoutType = 1;
                itemCount = 4; // Show 4 items horizontally
                holder.artistsRecyclerView.setLayoutManager(new LinearLayoutManager(
                        context,
                        LinearLayoutManager.HORIZONTAL,
                        false
                ));
                break;

            default:
                layoutType = 2;
                itemCount = 3; // Show 3 items vertically
                holder.artistsRecyclerView.setLayoutManager(new LinearLayoutManager(
                        context,
                        LinearLayoutManager.HORIZONTAL,
                        false
                ));
                break;
        }

        ShimmerArtistAdapter shimmerArtistAdapter = new ShimmerArtistAdapter(context, layoutType, itemCount);
        holder.artistsRecyclerView.setAdapter(shimmerArtistAdapter);

        // Start shimmer for each item in recycler view
        if (shimmerArtistAdapter.getItemCount() > 0) {
            holder.artistsRecyclerView.post(() -> {
                for (int i = 0; i < holder.artistsRecyclerView.getChildCount(); i++) {
                    View child = holder.artistsRecyclerView.getChildAt(i);
                    ShimmerFrameLayout shimmer = child.findViewById(R.id.shimmerLayout);
                    if (shimmer != null) {
                        shimmer.startShimmer();
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return sectionCount;
    }

    public void setSectionCount(int count) {
        this.sectionCount = count;
        notifyDataSetChanged();
    }

    static class ShimmerSectionViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout sectionTitleShimmer;
        RecyclerView artistsRecyclerView;

        public ShimmerSectionViewHolder(@NonNull View itemView) {
            super(itemView);
            sectionTitleShimmer = itemView.findViewById(R.id.sectionTitleShimmer);
            artistsRecyclerView = itemView.findViewById(R.id.artistsRecyclerView);
        }
    }
}