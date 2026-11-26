package com.m7s.battle;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

/** Website's .maint (Server Under Maintenance) and .blockp (account blocked) pages, with their Lottie animations. */
public class StateActivity extends Activity {
    private static boolean showing;

    static void show(Context c, String mode, String msg) {
        if (showing) return;
        showing = true;
        Intent i = new Intent(c, StateActivity.class);
        i.putExtra("mode", mode);
        i.putExtra("msg", msg == null ? "" : msg);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        c.startActivity(i);
    }

    @Override protected void onDestroy() { showing = false; super.onDestroy(); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.init(this);
        final boolean blocked = "blocked".equals(getIntent().getStringExtra("mode"));
        String msg = getIntent().getStringExtra("msg");
        JSONObject conf = Cfg.conf(this);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Ui.DARK);
        sv.setFillViewport(true);
        LinearLayout box = Ui.col(this);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(Ui.dp(20), Ui.dp(40), Ui.dp(20), Ui.dp(30));
        sv.addView(box);

        if (blocked) {
            Ui.lottie(this, box, "39421-ocl-rejected.json", 220, 0, 8);
            TextView h = Ui.tv(this, "Account blocked", 22, Ui.AMBER, true);
            h.setGravity(Gravity.CENTER);
            box.addView(h);
            String[] p = {"Your account has been temporarily blocked/banned.",
                    "Because we have found that you are violating our rules and not following the rules we made."};
            for (String s : p) {
                TextView t = Ui.tv(this, s, 16, Ui.TXT, false);
                t.setGravity(Gravity.CENTER);
                box.addView(t, Ui.lpm(-1, -2, 0, 10, 0, 0));
            }
            TextView h2 = Ui.tv(this, "What do I do now?", 20, Ui.AMBER, true);
            h2.setGravity(Gravity.CENTER);
            box.addView(h2, Ui.lpm(-1, -2, 0, 16, 0, 0));
            TextView t3 = Ui.tv(this, "If you think you have not violated our rules then you don't need to worry, you can get back your account by contacting us.", 16, Ui.TXT, false);
            t3.setGravity(Gravity.CENTER);
            box.addView(t3, Ui.lpm(-1, -2, 0, 8, 0, 0));
            TextView sup = Ui.btn(this, "CUSTOMER SUPPORT", Ui.AMBER);
            sup.setTextColor(Ui.BLACK);
            sup.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { Ui.open(StateActivity.this, Cfg.BASE + "support.php"); } });
            box.addView(sup, Ui.lpm(-1, -2, 0, 18, 0, 0));
            TextView out = Ui.btn(this, "LOGOUT", Ui.SLATE);
            out.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Cfg.setToken(StateActivity.this, "");
                    restart();
                }
            });
            box.addView(out, Ui.lpm(-1, -2, 0, 8, 0, 0));
        } else {
            Ui.lottie(this, box, "36572-under-maintenance.json", 260, 0, 8);
            TextView h = Ui.tv(this, "Server Under Maintenance", 24, Ui.AMBER, true);
            h.setGravity(Gravity.CENTER);
            box.addView(h);
            if (msg == null || msg.isEmpty()) msg = conf.optString("maintenance_msg", "Server under maintenance.");
            TextView t = Ui.tv(this, msg, 16, Ui.TXT, false);
            t.setGravity(Gravity.CENTER);
            box.addView(t, Ui.lpm(-1, -2, 0, 10, 0, 0));
            TextView retry = Ui.btn(this, "TRY AGAIN", Ui.AMBER);
            retry.setTextColor(Ui.BLACK);
            retry.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { restart(); } });
            box.addView(retry, Ui.lpm(-1, -2, 0, 18, 0, 0));
            TextView sup = Ui.btn(this, "CUSTOMER SUPPORT", Ui.SLATE);
            sup.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { Ui.open(StateActivity.this, Cfg.BASE + "support.php"); } });
            box.addView(sup, Ui.lpm(-1, -2, 0, 8, 0, 0));
        }
        setContentView(sv);
        Ui.font(sv);
    }

    private void restart() {
        Intent i = new Intent(this, SplashActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
