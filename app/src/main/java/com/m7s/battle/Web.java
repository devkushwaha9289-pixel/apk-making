package com.m7s.battle;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small components that repeat across pages: .jbox, .flash, .rules, .panel, .badge, .kv rows, expandable rows, tables. */
final class Web {
    interface Pick { void on(int i); }
    private Web() {}

    /** .jbox : white rounded box with label/value rows. */
    static LinearLayout jbox(Context c, LinearLayout parent) {
        LinearLayout b = Ui.col(c);
        b.setBackground(Ui.bg(Ui.WHITE, 6));
        b.setClipToOutline(true);
        parent.addView(b, Ui.lpm(-1, -2, 8, 8, 8, 8));
        return b;
    }
    static void jr(Context c, LinearLayout box, String label, String value) {
        TextView v = Ui.tv(c, value, 14, Ui.BLACK, true);
        v.setGravity(Gravity.END);
        jr(c, box, label, v);
    }
    static void jr(Context c, LinearLayout box, String label, View value) {
        LinearLayout r = Ui.row(c);
        r.setPadding(Ui.dp(12), Ui.dp(10), Ui.dp(12), Ui.dp(10));
        r.addView(Ui.tv(c, label, 14, Ui.SLATE2, true), Ui.weight(1));
        r.addView(value, Ui.lp(-2, -2));
        box.addView(r);
        box.addView(Ui.divider(c, 0xFFECEFF1));
    }
    /** .kv row inside .sec (label slate2 bold, value black bold, thin divider). */
    static void kv(Context c, LinearLayout box, String label, String value) {
        TextView v = Ui.tv(c, value, 14, Ui.BLACK, true);
        v.setGravity(Gravity.END);
        kv(c, box, label, v);
    }
    static void kv(Context c, LinearLayout box, String label, View value) {
        LinearLayout r = Ui.row(c);
        r.setPadding(0, Ui.dp(8), 0, Ui.dp(8));
        r.addView(Ui.tv(c, label, 14, Ui.SLATE2, true), Ui.weight(1));
        r.addView(value, Ui.lp(-2, -2));
        box.addView(r);
        box.addView(Ui.divider(c, 0x40607D8B));
    }
    static LinearLayout coinValue(Context c, double amt) { return Ui.coinAmt(c, Ui.money(amt), 14, Ui.BLACK, true); }

    /** .flash-ok / .flash-err */
    static TextView flash(Context c, LinearLayout parent, String text, boolean ok) {
        TextView f = Ui.tv(c, text, 14, ok ? 0xFF1B5E20 : 0xFFB71C1C, true);
        f.setBackground(Ui.bg(ok ? 0xFFC8E6C9 : 0xFFFFCDD2, 4));
        f.setPadding(Ui.dp(12), Ui.dp(10), Ui.dp(12), Ui.dp(10));
        parent.addView(f, Ui.lpm(-1, -2, 8, 8, 8, 8));
        return f;
    }

    /** .rules : cyan text in a dark box with thin cyan border. */
    static void rules(Context c, LinearLayout parent, String title, String text, int textColor) {
        LinearLayout r = Ui.col(c);
        r.setBackground(Ui.border(Ui.DARK, 0x4D18FFFF, 1, 6));
        r.setPadding(Ui.dp(14), Ui.dp(12), Ui.dp(14), Ui.dp(12));
        r.addView(Ui.tv(c, title, 15, Ui.AMBER, true));
        TextView t = Ui.tv(c, text, 13, textColor, false);
        t.setLineSpacing(0, 1.35f);
        t.setPadding(0, Ui.dp(6), 0, 0);
        r.addView(t);
        parent.addView(r, Ui.lpm(-1, -2, 8, 8, 8, 8));
    }

    /** .panel : dark form area. */
    static LinearLayout panel(Context c, LinearLayout parent) {
        LinearLayout p = Ui.col(c);
        p.setBackgroundColor(Ui.DARK);
        p.setPadding(Ui.dp(14), Ui.dp(10), Ui.dp(14), Ui.dp(10));
        parent.addView(p, Ui.lp(-1, -2));
        return p;
    }

    /** .badge b-green / b-gold / b-red / b-blue / b-gray */
    static TextView badge(Context c, String status) {
        int bg = 0xFFB0BEC5, fg = 0xFF263238;
        switch (status) {
            case "success": case "ongoing": case "winner": bg = 0xFFC8E6C9; fg = 0xFF1B5E20; break;
            case "pending": case "refunded": bg = 0xFFFFE082; fg = 0xFF6D4C00; break;
            case "rejected": case "cancelled": bg = 0xFFFFCDD2; fg = 0xFFB71C1C; break;
            case "upcoming": case "open": bg = 0xFFBBDEFB; fg = 0xFF0D47A1; break;
            default: break;
        }
        String t = status.isEmpty() ? "" : Character.toUpperCase(status.charAt(0)) + status.substring(1);
        TextView b = Ui.tv(c, t, 11, fg, true);
        b.setBackground(Ui.bg(bg, 10));
        b.setPadding(Ui.dp(7), Ui.dp(2), Ui.dp(7), Ui.dp(2));
        return b;
    }

    /** .rowlink inside .sec : tap to expand/collapse. Returns the body (initially hidden unless open). */
    static LinearLayout details(Context c, LinearLayout sec, String title, boolean open) {
        final LinearLayout head = Ui.row(c);
        head.setPadding(Ui.dp(12), Ui.dp(12), Ui.dp(12), Ui.dp(12));
        head.addView(Ui.tv(c, title, 14, Ui.SLATE, true), Ui.weight(1));
        final ImageView arrow = Ui.icon(c, R.drawable.ic_next, 24, Ui.SLATE);
        head.addView(arrow);
        final LinearLayout body = Ui.col(c);
        body.setVisibility(open ? View.VISIBLE : View.GONE);
        arrow.setRotation(open ? 90 : 0);
        head.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean show = body.getVisibility() != View.VISIBLE;
                body.setVisibility(show ? View.VISIBLE : View.GONE);
                arrow.setRotation(show ? 90 : 0);
            }
        });
        sec.addView(Ui.divider(c, 0x40607D8B));
        sec.addView(head);
        sec.addView(body);
        return body;
    }

    /** .wb table row. header=true -> grey header. */
    static void wbRow(Context c, LinearLayout table, boolean header, String... cells) {
        LinearLayout r = new LinearLayout(c);
        for (int i = 0; i < cells.length; i++) {
            TextView t = Ui.tv(c, cells[i], 14, header ? Ui.WHITE : Ui.BLACK, header);
            t.setBackgroundColor(header ? Ui.SLATE2 : Ui.WHITE);
            t.setPadding(Ui.dp(8), Ui.dp(8), Ui.dp(8), Ui.dp(8));
            if (i == cells.length - 1 && cells.length > 1) t.setGravity(Gravity.END);
            r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        table.addView(r);
        table.addView(Ui.divider(c, 0xFFB0BEC5));
    }

    /** .copy button (amber). */
    static TextView copyBtn(final Context c, final String value) {
        TextView b = Ui.tv(c, "Copy", 14, Ui.BLACK, true);
        b.setBackground(Ui.bg(Ui.AMBER, 4));
        b.setPadding(Ui.dp(12), Ui.dp(6), Ui.dp(12), Ui.dp(6));
        b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { Ui.copy(c, value); } });
        return b;
    }

    /** .item body line: label on the left, bold value on the right. */
    static void ib(Context c, LinearLayout body, String label, String value) {
        LinearLayout r = Ui.row(c);
        r.setPadding(0, Ui.dp(3), 0, Ui.dp(3));
        r.addView(Ui.tv(c, label, 13, Ui.SLATE2, false), Ui.weight(1));
        TextView v = Ui.tv(c, value, 13, Ui.BLACK, true);
        v.setGravity(Gravity.END);
        r.addView(v, Ui.lp(-2, -2));
        body.addView(r);
    }

    /** .ptabs : purple tab bar with amber underline on the active tab. */
    static LinearLayout ptabs(Context c, String[] labels, int active, final Pick pick) {
        LinearLayout t = new LinearLayout(c);
        for (int i = 0; i < labels.length; i++) {
            final int k = i;
            TextView b = Ui.tv(c, labels[i], 16, i == active ? Ui.AMBER : Ui.WHITE, true);
            b.setGravity(Gravity.CENTER);
            b.setBackgroundResource(i == active ? R.drawable.bg_tab_on : R.drawable.bg_tab_off);
            b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { pick.on(k); } });
            t.addView(b, new LinearLayout.LayoutParams(0, -1, 1f));
        }
        t.setLayoutParams(Ui.lp(-1, Ui.dp(50)));
        return t;
    }
}
