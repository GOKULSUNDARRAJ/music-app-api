package com.Saalai.SalaiMusicApp.Adapters;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.Saalai.SalaiMusicApp.Fragments.AudioDownloadFragment;
import com.Saalai.SalaiMusicApp.Fragments.DownloadPlaylistsFragment;

import org.jspecify.annotations.NonNull;

public class DownloadPagerAdapter extends FragmentStateAdapter {

    public DownloadPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) return new AudioDownloadFragment();
        else return new DownloadPlaylistsFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
