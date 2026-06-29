package com.Saalai.SalaiMusicApp.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Models.TopNavItem;
import com.Saalai.SalaiMusicApp.R;
import com.bumptech.glide.Glide;

import java.util.List;

public class TopTabAdapterMusic extends RecyclerView.Adapter<TopTabAdapterMusic.ViewHolder> {

    private List<TopNavItem> topNavItems;
    private OnTabClickListener listener;
    private int selectedPosition = 0;
    private Context context;

    public interface OnTabClickListener {
        void onTabClick(int position);
    }

    // FIXED CONSTRUCTOR - Use the reference directly
    public TopTabAdapterMusic(Context context, List<TopNavItem> topNavItems, OnTabClickListener listener) {
        this.context = context;
        this.topNavItems = topNavItems;  // Use the reference
        this.listener = listener;

        Log.d("TopTabAdapter", "Adapter created with " +
                (topNavItems != null ? topNavItems.size() : 0) + " items");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d("TopTabAdapter", "onCreateViewHolder called");
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_tab_music, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (topNavItems == null || position < 0 || position >= topNavItems.size()) {
            Log.e("TopTabAdapter", "Invalid position in onBindViewHolder: " + position);
            return;
        }

        TopNavItem item = topNavItems.get(position);
        Log.d("TopTabAdapter", "Binding position " + position + ": " + item.getTopmenuName());

        // Set tab text
        holder.tabText.setText(item.getTopmenuName());

        // Make sure views are visible

        holder.tabText.setVisibility(View.VISIBLE);

        // Set icon and background based on selection
        if (selectedPosition == position) {
            Log.d("TopTabAdapter", "Position " + position + " is SELECTED");

            // Selected tab - Set selected background
            holder.tabText.setBackgroundResource(R.drawable.tab_background_selected);
            holder.tabText.setTextColor(ContextCompat.getColor(context, R.color.yellow1));

            if (item.getTopmenuActiveIcon() != null && !item.getTopmenuActiveIcon().isEmpty()) {
                try {

                } catch (Exception e) {
                    Log.e("TopTabAdapter", "Error loading active icon: " + e.getMessage());
                }
            } else {
                // Handle case when no active icon is available

            }




        } else {
            // Unselected tab - Set unselected background
            holder.tabText.setBackgroundResource(R.drawable.tab_background_unselected);
            holder.tabText.setTextColor(ContextCompat.getColor(context, R.color.gray));


            if (item.getTopmenuInActiveIcon() != null && !item.getTopmenuInActiveIcon().isEmpty()) {
                try {

                } catch (Exception e) {
                    Log.e("TopTabAdapter", "Error loading inactive icon: " + e.getMessage());
                }
            } else {
                // Handle case when no inactive icon is available

            }


        }

        // Set click listener
        holder.tabContainer.setOnClickListener(v -> {
            Log.d("TopTabAdapter", "Tab clicked at position: " + position);
            if (listener != null) {
                setSelectedPosition(position);
                listener.onTabClick(position);
            }
        });
    }



    @Override
    public int getItemCount() {
        int count = (topNavItems != null) ? topNavItems.size() : 0;
        Log.d("TopTabAdapter", "getItemCount: " + count);
        return count;
    }

    public void setSelectedPosition(int position) {
        if (topNavItems == null || position < 0 || position >= topNavItems.size()) {
            Log.e("TopTabAdapter", "Invalid position to select: " + position);
            return;
        }

        Log.d("TopTabAdapter", "Setting selected position to: " + position);
        int previousPosition = selectedPosition;
        selectedPosition = position;

        if (previousPosition >= 0 && previousPosition < getItemCount()) {
            notifyItemChanged(previousPosition);
        }
        notifyItemChanged(selectedPosition);
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    // SIMPLIFIED updateData method
    public void notifyDataChanged() {
        Log.d("TopTabAdapter", "notifyDataChanged called");
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout tabContainer;
        TextView tabText;


        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tabContainer = itemView.findViewById(R.id.tab_container);
            tabText = itemView.findViewById(R.id.tab_text);


            Log.d("TopTabAdapter", "ViewHolder created - all views found");
        }
    }
}