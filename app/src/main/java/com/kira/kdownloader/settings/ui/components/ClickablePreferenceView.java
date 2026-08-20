package com.kira.kdownloader.settings.ui.components;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.kira.kdownloader.R;

public class ClickablePreferenceView extends FrameLayout {
    private final FrameLayout trailing;

    public ClickablePreferenceView(Context context, String title, String subtitle, int icon, Runnable click) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.view_preference_clickable, this, true);
        ImageView iconView = findViewById(R.id.preference_icon);
        TextView titleView = findViewById(R.id.preference_title);
        TextView subtitleView = findViewById(R.id.preference_subtitle);
        trailing = findViewById(R.id.preference_trailing);

        titleView.setText(title);
        if (icon != 0) {
            iconView.setImageResource(icon);
            iconView.setVisibility(VISIBLE);
        }
        if (subtitle != null && !subtitle.isEmpty()) {
            subtitleView.setText(subtitle);
            subtitleView.setVisibility(VISIBLE);
        }
        TypedValue ripple = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
        setForeground(context.getDrawable(ripple.resourceId));
        setClickable(click != null);
        setFocusable(click != null);
        if (click != null) setOnClickListener(ignored -> click.run());
        else trailing.setVisibility(GONE);
    }

    public void setTrailingView(View view) {
        trailing.removeAllViews();
        trailing.addView(view);
        trailing.setVisibility(VISIBLE);
    }

    public void setPreferenceEnabled(boolean enabled) {
        setEnabled(enabled);
        setClickable(enabled && hasOnClickListeners());
        setAlpha(enabled ? 1f : .38f);
    }
}
