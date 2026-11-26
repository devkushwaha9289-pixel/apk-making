package com.m7s.battle;

import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

/** index.php : announce ticker, slider, Tournaments (Ongoing/Upcoming/Completed), Select a game. Black page like .app.black. */
class PlayScreen extends Screen {
    private final Handler h = new Handler();
    private Runnable slide;

    String title() { return "Play"; }
    @Override int bgColor() { return Ui.BLACK; }
    @Override boolean isTab() { return true; }
    @Override void onHide() { if (slide != null) h.removeCallbacks(slide); }

    void load() {
        if (slide != null) h.removeCallbacks(slide);
        loading();
        fetch("home", null, new OkCb() { public void run(JSONObject r) { render(r); } });
    }

    private void render(JSONObject r) {
        String ann = r.optString("announcement");
        if (!ann.isEmpty()) {   // .announce : orange bar, white box with marquee text
            LinearLayout an = Ui.row(c);
            an.setBackgroundColor(Ui.ORANGE);
            an.addView(Ui.icon(c, R.drawable.ic_mega, 40, 0xFFFFFFFF), Ui.lpm(Ui.dp(40), Ui.dp(40), 5, 5, 5, 5));
            TextView t = Ui.tv(c, ann, 14, Ui.BLACK, true);
            t.setSingleLine(true);
            t.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            t.setMarqueeRepeatLimit(-1);
            t.setSelected(true);
            t.setBackgroundColor(Ui.WHITE);
            t.setGravity(Gravity.CENTER_VERTICAL);
            t.setPadding(Ui.dp(10), 0, Ui.dp(10), 0);
            an.addView(t, new LinearLayout.LayoutParams(0, -1, 1f));
            box.addView(an, Ui.lp(-1, Ui.dp(50)));
        }

        final JSONArray sl = r.optJSONArray("sliders");   // .slider : 200dp, image 180dp rounded, amber dots
        if (sl != null && sl.length() > 0) {
            LinearLayout wrap = Ui.col(c);
            wrap.setPadding(Ui.dp(8), Ui.dp(10), Ui.dp(8), Ui.dp(10));
            final ImageView iv = Ui.img(c, -1, 180, ImageView.ScaleType.CENTER_CROP);
            iv.setBackground(Ui.bg(Ui.LG, 8));
            iv.setClipToOutline(true);
            wrap.addView(iv, Ui.lp(-1, Ui.dp(180)));
            final LinearLayout dots = new LinearLayout(c);
            dots.setGravity(Gravity.CENTER);
            final View[] dv = new View[sl.length()];
            for (int i = 0; i < sl.length(); i++) {
                dv[i] = new View(c);
                dots.addView(dv[i], Ui.lpm(Ui.dp(7), Ui.dp(7), 3, 6, 3, 0));
            }
            if (sl.length() > 1) wrap.addView(dots);
            box.addView(wrap);
            final int[] pos = {0};
            slide = new Runnable() {
                public void run() {
                    int p = pos[0] % sl.length();
                    JSONObject s = sl.optJSONObject(p);
                    if (s != null) {
                        Img.load(iv, s.optString("image"));
                        final String link = s.optString("link");
                        iv.setOnClickListener(link.isEmpty() ? null : new View.OnClickListener() { public void onClick(View v) { Ui.open(c, link); } });
                    }
                    for (int i = 0; i < dv.length; i++) dv[i].setBackground(Ui.bg(i == p ? Ui.AMBER : 0x80FFFFFF, 4));
                    pos[0]++;
                    if (sl.length() > 1) h.postDelayed(this, 4000);
                }
            };
            slide.run();
        }

        box.addView(Ui.secbar(c, "Tournaments"));
        LinearLayout opts = new LinearLayout(c);
        opts.setPadding(Ui.dp(4), Ui.dp(4), Ui.dp(4), Ui.dp(4));
        Object[][] o = {{R.drawable.ic_live, "Ongoing", "ongoing", 0xFF4CAF50}, {R.drawable.ic_clock, "Upcoming", "upcoming", 0xFF2196F3}, {R.drawable.ic_check, "Completed", "done", 0xFFFF9800}};
        for (final Object[] x : o) {
            View v = Ui.inflate(c, R.layout.item_option, opts);
            ImageView ic = (ImageView) v.findViewById(R.id.o_icon);
            ic.setImageResource((Integer) x[0]);
            ic.setColorFilter((Integer) x[3]);
            ((TextView) v.findViewById(R.id.o_text)).setText((String) x[1]);
            v.setOnClickListener(new View.OnClickListener() {
                public void onClick(View vv) { a.push(new MatchesScreen(0, "My Matches", true, (String) x[2])); }
            });
            opts.addView(v);
        }
        box.addView(opts);

        box.addView(Ui.secbar(c, "Select a game"));
        JSONArray cats = r.optJSONArray("categories");
        if (cats == null || cats.length() == 0) { empty("🎮", "No games yet", "Games will appear here soon."); return; }
        LinearLayout rowL = null;
        for (int i = 0; i < cats.length(); i++) {
            if (i % 3 == 0) {
                rowL = new LinearLayout(c);
                rowL.setPadding(Ui.dp(4), 0, Ui.dp(4), 0);
                box.addView(rowL);
            }
            final JSONObject cat = cats.optJSONObject(i);
            View g = Ui.inflate(c, R.layout.item_game, rowL);
            Img.load((ImageView) g.findViewById(R.id.g_img), cat.optString("image"));
            ((TextView) g.findViewById(R.id.g_name)).setText(cat.optString("name"));
            g.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { a.push(new MatchesScreen(cat.optInt("id"), cat.optString("name"), false, "upcoming")); }
            });
            rowL.addView(g);
        }
        for (int k = cats.length() % 3; k != 0 && k < 3; k++) { View sp = new View(c); rowL.addView(sp, Ui.weightm(1, 4, 4, 4, 4)); }
    }
}
