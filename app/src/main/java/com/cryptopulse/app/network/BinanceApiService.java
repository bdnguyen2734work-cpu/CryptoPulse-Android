package com.cryptopulse.app.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface BinanceApiService {

    @GET("api/v3/ticker/24hr")
    Call<JsonArray> getAll24hrTickers();

    @GET("api/v3/ticker/24hr")
    Call<JsonObject> get24hrTicker(@Query("symbol") String symbol);

    @GET("api/v3/klines")
    Call<JsonArray> getKlines(
            @Query("symbol")    String symbol,
            @Query("interval")  String interval,
            @Query("limit")     int limit
    );
    @GET("api/v3/klines")
    Call<JsonArray> getKlinesBefore(
            @Query("symbol")    String symbol,
            @Query("interval")  String interval,
            @Query("endTime")   long endTime,
            @Query("limit")     int limit
    );
}