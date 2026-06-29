package com.Saalai.SalaiMusicApp.Response;


import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ContactUsResponse {

    @SerializedName("result")
    private String result;

    @SerializedName("error_type")
    private String errorType;

    @SerializedName("response")
    private ResponseData response;

    public String getResult() {
        return result;
    }

    public ResponseData getResponse() {
        return response;
    }

    public static class ResponseData {
        @SerializedName("title")
        private String title;

        @SerializedName("desc")
        private String description;

        @SerializedName("list")
        private List<SupportContact> contactList;

        @SerializedName("socialList")
        private List<SocialContact> socialList;

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public List<SupportContact> getContactList() {
            return contactList;
        }

        public List<SocialContact> getSocialList() {
            return socialList;
        }
    }

    public static class SupportContact {
        @SerializedName("country")
        private String country;

        @SerializedName("contactNo")
        private String contactNo;

        public String getCountry() {
            return country;
        }

        public String getContactNo() {
            return contactNo;
        }
    }

    public static class SocialContact {
        @SerializedName("contactNo")
        private String contactNo;

        @SerializedName("type")
        private String type;

        @SerializedName("url")
        private String url;

        public String getContactNo() {
            return contactNo;
        }

        public String getType() {
            return type;
        }

        public String getUrl() {
            return url;
        }
    }
}