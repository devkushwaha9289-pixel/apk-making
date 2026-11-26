package com.m7s.battle;

import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

/** notifications.php */
class NotifScreen extends Screen {
    String title() { return "Announcements"; }
    void load() {
        loading();
        fetch("notifications", null, new OkCb() {
            public void run(JSONObject r) {
                Cfg.put(c, "seen_nid", String.valueOf(r.optLong("max_id")));
                a.setUnread(0);
                JSONArray l = r.optJSONArray("notifications");
                if (l == null || l.length() == 0) { empty("🔔", "No Announcements", "You're all caught up."); return; }
                for (int i = 0; i < l.length(); i++) {
                    JSONObject n = l.optJSONObject(i);
                    View card = Ui.inflate(c, R.layout.item_card, box);
                    ((TextView) card.findViewById(R.id.c_title)).setText(n.optString("title"));
                    TextView t = (TextView) card.findViewById(R.id.c_right);
                    t.setText(n.optString("time"));
                    t.setTextSize(11);
                    LinearLayout body = (LinearLayout) card.findViewById(R.id.c_body);
                    if (!n.optString("image").isEmpty()) {
                        ImageView im = Ui.img(c, -1, 200, ImageView.ScaleType.FIT_CENTER);
                        im.setAdjustViewBounds(true);
                        body.addView(im, Ui.lpm(-1, Ui.dp(200), 0, 6, 0, 6));
                        Img.load(im, n.optString("image"));
                    }
                    body.addView(Ui.tv(c, n.optString("body"), 14, 0xFF37474F, false));
                    box.addView(card);
                }
            }
        });
    }
}

/** leaderboard.php */
class LeaderScreen extends Screen {
    private int per = 0;
    private final String[] keys = {"full", "week", "month"};
    String title() { return "Leaderboard"; }

    void load() {
        box.removeAllViews();
        LinearLayout hl = Ui.col(c);
        hl.setBackgroundColor(Ui.LG);
        hl.setGravity(Gravity.CENTER_HORIZONTAL);
        hl.setPadding(Ui.dp(14), Ui.dp(14), Ui.dp(14), Ui.dp(14));
        hl.addView(Ui.tv(c, "Leaderboard", 22, Ui.RED, true));
        TextView s = Ui.tv(c, "Top 100 Players Highest Earning On Our Platform", 14, Ui.SLATE, false);
        s.setGravity(Gravity.CENTER);
        hl.addView(s);
        box.addView(hl);
        box.addView(Web.ptabs(c, new String[]{"Fulltime", "Weekly", "Monthly"}, per, new Web.Pick() { public void on(int i) { per = i; load(); } }));
        final LinearLayout list = Ui.col(c);
        box.addView(list);
        Api.call("leaderboard", json("p", keys[per]), new Api.Cb() {
            public void done(JSONObject r) {
                if (destroyed) return;
                JSONArray p = r.optJSONArray("players");
                if (p == null || p.length() == 0) {
                    LinearLayout e = Ui.col(c);
                    e.setGravity(Gravity.CENTER_HORIZONTAL);
                    e.setPadding(Ui.dp(20), Ui.dp(40), Ui.dp(20), Ui.dp(40));
                    e.addView(Ui.tv(c, "🏆", 60, Ui.WHITE, false));
                    e.addView(Ui.tv(c, "No winners yet", 20, Ui.AMBER, true), Ui.lpm(-2, -2, 0, 10, 0, 6));
                    e.addView(Ui.tv(c, "Be the first!", 16, Ui.WHITE, false));
                    list.addView(e);
                    return;
                }
                for (int i = 0; i < p.length(); i++) {
                    JSONObject x = p.optJSONObject(i);
                    View k = Ui.inflate(c, R.layout.item_lb, list);
                    ((TextView) k.findViewById(R.id.l_rank)).setText(String.valueOf(x.optInt("rank")));
                    Img.load((ImageView) k.findViewById(R.id.l_img), x.optString("image"));
                    ((TextView) k.findViewById(R.id.l_name)).setText(x.optString("username"));
                    ((TextView) k.findViewById(R.id.l_sub)).setText("Level " + x.optInt("level") + " · " + x.optInt("kills") + " kills");
                    ((TextView) k.findViewById(R.id.l_amt)).setText(Ui.money(x.optDouble("coins")));
                    if (x.optBoolean("me")) k.setBackground(Ui.border(0xFF009688, Ui.AMBER, 2, 6));
                    list.addView(k);
                }
            }
        });
    }
}

/** luckydraw.php : tabs (Lucky Draws / Winners) with .pk rows. */
class LuckyScreen extends Screen {
    private int tab = 0;
    String title() { return "Lucky Draw"; }

    void load() {
        box.removeAllViews();
        box.addView(Web.ptabs(c, new String[]{"Lucky Draws", "Winners"}, tab, new Web.Pick() { public void on(int i) { tab = i; load(); } }));
        final LinearLayout list = Ui.col(c);
        box.addView(list);
        Api.call("luckydraws", null, new Api.Cb() {
            public void done(JSONObject r) {
                if (destroyed) return;
                if (!r.optBoolean("ok")) { Ui.info(c, "Error", r.optString("error")); return; }
                JSONArray l = r.optJSONArray("draws");
                int shown = 0;
                for (int i = 0; l != null && i < l.length(); i++) {
                    final JSONObject d = l.optJSONObject(i);
                    boolean drawn = !"open".equals(d.optString("status"));
                    if (drawn != (tab == 1)) continue;
                    shown++;
                    View v = Ui.inflate(c, R.layout.item_pack, list);
                    ((TextView) v.findViewById(R.id.p_coins)).setText(d.optString("title"));
                    ((TextView) v.findViewById(R.id.p_coins)).setTextSize(16);
                    ((TextView) v.findViewById(R.id.p_extra)).setText(tab == 1 ? "🏆 " + (d.optString("winner").isEmpty() ? "—" : d.optString("winner")) : d.optString("draw_text"));
                    ((TextView) v.findViewById(R.id.p_price)).setText(tab == 1 ? "Won" : "Register");
                    v.setOnClickListener(new View.OnClickListener() { public void onClick(View vv) { a.push(new LuckyDetailScreen(d)); } });
                    list.addView(v);
                }
                if (shown == 0) {
                    LinearLayout e = Ui.col(c);
                    e.setGravity(Gravity.CENTER_HORIZONTAL);
                    e.setPadding(Ui.dp(20), Ui.dp(40), Ui.dp(20), Ui.dp(40));
                    e.addView(Ui.tv(c, "🎟️", 60, Ui.WHITE, false));
                    e.addView(Ui.tv(c, "No Lucky Draw Found !", 20, Ui.AMBER, true), Ui.lpm(-2, -2, 0, 10, 0, 6));
                    TextView s = Ui.tv(c, "There Are No Lucky Draw History Found !", 16, Ui.WHITE, false);
                    s.setGravity(Gravity.CENTER);
                    e.addView(s);
                    list.addView(e);
                }
            }
        });
    }
}

class LuckyDetailScreen extends Screen {
    private final JSONObject d;
    private final Handler h = new Handler();
    private Runnable tick;
    LuckyDetailScreen(JSONObject d) { this.d = d; }
    String title() { return "Lucky Draw"; }
    @Override void onHide() { if (tick != null) h.removeCallbacks(tick); }
    @Override void onShow() {}

    void load() {
        box.removeAllViews();
        if (!d.optString("image").isEmpty()) {
            ImageView im = Ui.img(c, -1, 220, ImageView.ScaleType.CENTER_CROP);
            box.addView(im, Ui.lp(-1, Ui.dp(220)));
            Img.load(im, d.optString("image"));
        }
        LinearLayout s = Ui.sec(c, box, d.optString("title"));
        Web.kv(c, s, "1x Winner :", Web.coinValue(c, d.optDouble("prize")));
        Web.kv(c, s, "Reg. Fee :", d.optDouble("fee") > 0 ? (View) Web.coinValue(c, d.optDouble("fee")) : Ui.tv(c, "FREE", 14, Ui.BLACK, true));
        Web.kv(c, s, "Result On :", d.optString("draw_text"));
        Web.kv(c, s, "Spots Left", String.valueOf(Math.max(0, d.optInt("slots") - d.optInt("joined"))));
        TextView tl = Ui.tv(c, "Time Left:", 14, Ui.WHITE, false);
        tl.setGravity(Gravity.CENTER);
        box.addView(tl, Ui.lpm(-1, -2, 0, 8, 0, 0));
        LinearLayout cd = new LinearLayout(c);
        cd.setGravity(Gravity.CENTER);
        cd.setPadding(Ui.dp(10), Ui.dp(10), Ui.dp(10), Ui.dp(10));
        final TextView[] nums = new TextView[4];
        String[] lab = {"Days", "Hours", "Minutes", "Seconds"};
        for (int i = 0; i < 4; i++) {
            LinearLayout b = Ui.col(c);
            b.setGravity(Gravity.CENTER);
            b.setBackground(Ui.bg(0xFFE3F2FD, 6));
            b.setPadding(Ui.dp(10), Ui.dp(8), Ui.dp(10), Ui.dp(8));
            nums[i] = Ui.tv(c, "00", 22, Ui.BLACK, true);
            b.addView(nums[i]);
            b.addView(Ui.tv(c, lab[i], 11, Ui.SLATE2, false));
            cd.addView(b, Ui.lpm(Ui.dp(60), -2, 4, 0, 4, 0));
        }
        box.addView(cd);
        final long end = d.optLong("draw_ts");
        tick = new Runnable() {
            public void run() {
                long sec = Math.max(0, end - Cfg.now() / 1000L);
                long[] v = {sec / 86400, sec % 86400 / 3600, sec % 3600 / 60, sec % 60};
                for (int i = 0; i < 4; i++) nums[i].setText(String.format(java.util.Locale.US, "%02d", v[i]));
                h.postDelayed(this, 1000);
            }
        };
        tick.run();
        if (!d.optString("details").isEmpty()) Web.rules(c, box, "How to play?", d.optString("details"), Ui.WHITE);
        if (!"open".equals(d.optString("status"))) {
            TextView f = Web.flash(c, box, "🏆 Congratulations to Our Lucky Winner: " + (d.optString("winner").isEmpty() ? "—" : d.optString("winner")), true);
            f.setGravity(Gravity.CENTER);
        } else if (d.optBoolean("in")) Web.flash(c, box, "✓ You are registered. Good luck!", true);
        else {
            TextView b = Ui.btn(c, "Register", Ui.SKY);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Ui.confirm(c, "Lucky Draw", "Register for " + (d.optDouble("fee") > 0 ? Ui.money(d.optDouble("fee")) + " coins" : "free") + " (taken from deposit wallet)? One entry per user.", new Runnable() {
                        public void run() {
                            Api.call("luckydraw_join", json("id", d.optInt("id")), new Api.Cb() {
                                public void done(JSONObject x) {
                                    if (x.optBoolean("ok")) { Ui.toast(c, x.optString("message")); a.onBackPressed(); }
                                    else Ui.info(c, "Could not register", x.optString("error"));
                                }
                            });
                        }
                    });
                }
            });
            box.addView(Ui.btnbar(c, b));
        }
    }
}

/** scratch.php */
class ScratchScreen extends Screen {
    String title() { return "Scratch Card"; }

    void load() {
        loading();
        fetch("scratch_status", null, new OkCb() { public void run(JSONObject r) { render(r); } });
    }

    private TextView wtask(String head, String val) {
        LinearLayout w = Ui.col(c);
        w.setGravity(Gravity.CENTER);
        w.setBackground(Ui.bg(Ui.SLATE, 8));
        w.setPadding(Ui.dp(14), Ui.dp(14), Ui.dp(14), Ui.dp(14));
        w.addView(Ui.tv(c, head, 14, Ui.WHITE, true));
        TextView v = Ui.tv(c, val, 34, Ui.AMBER, true);
        w.addView(v);
        box.addView(w, Ui.lpm(-1, -2, 8, 14, 8, 14));
        return v;
    }

    private void render(final JSONObject r) {
        TextView h = Ui.tv(c, "SCRATCH & WIN COINS!", 22, 0xFFFFEB3B, true);
        h.setGravity(Gravity.CENTER);
        box.addView(h, Ui.lpm(-1, -2, 0, 14, 0, 4));
        TextView p = Ui.tv(c, "Scratch the card to reveal your reward.", 15, Ui.LG, false);
        p.setGravity(Gravity.CENTER);
        box.addView(p);
        final boolean on = r.optBoolean("on");
        final int left = on ? r.optInt("left") : 0, limit = r.optInt("limit", 1);
        final TextView leftTv = wtask("CARDS LEFT TODAY", left + "/" + limit);
        if (on && left > 0) {
            FrameLayout card = new FrameLayout(c);
            card.setBackground(Ui.bg(0xFFFFD54F, 14));
            card.setClipToOutline(true);
            final TextView prize = Ui.tv(c, "🎁", 40, 0xFFE65100, true);
            prize.setTypeface(Ui.SQ);
            prize.setTag("nofont");
            prize.setGravity(Gravity.CENTER);
            card.addView(prize, new FrameLayout.LayoutParams(-1, -1));
            final ScratchView sv2 = new ScratchView(c);
            card.addView(sv2, new FrameLayout.LayoutParams(-1, -1));
            LinearLayout wrap = new LinearLayout(c);
            wrap.setGravity(Gravity.CENTER);
            wrap.addView(card, new LinearLayout.LayoutParams(Ui.dp(300), Ui.dp(170)));
            box.addView(wrap, Ui.lpm(-1, -2, 0, 4, 0, 4));
            final TextView msg = Ui.tv(c, "", 18, Ui.WHITE, true);
            msg.setGravity(Gravity.CENTER);
            msg.setMinHeight(Ui.dp(30));
            box.addView(msg, Ui.lpm(-1, -2, 0, 10, 0, 0));
            final double[] won = {-1};
            sv2.setListener(new ScratchView.Listener() {
                public void onFirstTouch() {
                    Api.call("scratch_open", null, new Api.Cb() {
                        public void done(JSONObject x) {
                            if (!x.optBoolean("ok")) { Ui.info(c, "Scratch card", x.optString("error")); return; }
                            won[0] = x.optDouble("amount");
                            prize.setText(won[0] > 0 ? "YOU WON\n₹" + Ui.money(won[0]) + "\ncoins" : "Better luck\nnext time");
                            prize.setTextSize(won[0] > 0 ? 30 : 20);
                        }
                    });
                }
                public void onRevealed() {
                    msg.setText(won[0] > 0 ? "🎉 " + Ui.money(won[0]) + " coins added!" : "Better luck next time!");
                    msg.setTextColor(won[0] > 0 ? 0xFF76FF03 : Ui.WHITE);
                    leftTv.setText(Math.max(0, left - 1) + "/" + limit);
                    a.refreshHome();
                }
            });
        } else {
            empty("🪙", on ? "Come back tomorrow" : "Scratch cards are off", on ? "You used all your scratch cards for today." : "Please check again later.");
        }
        TextView n = Ui.tv(c, "*Note: " + limit + " scratch card" + (limit == 1 ? "" : "s") + " per day. Rewards go to your " + r.optString("wallet") + " wallet.", 13, Ui.LG, false);
        n.setTypeface(Ui.FB, android.graphics.Typeface.ITALIC);
        box.addView(n, Ui.lpm(-1, -2, 14, 10, 14, 10));
        JSONArray hs = r.optJSONArray("history");
        if (hs != null && hs.length() > 0) {
            box.addView(Ui.secbar(c, "Recent cards"));
            for (int i = 0; i < hs.length(); i++) {
                JSONObject x = hs.optJSONObject(i);
                View card = Ui.inflate(c, R.layout.item_card, box);
                ((View) card.findViewById(R.id.c_title).getParent()).setBackgroundColor(Ui.SLATE);
                ((TextView) card.findViewById(R.id.c_title)).setText(x.optString("time"));
                TextView rt = (TextView) card.findViewById(R.id.c_right);
                rt.setText(x.optDouble("amount") > 0 ? "+" + Ui.money(x.optDouble("amount")) : "—");
                ((LinearLayout) card.findViewById(R.id.c_body)).setVisibility(View.GONE);
                box.addView(card);
            }
        }
    }
}

/** watch.php : daily limit, countdown while the video plays, reward on completion. */
class WatchScreen extends Screen {
    private final Handler h = new Handler();
    private Runnable tick;
    private boolean busy;
    String title() { return "Watch & Earn"; }
    @Override void onHide() { if (tick != null) h.removeCallbacks(tick); }

    void load() {
        loading();
        fetch("watch", null, new OkCb() { public void run(JSONObject r) { render(r); } });
    }

    private void render(final JSONObject r) {
        TextView an = Ui.tv(c, "💸", 56, Ui.WHITE, false);
        an.setGravity(Gravity.CENTER);
        box.addView(an, Ui.lpm(-1, -2, 0, 10, 0, 0));
        TextView h2 = Ui.tv(c, "WATCH MORE TO EARN MORE!", 20, 0xFFFFEB3B, true);
        h2.setGravity(Gravity.CENTER);
        box.addView(h2, Ui.lpm(-1, -2, 0, 4, 0, 0));
        final int lim = r.optInt("limit"), done = r.optInt("done");
        LinearLayout w = Ui.col(c);
        w.setGravity(Gravity.CENTER);
        w.setBackground(Ui.bg(Ui.SLATE, 8));
        w.setPadding(Ui.dp(14), Ui.dp(14), Ui.dp(14), Ui.dp(14));
        w.addView(Ui.tv(c, "COMPLETE TASK", 14, Ui.WHITE, true));
        w.addView(Ui.tv(c, done + "/" + lim, 34, Ui.AMBER, true));
        box.addView(w, Ui.lpm(-1, -2, 8, 14, 8, 14));
        LinearLayout steps = new LinearLayout(c);
        steps.setPadding(0, Ui.dp(10), 0, Ui.dp(10));
        String[][] st = {{"📺", "Watch Video"}, {"🏆", "Get Reward"}, {"🎮", "Join Match"}};
        for (String[] x : st) {
            LinearLayout s = Ui.col(c);
            s.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView e = Ui.tv(c, x[0], 24, Ui.WHITE, false);
            e.setGravity(Gravity.CENTER);
            e.setBackgroundResource(R.drawable.bg_circle_dark);
            s.addView(e, Ui.lp(Ui.dp(52), Ui.dp(52)));
            s.addView(Ui.tv(c, x[1], 12, Ui.WHITE, false), Ui.lpm(-2, -2, 0, 6, 0, 0));
            steps.addView(s, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        box.addView(steps);
        final TextView b = Ui.btn(c, done >= lim ? "DAILY LIMIT DONE" : "WATCH VIDEO", Ui.RED);
        if (done >= lim) b.setAlpha(0.5f);
        else b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (busy) return;
                busy = true;
                b.setText("LOADING...");
                Api.call("watch_start", null, new Api.Cb() {
                    public void done(JSONObject x) {
                        if (!x.optBoolean("ok")) { busy = false; b.setText("WATCH VIDEO"); Ui.info(c, "Watch & Earn", x.optString("error")); return; }
                        String video = r.optString("video");
                        if (!video.isEmpty()) Ui.open(c, video);
                        countdown(b, r.optInt("wait"));
                    }
                });
            }
        });
        box.addView(Ui.btnbar(c, b));
        TextView n = Ui.tv(c, "*Note: max " + lim + " videos per day. Reward: " + Ui.money(r.optDouble("reward")) + " coins each, added to your bonus wallet. Closing or skipping the video early gives no reward.", 13, Ui.LG, false);
        n.setTypeface(Ui.FB, android.graphics.Typeface.ITALIC);
        box.addView(n, Ui.lpm(-1, -2, 14, 10, 14, 10));
    }

    private void countdown(final TextView b, final int wait) {
        final long end = System.currentTimeMillis() + wait * 1000L;
        tick = new Runnable() {
            public void run() {
                long left = (end - System.currentTimeMillis() + 999) / 1000;
                if (left > 0) { b.setText("WATCHING... " + left + "s"); h.postDelayed(this, 500); return; }
                b.setText("CLAIMING...");
                Api.call("watch_claim", null, new Api.Cb() {
                    public void done(JSONObject x) {
                        busy = false;
                        Ui.info(c, x.optBoolean("ok") ? "Reward" : "Watch & Earn", x.optString(x.optBoolean("ok") ? "message" : "error"));
                        a.refreshHome();
                        load();
                    }
                });
            }
        };
        tick.run();
    }
}

/** refer.php */
class ReferScreen extends Screen {
    String title() { return "My Referrals"; }

    void load() {
        loading();
        fetch("refer", null, new OkCb() { public void run(JSONObject r) { render(r); } });
    }

    private void render(final JSONObject r) {
        TextView h = Ui.tv(c, "Refer More Friends To Earn More !", 20, 0xFFFFEB3B, true);
        h.setGravity(Gravity.CENTER);
        box.addView(h, Ui.lpm(-1, -2, 10, 14, 10, 0));
        TextView p = Ui.tv(c, "Friends get " + Ui.money(r.optDouble("referee_bonus")) + " bonus coins, you get " + Ui.money(r.optDouble("referrer_bonus")) + ".", 14, Ui.LG, false);
        p.setGravity(Gravity.CENTER);
        box.addView(p, Ui.lpm(-1, -2, 14, 6, 14, 6));
        LinearLayout pn = Web.panel(c, box);
        TextView lb = Ui.plabel(c, "YOUR PROMO CODE");
        lb.setGravity(Gravity.CENTER);
        pn.addView(lb);
        TextView code = Ui.tv(c, r.optString("code"), 26, Ui.BLACK, true);
        code.setGravity(Gravity.CENTER);
        code.setLetterSpacing(0.15f);
        code.setBackgroundResource(R.drawable.bg_codebox);
        code.setPadding(Ui.dp(12), Ui.dp(12), Ui.dp(12), Ui.dp(12));
        code.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { Ui.copy(c, r.optString("code")); } });
        pn.addView(code, Ui.lpm(-1, -2, 0, 8, 0, 8));
        LinearLayout steps = new LinearLayout(c);
        steps.setPadding(0, Ui.dp(10), 0, Ui.dp(10));
        String[][] st = {{"👤", "User Register"}, {"🎮", "Join Match"}, {"🎁", "Get Rewards"}};
        for (String[] x : st) {
            LinearLayout s = Ui.col(c);
            s.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView e = Ui.tv(c, x[0], 24, Ui.WHITE, false);
            e.setGravity(Gravity.CENTER);
            e.setBackgroundResource(R.drawable.bg_circle_dark);
            s.addView(e, Ui.lp(Ui.dp(52), Ui.dp(52)));
            s.addView(Ui.tv(c, x[1], 12, Ui.WHITE, false), Ui.lpm(-2, -2, 0, 6, 0, 0));
            steps.addView(s, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        box.addView(steps);
        Profile.stats(c, box, new String[]{String.valueOf(r.optInt("count")), Ui.money(r.optDouble("earned"))}, new String[]{"Friends joined", "Coins earned"});
        TextView go = Ui.btn(c, "Refer Now", Ui.RED);
        go.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Ui.share(c, Cfg.conf(c).optString("site_name") + " - join with my code " + r.optString("code") + " : " + Cfg.BASE + "register.php?ref=" + r.optString("code"));
            }
        });
        box.addView(Ui.btnbar(c, go));
        if (!r.optBoolean("has_referrer")) {
            LinearLayout pp = Web.panel(c, box);
            pp.addView(Ui.plabel(c, "Have a friend's code?"));
            final EditText e = Ui.pinput(c, "Enter referral code", InputType.TYPE_CLASS_TEXT);
            pp.addView(e);
            TextView ap = Ui.btn(c, "Apply code", Ui.SKY);
            ap.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Api.call("refer_apply", json("code", e.getText().toString().trim()), new Api.Cb() {
                        public void done(JSONObject x) { result(x, "Done", "Could not apply", new Runnable() { public void run() { load(); } }); }
                    });
                }
            });
            box.addView(Ui.btnbar(c, ap));
        }
    }
}

/** support.php */
class SupportScreen extends Screen {
    String title() { return "Customer Support"; }
    private final String[] cats = {"Add money", "Withdrawal", "Match / results", "Account", "Other"};

    void load() {
        loading();
        fetch("support", null, new OkCb() { public void run(JSONObject r) { render(r); } });
    }

    private TextView line(LinearLayout p, String k, String v) {
        LinearLayout l = Ui.row(c);
        l.setPadding(0, Ui.dp(3), 0, Ui.dp(3));
        l.addView(Ui.tv(c, k, 14, Ui.SLATE2, true), Ui.lp(Ui.dp(130), -2));
        TextView t = Ui.tv(c, v, 14, Ui.BLACK, true);
        l.addView(t, Ui.weight(1));
        p.addView(l);
        return t;
    }

    private void render(JSONObject r) {
        JSONArray of = r.optJSONArray("officers");
        for (int i = 0; of != null && i < of.length(); i++) {
            final JSONObject o = of.optJSONObject(i);
            LinearLayout k = Ui.col(c);
            k.setBackground(Ui.bg(Ui.WHITE, 6));
            k.setPadding(Ui.dp(12), Ui.dp(12), Ui.dp(12), Ui.dp(12));
            LinearLayout top = Ui.row(c);
            ImageView im = Ui.img(c, 56, 56, ImageView.ScaleType.CENTER_CROP);
            im.setBackgroundResource(R.drawable.bg_circle_border);
            im.setClipToOutline(true);
            top.addView(im);
            Img.load(im, o.optString("image"));
            LinearLayout nm = Ui.col(c);
            nm.setPadding(Ui.dp(10), 0, 0, 0);
            line(nm, "Officer name :", o.optString("name"));
            line(nm, "Post :", o.optString("post"));
            top.addView(nm, Ui.weight(1));
            k.addView(top);
            line(k, "Contact Time :", o.optString("time"));
            LinearLayout acts = new LinearLayout(c);
            acts.setPadding(0, Ui.dp(10), 0, 0);
            if (!o.optString("whatsapp").isEmpty()) {
                TextView w = Ui.btnSm(c, "WhatsApp", 0xFF25D366, Ui.WHITE);
                w.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { Ui.open(c, o.optString("whatsapp")); } });
                acts.addView(w, Ui.weightm(1, 0, 0, 4, 0));
            }
            if (!o.optString("telegram").isEmpty()) {
                TextView t = Ui.btnSm(c, "Telegram", 0xFF0088CC, Ui.WHITE);
                t.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { Ui.open(c, o.optString("telegram")); } });
                acts.addView(t, Ui.weightm(1, 4, 0, 0, 0));
            }
            k.addView(acts);
            box.addView(k, Ui.lpm(-1, -2, 8, 8, 8, 8));
        }
        box.addView(Ui.secbar(c, "Raise a ticket"));
        LinearLayout p = Web.panel(c, box);
        p.addView(Ui.plabel(c, "Support Category"));
        final android.widget.Spinner sp = new android.widget.Spinner(c);
        sp.setAdapter(new android.widget.ArrayAdapter<String>(c, android.R.layout.simple_spinner_dropdown_item, cats));
        sp.setBackground(Ui.bg(Ui.WHITE, 4));
        p.addView(sp, Ui.lp(-1, Ui.dp(48)));
        p.addView(Ui.plabel(c, "Message"));
        final EditText msg = Ui.pinput(c, "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        msg.setMinLines(4);
        msg.setGravity(Gravity.TOP);
        p.addView(msg);
        TextView go = Ui.btn(c, "Send", Ui.SKY);
        go.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Api.call("support_new", json("category", (String) sp.getSelectedItem(), "message", msg.getText().toString().trim()), new Api.Cb() {
                    public void done(JSONObject x) { result(x, "Sent", "Could not send", new Runnable() { public void run() { load(); } }); }
                });
            }
        });
        box.addView(Ui.btnbar(c, go));
        JSONArray tk = r.optJSONArray("tickets");
        for (int i = 0; tk != null && i < tk.length(); i++) {
            JSONObject t = tk.optJSONObject(i);
            View card = Ui.inflate(c, R.layout.item_card, box);
            ((TextView) card.findViewById(R.id.c_title)).setText(t.optString("category"));
            TextView rt = (TextView) card.findViewById(R.id.c_right);
            rt.setText(Character.toUpperCase(t.optString("status").charAt(0)) + t.optString("status").substring(1));
            LinearLayout body = (LinearLayout) card.findViewById(R.id.c_body);
            body.addView(Ui.tv(c, t.optString("message"), 14, 0xFF37474F, false));
            if (!t.optString("reply").isEmpty()) {
                TextView fl = Web.flash(c, body, "Support: " + t.optString("reply"), true);
                ((LinearLayout.LayoutParams) fl.getLayoutParams()).setMargins(0, Ui.dp(8), 0, 0);
            }
            body.addView(Ui.tv(c, t.optString("time"), 11, Ui.SLATE2, false));
            box.addView(card);
        }
    }
}

/** page.php (about / privacy / refund) */
class PageScreen extends Screen {
    private final String slug, ttl;
    PageScreen(String slug, String ttl) { this.slug = slug; this.ttl = ttl; }
    String title() { return ttl; }
    void load() {
        loading();
        fetch("page", json("slug", slug), new OkCb() {
            public void run(JSONObject r) {
                LinearLayout s = Ui.sec(c, box, null);
                String body = r.optString("body");
                TextView t = Ui.tv(c, android.text.Html.fromHtml(body.isEmpty() ? "Nothing here yet." : body).toString(), 14, 0xFF263238, false);
                t.setLineSpacing(0, 1.35f);
                t.setPadding(Ui.dp(14), Ui.dp(14), Ui.dp(14), Ui.dp(14));
                s.addView(t);
            }
        });
    }
}
