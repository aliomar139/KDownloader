package com.kira.kdownloader.settings.ui.components;

import android.content.Context;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kira.kdownloader.settings.SettingOption;
import java.util.function.Consumer;

public final class SingleChoicePreferenceView<T extends Enum<T> & SettingOption> extends ClickablePreferenceView {
    public SingleChoicePreferenceView(Context context,String title,T[] values,T selected,boolean enabled,Consumer<T> change){
        super(context,title,selected.getLabel(),0,null);
        setOnClickListener(ignored->{String[] labels=new String[values.length];int checked=0;for(int i=0;i<values.length;i++){labels[i]=values[i].getLabel();if(values[i]==selected)checked=i;}new MaterialAlertDialogBuilder(context).setTitle(title).setSingleChoiceItems(labels,checked,(dialog,which)->{change.accept(values[which]);dialog.dismiss();}).show();});
        setPreferenceEnabled(enabled);
    }
}
