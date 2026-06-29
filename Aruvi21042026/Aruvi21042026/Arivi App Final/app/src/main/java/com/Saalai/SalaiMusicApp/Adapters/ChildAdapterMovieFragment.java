package com.Saalai.SalaiMusicApp.Adapters;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Fragments.MovieVideoPlayerFragment;
import com.Saalai.SalaiMusicApp.Fragments.ViewMoreAllFragment;
import com.Saalai.SalaiMusicApp.Models.ChildItemMovieFragment;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.SubscriptionBottomSheetFragment;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ChildAdapterMovieFragment extends RecyclerView.Adapter<ChildAdapterMovieFragment.ChildViewHolder> {

    private List<ChildItemMovieFragment> childItemList;
    private boolean isAccountBlocked = false;
    private String blockedMessage = "";
    private Context context;

    public ChildAdapterMovieFragment(List<ChildItemMovieFragment> childItemList) {
        this.childItemList = childItemList;
    }

    // Add this method to set account blocked status
    public void setAccountBlocked(boolean isBlocked, String message) {
        this.isAccountBlocked = isBlocked;
        this.blockedMessage = message;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_child_all_fragment, parent, false);
        return new ChildViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {
        ChildItemMovieFragment item = childItemList.get(position);
        holder.posterTitle.setText(item.getName());

        Picasso.get()
                .load(item.getImageUrl())
                .placeholder(R.drawable.movieplaceholder)
                .error(R.drawable.movieplaceholder)
                .into(holder.posterImage);

        holder.itemView.setOnClickListener(v -> {
            // Check if account is blocked
            if (isAccountBlocked) {
                AppCompatActivity activity = (AppCompatActivity) v.getContext();
                showAccountBlockedAlert(activity);
                return;
            }

            if (item.getType().equals("more")) {
                // Open "See All" fragment
                openViewMoreFragment(v, item);
            } else if (item.getType().equals("movie")) {
                // Open MovieVideoPlayerFragment instead of Activity
                openMoviePlayerFragment(v, item);
            }
        });
    }

    private void showAccountBlockedAlert(AppCompatActivity activity) {
        if (context == null) return;
        SubscriptionBottomSheetFragment bottomSheetFragment = new SubscriptionBottomSheetFragment();
        bottomSheetFragment.show(activity.getSupportFragmentManager(), "MenuBottomSheet");
    }

    private void openViewMoreFragment(View v, ChildItemMovieFragment item) {
        Bundle bundle = new Bundle();
        bundle.putString("Title", item.getTitle());
        bundle.putString("SeeAllType", item.getType());

        AppCompatActivity activity = (AppCompatActivity) v.getContext();
        ViewMoreAllFragment viewMoreAllFragment = new ViewMoreAllFragment();
        viewMoreAllFragment.setArguments(bundle);

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, viewMoreAllFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void openMoviePlayerFragment(View v, ChildItemMovieFragment item) {
        AppCompatActivity activity = (AppCompatActivity) v.getContext();

        // Create the MovieVideoPlayerFragment with the movie ID
        MovieVideoPlayerFragment moviePlayerFragment = MovieVideoPlayerFragment.newInstance(item.getMovieId());

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Use add instead of replace to preserve the current fragment state
        fragmentTransaction.add(R.id.fragment_container, moviePlayerFragment);
        fragmentTransaction.addToBackStack("movie_player");
        fragmentTransaction.commit();
    }

    @Override
    public int getItemCount() {
        return childItemList.size();
    }

    static class ChildViewHolder extends RecyclerView.ViewHolder {
        ImageView posterImage;
        TextView posterTitle;

        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            posterImage = itemView.findViewById(R.id.posterImage);
            posterTitle = itemView.findViewById(R.id.posterTitle);
        }
    }
}