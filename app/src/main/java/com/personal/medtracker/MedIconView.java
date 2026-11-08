package com.personal.medtracker;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Resolution-independent medication illustrations with depth, highlights and optional tint. */
public final class MedIconView extends View {
    private String type,color;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    public MedIconView(Context c,String type,String color){super(c);setAppearance(type,color);}
    public void setAppearance(String type,String color){this.type=MedAppearance.type(type);this.color=MedAppearance.color(color);setContentDescription(AppLocale.text(getContext(),R.string.icon_description,MedAppearance.typeLabel(getContext(),type),MedAppearance.colorLabel(getContext(),color)));invalidate();}
    private int mix(int a,int b,float f){return Color.rgb((int)(Color.red(a)*(1-f)+Color.red(b)*f),(int)(Color.green(a)*(1-f)+Color.green(b)*f),(int)(Color.blue(a)*(1-f)+Color.blue(b)*f));}
    private void fill(int c){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(c);}
    private void oval(Canvas c,float l,float t,float r,float b,int color){fill(color);c.drawOval(l,t,r,b,p);}
    private void round(Canvas c,float l,float t,float r,float b,float radius,int color){fill(color);c.drawRoundRect(l,t,r,b,radius,radius,p);}
    private void line(Canvas c,float x,float y,float a,float b,int color,float width){fill(color);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,a,b,p);}
    private void poly(Canvas c,int color,float... points){Path path=new Path();path.moveTo(points[0],points[1]);for(int i=2;i<points.length;i+=2)path.lineTo(points[i],points[i+1]);path.close();fill(color);c.drawPath(path,p);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float size=Math.min(getWidth(),getHeight());c.save();c.translate((getWidth()-size)/2,(getHeight()-size)/2);c.scale(size/100,size/100);
        int base=MedAppearance.tint(type,color),light=mix(base,Color.WHITE,.71f),highlight=mix(base,Color.WHITE,.88f),dark=mix(base,getContext().getColor(R.color.ink),.22f);
        round(c,1,1,99,99,25,mix(base,Color.WHITE,.91f));
        oval(c,22,73,83,86,mix(base,Color.WHITE,.79f));
        switch(type){
            case "tablet":
                // Raised round tablet, inspired by the supplied reference.
                round(c,20,43,80,67,12,base);oval(c,20,47,80,80,base);oval(c,20,32,80,66,light);
                oval(c,23,32,77,61,highlight);line(c,36,59,63,35,mix(base,Color.WHITE,.40f),5);
                line(c,36,56,61,34,mix(base,Color.WHITE,.80f),2);break;
            case "syrup":
                round(c,30,29,71,79,10,dark);round(c,31,28,68,76,9,base);round(c,35,35,40,65,3,light);
                round(c,27,19,74,34,4,dark);round(c,29,18,72,30,4,light);
                for(int x=35;x<70;x+=7)line(c,x,21,x,27,mix(base,Color.WHITE,.30f),2);
                round(c,32,47,68,65,3,highlight);line(c,46,56,55,56,base,3);line(c,50.5f,51.5f,50.5f,60.5f,base,3);break;
            case "package":
                poly(c,dark,60,32,78,25,78,73,60,84);poly(c,base,23,25,60,32,60,84,23,75);
                poly(c,light,23,25,42,17,78,25,60,34);poly(c,highlight,25,42,58,48,58,69,25,63);
                line(c,35,55,48,58,base,4);line(c,42,50,42,63,base,4);line(c,29,31,53,35,light,2);break;
            case "blister":
                c.save();c.rotate(-12,50,50);round(c,24,18,77,83,7,dark);round(c,21,16,74,80,7,light);
                for(int y=30;y<=66;y+=18)for(int x=35;x<=59;x+=24){oval(c,x-8,y-6,x+8,y+8,base);oval(c,x-8,y-8,x+8,y+4,highlight);line(c,x-3,y-4,x+3,y+1,mix(base,Color.WHITE,.40f),2);}
                c.restore();break;
            case "drops":
                round(c,32,39,70,80,10,base);round(c,33,37,67,76,9,light);round(c,35,28,66,44,4,dark);
                round(c,41,15,60,31,8,base);round(c,38,43,63,66,5,highlight);
                Path drop=new Path();drop.moveTo(51,47);drop.cubicTo(48,52,44,55,46,59);drop.cubicTo(49,65,58,60,56,56);drop.close();fill(base);c.drawPath(drop,p);break;
            default:
                c.save();c.rotate(-38,50,50);round(c,15,36,87,69,16,dark);round(c,13,31,85,64,16,base);
                c.save();c.clipRect(13,28,49,66);round(c,13,31,85,64,16,highlight);c.restore();
                line(c,49,33,49,62,mix(base,Color.WHITE,.40f),2);line(c,60,37,72,37,light,3);line(c,24,37,35,37,Color.WHITE,3);c.restore();break;
        }
        c.restore();
    }
}
