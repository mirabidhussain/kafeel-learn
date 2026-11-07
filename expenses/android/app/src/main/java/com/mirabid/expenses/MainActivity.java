package com.mirabid.expenses;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView web;

    public class Bridge {
        @JavascriptInterface public void copy(String text) {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("My Expenses", text));
        }
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.parseColor("#0b7a53"));
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);      // expenses are saved on the phone
        s.setAllowFileAccess(true);
        s.setBlockNetworkLoads(true);      // fully offline
        s.setTextZoom(100);
        web.setBackgroundColor(Color.parseColor("#f4f6fb"));
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { return true; }
        });
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override public void onBackPressed() {
        // close the add/edit sheet or a dialog first; otherwise leave the app
        web.evaluateJavascript(
            "(function(){var d=document.getElementById('dlg'); if(d.classList.contains('on')){d.classList.remove('on');return 'y'}" +
            "if(document.getElementById('sheet').classList.contains('on')){closeSheet();return 'y'} return 'n'})()",
            r -> { if (r != null && r.contains("n")) MainActivity.super.onBackPressed(); });
    }
}
