package com.kira.kdownloader

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kira.kdownloader.settings.AppTheme
import com.kira.kdownloader.settings.SettingsRepository
import com.kira.kdownloader.settings.platform.LanguageManager
import com.kira.kdownloader.settings.store.SharedPreferencesKeyValueStore
import com.kira.kdownloader.settings.ui.SettingsScreen
import com.kira.kdownloader.service.DownloadEvents
import com.kira.kdownloader.settings.ui.SettingsViewModel
import com.kira.kdownloader.ui.HistoryScreen
import com.kira.kdownloader.ui.HomeScreen
import com.kira.kdownloader.ui.theme.KDownloaderTheme
import kotlinx.coroutines.flow.MutableStateFlow

private data class TabSpec(
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
)

class MainActivity : ComponentActivity() {
    private val sharedUrl = MutableStateFlow("")
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

    /** Applies the stored per-app language on API < 33 by wrapping the base context. */
    override fun attachBaseContext(newBase: Context) {
        val tag = runCatching {
            SettingsRepository(SharedPreferencesKeyValueStore(newBase)).read().appearance.languageTag
        }.getOrDefault("")
        super.attachBaseContext(LanguageManager.wrap(newBase, tag))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        readSharedUrl(intent)
        requestRuntimePermissions()

        val tabs = listOf(
            TabSpec(getString(R.string.home), Icons.Filled.Home, Icons.Outlined.Home),
            TabSpec(getString(R.string.history), Icons.Filled.History, Icons.Outlined.History),
            TabSpec(getString(R.string.settings), Icons.Filled.Settings, Icons.Outlined.Settings),
        )

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val appSettings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val appearance = appSettings.appearance

            // Apply per-app language on API 33+ (system-managed). Keyed so it runs only when the
            // language actually changes — never on every recomposition / settings write.
            LaunchedEffect(appearance.languageTag) {
                runCatching { LanguageManager.apply(this@MainActivity, appearance.languageTag) }
            }

            val incomingUrl by sharedUrl.collectAsStateWithLifecycle()
            val downloadStates by produceState(DownloadEvents.getStates().get()) {
                val liveStates = DownloadEvents.getStates().live()
                val observer = Observer<Map<String, DownloadEvents.State>> { value = it }
                liveStates.observeForever(observer)
                awaitDispose { liveStates.removeObserver(observer) }
            }
            val activeDownloads = downloadStates.count {
                it.value.phase == DownloadEvents.Phase.PREPARING ||
                    it.value.phase == DownloadEvents.Phase.RUNNING
            }
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (appearance.theme) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
                AppTheme.SYSTEM -> systemDark
            }

            fun toggleTheme() {
                settingsViewModel.setAppearance(
                    appearance.withTheme(if (darkTheme) AppTheme.LIGHT else AppTheme.DARK),
                )
            }

            KDownloaderTheme(
                darkTheme = darkTheme,
                dynamicColor = appearance.dynamicColor,
                highContrast = appearance.highContrast,
            ) {
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            tabs.forEachIndexed { index, tab ->
                                NavigationBarItem(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    icon = {
                                        val icon = @Composable {
                                            Icon(
                                                if (selectedTab == index) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label,
                                            )
                                        }
                                        // Badge the History tab while downloads are in flight.
                                        if (index == 1 && activeDownloads > 0) {
                                            BadgedBox(badge = { Badge { Text("$activeDownloads") } }) {
                                                icon()
                                            }
                                        } else {
                                            icon()
                                        }
                                    },
                                    label = { Text(tab.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                )
                            }
                        }
                    },
                ) { contentPadding ->
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                    ) {
                        AnimatedContent(
                            targetState = selectedTab,
                            // Honour "Reduce animations" (Section 8) by snapping instead of fading.
                            transitionSpec = {
                                if (appearance.reduceAnimations) {
                                    fadeIn(snap()) togetherWith fadeOut(snap())
                                } else {
                                    fadeIn() togetherWith fadeOut()
                                }
                            },
                            label = "tab-switch",
                        ) { tab ->
                            when (tab) {
                                0 -> HomeScreen(
                                    initialUrl = incomingUrl,
                                    darkTheme = darkTheme,
                                    onToggleTheme = ::toggleTheme,
                                )
                                1 -> HistoryScreen(
                                    darkTheme = darkTheme,
                                    onToggleTheme = ::toggleTheme,
                                    onReDownload = { redownloadUrl ->
                                        sharedUrl.value = redownloadUrl
                                        selectedTab = 0
                                    },
                                )
                                else -> SettingsScreen(settingsViewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readSharedUrl(intent)
    }

    private fun requestRuntimePermissions() {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        if (permissions.isNotEmpty()) permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun readSharedUrl(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        sharedUrl.value = URL_PATTERN.find(sharedText)?.value ?: sharedText.trim()
    }

    companion object {
        private val URL_PATTERN = Regex("https?://\\S+")
    }
}
