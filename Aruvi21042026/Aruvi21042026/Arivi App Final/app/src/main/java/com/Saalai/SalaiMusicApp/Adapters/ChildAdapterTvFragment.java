package com.Saalai.SalaiMusicApp.Adapters;

import android.content.Context;
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

import com.Saalai.SalaiMusicApp.Fragments.TvShowEpisodeFragment;
import com.Saalai.SalaiMusicApp.Models.TvChannel;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.SubscriptionBottomSheetFragment;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ChildAdapterTvFragment extends RecyclerView.Adapter<ChildAdapterTvFragment.ChildViewHolder> {

    private List<TvChannel> channelList;
    private Context context;
    private boolean isAccountBlocked = false;
    private String blockedMessage = "";

    public ChildAdapterTvFragment(Context context, List<TvChannel> channelList) {
        this.context = context;
        this.channelList = channelList;
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_child_tv, parent, false);
        return new ChildViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {
        TvChannel channel = channelList.get(position);
        holder.title.setText(channel.getChannelName());

        Picasso.get()
                .load(channel.getChannelLogo())
                .placeholder(R.drawable.video_placholder)
                .error(R.drawable.video_placholder)
                .into(holder.thumbnail);

        // Click listener - open TvShowEpisodeFragment
        holder.itemView.setOnClickListener(v -> {
            // Check if account is blocked
            if (isAccountBlocked) {
                AppCompatActivity activity = (AppCompatActivity) v.getContext();
                showAccountBlockedAlert(activity);
                return;
            }

            // Convert int to String
            openTvShowEpisodeFragment(channel.getChannelId());
        });
    }

    private void showAccountBlockedAlert(AppCompatActivity activity) {
        if (context == null) return;

        SubscriptionBottomSheetFragment bottomSheetFragment = new SubscriptionBottomSheetFragment();
        bottomSheetFragment.show(activity.getSupportFragmentManager(), "MenuBottomSheet");
    }

    private void openTvShowEpisodeFragment(int channelId) {
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;

            // Create the TvShowEpisodeFragment with the channel ID
            TvShowEpisodeFragment tvShowEpisodeFragment = TvShowEpisodeFragment.newInstance(String.valueOf(channelId));

            FragmentManager fragmentManager = activity.getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            // Use add instead of replace to preserve the current fragment state
            fragmentTransaction.add(R.id.fragment_container, tvShowEpisodeFragment);
            fragmentTransaction.addToBackStack("tv_show_episodes");
            fragmentTransaction.commit();
        }
    }

    @Override
    public int getItemCount() {
        return channelList.size();
    }

    static class ChildViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title;

        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.posterImage);
            title = itemView.findViewById(R.id.posterTitle);
        }
    }
}