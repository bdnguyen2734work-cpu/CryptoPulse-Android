package com.cryptopulse.app.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class FearGreedGaugeView extends View {

    private Paint trackPaint, dotPaint, valuePaint, labelPaint;
    private final RectF arcRect = new RectF();

    private int displayValue = 50;
    private String label = "Neutral";

    // 5 Mức màu chuẩn xác theo biểu đồ gốc
    private final int[] SEGMENT_COLORS = {
            0xFFF44336, // 0-24: Extreme Fear (Đỏ)
            0xFFFF9800, // 25-46: Fear (Cam)
            0xFFFFEB3B, // 47-52: Neutral (Vàng chanh)
            0xFF8BC34A, // 53-74: Greed (Xanh lục nhạt)
            0xFF4CAF50  // 75-100: Extreme Greed (Xanh lục đậm)
    };

    public FearGreedGaugeView(Context ctx) {
        super(ctx);
        init();
    }

    public FearGreedGaugeView(Context ctx, AttributeSet a) {
        super(ctx, a);
        init();
    }

    public FearGreedGaugeView(Context ctx, AttributeSet a, int s) {
        super(ctx, a, s);
        init();
    }

    private void init() {
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.WHITE);
        dotPaint.setStyle(Paint.Style.FILL);
        // Đổ bóng mềm mại cho cục tròn để tạo cảm giác nổi khối (3D)
        dotPaint.setShadowLayer(10f, 0, 4f, 0x55000000);

        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setColor(Color.WHITE);
        valuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setColor(0xFF90CAF9); // Màu xanh dương nhạt cho chữ phụ

        // Bắt buộc bật để hiệu ứng đổ bóng (shadowLayer) có tác dụng
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setValue(int value, String label) {
        this.label = label;
        int from = this.displayValue;
        int to = Math.max(0, Math.min(100, value));

        ValueAnimator anim = ValueAnimator.ofInt(from, to);
        anim.setDuration(1000);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            displayValue = (int) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();

        // Độ dày thanh màu
        float stroke = Math.min(w, h) * 0.08f;
        trackPaint.setStrokeWidth(stroke);
        float safeTop = stroke * 1.5f + 10f;

        float safeBottom = stroke + 35f;

        float radius = Math.min((w - 2 * safeTop) / 2f, h - safeTop - safeBottom);

        float cx = w / 2f;
        float cy = h - safeBottom;

        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius);

        int numSegments = 5;
        float gapAngle = 2f;
        float totalGaps = gapAngle * (numSegments - 1);
        float segmentAngle = (180f - totalGaps) / numSegments;

        float currentAngle = 180f;
        for (int i = 0; i < numSegments; i++) {
            trackPaint.setColor(SEGMENT_COLORS[i]);
            canvas.drawArc(arcRect, currentAngle, segmentAngle, false, trackPaint);
            currentAngle += segmentAngle + gapAngle;
        }
        valuePaint.setTextSize(radius * 0.48f);
        canvas.drawText(String.valueOf(displayValue), cx, cy - radius * 0.05f, valuePaint);
        labelPaint.setTextSize(radius * 0.28f);
        canvas.drawText(label, cx, cy + radius * 0.32f, labelPaint);
        float valAngleDeg = 180f + (displayValue / 100f) * 180f;
        double valAngleRad = Math.toRadians(valAngleDeg);

        float dotX = cx + (float) (radius * Math.cos(valAngleRad));
        float dotY = cy + (float) (radius * Math.sin(valAngleRad));

        float dotR = stroke * 0.85f;
        canvas.drawCircle(dotX, dotY, dotR, dotPaint);
    }
}