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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.Saalai.SalaiMusicApp.Models.TopNavItem;
import com.Saalai.SalaiMusicApp.R;

import java.util.List;

public class TopTabAdapter extends RecyclerView.Adapter<TopTabAdapter.ViewHolder> {

    private List<TopNavItem> topNavItems;
    private OnTabClickListener listener;
    private int selectedPosition = 0;
    private Context context;

    public interface OnTabClickListener {
        void onTabClick(int position);
    }

    // FIXED CONSTRUCTOR - Use the reference directly
    public TopTabAdapter(Context context, List<TopNavItem> topNavItems, OnTabClickListener listener) {
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
                .inflate(R.layout.item_top_tab, parent, false);
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
        holder.tabIcon.setVisibility(View.VISIBLE);
        holder.tabText.setVisibility(View.VISIBLE);

        // Set icon based on selection
        if (selectedPosition == position) {
            Log.d("TopTabAdapter", "Position " + position + " is SELECTED");

            // Selected tab
            if (item.getTopmenuActiveIcon() != null && !item.getTopmenuActiveIcon().isEmpty()) {
                try {
                    Glide.with(context)
                            .load(item.getTopmenuActiveIcon())
                            .into(holder.tabIcon);
                } catch (Exception e) {

                }
            } else {

            }

            holder.tabText.setTextColor(context.getResources().getColor(R.color.white));
            holder.tabIndicator.setVisibility(View.VISIBLE);

        } else {
            // Unselected tab
            if (item.getTopmenuInActiveIcon() != null && !item.getTopmenuInActiveIcon().isEmpty()) {
                try {
                    Glide.with(context)
                            .load(item.getTopmenuInActiveIcon())
                            .placeholder(R.drawable.home_unselected)
                            .into(holder.tabIcon);
                } catch (Exception e) {

                }
            } else {

            }

            holder.tabText.setTextColor(context.getResources().getColor(R.color.gray));
            holder.tabIndicator.setVisibility(View.INVISIBLE);
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
        ImageView tabIcon;
        TextView tabText;
        View tabIndicator;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tabContainer = itemView.findViewById(R.id.tab_container);
            tabIcon = itemView.findViewById(R.id.tab_icon);
            tabText = itemView.findViewById(R.id.tab_text);
            tabIndicator = itemView.findViewById(R.id.tab_indicator);

            Log.d("TopTabAdapter", "ViewHolder created - all views found");
        }
    }
}