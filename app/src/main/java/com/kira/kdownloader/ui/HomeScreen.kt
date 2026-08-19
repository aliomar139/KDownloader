package com.kira.kdownloader.ui

import androidx.compose.ui.res.stringResource
import com.kira.kdownloader.R
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.kira.kdownloader.engine.DownloadChoice
import com.kira.kdownloader.engine.FormatSelector
import com.kira.kdownloader.service.DownloadEvents
import com.kira.kdownloader.service.DownloadService
import com.kira.kdownloader.util.RecentUrls
import com.kira.kdownloader.util.UrlExtractor
import com.kira.kdownloader.util.formatBytes
import com.kira.kdownloader.util.formatDuration
import com.kira.kdownloader.util.formatEta
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    initialUrl: String,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val haptics = LocalHapticFeedback.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val downloadStates by DownloadEvents.states.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDownloadsSheet by remember { mutableStateOf(false) }
    var url by rememberSaveable { mutableStateOf(initialUrl) }

    val activeDownloads = remember(downloadStates) {
        downloadStates.values.filter {
            it.phase == DownloadEvents.Phase.PREPARING || it.phase == DownloadEvents.Phase.RUNNING
        }
    }
    // Close the downloads sheet automatically once nothing is running.
    LaunchedEffect(activeDownloads.isEmpty()) {
        if (activeDownloads.isEmpty()) showDownloadsSheet = false
    }

    LaunchedEffect(initialUrl) {
        if (initialUrl.isNotBlank()) url = initialUrl
    }

    // Prepare the engine while the user is still pasting/typing, so the first fetch is quicker.
    LaunchedEffect(Unit) { viewModel.warmUp() }

    fun fetch(target: String = url) {
        keyboard?.hide()
        url = target
        viewModel.fetch(target)
    }

    // Recent URLs for the idle screen; recomputed whenever the screen state changes.
    val recentUrls = remember(state) { RecentUrls.all(context) }

    // Offer to paste a link on the clipboard. Kept in sync with the *latest* copied link: refreshed
    // whenever the clipboard changes while we're foreground, and whenever the screen regains focus
    // (covers copying a link in another app and switching back).
    var clipboardSuggestion by remember { mutableStateOf<String?>(null) }
    var dismissedLink by rememberSaveable { mutableStateOf<String?>(null) }
    fun refreshClipboardSuggestion() {
        val candidate = clipboard.getText()?.text?.let(UrlExtractor::fromText)
        clipboardSuggestion = candidate?.takeIf { it.startsWith("http", ignoreCase = true) }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshClipboardSuggestion() }
    DisposableEffect(Unit) {
        val manager = context.getSystemService(ClipboardManager::class.java)
        val listener = ClipboardManager.OnPrimaryClipChangedListener { refreshClipboardSuggestion() }
        manager?.addPrimaryClipChangedListener(listener)
        refreshClipboardSuggestion()
        onDispose { manager?.removePrimaryClipChangedListener(listener) }
    }

    // Success -> transient snackbar; failure -> dialog.
    val failure = downloadStates.entries.firstOrNull {
        it.value.phase == DownloadEvents.Phase.FAILED
    }
    val successEntry = downloadStates.entries.firstOrNull {
        it.value.phase == DownloadEvents.Phase.COMPLETED
    }
    LaunchedEffect(successEntry?.key) {
        val entry = successEntry ?: return@LaunchedEffect
        val done = entry.value
        val result = snackbarHostState.showSnackbar(
            message = "Saved · ${done.title}",
            actionLabel = if (done.fileUri != null) "Open" else null,
            withDismissAction = true,
        )
        if (result == SnackbarResult.ActionPerformed && done.fileUri != null) {
            openMedia(context, done.fileUri, done.kind)
        }
        DownloadEvents.clear(entry.key)
    }
    if (failure != null) {
        DownloadFinishedDialog(
            state = failure.value,
            onDismiss = { DownloadEvents.clear(failure.key) },
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(20.dp))
            BrandHeader(darkTheme = darkTheme, onToggleTheme = onToggleTheme)
            Spacer(Modifier.height(20.dp))
            UrlInputCard(
                url = url,
                onUrlChange = { url = it },
                onPaste = {
                    clipboard.getText()?.text?.takeIf(String::isNotBlank)?.let { url = it }
                },
                onClear = { url = "" },
                onFetch = { fetch() },
                isLoading = state is HomeUiState.Loading,
            )
            if (activeDownloads.isNotEmpty()) {
                ActiveDownloadsButton(
                    count = activeDownloads.size,
                    onClick = { showDownloadsSheet = true },
                )
            }
            val suggestion = clipboardSuggestion
            // Show whenever the clipboard holds a valid link that isn't already in the field and
            // wasn't the one just dismissed. A newly copied link re-shows even after a dismiss.
            AnimatedVisibility(
                visible = suggestion != null && suggestion != url && suggestion != dismissedLink,
            ) {
                if (suggestion != null) {
                    ClipboardSuggestionBanner(
                        url = suggestion,
                        onPaste = { fetch(suggestion) },
                        onDismiss = { dismissedLink = suggestion },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "home-state",
                modifier = Modifier.weight(1f),
            ) { currentState ->
                when (currentState) {
                    HomeUiState.Idle -> IdleState(
                        recentUrls = recentUrls,
                        onPick = { fetch(it) },
                    )
                    HomeUiState.Loading -> LoadingState()
                    is HomeUiState.Error -> ErrorState(
                        message = currentState.message,
                        onRetry = { fetch() },
                    )

                    is HomeUiState.Loaded -> ResultList(
                        state = currentState,
                        downloadStates = downloadStates,
                        onDownload = { choice ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            val key = DownloadEvents.keyOf(currentState.sourceUrl, choice.label)
                            val processId = "dl-${key.hashCode()}-${System.currentTimeMillis()}"
                            DownloadEvents.update(
                                key,
                                DownloadEvents.State(
                                    phase = DownloadEvents.Phase.PREPARING,
                                    title = currentState.info.title,
                                    kind = choice.kind.name,
                                    processId = processId,
                                ),
                            )
                            DownloadService.start(
                                context = context,
                                url = currentState.sourceUrl,
                                choice = choice,
                                title = currentState.info.title,
                                thumbnailUrl = currentState.info.thumbnailUrl,
                                processId = processId,
                            )
                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.download_started)) }
                        },
                        onCancel = { processId -> DownloadService.cancel(context, processId) },
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        if (showDownloadsSheet) {
            ActiveDownloadsSheet(
                downloads = activeDownloads,
                onCancel = { processId -> DownloadService.cancel(context, processId) },
                onDismiss = { showDownloadsSheet = false },
            )
        }
    }
}

@Composable
private fun ActiveDownloadsButton(count: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                if (count == 1) "1 download in progress" else "$count downloads in progress",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(R.string.view), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveDownloadsSheet(
    downloads: List<DownloadEvents.State>,
    onCancel: (processId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.downloads), style = MaterialTheme.typography.titleMedium)
            if (downloads.isEmpty()) {
                Text(
                    "No active downloads.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            downloads.forEach { download ->
                ActiveDownloadRow(download = download, onCancel = onCancel)
            }
        }
    }
}

@Composable
private fun ActiveDownloadRow(
    download: DownloadEvents.State,
    onCancel: (processId: String) -> Unit,
) {
    val percent = download.percent
    val preparing = download.phase == DownloadEvents.Phase.PREPARING || percent < 0
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    download.title.ifBlank { "Preparing…" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val eta = formatEta(download.etaSeconds)
                val status = when {
                    preparing -> "Preparing…"
                    eta.isEmpty() -> "$percent%"
                    else -> "$percent% · $eta left"
                }
                Text(
                    status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val processId = download.processId
            IconButton(
                onClick = { processId?.let(onCancel) },
                enabled = processId != null,
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Cancel ${download.title}",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        if (preparing) {
            LinearProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small),
            )
        } else {
            LinearProgressIndicator(
                progress = { percent.coerceIn(0, 100) / 100f },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small),
            )
        }
    }
}

@Composable
private fun ClipboardSuggestionBanner(
    url: String,
    onPaste: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
        ) {
            Icon(
                Icons.Outlined.ContentPaste,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Link on clipboard",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onPaste) { Text(stringResource(R.string.use)) }
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.dismiss),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun DownloadFinishedDialog(
    state: DownloadEvents.State,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val succeeded = state.phase == DownloadEvents.Phase.COMPLETED
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (succeeded) Icons.Default.CheckCircle else Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = if (succeeded) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        },
        title = { Text(if (succeeded) "Download complete" else "Download failed") },
        text = {
            Column {
                Text(
                    state.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (succeeded) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Saved to Download/KDownloader",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (state.message != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        confirmButton = {
            if (succeeded && state.fileUri != null) {
                TextButton(
                    onClick = {
                        openMedia(context, state.fileUri, state.kind)
                        onDismiss()
                    },
                ) { Text(stringResource(R.string.open)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
            }
        },
        dismissButton = {
            if (succeeded && state.fileUri != null) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
            }
        },
    )
}

private fun openMedia(context: Context, fileUri: String, kind: String) {
    val mimeType = if (kind == "AUDIO") "audio/*" else "video/*"
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(fileUri), mimeType)
        // No FLAG_GRANT_READ_URI_PERMISSION: these are public MediaStore URIs the app may not own,
        // and granting a permission we don't hold throws SecurityException at startActivity.
    }
    try {
        context.startActivity(Intent.createChooser(viewIntent, "Open with"))
    } catch (error: Throwable) {
        Log.w("HomeScreen", "Could not open media", error)
        Toast.makeText(context, context.getString(R.string.couldn_t_open_this_file), Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun BrandHeader(darkTheme: Boolean, onToggleTheme: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(
                Icons.Default.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .padding(10.dp)
                    .size(26.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.kdownloader), style = MaterialTheme.typography.headlineSmall)
            Text(
                "Save video & audio from any link",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ThemeToggleButton(darkTheme = darkTheme, onToggleTheme = onToggleTheme)
    }
}

@Composable
fun ThemeToggleButton(darkTheme: Boolean, onToggleTheme: () -> Unit) {
    IconButton(
        onClick = onToggleTheme,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Icon(
            if (darkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
            contentDescription = if (darkTheme) "Switch to light theme" else "Switch to dark theme",
        )
    }
}

@Composable
private fun UrlInputCard(
    url: String,
    onUrlChange: (String) -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    onFetch: () -> Unit,
    isLoading: Boolean,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                placeholder = { Text("https://…") },
                label = { Text(stringResource(R.string.video_or_audio_url)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = { onFetch() }),
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (url.isBlank()) {
                        IconButton(onClick = onPaste) {
                            Icon(
                                Icons.Outlined.ContentPaste,
                                contentDescription = stringResource(R.string.paste_from_clipboard),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        IconButton(onClick = onClear) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.clear_url),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onFetch,
                enabled = !isLoading && url.isNotBlank(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Fetching…")
                } else {
                    Icon(Icons.Default.Download, contentDescription = null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.get_formats))
                }
            }
        }
    }
}

@Composable
private fun IdleState(
    recentUrls: List<String>,
    onPick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Icon(
                        Icons.Outlined.SmartDisplay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(20.dp)
                            .size(40.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.ready_when_you_are), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Paste a link above, or share one to\nKDownloader from any app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (recentUrls.isNotEmpty()) {
            Text(
                "Recent links",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(recentUrls, key = { it }) { recent ->
                    val host = remember(recent) {
                        runCatching { Uri.parse(recent).host?.removePrefix("www.") }.getOrNull()
                            ?: recent
                    }
                    AssistChip(
                        onClick = { onPick(recent) },
                        label = {
                            Text(host, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Link,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SkeletonBox(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.large),
        )
        repeat(4) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(12.dp),
            ) {
                SkeletonBox(Modifier.size(40.dp).clip(MaterialTheme.shapes.small))
                Spacer(Modifier.width(14.dp))
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SkeletonBox(
                        Modifier
                            .fillMaxWidth(0.5f)
                            .height(14.dp)
                            .clip(MaterialTheme.shapes.small),
                    )
                    SkeletonBox(
                        Modifier
                            .fillMaxWidth(0.8f)
                            .height(12.dp)
                            .clip(MaterialTheme.shapes.small),
                    )
                }
                Spacer(Modifier.width(10.dp))
                SkeletonBox(Modifier.size(44.dp).clip(MaterialTheme.shapes.small))
            }
        }
    }
}

@Composable
private fun SkeletonBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "skeleton-alpha",
    )
    Box(modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.15f)))
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Couldn't fetch this link",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "This can happen with private or region-locked videos, an expired or mistyped " +
                    "link, or a site that isn't supported.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onRetry,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.try_again), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
private fun ResultList(
    state: HomeUiState.Loaded,
    downloadStates: Map<String, DownloadEvents.State>,
    onDownload: (DownloadChoice) -> Unit,
    onCancel: (processId: String) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "media-card") {
            MediaCard(
                title = state.info.title,
                thumbnailUrl = state.info.thumbnailUrl,
                sourceUrl = state.sourceUrl,
                uploader = state.info.uploader,
                durationSeconds = state.info.durationSeconds,
            )
        }
        item(key = "formats-header") {
            Text(
                "Available formats",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            )
        }
        val recommended = state.info.choices.firstOrNull { it.kind == FormatSelector.Kind.VIDEO }
        items(state.info.choices, key = { it.label }) { choice ->
            FormatRow(
                choice = choice,
                downloadState = downloadStates[DownloadEvents.keyOf(state.sourceUrl, choice.label)],
                isRecommended = choice == recommended,
                onDownload = { onDownload(choice) },
                onCancel = onCancel,
            )
        }
    }
}

@Composable
private fun MediaCard(
    title: String,
    thumbnailUrl: String?,
    sourceUrl: String,
    uploader: String?,
    durationSeconds: Int,
) {
    val host = remember(sourceUrl) {
        runCatching { Uri.parse(sourceUrl).host?.removePrefix("www.") }.getOrNull()
    }
    val meta = remember(host, uploader, durationSeconds) {
        listOfNotNull(
            host,
            uploader?.takeIf { it.isNotBlank() },
            formatDuration(durationSeconds).takeIf { it.isNotEmpty() },
        ).joinToString(" · ")
    }
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column {
            if (thumbnailUrl != null) {
                SubcomposeAsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "Thumbnail for $title",
                    contentScale = ContentScale.Crop,
                    loading = { ThumbnailPlaceholder() },
                    error = { ThumbnailPlaceholder() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                )
            }
            Column(Modifier.padding(16.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (meta.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThumbnailPlaceholder() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Icon(
            Icons.Outlined.SmartDisplay,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
private fun FormatRow(
    choice: DownloadChoice,
    downloadState: DownloadEvents.State?,
    isRecommended: Boolean,
    onDownload: () -> Unit,
    onCancel: (processId: String) -> Unit,
) {
    val isAudio = choice.kind == FormatSelector.Kind.AUDIO
    val isActive = downloadState?.phase == DownloadEvents.Phase.PREPARING ||
        downloadState?.phase == DownloadEvents.Phase.RUNNING
    Card(
        onClick = onDownload,
        enabled = !isActive,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (isAudio) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer
                    },
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(
                        if (isAudio) Icons.Default.MusicNote else Icons.Default.Videocam,
                        contentDescription = null,
                        tint = if (isAudio) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        },
                        modifier = Modifier
                            .padding(9.dp)
                            .size(22.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(choice.label, style = MaterialTheme.typography.titleSmall)
                        if (isRecommended) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                shape = RoundedCornerShape(50),
                            ) {
                                Text(
                                    "Best",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                    val base = if (isAudio) "MP3 · audio only" else "MP4 · video + audio"
                    val size = formatBytes(choice.approxBytes)
                    Text(
                        if (size.isEmpty()) base else "$base · ~$size",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isActive) {
                    Text(
                        when {
                            downloadState?.phase == DownloadEvents.Phase.PREPARING -> "Preparing…"
                            (downloadState?.percent ?: -1) < 0 -> "Working…"
                            else -> "${downloadState?.percent}%"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(10.dp))
                    val processId = downloadState?.processId
                    FilledIconButton(
                        onClick = { processId?.let(onCancel) },
                        enabled = processId != null,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Cancel download of ${choice.label}",
                            modifier = Modifier.size(22.dp),
                        )
                    }
                } else {
                    FilledIconButton(
                        onClick = onDownload,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Download ${choice.label}",
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            AnimatedVisibility(visible = isActive) {
                val percent = downloadState?.percent ?: -1
                val progressModifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp)
                    .clip(MaterialTheme.shapes.small)
                if (downloadState?.phase == DownloadEvents.Phase.PREPARING || percent < 0) {
                    LinearProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = progressModifier,
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { percent.coerceIn(0, 100) / 100f },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = progressModifier,
                    )
                }
            }
        }
    }
}
