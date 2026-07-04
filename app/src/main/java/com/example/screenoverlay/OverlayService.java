package com.example.screenoverlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Paint;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class OverlayService extends Service {
    private static final String CH_ID = "overlay_channel";
    private static final String PREFS = "overlay_config";

    private WindowManager wm;
    private View overlay;
    private WindowManager.LayoutParams params;
    private SharedPreferences sp;

    private float lastTouchX, lastTouchY;
    private int lastParamX, lastParamY;
    private boolean dragging;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        sp = getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showOverlay();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        removeOverlay();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void showOverlay() {
        removeOverlay();

        int cx = sp.getInt("pos_x", 540);
        int cy = sp.getInt("pos_y", 200);
        int radius = sp.getInt("radius", 40);
        int alpha = sp.getInt("alpha", 255);

        overlay = new CircleView(this, radius, alpha);

        overlay.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = event.getRawX();
                    lastTouchY = event.getRawY();
                    lastParamX = params.x;
                    lastParamY = params.y;
                    dragging = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - lastTouchX;
                    float dy = event.getRawY() - lastTouchY;
                    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) dragging = true;
                    if (dragging) {
                        params.x = lastParamX + (int) dx;
                        params.y = lastParamY + (int) dy;
                        wm.updateViewLayout(overlay, params);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (dragging) {
                        sp.edit()
                                .putInt("pos_x", params.x + radius)
                                .putInt("pos_y", params.y + radius)
                                .apply();
                    }
                    return true;
            }
            return false;
        });

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                radius * 2, radius * 2,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = cx - radius;
        params.y = cy - radius;

        wm.addView(overlay, params);
        startForeground(1, buildNotification());
    }

    private void removeOverlay() {
        if (overlay != null && wm != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
            overlay = null;
        }
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CH_ID, "屏幕覆盖", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
        Intent main = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, main, PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CH_ID)
                .setContentTitle("屏幕覆盖运行中")
                .setContentText("拖拽黑圈调整位置，打开App调整大小")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    static class CircleView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int size;
        CircleView(Context ctx, int radius, int alpha) {
            super(ctx);
            size = radius * 2;
            paint.setColor(Color.BLACK);
            paint.setAlpha(alpha);
            paint.setStyle(Paint.Style.FILL);
            setWillNotDraw(false);
        }
        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            setMeasuredDimension(size, size);
        }
        @Override
        protected void onDraw(Canvas c) {
            float r = size / 2f;
            c.drawCircle(r, r, r, paint);
        }
    }
}
