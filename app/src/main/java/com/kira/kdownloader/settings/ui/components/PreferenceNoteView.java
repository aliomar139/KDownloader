package com.kira.kdownloader.settings.ui.components;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kira.kdownloader.R;

public final class PreferenceNoteView extends LinearLayout {
    public PreferenceNoteView(Context context, String text) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.view_preference_note, this, true);
        ((TextView) findViewById(R.id.preference_note_text)).setText(text);
    }
}
