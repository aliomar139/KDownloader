package com.kira.kdownloader.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.kira.kdownloader.R;

public final class StateView extends LinearLayout {
    private final ImageView icon;
    private final TextView title;
    private final TextView message;
    private final MaterialButton action;

    public StateView(Context context) { this(context, null); }

    public StateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        float density = getResources().getDisplayMetrics().density;
        setPadding(Math.round(20 * density), Math.round(24 * density),
                Math.round(20 * density), Math.round(24 * density));
        LayoutInflater.from(context).inflate(R.layout.view_state, this, true);
        icon = findViewById(R.id.state_icon);
        title = findViewById(R.id.state_title);
        message = findViewById(R.id.state_message);
        action = findViewById(R.id.state_action);
    }

    public void setState(@DrawableRes int iconRes, CharSequence titleText, @Nullable CharSequence messageText) {
        icon.setImageResource(iconRes);
        title.setText(titleText);
        message.setText(messageText);
        message.setVisibility(messageText == null || messageText.length() == 0 ? GONE : VISIBLE);
    }

    public void setAction(CharSequence label, @Nullable OnClickListener listener) {
        action.setText(label);
        action.setOnClickListener(listener);
        action.setVisibility(listener == null ? GONE : VISIBLE);
    }
}
