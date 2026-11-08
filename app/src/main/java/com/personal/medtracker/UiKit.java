package com.personal.medtracker;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Shared visual primitives; icon-only actions always expose localized labels. */
final class UiKit {
    private UiKit(){}
    static int dp(Context c,float n){return (int)(n*c.getResources().getDisplayMetrics().density+.5f);}
    static GradientDrawable rounded(Context c,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
    static GradientDrawable surface(Context c){GradientDrawable d=rounded(c,0xDFFFFFFF,24);d.setStroke(dp(c,1),0x77FFFFFF);return d;}
    static GradientDrawable glowCard(Context c){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xEEF5E5F1,0xEEE9E5FF,0xEEDDEFFF});d.setCornerRadius(dp(c,26));d.setStroke(dp(c,1),0xAAFFFFFF);return d;}
    static View iconButton(Context c,IconGlyph.Kind kind,String label,boolean primary,Runnable action){
        FrameLayout b=new FrameLayout(c);b.setContentDescription(label);b.setTooltipText(label);b.setFocusable(true);b.setClickable(true);b.setTag("action-"+kind.name().toLowerCase(java.util.Locale.ROOT));
        int color=c.getColor(R.color.ink);GradientDrawable bg=rounded(c,primary?color:0xCCFFFFFF,24);b.setBackground(bg);b.setForeground(new RippleDrawable(android.content.res.ColorStateList.valueOf(0x337D8AB5),null,rounded(c,Color.WHITE,24)));
        IconGlyph icon=new IconGlyph(c,kind,primary?Color.WHITE:color);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(c,22),dp(c,22),Gravity.CENTER);b.addView(icon,p);b.setOnClickListener(v->action.run());b.setMinimumWidth(dp(c,48));b.setMinimumHeight(dp(c,48));return b;
    }
    static void addAction(LinearLayout row,Context c,IconGlyph.Kind kind,String label,boolean primary,Runnable action){View v=iconButton(c,kind,label,primary,action);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(c,48),dp(c,48));p.leftMargin=dp(c,7);row.addView(v,p);}
}
