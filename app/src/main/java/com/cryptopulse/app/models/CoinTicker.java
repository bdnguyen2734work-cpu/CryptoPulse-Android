package com.cryptopulse.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;

public class CoinTicker {

    // ── Binance WebSocket fields ───────────────────────────────────
    @SerializedName("s") public String symbol;
    @SerializedName("c") public String close;
    @SerializedName("o") public String open;
    @SerializedName("h") public String high;
    @SerializedName("l") public String low;
    @SerializedName("v") public String volume;        // số lượng coin
    @SerializedName("q") public String quoteVolume;   // volume tính bằng USDT ← THÊM
    @SerializedName("P") public String changePercent;
    @SerializedName("E") public long   eventTime;

    // ── Getters ───────────────────────────────────────────────────
    public double  getPrice()        { return parse(close); }
    public double  getOpen()         { return parse(open); }
    public double  getHigh()         { return parse(high); }
    public double  getLow()          { return parse(low); }
    public double  getVolume()       { return parse(volume); }
    public double  getQuoteVolume()  { return parse(quoteVolume); }
    public double  getChangePct()    { return parse(changePercent); }
    public boolean isPositive()      { return getChangePct() >= 0; }

    public String getDisplaySymbol() {
        if (symbol == null) return "";
        if (symbol.equals("MATICUSDT") || symbol.equals("POLUSDT")) return "POL";
        return symbol.endsWith("USDT") ? symbol.replace("USDT", "") : symbol;
    }

    public String getIconLetter() {
        String s = getDisplaySymbol();
        return s.isEmpty() ? "?" : String.valueOf(s.charAt(0));
    }

    public String getFormattedQuoteVolume() {
        double v = getQuoteVolume();
        if (v <= 0) {
            // fallback: price * volume coin
            v = getPrice() * getVolume();
        }
        if (v >= 1_000_000_000) return String.format(java.util.Locale.US, "$%.2fB", v / 1_000_000_000);
        if (v >= 1_000_000)     return String.format(java.util.Locale.US, "$%.2fM", v / 1_000_000);
        if (v >= 1_000)         return String.format(java.util.Locale.US, "$%.2fK", v / 1_000);
        return String.format(java.util.Locale.US, "$%.2f", v);
    }

    private static double parse(String s) {
        if (s == null || s.isEmpty()) return 0.0;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0.0; }
    }

    // ── Static maps ───────────────────────────────────────────────
    private static final Map<String, String>  NAMES  = new HashMap<>();
    private static final Map<String, Integer> COLORS = new HashMap<>();
    private static final Map<String, String>  LOGOS  = new HashMap<>();

    static {
        // ── Names ─────────────────────────────────────────────────
        String[][] nameData = {
                {"BTCUSDT",  "Bitcoin"},           {"ETHUSDT",  "Ethereum"},
                {"BNBUSDT",  "BNB"},               {"SOLUSDT",  "Solana"},
                {"XRPUSDT",  "XRP"},               {"ADAUSDT",  "Cardano"},
                {"DOGEUSDT", "Dogecoin"},           {"AVAXUSDT", "Avalanche"},
                {"DOTUSDT",  "Polkadot"},           {"LINKUSDT", "Chainlink"},
                {"MATICUSDT","Polygon"},            {"POLUSDT",  "Polygon"},
                {"UNIUSDT",  "Uniswap"},            {"ATOMUSDT", "Cosmos"},
                {"LTCUSDT",  "Litecoin"},           {"NEARUSDT", "NEAR"},
                {"APTUSDT",  "Aptos"},              {"ARBUSDT",  "Arbitrum"},
                {"OPUSDT",   "Optimism"},           {"INJUSDT",  "Injective"},
                {"SUIUSDT",  "Sui"},                {"TRXUSDT",  "TRON"},
                {"SHIBUSDT", "Shiba Inu"},          {"BCHUSDT",  "Bitcoin Cash"},
                {"ICPUSDT",  "Internet Computer"},
        };
        for (String[] d : nameData) {
            NAMES.put(d[0], d[1]);
            NAMES.put(d[0].replace("USDT", ""), d[1]);
        }

        // ── Colors ────────────────────────────────────────────────
        int[][] colorData = {
                // symbol hash → color
        };
        COLORS.put("BTCUSDT",  0xFFF7931A);
        COLORS.put("ETHUSDT",  0xFF627EEA);
        COLORS.put("BNBUSDT",  0xFFF3BA2F);
        COLORS.put("SOLUSDT",  0xFF9945FF);
        COLORS.put("XRPUSDT",  0xFF00AAE4);
        COLORS.put("ADAUSDT",  0xFF0033AD);
        COLORS.put("DOGEUSDT", 0xFFC2A633);
        COLORS.put("AVAXUSDT", 0xFFE84142);
        COLORS.put("DOTUSDT",  0xFFE6007A);
        COLORS.put("LINKUSDT", 0xFF2A5ADA);
        COLORS.put("MATICUSDT",0xFF8247E5);
        COLORS.put("POLUSDT",  0xFF8247E5);
        COLORS.put("UNIUSDT",  0xFFFF007A);
        COLORS.put("ATOMUSDT", 0xFF6F7CBA);
        COLORS.put("LTCUSDT",  0xFF345D9D);
        COLORS.put("NEARUSDT", 0xFF00C08B);
        COLORS.put("APTUSDT",  0xFF2DD8A3);
        COLORS.put("ARBUSDT",  0xFF28A0F0);
        COLORS.put("OPUSDT",   0xFFFF0420);
        COLORS.put("INJUSDT",  0xFF00BCFF);
        COLORS.put("SUIUSDT",  0xFF6FBCF0);
        COLORS.put("TRXUSDT",  0xFFEF0027);
        COLORS.put("SHIBUSDT", 0xFFFF6B00);
        COLORS.put("BCHUSDT",  0xFF0AC18E);
        COLORS.put("ICPUSDT",  0xFF29ABE2);

        // ── Logos ─────────────────────────────────────────────────
        String b = "https://coin-images.coingecko.com/coins/images/";
        LOGOS.put("BTCUSDT",  b+"1/large/bitcoin.png");
        LOGOS.put("ETHUSDT",  b+"279/large/ethereum.png");
        LOGOS.put("BNBUSDT",  b+"825/large/bnb-icon2_2x.png");
        LOGOS.put("SOLUSDT",  b+"4128/large/solana.png");
        LOGOS.put("XRPUSDT",  b+"44/large/xrp-symbol-white-128.png");
        LOGOS.put("ADAUSDT",  b+"975/large/cardano.png");
        LOGOS.put("DOGEUSDT", b+"5/large/dogecoin.png");
        LOGOS.put("AVAXUSDT", b+"12559/large/Avalanche_Circle_RedWhite_Trans.png");
        LOGOS.put("DOTUSDT",  b+"12171/large/polkadot.png");
        LOGOS.put("LINKUSDT", b+"877/large/chainlink-new-logo.png");
        LOGOS.put("POLUSDT",  b+"32440/large/polygon.png");
        LOGOS.put("MATICUSDT",b+"32440/large/polygon.png");
        LOGOS.put("UNIUSDT",  b+"12504/large/uniswap-uni.png");
        LOGOS.put("ATOMUSDT", b+"1481/large/cosmos_hub.png");
        LOGOS.put("LTCUSDT",  b+"2/large/litecoin.png");
        LOGOS.put("NEARUSDT", b+"10365/large/near.jpg");
        LOGOS.put("APTUSDT",  b+"26455/large/aptos_round.png");
        LOGOS.put("ARBUSDT",  b+"16547/large/photo_2023-03-29_21.47.00.jpeg");
        LOGOS.put("OPUSDT",   b+"25244/large/Optimism.png");
        LOGOS.put("INJUSDT",  b+"12882/large/Secondary_Symbol.png");
        LOGOS.put("SUIUSDT",  b+"26375/large/sui-ocean-square.png");
        LOGOS.put("TRXUSDT",  b+"1094/large/tron-logo.png");
        LOGOS.put("SHIBUSDT", b+"11939/large/shiba.png");
        LOGOS.put("BCHUSDT",  b+"780/large/bitcoin-cash-circle.png");
        LOGOS.put("ICPUSDT",  b+"14495/large/Internet_Computer_logo.png");
    }

    public String getCoinName() {
        return NAMES.containsKey(symbol) ? NAMES.get(symbol) : getDisplaySymbol();
    }

    public int getCoinColor() {
        return COLORS.containsKey(symbol) ? COLORS.get(symbol) : 0xFF39FF6E;
    }

    public String getLogoUrl() {
        if (symbol == null) return "";
        String s = symbol.toUpperCase().trim();
        if (LOGOS.containsKey(s)) return LOGOS.get(s);
        return LOGOS.getOrDefault(getDisplaySymbol().toUpperCase().trim(), "");
    }
}