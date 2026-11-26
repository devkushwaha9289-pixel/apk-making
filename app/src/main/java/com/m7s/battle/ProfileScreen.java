package com.m7s.battle;

import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.HashMap;

/** Shared piece: .pstats */
final class Profile {
    private Profile() {}
    static void stats(Context c, LinearLayout parent, String[] vals, String[] labels) {
        LinearLayout s = new LinearLayout(c);
        s.setBackground(Ui.bg(Ui.LG, 6));
        s.setClipToOutline(true);
        for (int i = 0; i < vals.length; i++) {
            LinearLayout cell = Ui.col(c);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(Ui.dp(4), Ui.dp(12), Ui.dp(4), Ui.dp(12));
            cell.addView(Ui.tv(c, vals[i], 20, Ui.BLACK, true));
            cell.addView(Ui.tv(c, labels[i], 12, Ui.SLATE, true));
            if (i > 0) { View d = new View(c); d.setBackgroundColor(0xFF90A4AE); s.addView(d, Ui.lp(1, -1)); }
            s.addView(cell, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        parent.addView(s, Ui.lpm(-1, -2, 8, 8, 8, 8));
    }
}

/** profile.php : card, stats, menu, social circles. */
class ProfileScreen extends Screen {
    String title() { return "Profile"; }
    @Override boolean isTab() { return true; }

    void load() {
        loading();
        fetch("me", null, new OkCb() { public void run(JSONObject r) { render(r); } });
    }

    private void item(LinearLayout menu, int icon, String text, int color, boolean last, final Runnable run) {
        View v = Ui.inflate(c, R.layout.row_pmenu, menu);
        ((ImageView) v.findViewById(R.id.pm_icon)).setImageResource(icon);
        ((ImageView) v.findViewById(R.id.pm_icon)).setColorFilter(color == 0 ? Ui.AMBER : color);
        TextView t = (TextView) v.findViewById(R.id.pm_text);
        t.setText(text);
        if (color != 0) t.setTextColor(color);
        v.setOnClickListener(new View.OnClickListener() { public void onClick(View vv) { run.run(); } });
        if (color != 0) v.findViewById(R.id.pm_next).setVisibility(View.GONE);
        menu.addView(v);
        if (!last) menu.addView(Ui.divider(c, 0xFF37474F));
    }

    private void render(final JSONObject r) {
        final JSONObject u = r.optJSONObject("user");
        LinearLayout pc = Ui.row(c);
        pc.setBackground(Ui.bg(Ui.SLATE, 6));
        pc.setPadding(Ui.dp(14), Ui.dp(14), Ui.dp(14), Ui.dp(14));
        ImageView av = Ui.img(c, 70, 70, ImageView.ScaleType.CENTER_CROP);
        av.setBackgroundResource(R.drawable.bg_logo_av);
        av.setClipToOutline(true);
        pc.addView(av);
        Img.load(av, u.optString("image").isEmpty() ? Cfg.conf(c).optString("logo") : u.optString("image"));
        LinearLayout n = Ui.col(c);
        n.setPadding(Ui.dp(14), 0, 0, 0);
        n.addView(Ui.tv(c, u.optString("username"), 20, Ui.WHITE, true));
        n.addView(Ui.tv(c, u.optString("email"), 13, Ui.LG, false));
        n.addView(Ui.tv(c, "ID: " + u.optInt("id"), 13, Ui.LG, false));
        pc.addView(n);
        box.addView(pc, Ui.lpm(-1, -2, 8, 8, 8, 8));
        Profile.stats(c, box, new String[]{"" + u.optInt("matches"), "" + u.optInt("kills"), Ui.money(u.optDouble("won"))}, new String[]{"Matches Played", "Total Kills", "Coin Won"});

        LinearLayout menu = Ui.col(c);
        menu.setBackground(Ui.bg(Ui.MENU, 6));
        menu.setClipToOutline(true);
        item(menu, R.drawable.ic_user, "Edit profile", 0, false, new Runnable() { public void run() { a.push(new EditProfileScreen(u)); } });
        item(menu, R.drawable.ic_wallet, "My Wallet", 0, false, new Runnable() { public void run() { a.push(new WalletScreen()); } });
        item(menu, R.drawable.ic_trophy, "My Tournaments", 0, false, new Runnable() { public void run() { a.push(new MatchesScreen(0, "My Matches", true, "upcoming")); } });
        item(menu, R.drawable.ic_history, "Transactions History", 0, false, new Runnable() { public void run() { a.push(new TxScreen()); } });
        item(menu, R.drawable.ic_group, "My Referrals", 0, false, new Runnable() { public void run() { a.push(new ReferScreen()); } });
        item(menu, R.drawable.ic_bell, "Announcements", 0, false, new Runnable() { public void run() { a.push(new NotifScreen()); } });
        item(menu, R.drawable.ic_trophy, "Leaderboard", 0, false, new Runnable() { public void run() { a.push(new LeaderScreen()); } });
        item(menu, R.drawable.ic_share, "Share App", 0, false, new Runnable() {
            public void run() { Ui.share(c, Cfg.conf(c).optString("site_name") + " - " + Cfg.BASE + "register.php?ref=" + u.optString("referral_code")); }
        });
        item(menu, R.drawable.ic_lock, "Refund Policy", 0, false, new Runnable() { public void run() { a.push(new PageScreen("refund", "Refund Policy")); } });
        item(menu, R.drawable.ic_lock, "Privacy Policy", 0, false, new Runnable() { public void run() { a.push(new PageScreen("privacy", "Privacy Policy")); } });
        item(menu, R.drawable.ic_info, "About Us", 0, false, new Runnable() { public void run() { a.push(new PageScreen("about", "About Us")); } });
        item(menu, R.drawable.ic_headset, "Customer Support", 0, false, new Runnable() { public void run() { a.push(new SupportScreen()); } });
        item(menu, R.drawable.ic_info, "App Setting", 0, false, new Runnable() { public void run() { a.push(new SettingsScreen(u.optBoolean("push_on"))); } });
        if (u.optBoolean("is_admin")) item(menu, R.drawable.ic_lock, "Admin panel", 0, false, new Runnable() { public void run() { WebActivity.page(c, Cfg.BASE + "admin/index.php", "Admin panel"); } });
        else if (r.optBoolean("is_staff")) item(menu, R.drawable.ic_lock, "Staff panel", 0, false, new Runnable() { public void run() { WebActivity.page(c, Cfg.BASE + "staff/index.php", "Staff panel"); } });
        item(menu, R.drawable.ic_exit, "Logout", Ui.RED, true, new Runnable() {
            public void run() {
                Ui.confirm(c, "Logout", "Do you want to logout?", new Runnable() {
                    public void run() {
                        Api.call("logout", json("token", Cfg.get(c, "fcm")), new Api.Cb() {
                            public void done(JSONObject x) { Cfg.setToken(c, ""); if (Api.onAuthLost != null) Api.onAuthLost.run(); }
                        });
                    }
                });
            }
        });
        box.addView(menu, Ui.lpm(-1, -2, 8, 8, 8, 8));

        JSONObject ln = r.optJSONObject("links");
        if (ln != null) {
            LinearLayout soc = new LinearLayout(c);
            soc.setGravity(Gravity.CENTER);
            soc.setPadding(0, Ui.dp(12), 0, Ui.dp(12));
            String[][] sc = {{"instagram", "IG"}, {"telegram", "TG"}, {"youtube", "YT"}, {"whatsapp", "WA"}};
            for (String[] x : sc) {
                final String url = ln.optString(x[0]);
                if (url.isEmpty()) continue;
                TextView b = Ui.tv(c, x[1], 12, Ui.WHITE, true);
                b.setGravity(Gravity.CENTER);
                b.setBackgroundResource(R.drawable.bg_circle_dark);
                b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { Ui.open(c, url); } });
                soc.addView(b, Ui.lpm(Ui.dp(44), Ui.dp(44), 8, 0, 8, 0));
            }
            box.addView(soc);
        }
    }
}

/** edit_profile.php */
class EditProfileScreen extends Screen {
    private final JSONObject u;
    private byte[] photo;
    private ImageView av;

    EditProfileScreen(JSONObject u) { this.u = u; }
    String title() { return "Edit profile"; }
    @Override void onShow() {}

    void load() {
        box.removeAllViews();
        av = Ui.img(c, 96, 96, ImageView.ScaleType.CENTER_CROP);
        av.setBackgroundResource(R.drawable.bg_logo_av);
        av.setClipToOutline(true);
        LinearLayout.LayoutParams lp = Ui.lpm(Ui.dp(96), Ui.dp(96), 0, 16, 0, 4);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        box.addView(av, lp);
        Img.load(av, u.optString("image").isEmpty() ? Cfg.conf(c).optString("logo") : u.optString("image"));
        LinearLayout p = Web.panel(c, box);
        p.addView(Ui.plabel(c, "Edit profile pic"));
        TextView ch = Ui.btnSm(c, "Choose photo", Ui.WHITE, Ui.BLACK);
        ch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                a.pickImage(new MainActivity.ImageCb() {
                    public void got(byte[] jpeg) {
                        photo = jpeg;
                        av.setTag(null);
                        av.setImageBitmap(android.graphics.BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length));
                    }
                });
            }
        });
        p.addView(ch, Ui.lp(-2, -2));
        p.addView(Ui.plabel(c, "Full Name"));
        final EditText fn = Ui.pinput(c, "Enter Full Name", InputType.TYPE_CLASS_TEXT);
        fn.setText(u.optString("full_name"));
        p.addView(fn);
        p.addView(Ui.plabel(c, "Username"));
        final EditText un = Ui.pinput(c, "Username", InputType.TYPE_CLASS_TEXT);
        un.setText(u.optString("username"));
        p.addView(un);
        p.addView(Ui.plabel(c, "Email"));
        EditText em = Ui.pinput(c, "", InputType.TYPE_CLASS_TEXT);
        em.setText(u.optString("email"));
        em.setEnabled(false);
        em.setAlpha(0.6f);
        p.addView(em);
        p.addView(Ui.plabel(c, "Code (your referral code)"));
        EditText rc = Ui.pinput(c, "", InputType.TYPE_CLASS_TEXT);
        rc.setText(u.optString("referral_code"));
        rc.setEnabled(false);
        rc.setAlpha(0.6f);
        p.addView(rc);
        p.addView(Ui.plabel(c, "Mobile Number"));
        final EditText ph = Ui.pinput(c, "Mobile Number", InputType.TYPE_CLASS_PHONE);
        ph.setText(u.optString("phone"));
        p.addView(ph);
        TextView save = Ui.btn(c, "Save", Ui.SKY);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                HashMap<String, String> f = new HashMap<>();
                f.put("username", un.getText().toString().trim());
                f.put("full_name", fn.getText().toString().trim());
                f.put("phone", ph.getText().toString().trim());
                Api.upload("profile_update", f, "avatar", photo, "avatar.jpg", new Api.Cb() {
                    public void done(JSONObject r) {
                        if (r.optBoolean("ok")) { Ui.toast(c, r.optString("message")); a.refreshHome(); a.onBackPressed(); }
                        else Ui.info(c, "Could not save", r.optString("error"));
                    }
                });
            }
        });
        box.addView(Ui.btnbar(c, save));

        box.addView(Ui.secbar(c, "Reset Password"));
        LinearLayout pw = Web.panel(c, box);
        final int PW = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
        pw.addView(Ui.plabel(c, "Old Password"));
        final EditText o = Ui.pinput(c, "Old Password", PW);
        pw.addView(o);
        pw.addView(Ui.plabel(c, "New Password"));
        final EditText n1 = Ui.pinput(c, "New Password", PW);
        pw.addView(n1);
        pw.addView(Ui.plabel(c, "Retype New Password"));
        final EditText n2 = Ui.pinput(c, "Retype New Password", PW);
        pw.addView(n2);
        TextView cp = Ui.btn(c, "Change Password", Ui.INDIGO);
        cp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!n1.getText().toString().equals(n2.getText().toString())) { Ui.info(c, "Check", "New passwords do not match."); return; }
                Api.call("password_change", json("old", o.getText().toString(), "new", n1.getText().toString()), new Api.Cb() {
                    public void done(JSONObject r) { result(r, "Done", "Could not change", null); }
                });
            }
        });
        box.addView(Ui.btnbar(c, cp));
    }
}

/** settings.php : push notification switch + announcements link. */
class SettingsScreen extends Screen {
    private final boolean on;
    SettingsScreen(boolean on) { this.on = on; }
    String title() { return "App Setting"; }
    @Override void onShow() {}

    void load() {
        box.removeAllViews();
        box.addView(Ui.secbar(c, "Notifications"));
        LinearLayout t = Ui.row(c);
        t.setBackground(Ui.bg(0xFF37474F, 6));
        t.setPadding(Ui.dp(14), Ui.dp(14), Ui.dp(14), Ui.dp(14));
        LinearLayout tx = Ui.col(c);
        tx.addView(Ui.tv(c, "Push notifications", 14, Ui.WHITE, false));
        final TextView st = Ui.tv(c, on ? "Enabled" : "Disabled", 12, Ui.SUB, false);
        tx.addView(st);
        t.addView(tx, Ui.weight(1));
        Switch sw = new Switch(c);
        sw.setChecked(on);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean v) {
                st.setText(v ? "Enabled" : "Disabled");
                Api.call("push_pref", json("on", v ? "1" : "0"), null);
                if (v) Push.register(c);
            }
        });
        t.addView(sw);
        box.addView(t, Ui.lpm(-1, -2, 8, 8, 8, 8));
        LinearLayout an = Ui.row(c);
        an.setBackground(Ui.bg(0xFF37474F, 6));
        an.setPadding(Ui.dp(14), Ui.dp(14), Ui.dp(14), Ui.dp(14));
        an.addView(Ui.tv(c, "Announcements", 14, Ui.WHITE, false), Ui.weight(1));
        TextView open = Ui.tv(c, "Open", 14, 0xFFFFEB3B, false);
        open.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.push(new NotifScreen()); } });
        an.addView(open);
        box.addView(an, Ui.lpm(-1, -2, 8, 8, 8, 8));
    }
}
