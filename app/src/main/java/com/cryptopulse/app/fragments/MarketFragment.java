package com.cryptopulse.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import com.cryptopulse.app.R;
import com.cryptopulse.app.activities.CoinDetailActivity;
import com.cryptopulse.app.adapters.MarketAdapter;
import com.cryptopulse.app.models.CoinTicker;
import com.cryptopulse.app.network.ApiClient;
import com.cryptopulse.app.network.BinanceWebSocketManager;
import com.cryptopulse.app.viewmodels.MarketViewModel;
import java.util.*;

public class MarketFragment extends Fragment implements BinanceWebSocketManager.TickerListener {

    private static final String[] WATCH = {
            "BTCUSDT","ETHUSDT","BNBUSDT","SOLUSDT","XRPUSDT",
            "ADAUSDT","DOGEUSDT","AVAXUSDT","DOTUSDT","LINKUSDT",
            "POLUSDT","UNIUSDT","ATOMUSDT","LTCUSDT","NEARUSDT",
            "APTUSDT","ARBUSDT","OPUSDT","INJUSDT","SUIUSDT",
            "TRXUSDT","SHIBUSDT","BCHUSDT","ICPUSDT"
    };

    private MarketAdapter adapter;
    private MarketViewModel viewModel;
    private final Map<String, CoinTicker> tickerCache = new LinkedHashMap<>();

    private TextView btnSortPrice, btnSortChange, btnSortVolume;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_market, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        viewModel = new ViewModelProvider(requireActivity()).get(MarketViewModel.class);

        setupRecyclerView(v);
        setupSearch(v);
        setupSortButtons(v);

        for (CoinTicker t : MarketViewModel.getDemoTickers()) {
            tickerCache.put(t.symbol, t);
        }
        refreshList();
        observeData();

        if (ApiClient.get() != null) viewModel.loadTopCoins();
    }

    private void setupRecyclerView(View v) {
        RecyclerView rv = v.findViewById(R.id.rv_market);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setItemAnimator(null);
        adapter = new MarketAdapter(new ArrayList<>(), ticker -> {
            Intent i = new Intent(requireContext(), CoinDetailActivity.class);
            i.putExtra("symbol", ticker.symbol);
            i.putExtra("last_price", ticker.getPrice() >= 1000
                    ? String.format(Locale.US, "$%,.2f", ticker.getPrice())
                    : String.format(Locale.US, "$%.4f",  ticker.getPrice()));
            i.putExtra("last_change", String.format(Locale.US, "%+.2f%%", ticker.getChangePct()));
            startActivity(i);
        });
        rv.setAdapter(adapter);
    }

    private void setupSearch(View v) {
        EditText et = v.findViewById(R.id.et_search_market);
        et.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable e) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                adapter.filter(s.toString());
            }
        });
    }

    private void setupSortButtons(View v) {
        btnSortPrice  = v.findViewById(R.id.btn_sort_price);
        btnSortChange = v.findViewById(R.id.btn_sort_change);
        btnSortVolume = v.findViewById(R.id.btn_sort_volume);

        btnSortPrice.setOnClickListener(b  -> { adapter.sort(MarketAdapter.SortBy.PRICE);  updateSortUI(); });
        btnSortChange.setOnClickListener(b -> { adapter.sort(MarketAdapter.SortBy.CHANGE); updateSortUI(); });
        btnSortVolume.setOnClickListener(b -> { adapter.sort(MarketAdapter.SortBy.VOLUME); updateSortUI(); });
    }

    private void updateSortUI() {
        MarketAdapter.SortBy sort = adapter.getCurrentSort();
        MarketAdapter.SortState state = adapter.getCurrentSortState();

        // Đặt lại mặc định
        btnSortPrice.setText("GIÁ ↕");
        btnSortPrice.setBackgroundResource(R.drawable.bg_timeframe_normal);
        btnSortPrice.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

        btnSortChange.setText("24H ↕");
        btnSortChange.setBackgroundResource(R.drawable.bg_timeframe_normal);
        btnSortChange.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

        btnSortVolume.setText("KHỐI LƯỢNG GD ↕");
        btnSortVolume.setBackgroundResource(R.drawable.bg_timeframe_normal);
        btnSortVolume.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

        if (sort == null || state == MarketAdapter.SortState.NONE) return;

        TextView active = null;
        String prefix = "";

        if (sort == MarketAdapter.SortBy.PRICE)  { active = btnSortPrice;  prefix = "GIÁ"; }
        if (sort == MarketAdapter.SortBy.CHANGE) { active = btnSortChange; prefix = "24H"; }
        if (sort == MarketAdapter.SortBy.VOLUME) { active = btnSortVolume; prefix = "KHỐI LƯỢNG"; }

        if (active != null) {
            active.setBackgroundResource(R.drawable.bg_timeframe_selected);
            active.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_green));
            String arrow = state == MarketAdapter.SortState.DESCENDING ? " ↓" : state == MarketAdapter.SortState.ASCENDING  ? " ↑" : " ↕";
            active.setText(prefix + arrow);
        }
    }

    private void observeData() {
        viewModel.getTickers().observe(getViewLifecycleOwner(), map -> {
            if (map == null) return;
            tickerCache.putAll(map);
            refreshList();
        });

        viewModel.getTopCoins().observe(getViewLifecycleOwner(), data -> {
            if (data == null) return;
            try {
                Object dataObj = data.get("data");
                if (!(dataObj instanceof List)) return;
                for (Object item : (List<?>) dataObj) {
                    if (!(item instanceof Map)) continue;
                    Map<?, ?> coin = (Map<?, ?>) item;
                    String sym = String.valueOf(coin.get("symbol"));

                    CoinTicker t = tickerCache.containsKey(sym) ? tickerCache.get(sym) : new CoinTicker();
                    t.symbol = sym;

                    if (coin.get("lastPrice") != null) t.close = String.valueOf(coin.get("lastPrice"));
                    if (coin.get("priceChangePercent") != null) t.changePercent = String.valueOf(coin.get("priceChangePercent"));
                    if (coin.get("volume") != null) t.volume = String.valueOf(coin.get("volume"));
                    if (coin.get("quoteVolume") != null) t.quoteVolume = String.valueOf(coin.get("quoteVolume"));
                    if (coin.get("highPrice") != null) t.high = String.valueOf(coin.get("highPrice"));
                    if (coin.get("lowPrice") != null) t.low = String.valueOf(coin.get("lowPrice"));

                    tickerCache.put(sym, t);
                }
                requireActivity().runOnUiThread(this::refreshList);
            } catch (Exception ignored) {}
        });
    }

    private void refreshList() {
        if (!isAdded()) return;
        List<CoinTicker> list = new ArrayList<>();
        for (String sym : WATCH) {
            CoinTicker t = tickerCache.get(sym);
            if (t != null) list.add(t);
        }
        adapter.updateData(list);
    }

    @Override public void onResume() {
        super.onResume();
        BinanceWebSocketManager.getInstance().addListener(this);
        if (ApiClient.get() != null) viewModel.loadTopCoins();
    }

    @Override public void onPause() {
        super.onPause();
        BinanceWebSocketManager.getInstance().removeListener(this);
    }

    @Override
    public void onTickerUpdate(Map<String, CoinTicker> map) {
        if (!isAdded()) return;
        boolean changed = false;
        for (String sym : WATCH) {
            if (map.containsKey(sym)) {
                CoinTicker incoming = map.get(sym);
                CoinTicker existing = tickerCache.get(sym);
                if (existing != null && (incoming.quoteVolume == null || incoming.quoteVolume.isEmpty()) && existing.quoteVolume != null) {
                    incoming.quoteVolume = existing.quoteVolume;
                }
                tickerCache.put(sym, incoming);
                changed = true;
            }
        }
        if (changed) requireActivity().runOnUiThread(this::refreshList);
    }

    @Override public void onConnected() {}
    @Override public void onDisconnected() {}
}