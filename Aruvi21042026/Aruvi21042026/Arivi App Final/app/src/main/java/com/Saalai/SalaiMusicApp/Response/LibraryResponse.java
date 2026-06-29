package com.Saalai.SalaiMusicApp.Response;


import java.util.List;

public class LibraryResponse {
    private boolean status;
    private boolean success;
    private String result;
    private List<LibrarySection> sections;

    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public List<LibrarySection> getSections() { return sections; }
    public void setSections(List<LibrarySection> sections) { this.sections = sections; }
}