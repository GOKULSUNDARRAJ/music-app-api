package com.Saalai.SalaiMusicApp.Models;

public class ChildItemMovieFragment {
    private String movieId;
    private String imageUrl;
    private String name;
    private String type;
    private String title;
    private String Url;


    public ChildItemMovieFragment(String movieId, String imageUrl, String name, String type, String title, String url) {
        this.movieId = movieId;
        this.imageUrl = imageUrl;
        this.name = name;
        this.type = type;
        this.title = title;
        Url = url;
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return Url;
    }

    public void setUrl(String url) {
        Url = url;
    }
}

