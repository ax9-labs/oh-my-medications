package com.personal.medtracker;
import android.content.Context;
final class TimingText {
    private TimingText(){}
    static String difference(Context c,Store.Event e){
        if(e.takenAt<=0||e.scheduledAt<=0)return AppLocale.text(c,R.string.actual_unknown);
        long millis=e.takenAt-e.scheduledAt,minutes=Math.round(Math.abs(millis)/60_000d);
        if(minutes==0)return AppLocale.text(c,R.string.at_scheduled_time);
        return AppLocale.text(c,millis<0?R.string.minutes_early:R.string.minutes_late,minutes);
    }
}
