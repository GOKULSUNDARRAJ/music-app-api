package com.Saalai.SalaiMusicApp;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Adapters.SleepTimerAdapter;
import com.Saalai.SalaiMusicApp.Models.SleepTimerModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SleepTimerBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "SleepTimerBottomSheet";

    private RecyclerView recyclerView;
    private TextView tvTimerStatus, tvTimerCountdown;
    private ImageView  btnClose;
    private ConstraintLayout btnCancelTimer;
    private SleepTimerAdapter adapter;

    private static CountDownTimer countDownTimer;
    private static long timeRemaining = 0;
    private static boolean isTimerActive = false;
    private static TimerListener timerListener;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateUIRunnable;

    public interface TimerListener {
        void onTimerStarted(int minutes);
        void onTimerFinished();
        void onTimerCancelled();
    }

    public static void setTimerListener(TimerListener listener) {
        timerListener = listener;
    }

    public static boolean isTimerActive() {
        return isTimerActive;
    }

    public static long getTimeRemaining() {
        return timeRemaining;
    }

    public static void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        isTimerActive = false;
        timeRemaining = 0;

        if (timerListener != null) {
            timerListener.onTimerCancelled();
        }
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_sleep_timer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewTimerOptions);
        tvTimerStatus = view.findViewById(R.id.tvTimerStatus);
        tvTimerCountdown = view.findViewById(R.id.tvTimerCountdown);
        btnCancelTimer = view.findViewById(R.id.btnCancelTimer);
        btnClose = view.findViewById(R.id.btnClose);

        setupRecyclerView();
        updateTimerUI();

        btnCancelTimer.setOnClickListener(v -> {
            cancelTimer();
            updateTimerUI();
            dismiss();
            Toast.makeText(getContext(), "Sleep timer cancelled", Toast.LENGTH_SHORT).show();

            // Broadcast update
            if (getContext() != null) {
                getContext().sendBroadcast(new Intent("UPDATE_TIMER_STATUS"));
            }
        });

        btnClose.setOnClickListener(v -> dismiss());

        // Update UI every second if timer is active
        if (isTimerActive) {
            startUIUpdater();
        }
    }

    private void setupRecyclerView() {
        List<SleepTimerModel> timerOptions = new ArrayList<>();
        timerOptions.add(new SleepTimerModel(5, "5 minutes"));
        timerOptions.add(new SleepTimerModel(10, "10 minutes"));
        timerOptions.add(new SleepTimerModel(15, "15 minutes"));
        timerOptions.add(new SleepTimerModel(30, "30 minutes"));
        timerOptions.add(new SleepTimerModel(45, "45 minutes"));
        timerOptions.add(new SleepTimerModel(60, "1 hour"));
        timerOptions.add(new SleepTimerModel(90, "1.5 hours"));
        timerOptions.add(new SleepTimerModel(120, "2 hours"));

        adapter = new SleepTimerAdapter(timerOptions, minutes -> {
            startSleepTimer(minutes);
            dismiss();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void startSleepTimer(int minutes) {
        // Cancel any existing timer
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        timeRemaining = TimeUnit.MINUTES.toMillis(minutes);
        isTimerActive = true;

        countDownTimer = new CountDownTimer(timeRemaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemaining = millisUntilFinished;
                updateCountdownDisplay();

                // Broadcast update for UI
                if (getContext() != null) {
                    getContext().sendBroadcast(new Intent("UPDATE_TIMER_STATUS"));
                }
            }

            @Override
            public void onFinish() {
                isTimerActive = false;
                timeRemaining = 0;

                // Stop playback
                if (PlayerManager.isPlaying()) {
                    PlayerManager.pausePlayback();
                }

                // Notify listener
                if (timerListener != null) {
                    timerListener.onTimerFinished();
                }

                // Show notification
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Sleep timer finished - Playback stopped", Toast.LENGTH_LONG).show();
                    getContext().sendBroadcast(new Intent("UPDATE_TIMER_STATUS"));
                }
            }
        }.start();

        // Notify listener
        if (timerListener != null) {
            timerListener.onTimerStarted(minutes);
        }

        // Show confirmation
        if (getContext() != null) {
            String message = String.format("Sleep timer set for %d minutes", minutes);
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            getContext().sendBroadcast(new Intent("UPDATE_TIMER_STATUS"));
        }
    }

    private void startUIUpdater() {
        updateUIRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTimerActive && isAdded()) {
                    updateCountdownDisplay();
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(updateUIRunnable);
    }

    private void updateCountdownDisplay() {
        if (isAdded() && tvTimerCountdown != null) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemaining);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(timeRemaining) -
                    TimeUnit.MINUTES.toSeconds(minutes);

            String timeLeft = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
            tvTimerCountdown.setText(timeLeft);
        }
    }

    private void updateTimerUI() {
        if (!isAdded()) return;

        if (isTimerActive) {
            tvTimerStatus.setText("");
            tvTimerStatus.setTextColor(getResources().getColor(android.R.color.white));
            tvTimerCountdown.setVisibility(View.VISIBLE);
            btnCancelTimer.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            updateCountdownDisplay();
            startUIUpdater();
        } else {
            tvTimerStatus.setText("");
            tvTimerStatus.setTextColor(getResources().getColor(android.R.color.white));
            tvTimerCountdown.setVisibility(View.GONE);
            btnCancelTimer.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(updateUIRunnable);
    }
}