package com.m7s.battle;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Receives pushes sent from the admin panel (manual and automatic).
 * With the app in the background Android/Firebase shows the notification itself (with the banner);
 * with the app open this service shows it, so users never miss one.
 */
public class PushService extends FirebaseMessagingService {

    @Override public void onNewToken(String token) {
        Push.send(this, token);
    }

    @Override public void onMessageReceived(RemoteMessage m) {
        Map<String, String> d = m.getData();
        String title = d.get("title"), body = d.get("body"), img = d.get("image"), link = d.get("link");
        RemoteMessage.Notification n = m.getNotification();
        if (n != null) {
            if (title == null) title = n.getTitle();
            if (body == null) body = n.getBody();
            if (img == null && n.getImageUrl() != null) img = n.getImageUrl().toString();
        }
        if (title == null && body == null) return;
        show(this, title == null ? "" : title, body == null ? "" : body, img, link);
    }

    static void show(Context c, String title, String body, String img, String link) {
        Push.createChannel(c);
        Intent i = new Intent(c, SplashActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (link != null) i.putExtra("link", link);
        int id = (int) (System.currentTimeMillis() / 1000L);
        PendingIntent pi = PendingIntent.getActivity(c, id, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, "battle_push")
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pi);
        if (img != null && !img.isEmpty()) {
            Bitmap bm = Img.fetch(img, 1000);
            if (bm != null) b.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bm).setSummaryText(body));
        }
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            try { nm.notify(id, b.build()); } catch (SecurityException ignored) {}
        }
    }
}
