package com.cryptopulse.app.adapters;

import android.annotation.SuppressLint;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.cryptopulse.app.R;
import com.cryptopulse.app.models.CoinTicker;
import com.cryptopulse.app.utils.AnimUtils;
import com.cryptopulse.app.utils.FormatUtils;
import java.util.*;

public class CoinListAdapter extends RecyclerView.Adapter<CoinListAdapter.VH> {

    public interface OnCoinClick { void onClick(CoinTicker t); }

    private List<CoinTicker> data;
    private final OnCoinClick listener;
    private final Map<String, Double> prevPrices = new HashMap<>();

    public CoinListAdapter(List<CoinTicker> data, OnCoinClick listener) {
        this.data = new ArrayList<>(data);
        this.listener = listener;
        setHasStableIds(true);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<CoinTicker> newData) {
        this.data = new ArrayList<>(newData);
        notifyDataSetChanged();
    }

    @Override public int getItemCount() { return data.size(); }

    @Override public long getItemId(int pos) {
        return data.get(pos).symbol != null ? data.get(pos).symbol.hashCode() : pos;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_coin, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        CoinTicker t = data.get(pos);
        h.tvSymbol.setText(t.getDisplaySymbol());
        h.tvName.setText(t.getCoinName());
        h.tvPrice.setText(FormatUtils.price(t.getPrice()));
        h.tvChange.setText(FormatUtils.pct(t.getChangePct()));

        h.tvChange.setTextColor(t.isPositive() ? 0xFF39FF6E : 0xFFFF4444);
        h.tvChange.setBackgroundResource(t.isPositive() ? R.drawable.tag_green : R.drawable.tag_breaking);

        // Hiển thị Logo từ URL hoặc dùng Avatar chữ cái nếu không có ảnh
        String logoUrl = t.getLogoUrl();
        if (logoUrl != null && !logoUrl.isEmpty()) {
            h.tvIcon.setVisibility(View.GONE);
            h.ivLogo.setVisibility(View.VISIBLE);
            Glide.with(h.ivLogo.getContext())
                    .load(logoUrl)
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_coin_placeholder)
                    .error(R.drawable.ic_coin_placeholder)
                    .into(h.ivLogo);
        } else {
            h.ivLogo.setVisibility(View.GONE);
            h.tvIcon.setVisibility(View.VISIBLE);
            h.tvIcon.setText(t.getIconLetter());
            h.tvIcon.setBackgroundColor(t.getCoinColor());
        }

        // Tạo hiệu ứng nháy đèn khi giá thay đổi
        Double prev = prevPrices.get(t.symbol);
        if (prev != null && Math.abs(prev - t.getPrice()) > 0.0001) {
            AnimUtils.flashPrice(h.tvPrice, t.getPrice() > prev, 0xFF39FF6E, 0xFFFF4444);
        }
        prevPrices.put(t.symbol, t.getPrice());

        h.itemView.setOnClickListener(v -> {
            AnimUtils.scalePress(h.itemView);
            listener.onClick(t);
        });
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView  tvSymbol, tvName, tvPrice, tvChange, tvIcon;
        ImageView ivLogo;

        VH(View v) {
            super(v);
            tvSymbol = v.findViewById(R.id.tv_coin_symbol);
            tvName   = v.findViewById(R.id.tv_coin_name);
            tvPrice  = v.findViewById(R.id.tv_coin_price);
            tvChange = v.findViewById(R.id.tv_coin_change);
            tvIcon   = v.findViewById(R.id.iv_coin_icon);
            ivLogo   = v.findViewById(R.id.iv_coin_logo);
        }
    }
}