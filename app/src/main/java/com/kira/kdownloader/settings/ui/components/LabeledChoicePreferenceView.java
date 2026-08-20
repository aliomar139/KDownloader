package com.kira.kdownloader.settings.ui.components;

import android.content.Context;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;
import java.util.function.Consumer;

public final class LabeledChoicePreferenceView extends ClickablePreferenceView {
    public static final class Option { public final String value,label; public Option(String value,String label){this.value=value;this.label=label;} }
    public LabeledChoicePreferenceView(Context context,String title,List<Option> options,String selected,boolean enabled,Consumer<String> change){
        super(context,title,label(options,selected),0,null);
        setOnClickListener(ignored->{String[] labels=new String[options.size()];int checked=0;for(int i=0;i<labels.length;i++){labels[i]=options.get(i).label;if(options.get(i).value.equals(selected))checked=i;}new MaterialAlertDialogBuilder(context).setTitle(title).setSingleChoiceItems(labels,checked,(dialog,which)->{change.accept(options.get(which).value);dialog.dismiss();}).show();});
        setPreferenceEnabled(enabled);
    }
    private static String label(List<Option> options,String selected){for(Option option:options)if(option.value.equals(selected))return option.label;return selected;}
}
