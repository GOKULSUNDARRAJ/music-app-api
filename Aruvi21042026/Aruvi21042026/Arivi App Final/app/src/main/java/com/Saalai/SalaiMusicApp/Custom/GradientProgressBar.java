package com.Saalai.SalaiMusicApp.Custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import com.Saalai.SalaiMusicApp.R;

public class GradientProgressBar extends ProgressBar {

    private int[] gradientColors;
    private Paint progressPaint;
    private RectF progressRect;

    public GradientProgressBar(Context context) {
        super(context);
        init(null);
    }

    public GradientProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public GradientProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        // Default gradient colors
        gradientColors = new int[]{
                0xFF33B5E5,
                0xFF3387CB,
                0xFF3300CC
        };

        // Get custom attributes if provided
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.GradientProgressBar);
            try {
                int colorsResId = a.getResourceId(R.styleable.GradientProgressBar_gradientColors, 0);
                if (colorsResId != 0) {
                    gradientColors = getResources().getIntArray(colorsResId);
                }
            } finally {
                a.recycle();
            }
        }

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressRect = new RectF();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        progressRect.set(0, 0, getWidth(), getHeight());

        // Create gradient
        LinearGradient gradient = new LinearGradient(
                0, 0, getWidth(), 0,
                gradientColors,
                null,
                Shader.TileMode.CLAMP
        );

        progressPaint.setShader(gradient);

        // Calculate progress width
        float progressWidth = (float) getProgress() / getMax() * getWidth();
        progressRect.right = progressWidth;

        // Draw progress
        canvas.drawRoundRect(progressRect, 10, 10, progressPaint);
    }

    public void setGradientColors(int[] colors) {
        this.gradientColors = colors;
        invalidate();
    }
}