package com.kira.kdownloader.settings.ui.components;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.slider.Slider;
import com.kira.kdownloader.R;

import java.util.function.Consumer;

public final class IntSliderPreferenceView extends LinearLayout {
    public IntSliderPreferenceView(Context context, String title, int value, int min, int max,
                                   Consumer<Integer> change) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.view_preference_slider, this, true);
        TextView titleView = findViewById(R.id.slider_title);
        TextView valueView = findViewById(R.id.slider_value);
        Slider slider = findViewById(R.id.preference_slider);
        titleView.setText(title);
        valueView.setText(Integer.toString(value));
        slider.setValueFrom(min);
        slider.setValueTo(max);
        slider.setStepSize(1);
        slider.setValue(value);
        slider.addOnChangeListener((view, next, fromUser) -> {
            if (fromUser) valueView.setText(Integer.toString(Math.round(next)));
        });
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override public void onStartTrackingTouch(@NonNull Slider view) { }
            @Override public void onStopTrackingTouch(@NonNull Slider view) {
                change.accept(Math.round(view.getValue()));
            }
        });
    }
}
