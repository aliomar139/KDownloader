package com.kira.kdownloader.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "downloads")
public final class DownloadEntity {
    @PrimaryKey(autoGenerate = true) private final long id;
    private final String title;
    private final String sourceUrl;
    private final String kind;
    private final String formatLabel;
    private final String fileUri;
    private final String thumbnailUrl;
    private final long createdAt;
    private final DownloadStatus status;

    public DownloadEntity(long id, String title, String sourceUrl, String kind, String formatLabel,
                          String fileUri, String thumbnailUrl, long createdAt, DownloadStatus status) {
        this.id=id; this.title=title; this.sourceUrl=sourceUrl; this.kind=kind; this.formatLabel=formatLabel;
        this.fileUri=fileUri; this.thumbnailUrl=thumbnailUrl; this.createdAt=createdAt; this.status=status;
    }
    public long getId(){return id;} public String getTitle(){return title;} public String getSourceUrl(){return sourceUrl;}
    public String getKind(){return kind;} public String getFormatLabel(){return formatLabel;} public String getFileUri(){return fileUri;}
    public String getThumbnailUrl(){return thumbnailUrl;} public long getCreatedAt(){return createdAt;} public DownloadStatus getStatus(){return status;}
    @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof DownloadEntity))return false;DownloadEntity x=(DownloadEntity)o;return id==x.id&&createdAt==x.createdAt&&title.equals(x.title)&&sourceUrl.equals(x.sourceUrl)&&kind.equals(x.kind)&&formatLabel.equals(x.formatLabel)&&Objects.equals(fileUri,x.fileUri)&&Objects.equals(thumbnailUrl,x.thumbnailUrl)&&status==x.status;}
    @Override public int hashCode(){return Objects.hash(id,title,sourceUrl,kind,formatLabel,fileUri,thumbnailUrl,createdAt,status);}
    @Override public String toString(){return "DownloadEntity(id="+id+", title="+title+", sourceUrl="+sourceUrl+", kind="+kind+", formatLabel="+formatLabel+", fileUri="+fileUri+", thumbnailUrl="+thumbnailUrl+", createdAt="+createdAt+", status="+status+")";}
}
