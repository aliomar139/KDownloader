package com.kira.kdownloader.settings;

import java.util.Objects;

public final class NotificationSettings {
    private final boolean showProgress, notifyOnEachComplete, notifyOnAllComplete, notifyOnFailure;
    private final boolean sound, vibration, showActions, groupNotifications;

    public NotificationSettings() { this(true, true, true, true, false, true, true, true); }
    public NotificationSettings(boolean showProgress, boolean notifyOnEachComplete, boolean notifyOnAllComplete,
                                boolean notifyOnFailure, boolean sound, boolean vibration,
                                boolean showActions, boolean groupNotifications) {
        this.showProgress = showProgress; this.notifyOnEachComplete = notifyOnEachComplete;
        this.notifyOnAllComplete = notifyOnAllComplete; this.notifyOnFailure = notifyOnFailure;
        this.sound = sound; this.vibration = vibration; this.showActions = showActions;
        this.groupNotifications = groupNotifications;
    }
    public boolean getShowProgress() { return showProgress; }
    public boolean getNotifyOnEachComplete() { return notifyOnEachComplete; }
    public boolean getNotifyOnAllComplete() { return notifyOnAllComplete; }
    public boolean getNotifyOnFailure() { return notifyOnFailure; }
    public boolean getSound() { return sound; }
    public boolean getVibration() { return vibration; }
    public boolean getShowActions() { return showActions; }
    public boolean getGroupNotifications() { return groupNotifications; }
    public NotificationSettings withShowProgress(boolean v) { return new NotificationSettings(v, notifyOnEachComplete, notifyOnAllComplete, notifyOnFailure, sound, vibration, showActions, groupNotifications); }
    public NotificationSettings withNotifyOnEachComplete(boolean v) { return new NotificationSettings(showProgress, v, notifyOnAllComplete, notifyOnFailure, sound, vibration, showActions, groupNotifications); }
    public NotificationSettings withNotifyOnAllComplete(boolean v) { return new NotificationSettings(showProgress, notifyOnEachComplete, v, notifyOnFailure, sound, vibration, showActions, groupNotifications); }
    public NotificationSettings withNotifyOnFailure(boolean v) { return new NotificationSettings(showProgress, notifyOnEachComplete, notifyOnAllComplete, v, sound, vibration, showActions, groupNotifications); }
    public NotificationSettings withSound(boolean v) { return new NotificationSettings(showProgress, notifyOnEachComplete, notifyOnAllComplete, notifyOnFailure, v, vibration, showActions, groupNotifications); }
    public NotificationSettings withVibration(boolean v) { return new NotificationSettings(showProgress, notifyOnEachComplete, notifyOnAllComplete, notifyOnFailure, sound, v, showActions, groupNotifications); }
    public NotificationSettings withShowActions(boolean v) { return new NotificationSettings(showProgress, notifyOnEachComplete, notifyOnAllComplete, notifyOnFailure, sound, vibration, v, groupNotifications); }
    public NotificationSettings withGroupNotifications(boolean v) { return new NotificationSettings(showProgress, notifyOnEachComplete, notifyOnAllComplete, notifyOnFailure, sound, vibration, showActions, v); }
    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof NotificationSettings)) return false; NotificationSettings x=(NotificationSettings)o; return showProgress==x.showProgress&&notifyOnEachComplete==x.notifyOnEachComplete&&notifyOnAllComplete==x.notifyOnAllComplete&&notifyOnFailure==x.notifyOnFailure&&sound==x.sound&&vibration==x.vibration&&showActions==x.showActions&&groupNotifications==x.groupNotifications; }
    @Override public int hashCode() { return Objects.hash(showProgress,notifyOnEachComplete,notifyOnAllComplete,notifyOnFailure,sound,vibration,showActions,groupNotifications); }
    @Override public String toString(){return "NotificationSettings(showProgress="+showProgress+", notifyOnEachComplete="+notifyOnEachComplete+", notifyOnAllComplete="+notifyOnAllComplete+", notifyOnFailure="+notifyOnFailure+", sound="+sound+", vibration="+vibration+", showActions="+showActions+", groupNotifications="+groupNotifications+")";}
}
