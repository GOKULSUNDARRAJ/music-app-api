package com.Saalai.SalaiMusicApp.Activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.Saalai.SalaiMusicApp.R;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

}

