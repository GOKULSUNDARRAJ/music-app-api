package com.Saalai.SalaiMusicApp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class GradientBorderView extends View {
    private Paint borderPaint;
    private RectF rectF;
    private float cornerRadius;
    private int[] colors = {0xFF667EEA, 0xFF764BA2, 0xFF667EEA};

    public GradientBorderView(Context context) {
        super(context);
        init();
    }

    public GradientBorderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GradientBorderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpToPx(1.5f));
        cornerRadius = dpToPx(12);
        rectF = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Update gradient when size changes
        if (w > 0 && h > 0) {
            LinearGradient gradient = new LinearGradient(
                    0, 0, w, 0,
                    colors, null, Shader.TileMode.CLAMP
            );
            borderPaint.setShader(gradient);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (getWidth() == 0 || getHeight() == 0) return;

        float strokeWidth = borderPaint.getStrokeWidth();
        rectF.set(
                strokeWidth / 2,
                strokeWidth / 2,
                getWidth() - strokeWidth / 2,
                getHeight() - strokeWidth / 2
        );

        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    // Optional: Set custom gradient colors
    public void setGradientColors(int[] colors) {
        this.colors = colors;
        if (getWidth() > 0 && getHeight() > 0) {
            LinearGradient gradient = new LinearGradient(
                    0, 0, getWidth(), 0,
                    colors, null, Shader.TileMode.CLAMP
            );
            borderPaint.setShader(gradient);
        }
        invalidate();
    }
}