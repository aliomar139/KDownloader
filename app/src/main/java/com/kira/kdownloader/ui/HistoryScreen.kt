package com.kira.kdownloader.ui

import androidx.compose.ui.res.stringResource
import com.kira.kdownloader.R
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.kira.kdownloader.data.AppDatabase
import com.kira.kdownloader.data.DownloadEntity
import com.kira.kdownloader.data.DownloadStatus
import com.kira.kdownloader.util.DownloadDirectoryScanner
import com.kira.kdownloader.util.MediaThumbnails
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class HistoryFilter { ALL, VIDEO, AUDIO }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onReDownload: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val dao = remember(context) { AppDatabase.get(context).downloadDao() }
    val historyFlow = remember(dao) { dao.observeAll() }
    val history by historyFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<DownloadEntity?>(null) }
    var detailItem by remember { mutableStateOf<DownloadEntity?>(null) }
    var showClearAll by remember { mutableStateOf(false) }
    var scanGeneration by remember { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(HistoryFilter.ALL) }
    var newestFirst by rememberSaveable { mutableStateOf(true) }

    // observeAll() already returns newest-first; filtering/sorting is applied for display only.
    val visible = remember(history, query, filter, newestFirst) {
        history
            .filter { entry ->
                when (filter) {
                    HistoryFilter.ALL -> true
                    HistoryFilter.VIDEO -> !entry.kind.equals("AUDIO", ignoreCase = true)
                    HistoryFilter.AUDIO -> entry.kind.equals("AUDIO", ignoreCase = true)
                }
            }
            .filter { entry ->
                query.isBlank() ||
                    entry.title.contains(query, ignoreCase = true) ||
                    entry.sourceUrl.contains(query, ignoreCase = true)
            }
            .let { if (newestFirst) it else it.reversed() }
    }
    val mediaPermissions = remember { historyMediaPermissions() }
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        scanGeneration++
    }

    LaunchedEffect(context, dao, scanGeneration) {
        // Force a fresh scan when re-triggered by a permission grant; otherwise let it throttle.
        withContext(Dispatchers.IO) {
            DownloadDirectoryScanner.syncIntoHistory(context, dao, scanGeneration > 0)
        }
    }
    LaunchedEffect(context, mediaPermissions) {
        val missingPermissions = mediaPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) !=
                PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            mediaPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        scope.launch {
            withContext(Dispatchers.IO) {
                DownloadDirectoryScanner.syncIntoHistory(context, dao)
            }
        }
    }

    if (showClearAll) {
        AlertDialog(
            onDismissRequest = { showClearAll = false },
            icon = {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text(stringResource(R.string.clear_all_history_cf155c)) },
            text = {
                Text(
                    "This removes every entry from your history. The downloaded files themselves " +
                        "are not deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearAll = false
                        scope.launch { dao.clearAll() }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text(stringResource(R.string.clear_all)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAll = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    detailItem?.let { download ->
        DownloadDetailSheet(
            download = download,
            formattedDate = dateFormat.format(Date(download.createdAt)),
            onOpen = {
                detailItem = null
                openDownload(context, download)
            },
            onShare = {
                detailItem = null
                shareDownload(context, download)
            },
            onReDownload = {
                detailItem = null
                onReDownload(download.sourceUrl)
            },
            onDelete = {
                detailItem = null
                pendingDelete = download
            },
            onDismiss = { detailItem = null },
        )
    }

    pendingDelete?.let { download ->
        DeleteDownloadDialog(
            download = download,
            onDismiss = { pendingDelete = null },
            onConfirm = { deleteFile ->
                scope.launch {
                    if (deleteFile && download.fileUri != null) {
                        withContext(Dispatchers.IO) {
                            runCatching {
                                context.contentResolver.delete(
                                    Uri.parse(download.fileUri),
                                    null,
                                    null,
                                )
                            }
                        }
                    }
                    dao.deleteById(download.id)
                    pendingDelete = null
                }
            },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.history), style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (history.isEmpty()) {
                        "No downloads yet"
                    } else {
                        "${history.size} download${if (history.size == 1) "" else "s"}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ThemeToggleButton(darkTheme = darkTheme, onToggleTheme = onToggleTheme)
            if (history.isNotEmpty()) {
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.more_options),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.clear_all_history)) },
                            onClick = {
                                menuOpen = false
                                showClearAll = true
                            },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (history.isEmpty()) {
            EmptyHistory()
        } else {
            HistoryToolbar(
                query = query,
                onQueryChange = { query = it },
                filter = filter,
                onFilterChange = { filter = it },
                newestFirst = newestFirst,
                onToggleSort = { newestFirst = !newestFirst },
            )
            Spacer(Modifier.height(12.dp))
            if (visible.isEmpty()) {
                NoMatches()
            } else {
                val grouped = remember(visible) { visible.groupBy { dateBucket(it.createdAt) } }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    grouped.forEach { (label, entries) ->
                        stickyHeader(key = "header-$label") { DateHeader(label) }
                        items(entries, key = { it.id }) { download ->
                            val formattedDate = remember(download.id, download.createdAt) {
                                dateFormat.format(Date(download.createdAt))
                            }
                            val dismissState = rememberSwipeToDismissBoxState(
                                // Trigger the delete dialog on swipe, but keep the row (return
                                // false) so its position resets if the user cancels.
                                confirmValueChange = { target ->
                                    if (target != SwipeToDismissBoxValue.Settled) {
                                        pendingDelete = download
                                    }
                                    false
                                },
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = { SwipeDeleteBackground() },
                            ) {
                                HistoryCard(
                                    download = download,
                                    formattedDate = formattedDate,
                                    onOpen = { openDownload(context, download) },
                                    onLongPress = { detailItem = download },
                                    onDelete = { pendingDelete = download },
                                    onShare = { shareDownload(context, download) },
                                    onReDownload = { onReDownload(download.sourceUrl) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    filter: HistoryFilter,
    onFilterChange: (HistoryFilter) -> Unit,
    newestFirst: Boolean,
    onToggleSort: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_downloads)) },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null)
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.clear_search))
                    }
                }
            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FilterChip(
                selected = filter == HistoryFilter.ALL,
                onClick = { onFilterChange(HistoryFilter.ALL) },
                label = { Text(stringResource(R.string.all)) },
            )
            FilterChip(
                selected = filter == HistoryFilter.VIDEO,
                onClick = { onFilterChange(HistoryFilter.VIDEO) },
                label = { Text(stringResource(R.string.video)) },
            )
            FilterChip(
                selected = filter == HistoryFilter.AUDIO,
                onClick = { onFilterChange(HistoryFilter.AUDIO) },
                label = { Text(stringResource(R.string.audio)) },
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onToggleSort) {
                Icon(
                    Icons.Default.SwapVert,
                    contentDescription = if (newestFirst) "Sort oldest first" else "Sort newest first",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SwipeDeleteBackground() {
    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 20.dp),
    ) {
        Icon(
            Icons.Outlined.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun NoMatches() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "No downloads match your search.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 48.dp),
        )
    }
}

private fun shareDownload(context: Context, download: DownloadEntity) {
    val fileUri = download.fileUri ?: return
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = if (download.kind == "AUDIO") "audio/*" else "video/*"
        putExtra(Intent.EXTRA_STREAM, Uri.parse(fileUri))
    }
    try {
        context.startActivity(Intent.createChooser(shareIntent, "Share"))
    } catch (error: Throwable) {
        Log.w("HistoryScreen", "Could not share ${download.title}", error)
        Toast.makeText(context, context.getString(R.string.no_app_available_to_share_this_file), Toast.LENGTH_SHORT).show()
    }
}

private fun historyMediaPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_MEDIA_VIDEO,
    )

    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

@Composable
private fun DeleteDownloadDialog(
    download: DownloadEntity,
    onDismiss: () -> Unit,
    onConfirm: (deleteFile: Boolean) -> Unit,
) {
    val fileAvailable = download.status == DownloadStatus.COMPLETED && download.fileUri != null
    var deleteFile by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(stringResource(R.string.delete_download)) },
        text = {
            Column {
                Text(
                    download.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (fileAvailable) {
                        "This removes the entry from your history."
                    } else {
                        "No saved file is linked to this entry; only the history record will be removed."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (fileAvailable) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = deleteFile,
                            onCheckedChange = { deleteFile = it },
                        )
                        Text(
                            "Also delete the file from storage",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(deleteFile && fileAvailable) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun openDownload(context: Context, download: DownloadEntity) {
    val fileUri = download.fileUri
    if (fileUri.isNullOrBlank()) {
        Toast.makeText(context, context.getString(R.string.this_download_has_no_saved_file), Toast.LENGTH_SHORT).show()
        return
    }
    val mimeType = if (download.kind == "AUDIO") "audio/*" else "video/*"
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(fileUri), mimeType)
        // NB: no FLAG_GRANT_READ_URI_PERMISSION. These are public MediaStore URIs we may not own;
        // trying to grant a permission we don't hold throws SecurityException at startActivity.
        // Media players read public content:// media through their own storage/media permission.
    }
    // Catch every failure mode (ActivityNotFound, SecurityException, OEM-specific, etc.) so a
    // failure to hand off to a player can never crash the app.
    try {
        context.startActivity(Intent.createChooser(viewIntent, "Open with"))
    } catch (error: Throwable) {
        Log.w("HistoryScreen", "Could not open ${download.title}", error)
        Toast.makeText(context, context.getString(R.string.couldn_t_open_this_file), Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun EmptyHistory() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 48.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(24.dp),
            ) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(20.dp)
                        .size(40.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.nothing_here_yet), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Files you download will show up here\nand in Download/KDownloader.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryCard(
    download: DownloadEntity,
    formattedDate: String,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onReDownload: () -> Unit,
) {
    val canOpen = download.status == DownloadStatus.COMPLETED && download.fileUri != null
    val canRedownload = download.status == DownloadStatus.FAILED && download.sourceUrl.isNotBlank()
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                // Tap plays; long-press opens the detail sheet for more actions.
                .combinedClickable(
                    onClick = { if (canOpen) onOpen() },
                    onLongClick = onLongPress,
                )
                .padding(12.dp),
        ) {
            HistoryThumbnail(download)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    download.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(download.status)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${download.formatLabel} · $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            if (canRedownload) {
                IconButton(onClick = onReDownload) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Retry download of ${download.title}",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (canOpen) {
                IconButton(onClick = onShare) {
                    Icon(
                        Icons.Outlined.Share,
                        contentDescription = "Share ${download.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete ${download.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HistoryThumbnail(download: DownloadEntity) {
    val isAudio = download.kind == "AUDIO"
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        when {
            // In-app downloads keep the source's remote thumbnail.
            download.thumbnailUrl != null -> SubcomposeAsyncImage(
                model = download.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = { HistoryThumbFallback(isAudio) },
                error = { HistoryThumbFallback(isAudio) },
                modifier = Modifier.fillMaxSize(),
            )
            // Files found on the device: extract a thumbnail from the media file itself.
            download.fileUri != null -> LocalMediaThumbnail(download.fileUri, isAudio)
            else -> HistoryThumbFallback(isAudio)
        }
    }
}

@Composable
private fun LocalMediaThumbnail(fileUri: String, isAudio: Boolean) {
    val context = LocalContext.current
    // Seed with any already-cached bitmap so revisiting this screen paints instantly (no flicker).
    val bitmap by produceState<Bitmap?>(initialValue = MediaThumbnails.peek(fileUri), fileUri) {
        if (value == null) value = withContext(Dispatchers.IO) {
            MediaThumbnails.load(context, fileUri, isAudio)
        }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        HistoryThumbFallback(isAudio)
    }
}

@Composable
private fun HistoryThumbFallback(isAudio: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Icon(
            if (isAudio) Icons.Default.MusicNote else Icons.Default.Videocam,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun StatusBadge(status: DownloadStatus) {
    val (label, container, content) = when (status) {
        DownloadStatus.COMPLETED -> Triple(
            "Done",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )

        DownloadStatus.RUNNING -> Triple(
            "Downloading",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )

        DownloadStatus.FAILED -> Triple(
            "Failed",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
    }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            when (status) {
                DownloadStatus.COMPLETED -> Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                )

                DownloadStatus.RUNNING -> CircularProgressIndicator(
                    strokeWidth = 1.5.dp,
                    color = content,
                    trackColor = Color.Transparent,
                    modifier = Modifier.size(11.dp),
                )

                DownloadStatus.FAILED -> Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun DateHeader(label: String) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadDetailSheet(
    download: DownloadEntity,
    formattedDate: String,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onReDownload: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val canOpen = download.status == DownloadStatus.COMPLETED && download.fileUri != null
    val canRedownload = download.status == DownloadStatus.FAILED && download.sourceUrl.isNotBlank()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HistoryThumbnail(download)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        download.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusBadge(download.status)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${download.formatLabel} · $formattedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (canOpen) {
                DetailAction(Icons.Default.PlayArrow, "Play", onClick = onOpen)
                DetailAction(Icons.Outlined.Share, "Share", onClick = onShare)
            }
            if (canRedownload) {
                DetailAction(Icons.Outlined.Refresh, "Download again", onClick = onReDownload)
            }
            DetailAction(Icons.Outlined.Delete, "Delete", destructive = true, onClick = onDelete)
        }
    }
}

@Composable
private fun DetailAction(
    icon: ImageVector,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val color = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(Modifier.width(16.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, color = color)
        }
    }
}

/** Buckets a timestamp into Today / Yesterday / Earlier this week / Earlier. */
private fun dateBucket(createdAt: Long): String {
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val dayMs = 86_400_000L
    return when {
        createdAt >= startOfToday -> "Today"
        createdAt >= startOfToday - dayMs -> "Yesterday"
        createdAt >= startOfToday - 7 * dayMs -> "Earlier this week"
        else -> "Earlier"
    }
}
