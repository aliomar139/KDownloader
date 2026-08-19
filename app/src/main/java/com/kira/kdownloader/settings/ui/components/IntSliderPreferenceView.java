package com.kira.kdownloader.settings.ui.components;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.android.material.slider.Slider;
import java.util.function.Consumer;

public final class IntSliderPreferenceView extends LinearLayout {
    public IntSliderPreferenceView(Context context,String title,int value,int min,int max,Consumer<Integer> change){
        super(context);setOrientation(VERTICAL);setPadding(dp(16),dp(8),dp(16),dp(8));
        TextView label=new TextView(context);label.setText(title+": "+value);addView(label);
        Slider slider=new Slider(context);slider.setValueFrom(min);slider.setValueTo(max);slider.setStepSize(1);slider.setValue(value);
        slider.addOnChangeListener((view,next,fromUser)->{if(fromUser)label.setText(title+": "+Math.round(next));});
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener(){public void onStartTrackingTouch(@NonNull Slider view){}public void onStopTrackingTouch(@NonNull Slider view){change.accept(Math.round(view.getValue()));}});addView(slider);
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
