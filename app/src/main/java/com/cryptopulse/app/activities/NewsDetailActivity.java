package com.cryptopulse.app.activities;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestOptions;
import com.cryptopulse.app.R;
import com.cryptopulse.app.utils.AnimUtils;

public class NewsDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_news_detail);

        String title         = getIntent().getStringExtra("news_title");
        String titleOriginal = getIntent().getStringExtra("news_title_original");
        String category      = getIntent().getStringExtra("news_category");
        String author        = getIntent().getStringExtra("news_author");
        String timeAgo       = getIntent().getStringExtra("news_time_ago");
        String content       = getIntent().getStringExtra("news_content");
        String url           = getIntent().getStringExtra("news_url");
        String imageUrl      = getIntent().getStringExtra("news_image");

        // Làm sạch URL
        if (url != null) url = url.replaceAll("\\s+", "").trim();

        setText(R.id.tv_news_detail_title,    title);
        setText(R.id.tv_news_detail_category, category);
        setText(R.id.tv_news_detail_author,
                "✍ " + (author != null ? author : "Unknown"));
        setText(R.id.tv_news_detail_time, timeAgo);
        setText(R.id.tv_news_detail_content,
                content != null && !content.isEmpty()
                        ? content : "Nội dung đầy đủ có tại nguồn gốc.");

        // ── Ảnh ───────────────────────────────────────────────
        loadDetailImage(imageUrl, category, title);

        // ── Back ──────────────────────────────────────────────
        findViewById(R.id.btn_news_back).setOnClickListener(v -> {
            AnimUtils.scalePress(v); finish();
        });

        // ── Nút xem bài gốc ───────────────────────────────────
        setupSourceButton(url, titleOriginal, title);

        AnimUtils.slideUp(findViewById(R.id.news_detail_scroll), 400, 0);
    }

    // ══════════════════════════════════════════════════════════
    //  LOAD ẢNH DETAIL
    // ══════════════════════════════════════════════════════════
    private void loadDetailImage(String imageUrl, String category, String title) {
        ImageView iv = findViewById(R.id.iv_news_thumbnail);
        if (iv == null) return;

        String fallbackUrl = getFallbackImage(category, title);
        ColorDrawable bg = new ColorDrawable(0xFF0D1F15);

        boolean hasReal = imageUrl != null && imageUrl.trim().startsWith("http");

        if (!hasReal) {
            showLogoFallback(iv, fallbackUrl, bg);
            return;
        }
        iv.setVisibility(View.VISIBLE);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setPadding(0, 0, 0, 0);

        GlideUrl glideUrl = new GlideUrl(imageUrl,
                new LazyHeaders.Builder()
                        .addHeader("Referer", getReferer(imageUrl))
                        .addHeader("User-Agent", "Mozilla/5.0")
                        .build());

        Glide.with(this)
                .load(glideUrl)
                .apply(new RequestOptions()
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Tối ưu cache
                        .placeholder(bg))
                .error(Glide.with(this)
                        .load(fallbackUrl)
                        .apply(new RequestOptions()
                                .fitCenter()
                                .placeholder(bg)))
                .listener(new com.bumptech.glide.request.RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<Drawable> target, boolean isFirstResource) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            int pad = Math.round(48 * getResources().getDisplayMetrics().density);
                            iv.setPadding(pad, pad, pad, pad);
                        });
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, com.bumptech.glide.request.target.Target<Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        return false;
                    }
                })
                .into(iv);
    }

    private void showLogoFallback(ImageView iv, String logoUrl, ColorDrawable bg) {
        iv.setVisibility(View.VISIBLE);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int pad = Math.round(48 * getResources().getDisplayMetrics().density);
        iv.setPadding(pad, pad, pad, pad);

        Glide.with(this)
                .load(logoUrl)
                .apply(new RequestOptions()
                        .fitCenter()
                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Tối ưu cache
                        .placeholder(bg))
                .into(iv);
    }

    // ══════════════════════════════════════════════════════════
    //  NÚT XEM BÀI GỐC
    // ══════════════════════════════════════════════════════════
    private void setupSourceButton(String url, String titleOriginal, String titleVi) {
        Button btn = findViewById(R.id.btn_open_source);
        if (btn == null) return;

        if (url == null || (!url.startsWith("http") && !url.startsWith("https"))) {
            btn.setVisibility(View.GONE);
            return;
        }

        btn.setVisibility(View.VISIBLE);
        btn.setText("Xem bài gốc");
        btn.setEnabled(true);

        btn.setOnClickListener(v -> {
            btn.setEnabled(false);
            btn.setText("Đang mở...");

            try {
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                builder.setToolbarColor(ContextCompat.getColor(this, R.color.bg_primary));
                builder.setShowTitle(true);

                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(this, Uri.parse(url));

            } catch (Exception e) {
                Toast.makeText(this, "Không thể mở link này!", Toast.LENGTH_SHORT).show();
            } finally {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    btn.setEnabled(true);
                    btn.setText("Xem bài gốc");
                }, 1000);
            }
        });
    }

    // ══════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════
    private void setText(int id, String text) {
        TextView tv = findViewById(id);
        if (tv != null && text != null) tv.setText(text);
    }

    private String getReferer(String url) {
        try {
            java.net.URL u = new java.net.URL(url);
            return u.getProtocol() + "://" + u.getHost() + "/";
        } catch (Exception e) { return "https://www.google.com/"; }
    }

    private String getFallbackImage(String category, String title) {
        String text = ((category != null ? category : "")
                + " " + (title != null ? title : "")).toUpperCase();
        if (text.contains("BTC") || text.contains("BITCOIN"))
            return "https://assets.coingecko.com/coins/images/1/large/bitcoin.png";
        if (text.contains("ETH") || text.contains("ETHEREUM"))
            return "https://assets.coingecko.com/coins/images/279/large/ethereum.png";
        if (text.contains("BNB"))
            return "https://assets.coingecko.com/coins/images/825/large/bnb-icon2_2x.png";
        if (text.contains("SOL") || text.contains("SOLANA"))
            return "https://assets.coingecko.com/coins/images/4128/large/solana.png";
        if (text.contains("XRP") || text.contains("RIPPLE"))
            return "https://assets.coingecko.com/coins/images/44/large/xrp-symbol-white-128.png";

        return "https://assets.coingecko.com/coins/images/1/large/bitcoin.png";
    }
}