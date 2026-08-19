package com.kira.kdownloader.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;
import androidx.fragment.app.Fragment;

import com.kira.kdownloader.MainActivity;

public final class HistoryFragment extends Fragment {
    @NonNull @Override public View onCreateView(@NonNull LayoutInflater inflater,
                                                @Nullable ViewGroup container,
                                                @Nullable Bundle savedInstanceState) {
        ComposeView view = new ComposeView(requireContext());
        MainActivity activity = (MainActivity) requireActivity();
        ComposeScreenBridge.setHistoryContent(
                view, activity.getSettingsViewModel(), activity::toggleTheme, activity::openHome);
        return view;
    }
}
