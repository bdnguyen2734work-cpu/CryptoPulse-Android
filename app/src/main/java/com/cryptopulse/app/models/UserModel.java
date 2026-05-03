package com.cryptopulse.app.models;

public class UserModel {
    private int id;
    private String full_name;
    private String email;
    private String role;
    private String status;
    private String avatar_url;

    public int getId() { return id; }
    public String getFullName() { return full_name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public String getAvatarUrl() { return avatar_url; }
}