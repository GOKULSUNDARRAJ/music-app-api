package com.Saalai.SalaiMusicApp.ShimmerAdapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.Saalai.SalaiMusicApp.R;
import com.facebook.shimmer.ShimmerFrameLayout;
import java.util.List;

public class ShimmerArtistAdapter extends RecyclerView.Adapter<ShimmerArtistAdapter.ShimmerViewHolder> {

    private Context context;
    private int layoutType; // 1, 2, 3, or 4
    private int itemCount = 4; // Default number of shimmer items

    public ShimmerArtistAdapter(Context context, int layoutType) {
        this.context = context;
        this.layoutType = layoutType;
    }

    public ShimmerArtistAdapter(Context context, int layoutType, int itemCount) {
        this.context = context;
        this.layoutType = layoutType;
        this.itemCount = itemCount;
    }

    @Override
    public int getItemViewType(int position) {
        return layoutType;
    }

    @NonNull
    @Override
    public ShimmerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        switch (viewType) {
            case 1: // Type 1: Square with radius
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shimmer_type1, parent, false);
                break;
            case 2: // Type 2: Round
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shimmer_type2, parent, false);
                break;
            case 3: // Type 3: Square without radius
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shimmer_type3, parent, false);
                break;
            case 4: // Type 4: Card layout
            default:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shimmer_type4, parent, false);
                break;
        }

        return new ShimmerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShimmerViewHolder holder, int position) {
        // Start shimmer animation
        holder.shimmerFrameLayout.startShimmer();
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int count) {
        this.itemCount = count;
        notifyDataSetChanged();
    }

    public static class ShimmerViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerFrameLayout;

        public ShimmerViewHolder(@NonNull View itemView) {
            super(itemView);
            shimmerFrameLayout = itemView.findViewById(R.id.shimmerLayout);
        }
    }
}