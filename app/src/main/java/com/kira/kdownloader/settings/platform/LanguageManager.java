package com.kira.kdownloader.settings.platform;

import android.app.LocaleManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class LanguageManager {
    public static final List<Language> SUPPORTED=Collections.unmodifiableList(Arrays.asList(
            new Language("","Follow system"),new Language("en","English"),new Language("es","Español"),
            new Language("fr","Français"),new Language("de","Deutsch"),new Language("pt","Português"),
            new Language("ru","Русский"),new Language("ar","العربية"),new Language("hi","हिन्दी"),
            new Language("id","Bahasa Indonesia"),new Language("ja","日本語"),new Language("zh","中文")));
    private LanguageManager(){}
    public static final class Language{private final String tag,display;public Language(String tag,String display){this.tag=tag;this.display=display;}public String getTag(){return tag;}public String getDisplay(){return display;}@Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof Language))return false;Language x=(Language)o;return tag.equals(x.tag)&&display.equals(x.display);}@Override public int hashCode(){return Objects.hash(tag,display);}@Override public String toString(){return "Language(tag="+tag+", display="+display+")";}}
    public static String displayName(String tag){for(Language language:SUPPORTED)if(language.tag.equals(tag))return language.display;if(!tag.trim().isEmpty())return Locale.forLanguageTag(tag).getDisplayName();return "Follow system";}
    public static void apply(Context context,String tag){if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){LocaleManager manager=context.getSystemService(LocaleManager.class);if(manager!=null)manager.setApplicationLocales(tag.trim().isEmpty()?LocaleList.getEmptyLocaleList():LocaleList.forLanguageTags(tag));}}
    public static Context wrap(Context base,String tag){if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU||tag.trim().isEmpty())return base;Locale locale=Locale.forLanguageTag(tag);Locale.setDefault(locale);Configuration configuration=new Configuration(base.getResources().getConfiguration());configuration.setLocale(locale);return new ContextWrapper(base.createConfigurationContext(configuration));}
}
