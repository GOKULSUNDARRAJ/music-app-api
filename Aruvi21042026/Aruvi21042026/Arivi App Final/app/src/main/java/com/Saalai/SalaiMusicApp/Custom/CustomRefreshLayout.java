package com.Saalai.SalaiMusicApp.Custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.R;

public class CustomRefreshLayout extends FrameLayout {

    private View refreshHeader;
    private RecyclerView recyclerView;
    private OnRefreshListener refreshListener;
    private View contentView;
    private ProgressBar refreshProgressBar;
    private TextView tvPullToRefresh;
    private TextView tvReleaseToRefresh;
    private TextView tvRefreshing;

    private float startY = 0;
    private float currentTranslation = 0;
    private boolean isDragging = false;
    private boolean isRefreshing = false;
    private int touchSlop;

    private static final int MAX_PULL_DISTANCE = 300;
    private static final int REFRESH_THRESHOLD = 150;

    public interface OnRefreshListener {
        void onRefresh();
    }

    public CustomRefreshLayout(@NonNull Context context) {
        super(context);
        init(context);
    }

    public CustomRefreshLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // Find views
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getId() == R.id.refreshHeader) {
                refreshHeader = child;
                // Initialize header views
                refreshProgressBar = refreshHeader.findViewById(R.id.refreshProgressBar);

            } else if (child instanceof FrameLayout) {
                contentView = child;
                // Find RecyclerView inside FrameLayout
                FrameLayout frameLayout = (FrameLayout) child;
                for (int j = 0; j < frameLayout.getChildCount(); j++) {
                    View innerChild = frameLayout.getChildAt(j);
                    if (innerChild instanceof RecyclerView) {
                        recyclerView = (RecyclerView) innerChild;
                        break;
                    }
                }
            }
        }

        // Initially hide header
        if (refreshHeader != null) {
            refreshHeader.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isRefreshing || recyclerView == null || contentView == null) {
            return false;
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = ev.getY();
                isDragging = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if (!isDragging) {
                    float dy = ev.getY() - startY;

                    // Check if pulling down from top
                    if (dy > touchSlop && !recyclerView.canScrollVertically(-1)) {
                        isDragging = true;
                        return true;
                    }
                }
                break;
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isRefreshing || recyclerView == null || refreshHeader == null || contentView == null) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    float currentY = event.getY();
                    float pullDistance = (currentY - startY) / 2; // Reduce sensitivity

                    if (pullDistance > 0) {
                        // Show header
                        if (refreshHeader.getVisibility() != View.VISIBLE) {
                            refreshHeader.setVisibility(View.VISIBLE);
                        }

                        // Limit pull distance
                        float translationY = Math.min(pullDistance, MAX_PULL_DISTANCE);
                        currentTranslation = translationY;

                        // Move the entire content view (which contains RecyclerView)
                        contentView.setTranslationY(translationY);

                        // Update progress and text based on pull distance
                        updateProgress(translationY);

                        return true;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    isDragging = false;

                    if (currentTranslation >= REFRESH_THRESHOLD && !isRefreshing) {
                        startRefreshing();
                    } else {
                        resetPosition();
                    }
                }
                break;
        }

        return false;
    }

    private void updateProgress(float translationY) {
        if (refreshProgressBar != null) {
            int progress = (int) ((translationY / REFRESH_THRESHOLD) * 100);
            progress = Math.min(progress, 100);
            refreshProgressBar.setProgress(progress);
        }

        // Update text based on pull distance
        if (tvPullToRefresh != null && tvReleaseToRefresh != null) {
            if (translationY >= REFRESH_THRESHOLD) {
                tvPullToRefresh.setVisibility(View.GONE);
                tvReleaseToRefresh.setVisibility(View.VISIBLE);
            } else {
                tvPullToRefresh.setVisibility(View.VISIBLE);
                tvReleaseToRefresh.setVisibility(View.GONE);
            }
        }
    }

    private void startRefreshing() {
        isRefreshing = true;

        if (refreshHeader != null && contentView != null) {
            // Update UI for refreshing state
            if (tvPullToRefresh != null) tvPullToRefresh.setVisibility(View.GONE);
            if (tvReleaseToRefresh != null) tvReleaseToRefresh.setVisibility(View.GONE);
            if (tvRefreshing != null) tvRefreshing.setVisibility(View.VISIBLE);

            // Set progress bar to indeterminate mode while refreshing
            if (refreshProgressBar != null) {
                refreshProgressBar.setIndeterminate(true);
                refreshProgressBar.setProgress(0);
            }

            // Keep at threshold position
            contentView.animate()
                    .translationY(REFRESH_THRESHOLD)
                    .setDuration(200)
                    .start();

            if (refreshListener != null) {
                refreshListener.onRefresh();
            }
        }
    }

    public void finishRefreshing() {
        isRefreshing = false;
        resetPosition();
    }

    private void resetPosition() {
        if (contentView != null) {
            contentView.animate()
                    .translationY(0)
                    .setDuration(300)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if (refreshHeader != null) {
                                refreshHeader.setVisibility(View.GONE);

                                // Reset header UI
                                if (tvPullToRefresh != null) tvPullToRefresh.setVisibility(View.VISIBLE);
                                if (tvReleaseToRefresh != null) tvReleaseToRefresh.setVisibility(View.GONE);
                                if (tvRefreshing != null) tvRefreshing.setVisibility(View.GONE);

                                // Reset progress bar
                                if (refreshProgressBar != null) {
                                    refreshProgressBar.setIndeterminate(false);
                                    refreshProgressBar.setProgress(0);
                                }
                            }
                        }
                    })
                    .start();
        }
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        this.refreshListener = listener;
    }

    public boolean isRefreshing() {
        return isRefreshing;
    }
}