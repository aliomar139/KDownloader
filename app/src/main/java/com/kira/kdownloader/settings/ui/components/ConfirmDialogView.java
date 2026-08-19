package com.kira.kdownloader.settings.ui.components;

import android.content.Context;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kira.kdownloader.R;

public final class ConfirmDialogView {
    private ConfirmDialogView(){}
    public static void show(Context context,String title,String message,Runnable confirm){new MaterialAlertDialogBuilder(context).setTitle(title).setMessage(message).setNegativeButton(R.string.cancel,null).setPositiveButton(android.R.string.ok,(dialog,which)->confirm.run()).show();}
}
