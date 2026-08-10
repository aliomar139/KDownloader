package com.kira.kdownloader.util

import android.content.Context

/**
 * Small persistent list of recently fetched URLs, surfaced as quick-pick chips on the Home idle
 * screen. Backed by SharedPreferences; most-recent first, de-duplicated, capped at [MAX].
 */
object RecentUrls {
    private const val PREFS = "recent_urls"
    private const val KEY = "urls"
    private const val SEP = "\n"
    private const val MAX = 8

    fun all(context: Context): List<String> =
        prefs(context).getString(KEY, "").orEmpty()
            .split(SEP)
            .filter(String::isNotBlank)

    fun add(context: Context, url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        val updated = (listOf(trimmed) + all(context).filter { it != trimmed }).take(MAX)
        prefs(context).edit().putString(KEY, updated.joinToString(SEP)).apply()
    }

    fun remove(context: Context, url: String) {
        val updated = all(context).filter { it != url }
        prefs(context).edit().putString(KEY, updated.joinToString(SEP)).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
