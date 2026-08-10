package com.kira.kdownloader.engine

import java.net.URI

data class FormatInput(
    val formatId: String,
    val ext: String,
    val height: Int?,
    val vcodec: String?,
    val acodec: String?,
    val url: String? = null,
    val httpHeaders: Map<String, String> = emptyMap(),
    /** Reported (or approximate) size of this individual format in bytes, if known. */
    val filesize: Long? = null,
)

data class DownloadChoice(
    val label: String,
    val kind: FormatSelector.Kind,
    val formatSelector: String,
    val directUrl: String? = null,
    val httpHeaders: Map<String, String> = emptyMap(),
    /** Best-effort estimated download size in bytes for display, if known. */
    val approxBytes: Long? = null,
)

object FormatSelector {
    enum class Kind { VIDEO, AUDIO }

    private val preferredHeights = listOf(1080, 720, 480, 360)
    private val singleQualityHosts = setOf(
        "instagram.com",
        "instagr.am",
        "tiktok.com",
        "facebook.com",
        "fb.com",
        "fb.watch",
    )

    fun choices(formats: List<FormatInput>, sourceUrl: String = ""): List<DownloadChoice> {
        if (usesSingleVideoQuality(sourceUrl)) {
            return singleQualityChoices(
                formats = formats,
                allowDirectVideo = ExtractorOptions.isTikTokUrl(sourceUrl),
            )
        }

        val videoHeights = formats
            .asSequence()
            .filter { format -> format.hasVideo() }
            .mapNotNull { it.height }
            .toSet()

        val audioEstimate = bestAudioBytes(formats)
        val videoChoices = preferredHeights
            .filter(videoHeights::contains)
            .map { height ->
                DownloadChoice(
                    label = "${height}p",
                    kind = Kind.VIDEO,
                    formatSelector =
                        "bestvideo[height<=$height]+bestaudio/best[height<=$height]",
                    approxBytes = videoBytes(formats, height, audioEstimate),
                )
            }

        return videoChoices + audioChoice(audioEstimate)
    }

    /** Largest known audio-only size, used as the audio-track estimate for merged video. */
    private fun bestAudioBytes(formats: List<FormatInput>): Long? = formats
        .filter { it.hasAudio() && !it.hasVideo() }
        .mapNotNull(FormatInput::filesize)
        .maxOrNull()

    /** Estimated size for a video choice at [height]: the video format plus an audio track. */
    private fun videoBytes(
        formats: List<FormatInput>,
        height: Int,
        bestAudioBytes: Long?,
    ): Long? {
        val atHeight = formats.filter { it.hasVideo() && it.height == height }
        val videoBytes = atHeight.mapNotNull(FormatInput::filesize).maxOrNull() ?: return null
        val alreadyMuxed = atHeight.any { it.hasAudio() }
        return if (alreadyMuxed) videoBytes else videoBytes + (bestAudioBytes ?: 0)
    }

    private fun singleQualityChoices(
        formats: List<FormatInput>,
        allowDirectVideo: Boolean,
    ): List<DownloadChoice> {
        val audioEstimate = bestAudioBytes(formats)
        val preferDirectCombined = allowDirectVideo && formats.any { format ->
            format.hasVideo() && format.hasAudio() && !format.url.isNullOrBlank()
        }
        val bestVideo = formats
            .withIndex()
            .filter { (_, format) -> format.hasVideo() }
            .maxWithOrNull(
                if (preferDirectCombined) {
                    compareBy(
                        { (_, format) -> if (format.hasAudio() && !format.url.isNullOrBlank()) 1 else 0 },
                        { (_, format) -> format.height ?: 0 },
                        IndexedValue<FormatInput>::index,
                    )
                } else {
                    compareBy(
                        { (_, format) -> format.height ?: 0 },
                        { (_, format) -> if (format.hasAudio()) 1 else 0 },
                        IndexedValue<FormatInput>::index,
                    )
                },
            )
            ?.value

        val videoChoice = bestVideo?.let { format ->
            val selector = format.formatId
                .takeIf(String::isNotBlank)
                ?.let { formatId ->
                    when {
                        allowDirectVideo && format.hasAudio() -> formatId
                        allowDirectVideo -> "$formatId+bestaudio/best"
                        else -> FALLBACK_VIDEO_SELECTOR
                    }
                }
                ?: FALLBACK_VIDEO_SELECTOR

            val directUrl = format.url
                ?.takeIf { url -> allowDirectVideo && format.hasAudio() && url.isNotBlank() }
            val approxBytes = format.filesize?.let { vBytes ->
                if (format.hasAudio()) vBytes else vBytes + (audioEstimate ?: 0)
            }
            DownloadChoice(
                label = format.height?.let { "${it}p" } ?: "Video",
                kind = Kind.VIDEO,
                formatSelector = selector,
                directUrl = directUrl,
                httpHeaders = if (directUrl != null) format.httpHeaders else emptyMap(),
                approxBytes = approxBytes,
            )
        }

        return listOfNotNull(videoChoice) + audioChoice(audioEstimate)
    }

    internal fun usesSingleVideoQuality(sourceUrl: String): Boolean {
        val host = runCatching { URI(sourceUrl.trim()).host }
            .getOrNull()
            ?.lowercase()
            ?: return false

        return singleQualityHosts.any { root -> host == root || host.endsWith(".$root") }
    }

    private fun FormatInput.hasVideo(): Boolean =
        vcodec != null && vcodec != "none"

    private fun FormatInput.hasAudio(): Boolean =
        acodec != null && acodec != "none"

    private fun audioChoice(approxBytes: Long? = null) = DownloadChoice(
        label = "Audio (mp3)",
        kind = Kind.AUDIO,
        formatSelector = "bestaudio/best",
        approxBytes = approxBytes,
    )

    const val FALLBACK_VIDEO_SELECTOR = "bestvideo*+bestaudio/best"
}
