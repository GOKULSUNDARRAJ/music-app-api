package com.Saalai.SalaiMusicApp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
public class GappedBorderView extends View {
    private Paint borderPaint;
    private Paint textMeasurePaint;
    private String label = "";
    private float strokeWidth = 4f;
    private float cornerRadius = 30f;
    private float textMargin = 15f; // Extra space around the text

    public GappedBorderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(strokeWidth);
        borderPaint.setColor(Color.WHITE);

        // This paint is used only to calculate text size
        textMeasurePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textMeasurePaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
    }

    public void setLabel(String text) {
        this.label = text;
        invalidate(); // Redraw with new gap size
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();

        // 1. Calculate the gap
        float textWidth = textMeasurePaint.measureText(label);
        float gapStart = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
        float gapEnd = gapStart + textWidth + textMargin;

        Path path = new Path();

        // 2. Draw path starting from the end of the gap
        path.moveTo(gapEnd, 0);
        path.lineTo(w - cornerRadius, 0);
        path.quadTo(w, 0, w, cornerRadius); // Top-right

        path.lineTo(w, h - cornerRadius);
        path.quadTo(w, h, w - cornerRadius, h); // Bottom-right

        path.lineTo(cornerRadius, h);
        path.quadTo(0, h, 0, h - cornerRadius); // Bottom-left

        path.lineTo(0, cornerRadius);
        path.quadTo(0, 0, cornerRadius, 0); // Top-left

        // 3. Draw top line up to the start of the gap
        path.lineTo(gapStart, 0);

        canvas.drawPath(path, borderPaint);
    }
}