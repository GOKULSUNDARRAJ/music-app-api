package com.Saalai.SalaiMusicApp.Custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WaveformView extends View {

    private Paint paint;
    private List<Float> amplitudes;
    private float maxAmplitude = 50f;
    private Random random;

    public WaveformView(Context context) {
        super(context);
        init();
    }

    public WaveformView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.parseColor("#FFFFFFFF"));
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        amplitudes = new ArrayList<>();
        random = new Random();

        // Initialize with some values
        for (int i = 0; i < 50; i++) {
            amplitudes.add(random.nextFloat() * maxAmplitude);
        }
    }

    public void addAmplitude(float amplitude) {
        amplitudes.add(amplitude);
        if (amplitudes.size() > 100) {
            amplitudes.remove(0);
        }
        invalidate();
    }

    public void clear() {
        amplitudes.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (amplitudes.isEmpty()) return;

        float width = getWidth();
        float height = getHeight();
        float centerY = height / 2;
        float barWidth = width / amplitudes.size();

        for (int i = 0; i < amplitudes.size(); i++) {
            float amplitude = amplitudes.get(i);
            float left = i * barWidth;
            float right = left + barWidth - 2; // -2 for spacing
            float top = centerY - amplitude;
            float bottom = centerY + amplitude;

            canvas.drawRect(left, top, right, bottom, paint);
        }
    }
}