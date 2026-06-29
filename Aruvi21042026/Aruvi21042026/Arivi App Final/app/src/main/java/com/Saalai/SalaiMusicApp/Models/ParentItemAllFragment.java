package com.Saalai.SalaiMusicApp.Models;

import java.util.List;

public class ParentItemAllFragment {
    private String sectionTitle;
    private String type;
    private List<ChildItemAllFragment> childItemList;

    public ParentItemAllFragment(String sectionTitle, String type, List<ChildItemAllFragment> childItemList) {
        this.sectionTitle = sectionTitle;
        this.type = type;
        this.childItemList = childItemList;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<ChildItemAllFragment> getChildItemList() {
        return childItemList;
    }

    public void setChildItemList(List<ChildItemAllFragment> childItemList) {
        this.childItemList = childItemList;
    }
}
