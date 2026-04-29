package com.cryptopulse.app.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.cryptopulse.app.R;
import com.cryptopulse.app.utils.AnimUtils;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Ánh xạ các view từ file XML
        etUsername = findViewById(R.id.et_reg_username);
        etEmail = findViewById(R.id.et_reg_email);
        etPassword = findViewById(R.id.et_reg_password);

        // Hiệu ứng trượt mượt mà cho đồng bộ với trang Login
        AnimUtils.slideUp(findViewById(R.id.register_logo),  600, 100);
        AnimUtils.slideUp(findViewById(R.id.register_title), 600, 200);
        AnimUtils.slideUp(findViewById(R.id.register_card),  700, 300);

        // Xử lý sự kiện nút Đăng ký
        findViewById(R.id.btn_register).setOnClickListener(v -> {
            AnimUtils.scalePress(v);

            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            // Validate dữ liệu đầu vào cơ bản
            if (TextUtils.isEmpty(username)) {
                etUsername.setError("Vui lòng nhập tên đăng nhập");
                return;
            }
            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Email không hợp lệ");
                return;
            }
            if (password.length() < 6) {
                etPassword.setError("Mật khẩu phải từ 6 ký tự");
                return;
            }

            // TODO: Chỗ này sẽ gắn hàm gọi API Retrofit gửi lên FastAPI
            // Tạm thời giả lập thành công:
            Toast.makeText(this, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
            finish(); // Đóng trang Đăng ký, quay về trang Đăng nhập
        });

        // Xử lý nút "Đã có tài khoản? Đăng nhập"
        findViewById(R.id.tv_go_login).setOnClickListener(v -> {
            finish(); // Đóng trang hiện tại để lùi về trang Login
        });
    }
}