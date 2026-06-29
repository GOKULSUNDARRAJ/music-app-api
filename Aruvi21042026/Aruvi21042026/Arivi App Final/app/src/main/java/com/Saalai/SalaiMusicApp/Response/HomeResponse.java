package com.Saalai.SalaiMusicApp.Response;

import com.Saalai.SalaiMusicApp.Models.ArtistCategory;
import com.Saalai.SalaiMusicApp.Models.PlaylistSection;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class HomeResponse {
    @SerializedName("sections")
    private List<PlaylistSection> sections;

    public List<PlaylistSection> getSections() {
        return sections;
    }

    public void setSections(List<PlaylistSection> sections) {
        this.sections = sections;
    }
}