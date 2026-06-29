package com.Saalai.SalaiMusicApp.SliderAnimation;

import android.view.View;

import androidx.viewpager2.widget.ViewPager2;

public class CubeOutPageTransformer implements ViewPager2.PageTransformer {
    public void transformPage(View view, float position) {
        if (position < -1) {    // [-Infinity,-1)
            // This page is way off-screen to the left.
            view.setAlpha(0);
        } else if (position <= 0) {    // [-1,0]
            view.setAlpha(1);
            view.setPivotX(view.getWidth());
            view.setRotationY(-90 * Math.abs(position));
        } else if (position <= 1) {    // (0,1]
            view.setAlpha(1);
            view.setPivotX(0);
            view.setRotationY(90 * Math.abs(position));
        } else {    // (1,+Infinity]
            // This page is way off-screen to the right.
            view.setAlpha(0);
        }
    }
}