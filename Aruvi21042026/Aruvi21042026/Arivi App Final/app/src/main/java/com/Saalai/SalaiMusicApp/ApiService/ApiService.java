package com.Saalai.SalaiMusicApp.ApiService;

import com.Saalai.SalaiMusicApp.Models.AddToPlaylistRequest;
import com.Saalai.SalaiMusicApp.Models.ArtistResponse;
import com.Saalai.SalaiMusicApp.Models.DevotionalResponse;
import com.Saalai.SalaiMusicApp.Models.NavigationResponse;
import com.Saalai.SalaiMusicApp.Models.PlaylistModels;
import com.Saalai.SalaiMusicApp.Models.PlaylistStatusResponse;
import com.Saalai.SalaiMusicApp.Models.RecentPlayRequest;
import com.Saalai.SalaiMusicApp.Response.AddToPlaylistResponse;
import com.Saalai.SalaiMusicApp.Response.CatchUpChannelDetailsResponse;
import com.Saalai.SalaiMusicApp.Response.CatchUpResponse;
import com.Saalai.SalaiMusicApp.Response.ContactUsResponse;
import com.Saalai.SalaiMusicApp.Response.DashboardResponse;
import com.Saalai.SalaiMusicApp.Response.EnquiryResponse;
import com.Saalai.SalaiMusicApp.Response.FollowRequest;
import com.Saalai.SalaiMusicApp.Response.FollowResponse;
import com.Saalai.SalaiMusicApp.Response.FollowStatusResponse;
import com.Saalai.SalaiMusicApp.Response.FollowedArtistsResponse;
import com.Saalai.SalaiMusicApp.Response.HomeResponse;
import com.Saalai.SalaiMusicApp.Response.LibraryResponse;
import com.Saalai.SalaiMusicApp.Response.LikeRequest;
import com.Saalai.SalaiMusicApp.Response.LikeResponse;
import com.Saalai.SalaiMusicApp.Response.LikeStatusResponse;
import com.Saalai.SalaiMusicApp.Response.LikedCategoriesResponse;
import com.Saalai.SalaiMusicApp.Response.LiveTvResponse;


import com.Saalai.SalaiMusicApp.Response.MyPlaylistResponse;
import com.Saalai.SalaiMusicApp.Response.OtpResponse;
import com.Saalai.SalaiMusicApp.Response.RadioResponse;
import com.Saalai.SalaiMusicApp.Response.RegisterResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @FormUrlEncoded
    @POST("checkRegister")
    Call<RegisterResponse> checkRegister(
            @Field("grant_type") String grantType,
            @Field("client_id") String clientId,
            @Field("userCountry") String userCountry,
            @Field("userMobile") String userMobile,
            @Field("deviceID") String deviceID,
            @Field("mobileType") String mobileType,
            @Field("device_token") String deviceToken,
            @Field("name") String name,
            @Field("referalCode") String referalCode
    );

    @FormUrlEncoded
    @POST("secure/numberVerification")
    Call<OtpResponse> verifyOtp(
            @Header("Authorization") String authToken,
            @Field("verificationCode") String verificationCode
    );

    @POST("secure/getDashboardList")
    Call<DashboardResponse> getDashboardList(
            @Header("Authorization") String authToken
    );

    @FormUrlEncoded
    @POST("secure/getLiveTvList")
    Call<LiveTvResponse> getLiveTvList(
            @Header("Authorization") String authToken,
            @Field("offset") String offset,
            @Field("count") String count
    );

    @FormUrlEncoded
    @POST("secure/getRadioList")
    Call<RadioResponse> getRadioList(
            @Header("Authorization") String authToken,
            @Field("channelId") String channelId,
            @Field("offset") String offset,
            @Field("count") String count
    );

    @POST("secure/getMovieDashboadList")
    Call<ResponseBody> getMovieDashboard
            (@Header("Authorization") String token
            );


    @FormUrlEncoded
    @POST("secure/getMovieDetails")
    Call<ResponseBody> getMovieDetails(
            @Header("Authorization") String token,
            @Field("channelId") String movieId
    );

    @POST("secure/getTvShowDashboadList")
    Call<ResponseBody> getTvShowDashboard(
            @Header("Authorization") String token
    );

    @FormUrlEncoded
    @POST("secure/getCategoryMovieList")
    Call<ResponseBody> getMovieList(
            @Header("Authorization") String token,
            @Field("categoryId") int categoryId,
            @Field("offset") int offset,
            @Field("count") int count
    );

    @FormUrlEncoded
    @POST("secure/getTvShowEpisodeList")
    Call<ResponseBody> getTvShowEpisodeList(
            @Header("Authorization") String token,
            @Field("channelId") String channelId,
            @Field("episodeId") String episodeId,
            @Field("offset") String offset,
            @Field("count") String count
    );


    @FormUrlEncoded
    @POST("secure/getTvShowList")
    Call<ResponseBody> getTvShowList(
            @Header("Authorization") String token,
            @Field("categoryId") int categoryId,
            @Field("offset") int offset,
            @Field("count") int count
    );


    @FormUrlEncoded
    @POST("secure/getCatchupChannelList")
    Call<CatchUpResponse> getCatchUpChannelList(
            @Header("Authorization") String authToken,
            @Field("offset") String offset,
            @Field("count") String count
    );

    @FormUrlEncoded
    @POST("secure/getCatchupChannelDetails")
    Call<CatchUpChannelDetailsResponse> getCatchUpChannelDetails(
            @Header("Authorization") String authToken,
            @Field("channelId") int channelId
    );


    @FormUrlEncoded
    @POST("secure/getLatestMovieList")
    Call<ResponseBody> getLatestMovieList(
            @Header("Authorization") String token,
            @Field("offset") int offset,
            @Field("count") int count
    );

    @FormUrlEncoded
    @POST("secure/getLatestTvShowList")
    Call<ResponseBody> getLatestTvShowList(
            @Header("Authorization") String token,
            @Field("offset") int offset,
            @Field("count") int count
    );


    @POST("secure/logout")
    Call<ResponseBody> logout(
            @Header("Authorization") String authToken
    );

    @FormUrlEncoded
    @POST("secure/updateStreamTime")
    Call<ResponseBody> updateStreamTime(
            @Header("Authorization") String authToken,
            @Field("channelId") String channelId,
            @Field("type") String type,
            @Field("time") String time
    );

    @POST("countryList")
    Call<ResponseBody> getCountryList();

    @POST("help")
    Call<ResponseBody> getHelp();

    @POST("getTermsAndConditions") Call<ResponseBody> getTermsAndConditions();

    // Add to ApiService.java
    @POST("secure/appMenuList")
    Call<NavigationResponse> getNavigationMenu(
            @Header("Authorization") String authToken);


    // Add this method to your ApiService interface:
    @FormUrlEncoded
    @POST("secure/reSendVerificationCode")
    Call<OtpResponse> resendVerificationCode(
            @Header("Authorization") String authToken);

    // In your ApiService interface
    @POST("contactUs")
    Call<ContactUsResponse> getContactUs();

    // In ApiService.java, add this method:
    @POST("secure/sendEnquiry")
    Call<EnquiryResponse> sendEnquiry(
            @Header("Authorization") String authToken);


    @GET("home")
    Call<HomeResponse> getHomeData(
            @Header("Authorization") String authToken
    );

    @GET("artist")
    Call<ArtistResponse> getArtistData(
            @Header("Authorization") String authToken
    );

    @GET("devotional")
    Call<DevotionalResponse> getDevotionalData(
            @Header("Authorization") String authToken
    );

    @POST("user/recordRecentPlay")
    Call<Void> recordRecentPlay(
            @Header("Authorization") String authorization,
            @Body RecentPlayRequest request
    );



    // Add these methods to your ApiService.java

    @POST("category/toggle")
    Call<LikeResponse> toggleCategoryLike(
            @Header("Authorization") String authorization,
            @Body LikeRequest request
    );

    @GET("category/status/{categoryId}")
    Call<LikeStatusResponse> getCategoryLikeStatus(
            @Header("Authorization") String authorization,
            @Path("categoryId") String categoryId
    );

    @GET("categories")
    Call<LikedCategoriesResponse> getLikedCategories(
            @Header("Authorization") String authToken
    );


    // Add these methods to your ApiService interface

    // Add these methods to your ApiService interface with proper naming
    @POST("AddToMyPlayList")
    Call<AddToPlaylistResponse> addToMyPlaylist(
            @Header("Authorization") String authorization,
            @Body AddToPlaylistRequest request
    );

    @GET("playlist/status/{categoryId}")
    Call<PlaylistStatusResponse> getPlaylistItemStatus(
            @Header("Authorization") String authorization,
            @Path("categoryId") String categoryId
    );

    @GET("playlist/my")
    Call<MyPlaylistResponse> getMyPlaylist(
            @Header("Authorization") String authorization
    );



    //create

    // FIXED: Use the model class instead of String
    @POST("playlist/create")
    Call<PlaylistModels.CreatePlaylistResponse> createPlaylist(
            @Header("Authorization") String authToken,
            @Body PlaylistModels.CreatePlaylistRequest request
    );

    @POST("playlist/add-item")
    Call<PlaylistModels.AddItemResponse> addItemToPlaylist(
            @Header("Authorization") String authToken,
            @Body PlaylistModels.AddItemRequest request
    );


    // Add this to your existing ApiService interface
    @GET("playlist/user-categories")
    Call<LibraryResponse> getUserPlaylists(@Header("Authorization") String token);


    @GET("playlist/status/{playlistId}")
    Call<PlaylistModels.PlaylistStatusResponse> checkSongInPlaylist(
            @Header("Authorization") String authHeader,
            @Path("playlistId") int playlistId,
            @Query("songId") String songId
    );


    @GET("artist/follow/status/{artistId}")
    Call<FollowStatusResponse> getFollowStatus(
            @Header("Authorization") String authHeader,
            @Path("artistId") String artistId
    );

    @POST("artist/follow/toggle")
    Call<FollowResponse> toggleFollowArtist(
            @Header("Authorization") String authHeader,
            @Body FollowRequest request
    );


    @GET("artist/followed")
    Call<FollowedArtistsResponse> getFollowedArtists(
            @Header("Authorization") String authHeader
    );


}
