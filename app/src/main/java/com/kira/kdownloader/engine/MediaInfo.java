package com.kira.kdownloader.engine;

import java.util.List;
import java.util.Objects;

public final class MediaInfo {
    private final String title;
    private final String thumbnailUrl;
    private final List<DownloadChoice> choices;
    private final int durationSeconds;
    private final String uploader;

    public MediaInfo(String title, String thumbnailUrl, List<DownloadChoice> choices,
                     int durationSeconds, String uploader) {
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.choices = choices;
        this.durationSeconds = durationSeconds;
        this.uploader = uploader;
    }

    public String getTitle() { return title; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public List<DownloadChoice> getChoices() { return choices; }
    public int getDurationSeconds() { return durationSeconds; }
    public String getUploader() { return uploader; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MediaInfo)) return false;
        MediaInfo that = (MediaInfo) other;
        return durationSeconds == that.durationSeconds
                && title.equals(that.title)
                && Objects.equals(thumbnailUrl, that.thumbnailUrl)
                && choices.equals(that.choices)
                && Objects.equals(uploader, that.uploader);
    }

    @Override public int hashCode() {
        return Objects.hash(title, thumbnailUrl, choices, durationSeconds, uploader);
    }
}
