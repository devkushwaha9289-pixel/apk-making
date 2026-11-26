package com.m7s.battle;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

/** .light pages: Login / Register / Forgot (+ e-mail OTP when the admin switched it on). Layout: res/layout/activity_auth.xml. */
public class AuthActivity extends Activity {
    private static final int LOGIN = 0, REGISTER = 1, FORGOT = 2, OTP = 3;
    private LinearLayout form;
    private TextView title, msg;
    private int mode = LOGIN;
    private JSONObject conf, pending;
    private String ticket = "";

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.init(this);
        conf = Cfg.conf(this);
        setContentView(R.layout.activity_auth);
        title = (TextView) findViewById(R.id.au_title);
        msg = (TextView) findViewById(R.id.au_msg);
        form = (LinearLayout) findViewById(R.id.au_form);
        String bg = conf.optString("login_bg");
        if (!bg.isEmpty()) Img.load((ImageView) findViewById(R.id.au_bg), bg);
        draw();
    }

    private void flash(String text, boolean ok) {
        msg.setVisibility(text == null || text.isEmpty() ? View.GONE : View.VISIBLE);
        msg.setText(text);
        msg.setBackgroundColor(ok ? 0xFFC8E6C9 : 0xFFFFCDD2);
        msg.setTextColor(ok ? 0xFF1B5E20 : 0xFFB71C1C);
    }

    private EditText field(String hint, int type, String prefix, boolean pass) {
        View row = LayoutInflater.from(this).inflate(R.layout.field_row, form, false);
        final EditText e = (EditText) row.findViewById(R.id.f_input);
        e.setHint(hint);
        e.setInputType(type);
        e.setTypeface(Ui.FB);
        if (prefix != null) { TextView p = (TextView) row.findViewById(R.id.f_pre); p.setText(prefix); p.setVisibility(View.VISIBLE); p.setTypeface(Ui.FB); }
        if (pass) {
            e.setTransformationMethod(PasswordTransformationMethod.getInstance());
            final TextView s = (TextView) row.findViewById(R.id.f_show);
            s.setVisibility(View.VISIBLE);
            s.setTypeface(Ui.FB);
            s.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    boolean hidden = e.getTransformationMethod() != null;
                    e.setTransformationMethod(hidden ? null : PasswordTransformationMethod.getInstance());
                    s.setText(hidden ? "Hide" : "Show");
                    e.setSelection(e.getText().length());
                }
            });
        }
        form.addView(row);
        return e;
    }

    private TextView button(String text, View.OnClickListener l) {
        TextView b = (TextView) LayoutInflater.from(this).inflate(R.layout.btn_light, form, false);
        b.setText(text);
        b.setTypeface(Ui.FB, android.graphics.Typeface.BOLD);
        b.setOnClickListener(l);
        form.addView(b);
        return b;
    }

    /** .lrow : "text" + amber bold link */
    private void lrow(String plain, String link, final int to) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER);
        r.setPadding(Ui.dp(8), Ui.dp(8), Ui.dp(8), Ui.dp(8));
        if (!plain.isEmpty()) r.addView(Ui.tv(this, plain + " ", 16, Color.BLACK, false));
        TextView l = Ui.tv(this, link, 16, Ui.AMBER, true);
        l.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { mode = to; flash(null, true); draw(); } });
        r.addView(l);
        form.addView(r);
    }

    private void draw() {
        form.removeAllViews();
        title.setTextSize(30);
        final int T = InputType.TYPE_CLASS_TEXT;
        final int P = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
        if (mode == LOGIN) {
            title.setText("Welcome to,\nLogin");
            final EditText id = field("Mobile number or email", T, null, false);
            final EditText pw = field("Password", P, null, true);
            LinearLayout fg = new LinearLayout(this);
            fg.setGravity(Gravity.END);
            fg.setPadding(Ui.dp(20), 0, Ui.dp(20), 0);
            TextView fp = Ui.tv(this, "Forgot password ?", 14, Color.BLACK, false);
            fp.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { mode = FORGOT; draw(); } });
            fg.addView(fp);
            form.addView(fg);
            button("LOGIN", new View.OnClickListener() {
                public void onClick(View v) { doLogin(id.getText().toString().trim(), pw.getText().toString()); }
            });
            lrow("Don't have an account?", "REGISTER", REGISTER);
        } else if (mode == REGISTER) {
            title.setText("Create your account"); title.setTextSize(26);
            final EditText un = field("Username", T, null, false);
            final EditText fn = field("Full name", T, null, false);
            final EditText em = field("Email address", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, null, false);
            final EditText ph = field("Mobile Number", InputType.TYPE_CLASS_PHONE, "+91", false);
            final EditText pw = field("Password (min 6)", P, null, true);
            final EditText rf = field("Refferal Code (optional)", T, null, false);
            button(conf.optBoolean("reg_otp") ? "Send verification code" : "Verify", new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        pending = new JSONObject();
                        pending.put("username", un.getText().toString().trim());
                        pending.put("full_name", fn.getText().toString().trim());
                        pending.put("email", em.getText().toString().trim());
                        pending.put("phone", ph.getText().toString().trim());
                        pending.put("password", pw.getText().toString());
                        pending.put("ref", rf.getText().toString().trim());
                        pending.put("device", Build.MODEL);
                    } catch (Exception ignored) {}
                    if (conf.optBoolean("reg_otp")) sendOtp(); else finishRegister();
                }
            });
            lrow("Already registered?", "LOGIN", LOGIN);
        } else if (mode == FORGOT) {
            title.setText("Forgot\nPassword ?");
            final EditText em = field("Email address", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, null, false);
            button("SEND RESET LINK", new View.OnClickListener() {
                public void onClick(View v) {
                    Api.call("forgot", Screen.json("email", em.getText().toString().trim()), new Api.Cb() {
                        public void done(JSONObject r) { flash(r.optString("message", r.optString("error")), r.optBoolean("ok")); }
                    });
                }
            });
            lrow("", "Back to login", LOGIN);
        } else {
            title.setText("Verify your email"); title.setTextSize(26);
            final EditText otp = field("6-digit code", InputType.TYPE_CLASS_NUMBER, null, false);
            button("Verify & Create account", new View.OnClickListener() {
                public void onClick(View v) {
                    try { pending.put("otp", otp.getText().toString().trim()); pending.put("ticket", ticket); } catch (Exception ignored) {}
                    finishRegister();
                }
            });
            lrow("", "Back", REGISTER);
        }
        Ui.font(form);
        title.setTypeface(Ui.FB, android.graphics.Typeface.BOLD);
    }

    private void doLogin(String id, String pw) {
        Api.call("login", Screen.json("id", id, "password", pw, "device", Build.MODEL), new Api.Cb() { public void done(JSONObject r) { afterAuth(r); } });
    }

    private void sendOtp() {
        Api.call("register_otp", pending, new Api.Cb() {
            public void done(JSONObject r) {
                if (!r.optBoolean("ok")) { flash(r.optString("error"), false); return; }
                ticket = r.optString("ticket");
                mode = OTP;
                draw();
                flash(r.optString("message"), true);
            }
        });
    }

    private void finishRegister() {
        Api.call("register", pending, new Api.Cb() { public void done(JSONObject r) { afterAuth(r); } });
    }

    private void afterAuth(JSONObject r) {
        if (!r.optBoolean("ok")) { flash(r.optString("error"), false); return; }
        Cfg.setToken(this, r.optString("token"));
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
