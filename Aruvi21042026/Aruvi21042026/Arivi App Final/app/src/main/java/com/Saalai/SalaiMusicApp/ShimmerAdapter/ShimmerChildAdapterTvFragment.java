package com.Saalai.SalaiMusicApp.ShimmerAdapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.Saalai.SalaiMusicApp.R;

public class ShimmerChildAdapterTvFragment extends RecyclerView.Adapter<ShimmerChildAdapterTvFragment.ShimmerViewHolder> {

    private int itemCount;

    public ShimmerChildAdapterTvFragment(int itemCount) {
        this.itemCount = itemCount;
    }

    @NonNull
    @Override
    public ShimmerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shimmer_child_tv, parent, false);
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
            shimmerLayout = itemView.findViewById(R.id.shimmerLayout);
        }
    }
}
