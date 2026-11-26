package com.m7s.battle;

import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;

/** match.php : banner, header, "Match details", "Prize Details", rules, participants, buttons. */
class MatchScreen extends Screen {
    private final int id;
    private final Handler h = new Handler();
    private Runnable tick;

    MatchScreen(int id) { this.id = id; }
    String title() { return "Match Details"; }
    @Override void onHide() { if (tick != null) h.removeCallbacks(tick); }

    void load() {
        if (tick != null) h.removeCallbacks(tick);
        loading();
        fetch("match", json("id", id), new OkCb() { public void run(JSONObject r) { render(r.optJSONObject("match")); } });
    }

    private String statusLabel(String s) {
        switch (s) {
            case "upcoming": return "Upcoming";
            case "ongoing": return "Ongoing";
            case "done": return "Completed";
            case "cancelled": return "Cancelled";
            default: return s;
        }
    }

    private View copyVal(String v) {
        LinearLayout r = Ui.row(c);
        r.addView(Ui.tv(c, v, 14, Ui.BLACK, true));
        r.addView(Web.copyBtn(c, v), Ui.lpm(-2, -2, 6, 0, 0, 0));
        return r;
    }

    private void render(final JSONObject m) {
        ImageView banner = Ui.img(c, -1, 180, ImageView.ScaleType.CENTER_CROP);
        box.addView(banner, Ui.lp(-1, Ui.dp(180)));
        Img.load(banner, m.optString("image"));
        LinearLayout hd = Ui.row(c);
        hd.setBackgroundColor(Ui.WHITE);
        hd.setPadding(Ui.dp(12), Ui.dp(10), Ui.dp(12), Ui.dp(10));
        ImageView lg = Ui.img(c, 50, 50, ImageView.ScaleType.CENTER_CROP);
        lg.setBackgroundResource(R.drawable.bg_circle_border);
        lg.setClipToOutline(true);
        hd.addView(lg);
        Img.load(lg, m.optString("cat_image"));
        LinearLayout nm = Ui.col(c);
        nm.setPadding(Ui.dp(10), 0, 0, 0);
        nm.addView(Ui.tv(c, m.optString("name").toUpperCase(), 18, Ui.PURPLE, true));
        nm.addView(Ui.tv(c, m.optString("cat_name") + " · " + statusLabel(m.optString("status")), 12, Ui.SLATE, false));
        TextView org = Ui.tv(c, "", 11, Ui.SLATE, false);
        android.text.SpannableString ss = new android.text.SpannableString("Organized By - " + m.optString("organizer"));
        ss.setSpan(new android.text.style.ForegroundColorSpan(Ui.RED), 15, ss.length(), 0);
        ss.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 15, ss.length(), 0);
        org.setText(ss);
        nm.addView(org);
        hd.addView(nm, Ui.weight(1));
        box.addView(hd);

        final boolean me = m.optInt("my_slots") > 0;
        LinearLayout s1 = Ui.sec(c, box, "Match details");
        Web.kv(c, s1, "Type :", m.optString("type"));
        Web.kv(c, s1, "Mode :", m.optString("mode"));
        Web.kv(c, s1, "Map :", m.optString("map").isEmpty() ? "—" : m.optString("map"));
        double fee = m.optDouble("entry_fee");
        Web.kv(c, s1, "ENTRY FEE :", fee > 0 ? (View) Web.coinValue(c, fee) : Ui.tv(c, "FREE", 14, Ui.BLACK, true));
        final TextView sched = Ui.tv(c, m.optString("start_text"), 14, Ui.BLACK, true);
        sched.setGravity(Gravity.END);
        Web.kv(c, s1, "Match Schedule :", sched);
        if ("upcoming".equals(m.optString("status"))) {
            final long start = m.optLong("start_ts");
            tick = new Runnable() { public void run() { sched.setText(Ui.countdown(start)); h.postDelayed(this, 1000); } };
            tick.run();
        }
        Web.kv(c, s1, "Players :", m.optInt("joined") + " / " + m.optInt("slots"));
        if (!m.optString("version").isEmpty()) Web.kv(c, s1, "Version :", m.optString("version"));
        if (me) {
            boolean vis = m.optBoolean("room_visible");
            Web.kv(c, s1, "Room Id :", vis ? copyVal(m.optString("room_id")) : Ui.tv(c, "--", 14, Ui.BLACK, true));
            Web.kv(c, s1, "Room Password :", vis ? copyVal(m.optString("room_password")) : Ui.tv(c, "--", 14, Ui.BLACK, true));
            JSONArray mine = m.optJSONArray("my_slot_list");
            LinearLayout mb = Ui.col(c);
            mb.setPadding(0, Ui.dp(8), 0, Ui.dp(8));
            mb.addView(Ui.tv(c, "✓ Your slot" + (mine != null && mine.length() > 1 ? "s" : "") + ":", 13, 0xFF00695C, true));
            for (int i = 0; mine != null && i < mine.length(); i++) {
                JSONObject x = mine.optJSONObject(i);
                TextView t = Ui.tv(c, x.optString("slot_label") + " · " + x.optString("game_name") + " · UID " + x.optString("game_uid"), 13, 0xFF00695C, false);
                t.setPadding(0, Ui.dp(4), 0, 0);
                mb.addView(t);
            }
            if (!vis && "upcoming".equals(m.optString("status")))
                mb.addView(Ui.tv(c, "Room ID & password appear " + m.optInt("room_reveal_minutes") + " min before start.", 13, 0xFF00695C, false), Ui.lpm(-2, -2, 0, 6, 0, 0));
            s1.addView(mb);
        }

        LinearLayout s2 = Ui.sec(c, box, "Prize Details");
        Web.kv(c, s2, "Winning Prize :", Web.coinValue(c, m.optDouble("prize")));
        Web.kv(c, s2, "PER KILL :", Web.coinValue(c, m.optDouble("per_kill")));
        JSONArray bk = m.optJSONArray("breakup");
        if (bk != null && bk.length() > 0) {
            LinearLayout body = Web.details(c, s2, "WINNING BREAKUP", false);
            Web.wbRow(c, body, true, "Rank", "Coin Prize");
            for (int i = 0; i < bk.length(); i++) {
                JSONObject g = bk.optJSONObject(i);
                String rk = g.optInt("from") == g.optInt("to") ? "" + g.optInt("from") : g.optInt("from") + " - " + g.optInt("to");
                Web.wbRow(c, body, false, "Rank : " + rk, Ui.money(g.optDouble("amount")));
            }
        }

        LinearLayout s3 = Ui.sec(c, box, null);
        LinearLayout rb = Web.details(c, s3, "Match Rules", false);
        TextView rt = Ui.tv(c, m.optString("rules"), 13, 0xFF37474F, false);
        rt.setLineSpacing(0, 1.35f);
        rt.setPadding(Ui.dp(12), 0, Ui.dp(12), Ui.dp(12));
        rb.addView(rt);

        if (me) {
            JSONArray pl = m.optJSONArray("participants");
            boolean res = m.optBoolean("results_published");
            LinearLayout s4 = Ui.sec(c, box, null);
            LinearLayout pb = Web.details(c, s4, (res ? "Results" : "Participants") + " (" + (pl == null ? 0 : pl.length()) + ")", res);
            if (pl == null || pl.length() == 0) pb.addView(Ui.tv(c, "No one has joined yet.", 14, Ui.SLATE, false), Ui.lpm(-2, -2, 12, 0, 12, 12));
            else {
                if (res) Web.wbRow(c, pb, true, "Team", "Player", "Rank", "Kills", "Won"); else Web.wbRow(c, pb, true, "Team", "Player");
                for (int i = 0; i < pl.length(); i++) {
                    JSONObject x = pl.optJSONObject(i);
                    if (res) Web.wbRow(c, pb, false, x.optString("slot_label"), x.optString("game_name"), x.optInt("rank") > 0 ? "#" + x.optInt("rank") : "—", "" + x.optInt("kills"), x.optDouble("prize") > 0 ? Ui.money(x.optDouble("prize")) : "—");
                    else Web.wbRow(c, pb, false, x.optString("slot_label"), x.optString("game_name"));
                }
            }
        }

        LinearLayout bb = new LinearLayout(c);
        bb.setOrientation(LinearLayout.VERTICAL);
        bb.setPadding(Ui.dp(8), Ui.dp(8), Ui.dp(8), Ui.dp(8));
        String status = m.optString("status");
        TextView main = null;
        if (status.equals("upcoming") && m.optBoolean("full")) { main = Ui.btn(c, "MATCH FULL", 0xFFC62828); }
        else if (m.optBoolean("can_join")) {
            main = Ui.btn(c, me ? "Add Another Slot" : "Join The Match", Ui.RED);
            main.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.push(new SlotsScreen(id)); } });
        } else if (me) {
            StringBuilder sb = new StringBuilder();
            JSONArray ms = m.optJSONArray("my_slot_list");
            for (int i = 0; ms != null && i < ms.length(); i++) sb.append(i > 0 ? ", " : "").append(ms.optJSONObject(i).optString("slot_label"));
            main = Ui.btn(c, "Joined · " + sb, Ui.GREEN);
        } else if (status.equals("upcoming")) main = Ui.btn(c, "Joining closed", 0xFF37474F);
        if (main != null) bb.addView(main, Ui.lpm(-1, -2, 0, 0, 0, 8));
        final String stream = m.optString("stream");
        if (!stream.isEmpty()) {
            TextView w = Ui.btn(c, "Watch Match", Ui.SKY);
            w.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { Ui.open(c, stream); } });
            bb.addView(w);
        }
        box.addView(bb);
    }
}

/** slots.php : team/position grid (Free / Selected / Joined / Your join), NEXT. */
class SlotsScreen extends Screen {
    private final int id;
    SlotsScreen(int id) { this.id = id; }
    String title() { return "Select slot"; }

    void load() {
        loading();
        fetch("slots", json("id", id), new OkCb() { public void run(JSONObject r) { render(r); } });
    }

    private void legend(LinearLayout row, int color, String text) {
        View sq = new View(c);
        sq.setBackground(Ui.bg(color, 3));
        row.addView(sq, Ui.lpm(Ui.dp(14), Ui.dp(14), 8, 0, 5, 0));
        row.addView(Ui.tv(c, text, 12, Ui.LG, false));
    }

    private void render(final JSONObject r) {
        final int total = r.optInt("slots"), ts = Math.max(1, r.optInt("team_size"));
        final HashSet<Integer> taken = new HashSet<>(), mine = new HashSet<>();
        JSONArray t = r.optJSONArray("taken"), mi = r.optJSONArray("mine");
        for (int i = 0; t != null && i < t.length(); i++) taken.add(t.optInt(i));
        for (int i = 0; mi != null && i < mi.length(); i++) mine.add(mi.optInt(i));
        final int allowed = ts - mine.size();
        LinearLayout jb = Web.jbox(c, box);
        Web.jr(c, jb, r.optString("cat_name"), r.optString("name"));
        Web.jr(c, jb, r.optString("type").toUpperCase() + " · pick up to", Math.max(0, allowed) + " slot" + (allowed > 1 ? "s" : ""));
        Web.jr(c, jb, "Entry fee per slot", r.optDouble("entry_fee") > 0 ? (View) Web.coinValue(c, r.optDouble("entry_fee")) : Ui.tv(c, "FREE", 14, Ui.BLACK, true));
        if (!r.optBoolean("open")) { Web.flash(c, box, "This match is not open for joining.", false); return; }

        LinearLayout lg = new LinearLayout(c);
        lg.setGravity(Gravity.CENTER);
        lg.setPadding(Ui.dp(8), Ui.dp(10), Ui.dp(8), Ui.dp(2));
        legend(lg, Ui.WHITE, "Free"); legend(lg, Ui.AMBER, "Selected"); legend(lg, 0xFF607D8B, "Joined"); legend(lg, Ui.GREEN, "Your join");
        box.addView(lg);

        final TreeSet<Integer> sel = new TreeSet<>();
        LinearLayout grid = Ui.col(c);
        grid.setBackground(Ui.bg(Ui.MENU, 8));
        grid.setPadding(Ui.dp(8), Ui.dp(6), Ui.dp(8), Ui.dp(10));
        LinearLayout hr = Ui.row(c);
        hr.setPadding(0, Ui.dp(4), 0, Ui.dp(4));
        TextView th = Ui.tv(c, "Team", 13, Ui.SUB, true);
        hr.addView(th, Ui.weightm(1.3f, 0, 0, 8, 0));
        for (int p = 0; p < ts; p++) {
            TextView x = Ui.tv(c, String.valueOf((char) (65 + p)), 13, Ui.SUB, true);
            x.setGravity(Gravity.CENTER);
            hr.addView(x, Ui.weightm(1, 0, 0, p == ts - 1 ? 0 : 8, 0));
        }
        grid.addView(hr);
        int teams = (total + ts - 1) / ts;
        for (int tm = 0; tm < teams; tm++) {
            LinearLayout row = Ui.row(c);
            row.setPadding(0, Ui.dp(4), 0, Ui.dp(4));
            row.addView(Ui.tv(c, "Team " + (tm + 1), 14, Ui.WHITE, true), Ui.weightm(1.3f, 0, 0, 8, 0));
            for (int p = 0; p < ts; p++) {
                final int slot = tm * ts + p + 1;
                if (slot > total) { row.addView(new View(c), Ui.weightm(1, 0, 0, p == ts - 1 ? 0 : 8, 0)); continue; }
                final View cell = new View(c);
                boolean tk = taken.contains(slot), my = mine.contains(slot);
                cell.setBackground(Ui.bg(my ? Ui.GREEN : (tk ? 0xFF607D8B : Ui.WHITE), 6));
                if (!tk) cell.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (sel.contains(slot)) { sel.remove(slot); cell.setBackground(Ui.bg(Ui.WHITE, 6)); }
                        else if (sel.size() < allowed) { sel.add(slot); cell.setBackground(Ui.bg(Ui.AMBER, 6)); }
                        else Ui.toast(c, "You can pick only " + Math.max(0, allowed) + " slot(s)");
                    }
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(38), 1f);
                lp.setMargins(0, 0, p == ts - 1 ? 0 : Ui.dp(8), 0);
                row.addView(cell, lp);
            }
            grid.addView(row);
        }
        box.addView(grid, Ui.lpm(-1, -2, 8, 8, 8, 8));
        TextView next = Ui.btn(c, "NEXT", Ui.SKY);
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (sel.isEmpty()) { Ui.toast(c, "Select a slot first"); return; }
                a.push(new JoinScreen(id, new ArrayList<>(sel), ts, r));
            }
        });
        box.addView(Ui.btnbar(c, next));
    }
}

/** join.php : balance box, wallet picker, game username + UID per slot, confirm. */
class JoinScreen extends Screen {
    private final int id, ts;
    private final ArrayList<Integer> slots;
    private final JSONObject info;
    private String wallet = "";

    JoinScreen(int id, ArrayList<Integer> slots, int ts, JSONObject info) { this.id = id; this.slots = slots; this.ts = ts; this.info = info; }
    String title() { return "Join Match"; }

    void load() {
        box.removeAllViews();
        final int n = slots.size();
        final double total = info.optDouble("entry_fee") * n;
        JSONObject w = info.optJSONObject("wallets");
        LinearLayout jb = Web.jbox(c, box);
        Web.jr(c, jb, "Total Balance", Web.coinValue(c, w.optDouble("total")));
        Web.jr(c, jb, "Match Category", info.optString("cat_name"));
        Web.jr(c, jb, "Match Name", info.optString("name"));
        Web.jr(c, jb, "Match Time", info.optString("start_text"));
        Web.jr(c, jb, "Entry fee × " + n + " slot" + (n > 1 ? "s" : ""), total > 0 ? (View) Web.coinValue(c, total) : Ui.tv(c, "FREE", 14, Ui.BLACK, true));

        final String[] keys = {"deposit", "winning", "bonus"}, labels = {"DEPOSITED", "WINNINGS", "BONUS"};
        final LinearLayout[] cards = new LinearLayout[3];
        LinearLayout ws = new LinearLayout(c);
        ws.setPadding(Ui.dp(8), Ui.dp(8), Ui.dp(8), Ui.dp(8));
        for (int i = 0; i < 3; i++) {
            double bal = w.optDouble(keys[i]);
            boolean ok = bal >= total;
            if (wallet.isEmpty() && ok) wallet = keys[i];
            LinearLayout k = Ui.col(c);
            k.setGravity(Gravity.CENTER_HORIZONTAL);
            k.setPadding(Ui.dp(4), Ui.dp(10), Ui.dp(4), Ui.dp(10));
            k.addView(Ui.tv(c, labels[i], 12, Ui.BLACK, true));
            LinearLayout v = Ui.coinAmt(c, Ui.money(bal), 17, Ui.BLACK, true);
            v.setGravity(Gravity.CENTER);
            k.addView(v, Ui.lpm(-2, -2, 0, 4, 0, 0));
            if (!ok) k.setAlpha(0.45f);
            cards[i] = k;
            final String key = keys[i];
            if (ok) k.setOnClickListener(new View.OnClickListener() {
                public void onClick(View vv) { wallet = key; paint(cards, keys); }
            });
            ws.addView(k, Ui.weightm(1, i == 0 ? 0 : 6, 0, 0, 0));
        }
        paint(cards, keys);
        box.addView(ws);
        if (wallet.isEmpty() && total > 0) {
            Web.flash(c, box, "You Don't Have Sufficient Balance", false);
            TextView cancel = Ui.btn(c, "Cancel", 0xFF37474F), add = Ui.btn(c, "ADD BALANCE", Ui.SKY);
            cancel.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.onBackPressed(); } });
            add.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.push(new RechargeScreen()); } });
            box.addView(Ui.btnbar(c, cancel, add));
            return;
        }
        final ArrayList<EditText> names = new ArrayList<>(), uids = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int s = slots.get(i);
            LinearLayout p = Web.panel(c, box);
            p.setPadding(Ui.dp(14), Ui.dp(10), Ui.dp(14), Ui.dp(4));
            LinearLayout head = Ui.row(c);
            head.addView(Ui.tv(c, "Team " + ((s - 1) / ts + 1), 15, Ui.AMBER, true));
            head.addView(Ui.tv(c, "  · Position " + (char) (65 + (s - 1) % ts), 12, Ui.SUB, false));
            p.addView(head);
            p.addView(Ui.plabel(c, "Game Username"));
            EditText nm = Ui.pinput(c, "Enter in-game username", InputType.TYPE_CLASS_TEXT);
            p.addView(nm);
            p.addView(Ui.plabel(c, "Game UID"));
            EditText ud = Ui.pinput(c, "Enter your game UID", InputType.TYPE_CLASS_TEXT);
            p.addView(ud);
            if (i == 0 && info.optJSONArray("mine") != null && info.optJSONArray("mine").length() == 0) { nm.setText(info.optString("ign")); ud.setText(info.optString("game_uid")); }
            names.add(nm);
            uids.add(ud);
        }
        LinearLayout np = Web.panel(c, box);
        TextView note = Ui.note(c, "Note : Make sure the " + info.optString("cat_name") + " username and UID are correct — you won't be able to change them for this match. Each player in the match needs a different UID.", Ui.WHITE);
        np.addView(note);
        TextView back = Ui.btn(c, "Back", 0xFF37474F), go = Ui.btn(c, "ADD NEW ENTRY", Ui.SKY);
        back.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.onBackPressed(); } });
        go.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final JSONArray sa = new JSONArray(), na = new JSONArray(), ua = new JSONArray();
                for (int i = 0; i < n; i++) { sa.put(slots.get(i)); na.put(names.get(i).getText().toString().trim()); ua.put(uids.get(i).getText().toString().trim()); }
                Ui.confirm(c, "Confirm", "Are You Sure Want To Join " + info.optString("cat_name") + " Tournament (" + n + " slot" + (n > 1 ? "s" : "") + ") ?", new Runnable() {
                    public void run() {
                        JSONObject b = json("id", id, "wallet", wallet);
                        try { b.put("slots", sa); b.put("names", na); b.put("uids", ua); } catch (Exception ignored) {}
                        Api.call("join", b, new Api.Cb() {
                            public void done(JSONObject r) {
                                if (r.optBoolean("ok")) { Ui.toast(c, r.optString("message")); a.onBackPressed(); a.onBackPressed(); }
                                else Ui.info(c, "Could not join", r.optString("error"));
                            }
                        });
                    }
                });
            }
        });
        box.addView(Ui.btnbar(c, back, go));
    }

    private void paint(LinearLayout[] cards, String[] keys) {
        for (int i = 0; i < 3; i++) cards[i].setBackgroundResource(keys[i].equals(wallet) ? R.drawable.bg_option_sel_amber : R.drawable.bg_option);
    }
}
