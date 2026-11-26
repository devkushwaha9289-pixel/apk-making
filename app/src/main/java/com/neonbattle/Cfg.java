package com.m7s.battle;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

/** Small settings store (login token, cached server config). */
final class Cfg {
    static final String BASE = BuildConfig.BASE_URL;
    static long skew = 0; // server time - phone time (ms), so countdowns are right even if the phone clock is off

    private Cfg() {}

    static SharedPreferences p(Context c) { return c.getSharedPreferences("battle", Context.MODE_PRIVATE); }
    static String get(Context c, String k) { return p(c).getString(k, ""); }
    static void put(Context c, String k, String v) { p(c).edit().putString(k, v).apply(); }
    static String token(Context c) { return get(c, "token"); }
    static void setToken(Context c, String t) { put(c, "token", t); }
    static long now() { return System.currentTimeMillis() + skew; }

    static JSONObject conf(Context c) {
        try { return new JSONObject(get(c, "conf")); } catch (Exception e) { return new JSONObject(); }
    }
}
