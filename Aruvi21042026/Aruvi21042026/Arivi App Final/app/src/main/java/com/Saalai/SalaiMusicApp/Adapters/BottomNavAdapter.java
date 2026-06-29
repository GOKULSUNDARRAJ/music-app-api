package com.Saalai.SalaiMusicApp.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.Saalai.SalaiMusicApp.Models.BottomNavItem;
import com.Saalai.SalaiMusicApp.R;
import java.util.List;

public class BottomNavAdapter extends RecyclerView.Adapter<BottomNavAdapter.ViewHolder> {

    private List<BottomNavItem> navItems;
    private OnItemClickListener listener;
    private int selectedPosition = 0;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public BottomNavAdapter(List<BottomNavItem> navItems, OnItemClickListener listener) {
        this.navItems = navItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bottom_nav, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BottomNavItem item = navItems.get(position);

        // Set name
        holder.title.setText(item.getBottommenuName());

        // Load icon based on selection
        if (selectedPosition == position) {
            // Load active icon
            if (item.getBottommenuActiveIcon() != null && !item.getBottommenuActiveIcon().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(item.getBottommenuActiveIcon())
                        .into(holder.icon);
            }
            holder.title.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.yellow));
        } else {
            // Load inactive icon
            if (item.getBottommenuInActiveIcon() != null && !item.getBottommenuInActiveIcon().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(item.getBottommenuInActiveIcon())
                        .into(holder.icon);
            }
            holder.title.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.gray));
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && item.isActive()) {
                setSelectedPosition(position);
                listener.onItemClick(position);
            }
        });

        // Disable if not active
        holder.itemView.setAlpha(item.isActive() ? 1.0f : 0.5f);
        holder.itemView.setEnabled(item.isActive());
    }

    @Override
    public int getItemCount() {
        return navItems.size();
    }

    public void setSelectedPosition(int position) {
        int previousPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(previousPosition);
        notifyItemChanged(selectedPosition);
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.nav_icon);
            title = itemView.findViewById(R.id.nav_title);
        }
    }
}