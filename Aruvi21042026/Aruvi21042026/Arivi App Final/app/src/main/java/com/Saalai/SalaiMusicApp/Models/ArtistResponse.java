package com.Saalai.SalaiMusicApp.Models;


import com.Saalai.SalaiMusicApp.Models.PlaylistSection;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ArtistResponse {

    @SerializedName("sections")
    private List<PlaylistSection> sections;

    public List<PlaylistSection> getSections() {
        return sections;
    }

    public void setSections(List<PlaylistSection> sections) {
        this.sections = sections;
    }
}