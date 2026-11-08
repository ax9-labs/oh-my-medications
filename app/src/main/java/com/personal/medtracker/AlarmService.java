package com.personal.medtracker;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.*;
import android.net.Uri;
import android.os.*;
import java.util.*;

/** One foreground player for all currently due doses. Stopping audio never logs a dose. */
public final class AlarmService extends Service {
    static final int NOTIFICATION_ID=71;
    private static volatile AlarmService current;
    private final Set<String> slots=new HashSet<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private AudioManager audio;
    private AudioFocusRequest focus;
    private Vibrator vibrator;
    private boolean hasFocus,usingFallback;
    private static final AudioAttributes ATTRS=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();

    static boolean start(Context c,long id,String day,String time,boolean test) {
        Intent i=new Intent(c,AlarmService.class).putExtra("med",id).putExtra("day",day).putExtra("time",time).putExtra("test",test);
        try {c.startForegroundService(i);return true;}catch(IllegalStateException|SecurityException ex){return false;}
    }
    static void stop(Context c) {c.stopService(new Intent(c,AlarmService.class));}
    static boolean ringing() {return current!=null;}
    static void refreshLocale(){AlarmService s=current;if(s!=null)s.handler.post(()->{if(current==s)s.getSystemService(NotificationManager.class).notify(NOTIFICATION_ID,s.notification());});}
    static boolean playing() {AlarmService s=current;return s!=null&&s.player!=null&&s.player.isPlaying();}
    static void dismissDose(long id,String time) {
        AlarmService s=current;
        if(s!=null)s.handler.post(()->{
            s.slots.removeIf(key->key.startsWith(id+"/")&&key.endsWith("/"+time));
            if(s.slots.isEmpty())s.stopSelf();
        });
    }
    @Override public void onCreate() {super.onCreate();current=this;audio=getSystemService(AudioManager.class);}
    @Override public IBinder onBind(Intent i) {return null;}
    @Override public int onStartCommand(Intent i,int flags,int startId) {
        if(i==null){stopSelf();return START_NOT_STICKY;}
        Reminders.channel(this);
        Notification n=notification();
        if(Build.VERSION.SDK_INT>=29)startForeground(NOTIFICATION_ID,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else startForeground(NOTIFICATION_ID,n);
        long id=i.getLongExtra("med",-1);String day=i.getStringExtra("day"),time=i.getStringExtra("time");
        boolean test=i.getBooleanExtra("test",false);
        if(!test) {
            // Recheck after asynchronous service startup: a dose can be logged or archived in between.
            try(Store s=new Store(this)) {
                Store.Med m=s.med(id);
                if(m==null||!m.reminders||time==null||day==null||!Treatment.includes(m.startDay,m.durationDays,java.time.LocalDate.parse(day))||!Schedule.parseTimes(m.times).contains(time)||s.handled(id,day,time)) {
                    if(slots.isEmpty())stopSelf();return START_NOT_STICKY;
                }
            }
        }
        slots.add(test?"test":Schedule.key(id,day,time));
        if(focus==null) {
            focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(ATTRS).setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(change->{
                    hasFocus=change==AudioManager.AUDIOFOCUS_GAIN;
                    if(hasFocus){if(player==null)playDefault();else if(!player.isPlaying())player.start();}
                    else if(player!=null&&player.isPlaying())player.pause();
                },handler).build();
            hasFocus=audio.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            if(hasFocus)playDefault();
            vibrator=getSystemService(Vibrator.class);
            if(vibrator!=null&&vibrator.hasVibrator())vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0,450,650},0),ATTRS);
        }
        return START_NOT_STICKY;
    }
    private Notification notification() {
        Intent screen=new Intent(this,AlarmActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent open=PendingIntent.getActivity(this,71,screen,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent stop=new Intent(this,AlarmActions.class).setAction("com.personal.medtracker.STOP_ALARM");
        PendingIntent stopAction=PendingIntent.getBroadcast(this,71,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=new Notification.Builder(this,Reminders.ALARM_CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(AppLocale.text(this,R.string.alarm_title)).setContentText(AppLocale.text(this,R.string.alarm_notification_text))
            .setCategory(Notification.CATEGORY_ALARM).setVisibility(Notification.VISIBILITY_PRIVATE).setOngoing(true).setOnlyAlertOnce(true)
            .setContentIntent(open).setDeleteIntent(stopAction).setFullScreenIntent(open,true)
            .addAction(new Notification.Action.Builder(null,AppLocale.text(this,R.string.stop_alarm),stopAction).build());
        if(Build.VERSION.SDK_INT>=31)b.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        return b.build();
    }
    private void playDefault() {
        usingFallback=false;
        Uri sound=RingtoneManager.getActualDefaultRingtoneUri(this,RingtoneManager.TYPE_ALARM);
        if(sound==null){playFallback();return;}
        try {prepare(sound);}catch(Exception ex){playFallback();}
    }
    private void prepare(Uri sound) throws Exception {
        releasePlayer();player=new MediaPlayer();player.setAudioAttributes(ATTRS);player.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);
        player.setOnErrorListener((p,what,extra)->{handler.post(()->{if(current==this&&!usingFallback)playFallback();});return true;});
        player.setDataSource(this,sound);player.setLooping(true);player.prepare();if(hasFocus)player.start();
    }
    private void playFallback() {
        usingFallback=true;
        try {prepare(Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.med_alarm));}
        catch(Exception ex){releasePlayer();android.util.Log.e("MedTrackerAlarm","Alarm audio unavailable; vibration and stop notification remain active");}
    }
    private void releasePlayer() {if(player!=null){player.release();player=null;}}
    @Override public void onDestroy() {
        if(current==this)current=null;
        handler.removeCallbacksAndMessages(null);releasePlayer();
        if(vibrator!=null)vibrator.cancel();if(audio!=null&&focus!=null)audio.abandonAudioFocusRequest(focus);
        stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();
    }
}
