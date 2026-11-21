package com.personal.medtracker;
import java.time.*;
import java.time.temporal.ChronoUnit;

/** Calendar-day courses and wall-clock scheduling; duration zero means no end date. */
public final class Treatment {
    private Treatment(){}
    public static LocalDate end(String start,int days){return days<=0?null:LocalDate.parse(start).plusDays(days-1L);}
    public static boolean includes(String start,int days,LocalDate day){LocalDate s=LocalDate.parse(start);LocalDate e=end(start,days);return !day.isBefore(s)&&(e==null||!day.isAfter(e));}
    public static int dayNumber(String start,int days,LocalDate today){if(days<=0)return 0;return (int)Math.max(0,Math.min(days,ChronoUnit.DAYS.between(LocalDate.parse(start),today)+1));}
    public static float progress(String start,int days,LocalDate today){return days<=0?0:dayNumber(start,days,today)/(float)days;}
    public static long scheduled(String day,String time,ZoneId zone){return LocalDate.parse(day).atTime(LocalTime.parse(time)).atZone(zone).toInstant().toEpochMilli();}
    public static long next(String time,String start,int days,long now,ZoneId zone){
        LocalDate first=Instant.ofEpochMilli(now).atZone(zone).toLocalDate();LocalDate begins=LocalDate.parse(start);if(first.isBefore(begins))first=begins;
        if(!includes(start,days,first))return -1;
        long when=scheduled(first.toString(),time,zone);if(when<=now){first=first.plusDays(1);if(!includes(start,days,first))return -1;when=scheduled(first.toString(),time,zone);}return when;
    }
}
