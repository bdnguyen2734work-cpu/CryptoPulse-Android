package com.cryptopulse.app.adapters;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cryptopulse.app.R;
import com.cryptopulse.app.models.CoinTicker;
import java.util.*;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.VH> {

    public interface OnPickListener { void onPick(CoinTicker t); }

    private final List<CoinTicker> items = new ArrayList<>();
    private final OnPickListener   listener;

    public SearchAdapter(OnPickListener l) { this.listener = l; }

    public void updateData(List<CoinTicker> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        View v = LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_search_result, p, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        CoinTicker t = items.get(pos);
        h.tvSymbol.setText(t.getDisplaySymbol());
        h.tvName.setText(t.getCoinName());
        h.tvPrice.setText(String.format(Locale.US,
                t.getPrice() >= 1 ? "$%,.2f" : "$%.6f", t.getPrice()));
        double pct = t.getChangePct();
        h.tvChange.setText(String.format(Locale.US,
                pct >= 0 ? "+%.2f%%" : "%.2f%%", pct));
        h.tvChange.setTextColor(pct >= 0 ? 0xFF39FF6E : 0xFFFF4444);
        h.itemView.setOnClickListener(v -> listener.onPick(t));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvSymbol, tvName, tvPrice, tvChange;
        VH(View v) {
            super(v);
            tvSymbol = v.findViewById(R.id.tv_search_symbol);
            tvName   = v.findViewById(R.id.tv_search_name);
            tvPrice  = v.findViewById(R.id.tv_search_price);
            tvChange = v.findViewById(R.id.tv_search_change);
        }
    }
}