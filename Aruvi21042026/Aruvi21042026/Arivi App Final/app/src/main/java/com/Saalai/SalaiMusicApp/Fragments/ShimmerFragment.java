package com.Saalai.SalaiMusicApp.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerAdapterfocatchup;

public class ShimmerFragment extends Fragment {

    private int itemCount = 5; // number of shimmer cards

    public ShimmerFragment() {
    }

    public static ShimmerFragment newInstance(int itemCount) {
        ShimmerFragment fragment = new ShimmerFragment();
        fragment.itemCount = itemCount;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shimmer_list, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerShimmer);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ShimmerAdapterfocatchup adapter = new ShimmerAdapterfocatchup(itemCount);
        recyclerView.setAdapter(adapter);

        return view;
    }
}
