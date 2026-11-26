package com.m7s.battle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tiny image loader: memory cache + disk cache. Images come from the cPanel uploads folder. */
final class Img {
    private static final LruCache<String, Bitmap> MEM = new LruCache<String, Bitmap>(24 * 1024 * 1024) {
        @Override protected int sizeOf(String k, Bitmap b) { return b.getByteCount(); }
    };
    private static final ExecutorService EX = Executors.newFixedThreadPool(3);
    private static final Handler H = new Handler(Looper.getMainLooper());
    private static File dir;

    private Img() {}

    static void init(Context c) {
        dir = new File(c.getCacheDir(), "img");
        if (!dir.exists()) dir.mkdirs();
    }

    static void load(final ImageView v, final String url) {
        if (url == null || url.isEmpty()) { v.setTag(null); v.setImageDrawable(null); return; }
        v.setTag(url);
        Bitmap b = MEM.get(url);
        if (b != null) { v.setImageBitmap(b); return; }
        v.setImageDrawable(null);
        EX.execute(new Runnable() {
            public void run() {
                final Bitmap bm = fetch(url, 900);
                if (bm == null) return;
                MEM.put(url, bm);
                H.post(new Runnable() { public void run() { if (url.equals(v.getTag())) v.setImageBitmap(bm); } });
            }
        });
    }

    /** Blocking download + decode (also used by the push service for the banner). */
    static Bitmap fetch(String url, int maxPx) {
        try {
            File f = new File(dir, md5(url));
            if (!f.exists() || f.length() == 0) {
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(12000);
                c.setReadTimeout(20000);
                InputStream is = c.getInputStream();
                File tmp = new File(dir, md5(url) + ".tmp");
                FileOutputStream os = new FileOutputStream(tmp);
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
                os.close();
                is.close();
                tmp.renameTo(f);
            }
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getAbsolutePath(), o);
            int s = 1;
            while (o.outWidth / (s * 2) >= maxPx) s *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = s;
            return BitmapFactory.decodeFile(f.getAbsolutePath(), o);
        } catch (Exception e) {
            return null;
        }
    }

    private static String md5(String s) throws Exception {
        byte[] d = MessageDigest.getInstance("MD5").digest(s.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte x : d) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}
