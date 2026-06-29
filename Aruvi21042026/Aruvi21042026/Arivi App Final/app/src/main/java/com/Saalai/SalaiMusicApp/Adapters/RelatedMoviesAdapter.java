package com.Saalai.SalaiMusicApp.Adapters;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Models.MovieRelatedChannel;
import com.Saalai.SalaiMusicApp.R;
import com.squareup.picasso.Picasso;

import java.util.List;
public class RelatedMoviesAdapter extends RecyclerView.Adapter<RelatedMoviesAdapter.ViewHolder> {

    public interface OnMovieClickListener {
        void onMovieClick(String channelId);
    }

    private Context context;
    private List<MovieRelatedChannel> movies;
    private OnMovieClickListener listener;

    public RelatedMoviesAdapter(Context context, List<MovieRelatedChannel> movies, OnMovieClickListener listener) {
        this.context = context;
        this.movies = movies;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_related_movie, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MovieRelatedChannel movie = movies.get(position);

        holder.title.setText(movie.getChannelName());
        // Log the URL before loading
        Log.d("MovieAdapter", "Loading image URL: " + movie.getChannelLogo());

// Load image with Glide
        Picasso.get()
                .load(movie.getChannelLogo())
                .placeholder(R.drawable.movieplaceholder) // optional placeholder
                .error(R.drawable.movieplaceholder)             // optional error image
                .into(holder.poster);



        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMovieClick(movie.getChannelId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView poster;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.posterTitle);
            poster = itemView.findViewById(R.id.posterImage);
        }
    }
}
