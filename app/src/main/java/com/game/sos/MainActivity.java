package com.numerasiapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
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
import androidx.appcompat.app.AlertDialog;
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

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private AlertDialog offlineDialog;
    private boolean isOfflineState = false;

    private boolean isInterstitialLoading = false;
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Paksa Window background Android menjadi gelap total #0b0f19
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#0b0f19")));

        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        MobileAds.initialize(this, initializationStatus -> loadInterstitialAd());

        adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.loadAd(new AdRequest.Builder().build());
        }

        webView = findViewById(R.id.webview);
        
        // 2. Kunci Background WebView
        webView.setBackgroundColor(Color.parseColor("#0b0f19"));

        // 3. Matikan Layer Type Hardware eksternal yang memicu kedap-kedip canvas pada WebView
        webView.setLayerType(View.LAYER_TYPE_NONE, null);

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

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
                    isOfflineState = true;
                    showOfflinePage(view);
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

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> {
                    if (offlineDialog != null && offlineDialog.isShowing()) {
                        offlineDialog.dismiss();
                    }
                    if (isOfflineState && webView != null) {
                        isOfflineState = false;
                        webView.loadUrl("https://gamenumerasi.blogspot.com");
                    }
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> {
                    isOfflineState = true;
                    showOfflineDialog();
                });
            }
        };

        if (!isInternetAvailable()) {
            isOfflineState = true;
            showOfflinePage(webView);
            showOfflineDialog();
        } else {
            isOfflineState = false;
            webView.loadUrl("https://gamenumerasi.blogspot.com");
        }

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

    private void showOfflinePage(WebView view) {
        String html = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<style>body{display:flex;justify-content:center;align-items:center;height:100vh;margin:0;background-color:#0b0f19;}"
                + "img{max-width:80%;height:auto;}</style></head>"
                + "<body><img src='file:///android_asset/no_internet.png'></body></html>";
        view.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
    }

    private void showOfflineDialog() {
        if (offlineDialog != null && offlineDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Koneksi Internet Terputus");
        builder.setMessage("Game ini memerlukan koneksi internet aktif untuk dimainkan.");
        builder.setCancelable(false);
        builder.setPositiveButton("Coba Lagi", (dialog, which) -> {
            if (!isInternetAvailable()) {
                showOfflineDialog();
            } else {
                dialog.dismiss();
                if (webView != null) {
                    isOfflineState = false;
                    webView.loadUrl("https://gamenumerasi.blogspot.com");
                }
            }
        });
        builder.setNegativeButton("Keluar", (dialog, which) -> finish());

        offlineDialog = builder.create();
        offlineDialog.setCanceledOnTouchOutside(false);
        offlineDialog.show();
    }

    private boolean isInternetAvailable() {
        if (connectivityManager == null) return false;
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                 capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                 capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
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
        if (adView != null) adView.pause();
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) adView.resume();

        try {
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!isInternetAvailable()) {
            isOfflineState = true;
            showOfflinePage(webView);
            showOfflineDialog();
        }
    }

    @Override
    protected void onDestroy() {
        if (adView != null) adView.destroy();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
