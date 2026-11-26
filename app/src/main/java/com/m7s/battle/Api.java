package com.m7s.battle;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Talks to  <site>/api/?a=action  (PHP + MySQL on cPanel). Every reply is a JSONObject that always has "ok". */
final class Api {
    interface Cb { void done(JSONObject r); }

    private static final ExecutorService EX = Executors.newFixedThreadPool(4);
    private static final Handler H = new Handler(Looper.getMainLooper());
    static Runnable onAuthLost;
    private static Context ctx;

    private Api() {}

    static void init(Context c) { ctx = c.getApplicationContext(); }

    static void call(final String action, final JSONObject body, final Cb cb) {
        EX.execute(new Runnable() {
            public void run() {
                final JSONObject r = post(action, body == null ? new JSONObject() : body);
                H.post(new Runnable() { public void run() { deliver(r, cb); } });
            }
        });
    }

    static void upload(final String action, final Map<String, String> fields, final String fileField,
                       final byte[] data, final String fileName, final Cb cb) {
        EX.execute(new Runnable() {
            public void run() {
                final JSONObject r = multipart(action, fields, fileField, data, fileName);
                H.post(new Runnable() { public void run() { deliver(r, cb); } });
            }
        });
    }

    private static void deliver(JSONObject r, Cb cb) {
        if (!r.optBoolean("ok") && !r.optBoolean("auth", true)) {
            Cfg.setToken(ctx, "");
            if (onAuthLost != null) onAuthLost.run();
            return;
        }
        if (!r.optBoolean("ok") && (r.optBoolean("blocked") || r.optBoolean("maintenance"))) {
            StateActivity.show(ctx, r.optBoolean("blocked") ? "blocked" : "maintenance", r.optString("error"));
            return;
        }
        if (cb != null) cb.done(r);
    }

    private static JSONObject err(String m) {
        JSONObject o = new JSONObject();
        try { o.put("ok", false); o.put("error", m); } catch (Exception ignored) {}
        return o;
    }

    private static HttpURLConnection open(String action) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(Cfg.BASE + "api/?a=" + action).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        String t = Cfg.token(ctx);
        if (!t.isEmpty()) {
            c.setRequestProperty("Authorization", "Bearer " + t);
            c.setRequestProperty("X-Auth-Token", t);
        }
        return c;
    }

    private static JSONObject read(HttpURLConnection c) throws Exception {
        int code = c.getResponseCode();
        InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        if (is != null) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
            is.close();
        }
        String s = bo.toString("UTF-8");
        try {
            JSONObject r = new JSONObject(s);
            if (r.has("server_time")) Cfg.skew = r.optLong("server_time") * 1000L - System.currentTimeMillis();
            return r;
        } catch (Exception e) {
            return err("Server problem (" + code + "). Please try again.");
        }
    }

    /** Blocking call (used from background threads such as the push service). */
    static JSONObject post(String action, JSONObject body) {
        try {
            HttpURLConnection c = open(action);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            OutputStream os = c.getOutputStream();
            os.write(body.toString().getBytes("UTF-8"));
            os.close();
            return read(c);
        } catch (Exception e) {
            return err("No internet connection. Please check and try again.");
        }
    }

    private static JSONObject multipart(String action, Map<String, String> fields, String fileField, byte[] data, String fileName) {
        try {
            String b = "----bt" + System.currentTimeMillis();
            HttpURLConnection c = open(action);
            c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + b);
            OutputStream os = c.getOutputStream();
            if (fields != null) {
                for (Map.Entry<String, String> e : fields.entrySet()) {
                    os.write(("--" + b + "\r\nContent-Disposition: form-data; name=\"" + e.getKey() + "\"\r\n\r\n").getBytes("UTF-8"));
                    os.write(e.getValue().getBytes("UTF-8"));
                    os.write("\r\n".getBytes("UTF-8"));
                }
            }
            if (data != null) {
                os.write(("--" + b + "\r\nContent-Disposition: form-data; name=\"" + fileField + "\"; filename=\"" + fileName
                        + "\"\r\nContent-Type: image/jpeg\r\n\r\n").getBytes("UTF-8"));
                os.write(data);
                os.write("\r\n".getBytes("UTF-8"));
            }
            os.write(("--" + b + "--\r\n").getBytes("UTF-8"));
            os.close();
            return read(c);
        } catch (Exception e) {
            return err("No internet connection. Please check and try again.");
        }
    }
}
