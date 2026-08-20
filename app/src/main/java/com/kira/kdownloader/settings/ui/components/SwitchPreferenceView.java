package com.kira.kdownloader.settings.ui.components;

import android.content.Context;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.function.Consumer;

public final class SwitchPreferenceView extends ClickablePreferenceView {
    public SwitchPreferenceView(Context context,String title,String subtitle,boolean checked,boolean enabled,Consumer<Boolean> change){
        super(context,title,subtitle,0,null);
        MaterialSwitch toggle=new MaterialSwitch(context);toggle.setChecked(checked);toggle.setEnabled(enabled);
        toggle.setOnCheckedChangeListener((button,value)->change.accept(value));setTrailingView(toggle);
        setOnClickListener(ignored->{if(enabled)toggle.setChecked(!toggle.isChecked());});setPreferenceEnabled(enabled);
    }
}
