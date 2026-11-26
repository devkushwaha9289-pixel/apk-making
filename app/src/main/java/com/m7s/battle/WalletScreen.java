package com.m7s.battle;

import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

/** wallet.php : golden hero card (Total Coin / ADD COIN / WITHDRAWAL), 3 wallet lines, 3 link notes. */
class WalletScreen extends Screen {
    String title() { return "My Wallet"; }

    void load() {
        loading();
        fetch("wallet", null, new OkCb() { public void run(JSONObject r) { render(r); } });
    }

    private void note(String text, final Runnable go) {
        LinearLayout n = Ui.row(c);
        n.setBackgroundResource(R.drawable.bg_wnote);
        n.setElevation(Ui.dp(2));
        n.setPadding(Ui.dp(12), Ui.dp(12), Ui.dp(12), Ui.dp(12));
        n.addView(Ui.tv(c, text, 16, Ui.SLATE2, false), Ui.weight(1));
        n.addView(Ui.icon(c, R.drawable.ic_next, 24, Ui.SLATE2));
        n.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { go.run(); } });
        box.addView(n, Ui.lpm(-1, -2, 8, 8, 8, 8));
    }

    private void render(final JSONObject r) {
        JSONObject w = r.optJSONObject("wallets");
        LinearLayout hero = Ui.row(c);
        hero.setBackgroundResource(R.drawable.bg_whero);
        hero.setElevation(Ui.dp(4));
        hero.setPadding(Ui.dp(12), Ui.dp(12), Ui.dp(12), Ui.dp(12));
        TextView big = Ui.tv(c, "💰", 56, Ui.BLACK, false);
        big.setGravity(Gravity.CENTER);
        hero.addView(big, Ui.lp(Ui.dp(96), -2));
        LinearLayout right = Ui.col(c);
        right.setPadding(Ui.dp(12), 0, 0, 0);
        right.addView(Ui.tv(c, "Total Coin", 18, 0xFFFFA000, true));
        LinearLayout tot = Ui.coinAmt(c, String.format(java.util.Locale.US, "%.2f", w.optDouble("total")), 28, 0xFF263238, true);
        right.addView(tot, Ui.lpm(-2, -2, 0, 4, 0, 8));
        TextView add = Ui.tv(c, "ADD COIN", 14, Ui.WHITE, true);
        add.setGravity(Gravity.CENTER);
        add.setBackgroundResource(R.drawable.bg_btn_add);
        add.setPadding(Ui.dp(8), Ui.dp(10), Ui.dp(8), Ui.dp(10));
        add.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.push(new RechargeScreen()); } });
        right.addView(add, Ui.lpm(-1, -2, 0, 0, 0, 6));
        TextView wd = Ui.tv(c, "🏧 WITHDRAWAL", 14, Ui.WHITE, true);
        wd.setGravity(Gravity.CENTER);
        wd.setBackgroundResource(R.drawable.bg_btn_wd);
        wd.setPadding(Ui.dp(8), Ui.dp(10), Ui.dp(8), Ui.dp(10));
        wd.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.push(new WithdrawScreen()); } });
        right.addView(wd, Ui.lp(-1, -2));
        hero.addView(right, Ui.weight(1));
        box.addView(hero, Ui.lpm(-1, -2, 8, 8, 8, 8));

        LinearLayout card = Ui.col(c);
        card.setBackgroundResource(R.drawable.bg_wcard);
        card.setElevation(Ui.dp(3));
        String[][] lines = {{"🐷", "DEPOSITED", "deposit", "Money you added. Use it to join matches and lucky draws."},
                {"🏧", "WITHDRAWABLE", "winning", "Your prize money. This is the only wallet you can withdraw from."},
                {"🎁", "BONUS", "bonus", "Referral and promo bonus. Use it to join matches."}};
        for (int i = 0; i < 3; i++) {
            final String tip = lines[i][3];
            LinearLayout l = Ui.row(c);
            l.setPadding(Ui.dp(12), Ui.dp(12), Ui.dp(12), Ui.dp(12));
            TextView em = Ui.tv(c, lines[i][0], 24, Ui.BLACK, false);
            em.setGravity(Gravity.CENTER);
            l.addView(em, Ui.lp(Ui.dp(40), -2));
            l.addView(Ui.tv(c, lines[i][1], 16, 0xFF37474F, true), Ui.weightm(1, 10, 0, 0, 0));
            l.addView(Ui.coinAmt(c, Ui.money(w.optDouble(lines[i][2])), 15, Ui.BLACK, true));
            ImageView inf = Ui.icon(c, R.drawable.ic_info, 24, Ui.SLATE2);
            inf.setPadding(Ui.dp(8), 0, 0, 0);
            inf.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { Ui.info(c, "Info", tip); } });
            l.addView(inf, Ui.lp(Ui.dp(32), Ui.dp(24)));
            if (i > 0) card.addView(Ui.divider(c, 0xFF78909C));
            card.addView(l);
        }
        box.addView(card, Ui.lpm(-1, -2, 8, 8, 8, 8));
        note("Update withdrawal account details", new Runnable() { public void run() { a.push(new AccountScreen()); } });
        note("Money Converter (Winning → Deposit)", new Runnable() { public void run() { a.push(new ExchangeScreen()); } });
        note("Transactions history", new Runnable() { public void run() { a.push(new TxScreen()); } });
    }
}

/** recharge.php : pack list, then the pay page (online + manual UPI/UTR), Deposit Rules. */
class RechargeScreen extends Screen {
    private final JSONObject pre;   // set when a pack is chosen
    RechargeScreen() { this.pre = null; }
    RechargeScreen(JSONObject pkg) { this.pre = pkg; }
    String title() { return pre == null ? "Add Coin" : "Add Coin"; }

    void load() {
        loading();
        fetch("wallet", null, new OkCb() { public void run(JSONObject r) { if (pre == null) list(r); else pay(r); } });
    }

    private void list(JSONObject r) {
        JSONArray pk = r.optJSONArray("packages");
        if (pk == null || pk.length() == 0) empty("🪙", "No packs available", "Recharge packs are not available right now.");
        for (int i = 0; pk != null && i < pk.length(); i++) {
            final JSONObject p = pk.optJSONObject(i);
            View v = Ui.inflate(c, R.layout.item_pack, box);
            ((TextView) v.findViewById(R.id.p_coins)).setText(Ui.money(p.optDouble("coins")) + " Coins !");
            double extra = p.optDouble("coins") - p.optDouble("money");
            ((TextView) v.findViewById(R.id.p_extra)).setText(extra > 0 ? "+" + Ui.money(extra) + " extra" : "");
            ((TextView) v.findViewById(R.id.p_price)).setText(Ui.money(p.optDouble("money")) + " INR");
            v.setOnClickListener(new View.OnClickListener() { public void onClick(View vv) { a.push(new RechargeScreen(p)); } });
            box.addView(v);
        }
        Web.rules(c, box, "Deposit Rules", r.optString("deposit_rules"), Ui.CYAN);
    }

    private void pay(final JSONObject r) {
        final JSONObject pkg = pre;
        LinearLayout p = Web.panel(c, box);
        p.addView(Ui.plabel(c, "Order Amount ( INR )"));
        p.addView(Ui.pbox(c, "₹" + Ui.money(pkg.optDouble("money"))));
        p.addView(Ui.plabel(c, "Get Coin"));
        LinearLayout gc = Ui.coinAmt(c, Ui.money(pkg.optDouble("coins")), 16, Ui.BLACK, true);
        gc.setBackground(Ui.bg(Ui.WHITE, 4));
        gc.setPadding(Ui.dp(10), Ui.dp(10), Ui.dp(10), Ui.dp(10));
        p.addView(gc);

        if (r.optBoolean("gateway")) {
            JSONArray ms = r.optJSONArray("merchants");
            for (int i = 0; ms != null && i < ms.length(); i++) {
                final String mer = ms.optString(i);
                String lab = mer.equals("paytm") ? " (PAYTM)" : (mer.equals("fampay") ? " (FAMPAY)" : "");
                TextView b = Ui.btn(c, "PAY ONLINE - AUTO COINS" + lab, Ui.GREEN);
                b.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Api.call("recharge_online", json("pkg", pkg.optInt("id"), "merchant", mer), new Api.Cb() {
                            public void done(JSONObject x) {
                                if (!x.optBoolean("ok")) { Ui.info(c, "Payment", x.optString("error")); return; }
                                WebActivity.pay(c, x.optString("payment_url"));
                            }
                        });
                    }
                });
                p.addView(b, Ui.lpm(-1, -2, 0, 14, 0, 0));
            }
            p.addView(Ui.note(c, "Coins are added automatically as soon as the payment is confirmed.", Ui.WHITE));
        }
        if (r.optBoolean("manual")) {
            final String order = r.optString("order_id"), upi = r.optString("upi_id");
            p.addView(Ui.plabel(c, "Order Id"));
            p.addView(Ui.pbox(c, order));
            p.addView(Ui.plabel(c, "Receiving Upi Id"));
            LinearLayout ub = Ui.row(c);
            ub.setBackground(Ui.bg(Ui.WHITE, 4));
            ub.setPadding(Ui.dp(10), Ui.dp(8), Ui.dp(10), Ui.dp(8));
            ub.addView(Ui.tv(c, upi.isEmpty() ? "getting upi please wait..." : upi, 16, Ui.BLACK, true), Ui.weight(1));
            ub.addView(Web.copyBtn(c, upi));
            TextView payB = Ui.btnSm(c, "Pay", Ui.GREEN, Ui.WHITE);
            payB.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String url = "upi://pay?pa=" + Uri.encode(upi) + "&pn=" + Uri.encode(r.optString("site_name")) + "&am=" + pkg.optDouble("money") + "&cu=INR&tn=" + Uri.encode(order);
                    Ui.open(c, url);
                }
            });
            ub.addView(payB, Ui.lpm(-2, -2, 6, 0, 0, 0));
            p.addView(ub);
            String qr = r.optString("upi_qr");
            if (!qr.isEmpty()) {
                ImageView q = Ui.img(c, 220, 220, ImageView.ScaleType.FIT_CENTER);
                q.setBackground(Ui.bg(Ui.WHITE, 8));
                q.setPadding(Ui.dp(8), Ui.dp(8), Ui.dp(8), Ui.dp(8));
                LinearLayout.LayoutParams lp = Ui.lpm(Ui.dp(220), Ui.dp(220), 0, 12, 0, 0);
                lp.gravity = Gravity.CENTER_HORIZONTAL;
                p.addView(q, lp);
                Img.load(q, qr);
            }
            p.addView(Ui.plabel(c, "Transaction/Upi ref no./UTR"));
            final EditText utr = Ui.pinput(c, "Enter transaction/Upi ref no./UTR", InputType.TYPE_CLASS_TEXT);
            p.addView(utr);
            p.addView(Ui.note(c, "Click On Pay Button Then Choose Any Payment Method Like Paytm, Phonepe, Gpay etc And Pay. After Payment Put Transaction No. Or Upi Ref No. Or UTR And Click On Continue & Then Submit Button And Wait Till Verification. " + r.optString("min_note"), Ui.WHITE));
            TextView go = Ui.btn(c, "Continue & Submit", Ui.SKY);
            go.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Api.call("recharge_manual", json("pkg", pkg.optInt("id"), "utr", utr.getText().toString().trim(), "order_id", order), new Api.Cb() {
                        public void done(JSONObject x) {
                            if (x.optBoolean("ok")) { Ui.toast(c, x.optString("message")); a.onBackPressed(); a.push(new TxScreen()); }
                            else Ui.info(c, "Could not submit", x.optString("error"));
                        }
                    });
                }
            });
            p.addView(go);
        }
        Web.rules(c, box, "Deposit Rules", r.optString("deposit_rules"), Ui.CYAN);
    }
}

/** withdraw.php */
class WithdrawScreen extends Screen {
    private int pick = -1;
    String title() { return "Withdrawal money"; }

    void load() {
        loading();
        fetch("wallet", null, new OkCb() { public void run(JSONObject r) { render(r); } });
    }

    private void render(final JSONObject r) {
        Ui.lottie(c, box, "90735-mobile-atm-withdrawal.json", 170, 4, 4);
        final JSONObject w = r.optJSONObject("wallets");
        LinearLayout jb = Web.jbox(c, box);
        Web.jr(c, jb, "Total Coins", Web.coinValue(c, w.optDouble("winning")));
        final boolean deposited = r.optBoolean("deposited");
        if (!deposited) Web.flash(c, box, "Add money (deposit) at least once to unlock withdrawal.", false);
        final JSONArray op = r.optJSONArray("withdraw_options");
        if (op == null || op.length() == 0) { empty("🏧", "Not available", "Withdrawals are not available right now."); }
        else {
            TextView sb = Ui.secbar(c, "👇Select Coin For Withdrawal👇");
            sb.setGravity(Gravity.CENTER);
            box.addView(sb);
            final LinearLayout[] cells = new LinearLayout[op.length()];
            LinearLayout rowL = null;
            for (int i = 0; i < op.length(); i++) {
                if (i % 2 == 0) { rowL = new LinearLayout(c); rowL.setPadding(Ui.dp(8), Ui.dp(4), Ui.dp(8), Ui.dp(4)); box.addView(rowL); }
                final int idx = i;
                JSONObject o = op.optJSONObject(i);
                boolean can = w.optDouble("winning") >= o.optDouble("coins");
                LinearLayout k = Ui.col(c);
                k.setGravity(Gravity.CENTER_HORIZONTAL);
                k.setPadding(Ui.dp(10), Ui.dp(10), Ui.dp(10), Ui.dp(10));
                LinearLayout cm = Ui.coinAmt(c, Ui.money(o.optDouble("coins")), 20, Ui.BLACK, true);
                cm.setGravity(Gravity.CENTER);
                k.addView(cm);
                TextView sm = Ui.tv(c, "≈ " + Ui.money(o.optDouble("payout")) + " Rs", 13, Ui.INDIGO, true);
                k.addView(sm, Ui.lpm(-2, -2, 0, 2, 0, 0));
                k.setBackgroundResource(R.drawable.bg_option);
                if (!can) k.setAlpha(0.4f);
                else k.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        pick = idx;
                        for (int j = 0; j < cells.length; j++) cells[j].setBackgroundResource(j == idx ? R.drawable.bg_option_sel : R.drawable.bg_option);
                    }
                });
                cells[i] = k;
                rowL.addView(k, Ui.weightm(1, 4, 0, 4, 0));
            }
            if (op.length() % 2 == 1 && rowL != null) rowL.addView(new View(c), Ui.weightm(1, 4, 0, 4, 0));
            JSONObject acc = r.optJSONObject("wd_account");
            final boolean hasAcc = acc != null && !acc.optString("upi").isEmpty();
            LinearLayout tb = Ui.row(c);
            tb.setBackground(Ui.bg(Ui.WHITE, 6));
            tb.setPadding(Ui.dp(12), Ui.dp(12), Ui.dp(12), Ui.dp(12));
            TextView t = Ui.tv(c, "", 14, Ui.BLACK, true);
            android.text.SpannableString ss = new android.text.SpannableString("Withdrawal Type : " + (hasAcc ? "UPI (" + acc.optString("upi") + ")" : "Not Select"));
            if (!hasAcc) ss.setSpan(new android.text.style.ForegroundColorSpan(Ui.RED), 18, ss.length(), 0);
            t.setText(ss);
            tb.addView(t, Ui.weight(1));
            TextView mod = Ui.tv(c, "Modify", 14, 0xFF2196F3, true);
            mod.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.push(new AccountScreen()); } });
            tb.addView(mod);
            box.addView(tb, Ui.lpm(-1, -2, 8, 8, 8, 8));
            TextView go = Ui.btn(c, "Continue", Ui.SKY);
            if (!deposited) go.setAlpha(0.5f);
            go.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (!deposited) return;
                    if (!hasAcc) { Ui.toast(c, "Please update your withdrawal account details first."); a.push(new AccountScreen()); return; }
                    if (pick < 0) { Ui.toast(c, "Select coin for withdrawal"); return; }
                    final JSONObject o = op.optJSONObject(pick);
                    Ui.confirm(c, "Withdrawal", "Withdraw " + Ui.money(o.optDouble("coins")) + " coins (≈ " + Ui.money(o.optDouble("payout")) + " Rs) to your UPI?", new Runnable() {
                        public void run() {
                            Api.call("withdraw", json("opt", o.optInt("id")), new Api.Cb() {
                                public void done(JSONObject x) {
                                    if (x.optBoolean("ok")) { Ui.toast(c, x.optString("message")); a.onBackPressed(); a.push(new TxScreen()); }
                                    else Ui.info(c, "Could not withdraw", x.optString("error"));
                                }
                            });
                        }
                    });
                }
            });
            box.addView(Ui.btnbar(c, go));
        }
        Web.rules(c, box, "Withdrawal Rules", r.optString("withdraw_rules"), Ui.CYAN);
    }
}

/** withdraw_account.php */
class AccountScreen extends Screen {
    String title() { return "Withdrawal account"; }

    void load() {
        loading();
        fetch("wallet", null, new OkCb() { public void run(JSONObject r) { render(r.optJSONObject("wd_account")); } });
    }

    private void render(JSONObject acc) {
        LinearLayout p = Web.panel(c, box);
        String[] have = {acc.optString("name"), acc.optString("email"), acc.optString("upi")};
        String[] lab = {"Name", "Email", "Upi Id"}, hint = {"Enter Your Name", "Enter Your Email", "Enter Your Upi Id"};
        final EditText[] in = new EditText[3];
        for (int i = 0; i < 3; i++) {
            TextView l = Ui.plabel(c, lab[i] + " ");
            android.text.SpannableString ss = new android.text.SpannableString(lab[i] + " " + (have[i].isEmpty() ? "(Not available)" : "(" + have[i] + ")"));
            ss.setSpan(new android.text.style.ForegroundColorSpan(0xFF76FF03), lab[i].length() + 1, ss.length(), 0);
            l.setText(ss);
            p.addView(l);
            in[i] = Ui.pinput(c, hint[i], i == 1 ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS : InputType.TYPE_CLASS_TEXT);
            in[i].setText(have[i]);
            p.addView(in[i]);
        }
        TextView go = Ui.btn(c, "Submit or Update", Ui.SKY);
        go.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Api.call("withdraw_account", json("name", in[0].getText().toString().trim(), "email", in[1].getText().toString().trim(), "upi", in[2].getText().toString().trim()), new Api.Cb() {
                    public void done(JSONObject x) {
                        if (x.optBoolean("ok")) { Ui.toast(c, x.optString("message")); a.onBackPressed(); }
                        else Ui.info(c, "Check details", x.optString("error"));
                    }
                });
            }
        });
        box.addView(Ui.btnbar(c, go));
    }
}

/** exchange.php */
class ExchangeScreen extends Screen {
    String title() { return "Money Converter"; }

    void load() {
        loading();
        fetch("wallet", null, new OkCb() { public void run(JSONObject r) { render(r.optJSONObject("wallets")); } });
    }

    private void render(JSONObject w) {
        Ui.lottie(c, box, "Money-Exchange.json", 170, 4, 4);
        TextView sb = Ui.secbar(c, "Winning To Deposit");
        sb.setGravity(Gravity.CENTER);
        box.addView(sb);
        LinearLayout jb = Web.jbox(c, box);
        Web.jr(c, jb, "Current Deposit", Web.coinValue(c, w.optDouble("deposit")));
        Web.jr(c, jb, "Current Winning", Web.coinValue(c, w.optDouble("winning")));
        LinearLayout p = Web.panel(c, box);
        final EditText amt = Ui.pinput(c, "Enter amount", InputType.TYPE_CLASS_NUMBER);
        p.addView(amt);
        p.addView(Ui.note(c, "Note- Once you convert your winning money into deposit money, it will not be converted back into winning money.", 0xFFFFEB3B));
        TextView go = Ui.btn(c, "Convert", Ui.RED);
        go.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Api.call("exchange", json("amount", amt.getText().toString().trim()), new Api.Cb() {
                    public void done(JSONObject x) {
                        if (x.optBoolean("ok")) { Ui.toast(c, x.optString("message")); load(); }
                        else Ui.info(c, "Could not convert", x.optString("error"));
                    }
                });
            }
        });
        box.addView(Ui.btnbar(c, go));
    }
}

/** transactions.php */
class TxScreen extends Screen {
    String title() { return "Transactions History"; }

    void load() {
        loading();
        fetch("transactions", null, new OkCb() {
            public void run(JSONObject r) {
                JSONArray t = r.optJSONArray("transactions");
                if (t == null || t.length() == 0) { empty("🧾", "No Transactions History !", "There Are No Transactions History Found !"); return; }
                for (int i = 0; i < t.length(); i++) {
                    JSONObject x = t.optJSONObject(i);
                    View card = Ui.inflate(c, R.layout.item_card, box);
                    ((TextView) card.findViewById(R.id.c_title)).setText(x.optString("title"));
                    TextView right = (TextView) card.findViewById(R.id.c_right);
                    int dir = x.optInt("direction");
                    right.setText((dir > 0 ? "+ " : (dir < 0 ? "− " : "")) + Ui.money(x.optDouble("amount")));
                    right.setCompoundDrawablePadding(Ui.dp(4));
                    right.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_coin, 0, 0, 0);
                    LinearLayout body = (LinearLayout) card.findViewById(R.id.c_body);
                    Web.ib(c, body, "Transaction Date", x.optString("time"));
                    Web.ib(c, body, "Details", x.optString("heading"));
                    if (!x.optString("detail").isEmpty()) Web.ib(c, body, "Note", x.optString("detail"));
                    String wl = x.optString("wallet");
                    if (!wl.isEmpty()) Web.ib(c, body, "Wallet", Character.toUpperCase(wl.charAt(0)) + wl.substring(1));
                    String kind = x.optString("kind");
                    if (kind.equals("recharge")) { Web.ib(c, body, "Order Id / UTR", x.optString("order_id") + " / " + x.optString("utr")); Web.ib(c, body, "Paid", "₹" + Ui.money(x.optDouble("inr"))); }
                    if (kind.equals("withdrawal")) { Web.ib(c, body, "Order Id", x.optString("order_id")); Web.ib(c, body, "Payout", "₹" + Ui.money(x.optDouble("inr"))); }
                    if (!x.optString("note").isEmpty()) Web.ib(c, body, "Admin note", x.optString("note"));
                    LinearLayout st = Ui.row(c);
                    st.setPadding(0, Ui.dp(3), 0, Ui.dp(3));
                    st.addView(Ui.tv(c, "Status", 13, Ui.SLATE2, false), Ui.weight(1));
                    st.addView(Web.badge(c, x.optString("status")));
                    body.addView(st);
                    box.addView(card);
                }
            }
        });
    }
}
