package com.personal.medtracker;
import android.content.Context;

/** Startup requires both missing; an attention indicator requires either missing. */
final class AlarmPermissions {
    static boolean shouldWelcome(boolean precise,boolean fullScreen){return !precise&&!fullScreen;}
    static boolean needsAttention(boolean precise,boolean fullScreen){return !precise||!fullScreen;}
    static State read(Context c){return new State(Reminders.precise(c),Reminders.fullScreen(c));}
    static final class State {
        final boolean precise,fullScreen;
        State(boolean precise,boolean fullScreen){this.precise=precise;this.fullScreen=fullScreen;}
        boolean bothMissing(){return shouldWelcome(precise,fullScreen);}
        boolean needsAttention(){return AlarmPermissions.needsAttention(precise,fullScreen);}
    }
}
