package com.Saalai.SalaiMusicApp.Response;



import com.Saalai.SalaiMusicApp.Models.PlaylistSection;
import java.util.List;

public class FollowedArtistsResponse {
    private boolean status;
    private boolean success;
    private List<PlaylistSection> sections;
    private String message;

    public boolean isStatus() {
        return status;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<PlaylistSection> getSections() {
        return sections;
    }

    public String getMessage() {
        return message;
    }
}