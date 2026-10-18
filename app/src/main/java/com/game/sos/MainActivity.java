package com.numerasiapp;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private AdView adView;
    private InterstitialAd interstitialAd;
    private ProgressBar progressBar;

    private boolean isInterstitialLoading = false;
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6983364109428063/3679555743";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // KUNCI 1: Ubah Window Background aplikasi ke #0b0f19 agar TIDAK ADA layar putih saat loading/kirim jawaban
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#0b0f19")));

        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);

        // 1. Inisialisasi SDK AdMob
        MobileAds.initialize(this, initializationStatus -> {
            loadInterstitialAd(); 
        });

        // 2. Banner Ad
        adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.loadAd(new AdRequest.Builder().build());
        }

        // 3. Konfigurasi WebView
        webView = findViewById(R.id.webview);
        
        webView.setVisibility(View.VISIBLE);
        // KUNCI 2: Gunakan LAYER_TYPE_HARDWARE untuk performa smooth HTML5 Canvas
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setBackgroundColor(Color.parseColor("#0b0f19"));

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // Aktifkan Cookie
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        webView.addJavascriptInterface(this, "AndroidApp");

        // Progress bar
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBar != null) {
                    if (newProgress < 100) {
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setProgress(newProgress);
                    } else {
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false; // Mengizinkan semua navigasi di dalam WebView tanpa memicu browser eksternal
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    String html = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                            + "<style>body{display:flex;justify-content:center;align-items:center;height:100vh;margin:0;background-color:#0b0f19;}"
                            + "img{max-width:80%;height:auto;}</style></head>"
                            + "<body><img src='file:///android_asset/no_internet.png'></body></html>";
                    view.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
                }
            }

            @Override
            public boolean onRenderProcessGone(WebView view, android.webkit.RenderProcessGoneDetail detail) {
                if (webView != null) {
                    webView.destroy();
                    webView = null;
                }
                recreate();
                return true;
            }
        });

        webView.loadUrl("https://gamenumerasi.blogspot.com");

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    private void loadInterstitialAd() {
        if (isInterstitialLoading || interstitialAd != null) {
            return;
        }
        isInterstitialLoading = true;

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, INTERSTITIAL_AD_UNIT_ID, adRequest,
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd ad) {
                    interstitialAd = ad;
                    isInterstitialLoading = false;
                    
                    interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            interstitialAd = null;
                            loadInterstitialAd();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                            interstitialAd = null;
                            loadInterstitialAd();
                        }
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    interstitialAd = null;
                    isInterstitialLoading = false;
                }
            });
    }

    @JavascriptInterface
    public void panggilIklanInterstisial() {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                if (interstitialAd != null) {
                    interstitialAd.show(MainActivity.this);
                } else {
                    loadInterstitialAd();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
