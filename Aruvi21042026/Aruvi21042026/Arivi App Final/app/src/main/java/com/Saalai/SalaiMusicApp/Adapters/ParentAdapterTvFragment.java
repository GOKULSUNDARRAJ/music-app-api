package com.Saalai.SalaiMusicApp.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Fragments.ViewMoreTvShowFragment;
import com.Saalai.SalaiMusicApp.Models.TvCategory;
import com.Saalai.SalaiMusicApp.R;

import java.util.List;

public class ParentAdapterTvFragment extends RecyclerView.Adapter<ParentAdapterTvFragment.ParentViewHolder> {

    private List<TvCategory> categoryList;
    private Context context;
    private boolean isAccountBlocked = false;
    private String blockedMessage = "";

    public ParentAdapterTvFragment(List<TvCategory> categoryList, Context context) {
        this.categoryList = categoryList;
        this.context = context;
    }

    // Add this method to set account blocked status
    public void setAccountBlocked(boolean isBlocked, String message) {
        this.isAccountBlocked = isBlocked;
        this.blockedMessage = message;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ParentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.parent_item_tv, parent, false);
        return new ParentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParentViewHolder holder, int position) {
        TvCategory category = categoryList.get(position);
        holder.categoryTitle.setText(category.getCategoryName());

        List<?> channels = category.getChannels();
        Log.d("ParentAdapterTvFragment", "Binding category: " + category.getCategoryName() +
                ", channels count: " + (channels != null ? channels.size() : 0));

        ChildAdapterTvFragment childAdapter = new ChildAdapterTvFragment(context, category.getChannels());

        // Pass account blocked status to child adapter
        childAdapter.setAccountBlocked(isAccountBlocked, blockedMessage);

        holder.childRecyclerView.setLayoutManager(
                new LinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        holder.childRecyclerView.setHasFixedSize(true);
        holder.childRecyclerView.setAdapter(childAdapter);

        holder.sellall.setOnClickListener(v -> {
            // Don't check for account blocked for "See All" - let it work normally

            AppCompatActivity activity = (AppCompatActivity) v.getContext();

            // Use fragment instead of activity
            ViewMoreTvShowFragment fragment = ViewMoreTvShowFragment.newInstance(
                    category.getCategoryId(),
                    category.getCategoryName()
            );

            FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.fragment_container, fragment);
            transaction.addToBackStack("view_more_tv_shows");
            transaction.commit();
        });
    }

    @Override
    public int getItemCount() {
        Log.d("ParentAdapterTvFragment", "Total parent items: " + categoryList.size());
        return categoryList.size();
    }

    static class ParentViewHolder extends RecyclerView.ViewHolder {
        TextView categoryTitle, sellall;
        RecyclerView childRecyclerView;

        public ParentViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTitle = itemView.findViewById(R.id.sectionTitle);
            childRecyclerView = itemView.findViewById(R.id.childRecyclerView);
            sellall = itemView.findViewById(R.id.sellall);
        }
    }
}