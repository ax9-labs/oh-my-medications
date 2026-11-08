package com.personal.medtracker;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

final class AnimatedLine extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final float target;private float value;private ValueAnimator animator;
    AnimatedLine(Context c,float progress){super(c);target=Math.max(0,Math.min(1,progress));setMinimumHeight(UiKit.dp(c,8));}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(!FloatingNavBar.motionEnabled()){value=target;invalidate();return;}animator=ValueAnimator.ofFloat(0,target);animator.setDuration(800);animator.setInterpolator(new android.view.animation.DecelerateInterpolator());animator.addUpdateListener(a->{value=(float)a.getAnimatedValue();invalidate();});animator.start();}
    @Override protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float h=getHeight(),r=h/2;p.setShader(null);p.setColor(0xFFE4E2EE);c.drawRoundRect(0,0,getWidth(),h,r,r,p);if(value>0){p.setShader(new LinearGradient(0,0,Math.max(1,getWidth()),0,new int[]{0xFFA88BE4,0xFFB6C5F5,0xFFF5B39A},null,Shader.TileMode.CLAMP));c.drawRoundRect(0,0,Math.max(h,getWidth()*value),h,r,r,p);}}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName(android.widget.ProgressBar.class.getName());info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT,0,1,target));}
}
