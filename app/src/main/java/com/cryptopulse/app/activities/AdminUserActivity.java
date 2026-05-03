package com.cryptopulse.app.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cryptopulse.app.R;
import com.cryptopulse.app.models.UserModel;
import com.cryptopulse.app.network.ApiClient;
import com.cryptopulse.app.utils.AppPrefs;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminUserActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private UserAdapter adapter;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user);

        token = "Bearer " + AppPrefs.get().getJwtToken();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        rvUsers = findViewById(R.id.rv_users);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(new ArrayList<>());
        rvUsers.setAdapter(adapter);

        loadUsers();
    }

    private void loadUsers() {
        ApiClient.get().getAllUsers(token, 1, 50, "").enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Object usersObj = response.body().get("users");
                        Gson gson = new Gson();
                        String json = gson.toJson(usersObj);
                        List<UserModel> userList = gson.fromJson(json, new TypeToken<List<UserModel>>() {
                        }.getType());

                        // Sắp xếp Admin lên đầu
                        Collections.sort(userList, (u1, u2) -> {
                            boolean isU1Admin = "admin".equalsIgnoreCase(u1.getRole());
                            boolean isU2Admin = "admin".equalsIgnoreCase(u2.getRole());
                            if (isU1Admin && !isU2Admin) return -1;
                            if (!isU1Admin && isU2Admin) return 1;
                            return 0;
                        });

                        adapter.setUsers(userList);
                    } catch (Exception e) {
                        Toast.makeText(AdminUserActivity.this, "Lỗi dữ liệu!", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
            }
        });
    }

    private void deleteUser(int userId, int position) {
        ApiClient.get().deleteUser(token, userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    adapter.removeUser(position);
                    Toast.makeText(AdminUserActivity.this, "Đã xóa User thành công!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
            }
        });
    }

    class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {
        private final List<UserModel> users;

        UserAdapter(List<UserModel> users) {
            this.users = users;
        }

        void setUsers(List<UserModel> list) {
            users.clear();
            users.addAll(list);
            notifyDataSetChanged();
        }

        void removeUser(int position) {
            users.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, users.size());
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            UserModel user = users.get(position);

            holder.tvName.setText(user.getFullName() != null ? user.getFullName() : "No Name");
            holder.tvEmail.setText(user.getEmail());

            loadUserAvatar(user.getAvatarUrl(), holder.ivAvatar);

            if ("admin".equalsIgnoreCase(user.getRole())) {
                holder.tvRole.setText("ADMIN");
                holder.tvRole.setTextColor(0xFF39FF6E);
                holder.btnDelete.setVisibility(View.GONE);
            } else {
                holder.tvRole.setText("USER");
                holder.tvRole.setTextColor(0xFFA0A0A0);
                holder.btnDelete.setVisibility(View.VISIBLE);
            }

            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(AdminUserActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn muốn xóa người dùng này vĩnh viễn?")
                        .setPositiveButton("Xóa", (d, w) -> deleteUser(user.getId(), position))
                        .setNegativeButton("Hủy", null).show();
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        private void loadUserAvatar(String url, ImageView iv) {
            if (url == null || url.isEmpty()) {
                iv.setImageResource(R.drawable.ic_user);
                iv.setColorFilter(0xFF00E676);
                return;
            }
            final String finalUrl = url.startsWith("/static") ? AppPrefs.get().getBackendUrl() + url : url;
            new Thread(() -> {
                try {
                    java.net.URL imageUrl = new java.net.URL(finalUrl);
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(imageUrl.openConnection().getInputStream());
                    runOnUiThread(() -> {
                        if (bitmap != null) {
                            androidx.core.graphics.drawable.RoundedBitmapDrawable rbd =
                                    androidx.core.graphics.drawable.RoundedBitmapDrawableFactory.create(getResources(), bitmap);
                            rbd.setCircular(true);
                            iv.setImageDrawable(rbd);
                            iv.clearColorFilter();
                        }
                    });
                } catch (Exception ignored) {
                }
            }).start();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvEmail, tvRole;
            ImageView ivAvatar;
            View btnDelete;

            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_user_name);
                tvEmail = v.findViewById(R.id.tv_user_email);
                tvRole = v.findViewById(R.id.tv_role_badge);
                ivAvatar = v.findViewById(R.id.iv_user_avatar);
                btnDelete = v.findViewById(R.id.btn_delete_user);
            }
        }
    }
}