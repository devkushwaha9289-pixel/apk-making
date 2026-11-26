package com.m7s.battle;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * In-app WebView.
 *  - mode "pay": opens the gateway's payment page, hands upi:// / phonepe:// / intent:// links to the installed UPI apps,
 *    and when the gateway returns to gateway_return.php shows the result natively (polls ?json=1 like the website does).
 *  - mode "page": plain page (Admin panel / Staff panel of the website).
 */
public class WebActivity extends Activity {
    /** MainActivity reloads the current screen (wallet) when this is true. */
    static boolean paid;

    static void pay(android.content.Context c, String url) { start(c, url, "Payment", "pay"); }
    static void page(android.content.Context c, String url, String title) { start(c, url, title, "page"); }

    private static void start(android.content.Context c, String url, String title, String mode) {
        Intent i = new Intent(c, WebActivity.class);
        i.putExtra("url", url);
        i.putExtra("title", title);
        i.putExtra("mode", mode);
        if (!(c instanceof Activity)) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
    }

    private WebView web;
    private ProgressBar bar, wait;
    private View result;
    private TextView icon, rtitle, rmsg, btn, btn2;
    private boolean payMode, settled, resultShown;
    private String order = "";
    private int polls;
    private final Handler h = new Handler();

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.init(this);
        setContentView(R.layout.activity_web);
        Ui.font(findViewById(android.R.id.content));
        payMode = "pay".equals(getIntent().getStringExtra("mode"));
        ((TextView) findViewById(R.id.wb_title)).setText(getIntent().getStringExtra("title"));
        findViewById(R.id.wb_back).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { onBackPressed(); } });
        web = (WebView) findViewById(R.id.wb_web);
        bar = (ProgressBar) findViewById(R.id.wb_progress);
        wait = (ProgressBar) findViewById(R.id.wb_wait);
        result = findViewById(R.id.wb_result);
        icon = (TextView) findViewById(R.id.wb_icon);
        rtitle = (TextView) findViewById(R.id.wb_rtitle);
        rmsg = (TextView) findViewById(R.id.wb_rmsg);
        btn = (TextView) findViewById(R.id.wb_btn);
        btn2 = (TextView) findViewById(R.id.wb_btn2);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(false);
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(web, true);

        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int p) {
                bar.setProgress(p);
                bar.setVisibility(p >= 100 ? View.GONE : View.VISIBLE);
            }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { return route(r.getUrl().toString()); }
            @Override public void onPageStarted(WebView v, String url, Bitmap f) { if (payMode && isReturn(url)) route(url); }
            @Override public void onReceivedError(WebView v, int code, String d, String url) {
                if (url != null && url.equals(v.getUrl()) && !resultShown) Ui.toast(WebActivity.this, "Network problem. Please check your internet.");
            }
        });
        web.loadUrl(getIntent().getStringExtra("url"));
    }

    private boolean isReturn(String url) { return url != null && url.contains("gateway_return.php"); }

    /** true = handled here (do not load in the WebView). */
    private boolean route(String url) {
        if (payMode && isReturn(url)) {
            String o = Uri.parse(url).getQueryParameter("o");
            if (o != null && !o.replaceAll("[^A-Za-z0-9]", "").isEmpty()) {
                if (!resultShown) { order = o.replaceAll("[^A-Za-z0-9]", ""); showResult(); poll(); }
                return true;
            }
            return false;
        }
        String sch = Uri.parse(url).getScheme();
        if (sch == null || sch.equals("http") || sch.equals("https") || sch.equals("about") || sch.equals("file") || sch.equals("data")) return false;
        try {
            Intent i = sch.equals("intent") ? Intent.parseUri(url, Intent.URI_INTENT_SCHEME) : new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.addCategory(Intent.CATEGORY_BROWSABLE);
            i.setComponent(null);
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Ui.toast(this, "This payment app is not installed. Choose another option.");
        } catch (Exception e) {
            Ui.toast(this, "Cannot open payment app.");
        }
        return true;
    }

    // ---------- native payment result (same states as gateway_return.php) ----------
    private void showResult() {
        resultShown = true;
        result.setVisibility(View.VISIBLE);
        look("⏳", "Verifying payment…", Ui.AMBER, "Waiting for the bank to confirm. Please do not close this page.\nOrder " + order, true);
        btn.setVisibility(View.GONE);
        btn2.setVisibility(View.GONE);
    }

    private void look(String ic, String title, int color, String msg, boolean waiting) {
        icon.setText(ic);
        rtitle.setText(title);
        rtitle.setTextColor(color);
        rmsg.setText(msg);
        wait.setVisibility(waiting ? View.VISIBLE : View.GONE);
    }

    private void poll() {
        if (settled) return;
        new Thread(new Runnable() {
            public void run() {
                String state = "pending", msg = "";
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(Cfg.BASE + "gateway_return.php?o=" + order + "&json=1").openConnection();
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(20000);
                    InputStream is = c.getInputStream();
                    ByteArrayOutputStream bo = new ByteArrayOutputStream();
                    byte[] buf = new byte[2048];
                    int n;
                    while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
                    is.close();
                    JSONObject j = new JSONObject(bo.toString("UTF-8"));
                    state = j.optString("state", "pending");
                    msg = j.optString("msg");
                } catch (Exception ignored) {}
                final String st = state, m = msg;
                h.post(new Runnable() { public void run() { got(st, m); } });
            }
        }).start();
    }

    private void got(String st, String msg) {
        if (isFinishing() || settled) return;
        if (st.equals("pending")) {
            if (++polls > 45) {
                settled = true;
                look("⏳", "Still verifying", Ui.AMBER, "No payment found yet. If money was deducted, coins arrive automatically — check your wallet in a few minutes.", false);
                buttons("OPEN WALLET", Ui.GREEN, true);
            } else h.postDelayed(new Runnable() { public void run() { poll(); } }, 4000);
            return;
        }
        settled = true;
        if (st.equals("paid")) {
            paid = true;
            look("✅", "Payment Successful", Ui.GREEN, msg.isEmpty() ? "Coins have been added to your deposit wallet." : msg, false);
            buttons("OPEN WALLET", Ui.GREEN, false);
        } else if (st.equals("review")) {
            look("🕵️", "Under review", Ui.ORANGE, msg, false);
            buttons("OPEN WALLET", Ui.GREEN, true);
        } else {
            look("❌", "Payment not completed", Ui.RED, msg.isEmpty() ? "Payment was not completed." : msg, false);
            buttons("TRY AGAIN", Ui.RED, true);
        }
    }

    private void buttons(String label, int color, boolean support) {
        btn.setText(label);
        btn.setBackgroundColor(color);
        btn.setVisibility(View.VISIBLE);
        btn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { finish(); } });
        btn2.setVisibility(support ? View.VISIBLE : View.GONE);
        btn2.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { Ui.open(WebActivity.this, Cfg.BASE + "support.php"); } });
    }

    @Override public void onBackPressed() {
        if (payMode && !settled) {
            new AlertDialog.Builder(this).setTitle("Cancel payment?")
                    .setMessage(resultShown ? "We are still confirming your payment. If money was deducted, coins arrive automatically."
                            : "Do you want to leave the payment page? If money was deducted, coins arrive automatically.")
                    .setNegativeButton("Stay", null)
                    .setPositiveButton("Leave", new DialogInterface.OnClickListener() { public void onClick(DialogInterface d, int w) { paid = true; finish(); } })
                    .show();
            return;
        }
        if (!payMode && web.canGoBack()) { web.goBack(); return; }
        finish();
    }

    @Override protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        if (web != null) { web.stopLoading(); web.destroy(); }
        super.onDestroy();
    }
}
