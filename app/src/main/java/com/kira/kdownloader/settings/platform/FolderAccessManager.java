package com.kira.kdownloader.settings.platform;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.Locale;

public final class FolderAccessManager {
    private static final String TAG = "FolderAccessManager";
    private final Context context;
    public FolderAccessManager(Context context){this.context=context.getApplicationContext();}
    public void persist(Uri uri){int flags=Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION;try{context.getContentResolver().takePersistableUriPermission(uri,flags);}catch(Throwable e){Log.w(TAG,"Could not persist permission for "+uri,e);}}
    public void release(String value){if(value.isEmpty())return;Uri uri=parse(value);if(uri==null)return;try{context.getContentResolver().releasePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}catch(Throwable ignored){}}
    public boolean hasAccess(String value){if(value.isEmpty())return false;Uri uri=parse(value);if(uri==null)return false;boolean granted=false;for(android.content.UriPermission permission:context.getContentResolver().getPersistedUriPermissions()){if(uri.equals(permission.getUri())&&permission.isWritePermission()){granted=true;break;}}if(!granted)return false;try{DocumentFile file=DocumentFile.fromTreeUri(context,uri);return file!=null&&file.canWrite();}catch(Throwable ignored){return false;}}
    public String displayName(String value){if(value.isEmpty())return "Not set";Uri uri=parse(value);if(uri==null)return "Unknown folder";try{DocumentFile file=DocumentFile.fromTreeUri(context,uri);String name=file==null?null:file.getName();if(name!=null&&!name.trim().isEmpty())return name;}catch(Throwable ignored){}String decoded=Uri.decode(uri.toString());int colon=decoded.lastIndexOf(':');String result=decoded.substring(colon+1);int slash=result.lastIndexOf('/');result=result.substring(slash+1);return result.trim().isEmpty()?"Selected folder":result;}
    public long availableBytes(){try{File dir=Environment.getExternalStorageDirectory();if(dir==null)dir=Environment.getDataDirectory();StatFs stat=new StatFs(dir.getPath());return stat.getAvailableBlocksLong()*stat.getBlockSizeLong();}catch(Throwable ignored){return -1L;}}
    public long totalBytes(){try{File dir=Environment.getExternalStorageDirectory();if(dir==null)dir=Environment.getDataDirectory();StatFs stat=new StatFs(dir.getPath());return stat.getBlockCountLong()*stat.getBlockSizeLong();}catch(Throwable ignored){return -1L;}}
    private static Uri parse(String value){try{return Uri.parse(value);}catch(Throwable ignored){return null;}}
    public static String formatBytes(long bytes){if(bytes<0)return "Unknown";if(bytes<1024)return bytes+" B";String[] units={"KB","MB","GB","TB"};double value=bytes/1024.0;int unit=0;while(value>=1024.0&&unit<units.length-1){value/=1024.0;unit++;}return String.format(Locale.US,"%.1f %s",value,units[unit]);}
}
