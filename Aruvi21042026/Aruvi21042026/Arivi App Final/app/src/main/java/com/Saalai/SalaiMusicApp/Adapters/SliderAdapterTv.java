package com.Saalai.SalaiMusicApp.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.Saalai.SalaiMusicApp.Models.SliderItemTv;
import com.Saalai.SalaiMusicApp.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class SliderAdapterTv extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(SliderItemTv item);
    }

    private OnItemClickListener listener;
    private static final int TYPE_DATA = 1;
    private static final int TYPE_SHIMMER = 0;

    private List<SliderItemTv> sliderItems;
    private boolean showShimmer = true;

    public SliderAdapterTv(List<SliderItemTv> sliderItems) {
        this.sliderItems = sliderItems;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void hideShimmer() {
        showShimmer = false;
        notifyDataSetChanged();
    }

    public void updateItems(List<SliderItemTv> items) {
        this.sliderItems = items;
        hideShimmer(); // automatically hide shimmer when data is available
    }

    @Override
    public int getItemViewType(int position) {
        return showShimmer ? TYPE_SHIMMER : TYPE_DATA;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SHIMMER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.slider_item_tv_shimmer, parent, false); // create shimmer layout
            return new ShimmerViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.slider_item_tv, parent, false);
            return new SliderViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SliderViewHolder) {
            SliderItemTv item = sliderItems.get(position);

            Picasso.get()
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.video_placholder) // optional placeholder
                    .error(R.drawable.video_placholder)             // optional error image
                    .into(((SliderViewHolder) holder).imageView);

            // Add click listener to the item view
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onItemClick(item);
                    }
                }
            });

        } else if (holder instanceof ShimmerViewHolder) {
            ((ShimmerViewHolder) holder).shimmerLayout.startShimmer();
        }
    }

    @Override
    public int getItemCount() {
        return showShimmer ? 5 : sliderItems.size(); // 5 shimmer placeholders
    }

    static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.sliderImage);
        }
    }

    static class ShimmerViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerLayout;

        public ShimmerViewHolder(@NonNull View itemView) {
            super(itemView);
            shimmerLayout = itemView.findViewById(R.id.shimmerLayout);
        }
    }

    public SliderItemTv getItem(int position) {
        if (sliderItems != null && position >= 0 && position < sliderItems.size()) {
            return sliderItems.get(position);
        }
        return null;
    }
}