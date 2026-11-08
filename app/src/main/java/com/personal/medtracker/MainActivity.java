package com.personal.medtracker;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class MainActivity extends LocalizedActivity {
    private Palette palette;
    private int BG,INK,MUTED,ACCENT,ACCENT_INK,PALE,LINE;
    private Store store;
    private static final long PROCESS_TOKEN=SystemClock.elapsedRealtimeNanos();
    private boolean welcomeVisible,welcomeDismissed,restoreVisit,returningFromChild;
    private int welcomeOffset;
    private Bundle pendingEditor;
    private AlarmPermissions.State alarmAccess;
    private FrameLayout root,pageHost;
    private LinearLayout content;
    private FloatingNavBar nav;
    private final int[] pageOffsets=new int[4];
    private View scroll;
    private final int[] pagePositions=new int[4];
    private int dashboardDays=7;
    private LocalDate scheduleFirst=LocalDate.now(),scheduleThrough=LocalDate.now().plusDays(13);
    private String formStartDay;
    private EditText formDays;
    private Button formStart;
    private AlertDialog timeEditor;
    private int tab=0;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable tick=new Runnable() { public void run() { if(tab==0||tab==2)render(); handler.postDelayed(this,30000); } };
    private AlertDialog editor;
    private EditText formName,formDose,formNotes;
    private final TreeSet<String> formTimes=new TreeSet<>();
    private Switch formReminders;
    private long editingId;
    private String formIconType="capsule",formIconColor="default",pickerIconType,pickerIconColor;
    private MedIconView formIconPreview;
    private TextView formIconLabel;
    private AlertDialog iconPicker;
    private AlertDialog settingsDialog,languageDialog;
    private AlertDialog discardDialog;
    private List<String> editorInitialValues;
    private LinearLayout iconPickerContent;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);palette=new Palette(this);BG=palette.background;INK=palette.ink;MUTED=palette.muted;ACCENT=palette.accent;ACCENT_INK=palette.accentInk;PALE=palette.pale;LINE=palette.line;store=new Store(this); Reminders.channel(this);
        if(state!=null){tab=state.getInt("tab",0);int[] offsets=state.getIntArray("pageOffsets");if(offsets!=null&&offsets.length==4)System.arraycopy(offsets,0,pageOffsets,0,4);int[] positions=state.getIntArray("pagePositions");if(positions!=null&&positions.length==4)System.arraycopy(positions,0,pagePositions,0,4);dashboardDays=state.getInt("dashboardDays",7);scheduleFirst=LocalDate.parse(state.getString("scheduleFirst",LocalDate.now().toString()));scheduleThrough=LocalDate.parse(state.getString("scheduleThrough",LocalDate.now().plusDays(13).toString()));}
        restoreVisit=state!=null&&state.getLong("welcomeProcess",-1)==PROCESS_TOKEN;
        if(restoreVisit){welcomeDismissed=state.getBoolean("welcomeDismissed",false);returningFromChild=state.getBoolean("returningFromChild",false);welcomeOffset=state.getInt("welcomeOffset",0);}
        render();
        Bundle draft=state==null?null:state.getBundle("pendingEditor");if(draft==null&&state!=null&&state.getBoolean("editor",false))draft=state;
        if(draft!=null){if(welcomeVisible)pendingEditor=new Bundle(draft);else restoreEditor(draft);}
    }
    private void restoreEditor(Bundle state){
        if(state!=null && state.getBoolean("editor",false)) {
            long id=state.getLong("editingId"); Store.Med med=id==0?null:store.med(id);
            if(id==0 || med!=null) {
                edit(med); formName.setText(state.getString("name","")); formDose.setText(state.getString("dose","")); formNotes.setText(state.getString("notes",""));
                formStartDay=state.getString("startDay",formStartDay);formStart.setText(AppLocale.date(this,LocalDate.parse(formStartDay),true));formDays.setText(state.getString("durationDays",""));
                formReminders.setChecked(state.getBoolean("reminders",true)); formTimes.clear(); formTimes.addAll(state.getStringArrayList("times")); rebuildTimes();
                formIconType=MedAppearance.type(state.getString("iconType",formIconType));formIconColor=MedAppearance.color(state.getString("iconColor",formIconColor));refreshIconForm();
                if(state.getBoolean("iconPicker",false))chooseIcon(state.getString("pickerType",formIconType),state.getString("pickerColor",formIconColor));
            }
        }
    }
    @Override protected void onStart(){super.onStart();if(!restoreVisit&&!returningFromChild)welcomeDismissed=false;restoreVisit=false;}
    @Override protected void onResume() {
        super.onResume();boolean wasWelcome=welcomeVisible,settingsOpen=settingsDialog!=null&&settingsDialog.isShowing();
        if(settingsOpen)settingsDialog.dismiss();returningFromChild=false;Reminders.reschedule(this);render();
        if(settingsOpen||(wasWelcome&&!welcomeVisible&&alarmAccess.needsAttention()&&(editor==null||!editor.isShowing())))settings();
        handler.removeCallbacks(tick);handler.postDelayed(tick,30000);
    }
    @Override protected void onPause() { handler.removeCallbacks(tick); super.onPause(); }
    @Override protected void onDestroy() { if(discardDialog!=null)discardDialog.dismiss();if(timeEditor!=null)timeEditor.dismiss();if(languageDialog!=null)languageDialog.dismiss();if(settingsDialog!=null)settingsDialog.dismiss();if(iconPicker!=null)iconPicker.dismiss();if(editor!=null) editor.dismiss(); store.close(); super.onDestroy(); }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent);if(tab!=0)switchTab(0);else render(); }
    @Override protected void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);b.putLong("welcomeProcess",PROCESS_TOKEN);b.putBoolean("welcomeDismissed",welcomeDismissed);b.putBoolean("returningFromChild",returningFromChild);
        capturePosition();b.putInt("welcomeOffset",welcomeOffset);if(pendingEditor!=null)b.putBundle("pendingEditor",pendingEditor);
        b.putInt("tab",tab);b.putIntArray("pageOffsets",pageOffsets);b.putIntArray("pagePositions",pagePositions);b.putInt("dashboardDays",dashboardDays);b.putString("scheduleFirst",scheduleFirst.toString());b.putString("scheduleThrough",scheduleThrough.toString());
        if(editor!=null && editor.isShowing()) {
            b.putBoolean("editor",true); b.putLong("editingId",editingId); b.putString("name",formName.getText().toString()); b.putString("dose",formDose.getText().toString()); b.putString("notes",formNotes.getText().toString());
            b.putString("startDay",formStartDay);b.putString("durationDays",formDays.getText().toString());b.putBoolean("reminders",formReminders.isChecked()); b.putStringArrayList("times",new ArrayList<>(formTimes));
            b.putString("iconType",formIconType);b.putString("iconColor",formIconColor);
            if(iconPicker!=null&&iconPicker.isShowing()){b.putBoolean("iconPicker",true);b.putString("pickerType",pickerIconType);b.putString("pickerColor",pickerIconColor);}
        }
    }
    private int dp(float v) { return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
    private GradientDrawable shape(int color,int radius) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private LinearLayout column() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private TextView text(String value,int sp,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(sp); t.setTextColor(color); t.setFontFeatureSettings("kern");
        if(bold) t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); t.setLineSpacing(dp(2),1f); return t;
    }
    private void space(LinearLayout l,int height) { l.addView(new View(this),new LinearLayout.LayoutParams(1,dp(height))); }
    private Button button(String label,boolean primary,Runnable action) {
        Button b=new Button(this); b.setText(label); b.setTextSize(14); b.setAllCaps(false); b.setTextColor(primary?INK:ACCENT_INK);
        b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); b.setMinHeight(dp(48)); b.setMinimumHeight(dp(48)); b.setPadding(dp(16),dp(6),dp(16),dp(6));
        b.setBackground(UiKit.interactive(this,shape(primary?ACCENT:PALE,14),14)); b.setOnClickListener(v->action.run()); return b;
    }
    private void fullButton(LinearLayout l,String label,boolean primary,Runnable action) {
        Button b=button(label,primary,action); l.addView(b,new LinearLayout.LayoutParams(-1,-2));
    }
    private LinearLayout card(LinearLayout parent) {
        LinearLayout l=column(); l.setPadding(dp(18),dp(18),dp(18),dp(18)); l.setBackground(UiKit.surface(this));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(12); parent.addView(l,p); return l;
    }
    private void medicationHeader(LinearLayout parent,String name,String dose,String type,String color,int size,int titleSize){
        LinearLayout heading=row();MedIconView icon=new MedIconView(this,type,color);icon.setTag("medication-icon");
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(size),dp(size));ip.rightMargin=dp(14);heading.addView(icon,ip);
        LinearLayout words=column();words.addView(text(name,titleSize,INK,true));space(words,4);words.addView(text(dose,15,MUTED,false));heading.addView(words,new LinearLayout.LayoutParams(0,-2,1));parent.addView(heading);
    }
    private void render() {
        if(root==null)createShell();
        capturePosition();
        alarmAccess=AlarmPermissions.read(this);welcomeVisible=!welcomeDismissed&&alarmAccess.bothMissing();nav.setVisibility(welcomeVisible?View.GONE:View.VISIBLE);
        showPage(false,0);nav.select(tab,false);
        if(!welcomeVisible&&pendingEditor!=null){Bundle draft=pendingEditor;pendingEditor=null;restoreEditor(draft);}
    }
    private void createShell(){
        root=new FrameLayout(this);root.setBackground(new AmbientBackground());
        root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});
        pageHost=new FrameLayout(this);pageHost.setTag("page-host");root.addView(pageHost,new FrameLayout.LayoutParams(-1,-1));
        nav=new FloatingNavBar(this,this::switchTab);
        FrameLayout.LayoutParams np=new FrameLayout.LayoutParams(-1,FloatingNavBar.preferredHeight(this),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);np.leftMargin=dp(22);np.rightMargin=dp(22);np.bottomMargin=dp(16);root.addView(nav,np);
        setContentView(root);root.requestApplyInsets();
    }
    private void switchTab(int next){
        if(welcomeVisible)return;
        alarmAccess=AlarmPermissions.read(this);
        if(next==tab){if(scroll instanceof ListView)((ListView)scroll).smoothScrollToPosition(0);else if(scroll instanceof ScrollView)((ScrollView)scroll).smoothScrollTo(0,0);return;}
        capturePosition();int direction=next>tab?1:-1;tab=next;showPage(true,direction);nav.select(tab,true);
    }
    private void showPage(boolean animate,int direction){
        View old=scroll;
        // Keep the newest page only before retargeting a rapid sequence of taps.
        for(int i=pageHost.getChildCount()-1;i>=0;i--){View child=pageHost.getChildAt(i);child.animate().cancel();if(child!=old)pageHost.removeViewAt(i);}
        View next=buildPage();scroll=next;pageHost.addView(next,new FrameLayout.LayoutParams(-1,-1));
        final int offset=welcomeVisible?welcomeOffset:pageOffsets[tab],position=pagePositions[tab];next.post(()->{if(next instanceof ListView)((ListView)next).setSelectionFromTop(position,offset);else next.scrollTo(0,offset);});
        if(animate&&old!=null&&FloatingNavBar.motionEnabled()){
            old.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            old.animate().alpha(0).translationX(-direction*dp(12)).setDuration(150).withEndAction(()->{if(old.getParent()==pageHost)pageHost.removeView(old);}).start();
            next.setAlpha(0);next.setTranslationX(direction*dp(16));next.animate().alpha(1).translationX(0).setDuration(260).setInterpolator(new android.view.animation.PathInterpolator(.22f,1f,.36f,1f)).start();
        }else if(old!=null)pageHost.removeView(old);
    }
    private void capturePosition(){
        if(scroll==null)return;if(welcomeVisible){welcomeOffset=scroll.getScrollY();return;}
        if(scroll instanceof ScheduleFeed){scheduleFirst=((ScheduleFeed)scroll).firstDay();scheduleThrough=((ScheduleFeed)scroll).lastDay();}
        if(scroll instanceof ListView){ListView list=(ListView)scroll;pagePositions[tab]=list.getFirstVisiblePosition();pageOffsets[tab]=list.getChildCount()==0?0:list.getChildAt(0).getTop();}else pageOffsets[tab]=scroll.getScrollY();
    }
    private View buildPage(){
        if(!welcomeVisible&&tab==0)return new ScheduleFeed(this,store,scheduleFirst,scheduleThrough,new ScheduleFeed.Actions(){
            public View brand(){return brandHeader();}public void add(){edit(null);}public void pickDate(){LocalDate d=LocalDate.now();DatePickerDialog picker=new DatePickerDialog(MainActivity.this,(v,y,m,day)->{if(scroll instanceof ScheduleFeed)((ScheduleFeed)scroll).jumpTo(LocalDate.of(y,m+1,day));},d.getYear(),d.getMonthValue()-1,d.getDayOfMonth());picker.getDatePicker().setMinDate(d.minusYears(10).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());picker.getDatePicker().setMaxDate(d.plusYears(10).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());picker.show();}
            public void taken(Store.Slot slot){markSlot(slot,"taken");}public void skip(Store.Slot slot){markSlot(slot,"skipped");}public void time(Store.Slot slot){takenTime(slot,slot.event);}public void undo(Store.Event e){MainActivity.this.undo(e);}
        });
        if(!welcomeVisible&&tab==3)return historyFeed();
        ScrollView page=new ScrollView(this);page.setFillViewport(true);page.setClipToPadding(false);page.setPadding(0,0,0,welcomeVisible?dp(24):FloatingNavBar.preferredHeight(this)+dp(36));
        content=column();content.setPadding(dp(22),dp(24),dp(22),dp(24));page.addView(content);content.addView(brandHeader());space(content,28);
        if(welcomeVisible){welcomePage();return page;}
        if(tab==1)medications();else if(tab==2)dashboard();return page;
    }
    private View brandHeader(){
        LinearLayout wrapper=column(),brand=row();ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.ic_app);logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);logo.setBackground(UiKit.rounded(this,ACCENT,12));logo.setClipToOutline(true);brand.addView(logo,new LinearLayout.LayoutParams(dp(36),dp(36)));
        TextView title=text(getString(R.string.app_name),UiKit.compact(this)?16:19,INK,true);title.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_BALANCED);title.setPadding(dp(8),0,0,0);brand.addView(title,new LinearLayout.LayoutParams(0,-2,1));brand.addView(settingsButton(),new LinearLayout.LayoutParams(dp(48),dp(48)));wrapper.addView(brand);
        if(!welcomeVisible&&AlarmService.ringing()){space(wrapper,12);fullButton(wrapper,getString(R.string.stop_ringing),true,()->{AlarmService.stop(this);render();});}
        if(!welcomeVisible&&!Reminders.allowed(this)){space(wrapper,10);fullButton(wrapper,getString(R.string.enable_reminders),false,this::requestNotifications);}
        return wrapper;
    }
    private View settingsButton(){
        FrameLayout b=(FrameLayout)UiKit.iconButton(this,IconGlyph.Kind.SETTINGS,getString(R.string.settings),false,this::settings);b.setTag("settings-entry");
        if(alarmAccess.needsAttention()){ImageView warning=new ImageView(this);warning.setImageResource(R.drawable.ic_permission_warning);warning.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(17),dp(17),Gravity.RIGHT|Gravity.TOP);b.addView(warning,p);b.setContentDescription(getString(R.string.settings_permission_alert));}return b;
    }
    private void welcomePage(){
        content.setTag("alarm-welcome");content.addView(text(getString(R.string.welcome_title),30,INK,true));space(content,10);
        content.addView(text(getString(R.string.welcome_subtitle),18,ACCENT_INK,true));space(content,8);
        content.addView(text(getString(R.string.welcome_body),15,MUTED,false));space(content,22);
        permissionIntro(content,R.drawable.ic_permission_clock,R.string.welcome_precise_title,R.string.welcome_precise_body,R.string.enable_precise_access,this::alarmSettings);
        permissionIntro(content,R.drawable.ic_permission_screen,R.string.welcome_screen_title,R.string.welcome_screen_body,R.string.enable_screen_access,this::fullScreenSettings);
        space(content,8);content.addView(text(getString(R.string.welcome_later_hint),13,MUTED,false));space(content,14);
        fullButton(content,getString(R.string.welcome_continue),false,()->{welcomeDismissed=true;render();});
    }
    private void permissionIntro(LinearLayout parent,int icon,int title,int description,int action,Runnable callback){
        LinearLayout c=card(parent),heading=row();ImageView image=new ImageView(this);image.setImageResource(icon);image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(32),dp(32));p.rightMargin=dp(12);heading.addView(image,p);heading.addView(text(getString(title),18,INK,true),new LinearLayout.LayoutParams(0,-2,1));c.addView(heading);
        space(c,10);c.addView(text(getString(description),14,MUTED,false));space(c,16);fullButton(c,getString(action),true,callback);
    }
    private void permissionProblem(LinearLayout parent,AlarmPermissions.State access){
        LinearLayout c=card(parent);c.setTag("alarm-permission-problem");c.setBackground(shape(0xFFFFF5E5,18));LinearLayout heading=row();
        ImageView image=new ImageView(this);image.setImageResource(R.drawable.ic_permission_warning);image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(24),dp(24));p.rightMargin=dp(10);heading.addView(image,p);
        heading.addView(text(getString(R.string.permission_attention_title),18,INK,true),new LinearLayout.LayoutParams(0,-2,1));c.addView(heading);space(c,12);
        if(!access.precise){c.addView(text(getString(R.string.permission_precise_missing),14,MUTED,false));space(c,12);fullButton(c,getString(R.string.enable_precise_access),true,this::alarmSettings);space(c,12);}
        if(!access.fullScreen){c.addView(text(getString(R.string.permission_screen_missing),14,MUTED,false));space(c,12);fullButton(c,getString(R.string.enable_screen_access),true,this::fullScreenSettings);}
    }
    private String timeLabel(String value) {return AppLocale.time(this,value);}
    private void heading(String eyebrow,String title,String subtitle){heading(eyebrow,title,subtitle,null,null,null);}
    private void heading(String eyebrow,String title,String subtitle,IconGlyph.Kind icon,String label,Runnable action){
        content.addView(text(eyebrow.toUpperCase(AppLocale.locale(this)),12,ACCENT_INK,true));space(content,8);LinearLayout titleRow=row();titleRow.addView(UiKit.heading(text(title,32,INK,true)),new LinearLayout.LayoutParams(0,-2,1));if(icon!=null)UiKit.addAction(titleRow,this,icon,label,true,action);content.addView(titleRow);space(content,8);content.addView(text(subtitle,14,MUTED,false));space(content,24);
    }
    private void markSlot(Store.Slot slot,String status){
        Runnable action=()->saveSlot(slot,status,System.currentTimeMillis());
        if(status.equals("skipped"))new AlertDialog.Builder(this).setTitle(getString(R.string.skip_confirm_title)).setMessage(getString(R.string.skip_confirm_text,slot.name,timeLabel(slot.time))).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.log_skipped,(d,w)->action.run()).show();
        else if(slot.scheduledAt>System.currentTimeMillis())new AlertDialog.Builder(this).setTitle(R.string.upcoming_confirm_title).setMessage(getString(R.string.upcoming_confirm_text,timeLabel(slot.time))).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.already_taken,(d,w)->action.run()).show();else action.run();
    }
    private void saveSlot(Store.Slot slot,String status,long actual){
        if(LocalDate.parse(slot.day).isAfter(LocalDate.now())){toast(getString(R.string.future_dose));return;}
        Store.Slot current=store.slot(slot.med,slot.day,slot.time);if(current==null){render();toast(getString(R.string.schedule_changed));return;}boolean saved=store.recordSlot(current,status,actual);Reminders.dismiss(this,slot.med,slot.time);render();toast(getString(saved?(status.equals("taken")?R.string.logged_taken:R.string.logged_skipped):R.string.already_logged));
    }
    private void takenTime(Store.Slot slot,Store.Event event){
        long initial=event!=null&&event.takenAt>0?event.takenAt:System.currentTimeMillis();final LocalDateTime[] chosen={LocalDateTime.ofInstant(Instant.ofEpochMilli(initial),ZoneId.systemDefault()).withSecond(0).withNano(0)};
        LinearLayout form=column();form.setPadding(dp(24),dp(12),dp(24),dp(20));form.addView(text(getString(R.string.actual_time_hint),14,MUTED,false));space(form,16);
        Button date=button(AppLocale.date(this,chosen[0].toLocalDate(),true),false,()->{}),time=button(AppLocale.time(this,chosen[0].toLocalTime().toString()),false,()->{});form.addView(date);space(form,12);form.addView(time);
        date.setOnClickListener(v->{DatePickerDialog picker=new DatePickerDialog(this,(view,y,m,d)->{chosen[0]=LocalDateTime.of(LocalDate.of(y,m+1,d),chosen[0].toLocalTime());date.setText(AppLocale.date(this,chosen[0].toLocalDate(),true));},chosen[0].getYear(),chosen[0].getMonthValue()-1,chosen[0].getDayOfMonth());picker.getDatePicker().setMaxDate(System.currentTimeMillis());picker.show();});
        time.setOnClickListener(v->new TimePickerDialog(this,(view,h,m)->{chosen[0]=LocalDateTime.of(chosen[0].toLocalDate(),LocalTime.of(h,m));time.setText(AppLocale.time(this,chosen[0].toLocalTime().toString()));},chosen[0].getHour(),chosen[0].getMinute(),android.text.format.DateFormat.is24HourFormat(this)).show());
        timeEditor=new AlertDialog.Builder(this).setTitle(R.string.edit_taken_time).setView(form).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.save,null).create();
        timeEditor.setOnShowListener(d->timeEditor.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{long actual=chosen[0].atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();if(actual>System.currentTimeMillis()){toast(getString(R.string.actual_future_error));return;}if(event!=null){store.updateTakenTime(event,actual);render();}else saveSlot(slot,"taken",actual);timeEditor.dismiss();}));timeEditor.show();
    }
    private void undo(Store.Event e) {
        new AlertDialog.Builder(this).setTitle(getString(R.string.remove_log_title)).setMessage(getString(R.string.remove_log_text,e.name,AppLocale.date(this,LocalDate.parse(e.day),true),timeLabel(e.time),statusLabel(e.status)))
            .setNegativeButton(getString(R.string.keep_entry),null).setPositiveButton(getString(R.string.remove_entry),(a,b)->{store.undo(e.id);render();}).show();
    }
    private void medications() {
        List<Store.Med> meds=store.meds();heading(getString(R.string.your_collection),getString(R.string.medications),getResources().getQuantityString(R.plurals.active_medications,meds.size(),meds.size()),IconGlyph.Kind.ADD,getString(R.string.add_medication),()->edit(null));
        if(meds.isEmpty()) { LinearLayout c=card(content); c.addView(text(getString(R.string.make_your_own),22,INK,true)); space(c,8); c.addView(text(getString(R.string.medications_empty_hint),15,MUTED,false)); }
        for(Store.Med m:meds) {
            LinearLayout c=card(content);medicationHeader(c,m.name,m.dose,m.iconType,m.iconColor,76,22);space(c,16);
            List<String> times=new ArrayList<>(); for(String t:Schedule.parseTimes(m.times)) times.add(timeLabel(t));
            c.addView(text(getString(R.string.daily_times,String.join(", ",times)),14,ACCENT_INK,true)); space(c,8);
            c.addView(text(m.reminders?getString(R.string.reminders_on) : getString(R.string.reminders_off),13,MUTED,false));
            if(!m.notes.isEmpty()) { space(c,10); c.addView(text(m.notes,14,MUTED,false)); }
            space(c,14);
            if(m.durationDays>0){LocalDate start=LocalDate.parse(m.startDay),end=Treatment.end(m.startDay,m.durationDays),today=LocalDate.now();String label=today.isBefore(start)?getString(R.string.treatment_starts,AppLocale.date(this,start,true)):today.isAfter(end)?getString(R.string.treatment_complete):getString(R.string.treatment_day,Treatment.dayNumber(m.startDay,m.durationDays,today),m.durationDays);c.addView(text(label,14,INK,true));space(c,10);AnimatedLine line=new AnimatedLine(this,Treatment.progress(m.startDay,m.durationDays,today));line.setContentDescription(label);c.addView(line,new LinearLayout.LayoutParams(-1,dp(8)));space(c,8);c.addView(text(getString(R.string.treatment_ends,AppLocale.date(this,end,true)),12,MUTED,false));}else c.addView(text(getString(R.string.no_end_date),12,MUTED,false));
            LinearLayout actions=row();actions.setGravity(Gravity.RIGHT);UiKit.addAction(actions,this,IconGlyph.Kind.EDIT,getString(R.string.edit_medication),false,()->edit(m));c.addView(actions);
        }
    }
    private View historyFeed() {
        final List<Store.Event> events=store.events(null);
        ListView feed=new ListView(this);feed.setTag("history-feed");feed.setDivider(null);feed.setSelector(android.R.color.transparent);feed.setCacheColorHint(0);feed.setClipToPadding(false);feed.setPadding(0,0,0,FloatingNavBar.preferredHeight(this)+dp(36));
        content=column();content.setPadding(dp(22),dp(24),dp(22),0);content.addView(brandHeader());space(content,28);
        heading(getString(R.string.your_record),getString(R.string.dose_history),getString(R.string.history_subtitle),IconGlyph.Kind.EXPORT,getString(R.string.export_csv),this::export);
        feed.addHeaderView(content,null,false);
        feed.setAdapter(new BaseAdapter(){
            public int getCount(){return Math.max(1,events.size());}
            public Object getItem(int position){return events.isEmpty()?null:events.get(position);}
            public long getItemId(int position){return events.isEmpty()?0:events.get(position).id;}
            public View getView(int position,View recycled,ViewGroup parent){
                LinearLayout row=column();row.setPadding(dp(22),0,dp(22),0);row.setLayoutParams(new AbsListView.LayoutParams(-1,-2));
                if(events.isEmpty()){LinearLayout empty=card(row);empty.addView(text(getString(R.string.history_empty_title),22,INK,true));space(empty,10);empty.addView(text(getString(R.string.history_empty_hint),15,MUTED,false));return row;}
                Store.Event e=events.get(position);
                if(position==0||!events.get(position-1).day.equals(e.day)){space(row,8);row.addView(UiKit.heading(text(AppLocale.date(MainActivity.this,LocalDate.parse(e.day),true),14,ACCENT_INK,true)));space(row,12);}
                historyCard(row,e);return row;
            }
        });return feed;
    }
    private void historyCard(LinearLayout row,Store.Event e){
        LinearLayout c=card(row);medicationHeader(c,e.name,e.dose,e.iconType,e.iconColor,48,20);space(c,10);c.addView(text(getString(R.string.scheduled_time,timeLabel(e.time)),14,MUTED,false));space(c,10);
        c.addView(text(e.status.equals("taken")?getString(R.string.taken_badge):getString(R.string.skipped_badge),15,e.status.equals("taken")?ACCENT_INK:MUTED,true)); space(c,5);
        c.addView(text(getString(R.string.logged_time,AppLocale.timestamp(this,e.recorded)),12,MUTED,false));
        if(e.status.equals("taken")){space(c,7);if(e.takenAt>0)c.addView(text(getString(R.string.actual_at,AppLocale.timestamp(this,e.takenAt)),14,INK,true));c.addView(text(TimingText.difference(this,e),13,MUTED,false));}
        space(c,12);LinearLayout actions=row();actions.setGravity(Gravity.RIGHT);if(e.status.equals("taken"))UiKit.addAction(actions,this,IconGlyph.Kind.CLOCK,getString(R.string.edit_taken_time),false,()->takenTime(null,e));UiKit.addAction(actions,this,IconGlyph.Kind.UNDO,getString(R.string.undo_log),false,()->undo(e));c.addView(actions);
    }
    private void dashboard(){
        LocalDate today=LocalDate.now(),from=today.minusDays(dashboardDays-1);store.ensurePlans(today,System.currentTimeMillis());Adherence.Report report=Adherence.summarize(store.samples(from,today),from,today,System.currentTimeMillis());
        heading(getString(R.string.your_insights),getString(R.string.dashboard),getString(R.string.accuracy_subtitle));
        LinearLayout ranges=row();for(int days:new int[]{7,30}){Button b=button(getString(R.string.days_range,days),dashboardDays==days,()->changeDashboardRange(days));b.setTag("dashboard-range-"+days);b.setSelected(dashboardDays==days);UiKit.choice(b);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(0,0,dp(days==7?8:0),0);ranges.addView(b,p);}content.addView(ranges);space(content,18);
        LinearLayout hero=card(content);hero.setBackground(UiKit.glowCard(this));hero.addView(text(getString(R.string.on_time_accuracy),12,ACCENT_INK,true));space(hero,6);LinearLayout metric=UiKit.compact(this)?column():row();TextView percentage=text(report.accuracy()<0?"-":getString(R.string.percent_value,report.accuracy()),40,INK,true);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-2,-2);pp.rightMargin=dp(18);percentage.setFontFeatureSettings("tnum");if(report.accuracy()<0)percentage.setContentDescription(getString(R.string.no_timing_data));metric.addView(percentage,pp);LinearLayout detail=column();detail.addView(text(getString(R.string.timing_sample,report.onTime,report.knownTiming),13,MUTED,false));space(detail,4);detail.addView(text(getString(R.string.on_time_window),12,ACCENT_INK,true));metric.addView(detail,UiKit.compact(this)?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1));hero.addView(metric);
        LinearLayout graph=card(content);graph.addView(UiKit.heading(text(getString(R.string.daily_consistency),20,INK,true)));space(graph,12);String desc=getString(R.string.chart_summary,report.onTime,report.outside,report.skipped,report.unlogged);graph.addView(new AdherenceChart(this,report,desc),new LinearLayout.LayoutParams(-1,AdherenceChart.preferredHeight(this)));space(graph,12);
        int[] colors=AdherenceChart.colors(this),labels={R.string.on_time,R.string.outside_window,R.string.status_skipped,R.string.not_logged};int[] counts={report.onTime,report.outside,report.skipped,report.unlogged};for(int i=0;i<4;i++){LinearLayout legend=row();TextView dot=text("●",13,colors[i],true);legend.addView(dot,new LinearLayout.LayoutParams(dp(22),-2));legend.addView(text(getString(labels[i]),13,MUTED,false),new LinearLayout.LayoutParams(0,-2,1));legend.addView(text(Integer.toString(counts[i]),14,INK,true));graph.addView(legend);space(graph,7);}
        LinearLayout stats=card(content);stats.addView(text(getString(R.string.average_difference),14,MUTED,false));space(stats,5);stats.addView(text(report.averageDifferenceMinutes()<0?"-":getString(R.string.minutes_value,report.averageDifferenceMinutes()),28,INK,true));space(stats,14);stats.addView(text(getString(R.string.logged_total,report.taken,report.skipped),14,MUTED,false));
        LinearLayout info=card(content);info.addView(text(getString(R.string.window_explanation),13,MUTED,false));space(info,9);info.addView(text(getString(R.string.tracking_coverage,AppLocale.timestamp(this,store.trackingStarted())),13,MUTED,false));space(info,9);info.addView(text(getString(R.string.missing_explanation),13,MUTED,false));space(info,9);info.addView(text(getString(R.string.legacy_explanation),13,MUTED,false));
    }
    private void changeDashboardRange(int days){
        View focused=root.findFocus();boolean restore=focused!=null&&focused.getTag()!=null&&focused.getTag().toString().startsWith("dashboard-range-");
        dashboardDays=days;render();if(restore){View selected=root.findViewWithTag("dashboard-range-"+days);selected.requestFocus();}
    }
    private LinearLayout timesBox;
    private void edit(Store.Med existing) {
        editingId=existing==null?0:existing.id; formTimes.clear(); formTimes.addAll(Schedule.parseTimes(existing==null?"08:00":existing.times));
        formIconType=existing==null?"capsule":existing.iconType;formIconColor=existing==null?"default":existing.iconColor;
        ScrollView sc=new ScrollView(this); LinearLayout form=column(); form.setPadding(dp(24),dp(10),dp(24),dp(16)); sc.addView(form);
        form.addView(text(getString(R.string.prescription_hint),14,MUTED,false)); space(form,16);
        formName=field(form,getString(R.string.medication_name),existing==null?"":existing.name,getString(R.string.medication_name_hint),false); formName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS); formName.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(100)});
        formDose=field(form,getString(R.string.dose),existing==null?"":existing.dose,getString(R.string.dose_hint),false); formDose.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(150)});
        form.addView(text(getString(R.string.medication_icon),14,INK,true));space(form,10);
        LinearLayout iconRow=row();formIconPreview=new MedIconView(this,formIconType,formIconColor);formIconPreview.setTag("med-icon-preview");
        LinearLayout.LayoutParams iconSize=new LinearLayout.LayoutParams(dp(80),dp(80));iconSize.rightMargin=dp(14);iconRow.addView(formIconPreview,iconSize);
        LinearLayout iconWords=column();formIconLabel=text("",15,INK,true);iconWords.addView(formIconLabel);space(iconWords,10);fullButton(iconWords,getString(R.string.choose_icon),false,()->chooseIcon(formIconType,formIconColor));iconRow.addView(iconWords,new LinearLayout.LayoutParams(0,-2,1));form.addView(iconRow);refreshIconForm();
        space(form,10);form.addView(text(getString(R.string.icon_hint),12,MUTED,false));space(form,20);
        form.addView(text(getString(R.string.dose_times),14,INK,true)); space(form,8); timesBox=column(); form.addView(timesBox); rebuildTimes();
        fullButton(form,getString(R.string.add_time),false,()-> {
            if(formTimes.size()>=12) {toast(getString(R.string.times_limit));return;}
            TimePickerDialog picker=new TimePickerDialog(this,(v,h,m)->{formTimes.add(String.format(Locale.US,"%02d:%02d",h,m));rebuildTimes();},8,0,android.text.format.DateFormat.is24HourFormat(this)); picker.show();
        }); space(form,18);
        formStartDay=existing==null?LocalDate.now().toString():existing.startDay;form.addView(text(getString(R.string.treatment_start),14,INK,true));space(form,8);formStart=button(AppLocale.date(this,LocalDate.parse(formStartDay),true),false,()->{LocalDate d=LocalDate.parse(formStartDay);new DatePickerDialog(this,(v,y,m,day)->{formStartDay=LocalDate.of(y,m+1,day).toString();formStart.setText(AppLocale.date(this,LocalDate.parse(formStartDay),true));},d.getYear(),d.getMonthValue()-1,d.getDayOfMonth()).show();});form.addView(formStart);space(form,16);
        formDays=field(form,getString(R.string.treatment_days),existing==null||existing.durationDays==0?"":Integer.toString(existing.durationDays),getString(R.string.undefined_duration),false);formDays.setInputType(InputType.TYPE_CLASS_NUMBER);formDays.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4)});form.addView(text(getString(R.string.duration_hint),12,MUTED,false));space(form,18);
        formNotes=field(form,getString(R.string.instructions),existing==null?"":existing.notes,getString(R.string.instructions_hint),true); formNotes.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(1000)});
        formReminders=new Switch(this); formReminders.setText(getString(R.string.remind_me)); formReminders.setTextColor(INK); formReminders.setTextSize(15); formReminders.setChecked(existing==null||existing.reminders); formReminders.setPadding(0,dp(8),0,dp(8)); formReminders.setMinHeight(dp(48)); form.addView(formReminders);
        space(form,8); form.addView(text(getString(R.string.alarm_schedule_hint),12,MUTED,false));
        if(existing!=null) { space(form,22); fullButton(form,getString(R.string.archive_medication),false,()->archive(existing)); }
        editorInitialValues=editorValues();
        editor=new AlertDialog(this){@Override public void cancel(){requestEditorDismiss();}};
        editor.setTitle(existing==null?getString(R.string.add_medication):getString(R.string.edit_medication));editor.setView(sc);
        editor.setButton(AlertDialog.BUTTON_NEGATIVE,getString(R.string.cancel),(d,w)->{});editor.setButton(AlertDialog.BUTTON_POSITIVE,getString(R.string.save),(d,w)->{});
        editor.setOnShowListener(dialog->editor.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String name=formName.getText().toString().trim(),dose=formDose.getText().toString().trim();
            if(name.isEmpty()) {formName.setError(getString(R.string.name_required));formName.requestFocus();return;}
            if(dose.isEmpty()) {formDose.setError(getString(R.string.dose_required));formDose.requestFocus();return;}
            if(formTimes.isEmpty()) {toast(getString(R.string.time_required));return;}
            Store.Med m=new Store.Med(); m.id=editingId; m.name=name; m.dose=dose; m.notes=formNotes.getText().toString().trim(); m.times=String.join(",",formTimes); m.reminders=formReminders.isChecked();
            m.iconType=formIconType;m.iconColor=formIconColor;m.startDay=formStartDay;String days=formDays.getText().toString().trim();if(!days.isEmpty()){try{m.durationDays=Integer.parseInt(days);}catch(NumberFormatException ex){m.durationDays=-1;}if(m.durationDays<1||m.durationDays>3650){formDays.setError(getString(R.string.duration_error));formDays.requestFocus();return;}}
            try { store.save(m); } catch(android.database.SQLException error) {toast(getString(R.string.save_failed));return;}
            if(existing!=null) Reminders.cancel(this,existing); Reminders.reschedule(this); editor.dismiss(); render(); toast(getString(R.string.medication_saved));
            if(m.reminders&&!Reminders.allowed(this)) requestNotifications();
        })); editor.show();editor.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v->requestEditorDismiss()); editor.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    private List<String> editorValues(){return Arrays.asList(formName.getText().toString(),formDose.getText().toString(),formNotes.getText().toString(),String.join(",",formTimes),formStartDay,formDays.getText().toString(),Boolean.toString(formReminders.isChecked()),formIconType,formIconColor);}
    private void requestEditorDismiss(){
        if(editor==null||!editor.isShowing())return;
        if(editorValues().equals(editorInitialValues)){editor.dismiss();return;}
        if(discardDialog!=null&&discardDialog.isShowing())return;
        discardDialog=new AlertDialog.Builder(this).setTitle(R.string.discard_changes_title).setMessage(R.string.discard_changes_hint)
            .setNegativeButton(R.string.keep_editing,null).setPositiveButton(R.string.discard_changes,(d,w)->editor.dismiss()).create();
        discardDialog.setOnDismissListener(d->discardDialog=null);discardDialog.show();
    }
    private void refreshIconForm(){
        if(formIconPreview!=null)formIconPreview.setAppearance(formIconType,formIconColor);
        if(formIconLabel!=null)formIconLabel.setText(getString(R.string.icon_choice_label,MedAppearance.typeLabel(this,formIconType),MedAppearance.colorLabel(this,formIconColor)));
    }
    private void chooseIcon(String type,String color){
        pickerIconType=MedAppearance.type(type);pickerIconColor=MedAppearance.color(color);
        ScrollView scroll=new ScrollView(this);iconPickerContent=column();iconPickerContent.setPadding(dp(18),dp(8),dp(18),dp(20));scroll.addView(iconPickerContent);renderIconPicker();
        iconPicker=new AlertDialog.Builder(this).setTitle(getString(R.string.choose_icon_title)).setView(scroll).setNegativeButton(getString(R.string.cancel),null)
            .setPositiveButton(getString(R.string.use_icon),(d,w)->{formIconType=pickerIconType;formIconColor=pickerIconColor;refreshIconForm();}).create();
        iconPicker.setOnDismissListener(d->iconPicker=null);iconPicker.show();iconPicker.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    private void renderIconPicker(){
        View focused=iconPickerContent.findFocus();Object focusedTag=focused==null?null:focused.getTag();
        iconPickerContent.removeAllViews();
        LinearLayout preview=row();MedIconView illustration=new MedIconView(this,pickerIconType,pickerIconColor);illustration.setTag("picker-preview");
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(88),dp(88));p.rightMargin=dp(16);preview.addView(illustration,p);
        LinearLayout captions=column();captions.addView(text(MedAppearance.typeLabel(this,pickerIconType),22,INK,true));space(captions,5);captions.addView(text(MedAppearance.colorLabel(this,pickerIconColor),14,MUTED,false));preview.addView(captions,new LinearLayout.LayoutParams(0,-2,1));iconPickerContent.addView(preview);space(iconPickerContent,20);
        iconPickerContent.addView(text(getString(R.string.icon_type),12,ACCENT_INK,true));space(iconPickerContent,8);
        for(int start=0;start<MedAppearance.TYPES.length;start+=3){
            LinearLayout choices=row();
            for(int i=start;i<Math.min(start+3,MedAppearance.TYPES.length);i++){
                String choice=MedAppearance.TYPES[i];boolean selected=choice.equals(pickerIconType);
                LinearLayout tile=column();tile.setGravity(Gravity.CENTER);tile.setPadding(dp(4),dp(8),dp(4),dp(7));tile.setTag("icon-type-"+choice);tile.setSelected(selected);
                GradientDrawable bg=shape(selected?PALE:Color.WHITE,14);bg.setStroke(dp(selected?2:1),selected?ACCENT_INK:LINE);tile.setBackground(UiKit.interactive(this,bg,14));UiKit.choice(tile);
                MedIconView icon=new MedIconView(this,choice,pickerIconColor);icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);tile.addView(icon,new LinearLayout.LayoutParams(dp(50),dp(50)));space(tile,5);
                TextView label=text(MedAppearance.typeLabel(this,choice),11,INK,selected);label.setGravity(Gravity.CENTER);label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);tile.addView(label);
                String description=getString(R.string.icon_type_description,MedAppearance.typeLabel(this,choice));tile.setContentDescription(selected?getString(R.string.selected_description,description):description);tile.setFocusable(true);tile.setClickable(true);tile.setOnClickListener(v->{pickerIconType=choice;renderIconPicker();});
                LinearLayout.LayoutParams t=new LinearLayout.LayoutParams(0,-2,1);t.setMargins(dp(3),dp(3),dp(3),dp(3));choices.addView(tile,t);
            }iconPickerContent.addView(choices);
        }
        space(iconPickerContent,18);iconPickerContent.addView(text(getString(R.string.icon_color),12,ACCENT_INK,true));space(iconPickerContent,8);
        for(int start=0;start<MedAppearance.COLORS.length;start+=3){
            LinearLayout choices=row();
            for(int i=start;i<Math.min(start+3,MedAppearance.COLORS.length);i++){
                String choice=MedAppearance.COLORS[i];boolean selected=choice.equals(pickerIconColor);
                LinearLayout tile=column();tile.setGravity(Gravity.CENTER);tile.setPadding(dp(4),dp(8),dp(4),dp(7));tile.setTag("icon-color-"+choice);tile.setSelected(selected);
                GradientDrawable bg=shape(selected?PALE:Color.WHITE,12);bg.setStroke(dp(selected?2:1),selected?ACCENT_INK:LINE);tile.setBackground(UiKit.interactive(this,bg,12));UiKit.choice(tile);
                TextView swatch=text(selected?"✓":"",18,INK,true);swatch.setGravity(Gravity.CENTER);swatch.setBackground(shape(MedAppearance.tint(pickerIconType,choice),18));swatch.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);tile.addView(swatch,new LinearLayout.LayoutParams(dp(30),dp(30)));space(tile,5);
                TextView label=text(MedAppearance.colorLabel(this,choice),11,INK,selected);label.setGravity(Gravity.CENTER);label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);tile.addView(label);
                String description=getString(R.string.icon_color_description,MedAppearance.colorLabel(this,choice));tile.setContentDescription(selected?getString(R.string.selected_description,description):description);tile.setFocusable(true);tile.setClickable(true);tile.setOnClickListener(v->{pickerIconColor=choice;renderIconPicker();});
                LinearLayout.LayoutParams t=new LinearLayout.LayoutParams(0,-2,1);t.setMargins(dp(3),dp(3),dp(3),dp(3));choices.addView(tile,t);
            }iconPickerContent.addView(choices);
        }
        if(focusedTag!=null){View replacement=iconPickerContent.findViewWithTag(focusedTag);if(replacement!=null)replacement.requestFocus();}
    }
    private EditText field(LinearLayout parent,String label,String value,String hint,boolean multiline) {
        TextView caption=text(label,14,INK,true);parent.addView(caption); space(parent,6); EditText e=new EditText(this);e.setId(View.generateViewId());caption.setLabelFor(e.getId());caption.setOnClickListener(v->e.requestFocus());caption.setFocusable(false); e.setText(value); e.setHint(hint); e.setTextSize(16); e.setTextColor(INK); e.setSingleLine(!multiline);
        e.setInputType(InputType.TYPE_CLASS_TEXT | (multiline?InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES:InputType.TYPE_TEXT_FLAG_CAP_SENTENCES));
        e.setPadding(dp(12),dp(12),dp(12),dp(12));e.setBackground(UiKit.fieldBackground(this));
        e.setMinHeight(dp(50)); if(multiline) {e.setMinLines(2);e.setMaxLines(5);} parent.addView(e,new LinearLayout.LayoutParams(-1,-2)); space(parent,18); return e;
    }
    private void rebuildTimes() {
        if(timesBox==null) return; timesBox.removeAllViews();
        for(String time:new ArrayList<>(formTimes)) {
            LinearLayout r=row(); r.setPadding(0,dp(4),0,dp(4)); r.addView(text(timeLabel(time),17,ACCENT_INK,true),new LinearLayout.LayoutParams(0,-2,1));
            Button remove=button(getString(R.string.remove),false,()->{formTimes.remove(time);rebuildTimes();}); remove.setContentDescription(getString(R.string.remove_time_description,timeLabel(time))); r.addView(remove); timesBox.addView(r);
        }
    }
    private void archive(Store.Med m) {
        new AlertDialog.Builder(this).setTitle(getString(R.string.archive_title,m.name)).setMessage(getString(R.string.archive_hint))
            .setNegativeButton(getString(R.string.cancel),null).setPositiveButton(getString(R.string.archive),(d,w)-> {store.archive(m.id);Reminders.cancel(this,m);editor.dismiss();render();toast(getString(R.string.medication_archived));}).show();
    }
    private void requestNotifications() {
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED && !getPreferences(0).getBoolean("notificationAsked",false)) {
            getPreferences(0).edit().putBoolean("notificationAsked",true).apply(); requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},100);
        } else openSettings(new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()).putExtra(Settings.EXTRA_CHANNEL_ID,Reminders.ALARM_CHANNEL));
    }
    @Override public void onRequestPermissionsResult(int request,String[] perms,int[] results) { super.onRequestPermissionsResult(request,perms,results); render(); }
    private void alarmSettings() {
        if(Build.VERSION.SDK_INT>=31) openSettings(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));
        else toast(getString(R.string.precise_available));
    }
    private void fullScreenSettings() {
        if(Build.VERSION.SDK_INT>=34)openSettings(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,Uri.parse("package:"+getPackageName())));
        else toast(getString(R.string.full_screen_available));
    }
    private void openSettings(Intent i) { returningFromChild=true;try { startActivity(i); } catch(ActivityNotFoundException e) { try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))); } catch(ActivityNotFoundException ignored) {toast(getString(R.string.open_app_settings));} } }
    private void settings() {
        LinearLayout l=column(); l.setPadding(dp(24),dp(12),dp(24),dp(16));
        AlarmPermissions.State access=AlarmPermissions.read(this);if(access.needsAttention())permissionProblem(l,access);
        String[] tags=getResources().getStringArray(R.array.language_tags),names=getResources().getStringArray(R.array.language_names);String current=names[0];for(int i=0;i<tags.length;i++)if(tags[i].equals(AppLocale.tag(this)))current=names[i];
        fullButton(l,getString(R.string.language_setting,current),false,this::chooseLanguage);space(l,10);l.addView(text(getString(R.string.language_hint),13,MUTED,false));space(l,22);
        l.addView(text(getString(R.string.reminders_heading),12,ACCENT_INK,true)); space(l,10);
        l.addView(text(getString(R.string.permission_summary,getString(Reminders.allowed(this)?R.string.permission_allowed:R.string.permission_off),getString(Reminders.precise(this)?R.string.permission_allowed:R.string.permission_not_allowed),getString(Reminders.fullScreen(this)?R.string.permission_allowed:R.string.permission_not_allowed)),15,INK,false)); space(l,14);
        fullButton(l,getString(R.string.notification_settings),false,this::requestNotifications); space(l,10);
        if(Build.VERSION.SDK_INT>=31) {fullButton(l,getString(R.string.precise_settings),false,this::alarmSettings);space(l,10);}
        if(Build.VERSION.SDK_INT>=34) {fullButton(l,getString(R.string.full_screen_settings),false,this::fullScreenSettings);space(l,10);}
        fullButton(l,getString(R.string.start_test_alarm),true,this::testAlarm);space(l,10);
        l.addView(text(getString(R.string.test_alarm_hint),13,MUTED,false));space(l,12);
        fullButton(l,getString(R.string.send_test_notification),false,this::testNotification); space(l,22);
        l.addView(text(getString(R.string.on_this_phone),12,ACCENT_INK,true)); space(l,10);
        l.addView(text(getString(R.string.privacy_text),14,MUTED,false)); space(l,16);
        l.addView(text(getString(R.string.alarm_behavior_text),13,MUTED,false)); space(l,16);
        l.addView(text(getString(R.string.version_label,getString(R.string.app_name),getString(R.string.version_name)),12,MUTED,false));
        ScrollView sc=new ScrollView(this);sc.addView(l);settingsDialog=new AlertDialog.Builder(this).setTitle(getString(R.string.settings_privacy)).setView(sc).setPositiveButton(getString(R.string.done),(d,w)->render()).create();settingsDialog.show();
    }
    private void chooseLanguage(){
        String[] tags=getResources().getStringArray(R.array.language_tags),names=getResources().getStringArray(R.array.language_names);int selected=0;for(int i=0;i<tags.length;i++)if(tags[i].equals(AppLocale.tag(this)))selected=i;
        languageDialog=new AlertDialog.Builder(this).setTitle(getString(R.string.choose_language)).setSingleChoiceItems(names,selected,(d,which)->{
            d.dismiss();if(tags[which].equals(AppLocale.tag(this)))return;AppLocale.set(this,tags[which]);Reminders.channel(this);AlarmService.refreshLocale();recreate();
        }).setNegativeButton(getString(R.string.cancel),null).create();languageDialog.show();
    }
    private String statusLabel(String code){return getString("taken".equals(code)?R.string.status_taken:R.string.status_skipped);}
    private void testAlarm() {
        if(!Reminders.allowed(this)){requestNotifications();return;}
        if(!AlarmService.start(this,-1,LocalDate.now().toString(),"test",true)){toast(getString(R.string.alarm_start_failed));return;}
        returningFromChild=true;startActivity(new Intent(this,AlarmActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }
    private void testNotification() {
        if(!Reminders.allowed(this)) {requestNotifications();return;}
        PendingIntent pi=Reminders.openTracker(this);
        Notification n=new Notification.Builder(this,Reminders.ALARM_CHANNEL).setSmallIcon(R.drawable.ic_notification).setContentTitle(getString(R.string.test_notification_title))
            .setContentText(getString(R.string.test_notification_text)).setVisibility(Notification.VISIBILITY_PRIVATE).setContentIntent(pi).setAutoCancel(true).build();
        try { getSystemService(NotificationManager.class).notify("test",2,n);toast(getString(R.string.test_notification_sent)); } catch(SecurityException e) {requestNotifications();}
    }
    private void export() {
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/csv").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,getString(R.string.csv_filename,LocalDate.now().toString()));
        try {returningFromChild=true;startActivityForResult(i,201);} catch(ActivityNotFoundException e) {toast(getString(R.string.no_document_provider));}
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=201||result!=RESULT_OK||data==null||data.getData()==null) return;
        try(OutputStream out=getContentResolver().openOutputStream(data.getData(),"wt")) {
            if(out==null) throw new IOException("No output stream");
            out.write(HistoryExport.csv(this,store.events(null)).getBytes(StandardCharsets.UTF_8));toast(getString(R.string.history_exported));
        } catch(IOException|SecurityException ex) {toast(getString(R.string.export_failed));}
    }
    private void toast(String message) { Toast.makeText(this,message,Toast.LENGTH_SHORT).show(); }
}
