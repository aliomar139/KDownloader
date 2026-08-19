package com.kira.kdownloader.settings;

import java.util.Objects;

public final class StorageSettings {
    private final String downloadFolderUri,videoFolderUri,audioFolderUri,tempFolderUri;
    private final boolean warnOnLowSpace; private final FilenameConflict filenameConflict;
    private final String filenameTemplate; private final int maxFilenameLength;
    private final SubfolderOrganization subfolderOrganization;
    public StorageSettings(){this("","","","",true,FilenameConflict.ADD_NUMBER,"{title}",120,SubfolderOrganization.NONE);}
    public StorageSettings(String downloadFolderUri,String videoFolderUri,String audioFolderUri,String tempFolderUri,
                           boolean warnOnLowSpace,FilenameConflict filenameConflict,String filenameTemplate,
                           int maxFilenameLength,SubfolderOrganization subfolderOrganization){this.downloadFolderUri=downloadFolderUri;this.videoFolderUri=videoFolderUri;this.audioFolderUri=audioFolderUri;this.tempFolderUri=tempFolderUri;this.warnOnLowSpace=warnOnLowSpace;this.filenameConflict=filenameConflict;this.filenameTemplate=filenameTemplate;this.maxFilenameLength=maxFilenameLength;this.subfolderOrganization=subfolderOrganization;}
    public String getDownloadFolderUri(){return downloadFolderUri;} public String getVideoFolderUri(){return videoFolderUri;}
    public String getAudioFolderUri(){return audioFolderUri;} public String getTempFolderUri(){return tempFolderUri;}
    public boolean getWarnOnLowSpace(){return warnOnLowSpace;} public FilenameConflict getFilenameConflict(){return filenameConflict;}
    public String getFilenameTemplate(){return filenameTemplate;} public int getMaxFilenameLength(){return maxFilenameLength;}
    public SubfolderOrganization getSubfolderOrganization(){return subfolderOrganization;}
    public StorageSettings withDownloadFolderUri(String v){return new StorageSettings(v,videoFolderUri,audioFolderUri,tempFolderUri,warnOnLowSpace,filenameConflict,filenameTemplate,maxFilenameLength,subfolderOrganization);}
    public StorageSettings withVideoFolderUri(String v){return new StorageSettings(downloadFolderUri,v,audioFolderUri,tempFolderUri,warnOnLowSpace,filenameConflict,filenameTemplate,maxFilenameLength,subfolderOrganization);}
    public StorageSettings withAudioFolderUri(String v){return new StorageSettings(downloadFolderUri,videoFolderUri,v,tempFolderUri,warnOnLowSpace,filenameConflict,filenameTemplate,maxFilenameLength,subfolderOrganization);}
    public StorageSettings withTempFolderUri(String v){return new StorageSettings(downloadFolderUri,videoFolderUri,audioFolderUri,v,warnOnLowSpace,filenameConflict,filenameTemplate,maxFilenameLength,subfolderOrganization);}
    public StorageSettings withWarnOnLowSpace(boolean v){return new StorageSettings(downloadFolderUri,videoFolderUri,audioFolderUri,tempFolderUri,v,filenameConflict,filenameTemplate,maxFilenameLength,subfolderOrganization);}
    public StorageSettings withFilenameConflict(FilenameConflict v){return new StorageSettings(downloadFolderUri,videoFolderUri,audioFolderUri,tempFolderUri,warnOnLowSpace,v,filenameTemplate,maxFilenameLength,subfolderOrganization);}
    public StorageSettings withFilenameTemplate(String v){return new StorageSettings(downloadFolderUri,videoFolderUri,audioFolderUri,tempFolderUri,warnOnLowSpace,filenameConflict,v,maxFilenameLength,subfolderOrganization);}
    public StorageSettings withMaxFilenameLength(int v){return new StorageSettings(downloadFolderUri,videoFolderUri,audioFolderUri,tempFolderUri,warnOnLowSpace,filenameConflict,filenameTemplate,v,subfolderOrganization);}
    public StorageSettings withSubfolderOrganization(SubfolderOrganization v){return new StorageSettings(downloadFolderUri,videoFolderUri,audioFolderUri,tempFolderUri,warnOnLowSpace,filenameConflict,filenameTemplate,maxFilenameLength,v);}
    @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof StorageSettings))return false;StorageSettings x=(StorageSettings)o;return warnOnLowSpace==x.warnOnLowSpace&&maxFilenameLength==x.maxFilenameLength&&downloadFolderUri.equals(x.downloadFolderUri)&&videoFolderUri.equals(x.videoFolderUri)&&audioFolderUri.equals(x.audioFolderUri)&&tempFolderUri.equals(x.tempFolderUri)&&filenameConflict==x.filenameConflict&&filenameTemplate.equals(x.filenameTemplate)&&subfolderOrganization==x.subfolderOrganization;}
    @Override public int hashCode(){return Objects.hash(downloadFolderUri,videoFolderUri,audioFolderUri,tempFolderUri,warnOnLowSpace,filenameConflict,filenameTemplate,maxFilenameLength,subfolderOrganization);}
    @Override public String toString(){return "StorageSettings(downloadFolderUri="+downloadFolderUri+", videoFolderUri="+videoFolderUri+", audioFolderUri="+audioFolderUri+", tempFolderUri="+tempFolderUri+", warnOnLowSpace="+warnOnLowSpace+", filenameConflict="+filenameConflict+", filenameTemplate="+filenameTemplate+", maxFilenameLength="+maxFilenameLength+", subfolderOrganization="+subfolderOrganization+")";}
}
