package com.Saalai.SalaiMusicApp.ShimmerAdapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.Saalai.SalaiMusicApp.R;

public class ShimmerParentAdapter extends RecyclerView.Adapter<ShimmerParentAdapter.ViewHolder> {

    private int parentCount;
    private int childCount;

    public ShimmerParentAdapter(int parentCount, int childCount) {
        this.parentCount = parentCount;
        this.childCount = childCount;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_parent_shimmer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                holder.childRecyclerView.getContext(),
                LinearLayoutManager.HORIZONTAL, false
        );
        holder.childRecyclerView.setLayoutManager(layoutManager);
        holder.childRecyclerView.setAdapter(new ShimmerChildAdapter(childCount));

        holder.shimmerLayout.startShimmer();
    }

    @Override
    public int getItemCount() {
        return parentCount;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        RecyclerView childRecyclerView;
        ShimmerFrameLayout shimmerLayout;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            childRecyclerView = itemView.findViewById(R.id.childRecyclerViewShimmer);

            shimmerLayout = itemView.findViewById(R.id.shimmerLayout);
        }
    }
}
