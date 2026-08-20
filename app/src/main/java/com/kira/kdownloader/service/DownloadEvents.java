package com.kira.kdownloader.service;

import com.kira.kdownloader.util.StateHolder;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class DownloadEvents {
    public enum Phase{PREPARING,RUNNING,COMPLETED,FAILED}
    public static final class State{private final Phase phase;private final int percent;private final String title,kind,fileUri,message;private final long etaSeconds;private final String processId;public State(Phase phase,int percent,String title,String kind,String fileUri,String message,long etaSeconds,String processId){this.phase=phase;this.percent=percent;this.title=title;this.kind=kind;this.fileUri=fileUri;this.message=message;this.etaSeconds=etaSeconds;this.processId=processId;}public Phase getPhase(){return phase;}public int getPercent(){return percent;}public String getTitle(){return title;}public String getKind(){return kind;}public String getFileUri(){return fileUri;}public String getMessage(){return message;}public long getEtaSeconds(){return etaSeconds;}public String getProcessId(){return processId;}@Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof State))return false;State x=(State)o;return percent==x.percent&&etaSeconds==x.etaSeconds&&phase==x.phase&&title.equals(x.title)&&kind.equals(x.kind)&&Objects.equals(fileUri,x.fileUri)&&Objects.equals(message,x.message)&&Objects.equals(processId,x.processId);}@Override public int hashCode(){return Objects.hash(phase,percent,title,kind,fileUri,message,etaSeconds,processId);}@Override public String toString(){return "State(phase="+phase+", percent="+percent+", title="+title+", kind="+kind+", fileUri="+fileUri+", message="+message+", etaSeconds="+etaSeconds+", processId="+processId+")";}}
    private static final StateHolder<Map<String,State>> STATES=new StateHolder<>(Collections.emptyMap());
    private DownloadEvents(){}
    public static StateHolder<Map<String,State>> getStates(){return STATES;}
    public static String keyOf(String url,String formatLabel){return url+"|"+formatLabel;}
    public static void update(String key,State state){STATES.update(current->{Map<String,State> next=new LinkedHashMap<>(current);next.put(key,state);return Collections.unmodifiableMap(next);});}
    public static void clear(String key){STATES.update(current->{Map<String,State> next=new LinkedHashMap<>(current);next.remove(key);return Collections.unmodifiableMap(next);});}
}
