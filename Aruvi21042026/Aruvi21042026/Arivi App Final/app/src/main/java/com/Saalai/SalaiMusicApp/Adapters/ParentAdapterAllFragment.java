package com.Saalai.SalaiMusicApp.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Activity.MainActivity;
import com.Saalai.SalaiMusicApp.ClickInterface.TabNavigationListener;
import com.Saalai.SalaiMusicApp.Fragments.MoreLastesMovieFragment;
import com.Saalai.SalaiMusicApp.Fragments.MoreLatestTvShowListFragment;
import com.Saalai.SalaiMusicApp.Fragments.RadioFragment;
import com.Saalai.SalaiMusicApp.Models.ParentItemAllFragment;
import com.Saalai.SalaiMusicApp.R;

import java.util.List;

public class ParentAdapterAllFragment extends RecyclerView.Adapter<ParentAdapterAllFragment.ParentViewHolder> {

    private List<ParentItemAllFragment> parentItemList;
    private TabNavigationListener tabNavigationListener;
    private AppCompatActivity activity;
    private boolean isAccountBlocked = false; // Keep this for passing to child
    private String blockedMessage = ""; // Keep this for passing to child

    public ParentAdapterAllFragment(List<ParentItemAllFragment> parentItemList, TabNavigationListener listener, AppCompatActivity activity) {
        this.parentItemList = parentItemList;
        this.tabNavigationListener = listener;
        this.activity = activity;
    }

    // Add this method to update account blocked status
    public void setAccountBlocked(boolean isBlocked, String message) {
        this.isAccountBlocked = isBlocked;
        this.blockedMessage = message;
        notifyDataSetChanged(); // Refresh to pass to child adapters
    }

    @NonNull
    @Override
    public ParentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_parent_all_fragment, parent, false);
        return new ParentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParentViewHolder holder, int position) {
        ParentItemAllFragment parentItem = parentItemList.get(position);
        holder.sectionTitle.setText(parentItem.getSectionTitle());

        LinearLayoutManager layoutManager = new LinearLayoutManager(holder.childRecyclerView.getContext(),
                LinearLayoutManager.HORIZONTAL, false);
        holder.childRecyclerView.setLayoutManager(layoutManager);

        // Create child adapter and set account blocked status
        ChildAdapterAllFragment childAdapter = new ChildAdapterAllFragment(parentItem.getChildItemList());
        childAdapter.setAccountBlocked(isAccountBlocked, blockedMessage); // Pass the blocked status to child adapter only
        holder.childRecyclerView.setAdapter(childAdapter);

        // Updated sellall click listener - DON'T check for account blocked here
        holder.sellall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // NO account blocked check here - let "See All" work normally

                String type = parentItem.getType();

                switch (type) {
                    case "Channels":
                        if (tabNavigationListener != null) {
                            tabNavigationListener.navigateToTab(1);
                        }
                        break;

                    case "Movies":
                        MoreLastesMovieFragment movieFragment = new MoreLastesMovieFragment();
                        activity.getSupportFragmentManager()
                                .beginTransaction()
                                .add(R.id.fragment_container, movieFragment)
                                .addToBackStack("more_movies")
                                .commit();
                        break;

                    case "TVShows":
                        MoreLatestTvShowListFragment tvShowFragment = new MoreLatestTvShowListFragment();
                        activity.getSupportFragmentManager()
                                .beginTransaction()
                                .add(R.id.fragment_container, tvShowFragment)
                                .addToBackStack("more_tv_shows")
                                .commit();
                        break;

                    case "Radio":
                        navigateToRadio();
                        break;

                    default:
                        break;
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return parentItemList.size();
    }

    // Helper method to navigate to Radio
    private void navigateToRadio() {
        // NO account blocked check here - let navigation work normally

        if (activity instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) activity;
            if (mainActivity.getBottomNavItems() != null && !mainActivity.getBottomNavItems().isEmpty()) {
                int radioPosition = -1;
                for (int i = 0; i < mainActivity.getBottomNavItems().size(); i++) {
                    String itemName = mainActivity.getBottomNavItems().get(i).getBottommenuName();
                    if (itemName != null && itemName.equalsIgnoreCase("Radio")) {
                        radioPosition = i;
                        break;
                    }
                }

                if (radioPosition != -1) {
                    mainActivity.selectTab(radioPosition);
                } else {
                    loadRadioFragmentDirectly();
                }
            } else {
                loadRadioFragmentDirectly();
            }
        } else {
            loadRadioFragmentDirectly();
        }
    }

    // Fallback method to load Radio fragment directly
    private void loadRadioFragmentDirectly() {
        RadioFragment radioFragment = new RadioFragment();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, radioFragment)
                .addToBackStack("radio")
                .commit();
    }

    // Add a public method to update the activity reference if needed
    public void setActivity(AppCompatActivity activity) {
        this.activity = activity;
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