package com.cryptopulse.app.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.cryptopulse.app.R;
import com.cryptopulse.app.models.NewsItem;
import com.cryptopulse.app.network.ApiClient;
import com.cryptopulse.app.utils.AppPrefs;
import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.VH> {

    public interface OnClickListener { void onClick(NewsItem item); }

    private final List<NewsItem>  items = new ArrayList<>();
    private final OnClickListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public NewsAdapter(OnClickListener l) { this.listener = l; }

    public void setItems(List<NewsItem> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    public void addItems(List<NewsItem> list) {
        int start = items.size();
        items.addAll(list);
        notifyItemRangeInserted(start, list.size());
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_news, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        NewsItem item = items.get(pos);

        h.tvTitle.setText(item.getTitle());
        h.tvSnippet.setText(item.getSnippet());
        h.tvTime.setText(item.getTimeAgo());
        h.tvAuthor.setText("✍ " + (item.getAuthor() != null ? item.getAuthor() : "Unknown"));

        String cat = item.getCategory();
        if (cat != null && !cat.isEmpty()) {
            h.tvCategory.setText(cat);
            h.tvCategory.setVisibility(View.VISIBLE);
        } else {
            h.tvCategory.setVisibility(View.GONE);
        }

        if (h.tvReadTime != null)
            h.tvReadTime.setVisibility(View.GONE);

        loadThumbnail(h.ivThumb, item);
        h.itemView.setOnClickListener(v -> listener.onClick(item));

        // ── LOGIC XÓA THÔNG MINH ──
        String userRole = AppPrefs.get().getUserRole();
        if ("admin".equals(userRole)) {
            h.btnDeleteNews.setVisibility(View.VISIBLE);
            h.btnDeleteNews.setOnClickListener(v -> {
                int currentPos = h.getAdapterPosition();
                if (currentPos == RecyclerView.NO_POSITION) return;

                new AlertDialog.Builder(v.getContext())
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc muốn xóa bài viết này?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            String rawId = String.valueOf(item.getId());

                            if (rawId == null || rawId.isEmpty() || rawId.equals("null")) {
                                Toast.makeText(v.getContext(), "Lỗi: Không có ID!", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // PHÂN TÁCH LOGIC: CHẶN URL HOẶC XÓA ID SỐ
                            if (rawId.startsWith("http")) {
                                // Tin thị trường -> Thêm vào sổ đen (Blacklist)
                                hideNewsFromApi(v.getContext(), rawId, currentPos);
                            } else {
                                // Tin nội bộ -> Xóa trong MySQL
                                try {
                                    int newsId = (int) Double.parseDouble(rawId);
                                    deleteNewsFromApi(v.getContext(), newsId, currentPos);
                                } catch (Exception e) {
                                    Toast.makeText(v.getContext(), "Lỗi định dạng ID!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        } else {
            h.btnDeleteNews.setVisibility(View.GONE);
        }
    }

    // ── 1. GỌI API CHẶN TIN THỊ TRƯỜNG (REDIS BLACKLIST) ──
    private void hideNewsFromApi(Context context, String url, int position) {
        String token = "Bearer " + AppPrefs.get().getJwtToken();
        Map<String, String> body = new HashMap<>();
        body.put("url", url);

        ApiClient.get().hideWhaleNews(token, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "Đã chặn vĩnh viễn bài báo này!", Toast.LENGTH_SHORT).show();
                    removeItemFromList(position);
                } else {
                    Toast.makeText(context, "Lỗi Server chặn tin!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(context, "Mất kết nối!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── 2. GỌI API XÓA TIN NỘI BỘ (MYSQL) ──
    private void deleteNewsFromApi(Context context, int newsId, int position) {
        String token = "Bearer " + AppPrefs.get().getJwtToken();

        ApiClient.get().deleteNews(token, newsId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "Đã xóa bài viết!", Toast.LENGTH_SHORT).show();
                    removeItemFromList(position);
                } else {
                    Toast.makeText(context, "Lỗi Server xóa tin!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(context, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeItemFromList(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, items.size());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  PHẦN TẢI ẢNH
    // ══════════════════════════════════════════════════════════
    private void loadThumbnail(ImageView iv, NewsItem item) {
        if (iv == null) return;

        String imgUrl      = item.getImageUrl();
        String coin        = extractCoinFromText(item.getCategory(), item.getTitle());
        String fallbackUrl = getDefaultCoinImage(coin);
        ColorDrawable bg   = new ColorDrawable(0xFF1A2E1F);

        boolean hasReal = imgUrl != null && imgUrl.trim().startsWith("http");

        if (!hasReal) {
            showLogoFallback(iv, fallbackUrl, bg);
            return;
        }

        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setPadding(0, 0, 0, 0);
        iv.setBackgroundColor(0xFF1A2E1F);
        iv.setVisibility(View.VISIBLE);

        GlideUrl glideUrl = new GlideUrl(imgUrl,
                new LazyHeaders.Builder()
                        .addHeader("Referer",    getReferer(imgUrl))
                        .addHeader("User-Agent", "Mozilla/5.0")
                        .build()
        );

        Glide.with(iv.getContext())
                .load(glideUrl)
                .apply(new RequestOptions()
                        .centerCrop()
                        .override(400, 300)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                        .transform(new RoundedCorners(16))
                        .placeholder(bg))
                .error(
                        Glide.with(iv.getContext())
                                .load(fallbackUrl)
                                .apply(new RequestOptions()
                                        .centerInside()
                                        .placeholder(bg))
                )
                .addListener(new RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(
                            @Nullable GlideException e,
                            Object model,
                            Target<android.graphics.drawable.Drawable> target,
                            boolean isFirstResource) {
                        mainHandler.post(() -> {
                            iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                            int pad = dpToPx(iv.getContext(), 28);
                            iv.setPadding(pad, pad, pad, pad);
                        });
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(
                            android.graphics.drawable.Drawable resource,
                            Object model,
                            Target<android.graphics.drawable.Drawable> target,
                            DataSource dataSource,
                            boolean isFirstResource) {
                        return false;
                    }
                })
                .into(iv);
    }

    private void showLogoFallback(ImageView iv, String logoUrl, ColorDrawable bg) {
        iv.setVisibility(View.VISIBLE);
        iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int pad = dpToPx(iv.getContext(), 28);
        iv.setPadding(pad, pad, pad, pad);
        iv.setBackgroundColor(0xFF1A2E1F);

        Glide.with(iv.getContext())
                .load(logoUrl)
                .apply(new RequestOptions()
                        .centerInside()
                        .placeholder(bg))
                .into(iv);
    }

    private String getReferer(String url) {
        try {
            java.net.URL u = new java.net.URL(url);
            return u.getProtocol() + "://" + u.getHost() + "/";
        } catch (Exception e) {
            return "https://www.google.com/";
        }
    }

    private String extractCoinFromText(String category, String title) {
        String text = ((category != null ? category : "")
                + " " + (title != null ? title : "")).toUpperCase();
        if (text.contains("BTC") || text.contains("BITCOIN")) return "BTC";
        if (text.contains("ETH") || text.contains("ETHEREUM")) return "ETH";
        if (text.contains("BNB"))                              return "BNB";
        if (text.contains("SOL") || text.contains("SOLANA"))  return "SOL";
        if (text.contains("XRP") || text.contains("RIPPLE"))  return "XRP";
        return "BTC";
    }

    private String getDefaultCoinImage(String coin) {
        switch (coin) {
            case "ETH": return "https://assets.coingecko.com/coins/images/279/large/ethereum.png";
            case "BNB": return "https://assets.coingecko.com/coins/images/825/large/bnb-icon2_2x.png";
            case "SOL": return "https://assets.coingecko.com/coins/images/4128/large/solana.png";
            case "XRP": return "https://assets.coingecko.com/coins/images/44/large/xrp-symbol-white-128.png";
            default:    return "https://assets.coingecko.com/coins/images/1/large/bitcoin.png";
        }
    }

    private int dpToPx(Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView  tvTitle, tvSnippet, tvCategory, tvTime, tvAuthor, tvReadTime;
        ImageView ivThumb, btnDeleteNews;

        VH(View v) {
            super(v);
            tvTitle       = v.findViewById(R.id.tv_news_title);
            tvSnippet     = v.findViewById(R.id.tv_news_summary);
            tvCategory    = v.findViewById(R.id.tv_news_category);
            tvTime        = v.findViewById(R.id.tv_news_time);
            tvAuthor      = v.findViewById(R.id.tv_news_author);
            tvReadTime    = v.findViewById(R.id.tv_read_time);
            ivThumb       = v.findViewById(R.id.iv_news_thumb);
            btnDeleteNews = v.findViewById(R.id.btn_delete_news);
        }
    }
}