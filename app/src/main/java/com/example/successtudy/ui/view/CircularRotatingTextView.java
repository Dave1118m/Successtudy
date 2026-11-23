package com.example.successtudy.ui.view;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

public class CircularRotatingTextView extends View {

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orbitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path circlePath = new Path();
    private final String[] fourWords = new String[]{"• STUDY", "• SMARTER", "• LEARN", "• BETTER"};
    private ObjectAnimator rotationAnimator;

    public CircularRotatingTextView(Context context) {
        super(context);
        init();
    }

    public CircularRotatingTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularRotatingTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Dotted circular orbit line
        orbitPaint.setColor(Color.parseColor("#4500BFA6")); // Translucent teal
        orbitPaint.setStyle(Paint.Style.STROKE);
        orbitPaint.setStrokeWidth(dpToPx(1.5f));
        orbitPaint.setPathEffect(new DashPathEffect(new float[]{dpToPx(4f), dpToPx(4f)}, 0f));

        // Circular rotating text
        textPaint.setColor(Color.parseColor("#64FFDA")); // Vibrant teal glow
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(spToPx(10f));
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        textPaint.setLetterSpacing(0.14f);

        startRotationAnimation();
    }

    private void startRotationAnimation() {
        rotationAnimator = ObjectAnimator.ofFloat(this, View.ROTATION, 0f, 360f);
        rotationAnimator.setDuration(16000);
        rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimator.setInterpolator(new LinearInterpolator());
        rotationAnimator.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        circlePath.reset();
        float centerX = w / 2f;
        float centerY = h / 2f;
        float radius = (Math.min(w, h) / 2f) - dpToPx(8f);
        circlePath.addCircle(centerX, centerY, radius, Path.Direction.CW);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = (Math.min(getWidth(), getHeight()) / 2f) - dpToPx(8f);

        if (radius <= 0) return;

        // Draw dotted circular rotating line
        canvas.drawCircle(centerX, centerY, radius, orbitPaint);

        // Circumference divided equally into 4 quadrants for the 4 words
        float circumference = 2f * (float) Math.PI * radius;
        float quadrantLength = circumference / 4f;

        for (int i = 0; i < fourWords.length; i++) {
            String word = fourWords[i];
            float wordWidth = textPaint.measureText(word);
            // Center the word within its quadrant
            float hOffset = (i * quadrantLength) + ((quadrantLength - wordWidth) / 2f);
            canvas.drawTextOnPath(word, circlePath, hOffset, dpToPx(3.5f), textPaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (rotationAnimator != null) {
            rotationAnimator.cancel();
        }
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private float spToPx(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }
}
