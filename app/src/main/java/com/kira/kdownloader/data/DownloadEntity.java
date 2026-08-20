package com.kira.kdownloader.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

@Entity(tableName = "downloads")
public final class DownloadEntity {
    @PrimaryKey(autoGenerate = true) private final long id;
    @NonNull private final String title;
    @NonNull private final String sourceUrl;
    @NonNull private final String kind;
    @NonNull private final String formatLabel;
    @Nullable private final String fileUri;
    @Nullable private final String thumbnailUrl;
    private final long createdAt;
    @NonNull private final DownloadStatus status;

    public DownloadEntity(long id, @NonNull String title, @NonNull String sourceUrl,
                          @NonNull String kind, @NonNull String formatLabel,
                          @Nullable String fileUri, @Nullable String thumbnailUrl,
                          long createdAt, @NonNull DownloadStatus status) {
        this.id=id; this.title=title; this.sourceUrl=sourceUrl; this.kind=kind; this.formatLabel=formatLabel;
        this.fileUri=fileUri; this.thumbnailUrl=thumbnailUrl; this.createdAt=createdAt; this.status=status;
    }
    public long getId(){return id;} @NonNull public String getTitle(){return title;} @NonNull public String getSourceUrl(){return sourceUrl;}
    @NonNull public String getKind(){return kind;} @NonNull public String getFormatLabel(){return formatLabel;} @Nullable public String getFileUri(){return fileUri;}
    @Nullable public String getThumbnailUrl(){return thumbnailUrl;} public long getCreatedAt(){return createdAt;} @NonNull public DownloadStatus getStatus(){return status;}
    @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof DownloadEntity))return false;DownloadEntity x=(DownloadEntity)o;return id==x.id&&createdAt==x.createdAt&&title.equals(x.title)&&sourceUrl.equals(x.sourceUrl)&&kind.equals(x.kind)&&formatLabel.equals(x.formatLabel)&&Objects.equals(fileUri,x.fileUri)&&Objects.equals(thumbnailUrl,x.thumbnailUrl)&&status==x.status;}
    @Override public int hashCode(){return Objects.hash(id,title,sourceUrl,kind,formatLabel,fileUri,thumbnailUrl,createdAt,status);}
    @Override public String toString(){return "DownloadEntity(id="+id+", title="+title+", sourceUrl="+sourceUrl+", kind="+kind+", formatLabel="+formatLabel+", fileUri="+fileUri+", thumbnailUrl="+thumbnailUrl+", createdAt="+createdAt+", status="+status+")";}
}
