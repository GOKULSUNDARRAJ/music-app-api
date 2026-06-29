package com.Saalai.SalaiMusicApp.ShimmerAdapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.R;

public class ShimmerAdapterforlivemore extends RecyclerView.Adapter<ShimmerAdapterforlivemore.ShimmerViewHolder> {

    private Context context;
    private int shimmerItemCount;

    public ShimmerAdapterforlivemore(Context context, int shimmerItemCount) {
        this.context = context;
        this.shimmerItemCount = shimmerItemCount;
    }

    @NonNull
    @Override
    public ShimmerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_channel_shimmer, parent, false);
        return new ShimmerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShimmerViewHolder holder, int position) {
        // No binding needed for shimmer
    }

    @Override
    public int getItemCount() {
        return shimmerItemCount; // show X shimmer items
    }

    public static class ShimmerViewHolder extends RecyclerView.ViewHolder {
        public ShimmerViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
