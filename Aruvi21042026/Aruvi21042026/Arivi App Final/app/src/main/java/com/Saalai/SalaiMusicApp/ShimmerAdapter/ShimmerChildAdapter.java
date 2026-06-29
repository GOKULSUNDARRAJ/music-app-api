package com.Saalai.SalaiMusicApp.ShimmerAdapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.Saalai.SalaiMusicApp.R;

public class ShimmerChildAdapter extends RecyclerView.Adapter<ShimmerChildAdapter.ViewHolder> {

    private int itemCount;

    public ShimmerChildAdapter(int itemCount) {
        this.itemCount = itemCount;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_child_shimmer, parent, false);


        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        holder.shimmerLayout.startShimmer();
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            shimmerLayout = itemView.findViewById(R.id.shimmerLayout);

        }



    }
}
