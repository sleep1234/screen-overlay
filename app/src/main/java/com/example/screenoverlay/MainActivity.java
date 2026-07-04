package com.example.screenoverlay;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
    private static final String KEY_W = "width";
    private static final String KEY_H = "height";
    private static final String KEY_ALPHA = "alpha";

    private SharedPreferences sp;
    private PreviewView preview;
    private SeekBar sbX, sbY, sbW, sbH, sbAlpha;
    private TextView tvX, tvY, tvW, tvH, tvA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sp = getSharedPreferences(PREFS, MODE_PRIVATE);

        preview = findViewById(R.id.preview);
        sbX = findViewById(R.id.sbX);
        sbY = findViewById(R.id.sbY);
        sbW = findViewById(R.id.sbW);
        sbH = findViewById(R.id.sbH);
        sbAlpha = findViewById(R.id.sbAlpha);
        tvX = findViewById(R.id.tvX);
        tvY = findViewById(R.id.tvY);
        tvW = findViewById(R.id.tvW);
        tvH = findViewById(R.id.tvH);
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
        sbW.setOnSeekBarChangeListener(listener);
        sbH.setOnSeekBarChangeListener(listener);
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
        int w = sp.getInt(KEY_W, 80);
        int h = sp.getInt(KEY_H, 80);
        int a = sp.getInt(KEY_ALPHA, 255);
        sbX.setProgress(x);
        sbY.setProgress(y);
        sbW.setProgress(w);
        sbH.setProgress(h);
        sbAlpha.setProgress(a);
        updateLabels(x, y, w, h, a);
        preview.setEllipse(x, y, w, h, a);
    }

    private void updatePreview() {
        int x = sbX.getProgress();
        int y = sbY.getProgress();
        int w = sbW.getProgress();
        int h = sbH.getProgress();
        int a = sbAlpha.getProgress();
        updateLabels(x, y, w, h, a);
        preview.setEllipse(x, y, w, h, a);
    }

    private void updateLabels(int x, int y, int w, int h, int a) {
        tvX.setText("X: " + x);
        tvY.setText("Y: " + y);
        tvW.setText("横向宽度: " + w);
        tvH.setText("纵向宽度: " + h);
        tvA.setText("不透明度: " + a);
    }

    private void save() {
        sp.edit()
                .putInt(KEY_X, sbX.getProgress())
                .putInt(KEY_Y, sbY.getProgress())
                .putInt(KEY_W, sbW.getProgress())
                .putInt(KEY_H, sbH.getProgress())
                .putInt(KEY_ALPHA, sbAlpha.getProgress())
                .apply();
    }
}
