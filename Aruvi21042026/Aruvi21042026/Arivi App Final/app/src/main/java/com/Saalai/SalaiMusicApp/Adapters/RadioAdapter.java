package com.Saalai.SalaiMusicApp.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Models.RadioModel;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.SubscriptionBottomSheetFragment;
import com.squareup.picasso.Picasso;

import java.util.List;

public class RadioAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<RadioModel> radioList;
    private boolean isLoading = false;
    private OnRadioClickListener onRadioClickListener;
    private boolean isAccountBlocked = false;
    private String blockedMessage = "";
    private Context context;

    public RadioAdapter(FragmentActivity activity, List<RadioModel> radioList) {
        this.radioList = radioList;
        this.context = activity;
    }

    // Add this method to set account blocked status
    public void setAccountBlocked(boolean isBlocked, String message) {
        this.isAccountBlocked = isBlocked;
        this.blockedMessage = message;
        notifyDataSetChanged();
    }

    public void setOnRadioClickListener(OnRadioClickListener listener) {
        this.onRadioClickListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_radio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RadioModel model = radioList.get(position);
        ViewHolder vh = (ViewHolder) holder;
        vh.name.setText(model.getChannelName());

        Picasso.get()
                .load(model.getChannelLogo())
                .placeholder(R.drawable.video_placholder)
                .error(R.drawable.video_placholder)
                .into(vh.logo);

        // **Trigger click**
        vh.itemView.setOnClickListener(v -> {
            // Check if account is blocked
            if (isAccountBlocked) {
                AppCompatActivity activity = (AppCompatActivity) v.getContext();
                showAccountBlockedAlert(activity);
                return;
            }

            if (onRadioClickListener != null) {
                onRadioClickListener.onRadioClick(model);
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
        return radioList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView logo;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.txtRadioName);
            logo = itemView.findViewById(R.id.imgRadioLogo);
        }
    }

    // **Click interface**
    public interface OnRadioClickListener {
        void onRadioClick(RadioModel radio);
    }
}