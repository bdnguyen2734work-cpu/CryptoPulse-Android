package com.cryptopulse.app;

import android.app.Application;
import com.cryptopulse.app.network.ApiClient;
import com.cryptopulse.app.utils.AppPrefs;

public class CryptoPulseApp extends Application {

    private static CryptoPulseApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        AppPrefs.init(this);
        ApiClient.init();
    }

    public static CryptoPulseApp getInstance() {
        return instance;
    }
}
