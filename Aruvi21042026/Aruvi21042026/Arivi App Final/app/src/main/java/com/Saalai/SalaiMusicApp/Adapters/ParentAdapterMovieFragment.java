package com.Saalai.SalaiMusicApp.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Fragments.ViewMoreMovieFragment;
import com.Saalai.SalaiMusicApp.Models.ParentItemMovieFragment;
import com.Saalai.SalaiMusicApp.R;

import java.util.List;

public class ParentAdapterMovieFragment extends RecyclerView.Adapter<ParentAdapterMovieFragment.ParentViewHolder> {

    private List<ParentItemMovieFragment> parentItemList;
    private boolean isAccountBlocked = false;
    private String blockedMessage = "";

    public ParentAdapterMovieFragment(List<ParentItemMovieFragment> parentItemList) {
        this.parentItemList = parentItemList;
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_parent_all_fragment, parent, false);
        return new ParentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParentViewHolder holder, int position) {
        ParentItemMovieFragment parentItem = parentItemList.get(position);
        holder.sectionTitle.setText(parentItem.getSectionTitle());

        LinearLayoutManager layoutManager = new LinearLayoutManager(holder.childRecyclerView.getContext(),
                LinearLayoutManager.HORIZONTAL, false);
        holder.childRecyclerView.setLayoutManager(layoutManager);

        // Create child adapter and set account blocked status
        ChildAdapterMovieFragment childAdapter = new ChildAdapterMovieFragment(parentItem.getChildItemList());
        childAdapter.setAccountBlocked(isAccountBlocked, blockedMessage);
        holder.childRecyclerView.setAdapter(childAdapter);

        holder.sellall.setOnClickListener(v -> {
            // Don't check for account blocked for "See All" - let it work normally

            AppCompatActivity activity = (AppCompatActivity) v.getContext();
            ViewMoreMovieFragment fragment = ViewMoreMovieFragment.newInstance(
                    parentItem.getCategoryId(),
                    parentItem.getSectionTitle()
            );

            FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
            // Use add instead of replace
            transaction.add(R.id.fragment_container, fragment);
            transaction.addToBackStack("view_more_movies");
            transaction.commit();
        });
    }

    @Override
    public int getItemCount() {
        return parentItemList.size();
    }

    static class ParentViewHolder extends RecyclerView.ViewHolder {
        TextView sectionTitle, sellall;
        RecyclerView childRecyclerView;

        public ParentViewHolder(@NonNull View itemView) {
            super(itemView);
            sectionTitle = itemView.findViewById(R.id.sectionTitle);
            sellall = itemView.findViewById(R.id.sellall);
            childRecyclerView = itemView.findViewById(R.id.childRecyclerView);
        }
    }
}