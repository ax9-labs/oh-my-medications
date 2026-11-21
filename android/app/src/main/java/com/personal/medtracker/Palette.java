package com.personal.medtracker;
import android.content.Context;

/** Theme colors are shared with the native Android controls through colors.xml. */
final class Palette {
    final int background,ink,muted,accent,accentInk,pale,line,progressTrack,navSelection,navSurface;
    Palette(Context c){
        background=c.getColor(R.color.background);ink=c.getColor(R.color.ink);muted=c.getColor(R.color.muted);
        accent=c.getColor(R.color.accent);accentInk=c.getColor(R.color.accent_ink);pale=c.getColor(R.color.pale);
        line=c.getColor(R.color.line);progressTrack=c.getColor(R.color.progress_track);
        navSelection=c.getColor(R.color.nav_selection);navSurface=c.getColor(R.color.nav_surface);
    }
}
