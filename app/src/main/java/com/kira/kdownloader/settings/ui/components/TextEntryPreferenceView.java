package com.kira.kdownloader.settings.ui.components;

import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kira.kdownloader.R;
import java.util.function.Consumer;
import java.util.function.Function;

public final class TextEntryPreferenceView extends ClickablePreferenceView {
    public TextEntryPreferenceView(Context context,String title,String value,boolean numeric,boolean password,boolean enabled,String summary,String placeholder,Function<String,String> validate,Consumer<String> change){
        super(context,title,summary!=null?summary:(password&&!value.isEmpty()?"\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022":value),0,null);
        setOnClickListener(ignored->{EditText input=new EditText(context);input.setText(value);input.setSelectAllOnFocus(true);input.setHint(placeholder);if(numeric)input.setInputType(InputType.TYPE_CLASS_NUMBER);if(password)input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);AlertDialog dialog=new MaterialAlertDialogBuilder(context).setTitle(title).setView(input).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.save,null).create();dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String draft=input.getText().toString();String error=validate.apply(draft);input.setError(error);if(error==null){change.accept(draft);dialog.dismiss();}}));dialog.show();});
        setPreferenceEnabled(enabled);
    }
}
