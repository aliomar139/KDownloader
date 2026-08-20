package com.kira.kdownloader.engine;

import java.util.Collections;
import java.util.Map;

public final class FormatInput {
    private final String formatId;
    private final String ext;
    private final Integer height;
    private final String vcodec;
    private final String acodec;
    private final String url;
    private final Map<String, String> httpHeaders;
    private final Long filesize;

    public FormatInput(String formatId, String ext, Integer height, String vcodec, String acodec,
                       String url, Map<String, String> httpHeaders, Long filesize) {
        this.formatId = formatId;
        this.ext = ext;
        this.height = height;
        this.vcodec = vcodec;
        this.acodec = acodec;
        this.url = url;
        this.httpHeaders = httpHeaders == null ? Collections.emptyMap() : httpHeaders;
        this.filesize = filesize;
    }

    public FormatInput(String formatId, String ext, Integer height, String vcodec, String acodec) {
        this(formatId, ext, height, vcodec, acodec, null, Collections.emptyMap(), null);
    }

    public String getFormatId() { return formatId; }
    public String getExt() { return ext; }
    public Integer getHeight() { return height; }
    public String getVcodec() { return vcodec; }
    public String getAcodec() { return acodec; }
    public String getUrl() { return url; }
    public Map<String, String> getHttpHeaders() { return httpHeaders; }
    public Long getFilesize() { return filesize; }
}
