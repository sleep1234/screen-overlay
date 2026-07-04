package com.example.screenoverlay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String PREFS = "overlay_config";
    private static final String KEY_X = "pos_x";
    private static final String KEY_Y = "pos_y";
    private static final String KEY_R = "radius";
    private static final String KEY_ALPHA = "alpha";

    private SharedPreferences sp;
    private PreviewView preview;
    private SeekBar sbX, sbY, sbR, sbAlpha;
    private TextView tvX, tvY, tvR, tvA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sp = getSharedPreferences(PREFS, MODE_PRIVATE);

        preview = findViewById(R.id.preview);
        sbX = findViewById(R.id.sbX);
        sbY = findViewById(R.id.sbY);
        sbR = findViewById(R.id.sbR);
        sbAlpha = findViewById(R.id.sbAlpha);
        tvX = findViewById(R.id.tvX);
        tvY = findViewById(R.id.tvY);
        tvR = findViewById(R.id.tvR);
        tvA = findViewById(R.id.tvA);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);

        loadAndPreview();

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int val, boolean fromUser) {
                updatePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        };
        sbX.setOnSeekBarChangeListener(listener);
        sbY.setOnSeekBarChangeListener(listener);
        sbR.setOnSeekBarChangeListener(listener);
        sbAlpha.setOnSeekBarChangeListener(listener);

        btnSave.setOnClickListener(v -> {
            save();
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        });

        btnStart.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请先授权悬浮窗权限", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
                return;
            }
            save();
            startService(new Intent(this, OverlayService.class));
            Toast.makeText(this, "覆盖层已启动", Toast.LENGTH_SHORT).show();
        });

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, OverlayService.class));
            Toast.makeText(this, "覆盖层已停止", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadAndPreview() {
        int x = sp.getInt(KEY_X, 540);
        int y = sp.getInt(KEY_Y, 200);
        int r = sp.getInt(KEY_R, 40);
        int a = sp.getInt(KEY_ALPHA, 255);
        sbX.setProgress(x);
        sbY.setProgress(y);
        sbR.setProgress(r);
        sbAlpha.setProgress(a);
        updateLabels(x, y, r, a);
        preview.setCircle(x, y, r, a);
    }

    private void updatePreview() {
        int x = sbX.getProgress();
        int y = sbY.getProgress();
        int r = sbR.getProgress();
        int a = sbAlpha.getProgress();
        updateLabels(x, y, r, a);
        preview.setCircle(x, y, r, a);
    }

    private void updateLabels(int x, int y, int r, int a) {
        tvX.setText("X: " + x);
        tvY.setText("Y: " + y);
        tvR.setText("半径: " + r);
        tvA.setText("不透明度: " + a);
    }

    private void save() {
        sp.edit()
                .putInt(KEY_X, sbX.getProgress())
                .putInt(KEY_Y, sbY.getProgress())
                .putInt(KEY_R, sbR.getProgress())
                .putInt(KEY_ALPHA, sbAlpha.getProgress())
                .apply();
    }

    static class PreviewView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint bgPaint = new Paint();
        private final Paint crossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int cx, cy, cr, alpha;

        public PreviewView(Context ctx, AttributeSet attrs) {
            super(ctx, attrs);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL);
            bgPaint.setColor(0xFFE0E0E0);
            crossPaint.setColor(Color.RED);
            crossPaint.setStrokeWidth(2);
        }

        public void setCircle(int x, int y, int r, int a) {
            cx = x; cy = y; cr = r; alpha = a;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            c.drawRect(0, 0, getWidth(), getHeight(), bgPaint);
            // 模拟1080x2400屏幕
            float sx = getWidth() / 1080f;
            float sy = getHeight() / 2400f;
            float s = Math.min(sx, sy);
            float ox = (getWidth() - 1080 * s) / 2;
            float oy = (getHeight() - 2400 * s) / 2;
            // 屏幕边框
            Paint border = new Paint();
            border.setColor(Color.DKGRAY);
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(1);
            c.drawRect(ox, oy, ox + 1080 * s, oy + 2400 * s, border);
            // 黑圈
            paint.setAlpha(alpha);
            c.drawCircle(ox + cx * s, oy + cy * s, cr * s, paint);
            // 中心十字
            float px = ox + cx * s;
            float py = oy + cy * s;
            c.drawLine(px - 10, py, px + 10, py, crossPaint);
            c.drawLine(px, py - 10, px, py + 10, crossPaint);
        }
    }
}
