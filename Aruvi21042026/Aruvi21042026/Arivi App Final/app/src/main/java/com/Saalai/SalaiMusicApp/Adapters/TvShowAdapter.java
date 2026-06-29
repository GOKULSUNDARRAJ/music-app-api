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

import com.Saalai.SalaiMusicApp.Fragments.TvShowEpisodeFragment;
import com.Saalai.SalaiMusicApp.Models.TvShow;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.SubscriptionBottomSheetFragment;
import com.squareup.picasso.Picasso;

import java.util.List;

public class TvShowAdapter extends RecyclerView.Adapter<TvShowAdapter.TvShowViewHolder> {

    private Context context;
    private List<TvShow> tvShowList;
    private boolean isAccountBlocked = false;
    private String blockedMessage = "";

    public TvShowAdapter(Context context, List<TvShow> tvShowList) {
        this.context = context;
        this.tvShowList = tvShowList;
    }

    // Add this method to set account blocked status
    public void setAccountBlocked(boolean isBlocked, String message) {
        this.isAccountBlocked = isBlocked;
        this.blockedMessage = message;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TvShowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tvshow, parent, false);
        return new TvShowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TvShowViewHolder holder, int position) {
        TvShow show = tvShowList.get(position);

        holder.name.setText(show.getChannelName());

        Picasso.get()
                .load(show.getChannelLogo())
                .placeholder(R.drawable.video_placholder)
                .error(R.drawable.video_placholder)
                .into(holder.logo);

        holder.itemView.setOnClickListener(v -> {
            // Check if account is blocked
            if (isAccountBlocked) {
                AppCompatActivity activity = (AppCompatActivity) v.getContext();
                showAccountBlockedAlert(activity);
                return;
            }

            // Navigate to TvShowEpisodeFragment instead of Activity
            if (context instanceof AppCompatActivity) {
                AppCompatActivity activity = (AppCompatActivity) context;
                TvShowEpisodeFragment fragment = TvShowEpisodeFragment.newInstance(String.valueOf(show.getChannelId()));

                FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
                transaction.add(R.id.fragment_container, fragment);
                transaction.addToBackStack(null);
                transaction.commit();

                Log.d("TvShowAdapter", "Clicked tvShowId: " + show.getChannelId());
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
        return tvShowList.size();
    }

    public class TvShowViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView logo;

        public TvShowViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvshow_title);
            logo = itemView.findViewById(R.id.tvshow_thumbnail);
        }
    }
}