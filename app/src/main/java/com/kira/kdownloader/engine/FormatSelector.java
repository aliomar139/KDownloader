package com.kira.kdownloader.engine;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class FormatSelector {
    public static final String FALLBACK_VIDEO_SELECTOR = "bestvideo*+bestaudio/best";
    private static final List<Integer> PREFERRED_HEIGHTS = Arrays.asList(1080, 720, 480, 360);
    private static final Set<String> SINGLE_QUALITY_HOSTS = new HashSet<>(Arrays.asList(
            "instagram.com", "instagr.am", "tiktok.com", "facebook.com", "fb.com", "fb.watch"));

    public enum Kind { VIDEO, AUDIO }

    private FormatSelector() {}

    public static List<DownloadChoice> choices(List<FormatInput> formats) { return choices(formats, ""); }

    public static List<DownloadChoice> choices(List<FormatInput> formats, String sourceUrl) {
        if (usesSingleVideoQuality(sourceUrl)) {
            return singleQualityChoices(formats, ExtractorOptions.isTikTokUrl(sourceUrl));
        }
        Set<Integer> heights = new HashSet<>();
        for (FormatInput format : formats) if (hasVideo(format) && format.getHeight() != null) heights.add(format.getHeight());
        Long audioBytes = bestAudioBytes(formats);
        List<DownloadChoice> result = new ArrayList<>();
        for (Integer height : PREFERRED_HEIGHTS) {
            if (!heights.contains(height)) continue;
            result.add(new DownloadChoice(
                    height + "p", Kind.VIDEO,
                    "bestvideo[height<=" + height + "]+bestaudio/best[height<=" + height + "]",
                    null, Collections.emptyMap(), videoBytes(formats, height, audioBytes)));
        }
        result.add(audioChoice(audioBytes));
        return result;
    }

    static boolean usesSingleVideoQuality(String sourceUrl) {
        try {
            String host = new URI(sourceUrl.trim()).getHost();
            if (host == null) return false;
            host = host.toLowerCase(Locale.ROOT);
            for (String root : SINGLE_QUALITY_HOSTS) {
                if (host.equals(root) || host.endsWith("." + root)) return true;
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private static Long bestAudioBytes(List<FormatInput> formats) {
        Long best = null;
        for (FormatInput format : formats) {
            Long size = format.getFilesize();
            if (hasAudio(format) && !hasVideo(format) && size != null && (best == null || size > best)) best = size;
        }
        return best;
    }

    private static Long videoBytes(List<FormatInput> formats, int height, Long audioBytes) {
        Long videoBytes = null;
        boolean alreadyMuxed = false;
        for (FormatInput format : formats) {
            if (!hasVideo(format) || format.getHeight() == null || format.getHeight() != height) continue;
            alreadyMuxed |= hasAudio(format);
            Long size = format.getFilesize();
            if (size != null && (videoBytes == null || size > videoBytes)) videoBytes = size;
        }
        if (videoBytes == null || alreadyMuxed) return videoBytes;
        return videoBytes + (audioBytes == null ? 0 : audioBytes);
    }

    private static List<DownloadChoice> singleQualityChoices(List<FormatInput> formats, boolean allowDirectVideo) {
        Long audioBytes = bestAudioBytes(formats);
        boolean preferDirectCombined = false;
        for (FormatInput format : formats) {
            if (allowDirectVideo && hasVideo(format) && hasAudio(format) && notBlank(format.getUrl())) {
                preferDirectCombined = true;
                break;
            }
        }

        FormatInput best = null;
        for (FormatInput candidate : formats) {
            if (!hasVideo(candidate)) continue;
            if (best == null || compareVideo(candidate, best, preferDirectCombined) >= 0) best = candidate;
        }

        List<DownloadChoice> result = new ArrayList<>();
        if (best != null) {
            String selector = FALLBACK_VIDEO_SELECTOR;
            if (notBlank(best.getFormatId()) && allowDirectVideo) {
                selector = hasAudio(best) ? best.getFormatId() : best.getFormatId() + "+bestaudio/best";
            }
            String directUrl = allowDirectVideo && hasAudio(best) && notBlank(best.getUrl()) ? best.getUrl() : null;
            Long size = best.getFilesize();
            Long approxBytes = size == null ? null : size + (hasAudio(best) || audioBytes == null ? 0 : audioBytes);
            result.add(new DownloadChoice(
                    best.getHeight() == null ? "Video" : best.getHeight() + "p",
                    Kind.VIDEO, selector, directUrl,
                    directUrl == null ? Collections.emptyMap() : best.getHttpHeaders(), approxBytes));
        }
        result.add(audioChoice(audioBytes));
        return result;
    }

    private static int compareVideo(FormatInput left, FormatInput right, boolean preferDirectCombined) {
        if (preferDirectCombined) {
            int direct = Boolean.compare(hasAudio(left) && notBlank(left.getUrl()), hasAudio(right) && notBlank(right.getUrl()));
            if (direct != 0) return direct;
        }
        int height = Integer.compare(left.getHeight() == null ? 0 : left.getHeight(), right.getHeight() == null ? 0 : right.getHeight());
        if (height != 0) return height;
        return preferDirectCombined ? 0 : Boolean.compare(hasAudio(left), hasAudio(right));
    }

    private static DownloadChoice audioChoice(Long approxBytes) {
        return new DownloadChoice("Audio (mp3)", Kind.AUDIO, "bestaudio/best", null, Collections.emptyMap(), approxBytes);
    }

    private static boolean hasVideo(FormatInput format) {
        return format.getVcodec() != null && !"none".equals(format.getVcodec());
    }

    private static boolean hasAudio(FormatInput format) {
        return format.getAcodec() != null && !"none".equals(format.getAcodec());
    }

    private static boolean notBlank(String value) { return value != null && !value.trim().isEmpty(); }
}
