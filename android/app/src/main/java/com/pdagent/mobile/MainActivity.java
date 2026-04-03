package com.pdagent.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.graphics.Color;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private static final String GIST_URL =
        "https://gist.githubusercontent.com/Charlie-coder/1b492ad9a8842b2942c29ac8bb872cb4/raw/pdagent.json";

    private WebView webView;
    private LinearLayout loadingLayout;
    private LinearLayout errorLayout;
    private EditText manualUrlInput;
    private ProgressBar progressBar;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().setStatusBarColor(Color.parseColor("#0a0b10"));
        getWindow().setNavigationBarColor(Color.parseColor("#0a0b10"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0a0b10"));

        // Loading screen
        loadingLayout = new LinearLayout(this);
        loadingLayout.setOrientation(LinearLayout.VERTICAL);
        loadingLayout.setGravity(android.view.Gravity.CENTER);
        loadingLayout.setPadding(60, 0, 60, 0);
        loadingLayout.setBackgroundColor(Color.parseColor("#0a0b10"));

        TextView logo = new TextView(this);
        logo.setText("PD");
        logo.setTextSize(48);
        logo.setTextColor(Color.parseColor("#3b82f6"));
        logo.setTypeface(null, android.graphics.Typeface.BOLD);
        logo.setGravity(android.view.Gravity.CENTER);
        loadingLayout.addView(logo);

        progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(80, 80);
        pbParams.gravity = android.view.Gravity.CENTER;
        pbParams.topMargin = 40;
        loadingLayout.addView(progressBar, pbParams);

        statusText = new TextView(this);
        statusText.setText("Connecting to PDAgent…");
        statusText.setTextColor(Color.parseColor("#94a3b8"));
        statusText.setTextSize(15);
        statusText.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams stParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        stParams.topMargin = 30;
        loadingLayout.addView(statusText, stParams);

        root.addView(loadingLayout, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        // Error / manual input layout (hidden by default)
        errorLayout = new LinearLayout(this);
        errorLayout.setOrientation(LinearLayout.VERTICAL);
        errorLayout.setGravity(android.view.Gravity.CENTER);
        errorLayout.setPadding(60, 0, 60, 0);
        errorLayout.setBackgroundColor(Color.parseColor("#0a0b10"));
        errorLayout.setVisibility(View.GONE);

        TextView errorTitle = new TextView(this);
        errorTitle.setText("Could not connect");
        errorTitle.setTextSize(18);
        errorTitle.setTextColor(Color.parseColor("#f87171"));
        errorTitle.setGravity(android.view.Gravity.CENTER);
        errorLayout.addView(errorTitle);

        TextView orText = new TextView(this);
        orText.setText("Paste URL manually:");
        orText.setTextSize(13);
        orText.setTextColor(Color.parseColor("#94a3b8"));
        orText.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams orParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        orParams.topMargin = 30;
        errorLayout.addView(orText, orParams);

        manualUrlInput = new EditText(this);
        manualUrlInput.setHint("https://xxx.trycloudflare.com/?token=...");
        manualUrlInput.setTextColor(Color.parseColor("#e5e7eb"));
        manualUrlInput.setHintTextColor(Color.parseColor("#6b7280"));
        manualUrlInput.setBackgroundColor(Color.parseColor("#1e1f2e"));
        manualUrlInput.setPadding(30, 24, 30, 24);
        manualUrlInput.setTextSize(13);
        manualUrlInput.setSingleLine(true);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        inputParams.topMargin = 16;
        errorLayout.addView(manualUrlInput, inputParams);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams btnRowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnRowParams.topMargin = 16;

        Button goBtn = new Button(this);
        goBtn.setText("Connect");
        goBtn.setTextColor(Color.WHITE);
        goBtn.setBackgroundColor(Color.parseColor("#3b82f6"));
        goBtn.setOnClickListener(v -> {
            String url = manualUrlInput.getText().toString().trim();
            if (!url.isEmpty()) loadUrl(url);
        });
        btnRow.addView(goBtn);

        Button retryBtn = new Button(this);
        retryBtn.setText("Retry Auto");
        retryBtn.setTextColor(Color.WHITE);
        retryBtn.setBackgroundColor(Color.parseColor("#374151"));
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        retryParams.leftMargin = 20;
        retryBtn.setOnClickListener(v -> fetchAndConnect());
        btnRow.addView(retryBtn, retryParams);

        errorLayout.addView(btnRow, btnRowParams);
        root.addView(errorLayout, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        // WebView (hidden by default)
        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor("#0a0b10"));
        webView.setVisibility(View.GONE);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        root.addView(webView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(root);
        fetchAndConnect();
    }

    private void fetchAndConnect() {
        loadingLayout.setVisibility(View.VISIBLE);
        errorLayout.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        statusText.setText("Connecting to PDAgent…");
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                URL url = new URL(GIST_URL + "?t=" + System.currentTimeMillis());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                String targetUrl = json.getString("url");

                if (targetUrl != null && !targetUrl.isEmpty() && !targetUrl.equals("placeholder")) {
                    getSharedPreferences("pdagent", MODE_PRIVATE).edit()
                        .putString("last_url", targetUrl).apply();
                    runOnUiThread(() -> loadUrl(targetUrl));
                } else {
                    throw new Exception("PDAgent not running");
                }
            } catch (Exception e) {
                String lastUrl = getSharedPreferences("pdagent", MODE_PRIVATE)
                    .getString("last_url", "");
                if (!lastUrl.isEmpty()) {
                    runOnUiThread(() -> loadUrl(lastUrl));
                } else {
                    runOnUiThread(() -> showError());
                }
            }
        }).start();
    }

    private void loadUrl(String url) {
        loadingLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        webView.loadUrl(url);
    }

    private void showError() {
        loadingLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (webView.getVisibility() == View.VISIBLE && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
