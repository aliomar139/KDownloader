package com.kira.kdownloader.engine

internal object YtDlpVersionPolicy {
    private val datePattern = Regex("(\\d{4})\\.(\\d{1,2})\\.(\\d{1,2})")

    /**
     * A stale or unreadable installed copy must be replaced. A newer self-updated copy is kept.
     */
    fun shouldInstallBundled(installedVersion: String?, bundledVersion: String): Boolean {
        if (installedVersion == bundledVersion) return false

        val installedDate = installedVersion?.let(::dateKey) ?: return true
        val bundledDate = dateKey(bundledVersion) ?: return installedVersion != bundledVersion
        return installedDate < bundledDate
    }

    private fun dateKey(version: String): Int? {
        val match = datePattern.find(version) ?: return null
        val (year, month, day) = match.destructured
        return year.toIntOrNull()?.times(10_000)
            ?.plus((month.toIntOrNull() ?: return null) * 100)
            ?.plus(day.toIntOrNull() ?: return null)
    }
}
