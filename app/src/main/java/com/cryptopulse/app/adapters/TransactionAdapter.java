package com.cryptopulse.app.adapters;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.cryptopulse.app.R;
import com.cryptopulse.app.models.Transaction;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.VH> {

    private final List<Transaction> data;

    public TransactionAdapter(List<Transaction> data) { this.data = data; }

    @Override public int getItemCount() { return data.size(); }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Transaction t = data.get(pos);

        // Icon + màu theo loại
        String icon;
        int    iconColor;
        switch (t.type) {
            case RECEIVED: icon = "↓"; iconColor = 0xFF39FF6E; break;
            case SENT:     icon = "↑"; iconColor = 0xFFFF4444; break;
            default:       icon = "⇄"; iconColor = 0xFF00BFFF; break;
        }
        h.tvIcon.setText(icon);
        h.tvIcon.setTextColor(iconColor);
        h.tvIcon.setBackgroundColor(iconColor & 0x22FFFFFF);

        h.tvDesc.setText(t.description);
        h.tvAddress.setText(t.address);
        h.tvTime.setText(t.timeAgo);
        h.tvAmount.setText(t.amountStr);
        h.tvAmount.setTextColor(t.type == Transaction.Type.RECEIVED
                ? 0xFF39FF6E : 0xFFFF4444);
        h.tvValue.setText(t.valueStr);

        // Status badge
        switch (t.status) {
            case SUCCESS:
                h.tvStatus.setText("✓ SUCCESS");
                h.tvStatus.setTextColor(0xFF39FF6E);
                break;
            case FAILED:
                h.tvStatus.setText("✗ FAILED");
                h.tvStatus.setTextColor(0xFFFF4444);
                break;
            case PENDING:
                h.tvStatus.setText("⏳ PENDING");
                h.tvStatus.setTextColor(0xFFFFB300);
                break;
        }

        // Expanded detail toggle
        boolean expanded = h.layoutDetail.getVisibility() == View.VISIBLE;
        h.layoutDetail.setVisibility(View.GONE);
        h.tvHash.setText(t.id != null ? t.id : "--");

        h.itemView.setOnClickListener(v -> {
            boolean isExpanded = h.layoutDetail.getVisibility() == View.VISIBLE;
            h.layoutDetail.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            h.tvExpand.setText(isExpanded ? "▼ Chi tiết" : "▲ Thu gọn");
        });
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvIcon, tvDesc, tvAddress, tvTime;
        TextView tvAmount, tvValue, tvStatus, tvExpand;
        View     layoutDetail;
        TextView tvHash;

        VH(View v) {
            super(v);
            tvIcon       = v.findViewById(R.id.tv_tx_icon);
            tvDesc       = v.findViewById(R.id.tv_tx_desc);
            tvAddress    = v.findViewById(R.id.tv_tx_address);
            tvTime       = v.findViewById(R.id.tv_tx_time);
            tvAmount     = v.findViewById(R.id.tv_tx_amount);
            tvValue      = v.findViewById(R.id.tv_tx_value);
            tvStatus     = v.findViewById(R.id.tv_tx_status);
            tvExpand     = v.findViewById(R.id.tv_tx_expand);
            layoutDetail = v.findViewById(R.id.layout_tx_detail);
            tvHash       = v.findViewById(R.id.tv_tx_hash);
        }
    }
}