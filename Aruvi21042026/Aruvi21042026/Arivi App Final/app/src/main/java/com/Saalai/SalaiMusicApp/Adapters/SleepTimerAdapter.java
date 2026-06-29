package com.Saalai.SalaiMusicApp.Adapters;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Models.SleepTimerModel;
import com.Saalai.SalaiMusicApp.R;

import java.util.List;

public class SleepTimerAdapter extends RecyclerView.Adapter<SleepTimerAdapter.ViewHolder> {

    private List<SleepTimerModel> timerOptions;
    private OnTimerSelectedListener listener;

    public interface OnTimerSelectedListener {
        void onTimerSelected(int minutes);
    }

    public SleepTimerAdapter(List<SleepTimerModel> timerOptions, OnTimerSelectedListener listener) {
        this.timerOptions = timerOptions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sleep_timer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SleepTimerModel option = timerOptions.get(position);
        holder.tvTimerOption.setText(option.getDisplayName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTimerSelected(option.getMinutes());
            }
        });
    }

    @Override
    public int getItemCount() {
        return timerOptions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTimerOption;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimerOption = itemView.findViewById(R.id.tvTimerOption);
        }
    }
}