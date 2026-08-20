package com.kira.kdownloader.engine;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class DownloadChoice {
    private final String label;
    private final FormatSelector.Kind kind;
    private final String formatSelector;
    private final String directUrl;
    private final Map<String, String> httpHeaders;
    private final Long approxBytes;

    public DownloadChoice(String label, FormatSelector.Kind kind, String formatSelector,
                          String directUrl, Map<String, String> httpHeaders, Long approxBytes) {
        this.label = label;
        this.kind = kind;
        this.formatSelector = formatSelector;
        this.directUrl = directUrl;
        this.httpHeaders = httpHeaders == null ? Collections.emptyMap() : httpHeaders;
        this.approxBytes = approxBytes;
    }

    public String getLabel() { return label; }
    public FormatSelector.Kind getKind() { return kind; }
    public String getFormatSelector() { return formatSelector; }
    public String getDirectUrl() { return directUrl; }
    public Map<String, String> getHttpHeaders() { return httpHeaders; }
    public Long getApproxBytes() { return approxBytes; }

    public DownloadChoice withDirect(String url, Map<String, String> headers) {
        return new DownloadChoice(label, kind, formatSelector, url, headers, approxBytes);
    }

    public DownloadChoice withFormatSelector(String selector) {
        return new DownloadChoice(label, kind, selector, directUrl, httpHeaders, approxBytes);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DownloadChoice)) return false;
        DownloadChoice that = (DownloadChoice) other;
        return label.equals(that.label) && kind == that.kind
                && formatSelector.equals(that.formatSelector)
                && Objects.equals(directUrl, that.directUrl)
                && httpHeaders.equals(that.httpHeaders)
                && Objects.equals(approxBytes, that.approxBytes);
    }

    @Override public int hashCode() {
        return Objects.hash(label, kind, formatSelector, directUrl, httpHeaders, approxBytes);
    }

    @Override public String toString() {
        return "DownloadChoice(label=" + label + ", kind=" + kind + ", formatSelector="
                + formatSelector + ", directUrl=" + directUrl + ", httpHeaders="
                + httpHeaders + ", approxBytes=" + approxBytes + ')';
    }
}
