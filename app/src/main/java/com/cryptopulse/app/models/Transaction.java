package com.cryptopulse.app.models;

public class Transaction {
    public enum Type   { RECEIVED, SENT, SWAP }
    public enum Status { SUCCESS, FAILED, PENDING }

    public final String id;
    public final Type   type;
    public final Status status;
    public final String description;
    public final String address;
    public final String timeAgo;
    public final String amountStr;
    public final String valueStr;

    public Transaction(String id, Type type, Status status, String description,
                       String address, String timeAgo, String amountStr, String valueStr) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.description = description;
        this.address = address;
        this.timeAgo = timeAgo;
        this.amountStr = amountStr;
        this.valueStr = valueStr;
    }
}