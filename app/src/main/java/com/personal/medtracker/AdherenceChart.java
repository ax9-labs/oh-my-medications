package com.personal.medtracker;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.time.format.DateTimeFormatter;

final class AdherenceChart extends View {
    private final Adherence.Report report;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private float reveal=1;private ValueAnimator animator;
    static final int ON_TIME=0xFF7CAF9C,OUTSIDE=0xFFA18ADB,SKIPPED=0xFFE6A397,UNLOGGED=0xFFCBD0DC;
    AdherenceChart(Context c,Adherence.Report report,String description){super(c);this.report=report;setContentDescription(description);setMinimumHeight(UiKit.dp(c,200));}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(FloatingNavBar.motionEnabled()){animator=ValueAnimator.ofFloat(0,1);animator.setDuration(700);animator.addUpdateListener(a->{reveal=(float)a.getAnimatedValue();invalidate();});animator.start();}}
    @Override protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float left=UiKit.dp(getContext(),22),top=UiKit.dp(getContext(),12),bottom=getHeight()-UiKit.dp(getContext(),28),width=getWidth()-left-4;int max=1;for(Adherence.Day d:report.days)max=Math.max(max,d.total());
        p.setTextSize(UiKit.dp(getContext(),10));p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextAlign(Paint.Align.RIGHT);
        for(int i=0;i<=2;i++){float y=bottom-(bottom-top)*i/2;p.setColor(0xFFE2DFEC);p.setStrokeWidth(1);c.drawLine(left,y,getWidth(),y,p);p.setColor(0xFF777184);c.drawText(""+Math.round(max*i/2f),left-UiKit.dp(getContext(),5),y+3,p);}
        float cell=width/Math.max(1,report.days.size()),bar=Math.min(UiKit.dp(getContext(),24),cell*.62f);int index=0;
        for(Adherence.Day d:report.days){float x=left+cell*(index+.5f),y=bottom;int[] counts={d.onTime,d.outside,d.skipped,d.unlogged},colors={ON_TIME,OUTSIDE,SKIPPED,UNLOGGED};
            for(int i=0;i<4;i++){float h=(bottom-top)*counts[i]/max*reveal;if(h>0){p.setColor(colors[i]);c.drawRoundRect(x-bar/2,y-h,x+bar/2,y,UiKit.dp(getContext(),3),UiKit.dp(getContext(),3),p);y-=h;}}
            if(report.days.size()<=7||index%5==0||index==report.days.size()-1){p.setTextAlign(Paint.Align.CENTER);p.setColor(0xFF777184);String label=report.days.size()<=7?d.date.format(DateTimeFormatter.ofPattern("EE",AppLocale.locale(getContext()))):""+d.date.getDayOfMonth();c.drawText(label,x,getHeight()-UiKit.dp(getContext(),7),p);}index++;
        }
    }
}
