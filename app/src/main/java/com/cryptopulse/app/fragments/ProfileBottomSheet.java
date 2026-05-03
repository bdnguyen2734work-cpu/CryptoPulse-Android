package com.cryptopulse.app.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.lifecycle.ViewModelProvider;

import com.cryptopulse.app.R;
import com.cryptopulse.app.activities.AdminDashboardActivity;
import com.cryptopulse.app.activities.AdminPostActivity;
import com.cryptopulse.app.activities.AdminUserActivity;
import com.cryptopulse.app.network.ApiClient;
import com.cryptopulse.app.utils.AppPrefs;
import com.cryptopulse.app.viewmodels.MarketViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.cloudinary.android.MediaManager;

public class ProfileBottomSheet extends BottomSheetDialogFragment {

    private ImageView ivAvatar;
    private TextInputEditText etName, etEmail, etPhone;
    private AppPrefs prefs;
    private MarketViewModel sharedViewModel;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        uploadAvatarToServer(selectedImageUri);
                    }
                }
            }
    );

    public static ProfileBottomSheet newInstance() {
        return new ProfileBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = new AppPrefs(requireContext());
        sharedViewModel = new ViewModelProvider(requireActivity()).get(MarketViewModel.class);

        ivAvatar = view.findViewById(R.id.iv_avatar);
        etName   = view.findViewById(R.id.et_profile_name);
        etEmail  = view.findViewById(R.id.et_profile_email);
        etPhone  = view.findViewById(R.id.et_profile_phone);

        etName.setText(prefs.getUserName());
        etEmail.setText(prefs.getUserEmail());
        etPhone.setText(prefs.getUserPhone());

        loadAvatarSafely(prefs.getUserAvatar());
        fetchLatestUserData();
        setupAdminUI(view);

        ivAvatar.setOnClickListener(v -> showAvatarOptions());
        view.findViewById(R.id.btn_save_profile).setOnClickListener(v -> saveProfileChanges());
        view.findViewById(R.id.btn_logout).setOnClickListener(v -> performLogout());
    }

    private void fetchLatestUserData() {
        String token = "Bearer " + prefs.getJwtToken();
        ApiClient.get().getProfile(token).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> user = (Map<String, Object>) response.body().get("user");
                    if (user != null) {
                        String serverAvatar = (String) user.get("avatar_url");
                        String serverName = (String) user.get("full_name");
                        String serverPhone = (String) user.get("phone");

                        prefs.setUserAvatar(serverAvatar != null ? serverAvatar : "");
                        prefs.setUserName(serverName != null ? serverName : "");
                        prefs.setUserPhone(serverPhone != null ? serverPhone : "");

                        etName.setText(serverName);
                        etPhone.setText(serverPhone);
                        loadAvatarSafely(serverAvatar);
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void loadAvatarSafely(String path) {
        if (path == null || path.isEmpty()) {
            ivAvatar.setImageResource(R.drawable.ic_user);
            ivAvatar.setColorFilter(0xFF00E676);
            return;
        }

        final String finalUrl = path.startsWith("/static") ? prefs.getBackendUrl() + path : path;

        new Thread(() -> {
            try {
                Bitmap bitmap;
                if (finalUrl.startsWith("http")) {
                    bitmap = BitmapFactory.decodeStream(new URL(finalUrl).openConnection().getInputStream());
                } else {
                    InputStream is = requireContext().getContentResolver().openInputStream(Uri.parse(finalUrl));
                    bitmap = BitmapFactory.decodeStream(is);
                }

                if (bitmap != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> formatAndApplyAvatar(bitmap));
                }
            } catch (Exception e) {
                Log.e("AvatarSync", "Lỗi hiển thị ảnh: " + finalUrl);
            }
        }).start();
    }

    private void formatAndApplyAvatar(Bitmap original) {
        int size = Math.min(original.getWidth(), original.getHeight());
        Bitmap cropped = Bitmap.createBitmap(original, (original.getWidth() - size) / 2, (original.getHeight() - size) / 2, size, size);
        Bitmap scaled = Bitmap.createScaledBitmap(cropped, 250, 250, true);

        RoundedBitmapDrawable rbd = RoundedBitmapDrawableFactory.create(getResources(), scaled);
        rbd.setCircular(true);
        rbd.setAntiAlias(true);

        ivAvatar.setImageDrawable(rbd);
        ivAvatar.clearColorFilter();
        ivAvatar.setPadding(0, 0, 0, 0);
    }

    private void uploadAvatarToServer(Uri uri) {
        Toast.makeText(getContext(), "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();

        MediaManager.get().upload(uri)
                .unsigned("cryptopulse_avatars")
                .callback(new com.cloudinary.android.callback.UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String cloudinaryUrl = (String) resultData.get("secure_url");
                        if (cloudinaryUrl == null) return;

                        // Lưu local
                        prefs.setUserAvatar(cloudinaryUrl);
                        loadAvatarSafely(cloudinaryUrl);

                        // Gửi URL lên backend để lưu vào DB
                        Map<String, String> avatarBody = new HashMap<>();
                        avatarBody.put("avatar_url", cloudinaryUrl);

                        ApiClient.get().uploadAvatar("Bearer " + prefs.getJwtToken(), avatarBody)
                                .enqueue(new Callback<Map<String, Object>>() {
                                    @Override
                                    public void onResponse(Call<Map<String, Object>> call,
                                                           Response<Map<String, Object>> response) {
                                        if (sharedViewModel != null) sharedViewModel.triggerProfileUpdate();
                                        if (getContext() != null)
                                            Toast.makeText(getContext(), "Đổi ảnh thành công!", Toast.LENGTH_SHORT).show();
                                    }
                                    @Override
                                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                                });
                    }

                    @Override
                    public void onError(String requestId, com.cloudinary.android.callback.ErrorInfo error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Lỗi upload: " + error.getDescription(),
                                            Toast.LENGTH_SHORT).show());
                        }
                    }

                    @Override
                    public void onReschedule(String requestId, com.cloudinary.android.callback.ErrorInfo error) {}
                })
                .dispatch();
    }

    private void setupAdminUI(View view) {
        boolean isAdmin = "admin".equals(prefs.getUserRole());
        int v = isAdmin ? View.VISIBLE : View.GONE;
        view.findViewById(R.id.btn_admin_post_news).setVisibility(v);
        view.findViewById(R.id.btn_admin_user).setVisibility(v);
        view.findViewById(R.id.btn_admin_dashboard).setVisibility(v);

        view.findViewById(R.id.btn_admin_post_news).setOnClickListener(btn -> { startActivity(new Intent(requireContext(), AdminPostActivity.class)); dismiss(); });
        view.findViewById(R.id.btn_admin_user).setOnClickListener(btn -> { startActivity(new Intent(requireContext(), AdminUserActivity.class)); dismiss(); });
        view.findViewById(R.id.btn_admin_dashboard).setOnClickListener(btn -> { startActivity(new Intent(requireContext(), AdminDashboardActivity.class)); dismiss(); });
    }

    private void showAvatarOptions() {
        String[] opts = {"🖼 Chọn ảnh mới", "🗑 Xóa ảnh hiện tại"};
        new android.app.AlertDialog.Builder(requireContext()).setTitle("Ảnh đại diện").setItems(opts, (d, w) -> {
            if (w == 0) imagePickerLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
            else requestDeleteAvatar();
        }).show();
    }

    private void saveProfileChanges() {
        String n = etName.getText().toString().trim();
        String p = etPhone.getText().toString().trim();
        Map<String, String> body = new HashMap<>();
        body.put("full_name", n); body.put("phone", p);

        ApiClient.get().updateProfile("Bearer " + prefs.getJwtToken(), body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    prefs.setUserName(n); prefs.setUserPhone(p);
                    if (sharedViewModel != null) sharedViewModel.triggerProfileUpdate();
                    Toast.makeText(getContext(), "Đã lưu thay đổi!", Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void performLogout() {
        prefs.setLoggedIn(false); prefs.setJwtToken(""); prefs.setUserRole("user"); prefs.setUserAvatar("");
        sharedViewModel.setLoginState(false); dismiss();
    }

    private void requestDeleteAvatar() {
        ApiClient.get().deleteAvatar("Bearer " + prefs.getJwtToken()).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    prefs.setUserAvatar(""); loadAvatarSafely("");
                    if (sharedViewModel != null) sharedViewModel.triggerProfileUpdate();
                    Toast.makeText(getContext(), "Đã xóa ảnh!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }
}