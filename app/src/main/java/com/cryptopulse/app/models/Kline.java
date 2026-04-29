package com.cryptopulse.app.models;

public class Kline {
    public String symbol;
    public long   openTime;
    public double open, high, low, close, volume;

    public Kline() {}
    public Kline(String sym, long t, double o, double h, double l, double c, double v) {
        this.symbol=sym; this.openTime=t;
        this.open=o; this.high=h; this.low=l; this.close=c; this.volume=v;
    }
    public boolean isGreen() { return close >= open; }
}
