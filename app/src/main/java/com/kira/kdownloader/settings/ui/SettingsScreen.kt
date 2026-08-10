package com.kira.kdownloader.settings.ui

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
        Route.DOWNLOAD -> SectionScaffold("Download preferences", back) { DownloadSectionContent(settings, viewModel) }
        Route.STORAGE -> SectionScaffold("Storage & files", back) { StorageSectionContent(settings, viewModel) }
        Route.BEHAVIOR -> SectionScaffold("Download behavior", back) { BehaviorSectionContent(settings, viewModel) }
        Route.NETWORK -> SectionScaffold("Network", back) { NetworkSectionContent(settings, viewModel) }
        Route.SUBTITLES -> SectionScaffold("Subtitles & captions", back) { SubtitlesSectionContent(settings, viewModel) }
        Route.NOTIFICATIONS -> SectionScaffold("Notifications", back) { NotificationsSectionContent(settings, viewModel) }
        Route.APPEARANCE -> SectionScaffold("Appearance & accessibility", back) { AppearanceSectionContent(settings, viewModel) }
        Route.HISTORY -> SectionScaffold("History & privacy", back) { HistoryPrivacySectionContent(settings, viewModel) }
        Route.ADVANCED -> SectionScaffold("Advanced processing", back) { AdvancedSectionContent(settings, viewModel) }
        Route.PERMISSIONS -> SectionScaffold("Permissions & status", back) { PermissionsSectionContent(status, viewModel) }
        Route.ABOUT -> SectionScaffold("About & support", back) { AboutSectionContent() }
        Route.RESET -> SectionScaffold("Reset", back) { ResetSectionContent(viewModel) }
    }
}

@Composable
private fun SettingsHome(onNavigate: (Route) -> Unit) {
    SectionScaffold(title = "Settings", onBack = null) {
        Category("Download preferences", "Type, format, quality, metadata", Icons.Filled.VideoLibrary) { onNavigate(Route.DOWNLOAD) }
        Category("Storage & files", "Folders, filenames, temporary files", Icons.Filled.Storage) { onNavigate(Route.STORAGE) }
        Category("Download behavior", "Queue, retries, power, scheduling", Icons.Filled.PlayArrow) { onNavigate(Route.BEHAVIOR) }
        Category("Network", "Allowed networks, mobile data, proxy", Icons.Filled.Wifi) { onNavigate(Route.NETWORK) }
        Category("Subtitles & captions", "Languages, type, format, embedding", Icons.Filled.Subtitles) { onNavigate(Route.SUBTITLES) }
        Category("Notifications", "Progress, completion, actions", Icons.Filled.Notifications) { onNavigate(Route.NOTIFICATIONS) }
        Category("Appearance & accessibility", "Theme, language, list, motion", Icons.Filled.Palette) { onNavigate(Route.APPEARANCE) }
        Category("History & privacy", "Retention, clearing, export/import", Icons.Filled.History) { onNavigate(Route.HISTORY) }
        Category("Advanced processing", "Conversion, hardware, logging", Icons.Filled.Tune) { onNavigate(Route.ADVANCED) }
        Category("Permissions & status", "Notifications, battery, background", Icons.Filled.Security) { onNavigate(Route.PERMISSIONS) }
        Category("Reset", "Restore settings to defaults", Icons.Filled.RestartAlt) { onNavigate(Route.RESET) }
        Category("About & support", "Version, licenses, help", Icons.Filled.Info) { onNavigate(Route.ABOUT) }
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
