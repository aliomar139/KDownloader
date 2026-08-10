package com.kira.kdownloader.util

import java.util.Locale

/** Human-readable byte size (e.g. 12.3 MB). Returns "" for unknown/zero sizes. */
fun formatBytes(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return ""
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "$bytes B" else String.format(Locale.US, "%.1f %s", value, units[unit])
}

/** Clock-style duration (e.g. 3:45 or 1:02:03). Returns "" for unknown/zero. */
fun formatDuration(totalSeconds: Int): String {
    if (totalSeconds <= 0) return ""
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

/** Compact "time remaining" label from seconds (e.g. 45s, 3m 20s, 1h 5m). Returns "" if unknown. */
fun formatEta(seconds: Long): String {
    if (seconds < 0) return ""
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}
