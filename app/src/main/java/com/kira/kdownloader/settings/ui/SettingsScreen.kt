package com.kira.kdownloader.settings.ui

import androidx.compose.ui.res.stringResource
import com.kira.kdownloader.R
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kira.kdownloader.settings.ui.components.ClickablePreference

private enum class Route {
    HOME, DOWNLOAD, STORAGE, BEHAVIOR, NETWORK, SUBTITLES,
    NOTIFICATIONS, APPEARANCE, HISTORY, ADVANCED, PERMISSIONS, ABOUT, RESET,
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    var route by rememberSaveable { mutableStateOf(Route.HOME) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val status by viewModel.systemStatus.collectAsStateWithLifecycle()

    // Keep the permissions/status view fresh whenever the user returns to Settings.
    LaunchedEffect(route) { viewModel.refreshStatus() }

    BackHandler(enabled = route != Route.HOME) { route = Route.HOME }

    val back = { route = Route.HOME }
    when (route) {
        Route.HOME -> SettingsHome(onNavigate = { route = it })
        Route.DOWNLOAD -> SectionScaffold(stringResource(R.string.download_preferences), back) { DownloadSectionContent(settings, viewModel) }
        Route.STORAGE -> SectionScaffold(stringResource(R.string.storage_and_files), back) { StorageSectionContent(settings, viewModel) }
        Route.BEHAVIOR -> SectionScaffold(stringResource(R.string.download_behavior), back) { BehaviorSectionContent(settings, viewModel) }
        Route.NETWORK -> SectionScaffold(stringResource(R.string.network), back) { NetworkSectionContent(settings, viewModel) }
        Route.SUBTITLES -> SectionScaffold(stringResource(R.string.subtitles_and_captions), back) { SubtitlesSectionContent(settings, viewModel) }
        Route.NOTIFICATIONS -> SectionScaffold(stringResource(R.string.notifications), back) { NotificationsSectionContent(settings, viewModel) }
        Route.APPEARANCE -> SectionScaffold(stringResource(R.string.appearance_and_accessibility), back) { AppearanceSectionContent(settings, viewModel) }
        Route.HISTORY -> SectionScaffold(stringResource(R.string.history_and_privacy), back) { HistoryPrivacySectionContent(settings, viewModel) }
        Route.ADVANCED -> SectionScaffold(stringResource(R.string.advanced_processing), back) { AdvancedSectionContent(settings, viewModel) }
        Route.PERMISSIONS -> SectionScaffold(stringResource(R.string.permissions_and_status), back) { PermissionsSectionContent(status, viewModel) }
        Route.ABOUT -> SectionScaffold(stringResource(R.string.about_and_support), back) { AboutSectionContent() }
        Route.RESET -> SectionScaffold(stringResource(R.string.reset), back) { ResetSectionContent(viewModel) }
    }
}

@Composable
private fun SettingsHome(onNavigate: (Route) -> Unit) {
    SectionScaffold(title = stringResource(R.string.settings), onBack = null) {
        Category(stringResource(R.string.download_preferences), stringResource(R.string.type_format_quality_metadata), Icons.Filled.VideoLibrary) { onNavigate(Route.DOWNLOAD) }
        Category(stringResource(R.string.storage_and_files), stringResource(R.string.folders_filenames_temporary_files), Icons.Filled.Storage) { onNavigate(Route.STORAGE) }
        Category(stringResource(R.string.download_behavior), stringResource(R.string.queue_retries_power_scheduling), Icons.Filled.PlayArrow) { onNavigate(Route.BEHAVIOR) }
        Category(stringResource(R.string.network), stringResource(R.string.allowed_networks_mobile_data_proxy), Icons.Filled.Wifi) { onNavigate(Route.NETWORK) }
        Category(stringResource(R.string.subtitles_and_captions), stringResource(R.string.languages_type_format_embedding), Icons.Filled.Subtitles) { onNavigate(Route.SUBTITLES) }
        Category(stringResource(R.string.notifications), stringResource(R.string.progress_completion_actions), Icons.Filled.Notifications) { onNavigate(Route.NOTIFICATIONS) }
        Category(stringResource(R.string.appearance_and_accessibility), stringResource(R.string.theme_language_list_motion), Icons.Filled.Palette) { onNavigate(Route.APPEARANCE) }
        Category(stringResource(R.string.history_and_privacy), stringResource(R.string.retention_clearing_export_import), Icons.Filled.History) { onNavigate(Route.HISTORY) }
        Category(stringResource(R.string.advanced_processing), stringResource(R.string.conversion_hardware_logging), Icons.Filled.Tune) { onNavigate(Route.ADVANCED) }
        Category(stringResource(R.string.permissions_and_status), stringResource(R.string.notifications_battery_background), Icons.Filled.Security) { onNavigate(Route.PERMISSIONS) }
        Category(stringResource(R.string.reset), stringResource(R.string.restore_settings_to_defaults), Icons.Filled.RestartAlt) { onNavigate(Route.RESET) }
        Category(stringResource(R.string.about_and_support), stringResource(R.string.version_licenses_help), Icons.Filled.Info) { onNavigate(Route.ABOUT) }
    }
}

@Composable
private fun Category(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    ClickablePreference(title = title, subtitle = subtitle, leadingIcon = icon, onClick = onClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionScaffold(
    title: String,
    onBack: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            content()
        }
    }
}
