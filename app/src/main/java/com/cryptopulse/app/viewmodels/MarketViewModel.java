package com.cryptopulse.app.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.cryptopulse.app.models.CoinTicker;
import com.cryptopulse.app.models.Kline;
import com.cryptopulse.app.network.ApiClient;
import com.google.gson.JsonArray;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MarketViewModel extends ViewModel {
    // ─── TRẠNG THÁI TOÀN CỤC ───
    private final MutableLiveData<Boolean>                 loginState         = new MutableLiveData<>();
    private final MutableLiveData<Boolean>                 profileUpdated     = new MutableLiveData<>(); // Tín hiệu đổi ảnh

    private final MutableLiveData<Map<String, CoinTicker>> liveTickers        = new MutableLiveData<>();
    private final MutableLiveData<List<Kline>>             liveKlines         = new MutableLiveData<>();
    private final MutableLiveData<List<Kline>>             liveKlinesPrepend  = new MutableLiveData<>();
    private final MutableLiveData<String>                  liveError          = new MutableLiveData<>();
    private final MutableLiveData<Boolean>                 liveLoading        = new MutableLiveData<>(false);
    private final MutableLiveData<Map<String, Object>>     liveTopCoins       = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>>     liveFearGreed      = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>>     liveTrend          = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>>     liveWalletTxs      = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>>     liveNews           = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>>     liveWhaleNews      = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>>     liveWhaleNewsCoins = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>>     liveKlineHistory   = new MutableLiveData<>();

    // ══════════════════════════════════════════════════════════════
    //  GETTERS & SETTERS
    // ══════════════════════════════════════════════════════════════
    public LiveData<Boolean>                 getLoginState()       { return loginState; }
    public void setLoginState(boolean isLoggedIn)                  { loginState.setValue(isLoggedIn); }

    // Gọi hàm này khi muốn báo cho toàn App biết Profile/Ảnh đã thay đổi
    public LiveData<Boolean>                 getProfileUpdated()   { return profileUpdated; }
    public void triggerProfileUpdate()                             { profileUpdated.setValue(true); }

    public LiveData<Map<String, CoinTicker>> getTickers()        { return liveTickers; }
    public LiveData<List<Kline>>             getKlines()         { return liveKlines; }
    public LiveData<List<Kline>>             getKlinesPrepend()  { return liveKlinesPrepend; }
    public LiveData<String>                  getError()          { return liveError; }
    public LiveData<Boolean>                 getLoading()        { return liveLoading; }
    public LiveData<Map<String, Object>>     getTopCoins()       { return liveTopCoins; }
    public LiveData<Map<String, Object>>     getFearGreed()      { return liveFearGreed; }
    public LiveData<Map<String, Object>>     getTrend()          { return liveTrend; }
    public LiveData<Map<String, Object>>     getWalletTxs()      { return liveWalletTxs; }
    public LiveData<Map<String, Object>>     getNews()           { return liveNews; }
    public LiveData<Map<String, Object>>     getWhaleNews()      { return liveWhaleNews; }
    public LiveData<Map<String, Object>>     getWhaleNewsCoins() { return liveWhaleNewsCoins; }
    public LiveData<Map<String, Object>>     getKlineHistory()   { return liveKlineHistory; }

    // ══════════════════════════════════════════════════════════════
    //  WEBSOCKET PUSH
    // ══════════════════════════════════════════════════════════════
    public void updateTickers(Map<String, CoinTicker> map) {
        liveTickers.setValue(map);
    }

    // ══════════════════════════════════════════════════════════════
    //  KLINES — Binance
    // ══════════════════════════════════════════════════════════════
    public void loadKlines(String symbol, String interval, int limit) {
        liveLoading.setValue(true);
        ApiClient.binance().getKlines(symbol, interval, limit)
                .enqueue(new Callback<JsonArray>() {
                    @Override
                    public void onResponse(Call<JsonArray> call, Response<JsonArray> resp) {
                        liveLoading.setValue(false);
                        if (!resp.isSuccessful() || resp.body() == null) return;
                        liveKlines.setValue(parseKlines(symbol, resp.body()));
                    }
                    @Override
                    public void onFailure(Call<JsonArray> call, Throwable t) {
                        liveLoading.setValue(false);
                        liveError.setValue("Klines: " + t.getMessage());
                    }
                });
    }

    public void loadKlinesBefore(String symbol, String interval, long endTime, int limit) {
        ApiClient.binance().getKlinesBefore(symbol, interval, endTime, limit)
                .enqueue(new Callback<JsonArray>() {
                    @Override
                    public void onResponse(Call<JsonArray> call, Response<JsonArray> resp) {
                        if (!resp.isSuccessful() || resp.body() == null) {
                            liveKlinesPrepend.postValue(new ArrayList<>());
                            return;
                        }
                        liveKlinesPrepend.postValue(parseKlines(symbol, resp.body()));
                    }
                    @Override
                    public void onFailure(Call<JsonArray> call, Throwable t) {
                        liveKlinesPrepend.postValue(new ArrayList<>());
                        liveError.postValue("Klines before: " + t.getMessage());
                    }
                });
    }

    private List<Kline> parseKlines(String symbol, JsonArray arr) {
        List<Kline> list = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JsonArray r = arr.get(i).getAsJsonArray();
            list.add(new Kline(
                    symbol,
                    r.get(0).getAsLong() / 1000L,
                    r.get(1).getAsDouble(),
                    r.get(2).getAsDouble(),
                    r.get(3).getAsDouble(),
                    r.get(4).getAsDouble(),
                    r.get(5).getAsDouble()
            ));
        }
        return list;
    }

    // ══════════════════════════════════════════════════════════════
    //  KLINE HISTORY — từ backend DB (phân trang)
    // ══════════════════════════════════════════════════════════════
    public void loadKlineHistory(String symbol, String timeframe, int limit, Long endTime) {
        if (ApiClient.get() == null) return;
        liveLoading.setValue(true);
        ApiClient.get().getKlineHistory(symbol, timeframe, limit, endTime)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call,
                                           Response<Map<String, Object>> resp) {
                        liveLoading.setValue(false);
                        if (resp.isSuccessful() && resp.body() != null)
                            liveKlineHistory.setValue(resp.body());
                    }
                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        liveLoading.setValue(false);
                        liveError.setValue("Kline history: " + t.getMessage());
                    }
                });
    }

    public void loadKlineHistory(String symbol, String timeframe, int limit) {
        loadKlineHistory(symbol, timeframe, limit, null);
    }

    // ══════════════════════════════════════════════════════════════
    //  TOP COINS & FEAR GREED
    // ══════════════════════════════════════════════════════════════
    public void loadTopCoins() {
        if (ApiClient.get() == null) return;
        ApiClient.get().getTopCoins()
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> resp) {
                        if (resp.isSuccessful() && resp.body() != null) liveTopCoins.setValue(resp.body());
                    }
                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        liveError.setValue("Top coins: " + t.getMessage());
                    }
                });
    }

    public void loadFearGreed() {
        if (ApiClient.get() == null) return;
        ApiClient.get().getFearGreed()
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> resp) {
                        if (resp.isSuccessful() && resp.body() != null) liveFearGreed.setValue(resp.body());
                    }
                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        liveError.setValue("Fear & Greed: " + t.getMessage());
                    }
                });
    }

    // ══════════════════════════════════════════════════════════════
    //  AI TREND ANALYSIS (GỌI API TỪ FASTAPI)
    // ══════════════════════════════════════════════════════════════
    public void loadMarketTrend(String symbol, String tf) {
        if (ApiClient.get() == null) return;
        liveLoading.setValue(true);
        ApiClient.get().getMarketTrend(symbol, tf)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call,
                                           Response<Map<String, Object>> resp) {
                        liveLoading.setValue(false);
                        if (resp.isSuccessful() && resp.body() != null) {
                            liveTrend.setValue(resp.body());
                        } else {
                            liveError.setValue("Trend API Error");
                        }
                    }
                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        liveLoading.setValue(false);
                        liveError.setValue("Trend: " + t.getMessage());
                    }
                });
    }

    // ══════════════════════════════════════════════════════════════
    //  NEWS & ON-CHAIN WALLET
    // ══════════════════════════════════════════════════════════════
    public void loadNews() {
        if (ApiClient.get() == null) return;
        ApiClient.get().getNews().enqueue(new Callback<Map<String, Object>>() {
            @Override public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> resp) {
                if (resp.isSuccessful() && resp.body() != null) liveNews.setValue(resp.body());
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                liveError.setValue("News: " + t.getMessage());
            }
        });
    }

    public void loadWhaleNews(boolean forceRefresh) { loadWhaleNews(forceRefresh, null, 20); }
    public void loadWhaleNewsByCoin(String coin) { loadWhaleNews(false, coin, 20); }

    public void loadWhaleNews(boolean forceRefresh, String coin, int limit) {
        if (ApiClient.get() == null) return;
        liveLoading.setValue(true);
        ApiClient.get().getWhaleNews(forceRefresh, coin, limit)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> resp) {
                        liveLoading.setValue(false);
                        if (resp.isSuccessful() && resp.body() != null) liveWhaleNews.setValue(resp.body());
                    }
                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        liveLoading.setValue(false);
                        liveError.setValue("Whale news: " + t.getMessage());
                    }
                });
    }

    public void loadWhaleNewsByCoins() {
        if (ApiClient.get() == null) return;
        ApiClient.get().getWhaleNewsByCoins().enqueue(new Callback<Map<String, Object>>() {
            @Override public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> resp) {
                if (resp.isSuccessful() && resp.body() != null) liveWhaleNewsCoins.setValue(resp.body());
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                liveError.setValue("Whale coins: " + t.getMessage());
            }
        });
    }

    public void loadWalletTransactions(String chain, String address, boolean forceRefresh) {
        if (ApiClient.get() == null) return;
        liveLoading.setValue(true);

        ApiClient.get().getWalletTransactions(chain, address, forceRefresh)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> resp) {
                        liveLoading.setValue(false);
                        if (resp.isSuccessful() && resp.body() != null) {
                            liveWalletTxs.setValue(resp.body());
                        } else {
                            liveError.setValue("Wallet API Error: " + resp.code());
                        }
                    }
                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        liveLoading.setValue(false);
                        liveError.setValue("Wallet: " + t.getMessage());
                    }
                });
    }

    // ══════════════════════════════════════════════════════════════
    //  DEMO DATA
    // ══════════════════════════════════════════════════════════════
    public static List<CoinTicker> getDemoTickers() {
        String[][] rows = {
                {"BTCUSDT",  "68492.20", "4.82",  "69120.00", "64280.50", "1200000"},
                {"ETHUSDT",  "2482.91",  "5.24",  "2510.00",  "2350.00",  "800000"},
                {"BNBUSDT",  "412.30",   "1.85",  "420.00",   "395.00",   "300000"},
                {"SOLUSDT",  "102.45",   "8.40",  "108.00",   "92.00",    "500000"},
                {"ADAUSDT",  "0.512",    "-2.10", "0.540",    "0.498",    "200000"},
                {"XRPUSDT",  "0.584",    "3.20",  "0.600",    "0.560",    "400000"},
                {"DOGEUSDT", "0.0821",   "6.50",  "0.0890",   "0.0750",   "900000"}
        };
        List<CoinTicker> list = new ArrayList<>();
        for (String[] r : rows) {
            CoinTicker t    = new CoinTicker();
            t.symbol        = r[0];
            t.close         = r[1];
            t.changePercent = r[2];
            t.high          = r[3];
            t.low           = r[4];
            t.volume        = r[5];
            double closeVal = Double.parseDouble(r[1]);
            double pct      = Double.parseDouble(r[2]);
            t.open          = String.valueOf(closeVal / (1.0 + pct / 100.0));
            list.add(t);
        }
        return list;
    }
}