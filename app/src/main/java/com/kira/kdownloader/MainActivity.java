package com.kira.kdownloader;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.DynamicColors;
import com.kira.kdownloader.service.DownloadEvents;
import com.kira.kdownloader.settings.AppTheme;
import com.kira.kdownloader.settings.AppearanceSettings;
import com.kira.kdownloader.settings.SettingsRepository;
import com.kira.kdownloader.settings.platform.LanguageManager;
import com.kira.kdownloader.settings.store.SharedPreferencesKeyValueStore;
import com.kira.kdownloader.settings.ui.SettingsFragment;
import com.kira.kdownloader.settings.ui.SettingsViewModel;
import com.kira.kdownloader.ui.HistoryFragment;
import com.kira.kdownloader.ui.HomeFragment;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MainActivity extends AppCompatActivity {
    private static final String STATE_SELECTED_TAB = "selected_tab";
    private static final String TAG_HOME = "home";
    private static final String TAG_HISTORY = "history";
    private static final String TAG_SETTINGS = "settings";
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    ignored -> { });

    private BottomNavigationView bottomNavigation;
    private HomeFragment homeFragment;
    private HistoryFragment historyFragment;
    private SettingsFragment settingsFragment;
    private SettingsViewModel settingsViewModel;
    private AppearanceSettings launchAppearance;
    private AppearanceSettings currentAppearance;
    private String sharedUrl = "";
    private int selectedTab = R.id.tab_home;

    @Override protected void attachBaseContext(Context newBase) {
        String languageTag = "";
        try {
            languageTag = new SettingsRepository(
                    new SharedPreferencesKeyValueStore(newBase)).read()
                    .getAppearance().getLanguageTag();
        } catch (Throwable ignored) {
            // The default locale is safe if preferences are unavailable during early startup.
        }
        super.attachBaseContext(LanguageManager.wrap(newBase, languageTag));
    }

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        launchAppearance = new SettingsRepository(
                new SharedPreferencesKeyValueStore(this)).read().getAppearance();
        currentAppearance = launchAppearance;
        AppCompatDelegate.setDefaultNightMode(nightModeFor(launchAppearance.getTheme()));
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        if (launchAppearance.getDynamicColor()) DynamicColors.applyToActivityIfAvailable(this);
        if (launchAppearance.getHighContrast()) {
            getTheme().applyStyle(R.style.ThemeOverlay_KDownloader_HighContrast, true);
        }

        setContentView(R.layout.activity_main);
        applyWindowInsets(findViewById(R.id.main_root));
        readSharedUrl(getIntent());
        requestRuntimePermissions();

        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        selectedTab = savedInstanceState == null
                ? R.id.tab_home : savedInstanceState.getInt(STATE_SELECTED_TAB, R.id.tab_home);
        installFragments(savedInstanceState == null);

        bottomNavigation.setOnItemSelectedListener(this::onTabSelected);
        bottomNavigation.setSelectedItemId(selectedTab);
        switchTab(selectedTab, false);

        observeAppearance();
        DownloadEvents.getStates().live().observe(this, this::updateHistoryBadge);
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_SELECTED_TAB, selectedTab);
        super.onSaveInstanceState(outState);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (readSharedUrl(intent)) openHome(sharedUrl);
    }

    public SettingsViewModel getSettingsViewModel() {
        return settingsViewModel;
    }

    public void toggleTheme() {
        AppearanceSettings appearance = settingsViewModel.getSettingsValue().getAppearance();
        AppTheme next = isDarkTheme(appearance.getTheme()) ? AppTheme.LIGHT : AppTheme.DARK;
        settingsViewModel.setAppearance(appearance.withTheme(next));
    }

    public void openHome(String url) {
        sharedUrl = url == null ? "" : url;
        homeFragment.setInitialUrl(sharedUrl);
        bottomNavigation.setSelectedItemId(R.id.tab_home);
        switchTab(R.id.tab_home, true);
    }

    private void installFragments(boolean firstCreation) {
        if (firstCreation) {
            homeFragment = HomeFragment.newInstance(sharedUrl);
            historyFragment = new HistoryFragment();
            settingsFragment = new SettingsFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, settingsFragment, TAG_SETTINGS).hide(settingsFragment)
                    .add(R.id.fragment_container, historyFragment, TAG_HISTORY).hide(historyFragment)
                    .add(R.id.fragment_container, homeFragment, TAG_HOME)
                    .commitNow();
        } else {
            homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(TAG_HOME);
            historyFragment = (HistoryFragment) getSupportFragmentManager().findFragmentByTag(TAG_HISTORY);
            settingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag(TAG_SETTINGS);
            homeFragment.setInitialUrl(sharedUrl);
        }
    }

    private boolean onTabSelected(android.view.MenuItem item) {
        switchTab(item.getItemId(), true);
        return true;
    }

    private void switchTab(int tabId, boolean animate) {
        Fragment target;
        if (tabId == R.id.tab_history) target = historyFragment;
        else if (tabId == R.id.tab_settings) target = settingsFragment;
        else {
            tabId = R.id.tab_home;
            target = homeFragment;
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (animate && !currentAppearance.getReduceAnimations()) {
            transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            transaction.setCustomAnimations(0, 0);
        }
        transaction.hide(homeFragment).hide(historyFragment).hide(settingsFragment).show(target).commit();
        selectedTab = tabId;
    }

    private void observeAppearance() {
        settingsViewModel.getSettingsLive().observe(this, settings -> {
            AppearanceSettings appearance = settings.getAppearance();
            currentAppearance = appearance;
            LanguageManager.apply(this, appearance.getLanguageTag());
            AppCompatDelegate.setDefaultNightMode(nightModeFor(appearance.getTheme()));

            boolean localeNeedsRestart = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                    && !appearance.getLanguageTag().equals(launchAppearance.getLanguageTag());
            if (appearance.getDynamicColor() != launchAppearance.getDynamicColor()
                    || appearance.getHighContrast() != launchAppearance.getHighContrast()
                    || localeNeedsRestart) {
                recreate();
            }
        });
    }

    private void updateHistoryBadge(Map<String, DownloadEvents.State> states) {
        int active = 0;
        for (DownloadEvents.State state : states.values()) {
            if (state.getPhase() == DownloadEvents.Phase.PREPARING
                    || state.getPhase() == DownloadEvents.Phase.RUNNING) active++;
        }
        if (active == 0) bottomNavigation.removeBadge(R.id.tab_history);
        else bottomNavigation.getOrCreateBadge(R.id.tab_history).setNumber(active);
    }

    private boolean readSharedUrl(@Nullable Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())
                || !"text/plain".equals(intent.getType())) return false;
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (text == null) text = "";
        Matcher matcher = URL_PATTERN.matcher(text);
        sharedUrl = matcher.find() ? matcher.group() : text.trim();
        return true;
    }

    private void requestRuntimePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(new String[]{Manifest.permission.POST_NOTIFICATIONS});
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissionLauncher.launch(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE});
        }
    }

    private static int nightModeFor(AppTheme theme) {
        if (theme == AppTheme.LIGHT) return AppCompatDelegate.MODE_NIGHT_NO;
        if (theme == AppTheme.DARK) return AppCompatDelegate.MODE_NIGHT_YES;
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    private boolean isDarkTheme(AppTheme theme) {
        if (theme == AppTheme.DARK) return true;
        if (theme == AppTheme.LIGHT) return false;
        return (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private static void applyWindowInsets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
