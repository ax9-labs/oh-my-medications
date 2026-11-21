package com.personal.medtracker;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.util.TypedValue;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;

final class AdherenceChart extends View {
    private final Adherence.Report report;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private float reveal=1;private ValueAnimator animator;
    static int[] colors(Context c){return new int[]{c.getColor(R.color.chart_on_time),c.getColor(R.color.chart_outside),c.getColor(R.color.chart_skipped),c.getColor(R.color.chart_unlogged)};}
    private static float labelSize(Context c){return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,10,c.getResources().getDisplayMetrics());}
    static int preferredHeight(Context c){return UiKit.dp(c,170)+Math.round(Math.max(0,labelSize(c)-UiKit.dp(c,10))*2);}
    AdherenceChart(Context c,Adherence.Report report,String description){super(c);this.report=report;setContentDescription(description);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(FloatingNavBar.motionEnabled()){animator=ValueAnimator.ofFloat(0,1);animator.setDuration(700);animator.addUpdateListener(a->{reveal=(float)a.getAnimatedValue();invalidate();});animator.start();}}
    @Override protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);int max=1;for(Adherence.Day d:report.days)max=Math.max(max,d.total());
        p.setTextSize(labelSize(getContext()));p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setFontFeatureSettings("tnum");p.setTextAlign(Paint.Align.RIGHT);
        NumberFormat numbers=NumberFormat.getIntegerInstance(AppLocale.locale(getContext()));int muted=getContext().getColor(R.color.muted);
        float left=p.measureText(numbers.format(max))+UiKit.dp(getContext(),8),top=UiKit.dp(getContext(),12),bottom=getHeight()-p.getFontSpacing()-UiKit.dp(getContext(),14),width=getWidth()-left-UiKit.dp(getContext(),4);
        int steps=Math.min(2,max);for(int i=0;i<=steps;i++){int tick=Math.round(max*i/(float)steps);float y=bottom-(bottom-top)*tick/max;p.setColor(getContext().getColor(R.color.line));p.setStrokeWidth(1);c.drawLine(left,y,getWidth(),y,p);p.setColor(muted);c.drawText(numbers.format(tick),left-UiKit.dp(getContext(),5),y-p.ascent()/3,p);}
        float cell=width/Math.max(1,report.days.size()),bar=Math.min(UiKit.dp(getContext(),24),cell*.62f);int index=0;
        int[] colors=colors(getContext());for(Adherence.Day d:report.days){float x=left+cell*(index+.5f),y=bottom;int[] counts={d.onTime,d.outside,d.skipped,d.unlogged};
            for(int i=0;i<4;i++){float h=(bottom-top)*counts[i]/max*reveal;if(h>0){p.setColor(colors[i]);c.drawRoundRect(x-bar/2,y-h,x+bar/2,y,UiKit.dp(getContext(),3),UiKit.dp(getContext(),3),p);y-=h;}}
            if(report.days.size()<=7||index%5==0||index==report.days.size()-1){p.setTextAlign(Paint.Align.CENTER);p.setColor(muted);String label=report.days.size()<=7?d.date.format(DateTimeFormatter.ofPattern("EE",AppLocale.locale(getContext()))):numbers.format(d.date.getDayOfMonth());float half=p.measureText(label)/2;float labelX=Math.max(half,Math.min(getWidth()-half,x));c.drawText(label,labelX,getHeight()-UiKit.dp(getContext(),7),p);}index++;
        }
    }
}
