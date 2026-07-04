package com.example.screenoverlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class PreviewView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint();
    private final Paint crossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int cx, cy, ew, eh, alpha;

    public PreviewView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(0xFFE0E0E0);
        crossPaint.setColor(Color.RED);
        crossPaint.setStrokeWidth(2);
    }

    public void setEllipse(int x, int y, int w, int h, int a) {
        cx = x;
        cy = y;
        ew = w;
        eh = h;
        alpha = a;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        c.drawRect(0, 0, getWidth(), getHeight(), bgPaint);
        float sx = getWidth() / 1080f;
        float sy = getHeight() / 2400f;
        float s = Math.min(sx, sy);
        float ox = (getWidth() - 1080 * s) / 2;
        float oy = (getHeight() - 2400 * s) / 2;
        Paint border = new Paint();
        border.setColor(Color.DKGRAY);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(1);
        c.drawRect(ox, oy, ox + 1080 * s, oy + 2400 * s, border);
        paint.setAlpha(alpha);
        float left = ox + cx * s - ew * s / 2f;
        float top = oy + cy * s - eh * s / 2f;
        float right = ox + cx * s + ew * s / 2f;
        float bottom = oy + cy * s + eh * s / 2f;
        rect.set(left, top, right, bottom);
        c.drawOval(rect, paint);
        float px = ox + cx * s;
        float py = oy + cy * s;
        c.drawLine(px - 10, py, px + 10, py, crossPaint);
        c.drawLine(px, py - 10, px, py + 10, crossPaint);
    }
}
