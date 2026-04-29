package com.cryptopulse.app.network;

import android.os.Handler;
import android.os.Looper;
import com.cryptopulse.app.models.CoinTicker;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BinanceWebSocketManager {
    private static final String WS_URL =
            "wss://stream.binance.com:9443/stream?streams=" +
                    "btcusdt@ticker/ethusdt@ticker/bnbusdt@ticker/solusdt@ticker/" +
                    "xrpusdt@ticker/adausdt@ticker/dogeusdt@ticker/avaxusdt@ticker/" +
                    "dotusdt@ticker/linkusdt@ticker/polusdt@ticker/uniusdt@ticker/" +
                    "atomusdt@ticker/ltcusdt@ticker/nearusdt@ticker/aptusdt@ticker/" +
                    "arbusdt@ticker/opusdt@ticker/injusdt@ticker/suiusdt@ticker/" +
                    "trxusdt@ticker/shibusdt@ticker/bchusdt@ticker/icpusdt@ticker";

    public interface TickerListener {
        void onTickerUpdate(Map<String, CoinTicker> tickers);
        void onConnected();
        void onDisconnected();
    }

    private static BinanceWebSocketManager instance;
    public static BinanceWebSocketManager getInstance() {
        if (instance == null) instance = new BinanceWebSocketManager();
        return instance;
    }

    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<TickerListener> listeners = new ArrayList<>();
    private final ExecutorService parseThread = Executors.newSingleThreadExecutor();

    private final Map<String, CoinTicker> pendingMap = new HashMap<>();
    private boolean flushPending = false;
    private static final long FLUSH_MS = 200;

    private WebSocket webSocket;
    private boolean connected = false;
    private int reconnectMs = 3_000;

    private BinanceWebSocketManager() {
        client = new OkHttpClient.Builder()
                .pingInterval(20, TimeUnit.SECONDS)
                .build();
    }

    public void addListener(TickerListener l) { if (!listeners.contains(l)) listeners.add(l); }
    public void removeListener(TickerListener l) { listeners.remove(l); }

    public void connect() {
        if (connected) return;
        webSocket = client.newWebSocket(new Request.Builder().url(WS_URL).build(), new WsListener());
    }

    public void disconnect() {
        if (webSocket != null) { webSocket.cancel(); webSocket = null; }
        connected = false;
    }

    private class WsListener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket ws, Response r) {
            connected = true;
            mainHandler.post(() -> { for (TickerListener l : new ArrayList<>(listeners)) l.onConnected(); });
        }

        @Override
        public void onMessage(WebSocket ws, String text) {
            parseThread.execute(() -> {
                try {
                    JsonObject root = JsonParser.parseString(text).getAsJsonObject();
                    if (!root.has("data")) return;
                    CoinTicker ticker = gson.fromJson(root.getAsJsonObject("data"), CoinTicker.class);
                    if (ticker == null || ticker.symbol == null) return;

                    mainHandler.post(() -> {
                        pendingMap.put(ticker.symbol, ticker);
                        if (!flushPending) {
                            flushPending = true;
                            mainHandler.postDelayed(() -> {
                                if (!pendingMap.isEmpty()) {
                                    Map<String, CoinTicker> snapshot = new HashMap<>(pendingMap);
                                    pendingMap.clear();
                                    for (TickerListener l : new ArrayList<>(listeners)) l.onTickerUpdate(snapshot);
                                }
                                flushPending = false;
                            }, FLUSH_MS);
                        }
                    });
                } catch (Exception ignored) {}
            });
        }

        @Override
        public void onClosed(WebSocket ws, int code, String reason) {
            connected = false;
            scheduleReconnect();
        }

        @Override
        public void onFailure(WebSocket ws, Throwable t, Response r) {
            connected = false;
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        mainHandler.postDelayed(this::connect, reconnectMs);
        reconnectMs = Math.min(reconnectMs * 2, 30_000);
    }
}