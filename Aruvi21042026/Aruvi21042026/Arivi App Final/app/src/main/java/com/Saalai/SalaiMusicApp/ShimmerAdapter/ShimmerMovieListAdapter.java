package com.Saalai.SalaiMusicApp.ShimmerAdapter;



import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.Saalai.SalaiMusicApp.R;

public class ShimmerMovieListAdapter extends RecyclerView.Adapter<ShimmerMovieListAdapter.ShimmerViewHolder> {

    private final int itemCount;

    public ShimmerMovieListAdapter(int itemCount) {
        this.itemCount = itemCount;
    }

    @NonNull
    @Override
    public ShimmerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_movie_list_shimmer, parent, false);
        return new ShimmerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShimmerViewHolder holder, int position) {
        holder.shimmerFrameLayout.startShimmer();
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    static class ShimmerViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerFrameLayout;
        public ShimmerViewHolder(@NonNull View itemView) {
            super(itemView);
            shimmerFrameLayout = itemView.findViewById(R.id.shimmer_layout);
        }
    }
}
