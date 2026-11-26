package com.m7s.battle;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Colours/fonts/builders that copy the website's CSS (assets/css/app.css) one-to-one. */
final class Ui {
    // CSS :root variables
    static final int TEAL = 0xFF008080, DARK = 0xFF1E1E2E, BLACK = 0xFF000000, RED = 0xFFF44336, AMBER = 0xFFFFC107, ORANGE = 0xFFFF9800,
            GOLD = 0xFFFFB300, PURPLE = 0xFF6000FF, BLUE = 0xFF008DCD, SKY = 0xFF03A9F4, SLATE = 0xFF455A64, SLATE2 = 0xFF546E7A,
            LG = 0xFFCFD8DC, LG2 = 0xFFECEFF1, FIELD = 0xFFEEEEEE, GREEN = 0xFF4CAF50, CYAN = 0xFF18FFFF, INDIGO = 0xFF303F9F,
            MENU = 0xFF23283A, WHITE = 0xFFFFFFFF, TXT = 0xFFFFFFFF, SUB = 0xFFB0BEC5, LINE = 0x40607D8B;
    static Typeface FB, SQ, NARROW, PUBG;
    private static float D = 3f;

    private Ui() {}

    static void init(Context c) {
        D = c.getResources().getDisplayMetrics().density;
        if (FB == null) {
            try {
                FB = Typeface.createFromAsset(c.getAssets(), "fonts/facebooksans.ttf");
                SQ = Typeface.createFromAsset(c.getAssets(), "fonts/squadaone_regular.ttf");
                NARROW = Typeface.createFromAsset(c.getAssets(), "fonts/facebooknarrow.ttf");
                PUBG = Typeface.createFromAsset(c.getAssets(), "fonts/pubg_font.ttf");
            } catch (Exception e) { FB = Typeface.DEFAULT; SQ = Typeface.DEFAULT; NARROW = Typeface.DEFAULT; PUBG = Typeface.DEFAULT; }
        }
    }
    static int dp(int v) { return Math.round(v * D); }

    /** Website font (FBSans) on every TextView inside v, keeping bold/italic. */
    static void font(View v) {
        if (v instanceof TextView) {
            TextView t = (TextView) v;
            if (t.getTag() != null && "nofont".equals(t.getTag())) return;
            Typeface cur = t.getTypeface();
            int st = cur == null ? Typeface.NORMAL : cur.getStyle();
            t.setTypeface(FB, st);
        } else if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) font(g.getChildAt(i));
        }
    }

    static View inflate(Context c, int layout, ViewGroup parent) {
        View v = LayoutInflater.from(c).inflate(layout, parent, false);
        font(v);
        return v;
    }

    static GradientDrawable bg(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }
    static GradientDrawable border(int fill, int stroke, int widthDp, int radiusDp) {
        GradientDrawable g = bg(fill, radiusDp);
        g.setStroke(dp(widthDp), stroke);
        return g;
    }

    /** Website's <div data-lottie="assets/anim/x.json">: looping Lottie from bundled assets. */
    static void lottie(Context c, LinearLayout parent, String file, int heightDp, int topDp, int bottomDp) {
        com.airbnb.lottie.LottieAnimationView v = new com.airbnb.lottie.LottieAnimationView(c);
        v.setAnimation("anim/" + file);
        v.setRepeatCount(com.airbnb.lottie.LottieDrawable.INFINITE);
        v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        v.playAnimation();
        parent.addView(v, lpm(-1, dp(heightDp), 0, topDp, 0, bottomDp));
    }

    static LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }
    static LinearLayout.LayoutParams lpm(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }
    static LinearLayout.LayoutParams weight(float w) { return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w); }
    static LinearLayout.LayoutParams weightm(float w, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    static TextView tv(Context c, String t, int sp, int color, boolean bold) {
        TextView v = new TextView(c);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setTypeface(FB, bold ? Typeface.BOLD : Typeface.NORMAL);
        return v;
    }

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }
    static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static ImageView img(Context c, int wDp, int hDp, ImageView.ScaleType st) {
        ImageView v = new ImageView(c);
        v.setScaleType(st);
        v.setLayoutParams(lp(wDp < 0 ? wDp : dp(wDp), hDp < 0 ? hDp : dp(hDp)));
        return v;
    }
    static ImageView icon(Context c, int res, int sizeDp, int tint) {
        ImageView v = new ImageView(c);
        v.setImageResource(res);
        if (tint != 0) v.setColorFilter(tint);
        v.setLayoutParams(lp(dp(sizeDp), dp(sizeDp)));
        return v;
    }

    /** coin icon + amount (the website's coin_amt()). */
    static LinearLayout coinAmt(Context c, String amount, int sp, int color, boolean bold) {
        LinearLayout r = row(c);
        r.addView(icon(c, R.drawable.ic_coin, sp + 4, 0));
        TextView t = tv(c, amount, sp, color, bold);
        t.setPadding(dp(3), 0, 0, 0);
        r.addView(t);
        return r;
    }

    // ---------- .btn / .btn.sm ----------
    static TextView btn(Context c, String t, int color) {
        TextView b = tv(c, t, 18, WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10), dp(14), dp(10), dp(14));
        b.setBackgroundColor(color);
        b.setClickable(true);
        return b;
    }
    static TextView btnSm(Context c, String t, int color, int textColor) {
        TextView b = tv(c, t, 13, textColor, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(14), dp(8), dp(14), dp(8));
        b.setBackground(bg(color, 4));
        b.setClickable(true);
        return b;
    }
    /** .btnbar : full-width buttons side by side. */
    static LinearLayout btnbar(Context c, TextView... bs) {
        LinearLayout l = new LinearLayout(c);
        l.setPadding(dp(8), dp(8), dp(8), dp(8));
        for (int i = 0; i < bs.length; i++) l.addView(bs[i], weightm(1, i == 0 ? 0 : 8, 0, 0, 0));
        return l;
    }

    // ---------- .secbar  (teal gradient, Squada font) ----------
    static TextView secbar(Context c, String t) {
        TextView v = tv(c, t, 18, WHITE, true);
        v.setTypeface(SQ);
        v.setTag("nofont");
        v.setBackgroundResource(R.drawable.bg_secbar);
        v.setPadding(dp(12), dp(8), dp(12), dp(8));
        v.setLayoutParams(lpm(-1, -2, 0, 6, 0, 6));
        return v;
    }

    // ---------- .sec  (light-grey box with teal title) : returns the body to add rows into ----------
    static LinearLayout sec(Context c, LinearLayout parent, String title) {
        LinearLayout s = col(c);
        s.setBackground(bg(LG, 6));
        s.setClipToOutline(true);
        if (title != null) {
            TextView h = tv(c, title, 16, WHITE, true);
            h.setBackgroundColor(TEAL);
            h.setPadding(dp(12), dp(9), dp(12), dp(9));
            s.addView(h);
        }
        LinearLayout body = col(c);
        body.setPadding(dp(12), dp(6), dp(12), dp(6));
        s.addView(body);
        parent.addView(s, lpm(-1, -2, 8, 8, 8, 8));
        return body;
    }

    /** .kv row inside .sec / .jbox. */
    static LinearLayout kv(Context c, String k, String v) {
        LinearLayout r = row(c);
        r.setPadding(0, dp(8), 0, dp(8));
        r.addView(tv(c, k, 14, SLATE2, true), weight(1));
        TextView b = tv(c, v, 14, BLACK, true);
        b.setGravity(Gravity.END);
        r.addView(b, weight(1));
        return r;
    }
    static View divider(Context c, int color) {
        View d = new View(c);
        d.setBackgroundColor(color);
        d.setLayoutParams(lp(-1, 1));
        return d;
    }

    // ---------- .panel (dark) form pieces ----------
    static TextView plabel(Context c, String t) {
        TextView l = tv(c, t, 12, SUB, false);
        l.setPadding(0, dp(12), 0, dp(4));
        return l;
    }
    static EditText pinput(Context c, String hint, int type) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(0xFF607D8B);
        e.setTextColor(BLACK);
        e.setTextSize(16);
        e.setTypeface(FB);
        e.setInputType(type);
        e.setBackground(bg(WHITE, 4));
        e.setPadding(dp(12), dp(12), dp(12), dp(12));
        return e;
    }
    static TextView pbox(Context c, String t) {
        TextView b = tv(c, t, 16, BLACK, true);
        b.setBackground(bg(WHITE, 4));
        b.setPadding(dp(10), dp(10), dp(10), dp(10));
        return b;
    }
    static TextView note(Context c, String t, int color) {
        TextView n = tv(c, t, 13, color, false);
        n.setLineSpacing(0, 1.3f);
        n.setPadding(0, dp(12), 0, dp(12));
        return n;
    }

    static String money(double d) {
        if (d == Math.floor(d)) return String.valueOf((long) d);
        String s = String.format(java.util.Locale.US, "%.2f", d);
        if (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        return s;
    }

    // ---------- dialogs ----------
    static void toast(Context c, String s) { Toast.makeText(c, s, Toast.LENGTH_LONG).show(); }

    static void confirm(Context c, String title, String msg, final Runnable yes) {
        new AlertDialog.Builder(c).setTitle(title).setMessage(msg)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { yes.run(); }
                }).show();
    }
    static void info(Context c, String title, String msg) {
        new AlertDialog.Builder(c).setTitle(title).setMessage(msg).setPositiveButton("OK", null).show();
    }
    static void copy(Context c, String text) {
        ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) { cm.setPrimaryClip(ClipData.newPlainText("copy", text)); toast(c, "Copied"); }
    }
    static void open(Context c, String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Exception e) { toast(c, "Cannot open link"); }
    }
    static void share(Context c, String text) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, text);
        c.startActivity(Intent.createChooser(i, "Share"));
    }

    static String countdown(long targetSec) {
        long s = targetSec - Cfg.now() / 1000L;
        if (s <= 0) return "Started";
        long d = s / 86400, h = (s % 86400) / 3600, m = (s % 3600) / 60, x = s % 60;
        return (d > 0 ? d + "d " : "") + String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, x);
    }

    static int textInput() { return InputType.TYPE_CLASS_TEXT; }
}
