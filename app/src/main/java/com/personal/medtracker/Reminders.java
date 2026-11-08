package com.personal.medtracker;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import java.time.*;

final class Reminders {
    static final String CHANNEL = "scheduled_doses";
    static final String ALARM_CHANNEL = "ringing_doses_v1";
    static void channel(Context c) {
        NotificationChannel ch = new NotificationChannel(CHANNEL,AppLocale.text(c,R.string.reminder_channel),NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription(AppLocale.text(c,R.string.reminder_channel_description)); ch.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        c.getSystemService(NotificationManager.class).createNotificationChannel(ch);
        NotificationChannel alarms = new NotificationChannel(ALARM_CHANNEL,AppLocale.text(c,R.string.alarm_channel),NotificationManager.IMPORTANCE_HIGH);
        alarms.setDescription(AppLocale.text(c,R.string.alarm_channel_description));
        alarms.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        // The foreground service supplies a looping alarm sound, independently of notification sounds.
        alarms.setSound(null,null); alarms.enableVibration(false);
        c.getSystemService(NotificationManager.class).createNotificationChannel(alarms);
    }
    static boolean allowed(Context c) {
        if (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel ch = nm.getNotificationChannel(ALARM_CHANNEL);
        return nm.areNotificationsEnabled() && (ch == null || ch.getImportance() != NotificationManager.IMPORTANCE_NONE);
    }
    static boolean precise(Context c) { return Build.VERSION.SDK_INT < 31 || c.getSystemService(AlarmManager.class).canScheduleExactAlarms(); }
    static boolean fullScreen(Context c) { return Build.VERSION.SDK_INT < 34 || c.getSystemService(NotificationManager.class).canUseFullScreenIntent(); }
    static PendingIntent pending(Context c, long id, String time) {
        Intent i = new Intent(c, ReminderReceiver.class).setData(Uri.parse("medtracker://dose/"+id+"/"+time)).putExtra("med",id).putExtra("time",time);
        return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static void schedule(Context c, Store.Med m, String time) {
        if (!m.reminders) return;
        long when=Treatment.next(time,m.startDay,m.durationDays,System.currentTimeMillis(),ZoneId.systemDefault());
        // Finishing a course removes future triggers without silencing its final ringing dose.
        if(when<0){c.getSystemService(AlarmManager.class).cancel(pending(c,m.id,time));return;}
        scheduleAt(c,m,time,when);
    }
    static void scheduleAt(Context c,Store.Med m,String time,long when) {
        if(!m.reminders)return;
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent pi = pending(c,m.id,time);
        try { if (precise(c)) am.setAlarmClock(new AlarmManager.AlarmClockInfo(when,openTracker(c)),pi); else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi); }
        catch (SecurityException ex) { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi); }
    }
    static void reschedule(Context c) {
        channel(c);
        try (Store s = new Store(c)) {s.ensurePlans(LocalDate.now().plusDays(30),System.currentTimeMillis());for(Store.Med m:s.meds()) for(String time:Schedule.parseTimes(m.times)) schedule(c,m,time); }
    }
    static void cancel(Context c, Store.Med m) {
        for(String time:Schedule.parseTimes(m.times)) { c.getSystemService(AlarmManager.class).cancel(pending(c,m.id,time)); dismiss(c,m.id,time); }
    }
    static PendingIntent openTracker(Context c) {
        Intent open=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static void dismiss(Context c,long id,String time) {
        c.getSystemService(NotificationManager.class).cancel(id+"/"+time,1);
        AlarmService.dismissDose(id,time);
    }
    static void fallback(Context c,long id,String time) {
        Notification n=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_notification).setCategory(Notification.CATEGORY_ALARM)
            .setContentTitle(AppLocale.text(c,R.string.dose_due_title)).setContentText(AppLocale.text(c,R.string.dose_due_fallback))
            .setContentIntent(openTracker(c)).setVisibility(Notification.VISIBILITY_PRIVATE).setAutoCancel(true).build();
        try {c.getSystemService(NotificationManager.class).notify(id+"/"+time,1,n);}catch(SecurityException ignored){}
    }
}
