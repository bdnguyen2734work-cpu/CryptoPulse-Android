package com.cryptopulse.app.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import java.util.Locale;

public class  AnimUtils {

    public static void fadeIn(View v, int durationMs) {
        v.setAlpha(0f);
        v.setVisibility(View.VISIBLE);
        v.animate().alpha(1f).setDuration(durationMs)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    public static void slideUp(View v, int durationMs, int delayMs) {
        v.setTranslationY(60f);
        v.setAlpha(0f);
        v.setVisibility(View.VISIBLE);
        v.animate().translationY(0f).alpha(1f)
                .setDuration(durationMs).setStartDelay(delayMs)
                .setInterpolator(new DecelerateInterpolator(2f)).start();
    }

    public static void scalePress(View v) {
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80).withEndAction(
            () -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        ).start();
    }

    public static void flashPrice(TextView tv, boolean isPositive, int colorPos, int colorNeg) {
        int targetColor = isPositive ? colorPos : colorNeg;
        ObjectAnimator anim = ObjectAnimator.ofArgb(tv, "textColor", 0xFFFFFFFF, targetColor);
        anim.setDuration(400);
        anim.setRepeatCount(1);
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.start();
    }

    public static void animateCounter(TextView tv, double from, double to, int durationMs) {
        ValueAnimator anim = ValueAnimator.ofFloat((float)from, (float)to);
        anim.setDuration(durationMs);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(a -> {
            float v = (float) a.getAnimatedValue();
            tv.setText(String.format(Locale.US, "$%,.2f", v));
        });
        anim.start();
    }
}
