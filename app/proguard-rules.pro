# =========================================================================
# 1. ATRIBUT INTI JAVA & PELACAKAN CRASH GOOGLE PLAY CONSOLE
# =========================================================================
-keepattributes JavascriptInterface
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# =========================================================================
# 2. PERLINDUNGAN ANDROID WEBVIEW & @JavascriptInterface
# Wajib agar jembatan AndroidAdMob & AndroidApp tetap terbaca dari JS
# =========================================================================
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keepclassmembers public class com.numerasiapp.MainActivity$WebAppAdMobInterface {
    public *;
}

-keep public class com.numerasiapp.MainActivity {
    public *;
}

-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
    public boolean *(android.webkit.WebView, android.webkit.WebResourceRequest);
    public void *(android.webkit.WebView, android.webkit.WebResourceRequest, android.webkit.WebResourceError);
}

-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void *(android.webkit.WebView, int);
}

# =========================================================================
# 3. PERLINDUNGAN LIFECYCLE OBSERVER (APP OPEN AD)
# Wajib untuk ProcessLifecycleOwner & DefaultLifecycleObserver di AdManager
# =========================================================================
-keep class com.numerasiapp.AdManager {
    public *;
}

-keep class * implements androidx.lifecycle.LifecycleObserver {
    <init>(...);
}

-keep class * implements androidx.lifecycle.DefaultLifecycleObserver {
    <init>(...);
}

-keepclassmembers class * implements androidx.lifecycle.LifecycleObserver {
    *;
}

-keepclassmembers class * implements androidx.lifecycle.DefaultLifecycleObserver {
    *;
}

# =========================================================================
# 4. GOOGLE MOBILE ADS SDK (APP OPEN, NATIVE ADVANCED, INTERSTITIAL)
# =========================================================================
-keep public class com.google.android.gms.ads.** {
    public *;
}

-keep public class com.google.ads.** {
    public *;
}

-keep class com.google.android.gms.common.annotation.KeepName
-keep @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembers class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-dontwarn com.google.android.gms.**
-dontwarn com.google.ads.**
