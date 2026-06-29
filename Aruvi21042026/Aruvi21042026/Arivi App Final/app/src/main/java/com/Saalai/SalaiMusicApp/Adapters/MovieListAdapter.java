package com.Saalai.SalaiMusicApp.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Fragments.MovieVideoPlayerFragment;
import com.Saalai.SalaiMusicApp.Models.MovieItem;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.SubscriptionBottomSheetFragment;
import com.squareup.picasso.Picasso;

import java.util.List;

public class MovieListAdapter extends RecyclerView.Adapter<MovieListAdapter.MovieViewHolder> {

    private Context context;
    private List<MovieItem> movieList;
    private boolean isAccountBlocked = false;
    private String blockedMessage = "";

    public MovieListAdapter(Context context, List<MovieItem> movieList) {
        this.context = context;
        this.movieList = movieList;
    }

    // Add this method to set account blocked status
    public void setAccountBlocked(boolean isBlocked, String message) {
        this.isAccountBlocked = isBlocked;
        this.blockedMessage = message;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_movie, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        MovieItem movie = movieList.get(position);
        holder.title.setText(movie.getChannelName());
        Log.d("GlideURL", "Loading image URL: " + movie.getChannelLogo());

        Picasso.get()
                .load(movie.getChannelLogo())
                .placeholder(R.drawable.movieplaceholder)
                .error(R.drawable.movieplaceholder)
                .into(holder.thumbnail);

        holder.itemView.setOnClickListener(v -> {
            // Check if account is blocked
            if (isAccountBlocked) {
                AppCompatActivity activity = (AppCompatActivity) v.getContext();
                showAccountBlockedAlert(activity);
                return;
            }

            // Navigate to MovieVideoPlayerFragment instead of Activity
            if (context instanceof AppCompatActivity) {
                AppCompatActivity activity = (AppCompatActivity) context;
                MovieVideoPlayerFragment fragment = MovieVideoPlayerFragment.newInstance(String.valueOf(movie.getChannelId()));

                FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
                transaction.add(R.id.fragment_container, fragment);
                transaction.addToBackStack(null);
                transaction.commit();

                Log.d("MovieListAdapter", "Clicked movieId: " + movie.getChannelId());
            }
        });
    }

    private void showAccountBlockedAlert(AppCompatActivity activity) {
        if (context == null) return;


        SubscriptionBottomSheetFragment bottomSheetFragment = new SubscriptionBottomSheetFragment();
        bottomSheetFragment.show(activity.getSupportFragmentManager(), "MenuBottomSheet");
    }

    @Override
    public int getItemCount() {
        return movieList.size();
    }

    static class MovieViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.movie_thumbnail);
            title = itemView.findViewById(R.id.movie_title);
        }
    }
}