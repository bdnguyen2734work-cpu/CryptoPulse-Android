package com.cryptopulse.app.network;

import com.cryptopulse.app.utils.AppPrefs;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class ApiClient {

    private static ApiService        apiService;
    private static BinanceApiService binanceService;
    private static final String BINANCE_BASE = "https://api.binance.com/";

    public static void init() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(new HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BASIC))
                .build();

        String backendBase = AppPrefs.get().getBackendUrl() + "/";
        try {
            apiService = new Retrofit.Builder()
                    .baseUrl(backendBase)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService.class);
        } catch (Exception ignored) { /* invalid URL in dev */ }

        binanceService = new Retrofit.Builder()
                .baseUrl(BINANCE_BASE)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(BinanceApiService.class);
    }

    public static ApiService        get()     { return apiService; }
    public static BinanceApiService binance() { return binanceService; }
}