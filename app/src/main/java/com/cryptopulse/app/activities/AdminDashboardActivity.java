package com.cryptopulse.app.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cryptopulse.app.R;
import com.cryptopulse.app.network.ApiClient;
import com.cryptopulse.app.utils.AppPrefs;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalUsers, tvNewUsers, tvTotalNews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Nút Back
        findViewById(R.id.btn_back_dashboard).setOnClickListener(v -> finish());

        // Ánh xạ View
        tvTotalUsers = findViewById(R.id.tv_stat_total_users);
        tvNewUsers   = findViewById(R.id.tv_stat_new_users);
        tvTotalNews  = findViewById(R.id.tv_stat_total_news);

        // Gọi API tải dữ liệu
        loadStatistics();
    }

    private void loadStatistics() {
        String token = "Bearer " + AppPrefs.get().getJwtToken();

        ApiClient.get().getAdminStats(token).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // Backend trả về mảng "stats"
                        LinkedTreeMap<String, Double> stats = (LinkedTreeMap<String, Double>) response.body().get("stats");

                        if (stats != null) {
                            // Backend trả về số kiểu Double, nên ta ép kiểu sang int để hiển thị đẹp
                            int totalUsers = stats.get("total_users").intValue();
                            int newUsersWeek = stats.get("new_users_week").intValue();
                            int totalNews = stats.get("total_news").intValue();

                            tvTotalUsers.setText(String.valueOf(totalUsers));
                            tvNewUsers.setText(String.valueOf(newUsersWeek));
                            tvTotalNews.setText(String.valueOf(totalNews));
                        }

                    } catch (Exception e) {
                        Toast.makeText(AdminDashboardActivity.this, "Lỗi phân tích số liệu!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AdminDashboardActivity.this, "Không lấy được số liệu!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}