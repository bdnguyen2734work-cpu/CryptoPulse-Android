package com.cryptopulse.app.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.cryptopulse.app.R;
import com.cryptopulse.app.utils.AnimUtils;
import com.cryptopulse.app.utils.AppPrefs;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private TextView tvError;
    private Button btnLogin;
    private AppPrefs prefs;

    private GoogleSignInClient mGoogleSignInClient;

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20,  TimeUnit.SECONDS)
            .build();
    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            String email = account.getEmail();
                            String name = account.getDisplayName();
                            String photoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "";

                            // LẤY ID TOKEN ĐỂ GỬI LÊN BACKEND (Đây là phần quan trọng nhất)
                            String idToken = account.getIdToken();

                            if (idToken != null) {
                                // Đăng nhập thành công, gửi xuống Backend kèm Token
                                doGoogleLoginApi(idToken, email, name, photoUrl);
                            } else {
                                showError("Không lấy được ID Token từ Google. Hãy kiểm tra Web Client ID trong code.");
                            }
                        }
                    } catch (ApiException e) {
                        int statusCode = e.getStatusCode();
                        String errorMsg = "Lỗi Google (Mã: " + statusCode + ")";
                        if (statusCode == 10) {
                            errorMsg += "\n(Lỗi 10: Kiểm tra SHA-1 và Web Client ID trên Firebase Console)";
                        }
                        showError(errorMsg);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        try {
            prefs = AppPrefs.get();
        } catch (Exception e) {
            AppPrefs.init(this);
            prefs = AppPrefs.get();
        }

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        tvError    = findViewById(R.id.tv_error);
        btnLogin   = findViewById(R.id.btn_login);
        String webClientId = "544581672301-7dkon5jq3iquhtbpk0j77h608tcnhjto.apps.googleusercontent.com";

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestIdToken(webClientId)
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mGoogleSignInClient.signOut();

        // ── 2. ĐĂNG NHẬP THƯỜNG ──
        btnLogin.setOnClickListener(v -> {
            String user = etUsername.getText().toString().trim();
            String pass = etPassword.getText().toString();
            if (TextUtils.isEmpty(user)) { etUsername.setError("Nhập tài khoản"); return; }
            if (pass.length() < 4) { etPassword.setError("Mật khẩu quá ngắn"); return; }
            doLogin(user, pass);
        });

        // ── 3. ĐĂNG NHẬP GOOGLE ──
        findViewById(R.id.btn_google).setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        findViewById(R.id.tv_register).setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void doLogin(String username, String password) {
        showLoading(true);
        hideError();
        String url = getBaseUrl() + "/api/v1/auth/login";
        try {
            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("password", password);
            sendAuthRequest(url, body, username);
        } catch (Exception e) { showError(e.getMessage()); }
    }

    // ── GỬI API ĐĂNG NHẬP GOOGLE KÈM ID_TOKEN ──
    private void doGoogleLoginApi(String idToken, String email, String name, String avatarUrl) {
        showLoading(true);
        hideError();
        String url = getBaseUrl() + "/api/v1/auth/google";
        try {
            JSONObject body = new JSONObject();
            body.put("id_token", idToken); // Sửa lỗi "missing id_token" tại đây
            body.put("email", email);
            body.put("name", name != null ? name : "Người dùng Google");
            body.put("avatar", avatarUrl);
            sendAuthRequest(url, body, email);
        } catch (Exception e) { showError(e.getMessage()); }
    }

    private void sendAuthRequest(String url, JSONObject body, String fallbackName) {
        RequestBody requestBody = RequestBody.create(
                body.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request req = new Request.Builder().url(url).post(requestBody).build();

        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> { showLoading(false); showError("Lỗi kết nối!"); });
            }
            @Override public void onResponse(Call call, Response resp) throws IOException {
                String bodyStr = resp.body() != null ? resp.body().string() : "{}";
                runOnUiThread(() -> {
                    showLoading(false);
                    try {
                        JSONObject json = new JSONObject(bodyStr);
                        if (resp.isSuccessful()) {
                            saveSession(json, fallbackName);
                            goToMain();
                        } else {
                            showError(json.optString("detail", "Lỗi xác thực"));
                        }
                    } catch (Exception e) { showError("Lỗi phản hồi hệ thống"); }
                });
            }
        });
    }

    private void saveSession(JSONObject json, String fallbackName) {
        try {
            String token = json.optString("access_token", "");
            JSONObject user = json.optJSONObject("user");
            prefs.setLoggedIn(true);
            prefs.setJwtToken(token);
            if (user != null) {
                prefs.setUserEmail(user.optString("email", ""));
                prefs.setUserName(user.optString("full_name", ""));
                prefs.setUserId(user.optInt("id", 0));
                prefs.setUserAvatar(user.optString("avatar_url", ""));
                prefs.setUserRole(user.optString("role", "user"));
            } else { prefs.setUserEmail(fallbackName); }
        } catch (Exception ignored) {}
    }

    private void goToMain() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void showLoading(boolean show) {
        btnLogin.setEnabled(!show);
        btnLogin.setText(show ? "ĐANG XỬ LÝ..." : "ĐĂNG NHẬP");
    }

    private void showError(String msg) {
        if (tvError != null) { tvError.setText(msg); tvError.setVisibility(View.VISIBLE); }
    }

    private void hideError() { if (tvError != null) tvError.setVisibility(View.GONE); }

    private String getBaseUrl() {
        String url = prefs.getBackendUrl();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}