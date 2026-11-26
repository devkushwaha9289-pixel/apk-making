package com.m7s.battle;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

/**
 * Firebase is started from the values the website serves (Admin > Push / Firebase > 2b),
 * so no google-services.json file is needed inside the APK.
 */
final class Push {
    private Push() {}

    static boolean ensureInit(Context c) {
        try {
            if (!FirebaseApp.getApps(c).isEmpty()) return true;
            JSONObject fb = Cfg.conf(c).optJSONObject("firebase");
            if (fb == null) return false;
            FirebaseOptions o = new FirebaseOptions.Builder()
                    .setApplicationId(fb.getString("appId"))
                    .setApiKey(fb.getString("apiKey"))
                    .setProjectId(fb.getString("projectId"))
                    .setGcmSenderId(fb.getString("senderId"))
                    .build();
            FirebaseApp.initializeApp(c.getApplicationContext(), o);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static void createChannel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("battle_push", "Match & wallet alerts", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    /** Gets this phone's FCM token and gives it to the server (only when logged in). */
    static void register(final Context c) {
        if (Cfg.token(c).isEmpty() || !ensureInit(c)) return;
        try {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
                @Override public void onComplete(Task<String> t) {
                    if (t.isSuccessful() && t.getResult() != null) send(c, t.getResult());
                }
            });
        } catch (Exception ignored) {}
    }

    static void send(Context c, String token) {
        Cfg.put(c, "fcm", token);
        if (Cfg.token(c).isEmpty()) return;
        try {
            JSONObject b = new JSONObject();
            b.put("token", token);
            Api.call("push_token", b, null);
        } catch (Exception ignored) {}
    }
}
