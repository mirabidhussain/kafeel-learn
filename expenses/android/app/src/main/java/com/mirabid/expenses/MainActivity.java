package com.mirabid.expenses;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 41;
    private WebView web;
    private ValueCallback<Uri[]> pickCallback;

    public class Bridge {
        @JavascriptInterface public void copy(String text) {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("My Expenses", text));
        }

        /** Save the Excel file and open Gmail (or the share sheet) with it attached. */
        @JavascriptInterface public void shareFile(String base64, String name, String mime,
                                                   String subject, String body, boolean gmail) {
            runOnUiThread(() -> {
                try {
                    File dir = new File(getCacheDir(), "exports");
                    dir.mkdirs();
                    File[] old = dir.listFiles();
                    if (old != null) for (File f : old) f.delete();
                    File out = new File(dir, new File(name).getName());
                    try (FileOutputStream fo = new FileOutputStream(out)) { fo.write(Base64.decode(base64, Base64.DEFAULT)); }
                    Uri uri = Uri.parse("content://" + ExportProvider.AUTHORITY + "/exports/" + Uri.encode(out.getName()));

                    Intent send = new Intent(Intent.ACTION_SEND);
                    send.setType(mime);
                    send.putExtra(Intent.EXTRA_STREAM, uri);
                    send.putExtra(Intent.EXTRA_SUBJECT, subject);
                    send.putExtra(Intent.EXTRA_TEXT, body);
                    send.setClipData(ClipData.newRawUri(out.getName(), uri));
                    send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    if (gmail) {
                        Intent g = new Intent(send).setPackage("com.google.android.gm");
                        try { startActivity(g); return; }
                        catch (ActivityNotFoundException e) {
                            Toast.makeText(MainActivity.this, "Gmail not found – choose an app", Toast.LENGTH_SHORT).show();
                        }
                    }
                    startActivity(Intent.createChooser(send, "Send Excel backup"));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Could not create the file", Toast.LENGTH_SHORT).show();
                }
            });
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
        web.setWebChromeClient(new WebChromeClient() {
            // "Restore from Excel file" → Android file picker
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (pickCallback != null) pickCallback.onReceiveValue(null);
                pickCallback = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{ExportProvider.MIME, "application/octet-stream", "application/zip"});
                try { startActivityForResult(Intent.createChooser(i, "Choose Excel backup"), PICK_FILE); }
                catch (ActivityNotFoundException e) { pickCallback = null; return false; }
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        if (req == PICK_FILE && pickCallback != null) {
            Uri[] r = (res == RESULT_OK && data != null && data.getData() != null) ? new Uri[]{data.getData()} : null;
            pickCallback.onReceiveValue(r);
            pickCallback = null;
            return;
        }
        super.onActivityResult(req, res, data);
    }

    @Override public void onBackPressed() {
        // close the add/edit or carry-forward panel or a dialog first; otherwise leave the app
        web.evaluateJavascript(
            "(function(){var d=document.getElementById('dlg'); if(d.classList.contains('on')){d.classList.remove('on');return 'y'}" +
            "if(document.getElementById('sheet').classList.contains('on')||document.getElementById('cfSheet').classList.contains('on')){closeSheet();return 'y'} return 'n'})()",
            r -> { if (r != null && r.contains("n")) MainActivity.super.onBackPressed(); });
    }
}
