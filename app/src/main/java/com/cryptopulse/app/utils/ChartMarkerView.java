package com.cryptopulse.app.utils;

import android.content.Context;
import android.widget.TextView;
import com.cryptopulse.app.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import java.util.Locale;

public class ChartMarkerView extends MarkerView {
    private final TextView tvPrice;

    public ChartMarkerView(Context context) {
        super(context, R.layout.custom_marker_view);
        tvPrice = findViewById(R.id.tv_marker_price);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        // Hiện giá trị của cây nến
        tvPrice.setText(String.format(Locale.US, "$%,.2f", e.getY()));
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        // Đặt popup lơ lửng ngay trên đầu cây nến
        return new MPPointF(-(getWidth() / 2f), -getHeight() - 15f);
    }
}