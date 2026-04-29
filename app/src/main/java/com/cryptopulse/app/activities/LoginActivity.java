package com.cryptopulse.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.cryptopulse.app.R;
import com.cryptopulse.app.utils.AnimUtils;
import com.cryptopulse.app.utils.AppPrefs;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private AppPrefs appPrefs;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_login);

        appPrefs = new AppPrefs(this);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);

        AnimUtils.slideUp(findViewById(R.id.login_logo),     600, 100);
        AnimUtils.slideUp(findViewById(R.id.login_title),    600, 200);
        AnimUtils.slideUp(findViewById(R.id.login_subtitle), 600, 280);
        AnimUtils.slideUp(findViewById(R.id.login_card),     700, 350);

        // Logic Nút Đăng Nhập
        findViewById(R.id.btn_login).setOnClickListener(v -> {
            AnimUtils.scalePress(v);
            String username = etUsername.getText().toString().trim();
            String pass  = etPassword.getText().toString();

            if (TextUtils.isEmpty(username)) {
                etUsername.setError("Vui lòng nhập tài khoản");
                return;
            }
            if (pass.length() < 4) {
                etPassword.setError("Mật khẩu ít nhất 4 ký tự");
                return;
            }
            login(username);
        });

        // Logic Nút Google
        findViewById(R.id.btn_google).setOnClickListener(v -> {
            AnimUtils.scalePress(v);
            // Tạm thời giả lập đăng nhập Google
            login("google_user");
        });

        // Chuyển sang màn hình Đăng ký
        findViewById(R.id.tv_register).setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    // Hàm giả lập Đăng nhập thành công (sẽ sửa lại khi gọi API thật)
    private void login(String username) {
        appPrefs.setLoggedIn(true);
        appPrefs.setUserEmail(username); // Tạm dùng biến này để lưu Tên đăng nhập

        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}