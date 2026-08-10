package com.kira.kdownloader.settings

/**
 * Renders and validates user filename templates (Section 3).
 *
 * Supported variables: {title} {channel} {date} {quality} {format} {id}. Rendering substitutes the
 * variables, strips characters Android's filesystem rejects, and enforces a maximum length while
 * preserving any file extension.
 *
 * Implemented with plain character scanning rather than regular expressions: it is pure, fully
 * unit-testable, and — importantly — has no work in its static initializer that could throw on a
 * device (a regex compiled in an object initializer that a stricter engine rejects would crash the
 * whole class with ExceptionInInitializerError).
 */
object FilenameTemplate {

    val VARIABLES = listOf("title", "channel", "date", "quality", "format", "id")

    /** The concrete values a download provides for substitution. */
    data class Values(
        val title: String = "",
        val channel: String = "",
        val date: String = "",
        val quality: String = "",
        val format: String = "",
        val id: String = "",
    ) {
        fun asMap(): Map<String, String> = mapOf(
            "title" to title,
            "channel" to channel,
            "date" to date,
            "quality" to quality,
            "format" to format,
            "id" to id,
        )
    }

    sealed interface Validation {
        data object Valid : Validation
        data class Invalid(val reason: String) : Validation
    }

    const val MIN_LENGTH = 8
    const val MAX_LENGTH = 250
    const val FALLBACK = "download"

    private val SAMPLE = Values(
        title = "My Great Video",
        channel = "Creator",
        date = "2026-07-20",
        quality = "1080p",
        format = "mp4",
        id = "dQw4w9WgXcQ",
    )

    /** Characters Android's filesystem does not allow in a filename. */
    private fun isIllegal(c: Char): Boolean =
        c.code < 0x20 || c == '\\' || c == '/' || c == ':' || c == '*' ||
            c == '?' || c == '"' || c == '<' || c == '>' || c == '|'

    /** Validates template syntax: balanced braces, no nesting, and only known variables. */
    fun validate(template: String): Validation {
        if (template.isBlank()) return Validation.Invalid("Template cannot be empty")

        var depth = 0
        for (c in template) {
            when (c) {
                '{' -> if (++depth > 1) return Validation.Invalid("Nested braces are not allowed")
                '}' -> if (--depth < 0) return Validation.Invalid("Unmatched '}'")
            }
        }
        if (depth != 0) return Validation.Invalid("Unmatched '{'")

        var i = 0
        while (i < template.length) {
            if (template[i] == '{') {
                val end = template.indexOf('}', i + 1)
                val name = template.substring(i + 1, end)
                if (name !in VARIABLES) return Validation.Invalid("Unknown variable {$name}")
                i = end + 1
            } else {
                i++
            }
        }
        return Validation.Valid
    }

    /**
     * Renders [template] using [values], sanitizing for Android and clamping to [maxLength].
     * Never returns blank — an empty result falls back to [FALLBACK].
     */
    fun render(template: String, values: Values, maxLength: Int): String {
        val effectiveTemplate = if (validate(template) is Validation.Valid) template else "{title}"
        return clamp(sanitize(substitute(effectiveTemplate, values.asMap())), maxLength)
    }

    /** Convenience for the settings UI's live preview using representative sample values. */
    fun example(template: String, maxLength: Int): String = render(template, SAMPLE, maxLength)

    /** Replaces `{var}` tokens with their values, leaving unknown tokens untouched. */
    private fun substitute(template: String, values: Map<String, String>): String {
        val sb = StringBuilder(template.length)
        var i = 0
        while (i < template.length) {
            val c = template[i]
            if (c == '{') {
                val end = template.indexOf('}', i + 1)
                if (end != -1) {
                    val name = template.substring(i + 1, end)
                    val replacement = values[name]
                    if (replacement != null) {
                        sb.append(replacement)
                        i = end + 1
                        continue
                    }
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    /** Replaces illegal characters with '_', collapses repeats, and trims stray separators. */
    fun sanitize(raw: String): String {
        val sb = StringBuilder(raw.length)
        var lastUnderscore = false
        for (c in raw) {
            if (isIllegal(c) || c == '_') {
                if (!lastUnderscore) {
                    sb.append('_')
                    lastUnderscore = true
                }
            } else {
                sb.append(c)
                lastUnderscore = false
            }
        }
        return sb.toString().trim(' ', '.', '_')
    }

    private fun clamp(name: String, maxLength: Int): String {
        val bound = maxLength.coerceIn(MIN_LENGTH, MAX_LENGTH)
        val cleaned = name.ifBlank { FALLBACK }
        if (cleaned.length <= bound) return cleaned

        // Preserve a trailing extension when clamping.
        val dot = cleaned.lastIndexOf('.')
        if (dot in 1 until cleaned.length) {
            val ext = cleaned.substring(dot)
            if (ext.length in 2..6) {
                val stem = cleaned.substring(0, dot)
                val room = (bound - ext.length).coerceAtLeast(1)
                return (stem.take(room).trim(' ', '.', '_') + ext).ifBlank { FALLBACK }
            }
        }
        return cleaned.take(bound).trim(' ', '.', '_').ifBlank { FALLBACK }
    }
}
