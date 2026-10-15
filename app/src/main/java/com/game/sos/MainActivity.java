package com.numerasiapp;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private AdView adView;
    private InterstitialAd interstitialAd;
    private AppOpenAd appOpenAd;
    private ProgressBar progressBar;

    // --- PENGATURAN KEAMANAN ADMOB ---
    private boolean isAdShowing = false; // Mencegah 2 iklan muncul bersamaan
    private int levelClickCounter = 0;   // Menghitung jumlah klik level
    private static final int AD_CLICK_INTERVAL = 2; // Tampilkan Interstitial setiap 2 kali klik level
    private long appOpenAdLoadTime = 0;  // Waktu muat App Open Ad (Mencegah iklan kadaluarsa)

    private static final String TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921";
    private static final String TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mencegah kebocoran warna putih saat transisi
        getWindow().getDecorView().setBackgroundColor(Color.parseColor("#0f172a"));

        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);

        MobileAds.initialize(this, initializationStatus -> {
            loadAppOpenAd();
            loadInterstitialAd();
        });

        adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.loadAd(new AdRequest.Builder().build());
        }

        webView = findViewById(R.id.webview);
        webView.setBackgroundColor(Color.parseColor("#0f172a"));
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.addJavascriptInterface(this, "AndroidApp");
        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.contains("gamenumerasi.blogspot.com")) {
                    return false;
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                webView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    String html = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                            + "<style>body{display:flex;justify-content:center;align-items:center;height:100vh;margin:0;background-color:#0f172a;}"
                            + "img{max-width:80%;height:auto;}</style></head>"
                            + "<body><img src='file:///android_asset/no_internet.png'></body></html>";

                    view.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
                }
            }
        });

        webView.loadUrl("https://gamenumerasi.blogspot.com");
    }

    // --- 1. APP OPEN AD ---
    private void loadAppOpenAd() {
        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(
            this, TEST_APP_OPEN_ID, request,
            new AppOpenAd.AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull AppOpenAd ad) {
                    appOpenAd = ad;
                    appOpenAdLoadTime = System.currentTimeMillis();
                    showAppOpenAdIfAvailable();
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Log.e("AdMob", "App Open Ad gagal dimuat: " + loadAdError.getMessage());
                }
            }
        );
    }

    private boolean isAppOpenAdExpired() {
        // Iklan App Open kadaluarsa setelah 4 jam sesuai aturan AdMob
        long dateDifference = System.currentTimeMillis() - appOpenAdLoadTime;
        return dateDifference > (3600000 * 4);
    }

    private void showAppOpenAdIfAvailable() {
        if (!isAdShowing && appOpenAd != null && !isAppOpenAdExpired() && !isFinishing() && !isDestroyed()) {
            appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdShowedFullScreenContent() {
                    isAdShowing = true;
                    pauseWebView();
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    appOpenAd = null;
                    isAdShowing = false;
                    resumeWebView();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    appOpenAd = null;
                    isAdShowing = false;
                    resumeWebView();
                }
            });

            appOpenAd.show(MainActivity.this);
        }
    }

    // --- 2. INTERSTITIAL AD (KHUSUS UNTUK KLIK LEVEL) ---
    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, TEST_INTERSTITIAL_ID, adRequest,
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd ad) {
                    interstitialAd = ad;

                    interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdShowedFullScreenContent() {
                            isAdShowing = true;
                            pauseWebView();
                        }

                        @Override
                        public void onAdDismissedFullScreenContent() {
                            isAdShowing = false;
                            resumeWebView();
                            interstitialAd = null;
                            loadInterstitialAd();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                            isAdShowing = false;
                            resumeWebView();
                            interstitialAd = null;
                            loadInterstitialAd();
                        }
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    interstitialAd = null;
                }
            });
    }

    // Dipanggil dari JS saat tombol level diklik
    @JavascriptInterface
    public void panggilIklanInterstisial() {
        runOnUiThread(() -> {
            levelClickCounter++;

            // Hanya tampilkan jika memenuhi kriteria jeda klik dan tidak ada iklan lain yang tampil
            if (levelClickCounter % AD_CLICK_INTERVAL == 0) {
                if (interstitialAd != null && !isAdShowing) {
                    interstitialAd.show(MainActivity.this);
                } else {
                    loadInterstitialAd();
                }
            }
        });
    }

    // --- FUNGSI KONTROL WEBVIEW ---
    private void pauseWebView() {
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();
        }
    }

    private void resumeWebView() {
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseWebView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeWebView();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
