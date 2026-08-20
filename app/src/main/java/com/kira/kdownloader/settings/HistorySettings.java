package com.kira.kdownloader.settings;

import java.util.Objects;

public final class HistorySettings {
    private final boolean keepHistory; private final HistoryRetention retention;
    private final boolean saveRecentUrls, saveSearchHistory;
    public HistorySettings(){this(true,HistoryRetention.FOREVER,true,true);}
    public HistorySettings(boolean keepHistory,HistoryRetention retention,boolean saveRecentUrls,boolean saveSearchHistory){this.keepHistory=keepHistory;this.retention=retention;this.saveRecentUrls=saveRecentUrls;this.saveSearchHistory=saveSearchHistory;}
    public boolean getKeepHistory(){return keepHistory;} public HistoryRetention getRetention(){return retention;}
    public boolean getSaveRecentUrls(){return saveRecentUrls;} public boolean getSaveSearchHistory(){return saveSearchHistory;}
    public HistorySettings withKeepHistory(boolean v){return new HistorySettings(v,retention,saveRecentUrls,saveSearchHistory);}
    public HistorySettings withRetention(HistoryRetention v){return new HistorySettings(keepHistory,v,saveRecentUrls,saveSearchHistory);}
    public HistorySettings withSaveRecentUrls(boolean v){return new HistorySettings(keepHistory,retention,v,saveSearchHistory);}
    public HistorySettings withSaveSearchHistory(boolean v){return new HistorySettings(keepHistory,retention,saveRecentUrls,v);}
    @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof HistorySettings))return false;HistorySettings x=(HistorySettings)o;return keepHistory==x.keepHistory&&saveRecentUrls==x.saveRecentUrls&&saveSearchHistory==x.saveSearchHistory&&retention==x.retention;}
    @Override public int hashCode(){return Objects.hash(keepHistory,retention,saveRecentUrls,saveSearchHistory);}
    @Override public String toString(){return "HistorySettings(keepHistory="+keepHistory+", retention="+retention+", saveRecentUrls="+saveRecentUrls+", saveSearchHistory="+saveSearchHistory+")";}
}
