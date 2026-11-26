package com.m7s.battle;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

/** earn.php : gift + 4 cards (Refer & earn, Lucky Draw, Watch & earn, Scratch Card). */
class EarnScreen extends Screen {
    String title() { return "Earn"; }
    @Override boolean isTab() { return true; }

    void load() {
        box.removeAllViews();
        TextView gift = Ui.tv(c, "🎁", 80, Ui.TXT, false);
        gift.setGravity(Gravity.CENTER);
        box.addView(gift, Ui.lpm(-1, -2, 0, 20, 0, 6));
        Object[][] cards = {
                {"🎁", "Refer & earn", "Refer this app to your friends and get money", 0},
                {"🎟️", "Lucky Draw", "Buy lottery ticket and try your luck!", 1},
                {"📺", "Watch & earn", "Watch ad video and win huge!", 2},
                {"🪙", "Scratch Card", "Scratch the Card and earn huge!", 3}};
        for (final Object[] x : cards) {
            View v = Ui.inflate(c, R.layout.item_ecard, box);
            ((TextView) v.findViewById(R.id.e_emoji)).setText((String) x[0]);
            ((TextView) v.findViewById(R.id.e_title)).setText((String) x[1]);
            ((TextView) v.findViewById(R.id.e_desc)).setText((String) x[2]);
            v.setOnClickListener(new View.OnClickListener() {
                public void onClick(View vv) {
                    int k = (Integer) x[3];
                    a.push(k == 0 ? new ReferScreen() : (k == 1 ? new LuckyScreen() : (k == 2 ? new WatchScreen() : new ScratchScreen())));
                }
            });
            box.addView(v);
        }
    }
    @Override void onShow() {}
}
