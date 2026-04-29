package com.cryptopulse.app.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.cryptopulse.app.R;
import com.cryptopulse.app.adapters.TransactionAdapter;
import com.cryptopulse.app.adapters.WalletAssetAdapter;
import com.cryptopulse.app.models.*;
import com.cryptopulse.app.network.BinanceWebSocketManager;
import com.cryptopulse.app.viewmodels.MarketViewModel;
import com.cryptopulse.app.utils.AnimUtils;
import java.util.*;

public class WalletFragment extends Fragment implements BinanceWebSocketManager.TickerListener {

    private static final String[] NETWORKS = {
            "Ethereum","BNB Smart Chain","Polygon",
            "Avalanche","Arbitrum","Optimism",
            "Base","Fantom","Cronos","zkSync Era",
            "Linea","Scroll","Moonbeam","Moonriver"
    };

    private static final String[] NET_KEYS = {
            "eth","bsc","polygon",
            "avalanche","arbitrum","optimism",
            "base","fantom","cronos","zksync",
            "linea","scroll","moonbeam","moonriver"
    };

    private static final String[] NET_LOGOS = {
            "https://cryptologos.cc/logos/ethereum-eth-logo.png",
            "https://cryptologos.cc/logos/bnb-bnb-logo.png",
            "https://cryptologos.cc/logos/polygon-matic-logo.png",
            "https://cryptologos.cc/logos/avalanche-avax-logo.png",
            "https://cryptologos.cc/logos/arbitrum-arb-logo.png",
            "https://cryptologos.cc/logos/optimism-ethereum-op-logo.png",
            "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/base/info/logo.png",
            "https://cryptologos.cc/logos/fantom-ftm-logo.png",
            "https://cryptologos.cc/logos/cronos-cro-logo.png",
            "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/zksync/info/logo.png",
            "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/linea/info/logo.png",
            "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/scroll/info/logo.png",
            "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/moonbeam/info/logo.png",
            "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/moonriver/info/logo.png"
    };

    private static final int[] NET_COLORS = {
            0xFF627EEA, 0xFFF3BA2F, 0xFF8247E5, 0xFFE84142, 0xFF28A0F0, 0xFFFF0420,
            0xFF0052FF, 0xFF1969FF, 0xFF002D74, 0xFF8C8DFC, 0xFFFFFFFF, 0xFFFFBF00,
            0xFF1EBDFF, 0xFF53CBC8
    };

    private TextView tvTotalValue, tvTotalLabel;
    private TextView tvTxCount, tvTxSent, tvTxReceived;
    private TextView tvTotalIn, tvTotalOut, tvNetFlow, tvEmptyState;
    private View progressBar, cardPortfolio, cardStats;
    private EditText etWalletAddress;
    private Spinner spinnerNetwork;
    private MarketViewModel viewModel;

    private WalletAssetAdapter assetAdapter;
    private TransactionAdapter txAdapter;
    private final List<WalletAsset> assets = new ArrayList<>();
    private final List<Transaction> transactionList = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_wallet, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        viewModel = new ViewModelProvider(requireActivity()).get(MarketViewModel.class);

        bindViews(v);
        setupNetworkSpinner();
        setupRecyclerViews(v);
        observeWalletData();
        observeErrors();
        observeLoading();

        v.findViewById(R.id.btn_track).setOnClickListener(btn -> {
            AnimUtils.scalePress(btn);
            performWalletLookup();
        });
    }

    private void bindViews(View v) {
        tvTotalValue   = v.findViewById(R.id.tv_total_value);
        tvTotalLabel   = v.findViewById(R.id.tv_total_label);
        tvTxCount      = v.findViewById(R.id.tv_tx_count);
        tvTxSent       = v.findViewById(R.id.tv_tx_sent);
        tvTxReceived   = v.findViewById(R.id.tv_tx_received);
        tvTotalIn      = v.findViewById(R.id.tv_total_in);
        tvTotalOut     = v.findViewById(R.id.tv_total_out);
        tvNetFlow      = v.findViewById(R.id.tv_net_flow);
        tvEmptyState   = v.findViewById(R.id.tv_empty_state);
        progressBar    = v.findViewById(R.id.progress_loading);
        cardPortfolio  = v.findViewById(R.id.card_portfolio);
        cardStats      = v.findViewById(R.id.card_stats);
        etWalletAddress = v.findViewById(R.id.et_wallet_address);
        spinnerNetwork  = v.findViewById(R.id.spinner_network);
    }

    private void setupNetworkSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(), R.layout.item_network_spinner, NETWORKS) {

            @Override public View getView(int p, View cv, ViewGroup vg) { return buildRow(p, cv, vg); }
            @Override public View getDropDownView(int p, View cv, ViewGroup vg) { return buildRow(p, cv, vg); }

            private View buildRow(int pos, View cv, ViewGroup parent) {
                if (cv == null) cv = LayoutInflater.from(getContext()).inflate(R.layout.item_network_spinner, parent, false);
                ImageView iv = cv.findViewById(R.id.iv_network_logo);
                TextView tv = cv.findViewById(R.id.tv_network_name);
                View dot = cv.findViewById(R.id.view_net_dot);

                tv.setText(NETWORKS[pos]);
                if (dot != null) {
                    android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                    gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                    gd.setColor(NET_COLORS[pos]);
                    dot.setBackground(gd);
                }
                Glide.with(getContext()).load(NET_LOGOS[pos]).transform(new CircleCrop()).error(android.R.drawable.ic_menu_gallery).into(iv);
                return cv;
            }
        };
        spinnerNetwork.setAdapter(adapter);
    }

    private void setupRecyclerViews(View v) {
        RecyclerView rvAssets = v.findViewById(R.id.rv_assets);
        rvAssets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAssets.setNestedScrollingEnabled(false);
        rvAssets.setItemAnimator(null);
        assetAdapter = new WalletAssetAdapter(assets);
        rvAssets.setAdapter(assetAdapter);

        RecyclerView rvTx = v.findViewById(R.id.rv_transactions);
        rvTx.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTx.setNestedScrollingEnabled(false);
        rvTx.setItemAnimator(null);
        txAdapter = new TransactionAdapter(transactionList);
        rvTx.setAdapter(txAdapter);
    }

    private void performWalletLookup() {
        String addr = etWalletAddress.getText().toString().trim();
        if (addr.isEmpty()) {
            showToast("Vui lòng nhập địa chỉ ví");
            return;
        }
        if (!addr.startsWith("0x") || addr.length() < 10) {
            showToast("Địa chỉ ví không hợp lệ (Phải bắt đầu bằng 0x)");
            return;
        }

        String key = NET_KEYS[spinnerNetwork.getSelectedItemPosition()];
        resetStats();
        transactionList.clear();
        assets.clear();
        txAdapter.notifyDataSetChanged();
        assetAdapter.notifyDataSetChanged();
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);

        viewModel.loadWalletTransactions(key, addr, true);
    }

    private void observeLoading() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (progressBar != null) progressBar.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE);
        });
    }

    private void observeErrors() {
        viewModel.getError().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && msg.contains("Wallet") && isAdded()) {
                if (tvTxCount != null) tvTxCount.setText("Lỗi kết nối");
                showToast("Lỗi kết nối API. Vui lòng thử lại sau.");
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void observeWalletData() {
        viewModel.getWalletTxs().observe(getViewLifecycleOwner(), map -> {
            if (map == null || !isAdded()) return;
            try {
                String status = str(map, "status", "");
                if ("error".equals(status)) {
                    if (tvTxCount != null) tvTxCount.setText("Không tìm thấy dữ liệu");
                    showToast(str(map, "message", "Không thể lấy dữ liệu ví"));
                    return;
                }

                double totalPortfolio = 0;
                assets.clear();

                // 1. Phân tích Token Holdings
                if (map.containsKey("portfolio")) {
                    Map<String, Object> pObj = (Map<String, Object>) map.get("portfolio");
                    totalPortfolio = dbl(pObj, "total_usd", 0);
                    Object tObj = pObj.get("tokens");
                    if (tObj instanceof List) {
                        for (Map<String, Object> t : (List<Map<String, Object>>) tObj) {
                            double bal = dbl(t, "balance", 0);
                            double val = dbl(t, "usd_value", 0);
                            String logoUrl = str(t, "logo_url", "");
                            if (bal > 0 || val > 0) {
                                double currentPrice = bal > 0 ? val / bal : 0;
                                WalletAsset wa = new WalletAsset(
                                        str(t, "symbol", "UNK"),
                                        str(t, "name", "Unknown Token"),
                                        "#39FF6E", bal, currentPrice, logoUrl
                                );
                                wa.value = val;
                                assets.add(wa);
                            }
                        }
                    }
                }

                // 2. Phân tích Lịch sử Giao dịch
                Object txObj = map.get("transactions");
                if (!(txObj instanceof List)) {
                    if (tvTxCount != null) tvTxCount.setText("0 giao dịch");
                    showEmptyState();
                    return;
                }

                List<?> rawList = (List<?>) txObj;
                transactionList.clear();

                int sentCount = 0, receivedCount = 0;
                double totalIn = 0, totalOut = 0;
                Map<String, double[]> fallbackMap = new LinkedHashMap<>();

                for (Object obj : rawList) {
                    if (!(obj instanceof Map)) continue;
                    Map<String,Object> item = (Map<String,Object>) obj;

                    String dir = str(item, "direction", "unknown");
                    String symbol = str(item, "token_symbol", "");
                    if (symbol.isEmpty() || "null".equals(symbol)) symbol = "Native";

                    double amount = dbl(item, "amount", 0);
                    double usdVal = dbl(item, "usd_value", 0);
                    double priceAtTx = dbl(item, "price_at_tx", 0);

                    Transaction.Type txType;
                    switch (dir) {
                        case "received":
                        case "mint":
                            txType = Transaction.Type.RECEIVED;
                            receivedCount++;
                            totalIn += usdVal;
                            if (!fallbackMap.containsKey(symbol)) fallbackMap.put(symbol, new double[]{0, 0, priceAtTx});
                            fallbackMap.get(symbol)[0] += amount;
                            if (priceAtTx > 0) fallbackMap.get(symbol)[2] = priceAtTx;
                            break;
                        case "sent":
                            txType = Transaction.Type.SENT;
                            sentCount++;
                            totalOut += usdVal;
                            if (!fallbackMap.containsKey(symbol)) fallbackMap.put(symbol, new double[]{0, 0, priceAtTx});
                            fallbackMap.get(symbol)[1] += amount;
                            if (priceAtTx > 0) fallbackMap.get(symbol)[2] = priceAtTx;
                            break;
                        default: txType = Transaction.Type.SWAP; break;
                    }

                    String from = str(item, "from", "");
                    String to = str(item, "to", "");
                    String addrStr = txType == Transaction.Type.RECEIVED ? "Từ: " + shortAddr(from) : "Đến: " + shortAddr(to);
                    String sign = txType == Transaction.Type.RECEIVED ? "+" : "-";
                    String amtStr = sign + fmtAmount(amount) + " " + symbol;
                    String usdStr = usdVal > 0 ? (txType == Transaction.Type.RECEIVED ? "+$" : "-$") + String.format(Locale.US, "%.2f", usdVal) : "";

                    Transaction.Status txStatus = "1".equals(str(item, "receipt_status", "1")) ? Transaction.Status.SUCCESS : Transaction.Status.FAILED;

                    transactionList.add(new Transaction(
                            str(item, "tx_hash", "--"), txType, txStatus,
                            cap(dir) + " " + symbol, addrStr,
                            fmtTime(str(item, "block_timestamp", "")),
                            amtStr, usdStr
                    ));
                }

                // 3. Fallback ước tính nếu ví rỗng
                boolean isEstimated = false;
                if (assets.isEmpty() && totalPortfolio == 0) {
                    isEstimated = true;
                    for (Map.Entry<String, double[]> e : fallbackMap.entrySet()) {
                        double netAmt = e.getValue()[0] - e.getValue()[1];
                        double price = e.getValue()[2];
                        if (netAmt > 0) {
                            double val = netAmt * price;
                            WalletAsset wa = new WalletAsset(e.getKey(), e.getKey(), "#39FF6E", netAmt, price, "");
                            wa.value = val;
                            assets.add(wa);
                            totalPortfolio += val;
                        }
                    }
                }

                assets.sort((a, b) -> Double.compare(b.value, a.value));
                for (WalletAsset wa : assets) {
                    wa.portfolioRatio = totalPortfolio > 0 ? wa.value / totalPortfolio : 0;
                }

                // 4. Cập nhật UI
                final double fPortfolio = totalPortfolio;
                final double fIn = totalIn;
                final double fOut = totalOut;
                final double fNet = totalIn - totalOut;
                final int fSent = sentCount;
                final int fRecv = receivedCount;
                final int fTotal = transactionList.size();
                final boolean fEstimated = isEstimated;

                requireActivity().runOnUiThread(() -> {
                    tvTotalValue.setText(fPortfolio > 0 ? String.format(Locale.US, "$%,.2f", fPortfolio) : "$0.00");
                    if (tvTotalLabel != null) {
                        tvTotalLabel.setText(fEstimated ? "Ước tính từ lịch sử giao dịch" : "Tổng tài sản từ " + assets.size() + " loại token");
                    }
                    if (tvTxCount != null) tvTxCount.setText(fTotal + " giao dịch");
                    if (tvTxSent != null) tvTxSent.setText("↑ " + fSent);
                    if (tvTxReceived != null) tvTxReceived.setText("↓ " + fRecv);
                    if (tvTotalIn != null) tvTotalIn.setText("+" + String.format(Locale.US, "$%,.2f", fIn));
                    if (tvTotalOut != null) tvTotalOut.setText("-" + String.format(Locale.US, "$%,.2f", fOut));
                    if (tvNetFlow != null) {
                        tvNetFlow.setText((fNet >= 0 ? "+" : "") + String.format(Locale.US, "$%,.2f", fNet));
                        tvNetFlow.setTextColor(fNet >= 0 ? 0xFF39FF6E : 0xFFFF4444);
                    }

                    assetAdapter.notifyDataSetChanged();
                    txAdapter.notifyDataSetChanged();

                    if (fTotal == 0) showEmptyState();
                    else if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);

                    if (cardPortfolio != null) AnimUtils.slideUp(cardPortfolio, 400, 0);
                    if (cardStats != null) AnimUtils.slideUp(cardStats, 400, 100);
                });

            } catch (Exception e) {
                Log.e("Wallet", "Parse error: " + e.getMessage(), e);
                if (isAdded()) showToast("Lỗi xử lý dữ liệu");
            }
        });
    }

    private void resetStats() {
        if (tvTxCount != null) tvTxCount.setText("Đang tải...");
        if (tvTxSent != null) tvTxSent.setText("↑ 0");
        if (tvTxReceived != null) tvTxReceived.setText("↓ 0");
        if (tvTotalIn != null) tvTotalIn.setText("--");
        if (tvTotalOut != null) tvTotalOut.setText("--");
        if (tvNetFlow != null) tvNetFlow.setText("--");
        if (tvTotalValue != null) tvTotalValue.setText("$0.00");
    }

    private void showEmptyState() {
        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("Không tìm thấy giao dịch nào.");
        }
        if (tvTxCount != null) tvTxCount.setText("0 giao dịch");
    }

    private void showToast(String msg) {
        if (isAdded()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private String str(Map<String,Object> m, String k, String def) { return (m != null && m.get(k) != null) ? String.valueOf(m.get(k)) : def; }
    private double dbl(Map<String,Object> m, String k, double def) { try { return (m != null && m.get(k) != null) ? Double.parseDouble(String.valueOf(m.get(k))) : def; } catch (Exception e) { return def; } }
    private String shortAddr(String a) { if (a == null || a.length() < 10) return a != null ? a : "--"; return a.substring(0, 6) + "…" + a.substring(a.length() - 4); }
    private String fmtTime(String iso) { if (iso == null || iso.length() < 16) return "--"; return iso.substring(0, 10) + " " + iso.substring(11, 16); }

    private String fmtAmount(double v) {
        if (v == 0) return "0";
        if (v < 0.000001) return String.format(Locale.US, "%.8f", v);
        if (v < 0.001) return String.format(Locale.US, "%.6f", v);
        if (v < 1) return String.format(Locale.US, "%.4f", v);
        if (v < 1_000_000) return String.format(Locale.US, "%,.3f", v);
        return String.format(Locale.US, "%,.0f", v);
    }

    private String cap(String s) { if (s == null || s.isEmpty()) return ""; return s.substring(0, 1).toUpperCase() + s.substring(1); }

    @Override public void onResume() { super.onResume(); BinanceWebSocketManager.getInstance().addListener(this); }
    @Override public void onPause() { super.onPause(); BinanceWebSocketManager.getInstance().removeListener(this); }
    @Override public void onTickerUpdate(Map<String, CoinTicker> map) {}
    @Override public void onConnected() {}
    @Override public void onDisconnected() {}
}