package com.kira.kdownloader.engine

import java.io.File

internal object DownloadOutputSelector {
    private val audioExtensions = setOf("aac", "flac", "m4a", "mp3", "ogg", "opus", "wav")
    private val videoExtensions = setOf("avi", "m4v", "mkv", "mov", "mp4", "webm")
    private val ignoredSuffixes = setOf("part", "temp", "tmp", "ytdl")
    private val formatFragment = Regex("\\.f\\d+(?=\\.[^.]+$)", RegexOption.IGNORE_CASE)

    fun select(files: List<File>, kind: FormatSelector.Kind): File? {
        val supportedExtensions = when (kind) {
            FormatSelector.Kind.AUDIO -> audioExtensions
            FormatSelector.Kind.VIDEO -> videoExtensions
        }
        return files
            .asSequence()
            .filter(File::isFile)
            .filterNot { it.extension.lowercase() in ignoredSuffixes }
            .filter { it.extension.lowercase() in supportedExtensions }
            // A .f123.mp4 file is an intermediate video-only DASH stream. Publishing it as the
            // completed video is how an apparently successful download can have no audio.
            .filter {
                kind != FormatSelector.Kind.VIDEO ||
                    !formatFragment.containsMatchIn(it.name)
            }
            .maxWithOrNull(
                compareBy<File>(
                    { if (it.extension.equals(preferredExtension(kind), ignoreCase = true)) 1 else 0 },
                    File::lastModified,
                    File::length,
                ),
            )
    }

    private fun preferredExtension(kind: FormatSelector.Kind): String = when (kind) {
        FormatSelector.Kind.AUDIO -> "mp3"
        FormatSelector.Kind.VIDEO -> "mp4"
    }
}
