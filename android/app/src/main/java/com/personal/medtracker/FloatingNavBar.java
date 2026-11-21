package com.personal.medtracker;

import android.animation.*;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Build;
import android.text.TextUtils;
import android.view.*;
import android.view.animation.PathInterpolator;
import android.widget.*;

/** Persistent floating navigation, with one retargetable animation for its selection pill. */
public final class FloatingNavBar extends FrameLayout {
    interface Listener {void onTab(int tab);}
    private final Palette colors;
    private final View indicator;
    private final FrameLayout[] items=new FrameLayout[4];
    private final NavIcon[] icons=new NavIcon[4];
    private final TextView[] labels=new TextView[4];
    private final ValueAnimator[] tintAnimations=new ValueAnimator[4];
    private final String[] titles;
    private final Listener listener;
    private int selected;
    private ObjectAnimator movement;
    private final boolean compact;
    private boolean pendingPillAnimation;
    private static final PathInterpolator EASE=new PathInterpolator(.22f,1f,.36f,1f);
    public FloatingNavBar(Context c,Listener listener){
        super(c);this.listener=listener;colors=new Palette(c);compact=UiKit.compact(c);setTag("floating-navigation");
        titles=new String[]{c.getString(R.string.schedule_title),c.getString(R.string.medications),c.getString(R.string.dashboard),c.getString(R.string.history)};
        GradientDrawable bg=rounded(colors.navSurface,36);bg.setStroke(dp(1),colors.line);setBackground(bg);setElevation(dp(10));
        if(Build.VERSION.SDK_INT>=28){setOutlineAmbientShadowColor(0x227096B3);setOutlineSpotShadowColor(0x337096B3);}
        setClipChildren(false);setClipToPadding(false);
        indicator=new View(c);indicator.setTag("nav-selection");indicator.setBackground(rounded(colors.navSelection,27));indicator.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(indicator,new FrameLayout.LayoutParams(1,1));
        LinearLayout row=new LinearLayout(c);row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(dp(7),0,dp(7),0);addView(row,new FrameLayout.LayoutParams(-1,-1));
        for(int i=0;i<4;i++){
            final int index=i;FrameLayout item=new FrameLayout(c);items[i]=item;item.setTag("nav-tab-"+i);item.setFocusable(true);item.setClickable(true);item.setTooltipText(titles[i]);
            item.setOnClickListener(v->listener.onTab(index));item.setBackground(UiKit.interactive(c,rounded(Color.TRANSPARENT,28),28));UiKit.choice(item);
            row.addView(item,new LinearLayout.LayoutParams(0,-1,1));
            NavIcon icon=new NavIcon(c,i);icons[i]=icon;icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(dp(25),dp(25),Gravity.TOP|Gravity.CENTER_HORIZONTAL);ip.topMargin=dp(25);item.addView(icon,ip);
            TextView label=new TextView(c);labels[i]=label;label.setText(titles[i]);label.setTextSize(12);label.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));label.setTextColor(colors.accentInk);
            label.setSingleLine(true);label.setEllipsize(TextUtils.TruncateAt.END);label.setGravity(Gravity.CENTER);label.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);label.setFocusable(false);label.setClickable(false);
            label.setAutoSizeTextTypeUniformWithConfiguration(8,12,1,android.util.TypedValue.COMPLEX_UNIT_SP);
            label.setHorizontallyScrolling(false);
            FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);lp.bottomMargin=dp(7);lp.leftMargin=dp(3);lp.rightMargin=dp(3);item.addView(label,lp);
        }
        select(0,false);
    }
    public static boolean motionEnabled(){return ValueAnimator.areAnimatorsEnabled();}
    static int preferredHeight(Context c){TextView label=new TextView(c);label.setTextSize(12);label.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));Paint.FontMetricsInt metrics=label.getPaint().getFontMetricsInt();return Math.max(UiKit.dp(c,76),UiKit.dp(c,55)+metrics.bottom-metrics.top);}
    public int selectedTab(){return selected;}
    public boolean animating(){return movement!=null&&movement.isRunning();}
    void select(int tab,boolean animate){
        selected=tab;boolean smooth=animate&&motionEnabled()&&getWidth()>0;
        if(movement!=null)movement.cancel();
        float destination=position(tab);
        if(compact){pendingPillAnimation=smooth;requestLayout();}
        else if(smooth){movement=ObjectAnimator.ofFloat(indicator,View.TRANSLATION_X,destination);movement.setDuration(300);movement.setInterpolator(EASE);movement.start();}
        else indicator.setTranslationX(destination);
        for(int i=0;i<4;i++){
            boolean active=i==tab;items[i].setSelected(active);items[i].setContentDescription(active?getContext().getString(R.string.selected_description,titles[i]):titles[i]);
            icons[i].animate().cancel();labels[i].animate().cancel();if(tintAnimations[i]!=null)tintAnimations[i].cancel();
            float y=active?-dp(10):0,labelY=active?0:dp(5),alpha=active?1:0,scale=active?1.04f:1;
            if(smooth){
                icons[i].animate().translationY(y).scaleX(scale).scaleY(scale).setDuration(260).setInterpolator(EASE).start();
                labels[i].animate().alpha(alpha).translationY(labelY).setDuration(200).setInterpolator(EASE).start();
                final NavIcon icon=icons[i];ValueAnimator tint=ValueAnimator.ofFloat(icon.activation,active?1:0);tintAnimations[i]=tint;tint.setDuration(230);tint.addUpdateListener(a->icon.activate((float)a.getAnimatedValue()));tint.start();
            }else{
                icons[i].setTranslationY(y);icons[i].setScaleX(scale);icons[i].setScaleY(scale);icons[i].activate(active?1:0);
                labels[i].setAlpha(alpha);labels[i].setTranslationY(labelY);
            }
        }
    }
    private float position(int tab){return compact?items[tab].getLeft()+dp(2):dp(7)+tab*(getWidth()-dp(14))/4f+dp(2);}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=Math.min(MeasureSpec.getSize(widthSpec),dp(420));
        int selectedWidth=Math.round((width-dp(14))/4f);
        if(compact){
            int available=width-dp(14);Paint text=new Paint(labels[selected].getPaint());text.setTextSize(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,12,getResources().getDisplayMetrics()));
            selectedWidth=Math.min(available-dp(48)*3,Math.max(dp(48),Math.round(text.measureText(titles[selected]))+dp(14)));
            int rest=(available-selectedWidth)/3;
            for(int i=0;i<4;i++){LinearLayout.LayoutParams item=(LinearLayout.LayoutParams)items[i].getLayoutParams();int target=i==selected?selectedWidth:rest;if(item.width!=target||item.weight!=0){item.width=target;item.weight=0;items[i].setLayoutParams(item);}}
        }
        // Size the selection before children are measured, including the very first layout.
        FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)indicator.getLayoutParams();p.width=Math.max(1,selectedWidth-dp(4));p.height=Math.max(1,MeasureSpec.getSize(heightSpec)-dp(12));p.topMargin=dp(6);
        super.onMeasure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),heightSpec);
    }
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
        super.onLayout(changed,left,top,right,bottom);if(!compact)return;
        if(pendingPillAnimation){if(movement!=null)movement.cancel();movement=ObjectAnimator.ofFloat(indicator,View.TRANSLATION_X,position(selected));movement.setDuration(300);movement.setInterpolator(EASE);movement.start();}
        else if(movement==null||!movement.isRunning())indicator.setTranslationX(position(selected));pendingPillAnimation=false;
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);if(movement!=null)movement.cancel();indicator.setTranslationX(position(selected));
    }
    @Override protected void onDetachedFromWindow(){if(movement!=null)movement.cancel();for(int i=0;i<4;i++){icons[i].animate().cancel();labels[i].animate().cancel();if(tintAnimations[i]!=null)tintAnimations[i].cancel();}super.onDetachedFromWindow();}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable rounded(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private final class NavIcon extends View {
        private final int kind;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private float activation;
        NavIcon(Context c,int kind){super(c);this.kind=kind;}
        void activate(float value){activation=value;invalidate();}
        private int mix(int a,int b,float f){return Color.rgb((int)(Color.red(a)*(1-f)+Color.red(b)*f),(int)(Color.green(a)*(1-f)+Color.green(b)*f),(int)(Color.blue(a)*(1-f)+Color.blue(b)*f));}
        @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();c.scale(getWidth()/24f,getHeight()/24f);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setColor(mix(colors.muted,colors.accentInk,activation));
            if(kind==0){c.drawRoundRect(3.5f,5,20.5f,21,4,4,p);c.drawLine(8,3,8,7,p);c.drawLine(16,3,16,7,p);c.drawLine(4,10,20,10,p);Path check=new Path();check.moveTo(8,15);check.lineTo(11,18);check.lineTo(16,13);c.drawPath(check,p);}
            else if(kind==1){c.rotate(-42,12,12);c.drawRoundRect(2.5f,6.5f,21.5f,17.5f,5.5f,5.5f,p);c.drawLine(12,7,12,17,p);}
            else if(kind==2){c.drawLine(4,20,20,20,p);c.drawLine(7,15,7,18,p);c.drawLine(12,10,12,18,p);c.drawLine(17,5,17,18,p);}
            else {c.drawArc(3.5f,3.5f,20.5f,20.5f,-130,310,false,p);Path arrow=new Path();arrow.moveTo(3,3);arrow.lineTo(3,8);arrow.lineTo(8,8);c.drawPath(arrow,p);c.drawLine(12,7.5f,12,12,p);c.drawLine(12,12,16,14,p);}
            c.restore();
        }
    }
}
