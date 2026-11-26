package com.m7s.battle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONObject;

/** .splash : dark-teal radial background, big logo, thin blue progress bar, version. Loads live settings from the website. */
public class SplashActivity extends Activity {
    private ImageView logo;
    private TextView name;

    @Override protected void onCreate(Bundle b) {
        setTheme(R.style.Splash);
        super.onCreate(b);
        Ui.init(this);
        setContentView(R.layout.activity_splash);
        Ui.font(findViewById(android.R.id.content));
        logo = (ImageView) findViewById(R.id.sp_logo);
        name = (TextView) findViewById(R.id.sp_name);
        ((TextView) findViewById(R.id.sp_ver)).setText("Version " + BuildConfig.VERSION_NAME);
        JSONObject cached = Cfg.conf(this);
        name.setText(cached.optString("site_name", ""));
        if (!cached.optString("splash_bg").isEmpty()) Img.load((ImageView) findViewById(R.id.sp_logo), cached.optString("logo"));
        else if (!cached.optString("logo").isEmpty()) Img.load(logo, cached.optString("logo"));
        start();
    }

    private void start() {
        Api.call("config", null, new Api.Cb() {
            public void done(JSONObject r) {
                if (!r.optBoolean("ok")) { retry(r.optString("error", "Cannot connect to server.")); return; }
                Cfg.put(SplashActivity.this, "conf", r.toString());
                Push.ensureInit(SplashActivity.this);
                name.setText(r.optString("site_name"));
                Img.load(logo, r.optString("logo"));
                JSONObject apk = r.optJSONObject("apk");
                if (apk != null && apk.optInt("min_code", 1) > BuildConfig.APP_VERSION_CODE) { forceUpdate(apk); return; }
                if (r.optBoolean("maintenance") && Cfg.token(SplashActivity.this).isEmpty()) {
                    retry(r.optString("maintenance_msg", "Server under maintenance."));
                    return;
                }
                logo.postDelayed(new Runnable() { public void run() { go(); } }, 600);
            }
        });
    }

    private void go() {
        Intent i = new Intent(this, Cfg.token(this).isEmpty() ? AuthActivity.class : MainActivity.class);
        String link = getIntent() != null ? getIntent().getStringExtra("link") : null;
        if (link != null) i.putExtra("link", link);
        startActivity(i);
        finish();
    }

    private void retry(String msg) {
        new AlertDialog.Builder(this).setTitle("Server Under Maintenance / No connection").setMessage(msg).setCancelable(false)
                .setPositiveButton("Retry", new DialogInterface.OnClickListener() { public void onClick(DialogInterface d, int w) { start(); } })
                .setNegativeButton("Exit", new DialogInterface.OnClickListener() { public void onClick(DialogInterface d, int w) { finish(); } }).show();
    }

    private void forceUpdate(final JSONObject apk) {
        new AlertDialog.Builder(this).setTitle("Update required")
                .setMessage("Please install the latest version (" + apk.optString("version") + ") to continue.").setCancelable(false)
                .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        String u = apk.optString("link");
                        Ui.open(SplashActivity.this, u.isEmpty() ? Cfg.BASE + "download.php" : u);
                        finish();
                    }
                }).show();
    }
}
