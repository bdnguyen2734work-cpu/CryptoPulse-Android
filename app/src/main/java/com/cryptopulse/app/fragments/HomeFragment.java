package com.cryptopulse.app.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.annotation.*;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import com.cryptopulse.app.R;
import com.cryptopulse.app.activities.CoinDetailActivity;
import com.cryptopulse.app.activities.LoginActivity;
import com.cryptopulse.app.adapters.CoinListAdapter;
import com.cryptopulse.app.adapters.HotAssetAdapter;
import com.cryptopulse.app.adapters.SearchAdapter;
import com.cryptopulse.app.models.CoinTicker;
import com.cryptopulse.app.network.ApiClient;
import com.cryptopulse.app.network.BinanceWebSocketManager;
import com.cryptopulse.app.utils.AnimUtils;
import com.cryptopulse.app.utils.AppPrefs;
import com.cryptopulse.app.views.FearGreedGaugeView;
import com.cryptopulse.app.viewmodels.MarketViewModel;
import java.util.*;

public class HomeFragment extends Fragment implements BinanceWebSocketManager.TickerListener {

    private static final String[] WATCH = {
            "BTCUSDT","ETHUSDT","BNBUSDT","SOLUSDT","XRPUSDT",
            "ADAUSDT","DOGEUSDT","AVAXUSDT","DOTUSDT","LINKUSDT",
            "MATICUSDT","UNIUSDT","ATOMUSDT","LTCUSDT","NEARUSDT",
            "APTUSDT","ARBUSDT","OPUSDT","INJUSDT","SUIUSDT",
            "TRXUSDT","SHIBUSDT","BCHUSDT","ICPUSDT"
    };

    private MarketViewModel viewModel;
    private CoinListAdapter adapter;
    private SearchAdapter searchAdapter;
    private FearGreedGaugeView gaugeView;
    private EditText etSearch;
    private CardView cardSearchResults;
    private RecyclerView rvHotAssets;
    private HotAssetAdapter hotAssetAdapter;

    private ImageView btnProfile;
    private TextView btnLoginHome;

    private final Map<String, CoinTicker> tickerCache = new LinkedHashMap<>();
    private boolean isShowingFavs = false;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_home, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        viewModel = new ViewModelProvider(requireActivity()).get(MarketViewModel.class);

        bindViews(v);
        setupSearch(v);
        setupCoinList(v);
        setupTabs(v);
        setupHotCard(v);
        seedCache();
        observeViewModel();
        loadBackendData();
        runAnimations(v);
        updateAuthUI();
    }

    private void bindViews(View v) {
        gaugeView = v.findViewById(R.id.gauge_fear_greed);
        if (gaugeView != null) gaugeView.setValue(50, "Trung lập");

        btnProfile = v.findViewById(R.id.btn_profile);
        btnLoginHome = v.findViewById(R.id.btn_login_home);

        btnProfile.setOnClickListener(btn -> {
            ProfileBottomSheet.newInstance().show(getParentFragmentManager(), "profile");
        });

        btnLoginHome.setOnClickListener(btn -> {
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
        });
    }

    private void setupSearch(View v) {
        etSearch = v.findViewById(R.id.et_search);
        cardSearchResults = v.findViewById(R.id.card_search_results);
        RecyclerView rvSearch = v.findViewById(R.id.rv_search_results);
        rvSearch.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSearch.setItemAnimator(null);
        searchAdapter = new SearchAdapter(ticker -> {
            hideSearch();
            openDetail(ticker.symbol);
        });
        rvSearch.setAdapter(searchAdapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSearch(s.toString().trim());
            }
        });

        etSearch.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) hideSearch();
        });
    }

    private void setupCoinList(View v) {
        RecyclerView rv = v.findViewById(R.id.rv_coins);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setHasFixedSize(false);
        rv.setItemAnimator(null);
        adapter = new CoinListAdapter(new ArrayList<>(), ticker -> openDetail(ticker.symbol));
        rv.setAdapter(adapter);
    }

    private void setupTabs(View v) {
        TextView tabPop = v.findViewById(R.id.tab_popular);
        TextView tabFav = v.findViewById(R.id.tab_favorites);

        tabPop.setOnClickListener(btn -> {
            isShowingFavs = false;
            tabPop.setTextColor(0xFF39FF6E);
            tabFav.setTextColor(0xFF555555);
            refreshList();
        });

        tabFav.setOnClickListener(btn -> {
            isShowingFavs = true;
            tabFav.setTextColor(0xFF39FF6E);
            tabPop.setTextColor(0xFF555555);
            refreshList();
        });
    }

    private void setupHotCard(View v) {
        rvHotAssets = v.findViewById(R.id.rv_hot_assets);
        if (rvHotAssets == null) return;
        rvHotAssets.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        new PagerSnapHelper().attachToRecyclerView(rvHotAssets);
        hotAssetAdapter = new HotAssetAdapter(ticker -> openDetail(ticker.symbol));
        rvHotAssets.setAdapter(hotAssetAdapter);
    }

    private void seedCache() {
        for (CoinTicker t : MarketViewModel.getDemoTickers()) {
            tickerCache.put(t.symbol, t);
        }
        refreshList();
        refreshHotAssets();
    }

    private void runAnimations(View v) {
        AnimUtils.slideUp(v.findViewById(R.id.header_layout), 500, 0);
        AnimUtils.slideUp(v.findViewById(R.id.card_sentiment), 500, 100);
        AnimUtils.slideUp(v.findViewById(R.id.rv_hot_assets), 500, 200);
        AnimUtils.slideUp(v.findViewById(R.id.section_list), 500, 300);
    }

    private void filterSearch(String query) {
        if (query.isEmpty()) {
            hideSearch();
            return;
        }
        String q = query.toUpperCase();
        List<CoinTicker> results = new ArrayList<>();
        for (String sym : WATCH) {
            CoinTicker t = tickerCache.get(sym);
            if (t == null) continue;
            if (t.getDisplaySymbol().toUpperCase().startsWith(q) || t.getCoinName().toUpperCase().startsWith(q)) {
                results.add(t);
            }
        }
        if (results.isEmpty()) {
            hideSearch();
        } else {
            searchAdapter.updateData(results.subList(0, Math.min(results.size(), 8)));
            if (cardSearchResults != null) cardSearchResults.setVisibility(View.VISIBLE);
        }
    }

    private void hideSearch() {
        if (cardSearchResults != null) cardSearchResults.setVisibility(View.GONE);
        if (etSearch != null) {
            etSearch.clearFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }
    }

    private void loadBackendData() {
        if (ApiClient.get() == null) return;
        viewModel.loadFearGreed();
        viewModel.loadTopCoins();
    }

    private String translateFearGreed(String status) {
        if (status == null) return "Trung lập";
        switch (status.toLowerCase()) {
            case "extreme fear": return "Sợ hãi tột độ";
            case "fear": return "Sợ hãi";
            case "neutral": return "Trung lập";
            case "greed": return "Tham lam";
            case "extreme greed": return "Tham lam tột độ";
            default: return status;
        }
    }

    private void observeViewModel() {
        viewModel.getLoginState().observe(getViewLifecycleOwner(), isLoggedIn -> {
            updateAuthUI();
        });

        viewModel.getProfileUpdated().observe(getViewLifecycleOwner(), updated -> {
            updateAuthUI();
        });

        viewModel.getFearGreed().observe(getViewLifecycleOwner(), data -> {
            if (data == null || gaugeView == null) return;
            try {
                Object dataObj = data.get("data");
                if (!(dataObj instanceof Map)) return;
                Map<?, ?> d = (Map<?, ?>) dataObj;
                Object val = d.get("value");
                Object lbl = d.get("classification");
                if (val == null) return;
                int intVal = (val instanceof Double) ? ((Double) val).intValue() : Integer.parseInt(val.toString());
                String translatedStatus = translateFearGreed(lbl != null ? lbl.toString() : "Neutral");
                requireActivity().runOnUiThread(() -> gaugeView.setValue(intVal, translatedStatus));
            } catch (Exception ignored) {}
        });

        viewModel.getTopCoins().observe(getViewLifecycleOwner(), data -> {
            if (data == null) return;
            try {
                Object dataObj = data.get("data");
                if (!(dataObj instanceof List)) return;
                for (Object item : (List<?>) dataObj) {
                    if (!(item instanceof Map)) continue;
                    Map<?, ?> coin = (Map<?, ?>) item;
                    String symbol = String.valueOf(coin.get("symbol"));
                    CoinTicker t = tickerCache.containsKey(symbol) ? tickerCache.get(symbol) : new CoinTicker();
                    t.symbol = symbol;
                    if (coin.get("lastPrice") != null) t.close = String.valueOf(coin.get("lastPrice"));
                    if (coin.get("priceChangePercent") != null) t.changePercent = String.valueOf(coin.get("priceChangePercent"));
                    tickerCache.put(symbol, t);
                }
                requireActivity().runOnUiThread(() -> { refreshList(); refreshHotAssets(); });
            } catch (Exception ignored) {}
        });

        viewModel.getTickers().observe(getViewLifecycleOwner(), map -> {
            if (map == null || map.isEmpty()) return;
            for (Map.Entry<String, CoinTicker> entry : map.entrySet()) {
                mergeTicker(entry.getKey(), entry.getValue());
            }
            refreshList();
            refreshHotAssets();
        });
    }

    private void mergeTicker(String symbol, CoinTicker live) {
        if (tickerCache.containsKey(symbol)) {
            CoinTicker old = tickerCache.get(symbol);
            if (live.close != null) old.close = live.close;
            if (live.changePercent != null) old.changePercent = live.changePercent;
            tickerCache.put(symbol, old);
        } else {
            tickerCache.put(symbol, live);
        }
    }

    private void refreshList() {
        if (!isAdded()) return;
        List<CoinTicker> list = new ArrayList<>();
        if (isShowingFavs) {
            Set<String> favs = AppPrefs.get().getFavorites();
            for (String sym : WATCH) {
                if (favs.contains(sym)) {
                    CoinTicker t = tickerCache.get(sym);
                    if (t != null) list.add(t);
                }
            }
        } else {
            for (String sym : WATCH) {
                CoinTicker t = tickerCache.get(sym);
                if (t != null) list.add(t);
            }
        }
        adapter.updateData(list);
    }

    private void refreshHotAssets() {
        if (!isAdded() || hotAssetAdapter == null) return;
        List<CoinTicker> hotList = new ArrayList<>();
        for (String sym : new String[]{"BTCUSDT","ETHUSDT","BNBUSDT","SOLUSDT"}) {
            CoinTicker t = tickerCache.get(sym);
            if (t != null) hotList.add(t);
        }
        hotAssetAdapter.updateData(hotList);
    }

    private void openDetail(String symbol) {
        Intent i = new Intent(requireContext(), CoinDetailActivity.class);
        i.putExtra("symbol", symbol);
        startActivity(i);
    }

    private void updateAuthUI() {
        if (!isAdded() || btnProfile == null || btnLoginHome == null) return;
        AppPrefs prefs = AppPrefs.get();
        if (prefs.isLoggedIn()) {
            btnProfile.setVisibility(View.VISIBLE);
            btnLoginHome.setVisibility(View.GONE);
            String savedAvatar = prefs.getUserAvatar();
            if (savedAvatar != null && !savedAvatar.isEmpty()) {
                final String finalUrl = savedAvatar.startsWith("/static")
                        ? prefs.getBackendUrl() + savedAvatar : savedAvatar;
                new Thread(() -> {
                    try {
                        java.net.URL url = new java.net.URL(finalUrl);
                        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(url.openConnection().getInputStream());
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (bitmap != null) applyCircularAvatar(bitmap);
                            });
                        }
                    } catch (Exception e) {
                        if (getActivity() != null) getActivity().runOnUiThread(this::resetToDefaultAvatar);
                    }
                }).start();
            } else { resetToDefaultAvatar(); }
        } else {
            btnProfile.setVisibility(View.GONE);
            btnLoginHome.setVisibility(View.VISIBLE);
        }
    }

    private void applyCircularAvatar(android.graphics.Bitmap bitmap) {
        androidx.core.graphics.drawable.RoundedBitmapDrawable circularDrawable =
                androidx.core.graphics.drawable.RoundedBitmapDrawableFactory.create(getResources(), bitmap);
        circularDrawable.setCircular(true);
        btnProfile.setImageDrawable(circularDrawable);
        btnProfile.clearColorFilter();
        btnProfile.setPadding(0, 0, 0, 0);
        btnProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    private void resetToDefaultAvatar() {
        btnProfile.setImageResource(R.drawable.ic_user);
        btnProfile.setBackgroundResource(R.drawable.bg_icon_btn);
        int padding = (int) (8 * getResources().getDisplayMetrics().density);
        btnProfile.setPadding(padding, padding, padding, padding);
        btnProfile.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        btnProfile.setImageTintList(android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_primary)
        ));
    }

    @Override public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) updateAuthUI();
    }

    @Override public void onResume() {
        super.onResume();
        BinanceWebSocketManager.getInstance().addListener(this);
        loadBackendData();
        refreshList();
        refreshHotAssets();
        updateAuthUI();
    }

    @Override public void onPause() {
        super.onPause();
        BinanceWebSocketManager.getInstance().removeListener(this);
    }

    @Override public void onTickerUpdate(Map<String, CoinTicker> liveDataMap) {
        if (!isAdded()) return;
        for (Map.Entry<String, CoinTicker> entry : liveDataMap.entrySet()) {
            mergeTicker(entry.getKey(), entry.getValue());
        }
        requireActivity().runOnUiThread(() -> { refreshList(); refreshHotAssets(); });
    }

    @Override public void onConnected() {}
    @Override public void onDisconnected() {}
}