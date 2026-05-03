# ============================================================
# CryptoPulse - ProGuard Rules
# ============================================================

# Giữ thông tin debug
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature

# ── Google Sign-in & GMS ─────────────────────────────────────
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ── Firebase ─────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ── Gson ─────────────────────────────────────────────────────
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**
-dontwarn sun.misc.Unsafe
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── OkHttp ───────────────────────────────────────────────────
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# ── Retrofit ─────────────────────────────────────────────────
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# ── Cloudinary ───────────────────────────────────────────────
-keep class com.cloudinary.** { *; }
-dontwarn com.cloudinary.**

# ── MPAndroidChart ───────────────────────────────────────────
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# ── JSON ─────────────────────────────────────────────────────
-keep class org.json.** { *; }

# FIX ClassCastException: ProGuard đổi tên Activity → crash khi parse JSON
-keep class com.cryptopulse.app.activities.** { *; }
-keep class com.cryptopulse.app.fragments.** { *; }
-keep class com.cryptopulse.app.adapters.** { *; }
-keep class com.cryptopulse.app.viewmodels.** { *; }
-keep class com.cryptopulse.app.models.** { *; }
-keep class com.cryptopulse.app.network.** { *; }
-keep class com.cryptopulse.app.utils.** { *; }

# Giữ fields trong Models để Gson map JSON đúng
-keepclassmembers class com.cryptopulse.app.models.** {
    <fields>;
}