package com.kira.kdownloader.settings;

import java.util.Objects;

public final class NetworkSettings {
    private final NetworkType allowedNetworks; private final boolean allowRoaming,confirmMobileData;
    private final int mobileDataWarningMb; private final boolean treatMeteredWifiAsMobile,pauseOnNetworkChange,retryAfterConnectionLoss;
    private final ProxyType proxyType; private final String proxyHost; private final int proxyPort;
    private final String proxyUsername; private final boolean proxyPasswordSet;
    public NetworkSettings(){this(NetworkType.WIFI_AND_MOBILE,false,true,100,true,false,true,ProxyType.DISABLED,"",0,"",false);}
    public NetworkSettings(NetworkType allowedNetworks,boolean allowRoaming,boolean confirmMobileData,int mobileDataWarningMb,
                           boolean treatMeteredWifiAsMobile,boolean pauseOnNetworkChange,boolean retryAfterConnectionLoss,
                           ProxyType proxyType,String proxyHost,int proxyPort,String proxyUsername,boolean proxyPasswordSet){this.allowedNetworks=allowedNetworks;this.allowRoaming=allowRoaming;this.confirmMobileData=confirmMobileData;this.mobileDataWarningMb=mobileDataWarningMb;this.treatMeteredWifiAsMobile=treatMeteredWifiAsMobile;this.pauseOnNetworkChange=pauseOnNetworkChange;this.retryAfterConnectionLoss=retryAfterConnectionLoss;this.proxyType=proxyType;this.proxyHost=proxyHost;this.proxyPort=proxyPort;this.proxyUsername=proxyUsername;this.proxyPasswordSet=proxyPasswordSet;}
    public NetworkType getAllowedNetworks(){return allowedNetworks;} public boolean getAllowRoaming(){return allowRoaming;}
    public boolean getConfirmMobileData(){return confirmMobileData;} public int getMobileDataWarningMb(){return mobileDataWarningMb;}
    public boolean getTreatMeteredWifiAsMobile(){return treatMeteredWifiAsMobile;} public boolean getPauseOnNetworkChange(){return pauseOnNetworkChange;}
    public boolean getRetryAfterConnectionLoss(){return retryAfterConnectionLoss;} public ProxyType getProxyType(){return proxyType;}
    public String getProxyHost(){return proxyHost;} public int getProxyPort(){return proxyPort;}
    public String getProxyUsername(){return proxyUsername;} public boolean getProxyPasswordSet(){return proxyPasswordSet;}
    public NetworkSettings withAllowedNetworks(NetworkType v){return new NetworkSettings(v,allowRoaming,confirmMobileData,mobileDataWarningMb,treatMeteredWifiAsMobile,pauseOnNetworkChange,retryAfterConnectionLoss,proxyType,proxyHost,proxyPort,proxyUsername,proxyPasswordSet);}
    public NetworkSettings withAllowRoaming(boolean v){return new NetworkSettings(allowedNetworks,v,confirmMobileData,mobileDataWarningMb,treatMeteredWifiAsMobile,pauseOnNetworkChange,retryAfterConnectionLoss,proxyType,proxyHost,proxyPort,proxyUsername,proxyPasswordSet);}
    public NetworkSettings withConfirmMobileData(boolean v){return new NetworkSettings(allowedNetworks,allowRoaming,v,mobileDataWarningMb,treatMeteredWifiAsMobile,pauseOnNetworkChange,retryAfterConnectionLoss,proxyType,proxyHost,proxyPort,proxyUsername,proxyPasswordSet);}
    public NetworkSettings withMobileDataWarningMb(int v){return new NetworkSettings(allowedNetworks,allowRoaming,confirmMobileData,v,treatMeteredWifiAsMobile,pauseOnNetworkChange,retryAfterConnectionLoss,proxyType,proxyHost,proxyPort,proxyUsername,proxyPasswordSet);}
    public NetworkSettings withTreatMeteredWifiAsMobile(boolean v){return new NetworkSettings(allowedNetworks,allowRoaming,confirmMobileData,mobileDataWarningMb,v,pauseOnNetworkChange,retryAfterConnectionLoss,proxyType,proxyHost,proxyPort,proxyUsername,proxyPasswordSet);}
    public NetworkSettings withPauseOnNetworkChange(boolean v){return new NetworkSettings(allowedNetworks,allowRoaming,confirmMobileData,mobileDataWarningMb,treatMeteredWifiAsMobile,v,retryAfterConnectionLoss,proxyType,proxyHost,proxyPort,proxyUsername,proxyPasswordSet);}
    public NetworkSettings withRetryAfterConnectionLoss(boolean v){return new NetworkSettings(allowedNetworks,allowRoaming,confirmMobileData,mobileDataWarningMb,treatMeteredWifiAsMobile,pauseOnNetworkChange,v,proxyType,proxyHost,proxyPort,proxyUsername,proxyPasswordSet);}
    public NetworkSettings withProxyType(ProxyType v){return new NetworkSettings(allowedNetworks,allowRoaming,confirmMobileData,mobileDataWarningMb,treatMeteredWifiAsMobile,pauseOnNetworkChange,retryAfterConnectionLoss,v,proxyHost,proxyPort,proxyUsername,proxyPasswordSet);}
    public NetworkSettings withProxyHost(String v){return new NetworkSettings(allowedNetworks,allowRoaming,confirmMobileData,mobileDataWarningMb,treatMeteredWifiAsMobile,pauseOnNetworkChange,retryAfterConnectionLoss,proxyType,v,proxyPort,proxyUsername,proxyPasswordSet);}
    public NetworkSettings withProxyPort(int v){return new NetworkSettings(allowedNetworks,allowRoaming,confirmMobileData,mobileDataWarningMb,treatMeteredWifiAsMobile,pauseOnNetworkChange,retryAfterConnectionLoss,proxyType,proxyHost,v,proxyUsername,proxyPasswordSet);}
    public NetworkSettings withProxyUsername(String v){return new NetworkSettings(allowedNetworks,allowRoaming,confirmMobileData,mobileDataWarningMb,treatMeteredWifiAsMobile,pauseOnNetworkChange,retryAfterConnectionLoss,proxyType,proxyHost,proxyPort,v,proxyPasswordSet);}
    public NetworkSettings withProxyPasswordSet(boolean v){return new NetworkSettings(allowedNetworks,allowRoaming,confirmMobileData,mobileDataWarningMb,treatMeteredWifiAsMobile,pauseOnNetworkChange,retryAfterConnectionLoss,proxyType,proxyHost,proxyPort,proxyUsername,v);}
    @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof NetworkSettings))return false;NetworkSettings x=(NetworkSettings)o;return allowRoaming==x.allowRoaming&&confirmMobileData==x.confirmMobileData&&mobileDataWarningMb==x.mobileDataWarningMb&&treatMeteredWifiAsMobile==x.treatMeteredWifiAsMobile&&pauseOnNetworkChange==x.pauseOnNetworkChange&&retryAfterConnectionLoss==x.retryAfterConnectionLoss&&proxyPort==x.proxyPort&&proxyPasswordSet==x.proxyPasswordSet&&allowedNetworks==x.allowedNetworks&&proxyType==x.proxyType&&proxyHost.equals(x.proxyHost)&&proxyUsername.equals(x.proxyUsername);}
    @Override public int hashCode(){return Objects.hash(allowedNetworks,allowRoaming,confirmMobileData,mobileDataWarningMb,treatMeteredWifiAsMobile,pauseOnNetworkChange,retryAfterConnectionLoss,proxyType,proxyHost,proxyPort,proxyUsername,proxyPasswordSet);}
    @Override public String toString(){return "NetworkSettings(allowedNetworks="+allowedNetworks+", allowRoaming="+allowRoaming+", confirmMobileData="+confirmMobileData+", mobileDataWarningMb="+mobileDataWarningMb+", treatMeteredWifiAsMobile="+treatMeteredWifiAsMobile+", pauseOnNetworkChange="+pauseOnNetworkChange+", retryAfterConnectionLoss="+retryAfterConnectionLoss+", proxyType="+proxyType+", proxyHost="+proxyHost+", proxyPort="+proxyPort+", proxyUsername="+proxyUsername+", proxyPasswordSet="+proxyPasswordSet+")";}
}
