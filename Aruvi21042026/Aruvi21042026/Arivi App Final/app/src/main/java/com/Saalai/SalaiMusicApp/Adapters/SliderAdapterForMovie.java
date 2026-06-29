package com.Saalai.SalaiMusicApp.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.Saalai.SalaiMusicApp.Models.SliderItemForMovie;
import com.Saalai.SalaiMusicApp.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class SliderAdapterForMovie extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DATA = 1;
    private static final int TYPE_SHIMMER = 0;

    private Context context;
    private List<SliderItemForMovie> sliderItems;
    private boolean showShimmer = true;

    // Add click listener interface
    public interface OnSliderClickListener {
        void onSliderItemClick(SliderItemForMovie sliderItem, int position);
    }

    private OnSliderClickListener sliderClickListener;

    public SliderAdapterForMovie(Context context, List<SliderItemForMovie> sliderItems) {
        this.context = context;
        this.sliderItems = sliderItems;
    }

    // Method to set click listener
    public void setOnSliderClickListener(OnSliderClickListener listener) {
        this.sliderClickListener = listener;
    }

    public void hideShimmer() {
        showShimmer = false;
        notifyDataSetChanged();
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
                    .inflate(R.layout.slider_item_movie_shimmer, parent, false);
            return new ShimmerViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.slider_item_movie, parent, false);
            return new SliderViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SliderViewHolder) {
            SliderItemForMovie item = sliderItems.get(position);

            Picasso.get()
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.video_placholder) // optional
                    .error(R.drawable.video_placholder)             // optional
                    .into(((SliderViewHolder) holder).imageView);

            ((SliderViewHolder) holder).name.setText(item.getTitle());
            ((SliderViewHolder) holder).subname.setText(item.getDescription());

            // Add click listener to the item view
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (sliderClickListener != null) {
                        sliderClickListener.onSliderItemClick(item, position);
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
        TextView name, subname;

        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.sliderImage);
            name = itemView.findViewById(R.id.name);
            subname = itemView.findViewById(R.id.subname);
        }
    }

    static class ShimmerViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerLayout;

        public ShimmerViewHolder(@NonNull View itemView) {
            super(itemView);
            shimmerLayout = itemView.findViewById(R.id.shimmerLayout);
        }
    }
}