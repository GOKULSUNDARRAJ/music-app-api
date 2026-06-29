package com.Saalai.SalaiMusicApp.Activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.Saalai.SalaiMusicApp.Adapters.OnboardingAdapter;
import com.Saalai.SalaiMusicApp.Models.OnboardingItem;
import com.Saalai.SalaiMusicApp.R;
import java.util.ArrayList;
import java.util.List;
public class OnboardingActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private LinearLayout dotsLayout;
    private TextView btnNext;
    private OnboardingAdapter adapter;
    private List<OnboardingItem> onboardingItems;
    private TextView[] dots;
    LinearLayout layoutNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);


        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.black)); // replace with your color
        }

        View rootView = findViewById(R.id.root_view);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && Build.VERSION.SDK_INT <= 34) {
            // For Android 11 to 14, explicitly disable edge-to-edge
            getWindow().setDecorFitsSystemWindows(true);

            if (rootView != null) {
                rootView.setOnApplyWindowInsetsListener((v, insets) -> {
                    v.setPadding(0, 0, 0, 0);
                    return insets;
                });
            }
        } else {
            // Pre-Android 12
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            if (rootView != null) {
                rootView.setFitsSystemWindows(true); // critical for proper layout
            }
        }





        // Initialize views
        viewPager = findViewById(R.id.viewPager);
        dotsLayout = findViewById(R.id.layoutDots);
        btnNext = findViewById(R.id.btnNext);
        layoutNext= findViewById(R.id.layoutNext);

        // Setup onboarding items
        onboardingItems = new ArrayList<>();
        onboardingItems.add(new OnboardingItem(
                R.drawable.onboarding1,
                "All the Music You Love, One Place",
                "Stream millions of songs and discover new music made just for you."
        ));
        onboardingItems.add(new OnboardingItem(
                R.drawable.onboarding2,
                "Find Your Sound",
                "Explore songs, albums, and playlists across every genre and mood."
        ));
        onboardingItems.add(new OnboardingItem(
                R.drawable.onboarding3,
                "Music Made for You",
                "Enjoy personalized playlists based on your listening habits and favorites."
        ));

        onboardingItems.add(new OnboardingItem(
                R.drawable.onboarding4,
                "Stay Connected to Artists",
                "Explore songs, albums, and playlists across every genre and mood"
        ));

        onboardingItems.add(new OnboardingItem(
                R.drawable.onboarding5,
                "Listen Anytime, Anywhere",
                "Play music while working, driving, or relaxing - even offline"
        ));


        // Setup adapter
        adapter = new OnboardingAdapter(this, onboardingItems);
        viewPager.setAdapter(adapter);

        // Add dots indicator
        addDotsIndicator(0);

        // Button click listener
        layoutNext.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
            finish();
        });

        // ViewPager page change listener
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                addDotsIndicator(position);


            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });


    }

    private void addDotsIndicator(int currentPosition) {
        dots = new TextView[onboardingItems.size()];
        dotsLayout.removeAllViews();

        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText("•"); // Using bullet character as dot
            dots[i].setTextSize(35);
            dots[i].setTextColor(getResources().getColor(
                    i == currentPosition ? R.color.yellow : R.color.white));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            dotsLayout.addView(dots[i], params);

        }
    }



    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }



}
