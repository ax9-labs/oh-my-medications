package com.personal.medtracker;
import android.content.*;
public final class AlarmActions extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i) {
        if("com.personal.medtracker.STOP_ALARM".equals(i.getAction())) AlarmService.stop(c);
    }
}
