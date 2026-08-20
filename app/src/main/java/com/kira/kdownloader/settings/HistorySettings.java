package com.kira.kdownloader.settings;

import java.util.Objects;

public final class HistorySettings {
    private final boolean keepHistory; private final HistoryRetention retention;
    private final boolean saveSearchHistory;
    public HistorySettings(){this(true,HistoryRetention.FOREVER,true);}
    public HistorySettings(boolean keepHistory,HistoryRetention retention,boolean saveSearchHistory){this.keepHistory=keepHistory;this.retention=retention;this.saveSearchHistory=saveSearchHistory;}
    public boolean getKeepHistory(){return keepHistory;} public HistoryRetention getRetention(){return retention;}
    public boolean getSaveSearchHistory(){return saveSearchHistory;}
    public HistorySettings withKeepHistory(boolean v){return new HistorySettings(v,retention,saveSearchHistory);}
    public HistorySettings withRetention(HistoryRetention v){return new HistorySettings(keepHistory,v,saveSearchHistory);}
    public HistorySettings withSaveSearchHistory(boolean v){return new HistorySettings(keepHistory,retention,v);}
    @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof HistorySettings))return false;HistorySettings x=(HistorySettings)o;return keepHistory==x.keepHistory&&saveSearchHistory==x.saveSearchHistory&&retention==x.retention;}
    @Override public int hashCode(){return Objects.hash(keepHistory,retention,saveSearchHistory);}
    @Override public String toString(){return "HistorySettings(keepHistory="+keepHistory+", retention="+retention+", saveSearchHistory="+saveSearchHistory+")";}
}
