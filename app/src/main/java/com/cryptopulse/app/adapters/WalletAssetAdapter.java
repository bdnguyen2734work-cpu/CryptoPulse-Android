package com.cryptopulse.app.adapters;

import android.graphics.Color;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.cryptopulse.app.R;
import com.cryptopulse.app.models.WalletAsset;
import com.cryptopulse.app.utils.FormatUtils;
import java.util.List;
import java.util.Locale;

public class WalletAssetAdapter extends RecyclerView.Adapter<WalletAssetAdapter.VH> {

    private final List<WalletAsset> data;

    public WalletAssetAdapter(List<WalletAsset> data) { this.data = data; }

    @Override public int getItemCount() { return data.size(); }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wallet_asset, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        WalletAsset a = data.get(pos);
        h.tvSymbol.setText(a.symbol);
        h.tvName.setText(a.name);
        h.tvAmount.setText(String.format(Locale.US, "%.4f %s", a.balance, a.symbol));
        h.tvValue.setText(FormatUtils.shortPrice(a.value));
        h.tvPrice.setText(FormatUtils.price(a.price));
        String finalLogoUrl = a.logoUrl;
        if (finalLogoUrl == null || finalLogoUrl.isEmpty()) {
            switch (a.symbol.toUpperCase()) {
                case "AVAX": finalLogoUrl = "https://cryptologos.cc/logos/avalanche-avax-logo.png"; break;
                case "ETH":  finalLogoUrl = "https://cryptologos.cc/logos/ethereum-eth-logo.png"; break;
                case "BNB":  finalLogoUrl = "https://cryptologos.cc/logos/bnb-bnb-logo.png"; break;
                case "MATIC":finalLogoUrl = "https://cryptologos.cc/logos/polygon-matic-logo.png"; break;
                case "USDT": finalLogoUrl = "https://cryptologos.cc/logos/tether-usdt-logo.png"; break;
                case "USDC": finalLogoUrl = "https://cryptologos.cc/logos/usd-coin-usdc-logo.png"; break;
                case "ARB":  finalLogoUrl = "https://cryptologos.cc/logos/arbitrum-arb-logo.png"; break;
                case "OP":   finalLogoUrl = "https://cryptologos.cc/logos/optimism-ethereum-op-logo.png"; break;
            }
        }
        if (finalLogoUrl != null && !finalLogoUrl.isEmpty()) {
            h.tvIcon.setVisibility(View.GONE);
            h.ivLogo.setVisibility(View.VISIBLE);
            Glide.with(h.ivLogo.getContext())
                    .load(finalLogoUrl)
                    .transform(new CircleCrop())
                    .error(R.drawable.ic_coin_placeholder)
                    .into(h.ivLogo);
        } else {
            h.ivLogo.setVisibility(View.GONE);
            h.tvIcon.setVisibility(View.VISIBLE);
            h.tvIcon.setText(a.symbol.isEmpty() ? "?" : String.valueOf(a.symbol.charAt(0)));
            try { h.tvIcon.getBackground().setTint(Color.parseColor(a.color));}
            catch (Exception e) { h.tvIcon.getBackground().setTint(0xFF39FF6E); }
        }

        // Portfolio % bar — tỷ lệ trong tổng danh mục
        double total = 0;
        for (WalletAsset w : data) total += w.value;
        int pct = total > 0 ? (int)(a.value / total * 100) : 0;
        h.tvPct.setText(pct + "%");
        ViewGroup.LayoutParams lp = h.viewBar.getLayoutParams();
        lp.width = (int)(h.viewBarBg.getLayoutParams().width * pct / 100.0);
        h.viewBar.setLayoutParams(lp);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView  tvSymbol, tvName, tvAmount, tvValue, tvPrice, tvIcon, tvPct;
        ImageView ivLogo;
        View      viewBar, viewBarBg;
        VH(View v) {
            super(v);
            tvSymbol  = v.findViewById(R.id.tv_asset_symbol);
            tvName    = v.findViewById(R.id.tv_asset_name);
            tvAmount  = v.findViewById(R.id.tv_asset_amount);
            tvValue   = v.findViewById(R.id.tv_asset_value);
            tvPrice   = v.findViewById(R.id.tv_asset_price);
            tvIcon    = v.findViewById(R.id.tv_asset_icon);
            ivLogo    = v.findViewById(R.id.iv_asset_logo);
            tvPct     = v.findViewById(R.id.tv_asset_pct);
            viewBar   = v.findViewById(R.id.view_asset_bar);
            viewBarBg = v.findViewById(R.id.view_asset_bar_bg);
        }
    }
}