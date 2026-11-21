package com.personal.medtracker;
import java.time.*;
import java.util.*;

/** Reported timing and unlogged plans are distinct. Future pending doses never count as missing. */
public final class Adherence {
    public static final int WINDOW_MINUTES=30;
    public static final long WINDOW_MS=WINDOW_MINUTES*60_000L;
    public static final class Sample {
        public String day,status;public long scheduled,taken;public boolean tracked;
        public Sample(String day,long scheduled,long taken,String status,boolean tracked){this.day=day;this.scheduled=scheduled;this.taken=taken;this.status=status;this.tracked=tracked;}
    }
    public static final class Day {
        public final LocalDate date;public int onTime,outside,skipped,unlogged;
        Day(LocalDate date){this.date=date;}
        public int total(){return onTime+outside+skipped+unlogged;}
    }
    public static final class Report {
        public final List<Day> days=new ArrayList<>();public int onTime,outside,skipped,unlogged,taken,knownTiming;public long absoluteDifferenceMs;
        public int accuracy(){return knownTiming==0?-1:Math.round(onTime*100f/knownTiming);}
        public long averageDifferenceMinutes(){return knownTiming==0?-1:Math.round(absoluteDifferenceMs/(double)knownTiming/60_000);}
        public int eligible(){return taken+skipped+unlogged;}
    }
    public static Report summarize(List<Sample> samples,LocalDate from,LocalDate to,long now){
        Report r=new Report();Map<String,Day> byDay=new HashMap<>();
        for(LocalDate d=from;!d.isAfter(to);d=d.plusDays(1)){Day p=new Day(d);r.days.add(p);byDay.put(d.toString(),p);}
        for(Sample s:samples){Day d=byDay.get(s.day);if(d==null)continue;
            if("taken".equals(s.status)){r.taken++;if(s.taken>0&&s.scheduled>0){r.knownTiming++;long difference=Math.abs(s.taken-s.scheduled);r.absoluteDifferenceMs+=difference;if(difference<=WINDOW_MS){d.onTime++;r.onTime++;}else{d.outside++;r.outside++;}}}
            else if("skipped".equals(s.status)){d.skipped++;r.skipped++;}
            else if(s.tracked&&s.scheduled+WINDOW_MS<now){d.unlogged++;r.unlogged++;}
        }return r;
    }
}
