package com.cryptopulse.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cryptopulse.app.R;
import com.cryptopulse.app.models.CoinTicker;
import com.cryptopulse.app.utils.FormatUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HotAssetAdapter extends RecyclerView.Adapter<HotAssetAdapter.VH> {

    public interface OnPickListener { void onPick(CoinTicker t); }

    private final List<CoinTicker> items = new ArrayList<>();
    private final OnPickListener listener;

    // KHO CHỨA LINK ẢNH TỪ COINGECKO
    private static final Map<String, String> LOGOS = new HashMap<>();
    static {
        LOGOS.put("BTCUSDT", "https://assets.coingecko.com/coins/images/1/large/bitcoin.png");
        LOGOS.put("ETHUSDT", "https://assets.coingecko.com/coins/images/279/large/ethereum.png");
        LOGOS.put("BNBUSDT", "https://assets.coingecko.com/coins/images/825/large/bnb-icon2_2x.png");
        LOGOS.put("SOLUSDT", "https://assets.coingecko.com/coins/images/4128/large/solana.png");
    }

    public HotAssetAdapter(OnPickListener l) { this.listener = l; }

    public void updateData(List<CoinTicker> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_hot_asset, p, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        CoinTicker t = items.get(pos);

        // Set các text cơ bản
        h.tvName.setText(t.getCoinName());
        h.tvSymbol.setText(t.getDisplaySymbol());
        h.tvPrice.setText(FormatUtils.price(t.getPrice()));
        h.tvChange.setText(FormatUtils.pct(t.getChangePct()));

        // --- 1. LOGIC ĐỔI MÀU ĐỘNG (XANH / ĐỎ) ---
        if (t.isPositive()) {
            // TRẠNG THÁI TĂNG (MÀU XANH)
            h.tvChange.setTextColor(0xFF39FF6E);           // Chữ % màu xanh
            h.tvBadge.setTextColor(0xFF39FF6E);            // Chữ HOT màu xanh
            h.cardView.setCardBackgroundColor(0xFF121F17); // Nền thẻ xanh đen
            h.cardView.setStrokeColor(0xFF2A4A35);         // Viền thẻ xanh lá tối
        } else {
            // TRẠNG THÁI GIẢM (MÀU ĐỎ)
            h.tvChange.setTextColor(0xFFFF4444);           // Chữ % màu đỏ
            h.tvBadge.setTextColor(0xFFFF4444);            // Chữ HOT màu đỏ
            h.cardView.setCardBackgroundColor(0xFF221212); // Nền thẻ đỏ đen bầm
            h.cardView.setStrokeColor(0xFF4A2A2A);         // Viền thẻ đỏ tối
        }

        // --- 2. LẤY LINK ẢNH VÀ DÙNG GLIDE ĐỂ LOAD ---
        String imageUrl = LOGOS.get(t.symbol);
        if (imageUrl != null) {
            Glide.with(h.itemView.getContext())
                    .load(imageUrl)
                    .into(h.ivIcon);
        } else {
            h.ivIcon.setImageDrawable(null);
        }

        // Sự kiện vuốt/bấm vào thẻ để xem chi tiết
        h.itemView.setOnClickListener(v -> listener.onPick(t));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvSymbol, tvPrice, tvChange, tvBadge;
        ImageView ivIcon;
        MaterialCardView cardView;

        VH(View v) {
            super(v);
            // Ép kiểu View thành MaterialCardView để có thể đổi màu viền/nền
            cardView = (MaterialCardView) v;
            tvBadge = v.findViewById(R.id.tv_hot_badge);
            ivIcon = v.findViewById(R.id.iv_hot_icon);
            tvName = v.findViewById(R.id.tv_hot_name);
            tvSymbol = v.findViewById(R.id.tv_hot_symbol);
            tvPrice = v.findViewById(R.id.tv_hot_price);
            tvChange = v.findViewById(R.id.tv_hot_change);
        }
    }
}