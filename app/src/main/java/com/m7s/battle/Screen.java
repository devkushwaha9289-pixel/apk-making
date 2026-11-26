package com.m7s.battle;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

/** One page inside MainActivity. load() fetches data from the API and fills box (same pages as the website). */
abstract class Screen {
    MainActivity a;
    Context c;
    ScrollView sv;
    LinearLayout box;
    boolean destroyed;

    abstract String title();
    abstract void load();

    /** Page background: .app = dark, .app.black = black (Play tab). */
    int bgColor() { return Ui.DARK; }
    /** true = home bar + bottom tabs (Earn / Play / Profile); false = back bar. */
    boolean isTab() { return false; }

    View view(MainActivity act) {
        if (sv == null) {
            a = act;
            c = act;
            sv = new ScrollView(c);
            sv.setBackgroundColor(bgColor());
            sv.setFillViewport(true);
            box = new LinearLayout(c);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(0, 0, 0, Ui.dp(20));
            sv.addView(box);
            load();
        }
        return sv;
    }

    void onShow() { if (sv != null) load(); }
    void onHide() {}

    void loading() {
        box.removeAllViews();
        ProgressBar p = new ProgressBar(c);
        LinearLayout.LayoutParams lp = Ui.lpm(Ui.dp(40), Ui.dp(40), 0, 60, 0, 0);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        box.addView(p, lp);
    }

    void showError(String msg) {
        box.removeAllViews();
        TextView t = Ui.tv(c, msg, 16, Ui.TXT, false);
        t.setGravity(Gravity.CENTER);
        box.addView(t, Ui.lpm(-1, -2, 16, 60, 16, 12));
        TextView b = Ui.btn(c, "TRY AGAIN", Ui.RED);
        b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { load(); } });
        box.addView(Ui.btnbar(c, b));
    }

    /** .empty block (big emoji, amber heading, white text). */
    void empty(String emoji, String head, String sub) {
        LinearLayout e = Ui.col(c);
        e.setGravity(Gravity.CENTER_HORIZONTAL);
        e.setPadding(Ui.dp(20), Ui.dp(40), Ui.dp(20), Ui.dp(40));
        TextView big = Ui.tv(c, emoji, 60, Ui.TXT, false);
        e.addView(big);
        TextView h = Ui.tv(c, head, 20, Ui.AMBER, true);
        h.setGravity(Gravity.CENTER);
        e.addView(h, Ui.lpm(-2, -2, 0, 10, 0, 6));
        TextView s = Ui.tv(c, sub, 16, Ui.TXT, false);
        s.setGravity(Gravity.CENTER);
        e.addView(s);
        box.addView(e);
    }

    void fetch(String action, JSONObject body, final OkCb ok) {
        Api.call(action, body, new Api.Cb() {
            public void done(JSONObject r) {
                if (destroyed || sv == null) return;
                if (r.optBoolean("ok")) { box.removeAllViews(); ok.run(r); }
                else showError(r.optString("error", "Something went wrong."));
            }
        });
    }

    interface OkCb { void run(JSONObject r); }

    static JSONObject json(Object... kv) {
        JSONObject o = new JSONObject();
        try { for (int i = 0; i + 1 < kv.length; i += 2) o.put((String) kv[i], kv[i + 1]); } catch (Exception ignored) {}
        return o;
    }

    /** Result dialog helper: shows message/error, calls after() on success. */
    void result(JSONObject r, String okTitle, String errTitle, Runnable after) {
        Ui.info(c, r.optBoolean("ok") ? okTitle : errTitle, r.optString(r.optBoolean("ok") ? "message" : "error"));
        if (r.optBoolean("ok") && after != null) after.run();
    }
}
