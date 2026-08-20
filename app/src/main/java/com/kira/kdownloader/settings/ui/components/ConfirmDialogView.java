package com.kira.kdownloader.settings.ui.components;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kira.kdownloader.R;

public final class ConfirmDialogView {
    private ConfirmDialogView() { }

    public static void show(Context context, String title, String message, Runnable confirm) {
        android.view.View content = LayoutInflater.from(context).inflate(R.layout.dialog_confirm, null);
        ((TextView) content.findViewById(R.id.confirm_message)).setText(message);
        new MaterialAlertDialogBuilder(context).setTitle(title).setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> confirm.run()).show();
    }
}
