package com.game.sos;

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
    private ProgressBar progressBar;

    // ID Uji Coba Resmi dari Google AdMob
    private static final String TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921";
    private static final String TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);

        // 1. Inisialisasi SDK AdMob & Preload Iklan
        MobileAds.initialize(this, initializationStatus -> {
            loadAppOpenAd();      // Iklan saat pertama kali dibuka
            loadInterstitialAd(); // Preload iklan untuk diklik di menu level
        });

        // 2. Banner Ad (Tampil di bagian bawah)
        adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.loadAd(new AdRequest.Builder().build());
        }

        // 3. Konfigurasi WebView
        webView = findViewById(R.id.webview);

        // Optimasi Warna Latar Belakang & Hardware Acceleration (Mencegah Layar Putih)
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

        // Menghubungkan JavaScript Blogspot ke Java Android
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
                    ad.show(MainActivity.this);
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Log.e("AdMob", "App Open Ad gagal dimuat: " + loadAdError.getMessage());
                }
            }
        );
    }

    // --- 2. INTERSTITIAL AD (KHUSUS UNTUK KLIK LEVEL) ---
    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, TEST_INTERSTITIAL_ID, adRequest,
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd ad) {
                    interstitialAd = ad;
                    
                    // Callback saat iklan selesai atau ditutup
                    interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // Setelah iklan ditutup pemain, kosongkan dan siapkan iklan untuk klik level berikutnya
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
                }
            });
    }

    // Dipanggil KHUSUS dari JavaScript saat tombol Level diklik
    @JavascriptInterface
    public void panggilIklanInterstisial() {
        runOnUiThread(() -> {
            if (interstitialAd != null) {
                interstitialAd.show(MainActivity.this);
            } else {
                // Jika iklan belum siap saat diklik, otomatis muat ulang tanpa mengganggu game
                loadInterstitialAd();
            }
        });
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
