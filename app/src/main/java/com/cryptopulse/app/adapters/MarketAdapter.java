package com.cryptopulse.app.adapters;

import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.cryptopulse.app.R;
import com.cryptopulse.app.models.CoinTicker;
import java.util.*;

public class MarketAdapter extends RecyclerView.Adapter<MarketAdapter.ViewHolder> {

    public enum SortBy    { PRICE, CHANGE, VOLUME }
    public enum SortState { NONE, SELECTED, DESCENDING, ASCENDING }

    public interface OnItemClickListener { void onItemClick(CoinTicker ticker); }

    private List<CoinTicker> displayList;
    private List<CoinTicker> originalList;
    private final OnItemClickListener listener;

    private String currentQuery = "";
    private SortBy currentSort = null;
    private SortState currentSortState = SortState.NONE;

    public MarketAdapter(List<CoinTicker> list, OnItemClickListener listener) {
        this.originalList = new ArrayList<>(list);
        this.displayList  = new ArrayList<>(list);
        this.listener     = listener;
    }

    public SortBy getCurrentSort() { return currentSort; }
    public SortState getCurrentSortState() { return currentSortState; }

    public void updateData(List<CoinTicker> newList) {
        this.originalList = new ArrayList<>(newList);
        filter(currentQuery);
    }

    public void sort(SortBy sortBy) {
        if (currentSort == sortBy) {
            switch (currentSortState) {
                case SELECTED:   currentSortState = SortState.DESCENDING; break;
                case DESCENDING: currentSortState = SortState.ASCENDING;  break;
                case ASCENDING:
                    currentSortState = SortState.NONE;
                    currentSort      = null;
                    break;
                default: currentSortState = SortState.SELECTED; break;
            }
        } else {
            currentSort = sortBy;
            currentSortState = SortState.SELECTED;
        }
        filter(currentQuery);
    }

    public void filter(String query) {
        currentQuery = query.toLowerCase().trim();
        displayList.clear();

        for (CoinTicker t : originalList) {
            if (currentQuery.isEmpty() || t.getDisplaySymbol().toLowerCase().contains(currentQuery) || t.getCoinName().toLowerCase().contains(currentQuery)) {
                displayList.add(t);
            }
        }

        if (currentSort != null && currentSortState != SortState.NONE && currentSortState != SortState.SELECTED) {
            displayList.sort((o1, o2) -> {
                int result = 0;
                switch (currentSort) {
                    case PRICE:
                        result = Double.compare(o1.getPrice(), o2.getPrice());
                        break;
                    case CHANGE:
                        result = Double.compare(o1.getChangePct(), o2.getChangePct());
                        break;
                    case VOLUME:
                        double v1 = o1.getQuoteVolume() > 0 ? o1.getQuoteVolume() : o1.getPrice() * o1.getVolume();
                        double v2 = o2.getQuoteVolume() > 0 ? o2.getQuoteVolume() : o2.getPrice() * o2.getVolume();
                        result = Double.compare(v1, v2);
                        break;
                }
                return currentSortState == SortState.DESCENDING ? -result : result;
            });
        }
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_coin_market, p, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        CoinTicker t = displayList.get(pos);

        h.tvRank.setText(String.valueOf(pos + 1));
        h.tvSym.setText(t.getDisplaySymbol());
        h.tvName.setText(t.getCoinName());

        if (!t.getLogoUrl().isEmpty()) {
            Glide.with(h.itemView.getContext())
                    .load(t.getLogoUrl())
                    .placeholder(R.drawable.ic_nav_market)
                    .into(h.ivLogo);
        }

        h.tvPrice.setText(formatPrice(t.getPrice()));

        double pct = t.getChangePct();
        h.tvChange.setText(String.format(Locale.US, "%+.2f%%", pct));
        h.tvChange.setTextColor(pct >= 0 ? 0xFF39FF6E : 0xFFFF4444);

        if (h.tvVolume != null) {
            h.tvVolume.setText(t.getFormattedQuoteVolume());
        }

        h.itemView.setOnClickListener(v -> listener.onItemClick(t));
    }

    @Override public int getItemCount() { return displayList.size(); }

    private String formatPrice(double p) {
        if (p <= 0)      return "$0.00";
        if (p < 0.00001) return String.format(Locale.US, "$%.8f", p);
        if (p < 0.01)    return String.format(Locale.US, "$%.6f", p);
        if (p < 1.0)     return String.format(Locale.US, "$%.4f", p);
        if (p < 10_000)  return String.format(Locale.US, "$%.2f", p);
        return              String.format(Locale.US, "$%,.2f", p);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView  tvRank, tvSym, tvName, tvPrice, tvChange, tvVolume;
        ImageView ivLogo;

        ViewHolder(View v) {
            super(v);
            tvRank   = v.findViewById(R.id.tv_market_rank);
            ivLogo   = v.findViewById(R.id.iv_market_logo);
            tvSym    = v.findViewById(R.id.tv_market_sym);
            tvName   = v.findViewById(R.id.tv_market_name);
            tvPrice  = v.findViewById(R.id.tv_market_price);
            tvChange = v.findViewById(R.id.tv_market_change);
            tvVolume = v.findViewById(R.id.tv_market_volume);
        }
    }
}