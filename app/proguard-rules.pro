# Menjaga seluruh package proyek agar tidak terhapus oleh R8
-keep class com.bilangannumerasiapp.** { *; }
-keep class com.ujiannet.id.** { *; }
-keep class com.game.sos.** { *; }

# Menjaga JavaScript Interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# Menjaga AdMob & Play Integrity (Diperbarui agar lebih komprehensif)
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
-keep class com.pairip.** { *; }
-keepclassmembers class com.pairip.** { *; }
-dontwarn com.pairip.**

# === TAMBAHAN PENTING KHUSUS NATIVE ADS ===

# 1. Menjaga Google Mobile Ads Native Templates (Jika Anda menggunakannya)
-keep class com.google.android.ads.nativetemplates.** { *; }
-dontwarn com.google.android.ads.nativetemplates.**

# 2. Menjaga Anotasi, Signature, dan Inner Classes untuk SDK Ads
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 3. Mencegah obfuscation pada View kustom yang dipakai Native Ad
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}
