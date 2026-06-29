package com.Saalai.SalaiMusicApp.Adapters;

import android.content.Context;
import android.os.Bundle;
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

import com.Saalai.SalaiMusicApp.Fragments.CatchUpDetailFragment;
import com.Saalai.SalaiMusicApp.Fragments.MovieVideoPlayerFragment;
import com.Saalai.SalaiMusicApp.Fragments.RadioPlayerFragment;
import com.Saalai.SalaiMusicApp.Fragments.TvShowEpisodeFragment;
import com.Saalai.SalaiMusicApp.Fragments.VideoPlayerFragment;
import com.Saalai.SalaiMusicApp.Fragments.ViewMoreAllFragment;
import com.Saalai.SalaiMusicApp.Models.ChildItemAllFragment;
import com.Saalai.SalaiMusicApp.Models.RadioModel;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.SubscriptionBottomSheetFragment;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ChildAdapterAllFragment extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_CHANNEL = 1;
    private static final int TYPE_DEFAULT = 2;

    private List<ChildItemAllFragment> childItemList;
    private boolean isAccountBlocked = false;
    private String blockedMessage = "";
    private Context context;

    public ChildAdapterAllFragment(List<ChildItemAllFragment> childItemList) {
        this.childItemList = childItemList;
    }

    // Add this method to set account blocked status
    public void setAccountBlocked(boolean isBlocked, String message) {
        this.isAccountBlocked = isBlocked;
        this.blockedMessage = message;
        notifyDataSetChanged(); // Refresh the adapter to show/hide click functionality
    }

    @Override
    public int getItemViewType(int position) {
        ChildItemAllFragment item = childItemList.get(position);
        String type = item.getType();

        if ("Channels".equalsIgnoreCase(type) ||
                "Radio".equalsIgnoreCase(type) ||
                "TVShows".equalsIgnoreCase(type)) {
            return TYPE_CHANNEL;
        } else {
            return TYPE_DEFAULT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();

        if (viewType == TYPE_CHANNEL) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_channel_design, parent, false);
            return new ChannelViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_child_all_fragment, parent, false);
            return new ChildViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChildItemAllFragment item = childItemList.get(position);

        if (holder instanceof ChannelViewHolder) {
            ChannelViewHolder channelHolder = (ChannelViewHolder) holder;
            channelHolder.posterTitle.setText(item.getName());
            Picasso.get()
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.video_placholder)
                    .error(R.drawable.video_placholder)
                    .into(channelHolder.posterImage);

            channelHolder.itemView.setOnClickListener(v -> {
                handleItemClick(item, v, TYPE_CHANNEL);
            });

        } else if (holder instanceof ChildViewHolder) {
            ChildViewHolder defaultHolder = (ChildViewHolder) holder;
            defaultHolder.posterTitle.setText(item.getName());
            Picasso.get()
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.movieplaceholder)
                    .error(R.drawable.movieplaceholder)
                    .into(defaultHolder.posterImage);

            defaultHolder.itemView.setOnClickListener(v -> {
                handleItemClick(item, v, TYPE_DEFAULT);
            });
        }
    }

    private void handleItemClick(ChildItemAllFragment item, View v, int viewType) {
        // Check if account is blocked
        if (isAccountBlocked) {
            AppCompatActivity activity = (AppCompatActivity) v.getContext();
            showAccountBlockedAlert(activity);
            return;
        }

        // If not blocked, proceed with normal navigation
        AppCompatActivity activity = (AppCompatActivity) v.getContext();

        if (viewType == TYPE_CHANNEL) {
            switch (item.getType()) {
                case "Channels":
                    navigateToVideoPlayer(item, activity);
                    break;

                case "Radio":
                    RadioModel radioModel = new RadioModel(
                            item.getChannelId(),
                            item.getName(),
                            item.getImageUrl(),
                            item.getUrl()
                    );
                    RadioPlayerFragment radioPlayerFragment = RadioPlayerFragment.newInstance(radioModel);
                    activity.getSupportFragmentManager().beginTransaction()
                            .add(R.id.fragment_container, radioPlayerFragment)
                            .addToBackStack("radio_player")
                            .commit();
                    break;

                case "TVShows":
                    TvShowEpisodeFragment tvShowFragment = TvShowEpisodeFragment.newInstance(String.valueOf(item.getChannelId()));
                    FragmentTransaction tvTransaction = activity.getSupportFragmentManager().beginTransaction();
                    tvTransaction.add(R.id.fragment_container, tvShowFragment);
                    tvTransaction.addToBackStack("tv_show_fragment");
                    tvTransaction.commit();
                    break;
            }
        } else if (viewType == TYPE_DEFAULT) {
            switch (item.getType()) {
                case "more":
                    ViewMoreAllFragment fragment = new ViewMoreAllFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("Title", item.getTitle());
                    bundle.putString("SeeAllType", item.getType());
                    fragment.setArguments(bundle);
                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            .add(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                    break;

                case "Movies":
                    MovieVideoPlayerFragment movieFragment = MovieVideoPlayerFragment.newInstance(String.valueOf(item.getChannelId()));
                    FragmentTransaction movieTransaction = activity.getSupportFragmentManager().beginTransaction();
                    movieTransaction.add(R.id.fragment_container, movieFragment);
                    movieTransaction.addToBackStack("movie_player_fragment");
                    movieTransaction.commit();
                    break;

                case "CatchUp":
                    CatchUpDetailFragment catchUpFragment = CatchUpDetailFragment.newInstance(String.valueOf(item.getChannelId()));
                    FragmentTransaction catchUpTransaction = activity.getSupportFragmentManager().beginTransaction();
                    catchUpTransaction.add(R.id.fragment_container, catchUpFragment);
                    catchUpTransaction.addToBackStack("catch_up_fragment");
                    catchUpTransaction.commit();
                    break;
            }
        }
    }

    private void showAccountBlockedAlert(AppCompatActivity activity) {
        if (context == null) return;

        SubscriptionBottomSheetFragment bottomSheetFragment = new SubscriptionBottomSheetFragment();
        bottomSheetFragment.show(activity.getSupportFragmentManager(), "MenuBottomSheet");
    }

    private void navigateToVideoPlayer(ChildItemAllFragment channel, AppCompatActivity activity) {
        Log.d("ChildAdapter", "Navigating to video player: " + channel.getName());

        VideoPlayerFragment videoPlayerFragment = VideoPlayerFragment.newInstance(
                channel.getUrl(),
                channel.getName()
        );

        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, videoPlayerFragment);
        transaction.addToBackStack("video_player");
        transaction.commit();
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

    static class ChannelViewHolder extends RecyclerView.ViewHolder {
        ImageView posterImage;
        TextView posterTitle;

        public ChannelViewHolder(@NonNull View itemView) {
            super(itemView);
            posterImage = itemView.findViewById(R.id.posterImage);
            posterTitle = itemView.findViewById(R.id.posterTitle);
        }
    }
}