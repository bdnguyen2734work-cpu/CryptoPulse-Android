package com.cryptopulse.app.models;

public class WalletAsset {
    public String symbol;
    public String name;
    public String color;
    public double balance;
    public double price;
    public double value;
    public double portfolioRatio;
    public String logoUrl;

    public WalletAsset(String symbol, String name, String color,
                       double balance, double price, String logoUrl) {
        this.symbol        = symbol;
        this.name          = name;
        this.color         = color;
        this.balance       = balance;
        this.price         = price;
        this.logoUrl       = logoUrl != null ? logoUrl : "";
        this.value         = balance * price;
        this.portfolioRatio = 0;
    }
}