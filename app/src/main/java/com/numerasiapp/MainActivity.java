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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private FrameLayout nativeAdContainer;
    private NativeAd currentNativeAd;
    private InterstitialAd interstitialAd;
    private ProgressBar progressBar;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private AlertDialog offlineDialog;
    private boolean isOfflineState = false;
    private boolean isKeyboardVisible = false;

    private boolean isInterstitialLoading = false;
    private boolean isNativeAdLoading = false;

    private final Handler adRetryHandler = new Handler(Looper.getMainLooper());
    private int nativeAdRetryCount = 0;
    private static final int MAX_AD_RETRY = 3;
    private static final long RETRY_DELAY_MS = 15000L;

    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6983364109428063/3679555743";
    private static final String NATIVE_AD_UNIT_ID = "ca-app-pub-6983364109428063/5305766094";
    private static final String GAME_URL = "https://gamenumerasi.blogspot.com";

    private final Runnable retryNativeAdRunnable = this::loadNativeAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Paksa Window background Android menjadi gelap total #0b0f19
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#0b0f19")));

        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);
        nativeAdContainer = findViewById(R.id.nativeAdContainer);
        nativeAdContainer.setVisibility(View.GONE);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Sembunyikan iklan otomatis saat keyboard muncul agar kolom input & tombol game tidak tertutup
        ViewCompat.setOnApplyWindowInsetsListener(nativeAdContainer, (v, insets) -> {
            isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            if (isKeyboardVisible) {
                v.setVisibility(View.GONE);
            } else if (currentNativeAd != null && !isOfflineState) {
                v.setVisibility(View.VISIBLE);
            }
            return ViewCompat.onApplyWindowInsets(v, insets);
        });

        MobileAds.initialize(this, initializationStatus -> {
            loadInterstitialAd();
            loadNativeAd();
        });

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
                        webView.loadUrl(GAME_URL);
                    }
                    if (currentNativeAd != null && !isKeyboardVisible && !isOfflineState) {
                        nativeAdContainer.setVisibility(View.VISIBLE);
                    } else if (currentNativeAd == null && !isOfflineState) {
                        nativeAdRetryCount = 0;
                        loadNativeAd();
                    }
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> {
                    isOfflineState = true;
                    adRetryHandler.removeCallbacks(retryNativeAdRunnable);
                    nativeAdContainer.setVisibility(View.GONE);
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
            webView.loadUrl(GAME_URL);
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

    private void loadNativeAd() {
        if (isFinishing() || isDestroyed() || isNativeAdLoading || !isInternetAvailable()) {
            return;
        }
        isNativeAdLoading = true;

        AdLoader adLoader = new AdLoader.Builder(this, NATIVE_AD_UNIT_ID)
                .forNativeAd(nativeAd -> {
                    isNativeAdLoading = false;
                    if (isFinishing() || isDestroyed()) {
                        nativeAd.destroy();
                        return;
                    }
                    nativeAdRetryCount = 0;
                    if (currentNativeAd != null) {
                        currentNativeAd.destroy();
                    }
                    currentNativeAd = nativeAd;

                    NativeAdView adView = (NativeAdView) getLayoutInflater()
                            .inflate(R.layout.native_ad_layout, nativeAdContainer, false);
                    populateNativeAdView(nativeAd, adView);

                    nativeAdContainer.removeAllViews();
                    nativeAdContainer.addView(adView);
                    if (!isOfflineState && !isKeyboardVisible) {
                        nativeAdContainer.setVisibility(View.VISIBLE);
                    }
                })
                .withAdListener(new com.google.android.gms.ads.AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        isNativeAdLoading = false;
                        Log.e("ADMOB_NATIVE", "Gagal muat iklan native | Kode: "
                                + loadAdError.getCode() + " | Pesan: " + loadAdError.getMessage());
                        nativeAdContainer.removeAllViews();
                        nativeAdContainer.setVisibility(View.GONE);

                        if (nativeAdRetryCount < MAX_AD_RETRY && !isOfflineState) {
                            nativeAdRetryCount++;
                            adRetryHandler.removeCallbacks(retryNativeAdRunnable);
                            adRetryHandler.postDelayed(retryNativeAdRunnable, RETRY_DELAY_MS);
                        }
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder().build())
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));

        if (nativeAd.getHeadline() != null && adView.getHeadlineView() != null) {
            ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        }

        if (adView.getBodyView() != null) {
            if (nativeAd.getBody() == null) {
                adView.getBodyView().setVisibility(View.GONE);
            } else {
                adView.getBodyView().setVisibility(View.VISIBLE);
                ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
            }
        }

        if (adView.getCallToActionView() != null) {
            if (nativeAd.getCallToAction() == null) {
                adView.getCallToActionView().setVisibility(View.GONE);
            } else {
                adView.getCallToActionView().setVisibility(View.VISIBLE);
                ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
            }
        }

        if (adView.getIconView() != null) {
            if (nativeAd.getIcon() == null) {
                adView.getIconView().setVisibility(View.GONE);
            } else {
                ((ImageView) adView.getIconView()).setImageDrawable(nativeAd.getIcon().getDrawable());
                adView.getIconView().setVisibility(View.VISIBLE);
            }
        }

        adView.setNativeAd(nativeAd);
    }

    private void showOfflinePage(WebView view) {
        nativeAdContainer.setVisibility(View.GONE);
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
                    webView.loadUrl(GAME_URL);
                    if (currentNativeAd != null && !isKeyboardVisible) {
                        nativeAdContainer.setVisibility(View.VISIBLE);
                    } else if (currentNativeAd == null) {
                        nativeAdRetryCount = 0;
                        loadNativeAd();
                    }
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
        if (webView != null) webView.onPause();
        try {
            if (connectivityManager != null && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();

        try {
            if (connectivityManager != null && networkCallback != null) {
                NetworkRequest networkRequest = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!isInternetAvailable()) {
            isOfflineState = true;
            showOfflinePage(webView);
            showOfflineDialog();
        } else {
            if (isOfflineState) {
                if (offlineDialog != null && offlineDialog.isShowing()) {
                    offlineDialog.dismiss();
                }
                isOfflineState = false;
                if (webView != null) {
                    webView.loadUrl(GAME_URL);
                }
            }
            if (currentNativeAd != null && !isKeyboardVisible) {
                nativeAdContainer.setVisibility(View.VISIBLE);
            } else if (currentNativeAd == null) {
                nativeAdRetryCount = 0;
                loadNativeAd();
            }
        }
    }

    @Override
    protected void onDestroy() {
        adRetryHandler.removeCallbacksAndMessages(null);
        if (offlineDialog != null && offlineDialog.isShowing()) {
            offlineDialog.dismiss();
        }
        nativeAdContainer.removeAllViews();
        if (currentNativeAd != null) {
            currentNativeAd.destroy();
        }
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
