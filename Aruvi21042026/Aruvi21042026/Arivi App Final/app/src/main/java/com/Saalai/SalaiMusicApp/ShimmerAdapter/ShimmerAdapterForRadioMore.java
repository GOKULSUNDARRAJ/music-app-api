package com.Saalai.SalaiMusicApp.ShimmerAdapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.Saalai.SalaiMusicApp.R;

public class ShimmerAdapterForRadioMore extends RecyclerView.Adapter<ShimmerAdapterForRadioMore.ShimmerViewHolder> {

    private int itemCount;

    public ShimmerAdapterForRadioMore(int itemCount) {
        this.itemCount = itemCount; // Number of shimmer placeholders
    }

    @NonNull
    @Override
    public ShimmerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_radio_shimmer_more, parent, false);
        return new ShimmerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShimmerViewHolder holder, int position) {
        holder.shimmerLayout.startShimmer();
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    public static class ShimmerViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerLayout;
        public ShimmerViewHolder(@NonNull View itemView) {
            super(itemView);
            shimmerLayout = itemView.findViewById(R.id.shimmerLayout);
        }
    }
}
