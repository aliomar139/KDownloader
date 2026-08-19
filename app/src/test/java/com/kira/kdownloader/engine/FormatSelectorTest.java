package com.kira.kdownloader.engine;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class FormatSelectorTest {
    private FormatInput video(String id, int height) {
        return new FormatInput(id, "mp4", height, "avc1", "mp4a");
    }

    private FormatInput input(String id, int height, String audio, String url, Map<String, String> headers) {
        return new FormatInput(id, "mp4", height, "avc1", audio, url, headers, null);
    }

    private List<String> labels(List<DownloadChoice> choices) {
        List<String> result = new ArrayList<>();
        for (DownloadChoice choice : choices) result.add(choice.getLabel());
        return result;
    }

    @Test public void returnsVideoChoicesHighToLowPlusOneAudioChoice() {
        List<DownloadChoice> choices = FormatSelector.choices(Arrays.asList(video("a", 360), video("b", 1080), video("c", 720)));
        assertEquals(Arrays.asList("1080p", "720p", "360p", "Audio (mp3)"), labels(choices));
        assertEquals(FormatSelector.Kind.AUDIO, choices.get(choices.size() - 1).getKind());
    }

    @Test public void dedupesRepeatedHeights() {
        assertEquals(Arrays.asList("720p", "Audio (mp3)"), labels(FormatSelector.choices(Arrays.asList(video("a", 720), video("b", 720)))));
    }

    @Test public void audioOnlySourceYieldsJustTheAudioChoice() {
        List<DownloadChoice> choices = FormatSelector.choices(Collections.singletonList(new FormatInput("a", "m4a", null, "none", "mp4a")));
        assertEquals(1, choices.size());
        assertEquals("Audio (mp3)", choices.get(0).getLabel());
        assertEquals(FormatSelector.Kind.AUDIO, choices.get(0).getKind());
    }

    @Test public void videoChoiceUsesHeightCappedSelector() {
        assertEquals("bestvideo[height<=720]+bestaudio/best[height<=720]", FormatSelector.choices(Collections.singletonList(video("a", 720))).get(0).getFormatSelector());
    }

    @Test public void ignoresHeightsOutsideTheCuratedSet() {
        assertEquals(Collections.singletonList("Audio (mp3)"), labels(FormatSelector.choices(Arrays.asList(video("a", 1440), video("b", 240)))));
    }

    @Test public void instagramExposesOneBestVideoQualityPlusAudio() {
        List<DownloadChoice> choices = FormatSelector.choices(Arrays.asList(video("low", 640), video("best", 960)), "https://www.instagram.com/reel/example/");
        assertEquals(Arrays.asList("960p", "Audio (mp3)"), labels(choices));
        assertEquals(FormatSelector.FALLBACK_VIDEO_SELECTOR, choices.get(0).getFormatSelector());
    }

    @Test public void tiktokUsesASeparateAudioStreamWhenTheBestVideoHasNoAudio() {
        List<DownloadChoice> choices = FormatSelector.choices(Arrays.asList(
                video("combined", 720), input("video-only", 1080, "none", null, Collections.emptyMap())), "https://vt.tiktok.com/example/");
        assertEquals(Arrays.asList("1080p", "Audio (mp3)"), labels(choices));
        assertEquals("video-only+bestaudio/best", choices.get(0).getFormatSelector());
    }

    @Test public void tiktokReusesTheBestCombinedDirectFormatInsteadOfExtractingTwice() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Referer", "https://www.tiktok.com/");
        List<DownloadChoice> choices = FormatSelector.choices(Arrays.asList(
                input("combined", 720, "mp4a", "https://video.example/tiktok.mp4", headers),
                input("video-only", 1080, "none", "https://video.example/tiktok-video-only.mp4", Collections.emptyMap())),
                "https://www.tiktok.com/@example/video/1");
        DownloadChoice video = choices.get(0);
        assertEquals("720p", video.getLabel());
        assertEquals("https://video.example/tiktok.mp4", video.getDirectUrl());
        assertEquals("https://www.tiktok.com/", video.getHttpHeaders().get("Referer"));
    }

    @Test public void facebookReelExposesOneBestVideoQualityPlusAudio() {
        List<DownloadChoice> choices = FormatSelector.choices(Arrays.asList(video("low", 540), video("best", 1350)), "https://www.facebook.com/reel/123456789/");
        assertEquals(Arrays.asList("1350p", "Audio (mp3)"), labels(choices));
        assertEquals(FormatSelector.FALLBACK_VIDEO_SELECTOR, choices.get(0).getFormatSelector());
    }

    @Test public void facebookPostMergesAudioWhenItsBestVideoIsVideoOnly() {
        List<DownloadChoice> choices = FormatSelector.choices(Arrays.asList(
                video("combined", 640), input("video-only", 1280, "none", null, Collections.emptyMap())),
                "https://m.facebook.com/example/posts/123456789/");
        assertEquals(Arrays.asList("1280p", "Audio (mp3)"), labels(choices));
        assertEquals(FormatSelector.FALLBACK_VIDEO_SELECTOR, choices.get(0).getFormatSelector());
    }

    @Test public void fbWatchLinksUseTheFacebookSingleQualityBehavior() {
        List<DownloadChoice> choices = FormatSelector.choices(Collections.singletonList(video("watch-best", 960)), "https://fb.watch/example/");
        assertEquals(FormatSelector.FALLBACK_VIDEO_SELECTOR, choices.get(0).getFormatSelector());
        assertEquals(FormatSelector.Kind.VIDEO, choices.get(0).getKind());
    }

    @Test public void singleQualityDetectionDoesNotTrustLookalikeDomains() {
        assertEquals(false, FormatSelector.usesSingleVideoQuality("https://instagram.com.example.test/reel/1"));
        assertEquals(false, FormatSelector.usesSingleVideoQuality("https://facebook.com.example.test/reel/1"));
    }
}
