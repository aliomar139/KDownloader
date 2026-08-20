package com.kira.kdownloader.settings;

import java.util.Objects;

public final class AppearanceSettings {
    private final AppTheme theme; private final boolean dynamicColor; private final String languageTag;
    private final boolean compactList, showFileSize, showSpeed, showEta, reduceAnimations, highContrast;
    public AppearanceSettings() { this(AppTheme.SYSTEM, false, "", false, true, true, true, false, false); }
    public AppearanceSettings(AppTheme theme, boolean dynamicColor, String languageTag, boolean compactList,
                              boolean showFileSize, boolean showSpeed, boolean showEta,
                              boolean reduceAnimations, boolean highContrast) {
        this.theme=theme; this.dynamicColor=dynamicColor; this.languageTag=languageTag; this.compactList=compactList;
        this.showFileSize=showFileSize; this.showSpeed=showSpeed; this.showEta=showEta;
        this.reduceAnimations=reduceAnimations; this.highContrast=highContrast;
    }
    public AppTheme getTheme(){return theme;} public boolean getDynamicColor(){return dynamicColor;}
    public String getLanguageTag(){return languageTag;} public boolean getCompactList(){return compactList;}
    public boolean getShowFileSize(){return showFileSize;} public boolean getShowSpeed(){return showSpeed;}
    public boolean getShowEta(){return showEta;} public boolean getReduceAnimations(){return reduceAnimations;}
    public boolean getHighContrast(){return highContrast;}
    public AppearanceSettings withTheme(AppTheme v){return new AppearanceSettings(v,dynamicColor,languageTag,compactList,showFileSize,showSpeed,showEta,reduceAnimations,highContrast);}
    public AppearanceSettings withDynamicColor(boolean v){return new AppearanceSettings(theme,v,languageTag,compactList,showFileSize,showSpeed,showEta,reduceAnimations,highContrast);}
    public AppearanceSettings withLanguageTag(String v){return new AppearanceSettings(theme,dynamicColor,v,compactList,showFileSize,showSpeed,showEta,reduceAnimations,highContrast);}
    public AppearanceSettings withCompactList(boolean v){return new AppearanceSettings(theme,dynamicColor,languageTag,v,showFileSize,showSpeed,showEta,reduceAnimations,highContrast);}
    public AppearanceSettings withShowFileSize(boolean v){return new AppearanceSettings(theme,dynamicColor,languageTag,compactList,v,showSpeed,showEta,reduceAnimations,highContrast);}
    public AppearanceSettings withShowSpeed(boolean v){return new AppearanceSettings(theme,dynamicColor,languageTag,compactList,showFileSize,v,showEta,reduceAnimations,highContrast);}
    public AppearanceSettings withShowEta(boolean v){return new AppearanceSettings(theme,dynamicColor,languageTag,compactList,showFileSize,showSpeed,v,reduceAnimations,highContrast);}
    public AppearanceSettings withReduceAnimations(boolean v){return new AppearanceSettings(theme,dynamicColor,languageTag,compactList,showFileSize,showSpeed,showEta,v,highContrast);}
    public AppearanceSettings withHighContrast(boolean v){return new AppearanceSettings(theme,dynamicColor,languageTag,compactList,showFileSize,showSpeed,showEta,reduceAnimations,v);}
    @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof AppearanceSettings))return false;AppearanceSettings x=(AppearanceSettings)o;return dynamicColor==x.dynamicColor&&compactList==x.compactList&&showFileSize==x.showFileSize&&showSpeed==x.showSpeed&&showEta==x.showEta&&reduceAnimations==x.reduceAnimations&&highContrast==x.highContrast&&theme==x.theme&&languageTag.equals(x.languageTag);}
    @Override public int hashCode(){return Objects.hash(theme,dynamicColor,languageTag,compactList,showFileSize,showSpeed,showEta,reduceAnimations,highContrast);}
    @Override public String toString(){return "AppearanceSettings(theme="+theme+", dynamicColor="+dynamicColor+", languageTag="+languageTag+", compactList="+compactList+", showFileSize="+showFileSize+", showSpeed="+showSpeed+", showEta="+showEta+", reduceAnimations="+reduceAnimations+", highContrast="+highContrast+")";}
}
