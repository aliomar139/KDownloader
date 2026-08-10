package com.kira.kdownloader.engine

import java.net.URI

internal object ExtractorOptions {
    private const val TIKTOK_HOST = "tiktok.com"
    private const val YOUTUBE_HOST = "youtube.com"
    private val installIdPattern = Regex("\\d{19}")

    fun isTikTokUrl(sourceUrl: String): Boolean {
        return hostMatches(sourceUrl, TIKTOK_HOST)
    }

    fun isYouTubeUrl(sourceUrl: String): Boolean {
        return hostMatches(sourceUrl, YOUTUBE_HOST) || hostMatches(sourceUrl, "youtu.be")
    }

    fun tikTokAppInfo(installIds: List<String>): String {
        require(installIds.isNotEmpty()) { "At least one TikTok install ID is required" }
        require(installIds.all(installIdPattern::matches)) {
            "TikTok install IDs must contain exactly 19 digits"
        }
        return "tiktok:app_info=${installIds.joinToString(",")}" 
    }

    private fun hostMatches(sourceUrl: String, expectedHost: String): Boolean {
        val host = runCatching { URI(sourceUrl.trim()).host }
            .getOrNull()
            ?.lowercase()
            ?: return false
        return host == expectedHost || host.endsWith(".$expectedHost")
    }
}
