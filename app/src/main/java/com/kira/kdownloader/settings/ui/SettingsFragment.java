package com.kira.kdownloader.settings.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;
import androidx.fragment.app.Fragment;

import com.kira.kdownloader.MainActivity;
import com.kira.kdownloader.ui.ComposeScreenBridge;

public final class SettingsFragment extends Fragment {
    @NonNull @Override public View onCreateView(@NonNull LayoutInflater inflater,
                                                @Nullable ViewGroup container,
                                                @Nullable Bundle savedInstanceState) {
        ComposeView view = new ComposeView(requireContext());
        MainActivity activity = (MainActivity) requireActivity();
        ComposeScreenBridge.setSettingsContent(view, activity.getSettingsViewModel());
        return view;
    }
}
