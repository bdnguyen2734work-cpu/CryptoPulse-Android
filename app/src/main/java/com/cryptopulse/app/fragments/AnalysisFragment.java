package com.cryptopulse.app.fragments;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.cryptopulse.app.R;
import com.cryptopulse.app.models.CoinTicker;
import com.cryptopulse.app.network.BinanceWebSocketManager;
import com.cryptopulse.app.viewmodels.MarketViewModel;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import java.util.*;

public class AnalysisFragment extends Fragment implements BinanceWebSocketManager.TickerListener {

    private static final String[] SYMS = {
            "BTCUSDT","ETHUSDT","BNBUSDT","SOLUSDT","XRPUSDT",
            "ADAUSDT","DOGEUSDT","AVAXUSDT","DOTUSDT","LINKUSDT",
            "POLUSDT","UNIUSDT","ATOMUSDT","LTCUSDT","NEARUSDT",
            "APTUSDT","ARBUSDT","OPUSDT","INJUSDT","SUIUSDT",
            "TRXUSDT","SHIBUSDT","BCHUSDT","ICPUSDT"
    };

    private static final String[] DISPLAY_NAMES = {
            "Bitcoin (BTC)","Ethereum (ETH)","BNB","Solana (SOL)","XRP",
            "Cardano (ADA)","Dogecoin (DOGE)","Avalanche (AVAX)","Polkadot (DOT)","Chainlink (LINK)",
            "Polygon (POL)","Uniswap (UNI)","Cosmos (ATOM)","Litecoin (LTC)","NEAR Protocol (NEAR)",
            "Aptos (APT)","Arbitrum (ARB)","Optimism (OP)","Injective (INJ)","Sui (SUI)",
            "TRON (TRX)","Shiba Inu (SHIB)","Bitcoin Cash (BCH)","Internet Computer (ICP)"
    };

    private String currentSymbol = "BTCUSDT";
    private String currentTf = "1d";
    private MarketViewModel viewModel;

    private ImageView ivActiveLogo;
    private TextView tvActiveSymbol, tvPrice, tvChange;
    private TextView tvScore, tvLabel, tvAction, tvRiskLevel;
    private TextView tvVolatility, tvVolumeStat, tvFng;
    private TextView tvRsi, tvMacd, tvSupport, tvResistance, tvEma20, tvEma50;
    private View viewScoreBar;
    private LinearLayout containerSignals;
    private CombinedChart chart;
    private final TextView[] tfBtns = new TextView[3];

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_analysis, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        viewModel = new ViewModelProvider(requireActivity()).get(MarketViewModel.class);

        bindViews(v);
        setupChart();
        setupTfButtons(v);
        v.findViewById(R.id.btn_select_asset).setOnClickListener(b -> showCoinDialog());

        observeTrend();
        selectCoin(0);
        selectTf(1);
    }

    @Override public void onResume() {
        super.onResume();
        BinanceWebSocketManager.getInstance().addListener(this);
    }

    @Override public void onPause() {
        super.onPause();
        BinanceWebSocketManager.getInstance().removeListener(this);
    }

    private void bindViews(View v) {
        ivActiveLogo     = v.findViewById(R.id.iv_active_logo);
        tvActiveSymbol   = v.findViewById(R.id.tv_active_symbol);
        tvPrice          = v.findViewById(R.id.tv_analysis_price);
        tvChange         = v.findViewById(R.id.tv_analysis_change);
        tvScore          = v.findViewById(R.id.tv_sentiment_score);
        tvLabel          = v.findViewById(R.id.tv_sentiment_label);
        tvAction         = v.findViewById(R.id.tv_action);
        tvRiskLevel      = v.findViewById(R.id.tv_risk_level);
        tvVolatility     = v.findViewById(R.id.tv_volatility);
        tvVolumeStat     = v.findViewById(R.id.tv_volume_stat);
        tvFng            = v.findViewById(R.id.tv_fng_val);
        tvRsi            = v.findViewById(R.id.tv_rsi_val);
        tvMacd           = v.findViewById(R.id.tv_macd_val);
        tvSupport        = v.findViewById(R.id.tv_support_val);
        tvResistance     = v.findViewById(R.id.tv_resistance_val);
        tvEma20          = v.findViewById(R.id.tv_ema20);
        tvEma50          = v.findViewById(R.id.tv_ema50);
        viewScoreBar     = v.findViewById(R.id.view_score_bar);
        containerSignals = v.findViewById(R.id.container_signals);
        chart            = v.findViewById(R.id.analysis_chart);
    }

    private String translateFearGreed(String status) {
        if (status == null) return "Trung lập";
        switch (status.toLowerCase()) {
            case "extreme fear": return "Rất sợ hãi";
            case "fear": return "Sợ hãi";
            case "neutral": return "Trung lập";
            case "greed": return "Tham lam";
            case "extreme greed": return "Rất tham lam";
            default: return status;
        }
    }

    @SuppressWarnings("unchecked")
    private void observeTrend() {
        viewModel.getTrend().observe(getViewLifecycleOwner(), map -> {
            if (map == null || !isAdded()) return;
            try {
                String status = str(map, "status", "");
                if ("error".equals(status)) {
                    showError(str(map, "message", "Chưa đủ dữ liệu"));
                    return;
                }

                Map<String,Object> analysis   = (Map<String,Object>) map.get("analysis");
                Map<String,Object> indicators = (Map<String,Object>) map.get("indicators");
                Map<String,Object> price      = (Map<String,Object>) map.get("price");
                Map<String,Object> fng        = (Map<String,Object>) map.get("market_sentiment");
                List<?>            signals    = (List<?>) map.get("signals");

                int score = (int) Math.round(dbl(analysis, "score", 50));
                String trend = str(analysis, "trend", "--");
                String action = str(analysis, "action", "--");
                String risk = str(analysis, "risk_level", "--");
                String vola = str(analysis, "volatility", "--");

                if (trend.contains("(")) {
                    trend = trend.substring(0, trend.indexOf("(")).trim();
                }

                action = sanitizeAction(action);

                final String trendFinal = trend;
                final String actionFinal = action;
                final int scoreFinal = score;

                requireActivity().runOnUiThread(() -> {
                    tvScore.setText(String.valueOf(scoreFinal));
                    tvLabel.setText(trendFinal);
                    tvAction.setText(actionFinal);
                    tvRiskLevel.setText("RỦI RO: " + riskVietnamese(risk));
                    tvVolatility.setText(vola.isEmpty() ? "--" : volatilityVietnamese(vola));

                    int color = scoreFinal >= 60 ? 0xFF39FF6E : scoreFinal <= 40 ? 0xFFFF4444 : 0xFFF3BA2F;
                    tvScore.setTextColor(color);
                    tvLabel.setTextColor(color);
                    tvRiskLevel.setTextColor(color);

                    viewScoreBar.post(() -> {
                        int parentW = ((View) viewScoreBar.getParent()).getWidth();
                        ViewGroup.LayoutParams lp = viewScoreBar.getLayoutParams();
                        lp.width = (int)(parentW * scoreFinal / 100.0);
                        viewScoreBar.setLayoutParams(lp);
                        viewScoreBar.setBackgroundColor(color);
                    });
                });

                double rsi = dbl(indicators, "rsi", 0);
                double macd = dbl(indicators, "macd", 0);
                double macdSig = dbl(indicators, "macd_signal", 0);
                double ema20v = dbl(indicators, "ema20", 0);
                double ema50v = dbl(indicators, "ema50", 0);
                double volRatio = dbl(indicators, "volume_ratio", 1);
                double supp = dbl(price, "support", 0);
                double resist = dbl(price, "resistance", 0);
                int fngVal = (int) Math.round(dbl(fng, "fear_greed_value", 50));
                String fngLbl = str(fng, "fear_greed_label", "Neutral");

                requireActivity().runOnUiThread(() -> {
                    if (rsi > 0) {
                        tvRsi.setText(String.format(Locale.US, "%.1f", rsi));
                        tvRsi.setTextColor(rsi >= 70 ? 0xFFFF4444 : rsi <= 30 ? 0xFF39FF6E : 0xFFFFFFFF);
                    } else {
                        tvRsi.setText("--");
                    }

                    boolean macdUp = macd > macdSig;
                    tvMacd.setText(macdUp ? "↑ Tích cực" : "↓ Tiêu cực");
                    tvMacd.setTextColor(macdUp ? 0xFF39FF6E : 0xFFFF4444);

                    tvEma20.setText(ema20v > 0 ? fmt(ema20v) : "--");
                    tvEma50.setText(ema50v > 0 ? fmt(ema50v) : "--");

                    tvSupport.setText(supp > 0 ? fmt(supp) : "--");
                    tvResistance.setText(resist > 0 ? fmt(resist) : "--");

                    tvFng.setText(fngVal + " – " + translateFearGreed(fngLbl));
                    tvFng.setTextColor(fngVal >= 60 ? 0xFF39FF6E : fngVal <= 30 ? 0xFFFF4444 : 0xFFF3BA2F);

                    double pctVol = (volRatio - 1.0) * 100.0;
                    tvVolumeStat.setText(String.format(Locale.US, "%+.1f%%", pctVol));
                    tvVolumeStat.setTextColor(volRatio >= 1 ? 0xFF39FF6E : 0xFFFF4444);
                });

                if (signals != null) buildSignalCards(signals);

            } catch (Exception e) {
                showError("Lỗi xử lý dữ liệu");
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void buildSignalCards(List<?> signals) {
        if (containerSignals == null || !isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            containerSignals.removeAllViews();

            for (Object item : signals) {
                if (!(item instanceof Map)) continue;
                Map<String,Object> sig = (Map<String,Object>) item;
                String indicator = str(sig, "indicator", "");
                String signal = str(sig, "signal", "");
                String note = str(sig, "note", "");

                int bgColor, textColor;
                String icon, badge;

                switch (signal) {
                    case "BUY":
                        bgColor = 0x1A39FF6E; textColor = 0xFF39FF6E;
                        icon = "↑"; badge = "TĂNG";
                        break;
                    case "SELL":
                        bgColor = 0x1AFF4444; textColor = 0xFFFF4444;
                        icon = "↓"; badge = "GIẢM";
                        break;
                    case "STRONG":
                        bgColor = 0x1A00BFFF; textColor = 0xFF00BFFF;
                        icon = "⚡"; badge = "MẠNH";
                        break;
                    case "WEAK":
                        bgColor = 0x1AFFA500; textColor = 0xFFFFA500;
                        icon = "⚠"; badge = "YẾU";
                        break;
                    case "NORMAL":
                        bgColor = 0x1A888888; textColor = 0xFF888888;
                        icon = "–"; badge = "BÌNH THƯỜNG";
                        break;
                    default:
                        bgColor = 0x1AFFFFFF; textColor = 0xFF888888;
                        icon = "–"; badge = "TRUNG TÍNH";
                        break;
                }

                LinearLayout card = new LinearLayout(requireContext());
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setGravity(Gravity.CENTER_VERTICAL);
                card.setBackground(roundedBg(bgColor, 12));
                card.setPadding(dp(14), dp(12), dp(14), dp(12));
                LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                cardLp.setMargins(0, 0, 0, dp(8));
                card.setLayoutParams(cardLp);

                TextView tvIcon = new TextView(requireContext());
                tvIcon.setText(icon);
                tvIcon.setTextSize(14);
                tvIcon.setTextColor(textColor);
                tvIcon.setTypeface(null, Typeface.BOLD);
                tvIcon.setGravity(Gravity.CENTER);
                tvIcon.setBackground(roundedBg(bgColor, 8));
                LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(28), dp(28));
                iconLp.setMarginEnd(dp(12));
                tvIcon.setLayoutParams(iconLp);
                card.addView(tvIcon);

                LinearLayout col = new LinearLayout(requireContext());
                col.setOrientation(LinearLayout.VERTICAL);
                col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                TextView tvInd = new TextView(requireContext());
                tvInd.setText(indicator);
                tvInd.setTextSize(13);
                tvInd.setTextColor(0xFFFFFFFF);
                tvInd.setTypeface(null, Typeface.BOLD);
                col.addView(tvInd);

                TextView tvNote = new TextView(requireContext());
                tvNote.setText(note);
                tvNote.setTextSize(11);
                tvNote.setTextColor(0xFF888888);
                tvNote.setMaxLines(2);
                col.addView(tvNote);

                card.addView(col);

                TextView tvBadge = new TextView(requireContext());
                tvBadge.setText(badge);
                tvBadge.setTextSize(10);
                tvBadge.setTextColor(textColor);
                tvBadge.setTypeface(null, Typeface.BOLD);
                tvBadge.setPadding(dp(8), dp(4), dp(8), dp(4));
                tvBadge.setBackground(roundedBg(bgColor, 6));
                card.addView(tvBadge);

                containerSignals.addView(card);
            }

            LinearLayout disc = new LinearLayout(requireContext());
            disc.setOrientation(LinearLayout.HORIZONTAL);
            disc.setGravity(Gravity.CENTER_VERTICAL);
            disc.setBackground(roundedBg(0x0DFFFFFF, 10));
            disc.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams discLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            discLp.setMargins(0, dp(4), 0, 0);
            disc.setLayoutParams(discLp);

            TextView tvWarn = new TextView(requireContext());
            tvWarn.setText("⚠");
            tvWarn.setTextSize(13);
            tvWarn.setTextColor(0xFFFFA500);
            LinearLayout.LayoutParams warnLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            warnLp.setMarginEnd(dp(8));
            tvWarn.setLayoutParams(warnLp);
            disc.addView(tvWarn);

            TextView tvDisc = new TextView(requireContext());
            tvDisc.setText(
                    "Dữ liệu phân tích kỹ thuật chỉ mang tính tham khảo. Không phải lời khuyên đầu tư. "
                            + "Mọi quyết định tài chính là trách nhiệm của người dùng."
            );
            tvDisc.setTextSize(12);
            tvDisc.setTextColor(0xFF666666);
            tvDisc.setLineSpacing(0, 1.4f);
            tvDisc.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            disc.addView(tvDisc);

            containerSignals.addView(disc);
        });
    }

    private void showCoinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xFF121212);
        layout.setPadding(dp(20), dp(20), dp(20), dp(20));

        EditText etSearch = new EditText(requireContext());
        etSearch.setHint("Tìm kiếm coin...");
        etSearch.setTextColor(0xFFFFFFFF);
        etSearch.setHintTextColor(0xFF555555);
        etSearch.setBackground(roundedBg(0xFF1E1E1E, 10));
        etSearch.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        searchLp.setMargins(0, 0, 0, dp(12));
        etSearch.setLayoutParams(searchLp);
        layout.addView(etSearch);

        ListView lv = new ListView(requireContext());
        lv.setDividerHeight(0);
        lv.setBackgroundColor(0xFF121212);
        layout.addView(lv);

        builder.setView(layout);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xFF121212));
        }

        List<String> allNames = new ArrayList<>(Arrays.asList(DISPLAY_NAMES));
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(), 0, allNames) {
            @NonNull @Override
            public View getView(int pos, View cv, @NonNull ViewGroup parent) {
                LinearLayout row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(16), dp(12), dp(16), dp(12));
                row.setBackgroundColor(0xFF121212);

                ImageView iv = new ImageView(getContext());
                LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(dp(36), dp(36));
                ivLp.setMarginEnd(dp(14));
                iv.setLayoutParams(ivLp);
                String name = getItem(pos);
                int origIdx = Arrays.asList(DISPLAY_NAMES).indexOf(name);
                if (origIdx >= 0) {
                    CoinTicker tmp = new CoinTicker();
                    tmp.symbol = SYMS[origIdx];
                    Glide.with(getContext())
                            .load(tmp.getLogoUrl())
                            .transform(new CircleCrop())
                            .placeholder(R.drawable.ic_coin_placeholder)
                            .into(iv);
                }
                row.addView(iv);

                TextView tv = new TextView(getContext());
                tv.setText(name);
                tv.setTextColor(0xFFFFFFFF);
                tv.setTextSize(14);
                tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(tv);

                return row;
            }
        };
        lv.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                adapter.getFilter().filter(s);
            }
        });

        lv.setOnItemClickListener((p, vv, pos, id) -> {
            String selected = (String) adapter.getItem(pos);
            int idx = Arrays.asList(DISPLAY_NAMES).indexOf(selected);
            if (idx >= 0) selectCoin(idx);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void selectCoin(int idx) {
        currentSymbol = SYMS[idx];
        String name = DISPLAY_NAMES[idx];
        String shortSym;
        if (name.contains("(") && name.contains(")")) {
            shortSym = name.substring(name.lastIndexOf("(") + 1, name.lastIndexOf(")"));
        } else {
            shortSym = currentSymbol.replace("USDT", "");
        }
        tvActiveSymbol.setText(shortSym);

        CoinTicker tmp = new CoinTicker();
        tmp.symbol = currentSymbol;
        Glide.with(this)
                .load(tmp.getLogoUrl())
                .transform(new CircleCrop())
                .placeholder(R.drawable.ic_coin_placeholder)
                .into(ivActiveLogo);

        resetUI();
        generateDemoChart(idx + 1);
        viewModel.loadMarketTrend(currentSymbol, currentTf);
    }

    private void setupTfButtons(View v) {
        int[] ids = {R.id.btn_tf_1h, R.id.btn_tf_1d, R.id.btn_tf_1w};
        for (int i = 0; i < ids.length; i++) {
            final int idx = i;
            tfBtns[i] = v.findViewById(ids[i]);
            if (tfBtns[i] != null) {
                tfBtns[i].setOnClickListener(b -> selectTf(idx));
            }
        }
    }

    private void selectTf(int idx) {
        for (int i = 0; i < tfBtns.length; i++) {
            if (tfBtns[i] == null) continue;
            tfBtns[i].setBackgroundResource(i == idx ? R.drawable.bg_timeframe_selected : R.drawable.bg_timeframe_normal);
            tfBtns[i].setTextColor(i == idx ? 0xFF39FF6E : 0xFF666666);
        }
        currentTf = idx == 0 ? "h1" : idx == 1 ? "1d" : "1w";
        resetUI();
        viewModel.loadMarketTrend(currentSymbol, currentTf);
    }

    private void setupChart() {
        if (chart == null) return;
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleXEnabled(true);
        chart.setScaleYEnabled(false);
        chart.setDrawOrder(new CombinedChart.DrawOrder[]{CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.LINE});
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setTextColor(0xFF555555);
        chart.getAxisLeft().setGridColor(0x20FFFFFF);
        chart.getAxisLeft().setTextColor(0xFF555555);
        chart.getAxisRight().setEnabled(false);
    }

    private void generateDemoChart(int seed) {
        if (chart == null) return;
        double base = currentSymbol.contains("BTC") ? 65000 : currentSymbol.contains("ETH") ? 2500 : 100;
        List<BarEntry> bars = new ArrayList<>();
        List<Entry> lines = new ArrayList<>();
        List<Integer> cols = new ArrayList<>();
        Random rnd = new Random(seed * 100L);
        double val = base;

        for (int i = 0; i < 80; i++) {
            val += (rnd.nextDouble() - 0.45) * base * 0.02;
            bars.add(new BarEntry(i, (float) val));
            lines.add(new Entry(i, (float)(val * (0.99 + rnd.nextDouble() * 0.02))));
            cols.add(val >= base ? 0xFF39FF6E : 0xFF2A2A2A);
        }

        CombinedData data = new CombinedData();
        BarDataSet bds = new BarDataSet(bars, "");
        bds.setColors(cols);
        bds.setDrawValues(false);
        data.setData(new BarData(bds));

        LineDataSet lds = new LineDataSet(lines, "");
        lds.setColor(0xFF00E5FF);
        lds.setLineWidth(1.5f);
        lds.setDrawCircles(false);
        lds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lds.setDrawValues(false);
        data.setData(new LineData(lds));

        chart.post(() -> {
            chart.setData(data);
            chart.setVisibleXRangeMaximum(30);
            chart.moveViewToX(bars.size() - 1);
            chart.animateY(500);
            chart.invalidate();
        });
    }

    @Override
    public void onTickerUpdate(Map<String, CoinTicker> map) {
        if (!isAdded() || tvPrice == null) return;
        CoinTicker t = map.get(currentSymbol);
        if (t == null) return;
        requireActivity().runOnUiThread(() -> {
            tvPrice.setText(fmt(t.getPrice()));
            double pct = t.getChangePct();
            tvChange.setText(String.format(Locale.US, "%+.2f%%", pct));
            tvChange.setTextColor(pct >= 0 ? 0xFF39FF6E : 0xFFFF4444);
        });
    }

    @Override public void onConnected() {}
    @Override public void onDisconnected() {}

    private void resetUI() {
        if (tvScore == null || !isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            tvScore.setText("--"); tvScore.setTextColor(0xFFAAAAAA);
            tvLabel.setText("ĐANG TẢI..."); tvLabel.setTextColor(0xFFAAAAAA);
            tvAction.setText("--");
            tvRiskLevel.setText("RỦI RO: --");
            tvRsi.setText("--"); tvMacd.setText("--");
            tvEma20.setText("--"); tvEma50.setText("--");
            tvSupport.setText("--"); tvResistance.setText("--");
            tvFng.setText("--"); tvVolumeStat.setText("--");
            tvVolatility.setText("--");
            if (containerSignals != null) containerSignals.removeAllViews();
            if (viewScoreBar != null) {
                ViewGroup.LayoutParams lp = viewScoreBar.getLayoutParams();
                lp.width = 0;
                viewScoreBar.setLayoutParams(lp);
            }
        });
    }

    private void showError(String msg) {
        if (!isAdded() || tvLabel == null) return;
        requireActivity().runOnUiThread(() -> {
            tvScore.setText("--");
            tvLabel.setText(msg.toUpperCase());
            tvLabel.setTextColor(0xFF888888);
        });
    }

    private String sanitizeAction(String raw) {
        if (raw == null) return "--";
        raw = raw.replaceAll("(?i)NÊN MUA|CÓ THỂ MUA|NÊN BÁN|TRANH MUA", "");
        raw = raw.replaceAll("(?i)NEN MUA|CO THE MUA|NEN BAN", "");
        if (raw.contains("–")) raw = raw.substring(raw.indexOf("–") + 1).trim();
        else if (raw.contains("-")) raw = raw.substring(raw.indexOf("-") + 1).trim();
        return raw.trim().isEmpty() ? "--" : raw.trim();
    }

    private String riskVietnamese(String risk) {
        if (risk == null) return "--";
        switch (risk.toLowerCase()) {
            case "thấp": case "thap": return "THẤP";
            case "trung bình": case "trung binh": return "TRUNG BÌNH";
            case "cao": return "CAO";
            case "rất cao": case "rat cao": return "RẤT CAO";
            default: return risk.toUpperCase();
        }
    }

    private String volatilityVietnamese(String v) {
        if (v == null) return "--";
        switch (v.toLowerCase()) {
            case "thấp": case "thap": return "Thấp";
            case "trung bình": case "trung binh": return "Trung bình";
            case "cao": return "Cao";
            case "rất cao": case "rat cao": return "Rất cao";
            default: return v;
        }
    }

    private String fmt(double p) {
        if (p <= 0) return "--";
        if (p < 0.00001) return String.format(Locale.US, "$%.8f", p);
        if (p < 0.01) return String.format(Locale.US, "$%.6f", p);
        if (p < 1) return String.format(Locale.US, "$%.4f", p);
        if (p < 10_000) return String.format(Locale.US, "$%.2f", p);
        return String.format(Locale.US, "$%,.2f", p);
    }

    private double dbl(Map<String,Object> m, String k, double def) {
        try {
            return (m != null && m.get(k) != null) ? Double.parseDouble(m.get(k).toString()) : def;
        } catch (Exception e) { return def; }
    }

    private String str(Map<String,Object> m, String k, String def) {
        return (m != null && m.get(k) != null) ? m.get(k).toString() : def;
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable roundedBg(int color, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(dp(radiusDp));
        return gd;
    }
}