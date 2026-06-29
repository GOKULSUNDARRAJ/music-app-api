package com.Saalai.SalaiMusicApp.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Models.RadioModel;
import com.Saalai.SalaiMusicApp.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class RadioAdapterDetail extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<RadioModel> radioList;
    private boolean isLoading = false;
    private OnRadioClickListener onRadioClickListener;

    public RadioAdapterDetail(FragmentActivity activity, List<RadioModel> radioList) {
        this.radioList = radioList;
    }

    public void setOnRadioClickListener(OnRadioClickListener listener) {
        this.onRadioClickListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_radio_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RadioModel model = radioList.get(position);
        ViewHolder vh = (ViewHolder) holder;
        vh.name.setText(model.getChannelName());

        Picasso.get()
                .load(model.getChannelLogo())
                .placeholder(R.drawable.video_placholder) // optional placeholder while loading
                .error(R.drawable.video_placholder)             // optional error image if loading fails
                .into(vh.logo);


        // **Trigger click**
        vh.itemView.setOnClickListener(v -> {
            if (onRadioClickListener != null) {
                onRadioClickListener.onRadioClick(model);
            }
        });
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
