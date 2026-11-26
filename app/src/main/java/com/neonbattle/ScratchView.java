package com.m7s.battle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.view.MotionEvent;
import android.view.View;

/** Scratch layer (silver) that is rubbed off with the finger; reports when ~45% is cleared. */
class ScratchView extends View {
    interface Listener { void onFirstTouch(); void onRevealed(); }

    private Bitmap layer;
    private Canvas lc;
    private final Paint erase = new Paint(Paint.ANTI_ALIAS_FLAG), fill = new Paint(), txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Listener l;
    private boolean touched, done;
    private float lx, ly;
    private int moves;

    ScratchView(Context c) {
        super(c);
        erase.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        erase.setStyle(Paint.Style.STROKE);
        erase.setStrokeCap(Paint.Cap.ROUND);
        erase.setStrokeJoin(Paint.Join.ROUND);
        fill.setColor(0xFFB0BEC5);
        txt.setColor(0xFF607D8B);
        txt.setTextAlign(Paint.Align.CENTER);
        txt.setFakeBoldText(true);
    }

    void setListener(Listener l) { this.l = l; }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (w <= 0 || h <= 0) return;
        layer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        lc = new Canvas(layer);
        lc.drawRect(0, 0, w, h, fill);
        txt.setTextSize(h * 0.12f);
        lc.drawText("SCRATCH HERE", w / 2f, h / 2f + h * 0.04f, txt);
        erase.setStrokeWidth(h * 0.16f);
    }

    @Override protected void onDraw(Canvas cv) {
        if (layer != null && !done) cv.drawBitmap(layer, 0, 0, null);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (done || lc == null) return true;
        getParent().requestDisallowInterceptTouchEvent(true);
        float x = e.getX(), y = e.getY();
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!touched) { touched = true; if (l != null) l.onFirstTouch(); }
                lx = x; ly = y;
                lc.drawPoint(x, y, erase);
                break;
            case MotionEvent.ACTION_MOVE:
                lc.drawLine(lx, ly, x, y, erase);
                lx = x; ly = y;
                if (++moves % 6 == 0) check();
                break;
            case MotionEvent.ACTION_UP:
                check();
                break;
            default: break;
        }
        invalidate();
        return true;
    }

    private void check() {
        int w = layer.getWidth(), h = layer.getHeight(), cleared = 0, total = 0;
        int[] px = new int[w];
        for (int y = 0; y < h; y += 8) {
            layer.getPixels(px, 0, w, 0, y, w, 1);
            for (int x = 0; x < w; x += 8) { total++; if (Color.alpha(px[x]) == 0) cleared++; }
        }
        if (total > 0 && cleared * 100 / total >= 45 && l != null && touched) { done = true; l.onRevealed(); invalidate(); }
    }
}
