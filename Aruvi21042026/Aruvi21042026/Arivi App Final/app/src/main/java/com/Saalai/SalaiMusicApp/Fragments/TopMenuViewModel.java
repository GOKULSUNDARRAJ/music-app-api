package com.Saalai.SalaiMusicApp.Fragments;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.Saalai.SalaiMusicApp.Models.TopNavItem;
import java.util.ArrayList;
import java.util.List;

public class TopMenuViewModel extends ViewModel {
    private MutableLiveData<List<TopNavItem>> topMenuLiveData = new MutableLiveData<>(new ArrayList<>());

    public MutableLiveData<List<TopNavItem>> getTopMenuLiveData() {
        return topMenuLiveData;
    }

    public void setTopMenu(List<TopNavItem> items) {
        topMenuLiveData.setValue(items);
    }

    public List<TopNavItem> getTopMenu() {
        return topMenuLiveData.getValue();
    }

    public void clearTopMenu() {
        topMenuLiveData.setValue(new ArrayList<>());
    }
}