package com.cryptopulse.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class AppPrefs {
    private static final String PREFS_NAME    = "cryptopulse_prefs";
    private static final String KEY_FAVORITES  = "favorites";
    private static final String KEY_LOGGED_IN  = "logged_in";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_BACKEND_URL= "backend_url";
    private static final String DEFAULT_BACKEND = "http://10.0.2.2:8000";

    // --- ĐÃ THÊM: Các Key lưu trữ cho Profile ---
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_AVATAR = "user_avatar";

    private static AppPrefs INSTANCE;

    private final SharedPreferences prefs;

    public AppPrefs(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        //Cài đặt mặc định 4 đồng coin yêu thích cho lần đầu mở app
        if (!prefs.contains(KEY_FAVORITES)) {
            Set<String> defaultFavs = new HashSet<>(java.util.Arrays.asList(
                    "BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT"
            ));
            prefs.edit().putStringSet(KEY_FAVORITES, defaultFavs).apply();
        }
    }

    // ĐÃ THÊM: init() gọi từ Application
    public static void init(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AppPrefs(context.getApplicationContext());
        }
    }

    // ĐÃ THÊM: get() dùng ở bất kỳ đâu không cần Context
    public static AppPrefs get() {
        if (INSTANCE == null) throw new IllegalStateException("AppPrefs chưa được init!");
        return INSTANCE;
    }

    public boolean isLoggedIn() { return prefs.getBoolean(KEY_LOGGED_IN, false); }
    public void setLoggedIn(boolean v) { prefs.edit().putBoolean(KEY_LOGGED_IN, v).apply(); }

    public String getUserEmail() { return prefs.getString(KEY_USER_EMAIL, ""); }
    public void setUserEmail(String email) { prefs.edit().putString(KEY_USER_EMAIL, email).apply(); }

    // --- ĐÃ THÊM: Các hàm Get/Set cho Profile ---
    public String getUserName() { return prefs.getString(KEY_USER_NAME, ""); }
    public void setUserName(String name) { prefs.edit().putString(KEY_USER_NAME, name).apply(); }

    public String getUserPhone() { return prefs.getString(KEY_USER_PHONE, ""); }
    public void setUserPhone(String phone) { prefs.edit().putString(KEY_USER_PHONE, phone).apply(); }

    public String getUserAvatar() { return prefs.getString(KEY_USER_AVATAR, ""); }
    public void setUserAvatar(String uri) { prefs.edit().putString(KEY_USER_AVATAR, uri).apply(); }
    // --------------------------------------------

    public String getBackendUrl() { return prefs.getString(KEY_BACKEND_URL, DEFAULT_BACKEND); }
    public void setBackendUrl(String url) { prefs.edit().putString(KEY_BACKEND_URL, url).apply(); }

    public Set<String> getFavorites() {
        return new HashSet<>(prefs.getStringSet(KEY_FAVORITES, new HashSet<>()));
    }
    public void toggleFavorite(String symbol) {
        Set<String> favs = getFavorites();
        if (favs.contains(symbol)) favs.remove(symbol); else favs.add(symbol);
        prefs.edit().putStringSet(KEY_FAVORITES, favs).apply();
    }
    public boolean isFavorite(String symbol) { return getFavorites().contains(symbol); }
}