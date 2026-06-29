package com.Saalai.SalaiMusicApp.Response;


import com.Saalai.SalaiMusicApp.Models.PlaylistSection;
import java.util.List;

public class MyPlaylistResponse {
    private List<PlaylistSection> sections;
    private boolean status;
    private String message;

    public List<PlaylistSection> getSections() {
        return sections;
    }

    public void setSections(List<PlaylistSection> sections) {
        this.sections = sections;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
