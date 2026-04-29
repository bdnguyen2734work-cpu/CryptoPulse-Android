package com.cryptopulse.app.network;

import com.cryptopulse.app.models.Kline;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // ── Kline history ─────────────────────────────────────────────
    @GET("api/v1/history/{symbol}")
    Call<Map<String, Object>> getKlineHistory(
            @Path("symbol")        String symbol,
            @Query("timeframe")    String timeframe,
            @Query("limit")        int limit,
            @Query("end_time")     Long endTime      // null = lấy mới nhất
    );

    // ── Market ────────────────────────────────────────────────────
    @GET("api/v1/market/top-coins")
    Call<Map<String, Object>> getTopCoins();

    @GET("api/v1/market/fear-greed")
    Call<Map<String, Object>> getFearGreed();

    // ── AI Trend Analysis ─────────────────────────────────────────
    // tf: "1h" | "1d" | "1w"
    @GET("api/v1/analysis/trend/{symbol}")
    Call<Map<String, Object>> getMarketTrend(
            @Path("symbol")  String symbol,
            @Query("tf")     String tf
    );

    // ── News ──────────────────────────────────────────────────────
    @GET("api/v1/news")
    Call<Map<String, Object>> getNews();

    @GET("api/v1/news/whale")
    Call<Map<String, Object>> getWhaleNews(
            @Query("force_refresh") boolean forceRefresh,
            @Query("coin")          String coin,   // null = tất cả coin
            @Query("limit")         int limit
    );

    @GET("api/v1/news/whale/coins")
    Call<Map<String, Object>> getWhaleNewsByCoins();

    // ── On-chain wallet ───────────────────────────────────────────
    // chain: "eth" | "bsc" | "polygon" | "avalanche"
    @GET("api/v1/onchain/wallet/{chain}/{address}")
    Call<Map<String, Object>> getWalletTransactions(
            @Path("chain")              String chain,
            @Path("address")            String address,
            @Query("force_refresh")     boolean forceRefresh
    );
}