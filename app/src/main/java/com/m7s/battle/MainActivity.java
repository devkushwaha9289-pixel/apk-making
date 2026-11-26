package com.m7s.battle;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Shell from res/layout/activity_main.xml: .homebar (Earn/Play/Profile tabs) or .appbar (inner pages), .bottomnav, My-Matches fab.
 * Same structure as the website's page_header()/page_footer().
 */
public class MainActivity extends Activity {
    interface ImageCb { void got(byte[] jpeg); }

    private View homebar, appbar, bottomnav, fab;
    private TextView hbName, hbCoins, hbBadge, abTitle;
    private ImageView hbAv;
    private FrameLayout content;
    private final LinearLayout[] navs = new LinearLayout[3];
    private final ImageView[] navI = new ImageView[3];
    private final TextView[] navT = new TextView[3];
    private final ArrayList<Screen> stack = new ArrayList<>();
    private Screen[] roots;
    private Screen cur;
    private int tab = 1;
    private ImageCb imageCb;
    private String myImage = "";

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.init(this);
        Api.onAuthLost = new Runnable() {
            public void run() {
                Intent i = new Intent(MainActivity.this, AuthActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }
        };
        View root = getLayoutInflater().inflate(R.layout.activity_main, null);
        setContentView(root);
        Ui.font(root);
        homebar = findViewById(R.id.homebar);
        appbar = findViewById(R.id.appbar);
        bottomnav = findViewById(R.id.bottomnav);
        fab = findViewById(R.id.fab);
        hbName = (TextView) findViewById(R.id.hb_name);
        hbCoins = (TextView) findViewById(R.id.hb_coins);
        hbBadge = (TextView) findViewById(R.id.hb_badge);
        hbAv = (ImageView) findViewById(R.id.hb_av);
        abTitle = (TextView) findViewById(R.id.ab_title);
        content = (FrameLayout) findViewById(R.id.content);
        JSONObject conf = Cfg.conf(this);
        hbName.setText("");
        if (!conf.optString("logo").isEmpty()) Img.load(hbAv, conf.optString("logo"));

        findViewById(R.id.ab_back).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { onBackPressed(); } });
        findViewById(R.id.hb_bell).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { push(new NotifScreen()); } });
        findViewById(R.id.hb_pill).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { push(new WalletScreen()); } });
        hbAv.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { switchTab(2); } });
        fab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { push(new MatchesScreen(0, "My Matches", true, "upcoming")); }
        });
        int[] ids = {R.id.nav_earn, R.id.nav_play, R.id.nav_profile}, ii = {R.id.nav_earn_i, R.id.nav_play_i, R.id.nav_profile_i}, ti = {R.id.nav_earn_t, R.id.nav_play_t, R.id.nav_profile_t};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            navs[i] = (LinearLayout) findViewById(ids[i]);
            navI[i] = (ImageView) findViewById(ii[i]);
            navT[i] = (TextView) findViewById(ti[i]);
            navs[i].setOnClickListener(new View.OnClickListener() { public void onClick(View v) { switchTab(idx); } });
        }

        roots = new Screen[]{new EarnScreen(), new PlayScreen(), new ProfileScreen()};
        switchTab(1);
        Push.register(this);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
        }
        handleLink(getIntent().getStringExtra("link"));
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        handleLink(i.getStringExtra("link"));
    }

    @Override protected void onResume() {
        super.onResume();
        refreshHome();   // balance in the pill updates
        if (WebActivity.paid) { WebActivity.paid = false; if (cur != null) cur.onShow(); }   // back from the payment page: reload wallet
    }

    /** Where a tapped notification goes (server links: wallet.php, transactions.php, my_matches.php, luckydraw.php...). */
    void handleLink(String link) {
        if (link == null || link.isEmpty()) return;
        String l = link.toLowerCase();
        if (l.contains("transactions")) push(new TxScreen());
        else if (l.contains("wallet") || l.contains("recharge") || l.contains("withdraw")) push(new WalletScreen());
        else if (l.contains("my_matches") || l.contains("matches")) push(new MatchesScreen(0, "My Matches", true, l.contains("done") ? "done" : (l.contains("ongoing") ? "ongoing" : "upcoming")));
        else if (l.contains("luckydraw")) push(new LuckyScreen());
        else if (l.contains("scratch")) push(new ScratchScreen());
        else if (l.contains("watch")) push(new WatchScreen());
        else push(new NotifScreen());
    }

    /** Fills the home bar: name, avatar, coin pill, bell badge. */
    void refreshHome() {
        if (hbName == null) return;
        long seen = 0;
        try { seen = Long.parseLong(Cfg.get(this, "seen_nid")); } catch (Exception ignored) {}
        Api.call("me", Screen.json("since", seen), new Api.Cb() {
            public void done(JSONObject r) {
                if (!r.optBoolean("ok")) return;
                JSONObject u = r.optJSONObject("user");
                if (u == null) return;
                hbName.setText(u.optString("username"));
                hbCoins.setText(Ui.money(u.optJSONObject("wallets").optDouble("total")));
                String im = u.optString("image");
                myImage = im;
                Img.load(hbAv, im.isEmpty() ? Cfg.conf(MainActivity.this).optString("logo") : im);
                setUnread(r.optInt("unread"));
            }
        });
    }

    void setUnread(int n) {
        hbBadge.setVisibility(n > 0 ? View.VISIBLE : View.GONE);
        hbBadge.setText(String.valueOf(n));
    }

    void switchTab(int i) {
        tab = i;
        stack.clear();
        stack.add(roots[i]);
        show(roots[i], true);
    }

    void push(Screen s) {
        stack.add(s);
        show(s, false);
    }

    private void show(Screen s, boolean reload) {
        if (cur != null && cur != s) cur.onHide();
        cur = s;
        boolean fresh = s.sv == null;
        View v = s.view(this);
        content.removeAllViews();
        content.addView(v, new FrameLayout.LayoutParams(-1, -1));
        if (reload && !fresh) s.onShow();
        boolean isTab = s.isTab() && stack.size() == 1;
        homebar.setVisibility(isTab ? View.VISIBLE : View.GONE);
        bottomnav.setVisibility(isTab ? View.VISIBLE : View.GONE);
        appbar.setVisibility(isTab ? View.GONE : View.VISIBLE);
        fab.setVisibility(isTab && s instanceof PlayScreen ? View.VISIBLE : View.GONE);
        abTitle.setText(s.title());
        if (isTab) {
            for (int k = 0; k < 3; k++) {
                int col = k == tab ? Ui.AMBER : 0xFF9E9E9E;
                navT[k].setTextColor(col);
                navI[k].setColorFilter(col);
            }
            refreshHome();
        }
    }

    @Override public void onBackPressed() {
        if (stack.size() > 1) {
            Screen old = stack.remove(stack.size() - 1);
            old.destroyed = true;
            show(stack.get(stack.size() - 1), true);
        } else if (tab != 1) {
            switchTab(1);
        } else {
            super.onBackPressed();
        }
    }

    // ---- gallery picker (profile photo) ----
    void pickImage(ImageCb cb) {
        imageCb = cb;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        startActivityForResult(Intent.createChooser(i, "Choose photo"), 91);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != 91 || res != RESULT_OK || data == null || data.getData() == null || imageCb == null) return;
        try {
            Uri u = data.getData();
            InputStream is = getContentResolver().openInputStream(u);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, o);
            is.close();
            int s = 1;
            while (o.outWidth / (s * 2) >= 800) s *= 2;   // keeps cPanel storage small: max ~800px, JPEG 82%
            o = new BitmapFactory.Options();
            o.inSampleSize = s;
            is = getContentResolver().openInputStream(u);
            Bitmap bm = BitmapFactory.decodeStream(is, null, o);
            is.close();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.JPEG, 82, bo);
            imageCb.got(bo.toByteArray());
        } catch (Exception e) {
            Ui.toast(this, "Could not read that image.");
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        for (Screen s : roots) if (s != null) s.destroyed = true;
    }
}
