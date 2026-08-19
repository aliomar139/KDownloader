package com.kira.kdownloader.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kira.kdownloader.settings.AppTheme
import com.kira.kdownloader.settings.AppearanceSettings
import com.kira.kdownloader.settings.ui.SettingsScreen
import com.kira.kdownloader.settings.ui.SettingsViewModel
import com.kira.kdownloader.ui.theme.KDownloaderTheme

/** Temporary host used only while the three Compose screens are replaced one phase at a time. */
object ComposeScreenBridge {
    @JvmStatic
    fun setSettingsContent(view: ComposeView, settingsViewModel: SettingsViewModel) {
        prepare(view)
        view.setContent {
            BridgeTheme(settingsViewModel) { _, _ -> SettingsScreen(settingsViewModel) }
        }
    }

    private fun prepare(view: ComposeView) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    @Composable
    private fun BridgeTheme(
        settingsViewModel: SettingsViewModel,
        content: @Composable (AppearanceSettings, Boolean) -> Unit,
    ) {
        val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
        val appearance = settings.appearance
        val systemDark = isSystemInDarkTheme()
        val darkTheme = when (appearance.theme) {
            AppTheme.DARK -> true
            AppTheme.LIGHT -> false
            AppTheme.SYSTEM -> systemDark
        }
        KDownloaderTheme(
            darkTheme = darkTheme,
            dynamicColor = appearance.dynamicColor,
            highContrast = appearance.highContrast,
        ) {
            content(appearance, darkTheme)
        }
    }
}
