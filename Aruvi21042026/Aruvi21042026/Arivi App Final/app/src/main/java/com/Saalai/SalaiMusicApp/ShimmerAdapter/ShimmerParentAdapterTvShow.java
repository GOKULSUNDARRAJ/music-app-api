package com.Saalai.SalaiMusicApp.ShimmerAdapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.R;
import com.facebook.shimmer.ShimmerFrameLayout;

public class ShimmerParentAdapterTvShow extends RecyclerView.Adapter<ShimmerParentAdapterTvShow.ShimmerViewHolder> {

    private int itemCount;


    public ShimmerParentAdapterTvShow(int count, int itemCount) {
        this.itemCount = itemCount;
    }

    @NonNull
    @Override
    public ShimmerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.shimmer_parent_item_tv, parent, false);
        return new ShimmerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShimmerViewHolder holder, int position) {
        holder.shimmerLayout.startShimmer();

        LinearLayoutManager layoutManager = new LinearLayoutManager(
                holder.recyclerView.getContext(),
                LinearLayoutManager.HORIZONTAL, false
        );
        holder.recyclerView.setLayoutManager(layoutManager);
        holder.recyclerView.setAdapter(new ShimmerChildAdapterTvFragment(itemCount));

        holder.shimmerLayout.startShimmer();
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    public static class ShimmerViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerLayout;
        RecyclerView recyclerView;
        public ShimmerViewHolder(@NonNull View itemView) {
            super(itemView);
            shimmerLayout = itemView.findViewById(R.id.shimmerLayoutParentTv);
            recyclerView= itemView.findViewById(R.id.recyclerView);
        }
    }
}
