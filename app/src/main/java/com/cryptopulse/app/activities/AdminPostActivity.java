package com.cryptopulse.app.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cryptopulse.app.R;
import com.cryptopulse.app.network.ApiClient;
import com.cryptopulse.app.utils.AppPrefs;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminPostActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etTags, etImageUrl, etContent;
    private RadioGroup rgCategory;
    private MaterialButton btnSubmit;
    private AppPrefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_admin_post);

            prefs = AppPrefs.get();

            // Chặn nếu không phải Admin
            if (!"admin".equals(prefs.getUserRole())) {
                Toast.makeText(this, "Bạn không có quyền truy cập!", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Ánh xạ View
            TextView btnBack = findViewById(R.id.btn_back);
            etTitle = findViewById(R.id.et_news_title);
            etTags = findViewById(R.id.et_news_tags);
            etImageUrl = findViewById(R.id.et_news_image_url);
            etContent = findViewById(R.id.et_news_content);
            rgCategory = findViewById(R.id.rg_category);
            btnSubmit = findViewById(R.id.btn_submit_news);

            // Nút Quay Lại
            btnBack.setOnClickListener(v -> finish());

            // Nút Xuất Bản
            btnSubmit.setOnClickListener(v -> submitNews());

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khởi tạo: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("AdminPost", "Lỗi onCreate", e);
        }
    }

    private void submitNews() {
        try {
            // Lấy dữ liệu an toàn
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String tags = etTags.getText() != null ? etTags.getText().toString().trim() : "";
            String imageUrl = etImageUrl.getText() != null ? etImageUrl.getText().toString().trim() : "";
            String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";

            if (TextUtils.isEmpty(title)) {
                etTitle.setError("Không được để trống tiêu đề");
                return;
            }
            if (TextUtils.isEmpty(content)) {
                etContent.setError("Không được để trống nội dung");
                return;
            }
            int categoryId = 3;
            if (rgCategory.getCheckedRadioButtonId() == R.id.rb_vietnam) {
                categoryId = 2;
            }

            btnSubmit.setEnabled(false);
            btnSubmit.setText("ĐANG XỬ LÝ...");

            String token = "Bearer " + prefs.getJwtToken();

            Map<String, Object> body = new HashMap<>();
            body.put("title", title);
            body.put("content", content);
            body.put("image_url", imageUrl);
            body.put("category_id", categoryId);
            body.put("tags", tags);

            ApiClient.get().postNews(token, body).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("XUẤT BẢN TIN TỨC");

                    if (response.isSuccessful()) {
                        Toast.makeText(AdminPostActivity.this, "Đăng tin thành công! 🎉", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(AdminPostActivity.this, "Lỗi Server (Mã " + response.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("XUẤT BẢN TIN TỨC");
                    Toast.makeText(AdminPostActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi Logic: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("AdminPost", "Lỗi submit", e);
            if (btnSubmit != null) {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("XUẤT BẢN TIN TỨC");
            }
        }
    }
}