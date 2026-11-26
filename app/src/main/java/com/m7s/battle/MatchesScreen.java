package com.m7s.battle;

import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

/** matches.php : purple tab bar (Ongoing / Upcoming / Result) + .mcard list built from res/layout/item_match.xml. */
class MatchesScreen extends Screen {
    private final int cat;
    private final String name;
    private final boolean mine;
    private String tab;
    private LinearLayout list;

    MatchesScreen(int cat, String name, boolean mine, String tab) { this.cat = cat; this.name = name; this.mine = mine; this.tab = tab; }

    String title() { return name; }

    void load() {
        box.removeAllViews();
        LinearLayout tabs = new LinearLayout(c);
        String[][] t = {{"Ongoing", "ongoing"}, {"Upcoming", "upcoming"}, {"Result", "done"}};
        for (final String[] x : t) {
            boolean on = x[1].equals(tab);
            TextView b = Ui.tv(c, x[0], 16, on ? Ui.AMBER : Ui.WHITE, true);
            b.setGravity(Gravity.CENTER);
            b.setBackgroundResource(on ? R.drawable.bg_tab_on : R.drawable.bg_tab_off);
            b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { tab = x[1]; load(); } });
            tabs.addView(b, new LinearLayout.LayoutParams(0, -1, 1f));
        }
        box.addView(tabs, Ui.lp(-1, Ui.dp(50)));
        list = Ui.col(c);
        list.setPadding(Ui.dp(8), Ui.dp(8), Ui.dp(8), Ui.dp(8));
        box.addView(list);
        TextView p = Ui.tv(c, "Loading...", 14, Ui.SUB, false);
        p.setGravity(Gravity.CENTER);
        list.addView(p, Ui.lpm(-1, -2, 0, 40, 0, 0));
        Api.call("matches", json("tab", tab, "cat", cat > 0 ? String.valueOf(cat) : "all", "mine", mine ? "1" : "0"), new Api.Cb() {
            public void done(JSONObject r) {
                if (destroyed || list == null) return;
                list.removeAllViews();
                if (!r.optBoolean("ok")) { Ui.info(c, "Error", r.optString("error")); return; }
                JSONArray m = r.optJSONArray("matches");
                if (m == null || m.length() == 0) {
                    String lbl = tab.equals("done") ? "Result" : (tab.equals("ongoing") ? "Ongoing" : "Upcoming");
                    LinearLayout e = Ui.col(c);
                    e.setGravity(Gravity.CENTER_HORIZONTAL);
                    e.setPadding(Ui.dp(20), Ui.dp(40), Ui.dp(20), Ui.dp(40));
                    e.addView(Ui.tv(c, "🎯", 60, Ui.TXT, false));
                    TextView h = Ui.tv(c, "No " + lbl + " Matches !", 20, Ui.AMBER, true);
                    e.addView(h, Ui.lpm(-2, -2, 0, 10, 0, 6));
                    TextView s = Ui.tv(c, "There Are No " + lbl + " Matches Currently.\nCheck Back In Some Time...", 16, Ui.TXT, false);
                    s.setGravity(Gravity.CENTER);
                    e.addView(s);
                    list.addView(e);
                    return;
                }
                for (int i = 0; i < m.length(); i++) list.addView(card(m.optJSONObject(i)));
            }
        });
    }

    private View card(final JSONObject m) {
        View k = Ui.inflate(c, R.layout.item_match, list);
        Img.load((ImageView) k.findViewById(R.id.m_banner), m.optString("image"));
        Img.load((ImageView) k.findViewById(R.id.m_logo), m.optString("cat_image"));
        ((TextView) k.findViewById(R.id.m_name)).setText(m.optString("name"));
        ((TextView) k.findViewById(R.id.m_time)).setText(m.optString("start_text"));
        TextView org = (TextView) k.findViewById(R.id.m_org);
        android.text.SpannableString ss = new android.text.SpannableString("Organized By - " + m.optString("organizer"));
        ss.setSpan(new android.text.style.ForegroundColorSpan(Ui.RED), 15, ss.length(), 0);
        org.setText(ss);
        ((TextView) k.findViewById(R.id.m_prize)).setText(Ui.money(m.optDouble("prize")));
        ((TextView) k.findViewById(R.id.m_kill)).setText(Ui.money(m.optDouble("per_kill")));
        double fee = m.optDouble("entry_fee");
        ((TextView) k.findViewById(R.id.m_fee)).setText(fee > 0 ? Ui.money(fee) : "FREE");
        k.findViewById(R.id.m_fee_ic).setVisibility(fee > 0 ? View.VISIBLE : View.GONE);
        ((TextView) k.findViewById(R.id.m_type)).setText(m.optString("type"));
        ((TextView) k.findViewById(R.id.m_mode)).setText(m.optString("mode"));
        ((TextView) k.findViewById(R.id.m_map)).setText(m.optString("map").isEmpty() ? "—" : m.optString("map"));
        int slots = Math.max(1, m.optInt("slots")), joined = m.optInt("joined");
        ProgressBar bar = (ProgressBar) k.findViewById(R.id.m_bar);
        bar.setMax(slots);
        bar.setProgress(joined);
        ((TextView) k.findViewById(R.id.m_cnt)).setText(joined + " / " + slots);
        String status = m.optString("status");
        boolean full = m.optBoolean("full");
        k.findViewById(R.id.m_full).setVisibility(full && status.equals("upcoming") ? View.VISIBLE : View.GONE);

        TextView w = (TextView) k.findViewById(R.id.m_watch);
        final String stream = m.optString("stream");
        if (stream.isEmpty()) w.setAlpha(0.45f); else w.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { Ui.open(c, stream); } });

        TextView j = (TextView) k.findViewById(R.id.m_join);
        int mySlots = m.optInt("my_slots");
        if (full && status.equals("upcoming")) { j.setText("MATCH FULL"); j.setBackground(Ui.bg(0xFFC62828, 4)); j.setTextColor(Ui.WHITE); }
        else if (m.optBoolean("can_join")) {
            j.setText("JOIN"); j.setBackground(Ui.bg(Ui.GREEN, 4)); j.setTextColor(Ui.WHITE);
            j.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.push(new SlotsScreen(m.optInt("id"))); } });
        } else if (mySlots > 0) { j.setText("JOINED"); j.setBackground(Ui.bg(Ui.GREEN, 4)); j.setTextColor(Ui.WHITE); j.setAlpha(0.45f); }
        else { j.setText("NOT JOINED"); j.setBackground(Ui.bg(Ui.LG2, 4)); j.setTextColor(0xFF1E88E5); }

        if (mySlots > 0) {
            k.findViewById(R.id.m_roombar).setVisibility(View.VISIBLE);
            boolean vis = m.optBoolean("room_visible");
            ((TextView) k.findViewById(R.id.m_room)).setText(vis ? m.optString("room_id") : "--");
            ((TextView) k.findViewById(R.id.m_pass)).setText(vis ? m.optString("room_password") : "--");
        }
        k.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.push(new MatchScreen(m.optInt("id"))); } });
        return k;
    }
}
