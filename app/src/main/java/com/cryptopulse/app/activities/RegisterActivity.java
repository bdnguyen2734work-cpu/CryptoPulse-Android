package com.cryptopulse.app.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.cryptopulse.app.R;
import com.cryptopulse.app.utils.AnimUtils;
import com.cryptopulse.app.utils.AppPrefs;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword;
    private TextView tvError;
    private Button btnRegister;

    // Cấu hình mạng OkHttp
    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20,  TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Ánh xạ View
        etUsername  = findViewById(R.id.et_reg_username);
        etEmail     = findViewById(R.id.et_reg_email);
        etPassword  = findViewById(R.id.et_reg_password);
        tvError     = findViewById(R.id.tv_reg_error);
        btnRegister = findViewById(R.id.btn_register);

        // Hiệu ứng Animation UI
        AnimUtils.slideUp(findViewById(R.id.register_logo),  600, 100);
        AnimUtils.slideUp(findViewById(R.id.register_title), 600, 200);
        AnimUtils.slideUp(findViewById(R.id.register_card),  700, 300);

        // ── Xử lý Nút Đăng ký ─────────────────────────────────────────
        btnRegister.setOnClickListener(v -> {
            AnimUtils.scalePress(v);
            tvError.setVisibility(View.GONE);

            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            // 1. Logic Kiểm tra (Validation) cơ bản
            if (TextUtils.isEmpty(username)) {
                etUsername.setError("Tên đăng nhập không được để trống");
                etUsername.requestFocus();
                return;
            }
            if (username.length() < 3) {
                etUsername.setError("Tên đăng nhập phải từ 3 ký tự trở lên");
                etUsername.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Email không đúng định dạng (VD: abc@gmail.com)");
                etEmail.requestFocus();
                return;
            }
            if (password.length() < 6) {
                etPassword.setError("Mật khẩu bảo mật phải từ 6 ký tự");
                etPassword.requestFocus();
                return;
            }

            if (password.length() > 50) {
                etPassword.setError("Mật khẩu không được vượt quá 50 ký tự");
                etPassword.requestFocus();
                return;
            }

            // Vượt qua kiểm tra -> Gọi API
            doRegister(username, email, password);
        });

        // ── Nút quay lại Đăng nhập ────────────────────────────────
        findViewById(R.id.tv_go_login).setOnClickListener(v -> finish());
    }

    // ══════════════════════════════════════════════════════════
    //  GỌI API ĐĂNG KÝ BẰNG JSON
    // ══════════════════════════════════════════════════════════
    private void doRegister(String username, String email, String password) {
        showLoading(true);

        String url = getBaseUrl() + "/api/v1/auth/register";

        try {
            // Khác với Login dùng Form Data, Register thường dùng JSON chuẩn
            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("email", email);
            body.put("password", password);

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request req = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            http.newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        showError("Lỗi kết nối máy chủ.\n" + e.getMessage());
                    });
                }

                @Override
                public void onResponse(Call call, Response resp) throws IOException {
                    String bodyStr = resp.body() != null ? resp.body().string() : "{}";

                    runOnUiThread(() -> {
                        showLoading(false);
                        try {
                            if (resp.isSuccessful()) {
                                Toast.makeText(RegisterActivity.this, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();
                                finish(); // Đóng màn hình đăng ký, quay về đăng nhập
                            } else {
                                JSONObject json = new JSONObject(bodyStr);
                                String detail = json.optString("detail", "Tên đăng nhập hoặc Email đã tồn tại!");
                                showError(detail);
                            }
                        } catch (Exception e) {
                            showError("Lỗi đọc dữ liệu từ Server.");
                        }
                    });
                }
            });

        } catch (Exception e) {
            showLoading(false);
            showError("Lỗi: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  UI HELPERS
    // ══════════════════════════════════════════════════════════
    private void showLoading(boolean show) {
        btnRegister.setEnabled(!show);
        btnRegister.setText(show ? "ĐANG XỬ LÝ..." : "ĐĂNG KÝ TÀI KHOẢN");
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private String getBaseUrl() {
        String url = AppPrefs.get().getBackendUrl();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}