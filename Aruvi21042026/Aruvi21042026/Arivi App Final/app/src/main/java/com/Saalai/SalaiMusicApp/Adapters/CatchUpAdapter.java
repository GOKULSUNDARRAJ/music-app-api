package com.Saalai.SalaiMusicApp.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Models.CatchUp;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.SubscriptionBottomSheetFragment;
import com.squareup.picasso.Picasso;

import java.util.List;

public class CatchUpAdapter extends RecyclerView.Adapter<CatchUpAdapter.ViewHolder> {

    private Context context;
    private List<CatchUp> channelList;
    private OnItemClickListener onItemClickListener;
    private int selectedPosition = 0;
    private boolean isAccountBlocked = false;
    private String blockedMessage = "";

    public CatchUpAdapter(Context context, List<CatchUp> channelList, OnItemClickListener listener) {
        this.context = context;
        this.channelList = channelList;
        this.onItemClickListener = listener;
    }

    // Add this method to set account blocked status
    public void setAccountBlocked(boolean isBlocked, String message) {
        this.isAccountBlocked = isBlocked;
        this.blockedMessage = message;
        notifyDataSetChanged();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_catchup, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CatchUp channel = channelList.get(position);
        holder.channelName.setText(channel.getChannelName());

        Picasso.get()
                .load(channel.getChannelLogo())
                .placeholder(R.drawable.video_placholder)
                .error(R.drawable.video_placholder)
                .into(holder.channelImage);

        holder.itemView.setSelected(selectedPosition == position);

        holder.itemView.setOnClickListener(v -> {
            // Check if account is blocked
            if (isAccountBlocked) {
                AppCompatActivity activity = (AppCompatActivity) v.getContext();
                showAccountBlockedAlert(activity);
                return;
            }

            selectedPosition = position;
            notifyDataSetChanged();

            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(channel);
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
        return channelList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView channelName;
        ImageView channelImage;
        ConstraintLayout itemContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            channelName = itemView.findViewById(R.id.channel_name);
            channelImage = itemView.findViewById(R.id.channel_image);
            itemContainer = itemView.findViewById(R.id.itemContainer);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(CatchUp channel);
    }
}