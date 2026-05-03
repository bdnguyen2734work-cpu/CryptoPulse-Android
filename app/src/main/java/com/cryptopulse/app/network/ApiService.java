package com.cryptopulse.app.network;

import com.cryptopulse.app.models.Kline;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @GET("api/v1/history/{symbol}")
    Call<Map<String, Object>> getKlineHistory(
            @Path("symbol") String symbol,
            @Query("timeframe") String timeframe,
            @Query("limit") int limit,
            @Query("end_time") Long endTime
    );

    @GET("api/v1/market/top-coins")
    Call<Map<String, Object>> getTopCoins();

    @GET("api/v1/market/fear-greed")
    Call<Map<String, Object>> getFearGreed();

    @GET("api/v1/analysis/trend/{symbol}")
    Call<Map<String, Object>> getMarketTrend(
            @Path("symbol") String symbol,
            @Query("tf") String tf
    );

    @GET("api/v1/news")
    Call<Map<String, Object>> getNews();

    @GET("api/v1/news/whale")
    Call<Map<String, Object>> getWhaleNews(
            @Query("force_refresh") boolean forceRefresh,
            @Query("coin") String coin,
            @Query("limit") int limit
    );

    @GET("api/v1/news/whale/coins")
    Call<Map<String, Object>> getWhaleNewsByCoins();

    @GET("api/v1/onchain/wallet/{chain}/{address}")
    Call<Map<String, Object>> getWalletTransactions(
            @Path("chain") String chain,
            @Path("address") String address,
            @Query("force_refresh") boolean forceRefresh
    );

    @PUT("/api/v1/auth/me")
    Call<Map<String, Object>> updateProfile(
            @Header("Authorization") String token,
            @Body Map<String, String> body
    );
    @POST("/api/v1/auth/avatar")
    Call<Map<String, Object>> uploadAvatar(
            @Header("Authorization") String token,
            @Body Map<String, String> body
    );

    @DELETE("/api/v1/auth/avatar")
    Call<Map<String, Object>> deleteAvatar(@Header("Authorization") String token);

    @POST("/api/v1/news/admin/post")
    Call<Map<String, Object>> postNews(
            @Header("Authorization") String token,
            @Body Map<String, Object> body
    );

    @DELETE("/api/v1/news/admin/{id}")
    Call<Map<String, Object>> deleteNews(
            @Header("Authorization") String token,
            @Path("id") int newsId
    );

    @GET("/api/v1/admin/users")
    Call<Map<String, Object>> getAllUsers(
            @Header("Authorization") String token,
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("search") String search
    );

    @DELETE("/api/v1/admin/users/{id}")
    Call<Map<String, Object>> deleteUser(
            @Header("Authorization") String token,
            @Path("id") int userId
    );

    @GET("api/v1/admin/stats")
    Call<Map<String, Object>> getAdminStats(@Header("Authorization") String token);

    @POST("api/v1/admin/news/hide")
    Call<Map<String, Object>> hideWhaleNews(
            @Header("Authorization") String token,
            @Body Map<String, String> body
    );

    @GET("/api/v1/auth/me")
    Call<Map<String, Object>> getProfile(@Header("Authorization") String token);
}