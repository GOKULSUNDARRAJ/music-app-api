package com.Saalai.SalaiMusicApp.ShimmerAdapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.Saalai.SalaiMusicApp.R;

public class ShimmerAdapterfocatchuptab extends RecyclerView.Adapter<ShimmerAdapterfocatchuptab.ShimmerTabViewHolder> {

    private final int itemCount;

    public ShimmerAdapterfocatchuptab(int itemCount) {
        this.itemCount = itemCount; // Number of shimmer tabs to show
    }

    @NonNull
    @Override
    public ShimmerTabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tab_shimmer, parent, false);
        return new ShimmerTabViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShimmerTabViewHolder holder, int position) {
        holder.shimmerLayout.startShimmer(); // Start shimmer animation
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    static class ShimmerTabViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerLayout;

        public ShimmerTabViewHolder(@NonNull View itemView) {
            super(itemView);
            shimmerLayout = itemView.findViewById(R.id.shimmerLayout);
        }
    }
}
