package com.example.screenoverlay;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MainActivity extends Activity {
    private static final String PREFS = "overlay_config";
    private static final String KEY_X = "pos_x";
    private static final String KEY_Y = "pos_y";
    private static final String KEY_W = "width";
    private static final String KEY_H = "height";
    private static final String KEY_ALPHA = "alpha";
    public static final String ACTION_UPDATE = "com.example.screenoverlay.UPDATE";

    private SharedPreferences sp;
    private SeekBar sbX, sbY, sbW, sbH, sbAlpha;
    private TextView tvX, tvY, tvW, tvH, tvA;
    private boolean overlayRunning;

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 服务确认更新成功
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sp = getSharedPreferences(PREFS, MODE_PRIVATE);

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
        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);

        loadAndPreview();

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int val, boolean fromUser) {
                updateLabels();
                if (overlayRunning) {
                    sendUpdate();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        };
        sbX.setOnSeekBarChangeListener(listener);
        sbY.setOnSeekBarChangeListener(listener);
        sbW.setOnSeekBarChangeListener(listener);
        sbH.setOnSeekBarChangeListener(listener);
        sbAlpha.setOnSeekBarChangeListener(listener);

        btnStart.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请先授权悬浮窗权限", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
                return;
            }
            save();
            startService(new Intent(this, OverlayService.class));
            overlayRunning = true;
            Toast.makeText(this, "覆盖层已启动", Toast.LENGTH_SHORT).show();
        });

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, OverlayService.class));
            overlayRunning = false;
            Toast.makeText(this, "覆盖层已停止", Toast.LENGTH_SHORT).show();
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver,
                new IntentFilter(ACTION_UPDATE));

        // 自动启动覆盖层
        if (Settings.canDrawOverlays(this)) {
            save();
            startService(new Intent(this, OverlayService.class));
            overlayRunning = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (overlayRunning) {
            sendUpdate();
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver);
        super.onDestroy();
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
        updateLabels();
    }

    private void updateLabels() {
        tvX.setText("X: " + sbX.getProgress());
        tvY.setText("Y: " + sbY.getProgress());
        tvW.setText("横向宽度: " + sbW.getProgress());
        tvH.setText("纵向宽度: " + sbH.getProgress());
        tvA.setText("不透明度: " + sbAlpha.getProgress());
    }

    private void sendUpdate() {
        save();
        Intent intent = new Intent(OverlayService.ACTION_UPDATE_OVERLAY);
        intent.putExtra("pos_x", sbX.getProgress());
        intent.putExtra("pos_y", sbY.getProgress());
        intent.putExtra("width", sbW.getProgress());
        intent.putExtra("height", sbH.getProgress());
        intent.putExtra("alpha", sbAlpha.getProgress());
        sendService(intent);
    }

    private void sendService(Intent intent) {
        intent.setComponent(new android.content.ComponentName(this, OverlayService.class));
        startService(intent);
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
