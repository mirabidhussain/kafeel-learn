package com.mirabid.kafeellearn;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.Locale;

public class MainActivity extends Activity {
    private WebView web;
    private TextToSpeech tts;

    public class Speaker {
        @JavascriptInterface public void speak(String text) {
            if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "q");
        }
        @JavascriptInterface public void stop() { if (tts != null) tts.stop(); }
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.parseColor("#7b61ff"));
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) { tts.setLanguage(new Locale("en", "IN")); tts.setSpeechRate(0.9f); tts.setPitch(1.15f); }
        });
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setBlockNetworkLoads(true);          // everything is inside the app
        s.setTextZoom(100);                    // keep layout steady if phone font size is large
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, android.webkit.WebResourceRequest r) { return true; } // never leave the app
        });
        web.setBackgroundColor(Color.parseColor("#e9f6ff"));
        web.addJavascriptInterface(new Speaker(), "AndroidTTS");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override public void onBackPressed() {
        web.evaluateJavascript(
            "(function(){var b=['quit','lQuit','toHome'].map(function(i){return document.getElementById(i)})" +
            ".filter(function(e){return e&&e.offsetParent!==null})[0]; if(b){b.click();return 'y'} return 'n'})()",
            r -> { if (r != null && r.contains("n")) MainActivity.super.onBackPressed(); });
    }

    @Override protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
