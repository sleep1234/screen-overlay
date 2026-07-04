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
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class OverlayService extends Service {
    private static final String CH_ID = "overlay_channel";
    private static final String PREFS = "overlay_config";
    public static final String ACTION_UPDATE_OVERLAY = "com.example.screenoverlay.UPDATE_OVERLAY";

    private WindowManager wm;
    private EllipseView overlay;
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
        if (intent != null && ACTION_UPDATE_OVERLAY.equals(intent.getAction())) {
            updateOverlay(
                    intent.getIntExtra("pos_x", 540),
                    intent.getIntExtra("pos_y", 200),
                    intent.getIntExtra("width", 80),
                    intent.getIntExtra("height", 80),
                    intent.getIntExtra("alpha", 255));
        } else {
            showOverlay();
        }
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
        int width = sp.getInt("width", 80);
        int height = sp.getInt("height", 80);
        int alpha = sp.getInt("alpha", 255);

        createOverlay(width, height, alpha);
        positionOverlay(cx, cy, width, height);
        wm.addView(overlay, params);
        startForeground(1, buildNotification());
    }

    private void updateOverlay(int cx, int cy, int width, int height, int alpha) {
        if (overlay == null) {
            createOverlay(width, height, alpha);
            positionOverlay(cx, cy, width, height);
            wm.addView(overlay, params);
            startForeground(1, buildNotification());
        } else {
            // 更新大小
            if (overlay.getWidth() != width || overlay.getHeight() != height) {
                wm.removeView(overlay);
                createOverlay(width, height, alpha);
                positionOverlay(cx, cy, width, height);
                wm.addView(overlay, params);
            } else {
                // 更新透明度和位置
                overlay.setAlpha(alpha);
                params.x = cx - width / 2;
                params.y = cy - height / 2;
                wm.updateViewLayout(overlay, params);
            }
        }
    }

    private void createOverlay(int width, int height, int alpha) {
        int finalWidth = width;
        int finalHeight = height;

        overlay = new EllipseView(this, width, height, alpha);

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
                                .putInt("pos_x", params.x + finalWidth / 2)
                                .putInt("pos_y", params.y + finalHeight / 2)
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
                width, height,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
    }

    private void positionOverlay(int cx, int cy, int width, int height) {
        params.x = cx - width / 2;
        params.y = cy - height / 2;
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
                .setContentText("打开App调整大小和位置")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    static class EllipseView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final int w, h;
        EllipseView(Context ctx, int width, int height, int alpha) {
            super(ctx);
            w = width;
            h = height;
            paint.setColor(Color.BLACK);
            paint.setAlpha(alpha);
            paint.setStyle(Paint.Style.FILL);
            setWillNotDraw(false);
        }
        void setAlpha(int alpha) {
            paint.setAlpha(alpha);
            invalidate();
        }
        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            setMeasuredDimension(w, h);
        }
        @Override
        protected void onDraw(Canvas c) {
            rect.set(0, 0, w, h);
            c.drawOval(rect, paint);
        }
    }
}
