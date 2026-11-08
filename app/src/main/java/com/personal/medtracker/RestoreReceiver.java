package com.personal.medtracker;
import android.content.*;
public final class RestoreReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i) { Reminders.reschedule(c); }
}
