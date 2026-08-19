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
import com.kira.kdownloader.util.StateHolder;

public final class HomeFragment extends Fragment {
    private static final String ARG_INITIAL_URL = "initial_url";
    private final StateHolder<String> incomingUrl = new StateHolder<>("");

    public static HomeFragment newInstance(String initialUrl) {
        HomeFragment fragment = new HomeFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_INITIAL_URL, initialUrl);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) incomingUrl.set(arguments.getString(ARG_INITIAL_URL, ""));
    }

    @NonNull @Override public View onCreateView(@NonNull LayoutInflater inflater,
                                                @Nullable ViewGroup container,
                                                @Nullable Bundle savedInstanceState) {
        ComposeView view = new ComposeView(requireContext());
        MainActivity activity = (MainActivity) requireActivity();
        ComposeScreenBridge.setHomeContent(
                view, incomingUrl, activity.getSettingsViewModel(), activity::toggleTheme);
        return view;
    }

    public void setInitialUrl(String url) {
        incomingUrl.set(url == null ? "" : url);
    }
}
