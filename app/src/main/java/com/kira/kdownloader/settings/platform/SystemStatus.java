package com.kira.kdownloader.settings.platform;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.core.app.NotificationManagerCompat;

import java.util.Objects;

public final class SystemStatus {
    private final Context context;
    public SystemStatus(Context context){this.context=context.getApplicationContext();}
    public static final class Snapshot{private final boolean notificationsEnabled,ignoringBatteryOptimizations,backgroundRestricted,hasMediaAccess;public Snapshot(boolean a,boolean b,boolean c,boolean d){notificationsEnabled=a;ignoringBatteryOptimizations=b;backgroundRestricted=c;hasMediaAccess=d;}public boolean getNotificationsEnabled(){return notificationsEnabled;}public boolean getIgnoringBatteryOptimizations(){return ignoringBatteryOptimizations;}public boolean getBackgroundRestricted(){return backgroundRestricted;}public boolean getHasMediaAccess(){return hasMediaAccess;}@Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof Snapshot))return false;Snapshot x=(Snapshot)o;return notificationsEnabled==x.notificationsEnabled&&ignoringBatteryOptimizations==x.ignoringBatteryOptimizations&&backgroundRestricted==x.backgroundRestricted&&hasMediaAccess==x.hasMediaAccess;}@Override public int hashCode(){return Objects.hash(notificationsEnabled,ignoringBatteryOptimizations,backgroundRestricted,hasMediaAccess);}@Override public String toString(){return "Snapshot(notificationsEnabled="+notificationsEnabled+", ignoringBatteryOptimizations="+ignoringBatteryOptimizations+", backgroundRestricted="+backgroundRestricted+", hasMediaAccess="+hasMediaAccess+")";}}
    public Snapshot snapshot(){return new Snapshot(notificationsEnabled(),ignoringBatteryOptimizations(),backgroundRestricted(),hasMediaAccess());}
    public boolean notificationsEnabled(){return NotificationManagerCompat.from(context).areNotificationsEnabled();}
    public boolean ignoringBatteryOptimizations(){PowerManager manager=(PowerManager)context.getSystemService(Context.POWER_SERVICE);return manager==null||manager.isIgnoringBatteryOptimizations(context.getPackageName());}
    public boolean backgroundRestricted(){if(Build.VERSION.SDK_INT<Build.VERSION_CODES.P)return false;ActivityManager manager=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);if(manager==null)return false;try{return manager.isBackgroundRestricted();}catch(Throwable ignored){return false;}}
    public boolean hasMediaAccess(){if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU)return granted("android.permission.READ_MEDIA_VIDEO")&&granted("android.permission.READ_MEDIA_AUDIO");if(Build.VERSION.SDK_INT<=Build.VERSION_CODES.S_V2)return granted("android.permission.READ_EXTERNAL_STORAGE");return true;}
    private boolean granted(String permission){return context.checkSelfPermission(permission)==PackageManager.PERMISSION_GRANTED;}
    public boolean canDownloadReliablyInBackground(){return ignoringBatteryOptimizations()&&!backgroundRestricted();}
    public Intent notificationSettingsIntent(){return new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.getPackageName());}
    public Intent batteryOptimizationSettingsIntent(){return new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);}
    public Intent appDetailsSettingsIntent(){return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.fromParts("package",context.getPackageName(),null));}
    public Intent appLocaleSettingsIntent(){return Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU?new Intent(Settings.ACTION_APP_LOCALE_SETTINGS).setData(Uri.fromParts("package",context.getPackageName(),null)):null;}
}
