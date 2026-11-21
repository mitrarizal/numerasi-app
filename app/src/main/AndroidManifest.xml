package com.numerasiapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity {

    private static final String BLOGGER_GAME_URL = "https://gamenumerasi.blogspot.com/?m=1";

    private LinearLayout rootContainer;
    private WebView webView;
    private FrameLayout nativeAdContainer;
    private AdManager adManager;
    private boolean isInGameSession = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        rootContainer = findViewById(R.id.root_container);
        webView = findViewById(R.id.game_webview);
        nativeAdContainer = findViewById(R.id.native_ad_container);

        configureEdgeToEdgeAndInsets();

        adManager = new AdManager(this, webView, nativeAdContainer);

        setupWebView();
        setupBackNavigation();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(BLOGGER_GAME_URL);
        }
    }

    /**
     * Kepatuhan Edge-to-Edge wajib SDK 35/36 agar konten dan Native Ad tidak terpotong poni layar.
     */
    private void configureEdgeToEdgeAndInsets() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        ViewCompat.setOnApplyWindowInsetsListener(rootContainer, (view, windowInsets) -> {
            Insets safeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.navigationBars()
            );
            view.setPadding(safeInsets.left, safeInsets.top, safeInsets.right, safeInsets.bottom);
            return windowInsets;
        });

        enableImmersiveFullscreen();
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setBackgroundColor(Color.parseColor("#F0F9FF"));
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // Daftarkan jembatan JavaScript dengan nama AndroidAdMob & AndroidApp
        WebAppAdMobInterface jsBridge = new WebAppAdMobInterface();
        webView.addJavascriptInterface(jsBridge, "AndroidAdMob");
        webView.addJavascriptInterface(jsBridge, "AndroidApp");

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                if (host != null && (host.equals("gamenumerasi.blogspot.com")
                        || host.endsWith(".blogspot.com")
                        || host.endsWith(".blogger.com")
                        || host.endsWith(".script.google.com")
                        || host.endsWith(".googleusercontent.com"))) {
                    return false;
                }

                // Buka tautan eksternal (misal Kebijakan Privasi) di browser sistem secara aman
                try {
                    Intent externalIntent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(externalIntent);
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url != null && url.contains("gamenumerasi.blogspot.com")) {
                    adManager.onContentReady();
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame()) {
                    showOfflineFallbackPage();
                }
            }
        });
    }

    private void showOfflineFallbackPage() {
        String offlineHtml = "<!DOCTYPE html><html lang='id'><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1.0'>"
                + "<style>"
                + "body{margin:0;padding:24px;font-family:sans-serif;background:#F0F9FF;color:#0F172A;"
                + "display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:90vh;text-align:center;}"
                + "img{max-width:180px;height:auto;margin-bottom:16px;}"
                + "h2{font-size:20px;margin:0 0 8px;}"
                + "p{font-size:14px;color:#475569;max-width:320px;margin:0 0 20px;line-height:1.5;}"
                + "button{background:#0284C7;color:#fff;border:none;border-radius:10px;padding:12px 24px;"
                + "font-size:15px;font-weight:bold;cursor:pointer;}"
                + "</style></head><body>"
                + "<img src='file:///android_asset/no_internet.png' alt='Tidak Ada Koneksi'/>"
                + "<h2>Koneksi Internet Terputus</h2>"
                + "<p>Pastikan perangkat terhubung ke internet untuk memuat Petualangan Numerasi.</p>"
                + "<button onclick=\"window.location.href='" + BLOGGER_GAME_URL + "'\">Coba Lagi</button>"
                + "</body></html>";

        webView.loadDataWithBaseURL("file:///android_asset/", offlineHtml, "text/html", "UTF-8", null);
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isInGameSession && webView != null) {
                    // Konfirmasi keluar soal tanpa memicu iklan saat tombol kembali ditekan (aturan AdMob)
                    webView.evaluateJavascript("if(typeof confirmQuitLevel==='function'){confirmQuitLevel();}", null);
                } else if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    private void enableImmersiveFullscreen() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableImmersiveFullscreen();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (adManager != null) adManager.onDestroy();
        if (webView != null) {
            webView.removeAllViews();
            webView.destroy();
        }
        super.onDestroy();
    }

    /**
     * Jembatan metode JavaScript yang dipanggil oleh halaman Blogspot.
     * Fokus hanya pada App Open Ad, Native Ad, dan Interstitial Ad.
     */
    private class WebAppAdMobInterface {

        @JavascriptInterface
        public void configureFamiliesPolicy(boolean childDirected, String maxAdContentRating) {
            String rating = (maxAdContentRating != null && !maxAdContentRating.isEmpty())
                    ? maxAdContentRating : "G";
            adManager.applyFamiliesPolicy(childDirected, rating);
        }

        @JavascriptInterface
        public void onGameContentReady() {
            adManager.onContentReady();
        }

        @JavascriptInterface
        public void onContentLoaded() {
            adManager.onContentReady();
        }

        @JavascriptInterface
        public void setGameplayState(boolean active) {
            isInGameSession = active;
            adManager.setGameplayActive(active);
        }

        @JavascriptInterface
        public void setNativeAdVisible(boolean visible) {
            adManager.setNativeAdVisible(visible);
        }

        @JavascriptInterface
        public void showInterstitial(String placement) {
            adManager.showInterstitialAd(placement != null ? placement : "level_transition");
        }

        @JavascriptInterface
        public void panggilIklanInterstisial() {
            adManager.showInterstitialAd("level_transition");
        }

        // Stub aman agar skrip lama tidak error jika memanggil fungsi banner/rewarded
        @JavascriptInterface
        public int getBannerHeightPx() {
            return 0;
        }

        @JavascriptInterface
        public void showRewardedAd(String rewardType) {
            if (webView != null) {
                webView.post(() -> webView.evaluateJavascript("if(window.onAdClosed){window.onAdClosed();}", null));
            }
        }

        @JavascriptInterface
        public void panggilIklanRewarded() {
            showRewardedAd("none");
        }
    }
}
