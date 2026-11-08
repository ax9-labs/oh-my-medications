package com.personal.medtracker;
import android.content.Context;
import android.graphics.*;
import android.view.View;

final class IconGlyph extends View {
    enum Kind {ADD,SETTINGS,EDIT,CHECK,SKIP,UNDO,CLOCK,CALENDAR,EXPORT,ARROW,CHART,ARCHIVE}
    private final Kind kind;private final int color;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    IconGlyph(Context c,Kind kind,int color){super(c);this.kind=kind;this.color=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    private void line(Canvas c,float x,float y,float a,float b){c.drawLine(x,y,a,b,p);}
    @Override protected void onDraw(Canvas c){c.save();c.scale(getWidth()/24f,getHeight()/24f);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        switch(kind){
            case ADD:line(c,12,5,12,19);line(c,5,12,19,12);break;
            case CHECK:Path k=new Path();k.moveTo(5,12);k.lineTo(10,17);k.lineTo(19,7);c.drawPath(k,p);break;
            case SKIP:line(c,6,6,18,18);line(c,18,6,6,18);break;
            case EDIT:c.save();c.rotate(-42,12,12);c.drawRoundRect(4,9,20,15,2,2,p);line(c,17,9,17,15);c.restore();line(c,4,20,9,19);break;
            case UNDO:c.drawArc(5,5,21,21,-130,260,false,p);Path a=new Path();a.moveTo(3,4);a.lineTo(3,10);a.lineTo(9,10);c.drawPath(a,p);break;
            case CLOCK:c.drawCircle(12,12,9,p);line(c,12,7,12,12);line(c,12,12,16,14);break;
            case CALENDAR:c.drawRoundRect(3,5,21,21,4,4,p);line(c,8,3,8,7);line(c,16,3,16,7);line(c,4,10,20,10);break;
            case EXPORT:c.drawRoundRect(4,10,20,21,3,3,p);line(c,12,3,12,15);line(c,8,7,12,3);line(c,16,7,12,3);break;
            case ARROW:line(c,5,12,19,12);line(c,14,7,19,12);line(c,14,17,19,12);break;
            case CHART:line(c,5,19,5,12);line(c,12,19,12,5);line(c,19,19,19,9);break;
            case ARCHIVE:c.drawRoundRect(4,8,20,21,2,2,p);c.drawRoundRect(3,3,21,8,2,2,p);line(c,9,12,15,12);break;
            default:c.drawCircle(12,12,3.2f,p);for(int i=0;i<8;i++){double angle=i*Math.PI/4;line(c,12+(float)Math.cos(angle)*7,12+(float)Math.sin(angle)*7,12+(float)Math.cos(angle)*10,12+(float)Math.sin(angle)*10);}c.drawCircle(12,12,7,p);break;
        }c.restore();
    }
}
