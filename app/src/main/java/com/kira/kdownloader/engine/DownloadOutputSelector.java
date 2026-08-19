package com.kira.kdownloader.engine;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class DownloadOutputSelector {
    private static final Set<String> AUDIO_EXTENSIONS = new HashSet<>(Arrays.asList("aac", "flac", "m4a", "mp3", "ogg", "opus", "wav"));
    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList("avi", "m4v", "mkv", "mov", "mp4", "webm"));
    private static final Set<String> IGNORED_SUFFIXES = new HashSet<>(Arrays.asList("part", "temp", "tmp", "ytdl"));
    private static final Pattern FORMAT_FRAGMENT = Pattern.compile("\\.f\\d+(?=\\.[^.]+$)", Pattern.CASE_INSENSITIVE);

    private DownloadOutputSelector() {}

    public static File select(List<File> files, FormatSelector.Kind kind) {
        Set<String> supported = kind == FormatSelector.Kind.AUDIO ? AUDIO_EXTENSIONS : VIDEO_EXTENSIONS;
        File best = null;
        for (File file : files) {
            String extension = extension(file.getName()).toLowerCase(Locale.ROOT);
            if (!file.isFile() || IGNORED_SUFFIXES.contains(extension) || !supported.contains(extension)) continue;
            if (kind == FormatSelector.Kind.VIDEO && FORMAT_FRAGMENT.matcher(file.getName()).find()) continue;
            if (best == null || compare(file, best, preferredExtension(kind)) > 0) best = file;
        }
        return best;
    }

    private static int compare(File left, File right, String preferred) {
        int result = Boolean.compare(extension(left.getName()).equalsIgnoreCase(preferred), extension(right.getName()).equalsIgnoreCase(preferred));
        if (result != 0) return result;
        result = Long.compare(left.lastModified(), right.lastModified());
        return result != 0 ? result : Long.compare(left.length(), right.length());
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1);
    }

    private static String preferredExtension(FormatSelector.Kind kind) {
        return kind == FormatSelector.Kind.AUDIO ? "mp3" : "mp4";
    }
}
