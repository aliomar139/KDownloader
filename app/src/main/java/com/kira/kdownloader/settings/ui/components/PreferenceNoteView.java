package com.kira.kdownloader.settings.ui.components;

import android.content.Context;
import androidx.appcompat.widget.AppCompatTextView;

public final class PreferenceNoteView extends AppCompatTextView {
    public PreferenceNoteView(Context context,String text){super(context);setText(text);setTextSize(13);setPadding(dp(16),dp(8),dp(16),dp(12));}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
