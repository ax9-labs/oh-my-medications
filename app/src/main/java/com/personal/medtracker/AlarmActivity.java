package com.personal.medtracker;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.*;
import android.view.*;
import android.widget.*;

public final class AlarmActivity extends LocalizedActivity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private long opened;
    private Palette colors;
    private final Runnable watch=new Runnable(){public void run(){
        if(!AlarmService.ringing()&&SystemClock.elapsedRealtime()-opened>2000){finish();return;}
        handler.postDelayed(this,500);
    }};
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);colors=new Palette(this);opened=SystemClock.elapsedRealtime();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}
        setVolumeControlStream(AudioManager.STREAM_ALARM);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setBackground(new AmbientBackground());
        int pad=dp(28);root.setPadding(pad,pad,pad,pad);
        root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(pad+i.getSystemWindowInsetLeft(),pad+i.getSystemWindowInsetTop(),pad+i.getSystemWindowInsetRight(),pad+i.getSystemWindowInsetBottom());return i;});
        label(root,getString(R.string.app_name).toUpperCase(AppLocale.locale(this)),14,colors.accentInk);space(root,24);label(root,getString(R.string.alarm_title),32,colors.ink);space(root,16);
        label(root,getString(R.string.alarm_due),19,colors.muted);space(root,36);
        button(root,getString(R.string.stop_alarm),true,()->{AlarmService.stop(this);finish();});space(root,14);
        button(root,getString(R.string.stop_open_tracker),false,()->{AlarmService.stop(this);startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));finish();});
        space(root,22);label(root,getString(R.string.alarm_stop_hint),14,colors.muted);
        if(getSystemService(AudioManager.class).getStreamVolume(AudioManager.STREAM_ALARM)==0){space(root,20);label(root,getString(R.string.alarm_muted),15,0xFF8F4C10);}
        setContentView(root);root.requestApplyInsets();
    }
    @Override protected void onResume(){super.onResume();handler.post(watch);}
    @Override protected void onPause(){handler.removeCallbacks(watch);super.onPause();}
    @Override public void onBackPressed(){moveTaskToBack(true);}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private void space(LinearLayout l,int n){l.addView(new View(this),new LinearLayout.LayoutParams(1,dp(n)));}
    private void label(LinearLayout l,String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);if(size>=30)t.setTypeface(null,Typeface.BOLD);l.addView(t,new LinearLayout.LayoutParams(-1,-2));}
    private void button(LinearLayout l,String value,boolean primary,Runnable action){Button b=new Button(this);b.setText(value);b.setTextSize(18);b.setAllCaps(false);b.setTextColor(primary?colors.ink:colors.accentInk);GradientDrawable bg=new GradientDrawable();bg.setColor(primary?colors.accent:colors.pale);bg.setCornerRadius(dp(18));b.setBackground(bg);b.setPadding(dp(12),dp(12),dp(12),dp(12));b.setMinHeight(dp(68));b.setOnClickListener(v->action.run());l.addView(b,new LinearLayout.LayoutParams(-1,-2));}
}
