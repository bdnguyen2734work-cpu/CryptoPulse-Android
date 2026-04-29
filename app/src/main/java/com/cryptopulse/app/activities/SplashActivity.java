package com.cryptopulse.app.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.appcompat.app.AppCompatActivity;
import com.cryptopulse.app.R;
import com.cryptopulse.app.utils.AppPrefs;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_splash);

        View logo    = findViewById(R.id.splash_logo);
        View tagline = findViewById(R.id.splash_tagline);
        View dots    = findViewById(R.id.splash_loading_dots);

        logo.setAlpha(0f);
        logo.setScaleX(0.7f);
        logo.setScaleY(0.7f);
        logo.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(700)
                .setInterpolator(new DecelerateInterpolator(2f))
                .withEndAction(() -> {
                    tagline.animate().alpha(1f).setDuration(400).start();
                    dots.setVisibility(View.VISIBLE);
                    dots.animate().alpha(1f).setDuration(300).start();
                }).start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AppPrefs appPrefs = new AppPrefs(this);
            Class<?> dest = appPrefs.isLoggedIn()
                    ? MainActivity.class : LoginActivity.class;
            startActivity(new Intent(this, dest));
            overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out);
            finish();
        }, 2_200);
    }
}