package com.kira.kdownloader.settings;

import java.util.Objects;

public final class AppSettings {
    public static final AppSettings DEFAULTS = new AppSettings();
    private final DownloadSettings download; private final StorageSettings storage; private final BehaviorSettings behavior;
    private final NetworkSettings network; private final SubtitleSettings subtitles; private final NotificationSettings notifications;
    private final AppearanceSettings appearance; private final HistorySettings history; private final ProcessingSettings processing;
    public AppSettings(){this(new DownloadSettings(),new StorageSettings(),new BehaviorSettings(),new NetworkSettings(),new SubtitleSettings(),new NotificationSettings(),new AppearanceSettings(),new HistorySettings(),new ProcessingSettings());}
    public AppSettings(DownloadSettings download,StorageSettings storage,BehaviorSettings behavior,NetworkSettings network,
                       SubtitleSettings subtitles,NotificationSettings notifications,AppearanceSettings appearance,
                       HistorySettings history,ProcessingSettings processing){this.download=download;this.storage=storage;this.behavior=behavior;this.network=network;this.subtitles=subtitles;this.notifications=notifications;this.appearance=appearance;this.history=history;this.processing=processing;}
    public DownloadSettings getDownload(){return download;} public StorageSettings getStorage(){return storage;}
    public BehaviorSettings getBehavior(){return behavior;} public NetworkSettings getNetwork(){return network;}
    public SubtitleSettings getSubtitles(){return subtitles;} public NotificationSettings getNotifications(){return notifications;}
    public AppearanceSettings getAppearance(){return appearance;} public HistorySettings getHistory(){return history;}
    public ProcessingSettings getProcessing(){return processing;}
    public AppSettings withDownload(DownloadSettings v){return new AppSettings(v,storage,behavior,network,subtitles,notifications,appearance,history,processing);}
    public AppSettings withStorage(StorageSettings v){return new AppSettings(download,v,behavior,network,subtitles,notifications,appearance,history,processing);}
    public AppSettings withBehavior(BehaviorSettings v){return new AppSettings(download,storage,v,network,subtitles,notifications,appearance,history,processing);}
    public AppSettings withNetwork(NetworkSettings v){return new AppSettings(download,storage,behavior,v,subtitles,notifications,appearance,history,processing);}
    public AppSettings withSubtitles(SubtitleSettings v){return new AppSettings(download,storage,behavior,network,v,notifications,appearance,history,processing);}
    public AppSettings withNotifications(NotificationSettings v){return new AppSettings(download,storage,behavior,network,subtitles,v,appearance,history,processing);}
    public AppSettings withAppearance(AppearanceSettings v){return new AppSettings(download,storage,behavior,network,subtitles,notifications,v,history,processing);}
    public AppSettings withHistory(HistorySettings v){return new AppSettings(download,storage,behavior,network,subtitles,notifications,appearance,v,processing);}
    public AppSettings withProcessing(ProcessingSettings v){return new AppSettings(download,storage,behavior,network,subtitles,notifications,appearance,history,v);}
    @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof AppSettings))return false;AppSettings x=(AppSettings)o;return download.equals(x.download)&&storage.equals(x.storage)&&behavior.equals(x.behavior)&&network.equals(x.network)&&subtitles.equals(x.subtitles)&&notifications.equals(x.notifications)&&appearance.equals(x.appearance)&&history.equals(x.history)&&processing.equals(x.processing);}
    @Override public int hashCode(){return Objects.hash(download,storage,behavior,network,subtitles,notifications,appearance,history,processing);}
    @Override public String toString(){return "AppSettings(download="+download+", storage="+storage+", behavior="+behavior+", network="+network+", subtitles="+subtitles+", notifications="+notifications+", appearance="+appearance+", history="+history+", processing="+processing+")";}
}
