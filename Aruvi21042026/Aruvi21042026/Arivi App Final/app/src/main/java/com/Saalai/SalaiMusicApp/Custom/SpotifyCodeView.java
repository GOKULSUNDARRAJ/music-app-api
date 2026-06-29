package com.Saalai.SalaiMusicApp.Custom;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class SpotifyCodeView extends View {

    private Paint barPaint;
    private String data;
    private static final int BAR_COUNT = 23; // Authentic music code count

    public SpotifyCodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SpotifyCodeView(Context context) {
        super(context);
        init();
    }

    private void init() {
        barPaint = new Paint();
        barPaint.setColor(Color.BLACK);
        barPaint.setAntiAlias(true);
        barPaint.setStrokeCap(Paint.Cap.ROUND);
        barPaint.setStyle(Paint.Style.FILL);
    }

    public void setData(String data) {
        this.data = (data == null || data.trim().isEmpty()) ? "aruvi" : data;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        float centerY = height / 2f;

        // --- AUTHENTIC SPOTIFY RATIOS ---
        // Thick bars and elegant spacing optimized for custom scanners
        float barWidth = width / (float) (BAR_COUNT * 1.8f); 
        float spacing = barWidth * 0.8f;
        float totalWidth = (BAR_COUNT * barWidth) + ((BAR_COUNT - 1) * spacing);
        float startX = (width - totalWidth) / 2f;

        // Use a deterministic seed for scannable consistency
        long seed = (data != null) ? (long) data.hashCode() : 0;
        
        for (int i = 0; i < BAR_COUNT; i++) {
            float x = startX + i * (barWidth + spacing);
            
            float heightFactor;
            if (i == 0 || i == BAR_COUNT - 1) {
                // Fixed-height guard bars to help the Aruvi scanner align
                heightFactor = 0.5f;
            } else {
                // 8 distinct height levels (0.2 to 0.9)
                // These distinct steps are what your scanner will "read"
                int level = (int) Math.abs((seed ^ (i * 997)) % 8);
                heightFactor = 0.2f + (level * 0.1f); 
            }
            
            float barHeight = height * heightFactor;

            RectF rect = new RectF(
                    x,
                    centerY - (barHeight / 2f),
                    x + barWidth,
                    centerY + (barHeight / 2f)
            );

            // Draw as premium rounded capsules
            canvas.drawRoundRect(rect, barWidth / 2f, barWidth / 2f, barPaint);
        }
    }
}