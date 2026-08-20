package com.kira.kdownloader.settings;

import java.util.Objects;

public final class SubtitleSettings {
    private final boolean downloadSubtitles; private final String preferredLanguage,fallbackLanguage;
    private final SubtitleTypePreference subtitleType; private final SubtitleFormat format;
    private final boolean embedInVideo,saveAsSeparateFiles,includeAllLanguages,addLanguageCodeToFilename;
    public SubtitleSettings(){this(false,"en","",SubtitleTypePreference.PREFER_MANUAL,SubtitleFormat.SRT,true,false,false,true);}
    public SubtitleSettings(boolean downloadSubtitles,String preferredLanguage,String fallbackLanguage,
                            SubtitleTypePreference subtitleType,SubtitleFormat format,boolean embedInVideo,
                            boolean saveAsSeparateFiles,boolean includeAllLanguages,boolean addLanguageCodeToFilename){this.downloadSubtitles=downloadSubtitles;this.preferredLanguage=preferredLanguage;this.fallbackLanguage=fallbackLanguage;this.subtitleType=subtitleType;this.format=format;this.embedInVideo=embedInVideo;this.saveAsSeparateFiles=saveAsSeparateFiles;this.includeAllLanguages=includeAllLanguages;this.addLanguageCodeToFilename=addLanguageCodeToFilename;}
    public boolean getDownloadSubtitles(){return downloadSubtitles;} public String getPreferredLanguage(){return preferredLanguage;}
    public String getFallbackLanguage(){return fallbackLanguage;} public SubtitleTypePreference getSubtitleType(){return subtitleType;}
    public SubtitleFormat getFormat(){return format;} public boolean getEmbedInVideo(){return embedInVideo;}
    public boolean getSaveAsSeparateFiles(){return saveAsSeparateFiles;} public boolean getIncludeAllLanguages(){return includeAllLanguages;}
    public boolean getAddLanguageCodeToFilename(){return addLanguageCodeToFilename;}
    public SubtitleSettings withDownloadSubtitles(boolean v){return new SubtitleSettings(v,preferredLanguage,fallbackLanguage,subtitleType,format,embedInVideo,saveAsSeparateFiles,includeAllLanguages,addLanguageCodeToFilename);}
    public SubtitleSettings withPreferredLanguage(String v){return new SubtitleSettings(downloadSubtitles,v,fallbackLanguage,subtitleType,format,embedInVideo,saveAsSeparateFiles,includeAllLanguages,addLanguageCodeToFilename);}
    public SubtitleSettings withFallbackLanguage(String v){return new SubtitleSettings(downloadSubtitles,preferredLanguage,v,subtitleType,format,embedInVideo,saveAsSeparateFiles,includeAllLanguages,addLanguageCodeToFilename);}
    public SubtitleSettings withSubtitleType(SubtitleTypePreference v){return new SubtitleSettings(downloadSubtitles,preferredLanguage,fallbackLanguage,v,format,embedInVideo,saveAsSeparateFiles,includeAllLanguages,addLanguageCodeToFilename);}
    public SubtitleSettings withFormat(SubtitleFormat v){return new SubtitleSettings(downloadSubtitles,preferredLanguage,fallbackLanguage,subtitleType,v,embedInVideo,saveAsSeparateFiles,includeAllLanguages,addLanguageCodeToFilename);}
    public SubtitleSettings withEmbedInVideo(boolean v){return new SubtitleSettings(downloadSubtitles,preferredLanguage,fallbackLanguage,subtitleType,format,v,saveAsSeparateFiles,includeAllLanguages,addLanguageCodeToFilename);}
    public SubtitleSettings withSaveAsSeparateFiles(boolean v){return new SubtitleSettings(downloadSubtitles,preferredLanguage,fallbackLanguage,subtitleType,format,embedInVideo,v,includeAllLanguages,addLanguageCodeToFilename);}
    public SubtitleSettings withIncludeAllLanguages(boolean v){return new SubtitleSettings(downloadSubtitles,preferredLanguage,fallbackLanguage,subtitleType,format,embedInVideo,saveAsSeparateFiles,v,addLanguageCodeToFilename);}
    public SubtitleSettings withAddLanguageCodeToFilename(boolean v){return new SubtitleSettings(downloadSubtitles,preferredLanguage,fallbackLanguage,subtitleType,format,embedInVideo,saveAsSeparateFiles,includeAllLanguages,v);}
    @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof SubtitleSettings))return false;SubtitleSettings x=(SubtitleSettings)o;return downloadSubtitles==x.downloadSubtitles&&embedInVideo==x.embedInVideo&&saveAsSeparateFiles==x.saveAsSeparateFiles&&includeAllLanguages==x.includeAllLanguages&&addLanguageCodeToFilename==x.addLanguageCodeToFilename&&preferredLanguage.equals(x.preferredLanguage)&&fallbackLanguage.equals(x.fallbackLanguage)&&subtitleType==x.subtitleType&&format==x.format;}
    @Override public int hashCode(){return Objects.hash(downloadSubtitles,preferredLanguage,fallbackLanguage,subtitleType,format,embedInVideo,saveAsSeparateFiles,includeAllLanguages,addLanguageCodeToFilename);}
    @Override public String toString(){return "SubtitleSettings(downloadSubtitles="+downloadSubtitles+", preferredLanguage="+preferredLanguage+", fallbackLanguage="+fallbackLanguage+", subtitleType="+subtitleType+", format="+format+", embedInVideo="+embedInVideo+", saveAsSeparateFiles="+saveAsSeparateFiles+", includeAllLanguages="+includeAllLanguages+", addLanguageCodeToFilename="+addLanguageCodeToFilename+")";}
}
