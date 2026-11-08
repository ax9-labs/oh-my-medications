package com.personal.medtracker;

import android.app.LocaleManager;
import android.content.*;
import android.content.res.Configuration;
import android.os.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Shared locale handling for activities, services, receivers, formatting and future languages. */
public final class AppLocale {
    private static final String PREFS="app-language",KEY="language";
    private AppLocale(){}
    public static String tag(Context c){
        if(Build.VERSION.SDK_INT>=33){
            LocaleList selected=c.getSystemService(LocaleManager.class).getApplicationLocales();
            // An empty system selection deliberately means English, not the device language.
            return selected.isEmpty()?"en":supported(c,selected.get(0).toLanguageTag());
        }
        return supported(c,c.getSharedPreferences(PREFS,0).getString(KEY,"en"));
    }
    public static String supported(Context c,String requested){
        String[] choices=c.getResources().getStringArray(R.array.language_tags);
        for(String tag:choices)if(tag.equalsIgnoreCase(requested))return tag;
        String language=Locale.forLanguageTag(requested==null?"":requested).getLanguage();
        for(String tag:choices)if(tag.equals(language))return tag;
        return "en";
    }
    public static Locale locale(Context c){return Locale.forLanguageTag(tag(c));}
    public static Context wrap(Context c){
        Locale locale=locale(c);Configuration current=c.getResources().getConfiguration();
        if(current.getLocales().get(0).equals(locale))return c;
        Configuration config=new Configuration(current);config.setLocale(locale);config.setLayoutDirection(locale);
        return c.createConfigurationContext(config);
    }
    public static void set(Context c,String tag){
        String selected=supported(c,tag);c.getSharedPreferences(PREFS,0).edit().putString(KEY,selected).apply();
        if(Build.VERSION.SDK_INT>=33)c.getSystemService(LocaleManager.class).setApplicationLocales(LocaleList.forLanguageTags(selected));
    }
    public static String text(Context c,int id,Object... args){return wrap(c).getString(id,args);}
    public static String date(Context c,LocalDate day,boolean year){
        String pattern=android.text.format.DateFormat.getBestDateTimePattern(locale(c),year?"yMMMEEEEd":"EEEEMMMMd");
        return day.format(DateTimeFormatter.ofPattern(pattern,locale(c)));
    }
    public static String time(Context c,String time){
        String pattern=android.text.format.DateFormat.getBestDateTimePattern(locale(c),android.text.format.DateFormat.is24HourFormat(c)?"Hm":"hm");
        return LocalTime.parse(time).format(DateTimeFormatter.ofPattern(pattern,locale(c)));
    }
    public static String timestamp(Context c,long millis){
        String date=android.text.format.DateFormat.getBestDateTimePattern(locale(c),"yMMMd");
        String time=android.text.format.DateFormat.getBestDateTimePattern(locale(c),android.text.format.DateFormat.is24HourFormat(c)?"Hm":"hm");
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(date+" · "+time+" z",locale(c)));
    }
}
