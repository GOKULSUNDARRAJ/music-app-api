package com.Saalai.SalaiMusicApp.Adapters;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Models.SliderItem;
import com.Saalai.SalaiMusicApp.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class SliderAdapter extends RecyclerView.Adapter<SliderAdapter.SliderViewHolder> {

    private Context context;
    private List<SliderItem> sliderItems;

    public SliderAdapter(Context context, List<SliderItem> sliderItems) {
        this.context = context;
        this.sliderItems = sliderItems;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.slider_item, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        SliderItem item = sliderItems.get(position);

        // Load image with Glide (you'll need to add this dependency)
        Picasso.get()
                .load(item.getImageUrl())
                .placeholder(R.drawable.video_placholder) // optional placeholder
                .error(R.drawable.video_placholder)             // optional error image
                .into(holder.imageView);


        holder.titleTextView.setText(item.getTitle());
    }

    @Override
    public int getItemCount() {
        if (sliderItems == null) {
            return 0;  // prevents NullPointerException
        }
        return sliderItems.size();
    }



    static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleTextView;

        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.sliderImage);
            titleTextView = itemView.findViewById(R.id.sliderTitle);
        }
    }
}