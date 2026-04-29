package com.cryptopulse.app.activities;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.cryptopulse.app.R;
import com.cryptopulse.app.models.CoinTicker;
import com.cryptopulse.app.models.Kline;
import com.cryptopulse.app.network.BinanceWebSocketManager;
import com.cryptopulse.app.utils.AppPrefs;
import com.cryptopulse.app.viewmodels.MarketViewModel;
import com.github.mikephil.charting.charts.CandleStickChart;
import com.github.mikephil.charting.components.*;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class CoinDetailActivity extends AppCompatActivity
        implements BinanceWebSocketManager.TickerListener {

    private static final String[] TF_LABELS = {"1M","5M","15M","1H","1D","1W"};
    private static final String[] TF_CHART  = {"1m","5m","15m","1h","1d","1w"};
    private static final String[] TF_SENT   = {"h1","h1","h1","h1","1d","1w"};
    private static final String[] SENT_TABS = {"h1","1d","1w"};

    private static final int PAGE_SIZE = 100;

    private String          symbol;
    private int             currentTfIdx   = 3;
    private boolean         isLoadingMore  = false;
    private boolean         hasMoreData    = true;

    private final List<Kline> allKlines = new ArrayList<>();

    private MarketViewModel viewModel;
    private AppPrefs        prefs;

    // UI
    private TextView         tvPrice, tvChange, tvHigh, tvLow, tvVolume; // Đã xóa tvMarketCap
    private TextView         tvSentiment, tvSentimentLabel;
    private TextView         tvRsi, tvMacd, tvSupport, tvEma20, tvEma50, tvFearGreed;
    private ImageView        btnFavorite, ivTokenLogo;
    private CandleStickChart chart;
    private final View[]     tfBtns   = new View[6];
    private final View[]     sentBtns = new View[3];

    // Candle info popup
    private View     candleInfoCard;
    private TextView tvCandleTime, tvCandleOpen, tvCandleHigh;
    private TextView tvCandleLow,  tvCandleClose, tvCandleVolume;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_coin_detail);

        symbol    = getIntent().getStringExtra("symbol");
        if (symbol == null) symbol = "BTCUSDT";
        viewModel = new ViewModelProvider(this).get(MarketViewModel.class);
        prefs     = AppPrefs.get();

        bindViews();
        setupTfButtons();
        setupSentimentTabs();
        setupFavoriteButton();
        showPriceFromCache();

        BinanceWebSocketManager.getInstance().addListener(this);

        loadChartFresh("1h");

        // Observe klines mới (fresh load)
        viewModel.getKlines().observe(this, klines -> {
            if (klines == null || klines.isEmpty()) return;
            allKlines.clear();
            allKlines.addAll(klines);
            hasMoreData = klines.size() >= PAGE_SIZE;
            renderInitialChart();
        });

        // Observe klines load về quá khứ
        viewModel.getKlinesPrepend().observe(this, klines -> {
            if (klines == null || klines.isEmpty()) {
                hasMoreData = false;
                isLoadingMore = false;
                return;
            }

            List<Kline> prepend = new ArrayList<>(klines);
            if (!prepend.isEmpty() && !allKlines.isEmpty()) {
                prepend.remove(prepend.size() - 1); // Tránh trùng nến nối
            }

            int newItemsCount = prepend.size();
            prepend.addAll(allKlines);
            allKlines.clear();
            allKlines.addAll(prepend);

            hasMoreData = klines.size() >= PAGE_SIZE;
            updateChartWithHistory(newItemsCount);
            isLoadingMore = false;
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  BIND VIEWS
    // ══════════════════════════════════════════════════════════════
    private void bindViews() {
        CoinTicker info = new CoinTicker();
        info.symbol = symbol;
        TextView tvSymbol = findViewById(R.id.tv_detail_symbol);
        tvSymbol.setText(info.getCoinName() + " (" + info.getDisplaySymbol() + ")");

        ivTokenLogo = findViewById(R.id.iv_token_logo);
        if (!info.getLogoUrl().isEmpty())
            Glide.with(this)
                    .load(info.getLogoUrl())
                    .error(R.drawable.ic_launcher_foreground)
                    .into(ivTokenLogo);

        tvPrice         = findViewById(R.id.tv_detail_price);
        tvChange        = findViewById(R.id.tv_detail_change);
        tvHigh          = findViewById(R.id.tv_24h_high);
        tvLow           = findViewById(R.id.tv_24h_low);
        tvVolume        = findViewById(R.id.tv_24h_volume);

        tvSentiment     = findViewById(R.id.tv_detail_sentiment);
        tvSentimentLabel = findViewById(R.id.tv_detail_sentiment_label);
        tvRsi           = findViewById(R.id.tv_detail_rsi);
        tvMacd          = findViewById(R.id.tv_detail_macd);
        tvSupport       = findViewById(R.id.tv_detail_support);
        tvEma20         = findViewById(R.id.tv_detail_ema20);
        tvEma50         = findViewById(R.id.tv_detail_ema50);
        tvFearGreed     = findViewById(R.id.tv_detail_fear_greed);
        chart           = findViewById(R.id.candle_chart);

        // Candle info popup
        candleInfoCard  = findViewById(R.id.card_candle_info);
        tvCandleTime    = findViewById(R.id.tv_candle_time);
        tvCandleOpen    = findViewById(R.id.tv_candle_open);
        tvCandleHigh    = findViewById(R.id.tv_candle_high);
        tvCandleLow     = findViewById(R.id.tv_candle_low);
        tvCandleClose   = findViewById(R.id.tv_candle_close);
        tvCandleVolume  = findViewById(R.id.tv_candle_volume);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        setupChartConfig();
    }

    // ══════════════════════════════════════════════════════════════
    //  CHART CONFIG & VUỐT MƯỢT MÀ
    // ══════════════════════════════════════════════════════════════
    private void setupChartConfig() {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);

        // Mở khóa chuyển động
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDoubleTapToZoomEnabled(true);
        chart.setAutoScaleMinMaxEnabled(true); // Bật để nến luôn co giãn mập mạp
        chart.setDragDecelerationEnabled(true);
        chart.setDragDecelerationFrictionCoef(0.9f);

        // Chặn NestedScrollView khi vuốt biểu đồ
        chart.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return false;
        });

        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.setMaxVisibleValueCount(200);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGridColor(0x15FFFFFF);
        xAxis.setTextColor(0xFF888888);
        xAxis.setTextSize(9f);
        xAxis.setAvoidFirstLastClipping(true);

        chart.getAxisRight().setGridColor(0x15FFFFFF);
        chart.getAxisRight().setTextColor(0xFF888888);
        chart.getAxisRight().setTextSize(9f);
        chart.getAxisRight().setSpaceTop(10f);
        chart.getAxisRight().setSpaceBottom(10f);
        chart.getAxisLeft().setEnabled(false);

        // Load quá khứ khi vuốt về trái
        chart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastGesture) {
                if (chart.getLowestVisibleX() <= 10 && !isLoadingMore && hasMoreData) loadMoreHistory();
            }
            @Override public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastGesture) {}
            @Override public void onChartLongPressed(MotionEvent me) {}
            @Override public void onChartDoubleTapped(MotionEvent me) {}
            @Override public void onChartSingleTapped(MotionEvent me) {
                if (candleInfoCard != null) candleInfoCard.setVisibility(View.GONE);
            }
            @Override public void onChartFling(MotionEvent me1, MotionEvent me2, float vX, float vY) {}
            @Override public void onChartScale(MotionEvent me, float scaleX, float scaleY) {}
            @Override public void onChartTranslate(MotionEvent me, float dX, float dY) {
                if (chart.getLowestVisibleX() <= 10 && !isLoadingMore && hasMoreData) loadMoreHistory();
            }
        });

        // Hiển thị thông tin nến khi chạm
        chart.setOnChartValueSelectedListener(new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, Highlight h) {
                int idx = (int) e.getX();
                if (idx >= 0 && idx < allKlines.size()) showCandleInfo(allKlines.get(idx));
            }
            @Override public void onNothingSelected() {
                if (candleInfoCard != null) candleInfoCard.setVisibility(View.GONE);
            }
        });
    }

    private void loadChartFresh(String tf) {
        allKlines.clear();
        hasMoreData = true;
        viewModel.loadKlines(symbol, tf, PAGE_SIZE);
    }

    private void loadMoreHistory() {
        if (allKlines.isEmpty()) return;
        isLoadingMore = true;
        long endTimeMs = allKlines.get(0).openTime * 1000L;
        viewModel.loadKlinesBefore(symbol, TF_CHART[currentTfIdx], endTimeMs, PAGE_SIZE);
    }

    // ══════════════════════════════════════════════════════════════
    //  VẼ BIỂU ĐỒ & FORMAT THỜI GIAN
    // ══════════════════════════════════════════════════════════════
    private void renderInitialChart() {
        List<CandleEntry> entries = new ArrayList<>();
        for (int i = 0; i < allKlines.size(); i++) {
            Kline k = allKlines.get(i);
            entries.add(new CandleEntry(i, (float) k.high, (float) k.low, (float) k.open, (float) k.close));
        }

        CandleDataSet ds = new CandleDataSet(entries, "");
        ds.setDecreasingColor(0xFFFF4444);
        ds.setDecreasingPaintStyle(android.graphics.Paint.Style.FILL);
        ds.setIncreasingColor(0xFF39FF6E);
        ds.setIncreasingPaintStyle(android.graphics.Paint.Style.FILL);
        ds.setShadowColorSameAsCandle(true);
        ds.setShadowWidth(0.8f);
        ds.setDrawValues(false);
        ds.setHighLightColor(0x8039FF6E);
        ds.setHighlightLineWidth(1f);

        chart.setData(new CandleData(ds));

        // Phiên dịch ngày/giờ cho trục X
        chart.getXAxis().setGranularity(1f);
        chart.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            private final SimpleDateFormat sdfDay = new SimpleDateFormat("dd/MM", Locale.getDefault());
            private final SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());

            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                if (idx >= 0 && idx < allKlines.size()) {
                    long timestamp = allKlines.get(idx).openTime * 1000L;
                    String tf = TF_CHART[currentTfIdx];
                    if (tf.equals("1d") || tf.equals("1w")) {
                        return sdfDay.format(new Date(timestamp));
                    } else {
                        return sdfTime.format(new Date(timestamp));
                    }
                }
                return "";
            }
        });

        chart.setVisibleXRangeMaximum(35);
        chart.getViewPortHandler().setMinMaxScaleY(1.1f, 100f);
        chart.moveViewToX(entries.size() - 1);
        chart.invalidate();
    }

    private void updateChartWithHistory(int newItemsAdded) {
        if (chart.getData() == null) return;
        float currentLowestVisibleX = chart.getLowestVisibleX();

        List<CandleEntry> entries = new ArrayList<>();
        for (int i = 0; i < allKlines.size(); i++) {
            Kline k = allKlines.get(i);
            entries.add(new CandleEntry(i, (float) k.high, (float) k.low, (float) k.open, (float) k.close));
        }

        CandleDataSet ds = (CandleDataSet) chart.getData().getDataSetByIndex(0);
        ds.setValues(entries);

        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.moveViewToX(currentLowestVisibleX + newItemsAdded);
    }

    // ══════════════════════════════════════════════════════════════
    //  PRICE, 24H STATS & REAL-TIME WEBSOCKET
    // ══════════════════════════════════════════════════════════════
    private void showPriceFromCache() {
        String lastPrice  = getIntent().getStringExtra("last_price");
        String lastChange = getIntent().getStringExtra("last_change");
        if (lastPrice != null && !lastPrice.isEmpty()) {
            tvPrice.setText(lastPrice);
            if (lastChange != null) {
                tvChange.setText(lastChange);
                tvChange.setTextColor(lastChange.contains("-") ? 0xFFFF4444 : 0xFF39FF6E);
            }
            return;
        }
        Map<String, CoinTicker> cached = viewModel.getTickers().getValue();
        if (cached != null) {
            CoinTicker t = cached.get(symbol);
            if (t != null) updatePriceUI(t);
        }
    }

    @Override
    public void onTickerUpdate(Map<String, CoinTicker> map) {
        CoinTicker t = map.get(symbol);
        if (t == null) return;

        runOnUiThread(() -> {
            updatePriceUI(t);

            // Cập nhật High, Low, Vol real-time
            if (tvHigh != null)   tvHigh.setText(formatPrice(t.getHigh()));
            if (tvLow != null)    tvLow.setText(formatPrice(t.getLow()));
            if (tvVolume != null) tvVolume.setText(formatVolume(t.getVolume()) + " " + t.getDisplaySymbol());
        });
    }

    private void updatePriceUI(CoinTicker t) {
        tvPrice.setText(formatPrice(t.getPrice()));
        double pct = t.getChangePct();
        tvChange.setText(String.format(Locale.US, "%+.2f%%", pct));
        tvChange.setTextColor(pct >= 0 ? 0xFF39FF6E : 0xFFFF4444);
    }

    private String formatPrice(double p) {
        if (p == 0) return "$0.00";
        if (p < 0.0001) return String.format(Locale.US, "$%.8f", p);
        if (p < 1.0)    return String.format(Locale.US, "$%.4f", p);
        if (p < 1000)   return String.format(Locale.US, "$%.2f", p);
        return String.format(Locale.US, "$%,.2f", p);
    }

    private String formatVolume(double v) {
        if (v >= 1_000_000_000) return String.format(Locale.US, "%.2fB", v / 1_000_000_000);
        if (v >= 1_000_000)     return String.format(Locale.US, "%.2fM", v / 1_000_000);
        if (v >= 1_000)         return String.format(Locale.US, "%.2fK", v / 1_000);
        return String.format(Locale.US, "%.2f", v);
    }

    @SuppressLint("SetTextI18n")
    private void showCandleInfo(Kline k) {
        if (candleInfoCard == null) return;
        candleInfoCard.setVisibility(View.VISIBLE);

        String tf  = TF_CHART[currentTfIdx];
        String fmt = (tf.equals("1d") || tf.equals("1w")) ? "dd/MM/yyyy" : "dd/MM HH:mm";
        SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.getDefault());
        String time = sdf.format(new Date(k.openTime * 1000L));

        boolean green = k.close >= k.open;
        int color = green ? 0xFF39FF6E : 0xFFFF4444;

        tvCandleTime.setText(time);
        tvCandleOpen.setText(formatPrice(k.open));
        tvCandleHigh.setText(formatPrice(k.high));   tvCandleHigh.setTextColor(0xFF39FF6E);
        tvCandleLow.setText(formatPrice(k.low));     tvCandleLow.setTextColor(0xFFFF4444);
        tvCandleClose.setText(formatPrice(k.close)); tvCandleClose.setTextColor(color);
        tvCandleVolume.setText(formatVolume(k.volume));
    }

    // ══════════════════════════════════════════════════════════════
    //  FAVORITE BUTTON
    // ══════════════════════════════════════════════════════════════
    private void setupFavoriteButton() {
        btnFavorite = findViewById(R.id.btn_favorite);
        updateFavoriteIcon(prefs.isFavorite(symbol));

        btnFavorite.setOnClickListener(v -> {
            prefs.toggleFavorite(symbol);
            boolean isNowFavorite = prefs.isFavorite(symbol);
            updateFavoriteIcon(isNowFavorite);

            if (isNowFavorite) {
                Toast.makeText(this, "Đã thêm " + symbol + " vào danh sách Yêu thích", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Đã xóa " + symbol + " khỏi danh sách Yêu thích", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFavoriteIcon(boolean isFav) {
        btnFavorite.setImageResource(isFav ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        btnFavorite.setColorFilter(isFav ? 0xFFFFD700 : 0xFF888888);
    }

    // ══════════════════════════════════════════════════════════════
    //  TIMEFRAME & SENTIMENT TABS
    // ══════════════════════════════════════════════════════════════
    private void setupTfButtons() {
        int[] ids = {R.id.btn_tf_1m, R.id.btn_tf_5m, R.id.btn_tf_15m, R.id.btn_tf_1h, R.id.btn_tf_1d, R.id.btn_tf_1w};
        for (int i = 0; i < ids.length; i++) {
            tfBtns[i] = findViewById(ids[i]);
            final int idx = i;
            if (tfBtns[i] != null) tfBtns[i].setOnClickListener(v -> selectTf(idx));
        }
        selectTf(3);
    }

    private void selectTf(int idx) {
        currentTfIdx = idx;
        for (int i = 0; i < tfBtns.length; i++) {
            if (tfBtns[i] == null) continue;
            tfBtns[i].setBackgroundResource(R.drawable.bg_timeframe_normal);
            if (tfBtns[i] instanceof TextView) ((TextView) tfBtns[i]).setTextColor(0xFF666666);
        }
        if (tfBtns[idx] != null) {
            tfBtns[idx].setBackgroundResource(R.drawable.bg_timeframe_selected);
            if (tfBtns[idx] instanceof TextView) ((TextView) tfBtns[idx]).setTextColor(0xFF39FF6E);
        }
        loadChartFresh(TF_CHART[idx]);
    }

    private void setupSentimentTabs() {
        int[] ids = {R.id.btn_sent_1h, R.id.btn_sent_1d, R.id.btn_sent_1w};
        for (int i = 0; i < ids.length; i++) {
            sentBtns[i] = findViewById(ids[i]);
            final int idx = i;
            if (sentBtns[i] != null) sentBtns[i].setOnClickListener(v -> selectSentimentTab(idx));
        }
        selectSentimentTab(0);
    }

    private void selectSentimentTab(int idx) {
        for (View b : sentBtns) {
            if (b == null) continue;
            b.setBackgroundResource(0);
            if (b instanceof TextView) ((TextView) b).setTextColor(0xFF666666);
        }
        if (sentBtns[idx] != null) {
            sentBtns[idx].setBackgroundResource(R.drawable.bg_timeframe_selected);
            if (sentBtns[idx] instanceof TextView) ((TextView) sentBtns[idx]).setTextColor(0xFF39FF6E);
        }
        fetchSentiment(SENT_TABS[idx]);
    }

    private void fetchSentiment(String sentTf) {
        if (tvSentiment == null) return;
        tvSentiment.setText("...");
        tvSentimentLabel.setText("ĐANG PHÂN TÍCH...");
        tvSentimentLabel.setBackgroundTintList(ColorStateList.valueOf(0xFF888888));
        clearIndicators();

        String backendUrl = prefs.getBackendUrl();
        if (backendUrl == null || backendUrl.isEmpty()) {
            tvSentiment.setText("--"); tvSentimentLabel.setText("CHƯA CẤU HÌNH URL"); return;
        }
        if (!backendUrl.endsWith("/")) backendUrl += "/";

        retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()
                .baseUrl(backendUrl)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build();
        retrofit.create(FastApiService.class)
                .getTrendAnalysis(symbol, sentTf)
                .enqueue(new retrofit2.Callback<TrendResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<TrendResponse> call, retrofit2.Response<TrendResponse> resp) {
                        if (!resp.isSuccessful() || resp.body() == null) {
                            runOnUiThread(() -> {
                                tvSentiment.setText("--");
                                tvSentimentLabel.setText("CHƯA ĐỦ DỮ LIỆU");
                                tvSentimentLabel.setBackgroundTintList(ColorStateList.valueOf(0xFF555555));
                                clearIndicators();
                            });
                            return;
                        }
                        runOnUiThread(() -> bindSentimentData(resp.body()));
                    }
                    @Override
                    public void onFailure(retrofit2.Call<TrendResponse> call, Throwable t) {
                        runOnUiThread(() -> {
                            tvSentiment.setText("--");
                            tvSentimentLabel.setText("MẤT KẾT NỐI");
                            tvSentimentLabel.setBackgroundTintList(ColorStateList.valueOf(0xFFFF4444));
                        });
                    }
                });
    }

    private void bindSentimentData(TrendResponse data) {
        if (data.analysis != null) {
            int score = (int) Math.round(data.analysis.score);
            String trend = data.analysis.trend != null ? data.analysis.trend : "";
            if (trend.contains("(")) trend = trend.substring(0, trend.indexOf("(")).trim();
            tvSentiment.setText(String.valueOf(score));
            tvSentimentLabel.setText(trend.toUpperCase());
            int color = score >= 60 ? 0xFF39FF6E : score <= 40 ? 0xFFFF4444 : 0xFFF3BA2F;
            tvSentimentLabel.setBackgroundTintList(ColorStateList.valueOf(color));
        }
        if (data.indicators != null) {
            boolean macdUp = data.indicators.macd > data.indicators.macd_signal;
            tvRsi.setText(String.format(Locale.US, "%.1f", data.indicators.rsi));
            tvMacd.setText(macdUp ? "Cắt lên (Tăng)" : "Cắt xuống (Giảm)");
            tvMacd.setTextColor(macdUp ? 0xFF39FF6E : 0xFFFF4444);
            tvEma20.setText(String.format(Locale.US, "$%,.2f", data.indicators.ema20));
            tvEma50.setText(String.format(Locale.US, "$%,.2f", data.indicators.ema50));
        }
        if (data.price != null)
            tvSupport.setText(String.format(Locale.US, "$%,.2f", data.price.support));
        if (data.market_sentiment != null)
            tvFearGreed.setText(data.market_sentiment.fear_greed_value + " (" + data.market_sentiment.fear_greed_label + ")");
    }

    private void clearIndicators() {
        if (tvRsi != null)      tvRsi.setText("--");
        if (tvMacd != null)     tvMacd.setText("--");
        if (tvEma20 != null)    tvEma20.setText("--");
        if (tvEma50 != null)    tvEma50.setText("--");
        if (tvSupport != null)  tvSupport.setText("--");
        if (tvFearGreed != null)tvFearGreed.setText("--");
    }

    @Override public void onConnected()    {}
    @Override public void onDisconnected() {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BinanceWebSocketManager.getInstance().removeListener(this);
    }

    // ── Retrofit models ───────────────────────────────────────────
    public static class TrendResponse {
        public AnalysisData  analysis;
        public IndicatorData indicators;
        public PriceData     price;
        public SentimentData market_sentiment;
    }
    public static class AnalysisData   { public double score; public String trend; }
    public static class IndicatorData  { public double rsi, macd, macd_signal, ema20, ema50; }
    public static class PriceData      { public double current, change_pct, support; }
    public static class SentimentData  { public int fear_greed_value; public String fear_greed_label; }

    interface FastApiService {
        @retrofit2.http.GET("/api/v1/analysis/trend/{symbol}")
        retrofit2.Call<TrendResponse> getTrendAnalysis(
                @retrofit2.http.Path("symbol") String symbol,
                @retrofit2.http.Query("tf")    String tf);
    }
}