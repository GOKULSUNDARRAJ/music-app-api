package com.Saalai.SalaiMusicApp.Fragments;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.Saalai.SalaiMusicApp.R;

public class ViewMoreAllFragment extends Fragment {

    String SeeAllType,Title;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_view_more_all, container, false);

        ImageView back = view.findViewById(R.id.backimg);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SaalaiFragment fragment = new SaalaiFragment();
                AppCompatActivity activity = (AppCompatActivity) v.getContext();
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });


        TextView titleText = view.findViewById(R.id.channel_name);
        if (Title != null) {
            titleText.setText(Title);
        }

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

            SeeAllType = getArguments().getString("SeeAllType");
            Title = getArguments().getString("Title");

        }
    }

}