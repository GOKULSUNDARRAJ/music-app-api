package com.Saalai.SalaiMusicApp.ShimmerAdapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.Saalai.SalaiMusicApp.R;

public class RelatedMoviesShimmerAdapter extends RecyclerView.Adapter<RelatedMoviesShimmerAdapter.ShimmerViewHolder> {

    private Context context;
    private int itemCount;

    public RelatedMoviesShimmerAdapter(Context context, int itemCount) {
        this.context = context;
        this.itemCount = itemCount; // Number of shimmer placeholders
    }

    @NonNull
    @Override
    public ShimmerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_related_movieshimmer, parent, false);
        return new ShimmerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShimmerViewHolder holder, int position) {
        // Start shimmer animation
        holder.shimmerLayout.startShimmer();
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    static class ShimmerViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerLayout;

        public ShimmerViewHolder(@NonNull View itemView) {
            super(itemView);
            shimmerLayout = itemView.findViewById(R.id.shimmer_layout);
        }
    }
}
