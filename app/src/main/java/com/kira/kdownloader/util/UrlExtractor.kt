package com.kira.kdownloader.util

object UrlExtractor {
    private val urlPattern = Regex("https?://\\S+", RegexOption.IGNORE_CASE)

    fun fromText(text: String): String = urlPattern
        .find(text)
        ?.value
        ?.trimEnd('.', ',', ';', ':', ')', ']', '}')
        ?: text.trim()
}
