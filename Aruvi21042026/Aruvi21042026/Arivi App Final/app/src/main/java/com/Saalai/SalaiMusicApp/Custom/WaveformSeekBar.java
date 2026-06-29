package com.Saalai.SalaiMusicApp.Custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WaveformSeekBar extends View {

    private Paint progressPaint;
    private Paint backgroundPaint;
    private Paint thumbPaint;
    private Path waveformPath;

    private List<Float> amplitudes = new ArrayList<>();
    private int progress = 0;
    private int maxProgress = 100;
    private float thumbPosition = 0;
    private boolean isDragging = false;

    private OnSeekBarChangeListener listener;

    private int waveColor = Color.parseColor("#FF6B6B");
    private int backgroundColor = Color.parseColor("#333333");
    private int thumbColor = Color.WHITE;

    private float density;

    public WaveformSeekBar(Context context) {
        super(context);
        init();
    }

    public WaveformSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaveformSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(waveColor);
        progressPaint.setStyle(Paint.Style.FILL);
        progressPaint.setStrokeWidth(2 * density);

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(backgroundColor);
        backgroundPaint.setStyle(Paint.Style.FILL);

        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setColor(thumbColor);
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setShadowLayer(8, 0, 2, Color.parseColor("#55000000"));

        waveformPath = new Path();

        // Generate random waveform for demo
        generateRandomWaveform(50);
    }

    public void setAmplitudes(List<Float> amplitudes) {
        this.amplitudes = amplitudes;
        invalidate();
    }

    public void addAmplitude(float amplitude) {
        amplitudes.add(amplitude);
        if (amplitudes.size() > 200) { // Limit to 200 points for performance
            amplitudes.remove(0);
        }
        invalidate();
    }

    public void generateRandomWaveform(int count) {
        amplitudes.clear();
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            amplitudes.add(random.nextFloat() * 50);
        }
        invalidate();
    }

    public void setProgress(int progress) {
        this.progress = progress;
        updateThumbPosition();
        invalidate();
    }

    public void setMaxProgress(int maxProgress) {
        this.maxProgress = maxProgress;
        updateThumbPosition();
        invalidate();
    }

    private void updateThumbPosition() {
        if (maxProgress > 0) {
            thumbPosition = (float) progress / maxProgress * getWidth();
        }
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public void setWaveColor(int color) {
        this.waveColor = color;
        progressPaint.setColor(color);
        invalidate();
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
        backgroundPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int centerY = height / 2;

        if (amplitudes.isEmpty()) {
            return;
        }

        // Draw background waveform (gray)
        waveformPath.reset();
        float segmentWidth = (float) width / amplitudes.size();
        float x = 0;

        for (int i = 0; i < amplitudes.size(); i++) {
            float amplitude = amplitudes.get(i);
            float barHeight = Math.min(amplitude, height / 2f);

            if (i == 0) {
                waveformPath.moveTo(x, centerY - barHeight);
            } else {
                waveformPath.lineTo(x, centerY - barHeight);
            }

            x += segmentWidth;
        }

        // Mirror to bottom
        for (int i = amplitudes.size() - 1; i >= 0; i--) {
            float amplitude = amplitudes.get(i);
            float barHeight = Math.min(amplitude, height / 2f);
            x -= segmentWidth;
            waveformPath.lineTo(x, centerY + barHeight);
        }

        waveformPath.close();

        // Draw background waveform
        backgroundPaint.setColor(backgroundColor);
        canvas.drawPath(waveformPath, backgroundPaint);

        // Draw progress waveform (red)
        if (thumbPosition > 0) {
            canvas.save();
            canvas.clipRect(0, 0, thumbPosition, height);
            progressPaint.setColor(waveColor);
            canvas.drawPath(waveformPath, progressPaint);
            canvas.restore();
        }

        // Draw thumb
        float thumbX = thumbPosition;
        if (thumbX < 0) thumbX = 0;
        if (thumbX > width) thumbX = width;

        canvas.drawCircle(thumbX, centerY, 8 * density, thumbPaint);

        // Draw small center line in thumb
        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(waveColor);
        linePaint.setStrokeWidth(2 * density);
        canvas.drawLine(thumbX, centerY - 10 * density, thumbX, centerY + 10 * density, linePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDragging = true;
                updateProgressFromTouch(x);
                if (listener != null) {
                    listener.onStartTrackingTouch(this);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    updateProgressFromTouch(x);
                    if (listener != null) {
                        listener.onProgressChanged(this, progress, true);
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                updateProgressFromTouch(x);
                if (listener != null) {
                    listener.onStopTrackingTouch(this);
                }
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void updateProgressFromTouch(float x) {
        if (x < 0) x = 0;
        if (x > getWidth()) x = getWidth();

        thumbPosition = x;
        progress = (int) (x / getWidth() * maxProgress);

        if (progress < 0) progress = 0;
        if (progress > maxProgress) progress = maxProgress;

        invalidate();
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        this.listener = listener;
    }

    public interface OnSeekBarChangeListener {
        void onProgressChanged(WaveformSeekBar seekBar, int progress, boolean fromUser);
        void onStartTrackingTouch(WaveformSeekBar seekBar);
        void onStopTrackingTouch(WaveformSeekBar seekBar);
    }
}