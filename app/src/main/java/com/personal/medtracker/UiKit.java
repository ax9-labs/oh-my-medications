package com.personal.medtracker;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;

/** Shared visual primitives; icon-only actions always expose localized labels. */
final class UiKit {
    private UiKit(){}
    static int dp(Context c,float n){return (int)(n*c.getResources().getDisplayMetrics().density+.5f);}
    static GradientDrawable rounded(Context c,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
    static GradientDrawable surface(Context c){GradientDrawable d=rounded(c,0xDFFFFFFF,24);d.setStroke(dp(c,1),0x77FFFFFF);return d;}
    static GradientDrawable glowCard(Context c){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xEEF5E5F1,0xEEE9E5FF,0xEEDDEFFF});d.setCornerRadius(dp(c,26));d.setStroke(dp(c,1),0xAAFFFFFF);return d;}
    static boolean compact(Context c){return c.getResources().getConfiguration().screenWidthDp<360||c.getResources().getConfiguration().fontScale>=1.5f;}
    static Drawable interactive(Context c,Drawable base,int radius){
        StateListDrawable outlines=new StateListDrawable();
        GradientDrawable focus=rounded(c,Color.TRANSPARENT,radius);focus.setStroke(dp(c,2),c.getColor(R.color.accent_control));
        GradientDrawable hover=rounded(c,Color.TRANSPARENT,radius);hover.setStroke(dp(c,1),c.getColor(R.color.accent_ink));
        outlines.addState(new int[]{android.R.attr.state_focused},focus);outlines.addState(new int[]{android.R.attr.state_hovered},hover);outlines.addState(new int[]{},rounded(c,Color.TRANSPARENT,radius));
        return new RippleDrawable(android.content.res.ColorStateList.valueOf(0x33205F8C),new LayerDrawable(new Drawable[]{base,outlines}),rounded(c,Color.WHITE,radius));
    }
    static Drawable fieldBackground(Context c){
        StateListDrawable states=new StateListDrawable();GradientDrawable focused=rounded(c,c.getColor(R.color.background),10);focused.setStroke(dp(c,2),c.getColor(R.color.accent_ink));
        GradientDrawable normal=rounded(c,c.getColor(R.color.background),10);normal.setStroke(dp(c,1),c.getColor(R.color.line));
        states.addState(new int[]{android.R.attr.state_focused},focused);states.addState(new int[]{},normal);return states;
    }
    static void action(View view){semantics(view,false);}
    static void choice(View view){semantics(view,true);}
    private static void semantics(View view,boolean checkable){
        view.setFocusable(true);view.setClickable(true);view.setAccessibilityDelegate(new View.AccessibilityDelegate(){
            @Override public void onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName(checkable?RadioButton.class.getName():Button.class.getName());if(checkable){info.setCheckable(true);info.setChecked(host.isSelected());}}
        });
    }
    static TextView heading(TextView text){if(android.os.Build.VERSION.SDK_INT>=28)text.setAccessibilityHeading(true);return text;}
    static View iconButton(Context c,IconGlyph.Kind kind,String label,boolean primary,Runnable action){
        FrameLayout b=new FrameLayout(c);b.setContentDescription(label);b.setTooltipText(label);b.setFocusable(true);b.setClickable(true);b.setTag("action-"+kind.name().toLowerCase(java.util.Locale.ROOT));
        int color=c.getColor(R.color.ink);GradientDrawable bg=rounded(c,primary?color:0xCCFFFFFF,24);b.setBackground(interactive(c,bg,24));action(b);
        IconGlyph icon=new IconGlyph(c,kind,primary?Color.WHITE:color);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(c,22),dp(c,22),Gravity.CENTER);b.addView(icon,p);b.setOnClickListener(v->action.run());b.setMinimumWidth(dp(c,48));b.setMinimumHeight(dp(c,48));return b;
    }
    static void addAction(LinearLayout row,Context c,IconGlyph.Kind kind,String label,boolean primary,Runnable action){View v=iconButton(c,kind,label,primary,action);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(c,48),dp(c,48));p.leftMargin=dp(c,7);row.addView(v,p);}
}
