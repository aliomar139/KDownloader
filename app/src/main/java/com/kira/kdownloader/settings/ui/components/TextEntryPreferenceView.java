package com.kira.kdownloader.settings.ui.components;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.kira.kdownloader.R;

import java.util.function.Consumer;
import java.util.function.Function;

public final class TextEntryPreferenceView extends ClickablePreferenceView {
    public TextEntryPreferenceView(Context context, String title, String value, boolean numeric,
                                   boolean password, boolean enabled, String summary, String placeholder,
                                   Function<String, String> validate, Consumer<String> change) {
        super(context, title, summary != null ? summary :
                (password && !value.isEmpty() ? "••••••••" : value), 0, null);
        setOnClickListener(ignored -> {
            android.view.View content = LayoutInflater.from(context).inflate(R.layout.dialog_text_entry, null);
            TextInputEditText input = content.findViewById(R.id.dialog_input);
            input.setText(value);
            input.setSelectAllOnFocus(true);
            input.setHint(placeholder);
            if (numeric) input.setInputType(InputType.TYPE_CLASS_NUMBER);
            if (password) input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            AlertDialog dialog = new MaterialAlertDialogBuilder(context).setTitle(title).setView(content)
                    .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.save, null).create();
            dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String draft = input.getText() == null ? "" : input.getText().toString();
                String error = validate.apply(draft);
                input.setError(error);
                if (error == null) {
                    change.accept(draft);
                    dialog.dismiss();
                }
            }));
            dialog.show();
        });
        setClickable(enabled);
        setFocusable(enabled);
        setPreferenceEnabled(enabled);
    }
}
