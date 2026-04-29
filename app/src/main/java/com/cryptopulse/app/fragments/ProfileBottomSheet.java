package com.cryptopulse.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cryptopulse.app.R;
import com.cryptopulse.app.activities.MainActivity;
import com.cryptopulse.app.utils.AppPrefs;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ProfileBottomSheet extends BottomSheetDialogFragment {

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

        AppPrefs prefs = new AppPrefs(requireContext());

        // 1. Hiển thị Email đang đăng nhập (Nếu có)
        EditText etEmail = view.findViewById(R.id.et_profile_email);
        if (etEmail != null) {
            etEmail.setText(prefs.getUserEmail());
        }

        // 2. Logic nút Đăng xuất
        Button btnLogout = view.findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                // XÓA TRẠNG THÁI ĐĂNG NHẬP
                prefs.setLoggedIn(false);
                prefs.setUserEmail("");

                Toast.makeText(requireContext(), "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();
                dismiss(); // Đóng BottomSheet

                // Khởi động lại MainActivity để hiển thị lại giao diện Khách
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }

        // 3. Logic nút Lưu thay đổi (Tạm thời)
        Button btnSave = view.findViewById(R.id.btn_save_profile);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Đang cập nhật hồ sơ...", Toast.LENGTH_SHORT).show();
                dismiss();
            });
        }
    }
}