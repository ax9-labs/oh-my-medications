package com.personal.medtracker;

import android.app.*;
import android.content.*;
import java.time.*;

public final class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        long id=i.getLongExtra("med",-1); String time=i.getStringExtra("time");
        if(time==null) return;
        try(Store s=new Store(c)) {
            Store.Med m=s.med(id);
            if(m==null || !m.reminders || !Schedule.parseTimes(m.times).contains(time)) return;
            Reminders.schedule(c,m,time);
            if(!Treatment.includes(m.startDay,m.durationDays,LocalDate.now()))return;
            s.ensurePlans(LocalDate.now().plusDays(1),System.currentTimeMillis());
            String day=LocalDate.now().toString();
            if(s.handled(id,day,time)||!Reminders.allowed(c)) return;
            // AlarmManager never fires early; stale alarms from a previous day are not shown.
            if(LocalTime.now().isBefore(LocalTime.parse(time))) return;
            Reminders.channel(c);
            if(!AlarmService.start(c,id,day,time,false)) Reminders.fallback(c,id,time);
        }
    }
}
