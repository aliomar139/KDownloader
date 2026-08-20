package com.kira.kdownloader.settings.ui.components;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kira.kdownloader.R;

public final class PreferenceGroupTitleView extends LinearLayout {
    public PreferenceGroupTitleView(Context context, String title) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.view_preference_group_title, this, true);
        ((TextView) findViewById(R.id.group_title)).setText(title);
    }
}
