package com.kira.kdownloader.settings;

import java.util.Objects;

public final class ProcessingSettings {
    private final boolean enableConversion,deleteSourceAfterConversion,preserveSourceOnFailure,preferHardwareAcceleration;
    private final ProcessingPriority priority; private final boolean allowBackgroundProcessing;
    private final int maxTempStorageMb; private final DiagnosticLogLevel logLevel;
    public ProcessingSettings(){this(false,false,true,true,ProcessingPriority.BALANCED,true,2048,DiagnosticLogLevel.ERRORS);}
    public ProcessingSettings(boolean enableConversion,boolean deleteSourceAfterConversion,boolean preserveSourceOnFailure,
                              boolean preferHardwareAcceleration,ProcessingPriority priority,boolean allowBackgroundProcessing,
                              int maxTempStorageMb,DiagnosticLogLevel logLevel){this.enableConversion=enableConversion;this.deleteSourceAfterConversion=deleteSourceAfterConversion;this.preserveSourceOnFailure=preserveSourceOnFailure;this.preferHardwareAcceleration=preferHardwareAcceleration;this.priority=priority;this.allowBackgroundProcessing=allowBackgroundProcessing;this.maxTempStorageMb=maxTempStorageMb;this.logLevel=logLevel;}
    public boolean getEnableConversion(){return enableConversion;} public boolean getDeleteSourceAfterConversion(){return deleteSourceAfterConversion;}
    public boolean getPreserveSourceOnFailure(){return preserveSourceOnFailure;} public boolean getPreferHardwareAcceleration(){return preferHardwareAcceleration;}
    public ProcessingPriority getPriority(){return priority;} public boolean getAllowBackgroundProcessing(){return allowBackgroundProcessing;}
    public int getMaxTempStorageMb(){return maxTempStorageMb;} public DiagnosticLogLevel getLogLevel(){return logLevel;}
    public ProcessingSettings withEnableConversion(boolean v){return new ProcessingSettings(v,deleteSourceAfterConversion,preserveSourceOnFailure,preferHardwareAcceleration,priority,allowBackgroundProcessing,maxTempStorageMb,logLevel);}
    public ProcessingSettings withDeleteSourceAfterConversion(boolean v){return new ProcessingSettings(enableConversion,v,preserveSourceOnFailure,preferHardwareAcceleration,priority,allowBackgroundProcessing,maxTempStorageMb,logLevel);}
    public ProcessingSettings withPreserveSourceOnFailure(boolean v){return new ProcessingSettings(enableConversion,deleteSourceAfterConversion,v,preferHardwareAcceleration,priority,allowBackgroundProcessing,maxTempStorageMb,logLevel);}
    public ProcessingSettings withPreferHardwareAcceleration(boolean v){return new ProcessingSettings(enableConversion,deleteSourceAfterConversion,preserveSourceOnFailure,v,priority,allowBackgroundProcessing,maxTempStorageMb,logLevel);}
    public ProcessingSettings withPriority(ProcessingPriority v){return new ProcessingSettings(enableConversion,deleteSourceAfterConversion,preserveSourceOnFailure,preferHardwareAcceleration,v,allowBackgroundProcessing,maxTempStorageMb,logLevel);}
    public ProcessingSettings withAllowBackgroundProcessing(boolean v){return new ProcessingSettings(enableConversion,deleteSourceAfterConversion,preserveSourceOnFailure,preferHardwareAcceleration,priority,v,maxTempStorageMb,logLevel);}
    public ProcessingSettings withMaxTempStorageMb(int v){return new ProcessingSettings(enableConversion,deleteSourceAfterConversion,preserveSourceOnFailure,preferHardwareAcceleration,priority,allowBackgroundProcessing,v,logLevel);}
    public ProcessingSettings withLogLevel(DiagnosticLogLevel v){return new ProcessingSettings(enableConversion,deleteSourceAfterConversion,preserveSourceOnFailure,preferHardwareAcceleration,priority,allowBackgroundProcessing,maxTempStorageMb,v);}
    @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof ProcessingSettings))return false;ProcessingSettings x=(ProcessingSettings)o;return enableConversion==x.enableConversion&&deleteSourceAfterConversion==x.deleteSourceAfterConversion&&preserveSourceOnFailure==x.preserveSourceOnFailure&&preferHardwareAcceleration==x.preferHardwareAcceleration&&allowBackgroundProcessing==x.allowBackgroundProcessing&&maxTempStorageMb==x.maxTempStorageMb&&priority==x.priority&&logLevel==x.logLevel;}
    @Override public int hashCode(){return Objects.hash(enableConversion,deleteSourceAfterConversion,preserveSourceOnFailure,preferHardwareAcceleration,priority,allowBackgroundProcessing,maxTempStorageMb,logLevel);}
    @Override public String toString(){return "ProcessingSettings(enableConversion="+enableConversion+", deleteSourceAfterConversion="+deleteSourceAfterConversion+", preserveSourceOnFailure="+preserveSourceOnFailure+", preferHardwareAcceleration="+preferHardwareAcceleration+", priority="+priority+", allowBackgroundProcessing="+allowBackgroundProcessing+", maxTempStorageMb="+maxTempStorageMb+", logLevel="+logLevel+")";}
}
