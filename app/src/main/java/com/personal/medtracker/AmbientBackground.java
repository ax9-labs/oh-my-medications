package com.personal.medtracker;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Soft lavender, peach and blue light behind translucent cards. */
final class AmbientBackground extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    @Override public void draw(Canvas c){float w=getBounds().width(),h=getBounds().height();paint.setShader(null);paint.setColor(0xFFF6F4FC);c.drawRect(getBounds(),paint);
        glow(c,w*.12f,h*.52f,w*.8f,0x66BDAAFF);glow(c,w*.94f,h*.45f,w*.85f,0x66FFD2BB);glow(c,w*.62f,h*.94f,w*.94f,0x889BD5FF);
    }
    private void glow(Canvas c,float x,float y,float radius,int color){paint.setShader(new RadialGradient(x,y,Math.max(1,radius),color,Color.TRANSPARENT,Shader.TileMode.CLAMP));c.drawCircle(x,y,radius,paint);}
    @Override public void setAlpha(int a){}@Override public void setColorFilter(ColorFilter f){}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
