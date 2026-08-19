package com.kira.kdownloader.settings.ui.components;

import android.content.Context;
import android.graphics.Typeface;
import androidx.appcompat.widget.AppCompatTextView;

public final class PreferenceGroupTitleView extends AppCompatTextView {
    public PreferenceGroupTitleView(Context context,String title){super(context);setText(title);setTypeface(Typeface.DEFAULT,Typeface.BOLD);setTextSize(14);setPadding(dp(16),dp(22),dp(16),dp(6));}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
