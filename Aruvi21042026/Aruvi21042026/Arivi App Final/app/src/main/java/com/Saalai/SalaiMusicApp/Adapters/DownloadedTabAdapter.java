package com.Saalai.SalaiMusicApp.Adapters;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.R;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class DownloadedTabAdapter extends RecyclerView.Adapter<DownloadedTabAdapter.TabViewHolder> {

    private Context context;
    private List<String> tabTitles;
    private int selectedPosition = 0;
    private OnTabSelectedListener tabSelectedListener;

    public interface OnTabSelectedListener {
        void onTabSelected(int position);
    }

    public DownloadedTabAdapter(Context context) {
        this.context = context;
        this.tabTitles = new ArrayList<>();
        this.tabTitles.add("Songs");
        this.tabTitles.add("Playlists");
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        this.tabSelectedListener = listener;
    }

    @NonNull
    @Override
    public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.custom_tab, parent, false);
        return new TabViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
        String tabTitle = tabTitles.get(position);
        holder.tabText.setText(tabTitle);

        // Set background and text color based on selection
        if (position == selectedPosition) {
            holder.tabText.setBackgroundResource(R.drawable.tab_background_selected);
            holder.tabText.setTextColor(context.getResources().getColor(R.color.yellow1));
        } else {
            holder.tabText.setBackgroundResource(R.drawable.tab_background_unselected);
            holder.tabText.setTextColor(context.getResources().getColor(R.color.white));
        }

        // Handle click
        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = position;

            // Notify item changes
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);

            // Notify listener
            if (tabSelectedListener != null) {
                tabSelectedListener.onTabSelected(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tabTitles.size();
    }

    static class TabViewHolder extends RecyclerView.ViewHolder {
        TextView tabText;

        TabViewHolder(@NonNull View itemView) {
            super(itemView);
            tabText = itemView.findViewById(R.id.tab_text);
        }
    }
}