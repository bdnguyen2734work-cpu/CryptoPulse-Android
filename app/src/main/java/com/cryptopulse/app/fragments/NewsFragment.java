package com.cryptopulse.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.cryptopulse.app.R;
import com.cryptopulse.app.activities.NewsDetailActivity;
import com.cryptopulse.app.adapters.NewsAdapter;
import com.cryptopulse.app.models.NewsItem;
import com.cryptopulse.app.network.ApiClient;
import com.cryptopulse.app.viewmodels.MarketViewModel;
import java.util.*;

public class NewsFragment extends Fragment {

    private static final int  TAB_MARKET   = 0;
    private static final int  TAB_VIETNAM  = 1;
    private static final long THROTTLE_MS  = 5 * 60 * 1000L;

    private long lastLoadTimeMs = 0L;
    private int  currentTab     = TAB_MARKET;

    // ── Views ─────────────────────────────────────────────────────
    private RecyclerView       rvNews;
    private NewsAdapter        adapter;
    private SwipeRefreshLayout swipeRefresh;
    private TextView           tvTabMarket, tvTabVietnam;
    private TextView           tvUpdatedAt, tvEmptyState;
    private ProgressBar        progressBar;

    // ── Data ──────────────────────────────────────────────────────
    private MarketViewModel      viewModel;
    private final List<NewsItem> marketItems  = new ArrayList<>();
    private final List<NewsItem> vietnamItems = new ArrayList<>();

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup c, @Nullable Bundle b) {
        return inf.inflate(R.layout.fragment_news, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        viewModel = new ViewModelProvider(requireActivity()).get(MarketViewModel.class);
        bindViews(v);
        setupRecyclerView();
        setupTabs();
        setupSwipeRefresh();
        observeData();
        loadAll(false);
    }

    // ══════════════════════════════════════════════════════════════
    //  BIND
    // ══════════════════════════════════════════════════════════════
    private void bindViews(View v) {
        rvNews       = v.findViewById(R.id.rv_news);
        swipeRefresh = v.findViewById(R.id.swipe_refresh);
        tvTabMarket  = v.findViewById(R.id.tab_market);
        tvTabVietnam = v.findViewById(R.id.tab_vietnam);
        tvUpdatedAt  = v.findViewById(R.id.tv_updated_at);
        tvEmptyState = v.findViewById(R.id.tv_empty_state);
        progressBar  = v.findViewById(R.id.progress_news);
    }

    // ══════════════════════════════════════════════════════════════
    //  RECYCLER
    // ══════════════════════════════════════════════════════════════
    private void setupRecyclerView() {
        adapter = new NewsAdapter(item -> {
            Intent i = new Intent(requireContext(), NewsDetailActivity.class);
            i.putExtra("news_title",          item.getTitle());
            i.putExtra("news_title_original", item.getTitleOriginal());
            i.putExtra("news_category",       item.getCategory());
            i.putExtra("news_author",         item.getAuthor());
            i.putExtra("news_time_ago",       item.getTimeAgo());
            i.putExtra("news_content",        item.getBody());
            i.putExtra("news_url",            item.getUrl());
            i.putExtra("news_image",          item.getImageUrl());
            i.putExtra("news_snippet",        item.getSnippet());
            startActivity(i);
        });
        rvNews.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNews.setItemAnimator(null);
        rvNews.setHasFixedSize(true);
        rvNews.setItemViewCacheSize(20);
        rvNews.setAdapter(adapter);
    }

    // ══════════════════════════════════════════════════════════════
    //  TABS
    // ══════════════════════════════════════════════════════════════
    private void setupTabs() {
        tvTabMarket.setOnClickListener(v  -> selectTab(TAB_MARKET));
        tvTabVietnam.setOnClickListener(v -> selectTab(TAB_VIETNAM));
        selectTab(TAB_MARKET);
    }

    private void selectTab(int tab) {
        currentTab = tab;
        int normal = ContextCompat.getColor(requireContext(), R.color.text_secondary);
        int active = ContextCompat.getColor(requireContext(), R.color.accent_green);

        tvTabMarket.setTextColor(normal);
        tvTabMarket.setBackgroundResource(R.drawable.bg_timeframe_normal);
        tvTabVietnam.setTextColor(normal);
        tvTabVietnam.setBackgroundResource(R.drawable.bg_timeframe_normal);

        TextView activeTab    = (tab == TAB_VIETNAM) ? tvTabVietnam : tvTabMarket;
        List<NewsItem> source = (tab == TAB_VIETNAM) ? vietnamItems : marketItems;

        activeTab.setTextColor(active);
        activeTab.setBackgroundResource(R.drawable.bg_timeframe_selected);
        adapter.setItems(source);
        updateEmptyState(source);
    }

    // ══════════════════════════════════════════════════════════════
    //  SWIPE REFRESH
    // ══════════════════════════════════════════════════════════════
    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(
                ContextCompat.getColor(requireContext(), R.color.accent_green));
        swipeRefresh.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(requireContext(), R.color.bg_card));
        swipeRefresh.setOnRefreshListener(() -> {
            lastLoadTimeMs = 0L;
            loadAll(true);
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  LOAD
    // ══════════════════════════════════════════════════════════════
    private void loadAll(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && (now - lastLoadTimeMs) < THROTTLE_MS) {
            stopRefreshing(); return;
        }
        lastLoadTimeMs = now;
        if (ApiClient.get() == null) { loadFallback(); stopRefreshing(); return; }

        showLoading(true);
        viewModel.loadNews();
        viewModel.loadWhaleNews(false);

        uiHandler.postDelayed(() -> {
            showLoading(false);
            stopRefreshing();
        }, 15_000);
    }

    // ══════════════════════════════════════════════════════════════
    //  OBSERVE
    // ══════════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private void observeData() {

        // ── Tin Việt Nam (Tải từ Database Admin) ───────────────────
        viewModel.getNews().observe(getViewLifecycleOwner(), data -> {
            uiHandler.removeCallbacksAndMessages(null);
            showLoading(false);
            stopRefreshing();
            if (data == null || !isAdded()) return;
            try {
                Object obj = data.get("data");
                if (!(obj instanceof List)) return;
                vietnamItems.clear();
                for (Object item : (List<?>) obj) {
                    if (!(item instanceof Map)) continue;
                    Map<?,?> m      = (Map<?,?>) item;
                    String content  = str(m, "content");
                    NewsItem n      = new NewsItem();
                    n.setId(str(m, "id"));
                    n.setTitle(str(m, "title"));
                    n.setBody(content);
                    n.setSnippet(truncate(content, 120));
                    n.setCategory("🇻🇳 VIỆT NAM"); // Tag màu nhận diện
                    n.setAuthor(str(m, "author"));
                    n.setTimeAgo(fmtTime(str(m, "created_at")));
                    n.setImageUrl(str(m, "image_url"));
                    n.setReadTime(estimateReadTime(content));
                    n.setUrl(str(m, "original_url"));
                    n.setTitleOriginal("");
                    vietnamItems.add(n);
                }
                rebuildAll();
            } catch (Exception ignored) {}
        });

        viewModel.getWhaleNews().observe(getViewLifecycleOwner(), data -> {
            uiHandler.removeCallbacksAndMessages(null);
            showLoading(false);
            stopRefreshing();
            if (data == null || !isAdded()) return;
            try {
                Object obj       = data.get("data");
                String updatedAt = str2(data, "updated_at");
                if (!(obj instanceof List)) return;

                marketItems.clear();
                for (Object item : (List<?>) obj) {
                    if (!(item instanceof Map)) continue;
                    Map<?,?> m = (Map<?,?>) item;

                    String title         = str(m, "title");
                    String titleOriginal = str(m, "title_original");
                    String summary       = str(m, "summary");
                    String url           = str(m, "url");
                    String source        = str(m, "source");
                    String pub           = str(m, "published");
                    String image         = str(m, "image");
                    String coinTag       = buildCoinTag(m.get("coins"), 3);

                    NewsItem n = new NewsItem();
                    n.setId(url);
                    n.setTitle(title);
                    n.setTitleOriginal(titleOriginal);
                    n.setSnippet(summary.isEmpty()
                            ? truncate(title, 100) : truncate(summary, 120));
                    n.setBody(summary.isEmpty() ? title : summary);
                    n.setCategory(coinTag.isEmpty() ? "GLOBAL" : coinTag);
                    n.setAuthor(source != null && !source.isEmpty()
                            ? source : "Crypto News");
                    n.setTimeAgo(fmtPubDate(pub));
                    n.setImageUrl(image != null && image.startsWith("http") ? image : "");
                    n.setUrl(url);
                    n.setReadTime("2 min");
                    marketItems.add(n);
                }

                rebuildAll();

                if (tvUpdatedAt != null && !updatedAt.isEmpty()) {
                    try {
                        String t = updatedAt.length() > 16
                                ? updatedAt.substring(11, 16) : updatedAt;
                        tvUpdatedAt.setText("• " + t);
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  REBUILD
    // ══════════════════════════════════════════════════════════════
    private void rebuildAll() {
        if (currentTab == TAB_MARKET)  adapter.setItems(marketItems);
        if (currentTab == TAB_VIETNAM) adapter.setItems(vietnamItems);
        updateEmptyState(currentTabItems());
    }

    private List<NewsItem> currentTabItems() {
        return (currentTab == TAB_VIETNAM) ? vietnamItems : marketItems;
    }

    // ══════════════════════════════════════════════════════════════
    //  FALLBACK
    // ══════════════════════════════════════════════════════════════
    private void loadFallback() {
        vietnamItems.clear();
        vietnamItems.add(makeLocal("1",
                "Chính phủ ban hành quy định mới về quản lý tài sản số",
                "Ngân hàng nhà nước Việt Nam vừa đưa ra dự thảo mới...",
                "Ngân hàng nhà nước Việt Nam vừa đưa ra dự thảo mới nhằm siết chặt và quản lý minh bạch dòng tiền điện tử...",
                "🇻🇳 CHÍNH SÁCH", "Admin", "Vừa xong"));
        rebuildAll();
    }

    private NewsItem makeLocal(String id, String title, String snippet,
                               String body, String cat, String author, String time) {
        NewsItem n = new NewsItem();
        n.setId(id); n.setTitle(title); n.setSnippet(snippet);
        n.setBody(body); n.setCategory(cat); n.setAuthor(author);
        n.setTimeAgo(time); n.setReadTime("3 min");
        return n;
    }

    // ══════════════════════════════════════════════════════════════
    //  UI & STRING HELPERS
    // ══════════════════════════════════════════════════════════════
    private void showLoading(boolean show) {
        if (progressBar != null)
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void stopRefreshing() {
        if (swipeRefresh != null && swipeRefresh.isRefreshing())
            swipeRefresh.setRefreshing(false);
    }

    private void updateEmptyState(List<NewsItem> list) {
        if (tvEmptyState == null) return;
        tvEmptyState.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String str(Map<?,?> m, String k) {
        Object v = m.get(k);
        return (v != null && !"null".equals(String.valueOf(v)))
                ? String.valueOf(v).trim() : "";
    }

    private String str2(Map<?,?> m, String k) {
        Object v = m.get(k);
        return v != null ? String.valueOf(v) : "";
    }

    private String buildCoinTag(Object coinsObj, int maxCoins) {
        if (!(coinsObj instanceof List)) return "";
        List<?> coins = (List<?>) coinsObj;
        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(coins.size(), maxCoins); i++) {
            String c = String.valueOf(coins.get(i)).trim();
            if (!c.isEmpty() && !"null".equals(c)) result.add(c);
        }
        return String.join(" • ", result);
    }

    private String truncate(String s, int max) {
        if (s == null || s.isEmpty()) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private String fmtTime(String iso) {
        if (iso == null || iso.length() < 10) return "";
        try {
            return iso.length() >= 16
                    ? iso.substring(0, 10) + " " + iso.substring(11, 16)
                    : iso.substring(0, 10);
        } catch (Exception e) { return iso; }
    }

    private String fmtPubDate(String pub) {
        if (pub == null || pub.isEmpty()) return "";
        try {
            if (pub.length() >= 16)
                return pub.substring(0, 10) + " " + pub.substring(11, 16);
            return pub.substring(0, Math.min(10, pub.length()));
        } catch (Exception e) { return pub; }
    }

    private String estimateReadTime(String content) {
        if (content == null || content.isEmpty()) return "1 min";
        int words = content.trim().split("\\s+").length;
        return Math.max(1, words / 200) + " min";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        uiHandler.removeCallbacksAndMessages(null);
    }
}