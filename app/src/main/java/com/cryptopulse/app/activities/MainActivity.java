package com.cryptopulse.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.cryptopulse.app.R;
import com.cryptopulse.app.fragments.*;
import com.cryptopulse.app.network.BinanceWebSocketManager;
import com.cryptopulse.app.utils.AppPrefs;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;
    private final Fragment[] fragments = new Fragment[5];
    private int currentIndex = 0;

    private AppPrefs appPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appPrefs = new AppPrefs(this);

        fragments[0] = new HomeFragment();
        fragments[1] = new MarketFragment();
        fragments[2] = new AnalysisFragment();
        fragments[3] = new WalletFragment();
        fragments[4] = new NewsFragment();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, fragments[0], "home")
                .commit();

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            int idx = 0;

            if      (id == R.id.nav_home)     idx = 0;
            else if (id == R.id.nav_market)   idx = 1;
            else if (id == R.id.nav_analysis) idx = 2;
            else if (id == R.id.nav_wallet)   idx = 3;
            else if (id == R.id.nav_news)     idx = 4;

            // ── BỨC TƯỜNG BẢO VỆ ──
            // Chặn người dùng nếu họ vào Phân Tích (idx = 2) hoặc Ví (idx = 3) mà chưa đăng nhập
            if ((idx == 2 || idx == 3) && !appPrefs.isLoggedIn()) {
                Toast.makeText(this, "Vui lòng đăng nhập để sử dụng tính năng này!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                return false; // Trả về false để BottomNav không bị đổi màu/chuyển tab
            }

            switchFragment(idx);
            return true;
        });

        BinanceWebSocketManager.getInstance().connect();
    }

    private void switchFragment(int index) {
        if (index == currentIndex) return;
        Fragment next = fragments[index];
        var tx = getSupportFragmentManager().beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        if (!next.isAdded()) {
            tx.add(R.id.fragment_container, next);
        }
        tx.hide(fragments[currentIndex]).show(next).commit();
        currentIndex = index;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BinanceWebSocketManager.getInstance().disconnect();
    }
}