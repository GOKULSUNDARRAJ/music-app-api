package com.Saalai.SalaiMusicApp.SliderAnimation;


import android.view.View;

import androidx.viewpager2.widget.ViewPager2;

public class FlipHorizontalPageTransformer implements ViewPager2.PageTransformer {
    @Override
    public void transformPage(View view, float position) {
        float percentage = 1 - Math.abs(position);
        view.setCameraDistance(12000);
        setVisibility(view, position);
        setTranslation(view);
        setSize(view, position, percentage);
        setRotation(view, position, percentage);
    }

    private void setVisibility(View view, float position) {
        if (position < 0.5 && position > -0.5) {
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.INVISIBLE);
        }
    }

    private void setTranslation(View view) {
        ViewPager2 viewPager = (ViewPager2) view.getParent();
        int scroll = viewPager.getScrollX() - view.getLeft();
        view.setTranslationX(scroll);
    }

    private void setSize(View view, float position, float percentage) {
        view.setScaleX((position != 0 && position != 1) ? percentage : 1);
        view.setScaleY((position != 0 && position != 1) ? percentage : 1);
    }

    private void setRotation(View view, float position, float percentage) {
        if (position > 0) {
            view.setRotationY(-180 * (percentage + 1));
        } else {
            view.setRotationY(180 * (percentage + 1));
        }
    }
}