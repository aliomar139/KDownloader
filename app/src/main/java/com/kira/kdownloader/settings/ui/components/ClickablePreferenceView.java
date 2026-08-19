package com.kira.kdownloader.settings.ui.components;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ClickablePreferenceView extends LinearLayout {
    private final LinearLayout labels;

    public ClickablePreferenceView(Context context,String title,String subtitle,int icon,Runnable click){
        super(context);setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);int p=dp(16);setPadding(p,dp(11),p,dp(11));setMinimumHeight(dp(56));setBackgroundResource(android.R.drawable.list_selector_background);
        if(icon!=0){ImageView image=new ImageView(context);image.setImageResource(icon);addView(image,new LayoutParams(dp(32),dp(32)));}
        labels=new LinearLayout(context);labels.setOrientation(VERTICAL);
        TextView titleView=new TextView(context);titleView.setText(title);titleView.setTextSize(16);labels.addView(titleView);
        if(subtitle!=null&&!subtitle.isEmpty()){TextView subtitleView=new TextView(context);subtitleView.setText(subtitle);subtitleView.setTextSize(13);subtitleView.setMaxLines(3);labels.addView(subtitleView);}
        LayoutParams params=new LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1);params.setMarginStart(icon==0?0:dp(14));addView(labels,params);
        if(click!=null)setOnClickListener(ignored->click.run());
    }

    public void setTrailingView(View view){addView(view);}
    public void setPreferenceEnabled(boolean enabled){setEnabled(enabled);setAlpha(enabled?1f:.45f);}
    protected int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
