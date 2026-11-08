package com.personal.medtracker;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.time.*;
import java.util.*;

/** Runs only on a disposable emulator; resets the target app database. */
public final class AppInstrumentation extends Instrumentation {
    private int count;
    private Activity activity;
    private volatile Activity resumedMain;
    private boolean navOnly,gallery,uiAudit;
    private String permissionScenario,testLanguage="en";
    @Override public void callActivityOnResume(Activity a){super.callActivityOnResume(a);if(a instanceof MainActivity)resumedMain=a;}
    private final StringBuilder report=new StringBuilder();
    private void check(boolean value,String label) { if(!value) throw new AssertionError(label); count++; report.append("PASS: ").append(label).append('\n');android.util.Log.i("MedTrackerTests","PASS: "+label); }
    @Override public void onCreate(Bundle args) { super.onCreate(args);navOnly=args!=null&&"true".equals(args.getString("navOnly"));if(args!=null){uiAudit="true".equals(args.getString("uiAudit"));gallery="true".equals(args.getString("gallery"));permissionScenario=args.getString("permissionState");testLanguage=args.getString("testLocale","en");}start(); }
    @Override public void onStart() {
        Bundle results=new Bundle();
        try {
            Context c=getTargetContext();AppLocale.set(c,testLanguage);waitForIdleSync();reset(c);c.getSystemService(NotificationManager.class).cancelAll();
            if(permissionScenario!=null){permissionFlow(c);results.putString("stream",report.toString()+"PASS: "+count+" alarm permission flow checks\n");finish(Activity.RESULT_OK,results);return;}
            if(gallery){gallery(c);results.putString("stream",report.toString()+"PASS: gallery captured\n");finish(Activity.RESULT_OK,results);return;}
            if(uiAudit){uiAudit(c);results.putString("stream",report.toString()+"PASS: "+count+" UI audit checks\n");finish(Activity.RESULT_OK,results);return;}
            permissionPolicy();migration(c);tracking(c);
            if(navOnly){activity=startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();navigation(c);runOnMainSync(()->activity.finish());results.putString("stream",report.toString()+"PASS: "+count+" focused navigation and migration checks\n");finish(Activity.RESULT_OK,results);return;}
            check(AppLocale.supported(c,"fr").equals("en"),"Unsupported languages fall back to English");
            check(AppLocale.supported(c,"es-MX").equals("es"),"Regional Spanish falls back to the shared Spanish translation");
            String day=LocalDate.now().toString(); Store.Med m=new Store.Med(); m.name="Verification medication";m.dose="Test dose";m.notes="Emulator test only";m.times="08:00,20:00";m.reminders=true;
            try(Store s=new Store(c)) {
                s.save(m); check(m.id>0,"Medication receives a stable ID"); check(s.meds().size()==1,"Medication saved to SQLite");
                check(s.med(m.id).times.equals("08:00,20:00"),"Multiple dose times persist");
                check(s.med(m.id).iconType.equals("capsule")&&s.med(m.id).iconColor.equals("default"),"New medications keep the default icon without an explicit selection");
                check(s.record(m,day,"08:00","taken"),"Taken dose is saved"); check(!s.record(m,day,"08:00","taken"),"Duplicate logging is rejected");
                check(!s.record(m,day,"08:00","skipped"),"A logged dose cannot be overwritten accidentally");
                check(s.events(day).size()==1,"Duplicate attempts do not add history");
                check(s.record(m,day,"20:00","skipped"),"Skipped dose is saved independently");
                m.name="Updated medication";m.dose="Changed dose";s.save(m);
                check(s.events(day).get(0).name.equals("Verification medication"),"History retains original name after editing");
                check(s.events(day).get(0).dose.equals("Test dose"),"History retains original dose after editing");
                Store.Event e=s.events(day).get(0);s.undo(e.id);check(!s.handled(m.id,day,e.time),"Undo returns the dose to unlogged");
                Reminders.reschedule(c);
                check(Reminders.allowed(c),"Notification permission is enabled in emulator");
                check(Reminders.precise(c),"Precise alarm permission is enabled in emulator");
                check(c.getSystemService(AlarmManager.class).getNextAlarmClock()!=null,"Dose schedules register as Android alarm-clock alarms");
                // Exercise the actual receiver at an already-due time.
                String due=LocalTime.now().minusMinutes(1).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                Reminders.cancel(c,m);m.times=due;m.durationDays=1;s.save(m);
                new ReminderReceiver().onReceive(c,new Intent(c,ReminderReceiver.class).putExtra("med",m.id).putExtra("time",due));
                check(notificationCount(c,1),"Receiver posts a real Android notification");
                check(waitRinging(true),"Due dose starts the foreground ringing service");
                check(waitPlaying(),"Alarm player starts audible playback through the alarm stream");Reminders.reschedule(c);waitForIdleSync();check(AlarmService.ringing()&&waitPlaying(),"Rescheduling after the final course dose preserves its ringing alarm until stopped");
                final boolean[] looping=new boolean[1];runOnMainSync(()->{
                    for(android.service.notification.StatusBarNotification n:c.getSystemService(NotificationManager.class).getActiveNotifications())
                        if(n.getId()==AlarmService.NOTIFICATION_ID)looping[0]=n.getNotification().fullScreenIntent!=null&&n.getNotification().actions!=null&&n.getNotification().actions.length>0;
                });check(looping[0],"Alarm notification contains a full-screen intent and Stop action");
                check(s.record(m,day,due,"taken"),"Due reminder dose can be logged");Reminders.dismiss(c,m.id,due);
                new ReminderReceiver().onReceive(c,new Intent(c,ReminderReceiver.class).putExtra("med",m.id).putExtra("time",due));
                check(notificationCount(c,0),"Handled dose does not notify again");
                check(waitRinging(false),"Logging the ringing dose stops its alarm");
                // Exercise actual timed AlarmManager delivery, not just a direct receiver call.
                String secondDue=LocalTime.now().minusMinutes(2).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                Reminders.cancel(c,m);m.times=secondDue;s.save(m);
                long trigger=System.currentTimeMillis()+8000;Reminders.scheduleAt(c,m,secondDue,trigger);
                check(c.getSystemService(AlarmManager.class).getNextAlarmClock().getTriggerTime()==trigger,"Exact ringing alarm is registered with the requested trigger time");
                check(waitRinging(true),"Android delivers the scheduled alarm to the ringing service");
                check(waitPlaying(),"Scheduled alarm plays continuously");
                int beforeStop=s.events(null).size();new AlarmActions().onReceive(c,new Intent().setAction("com.personal.medtracker.STOP_ALARM"));
                check(waitRinging(false),"Notification Stop action silences a scheduled alarm");
                check(s.events(null).size()==beforeStop,"Stopping the alarm does not log a dose");
                check(AlarmService.start(c,-1,day,"test",true),"Test alarm can be started");check(waitRinging(true)&&waitPlaying(),"Test alarm rings");
                AlarmService.stop(c);check(waitRinging(false),"Test alarm can be stopped");
                check(s.events(null).size()==beforeStop,"Test alarm does not change dose history");
                s.archive(m.id);Reminders.cancel(c,m);check(s.meds().isEmpty(),"Archiving removes active schedule");check(s.med(m.id)==null,"Archived medication cannot trigger reminders");
                check(s.events(null).size()==2,"Archiving preserves dose history");
            }
            try(Store reopened=new Store(c)) {check(reopened.events(null).size()==2,"Dose history survives database close and reopen");}
            reset(c);
            Intent launch=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); activity=startActivitySync(launch);waitForIdleSync();
            runOnMainSync(()->{
                activity.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                if(Build.VERSION.SDK_INT>=27){activity.setShowWhenLocked(true);activity.setTurnScreenOn(true);}
            });waitForIdleSync();
            check(find("A little breathing room")!=null,"Today opens with the real empty state");
            navigation(c);
            // Test the actual alarm screen and its Stop button separately from the main tracker.
            check(AlarmService.start(c,-1,day,"test",true),"Alarm screen test starts its alarm");check(waitRinging(true),"Alarm is active before opening the alarm screen");
            Activity alarmScreen=startActivitySync(new Intent(c,AlarmActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
            runOnMainSync(()->{View stop=match(alarmScreen.getWindow().getDecorView(),"Stop alarm");if(stop==null)throw new AssertionError("Missing alarm screen Stop button");stop.performClick();});
            check(waitRinging(false),"Alarm screen Stop button silences the alarm");waitForIdleSync();
            click("Add medication");waitForIdleSync();
            click("Save");check(find("Add medication")!=null,"Empty medication form stays open for validation");
            List<EditText> edits=new ArrayList<>();runOnMainSync(()->collectEdits(activeRoot(),edits));check(edits.size()>=2,"Medication form exposes editable text fields");
            setText(edits.get(0),"UI verification medication");setText(edits.get(1),"1 test tablet");
            check(iconDescription("med-icon-preview").equals("Pill icon, Default"),"Medication form shows the default pill preview");
            click("Choose icon");clickTag("icon-type-tablet");clickTag("icon-color-rose");
            check(iconDescription("picker-preview").equals("Tablet icon, Rose"),"Picker updates its shape and color preview immediately");
            snapshot(c,"icon-picker.png",activeRoot());click("Cancel");
            check(iconDescription("med-icon-preview").equals("Pill icon, Default"),"Canceling the icon picker keeps the previous choice");
            click("Choose icon");clickTag("icon-type-tablet");clickTag("icon-color-rose");click("Use this icon");
            check(iconDescription("med-icon-preview").equals("Tablet icon, Rose"),"Confirmed icon selection appears in the medication form");
            // Use an already-due time so this test does not depend on system dialog focus.
            java.lang.reflect.Field times=MainActivity.class.getDeclaredField("formTimes");times.setAccessible(true);
            @SuppressWarnings("unchecked") TreeSet<String> selected=(TreeSet<String>)times.get(activity);runOnMainSync(()->{selected.clear();selected.add("00:00");});
            java.lang.reflect.Field length=MainActivity.class.getDeclaredField("formDays");length.setAccessible(true);setText((EditText)length.get(activity),"10");
            click("Save");waitForIdleSync();
            try(Store s=new Store(c)) {check(s.med(s.meds().get(0).id).durationDays==10,"Medication editor persists optional treatment duration");check(s.meds().size()==1,"Saving the visible form creates a medication");check(s.meds().get(0).iconType.equals("tablet")&&s.meds().get(0).iconColor.equals("rose"),"Saving the form persists the selected icon and color");}
            runOnMainSync(()->((ScheduleFeed)activity.getWindow().getDecorView().findViewWithTag("schedule-feed")).jumpTo(LocalDate.now()));waitForIdleSync();
            check(iconDescription("medication-icon").equals("Tablet icon, Rose"),"Today shows the saved medication icon");
            click("Mark taken now");waitForIdleSync();
            try(Store s=new Store(c)) {check(s.events(day).size()==1&&s.events(day).get(0).status.equals("taken")&&Math.abs(s.events(day).get(0).takenAt-System.currentTimeMillis())<60000,"Schedule action records actual taken time");}
            runOnMainSync(()->((ScheduleFeed)activity.getWindow().getDecorView().findViewWithTag("schedule-feed")).jumpTo(LocalDate.now().plusDays(40)));waitForIdleSync();check(find("Upcoming day")==null,"Finished treatment has no future dose cards");click("Medications");click("Schedule");ScheduleFeed restored=activity.getWindow().getDecorView().findViewWithTag("schedule-feed");check(!restored.lastDay().isBefore(LocalDate.now().plusDays(40)),"Calendar range survives switching tabs");runOnMainSync(()->restored.jumpTo(LocalDate.now()));waitForIdleSync();snapshot(c,"schedule-en.png",activity.getWindow().getDecorView());
            click("History");waitForIdleSync();click("Set actual taken time");check(find("Record when you actually took this dose. This changes the timing record, while keeping the original log date.")!=null,"Actual-time editor explains its audit behavior");click("Save");
            check(find("Dose history")!=null,"History tab opens");check(find("UI verification medication")!=null,"History shows the logged medication");
            check(iconDescription("medication-icon").equals("Tablet icon, Rose"),"History shows the icon captured when the dose was logged");
            click("Dashboard");check(find("Your daily rhythm")!=null,"Dashboard shows its accuracy graph");snapshot(c,"dashboard-en.png",activity.getWindow().getDecorView());click("30 days");check(find("30 days")!=null,"Dashboard range changes to 30 days");
            click("Medications");waitForIdleSync();check(find("Edit medication")!=null,"Medications tab shows saved medication");
            check(iconDescription("medication-icon").equals("Tablet icon, Rose"),"Medications list shows the selected icon");
            check(find("Day 1 of 10")!=null,"Treatment card shows inclusive calendar progress");snapshot(c,"medications.png",activity.getWindow().getDecorView());
            click("Edit medication");check(find("Edit medication")!=null,"Saved medication can be opened for editing");
            check(iconDescription("med-icon-preview").equals("Tablet icon, Rose"),"Editing restores the saved icon selection");
            click("Choose icon");clickTag("icon-type-syrup");clickTag("icon-color-default");click("Use this icon");click("Save");
            try(Store s=new Store(c)){check(s.meds().get(0).iconType.equals("syrup")&&s.meds().get(0).iconColor.equals("default"),"An existing medication can return to a type's default color");check(s.events(day).get(0).iconType.equals("tablet")&&s.events(day).get(0).iconColor.equals("rose"),"Changing an icon preserves historical icon snapshots");}
            click("Edit medication");click("Choose icon");clickTag("icon-type-package");clickTag("icon-color-blue");click("Use this icon");click("Cancel");click("Discard changes");
            try(Store s=new Store(c)){check(s.meds().get(0).iconType.equals("syrup")&&s.meds().get(0).iconColor.equals("default"),"Canceling medication editing does not overwrite its saved appearance");}
            File screenshot=new File(c.getExternalFilesDir(null),"medications.png");
            final Bitmap[] image=new Bitmap[1];runOnMainSync(()->{View v=activity.getWindow().getDecorView();image[0]=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new android.graphics.Canvas(image[0]));});
            try(FileOutputStream f=new FileOutputStream(screenshot)){image[0].compress(Bitmap.CompressFormat.PNG,100,f);}
            spanish(c,day);
            runOnMainSync(()->activity.finish());waitForIdleSync();
            // Leave the emulator clean, matching the APK's first-run state.
            reset(c);
            results.putString("stream",report.toString()+"PASS: "+count+" Android integration checks\n");finish(Activity.RESULT_OK,results);
        } catch(Throwable ex) {android.util.Log.e("MedTrackerTests","Instrumentation failed",ex);StringWriter sw=new StringWriter();ex.printStackTrace(new PrintWriter(sw));results.putString("stream",report.toString()+"FAIL: "+sw);finish(Activity.RESULT_CANCELED,results);}
    }
    private void permissionPolicy(){
        check(AlarmPermissions.shouldWelcome(false,false),"Welcome appears when both alarm permissions are missing");
        check(!AlarmPermissions.shouldWelcome(true,false),"Welcome stays hidden when only full-screen access is missing");
        check(!AlarmPermissions.shouldWelcome(false,true),"Welcome stays hidden when only precise access is missing");
        check(!AlarmPermissions.shouldWelcome(true,true),"Welcome stays hidden when both alarm permissions are allowed");
        check(AlarmPermissions.needsAttention(false,false),"Settings warns when both alarm permissions are missing");
        check(AlarmPermissions.needsAttention(true,false),"Settings warns when full-screen access is missing");
        check(AlarmPermissions.needsAttention(false,true),"Settings warns when precise access is missing");
        check(!AlarmPermissions.needsAttention(true,true),"Settings warning clears when both permissions are allowed");
    }
    private void permissionFlow(Context c)throws Exception{
        boolean precise=permissionScenario.equals("ready")||permissionScenario.equals("missing-full");
        boolean screen=permissionScenario.equals("ready")||permissionScenario.equals("missing-precise");
        AlarmPermissions.State state=AlarmPermissions.read(c);check(state.precise==precise&&state.fullScreen==screen,"Android reports the requested real permission state");
        activity=startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
        boolean greet=!precise&&!screen,problem=!precise||!screen;
        check((activity.getWindow().getDecorView().findViewWithTag("alarm-welcome")!=null)==greet,"Startup greeting follows the both-missing rule");
        View bar=activity.getWindow().getDecorView().findViewWithTag("floating-navigation");check(bar.getVisibility()==(greet?View.GONE:View.VISIBLE),"Welcome and main navigation are shown in the correct state");
        FrameLayout entry=activity.getWindow().getDecorView().findViewWithTag("settings-entry");check((entry.getChildCount()>1)==problem,"Settings alert icon matches the current missing permissions");
        if(problem)check(entry.getContentDescription().toString().equals(activity.getString(R.string.settings_permission_alert)),"Permission warning has a localized accessible description");
        try(Store s=new Store(c)){check(s.meds().isEmpty(),"Permission guidance works before any medication is added");}
        if(greet){
            snapshot(c,"welcome-"+testLanguage+".png",activity.getWindow().getDecorView());
            IntentFilter exactFilter=new IntentFilter(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);exactFilter.addDataScheme("package");ActivityMonitor exact=addMonitor(exactFilter,new ActivityResult(Activity.RESULT_CANCELED,null),true);
            click(activity.getString(R.string.enable_precise_access));check(exact.getHits()==1,"Welcome precise-alarm button opens the matching Android settings action");removeMonitor(exact);
            IntentFilter fullFilter=new IntentFilter(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);fullFilter.addDataScheme("package");ActivityMonitor full=addMonitor(fullFilter,new ActivityResult(Activity.RESULT_CANCELED,null),true);
            click(activity.getString(R.string.enable_screen_access));check(full.getHits()==1,"Welcome full-screen button opens the matching Android settings action");removeMonitor(full);
        }
        clickTag("settings-entry");
        check((activeRoot().findViewWithTag("alarm-permission-problem")!=null)==problem,"Settings details show a repair panel only when an alarm permission is missing");
        if(problem){
            check((find(activity.getString(R.string.permission_precise_missing))!=null)==!precise,"Settings identifies missing precise-alarm access accurately");
            check((find(activity.getString(R.string.permission_screen_missing))!=null)==!screen,"Settings identifies missing full-screen access accurately");
            snapshot(c,"permission-settings-"+permissionScenario+"-"+testLanguage+".png",activeRoot());
        }
        click(activity.getString(R.string.done));
        if(greet){
            click(activity.getString(R.string.welcome_continue));check(activity.getWindow().getDecorView().findViewWithTag("alarm-welcome")==null,"Continue for now opens the tracker without granting permissions");
            entry=activity.getWindow().getDecorView().findViewWithTag("settings-entry");check(entry.getChildCount()>1,"Settings keeps its warning after welcome is skipped");
            Activity previous=activity;runOnMainSync(()->previous.recreate());long until=SystemClock.uptimeMillis()+30000;
            while((resumedMain==previous||resumedMain==null)&&SystemClock.uptimeMillis()<until)SystemClock.sleep(100);waitForIdleSync();activity=resumedMain;
            check(activity!=previous&&activity.getWindow().getDecorView().findViewWithTag("alarm-welcome")==null,"Activity recreation preserves welcome dismissal for the current visit");
            runOnMainSync(()->activity.finish());waitForIdleSync();activity=startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
            check(activity.getWindow().getDecorView().findViewWithTag("alarm-welcome")!=null,"A new app start shows the welcome again if both permissions remain missing");
        }
        runOnMainSync(()->activity.finish());waitForIdleSync();
    }
    private void navigation(Context c)throws Exception{
        String originalScale=shell("settings get global animator_duration_scale").trim();
        shell("settings put global animator_duration_scale 0");waitMotion(false);
        try {
        View decor=activity.getWindow().getDecorView();FloatingNavBar bar=decor.findViewWithTag("floating-navigation");FrameLayout host=decor.findViewWithTag("page-host");
        check(bar!=null&&bar.getElevation()>0,"Navigation is a floating elevated surface");
        View selection=bar.findViewWithTag("nav-selection");check(selection.getWidth()>bar.getWidth()/5&&selection.getHeight()>bar.getHeight()/2,"Selected tab pill has visible measured bounds on the first layout");
        final boolean[] enabled=new boolean[1];runOnMainSync(()->enabled[0]=FloatingNavBar.motionEnabled());
        check(!enabled[0],"Navigation respects disabled system animations");
        clickTag("nav-tab-3");check(find("Your history starts here")!=null,"History opens immediately with animations disabled");
        check(bar==activity.getWindow().getDecorView().findViewWithTag("floating-navigation"),"Tab switching preserves the same navigation bar");
        runOnMainSync(()->bar.findViewWithTag("nav-tab-0").performClick());waitForIdleSync();
        // Shell commands need no accessibility session and must not suspend existing services.
        shell("settings put global animator_duration_scale 1");
        long until=SystemClock.uptimeMillis()+10000;do{runOnMainSync(()->enabled[0]=FloatingNavBar.motionEnabled());if(enabled[0])break;SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<until);
        check(enabled[0],"Navigation enables transitions when system animations are enabled");
        runOnMainSync(()->{bar.findViewWithTag("nav-tab-1").performClick();bar.findViewWithTag("nav-tab-3").performClick();bar.findViewWithTag("nav-tab-0").performClick();bar.findViewWithTag("nav-tab-3").performClick();});
        waitNavigation();
        check(bar.selectedTab()==3&&find("Your history starts here")!=null,"Rapid animated tab changes settle on the last requested view");
        check(host.getChildCount()==1,"Finished transitions release old pages");
        final boolean[] settled=new boolean[1];runOnMainSync(()->settled[0]=host.getChildAt(0).getAlpha()==1&&host.getChildAt(0).getTranslationX()==0);check(settled[0],"Selected page finishes fully visible and aligned");
        View selected=bar.findViewWithTag("nav-tab-3");check(selected.isSelected()&&selected.getContentDescription().toString().contains("History"),"Icon navigation exposes the selected tab to accessibility services");
        runOnMainSync(()->bar.findViewWithTag("nav-tab-0").performClick());waitNavigation();snapshot(c,"floating-nav-en.png",decor);
        shell("settings put global animator_duration_scale 0");
        until=SystemClock.uptimeMillis()+10000;do{runOnMainSync(()->enabled[0]=FloatingNavBar.motionEnabled());if(!enabled[0])break;SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<until);
        check(!enabled[0],"Motion can be disabled again without affecting navigation");
        }finally{shell("null".equals(originalScale)?"settings delete global animator_duration_scale":"settings put global animator_duration_scale "+originalScale);}
    }
    private String shell(String command)throws IOException{
        android.os.ParcelFileDescriptor fd=getUiAutomation(android.app.UiAutomation.FLAG_DONT_USE_ACCESSIBILITY).executeShellCommand(command);
        try(InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[1024];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return out.toString("UTF-8");}
    }
    private void waitMotion(boolean expected){long until=SystemClock.uptimeMillis()+10000;final boolean[] value={false};do{runOnMainSync(()->value[0]=FloatingNavBar.motionEnabled());if(value[0]==expected)return;SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<until);throw new AssertionError("System animation preference did not update");}
    private void waitNavigation(){
        long until=SystemClock.uptimeMillis()+15000;final boolean[] done=new boolean[1];
        do{runOnMainSync(()->{View root=activity.getWindow().getDecorView();FloatingNavBar nav=root.findViewWithTag("floating-navigation");FrameLayout host=root.findViewWithTag("page-host");done[0]=!nav.animating()&&host.getChildCount()==1&&host.getChildAt(0).getAlpha()==1&&host.getChildAt(0).getTranslationX()==0;});if(done[0])return;SystemClock.sleep(50);}while(SystemClock.uptimeMillis()<until);
        throw new AssertionError("Navigation animation did not settle");
    }
    private void spanish(Context c,String day)throws Exception{
        Store.Med before;try(Store s=new Store(c)){before=s.meds().get(0);}
        check(activity.getString(R.string.today).equals("Today"),"English is the initial app language");
        clickTag("settings-entry");click("Language: English");selectLanguage(1,"Hoy");
        check(AppLocale.tag(c).equals("es"),"Language picker persists Spanish using Android app locales");
        check(find("Medicamentos")!=null,"Navigation is translated after switching language");
        snapshot(c,"medications-es.png",activity.getWindow().getDecorView());
        click("Calendario");check(find("Calendario")!=null,"Spanish Today screen uses translated headings");
        check(AppLocale.date(c,LocalDate.of(2026,11,6),false).contains("noviembre"),"Dates use Spanish month names");
        check(activity.getResources().getQuantityString(R.plurals.active_medications,1,1).equals("1 medicamento activo · horarios diarios"),"Spanish singular medication counts are grammatical");
        check(activity.getResources().getQuantityString(R.plurals.active_medications,2,2).equals("2 medicamentos activos · horarios diarios"),"Spanish plural medication counts are grammatical");
        snapshot(c,"today-es.png",activity.getWindow().getDecorView());
        click("Medicamentos");click("Editar medicamento");
        check(iconDescription("med-icon-preview").equals("Icono de Jarabe, color Predeterminado"),"Medication icon descriptions are translated for accessibility");
        List<EditText> fields=new ArrayList<>();runOnMainSync(()->collectEdits(activeRoot(),fields));setText(fields.get(0),"");click("Guardar");
        check(fields.get(0).getError()!=null&&fields.get(0).getError().toString().equals("Introduce el nombre del medicamento"),"Validation messages are translated");
        setText(fields.get(0),before.name);click("Elegir icono");clickTag("icon-type-tablet");clickTag("icon-color-rose");
        check(iconDescription("picker-preview").equals("Icono de Comprimido, color Rosa"),"Icon and color labels are translated");
        snapshot(c,"icons-es.png",activeRoot());click("Cancelar");click("Cancelar");
        click("Historial");check(find("Historial de dosis")!=null,"Dose history is translated");
        try(Store s=new Store(c)){
            String csv=HistoryExport.csv(c,s.events(day));check(csv.contains("Fecha programada")&&csv.contains("\"tomada\""),"CSV headers and status labels use Spanish");
            check(csv.contains(before.name),"Translated exports preserve user-entered medication names");
            Store.Med after=s.med(before.id);check(after.name.equals(before.name)&&after.dose.equals(before.dose)&&after.times.equals(before.times)&&after.iconType.equals(before.iconType)&&after.iconColor.equals(before.iconColor),"Changing language preserves medication data and stored identifiers");
            check(s.events(day).get(0).status.equals("taken"),"Stored dose status codes are independent of the UI language");
        }
        Reminders.channel(c);check(c.getSystemService(NotificationManager.class).getNotificationChannel(Reminders.ALARM_CHANNEL).getName().toString().equals("Alarmas sonoras de medicamentos"),"System notification channel names use the app language");
        check(AlarmService.start(c,-1,day,"test",true)&&waitRinging(true)&&waitPlaying(),"Alarm audio works while Spanish is selected");
        final boolean[] translated=new boolean[1];runOnMainSync(()->{for(android.service.notification.StatusBarNotification n:c.getSystemService(NotificationManager.class).getActiveNotifications())if(n.getId()==AlarmService.NOTIFICATION_ID)translated[0]=n.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE).toString().equals("Alarma de medicamento")&&n.getNotification().actions[0].title.toString().equals("Detener alarma");});
        check(translated[0],"Background alarm notification and Stop action use Spanish");
        Activity screen=startActivitySync(new Intent(c,AlarmActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
        check(screen.getString(R.string.alarm_title).equals("Alarma de medicamento"),"Alarm screen uses the saved Spanish preference");snapshot(c,"alarm-es.png",screen.getWindow().getDecorView());
        runOnMainSync(()->match(screen.getWindow().getDecorView(),"Detener alarma").performClick());check(waitRinging(false),"Spanish Stop button silences the alarm");
        clickTag("settings-entry");click("Idioma: Español");selectLanguage(0,"Today");
        check(AppLocale.tag(c).equals("en")&&activity.getString(R.string.today).equals("Today"),"Language picker can restore English");
        try(Store s=new Store(c)){check(HistoryExport.csv(c,s.events(day)).contains("Scheduled date"),"CSV export returns to English after switching back");}
    }
    private void selectLanguage(int index,String expected)throws Exception{
        Activity previous=activity;runOnMainSync(()->{try{java.lang.reflect.Field f=MainActivity.class.getDeclaredField("languageDialog");f.setAccessible(true);AlertDialog d=(AlertDialog)f.get(previous);ListView l=d.getListView();l.performItemClick(l.getChildAt(index),index,l.getAdapter().getItemId(index));}catch(ReflectiveOperationException ex){throw new AssertionError(ex);}});
        long until=SystemClock.uptimeMillis()+45000;
        do{Activity next=resumedMain;if(next!=null&&next!=previous&&!next.isDestroyed()&&next.getString(R.string.today).equals(expected)){waitForIdleSync();activity=resumedMain;return;}SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<until);
        throw new AssertionError("Language did not refresh the activity");
    }
    private void migration(Context c){
        String name="appearance-migration-test.db";c.deleteDatabase(name);
        try(android.database.sqlite.SQLiteDatabase db=c.openOrCreateDatabase(name,0,null)){
            db.execSQL("CREATE TABLE meds(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,dose TEXT NOT NULL,notes TEXT NOT NULL,times TEXT NOT NULL,reminders INTEGER NOT NULL,active INTEGER NOT NULL DEFAULT 1,created INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE events(id INTEGER PRIMARY KEY AUTOINCREMENT,med INTEGER NOT NULL,day TEXT NOT NULL,time TEXT NOT NULL,name TEXT NOT NULL,dose TEXT NOT NULL,status TEXT NOT NULL,recorded INTEGER NOT NULL,UNIQUE(med,day,time))");
            db.execSQL("INSERT INTO meds VALUES(42,'Existing medication','Existing dose','Existing notes','08:00,20:00',1,1,1690000000)");
            db.execSQL("INSERT INTO meds VALUES(43,'Archived medication','Old dose','','09:00',0,0,1690000001)");
            db.execSQL("INSERT INTO events VALUES(7,42,'2026-11-05','08:00','Original log name','Original dose','taken',1690000010)");db.setVersion(1);
        }
        try(Store s=new Store(c,name)){
            check(s.getReadableDatabase().getVersion()==3,"Version 1 database upgrades additively to version 3");
            Store.Med m=s.med(42);check(m!=null&&m.name.equals("Existing medication")&&m.dose.equals("Existing dose")&&m.notes.equals("Existing notes"),"Upgrade preserves existing medication details and IDs");
            check(m.times.equals("08:00,20:00")&&m.reminders&&m.created==1690000000L,"Upgrade preserves alarm schedules and creation dates");
            check(m.iconType.equals("capsule")&&m.iconColor.equals("default"),"Existing medications receive a default pill icon");
            check(s.med(43)==null&&s.meds().size()==1,"Upgrade preserves archived medication state");
            Store.Event e=s.events(null).get(0);check(e.id==7&&e.med==42&&e.name.equals("Original log name")&&e.dose.equals("Original dose")&&e.status.equals("taken")&&e.recorded==1690000010L,"Upgrade preserves dose history snapshots and timestamps");
            check(e.iconType.equals("capsule")&&e.iconColor.equals("default"),"Existing history receives safe default icons");
            check(e.takenAt==0&&e.scheduledAt==0&&e.timingSource.equals("legacy"),"Migration keeps actual taken time unknown for old logs");s.onUpgrade(s.getWritableDatabase(),1,3);check(s.events(null).size()==1,"Repeating the additive migration does not remove history");
            m.iconType="package";m.iconColor="purple";s.save(m);check(s.med(42).iconType.equals("package")&&s.med(42).iconColor.equals("purple"),"Upgraded medications can save new icon choices");
            m.iconType="unknown";m.iconColor="unknown";s.save(m);check(s.med(42).iconType.equals("capsule")&&s.med(42).iconColor.equals("default"),"Unknown icon identifiers fall back safely");
        }c.deleteDatabase(name);
        try(android.database.sqlite.SQLiteDatabase db=c.openOrCreateDatabase(name,0,null)){
            db.execSQL("CREATE TABLE meds(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,dose TEXT NOT NULL,notes TEXT NOT NULL,times TEXT NOT NULL,reminders INTEGER NOT NULL,active INTEGER NOT NULL DEFAULT 1,created INTEGER NOT NULL,icon_type TEXT NOT NULL DEFAULT 'capsule',icon_color TEXT NOT NULL DEFAULT 'default')");
            db.execSQL("CREATE TABLE events(id INTEGER PRIMARY KEY AUTOINCREMENT,med INTEGER NOT NULL,day TEXT NOT NULL,time TEXT NOT NULL,name TEXT NOT NULL,dose TEXT NOT NULL,status TEXT NOT NULL,recorded INTEGER NOT NULL,icon_type TEXT NOT NULL DEFAULT 'capsule',icon_color TEXT NOT NULL DEFAULT 'default',UNIQUE(med,day,time))");
            db.execSQL("INSERT INTO meds VALUES(8,'Version two','1','','08:00',1,1,1690000000,'syrup','rose')");db.execSQL("INSERT INTO events VALUES(9,8,'2026-11-05','08:00','Old snapshot','1','taken',1690000010,'tablet','purple')");db.setVersion(2);
        }
        try(Store s=new Store(c,name)){check(s.getReadableDatabase().getVersion()==3,"Version 2 database upgrades to version 3");check(s.med(8).iconType.equals("syrup")&&s.med(8).iconColor.equals("rose"),"Version 2 medication appearance survives the upgrade");Store.Event e=s.events(null).get(0);check(e.id==9&&e.iconType.equals("tablet")&&e.iconColor.equals("purple")&&e.recorded==1690000010L&&e.takenAt==0,"Version 2 history retains its icon snapshot and unknown actual time");check(s.med(8).durationDays==0&&s.trackingStarted()>0,"Existing medications default to indefinite duration with fresh coverage");}c.deleteDatabase(name);
    }
    private void tracking(Context c){
        String name="tracking-test.db";c.deleteDatabase(name);LocalDate today=LocalDate.now();long now=System.currentTimeMillis();
        try(Store s=new Store(c,name)){
            Store.Med m=new Store.Med();m.name="Course";m.dose="1";m.notes="";m.times="00:00,23:59";m.startDay=today.toString();m.durationDays=2;s.save(m);
            check(s.med(m.id).durationDays==2&&s.med(m.id).startDay.equals(today.toString()),"Treatment dates persist in SQLite");
            check(s.slots(today,today.plusDays(4)).size()==4,"Expected plans stop at the inclusive treatment end");
            Store.Slot past=s.slot(m.id,today.toString(),"00:00"),future=s.slot(m.id,today.toString(),"23:59");check(!past.tracked,"Earlier doses on enrollment day do not become fabricated misses");
            check(future.tracked,"Upcoming enrollment-day doses are tracked");
            check(s.recordSlot(past,"taken",now-120000),"Actual taken time can be logged independently of schedule");Store.Event e=s.events(today.toString()).get(0);long recorded=e.recorded;
            check(e.takenAt==now-120000&&e.scheduledAt==past.scheduledAt,"Log stores both actual and scheduled instants");s.updateTakenTime(e,now-60000);e=s.events(today.toString()).get(0);check(e.takenAt==now-60000&&e.recorded==recorded&&e.updatedAt>0,"Correcting actual time preserves original log audit time");
            boolean rejected=false;try{s.updateTakenTime(e,now+86400000);}catch(IllegalArgumentException ex){rejected=true;}check(rejected,"Future actual taken timestamps are rejected");
            m.times="12:00";s.save(m);check(s.slot(m.id,today.toString(),"00:00")!=null&&s.events(null).size()==1,"Schedule edits preserve past slots and existing logs");check(s.slot(m.id,today.plusDays(1).toString(),"23:59")==null,"Schedule edits remove superseded future slots");
            check(s.slot(m.id,today.plusDays(1).toString(),"12:00")!=null,"Schedule edits generate replacement future slots");s.archive(m.id);check(s.slots(today.plusDays(1),today.plusDays(4)).isEmpty(),"Archiving removes future unlogged treatment plans");check(s.events(null).size()==1,"Archiving keeps actual taken history");
            s.getWritableDatabase().execSQL("INSERT INTO events(med,day,time,name,dose,status,recorded) VALUES(99,?,'08:00','Legacy','1','taken',123)",new Object[]{today.toString()});Store.Event legacy=null;for(Store.Event v:s.events(null))if(v.med==99)legacy=v;
            check(legacy!=null&&legacy.takenAt==0,"Legacy log remains explicitly untimed");Adherence.Report r=Adherence.summarize(s.samples(today,today),today,today,now);check(r.taken==2&&r.knownTiming==1,"Dashboard counts old logs without inventing their accuracy");s.updateTakenTime(legacy,Treatment.scheduled(today.toString(),"08:00",ZoneId.systemDefault())>now?now:Treatment.scheduled(today.toString(),"08:00",ZoneId.systemDefault()));r=Adherence.summarize(s.samples(today,today),today,today,now);check(r.knownTiming==2,"A confirmed legacy actual time becomes eligible for accuracy");
            Store.Med absent=new Store.Med();absent.name="Unopened routine";absent.dose="1";absent.notes="";absent.times="01:00";absent.startDay=today.minusDays(5).toString();s.save(absent);long enrolled=today.minusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();s.getWritableDatabase().delete("plans","med=?",new String[]{""+absent.id});s.getWritableDatabase().execSQL("UPDATE meds SET enrolled_at=?,revision_at=?,generated_through=? WHERE id=?",new Object[]{enrolled,enrolled,today.minusDays(3).toString(),absent.id});absent.times="22:00";s.save(absent);check(s.slot(absent.id,today.minusDays(2).toString(),"01:00")!=null&&s.slot(absent.id,today.minusDays(2).toString(),"01:00").tracked,"Editing after an absence first preserves old expected doses");
            boolean futureLog=false;Store.Slot tomorrow=s.slot(absent.id,today.plusDays(1).toString(),"22:00");try{s.recordSlot(tomorrow,"taken",now);}catch(IllegalArgumentException ex){futureLog=true;}check(futureLog,"Future-day logging is rejected by storage");
            Store.Med ended=new Store.Med();ended.name="Ended";ended.dose="1";ended.notes="";ended.times="08:00";ended.startDay=today.minusDays(5).toString();ended.durationDays=2;ended.reminders=true;s.save(ended);Reminders.schedule(c,ended,"08:00");check(Treatment.next("08:00",ended.startDay,ended.durationDays,now,ZoneId.systemDefault())<0,"Completed treatments do not schedule a new alarm");
        }c.deleteDatabase(name);
    }
    private void gallery(Context c)throws Exception{
        LocalDate today=LocalDate.now();long now=System.currentTimeMillis();
        try(Store s=new Store(c)){
            for(int i=0;i<3;i++){Store.Med m=new Store.Med();m.name=new String[]{"Vitamin D3","Daily capsule","Evening syrup"}[i];m.dose=new String[]{"1 tablet","1 capsule","5 mL"}[i];m.notes="";m.times=new String[]{"08:00","08:00,20:00","21:00"}[i];m.startDay=today.minusDays(i==1?3:5).toString();m.durationDays=i==1?10:i==0?30:0;m.iconType=new String[]{"tablet","capsule","syrup"}[i];m.iconColor=new String[]{"amber","purple","rose"}[i];s.save(m);long enrolled=today.minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();s.getWritableDatabase().execSQL("UPDATE meds SET enrolled_at=?,revision_at=?,generated_through='' WHERE id=?",new Object[]{enrolled,enrolled,m.id});}
            s.getWritableDatabase().delete("plans",null,null);s.getWritableDatabase().execSQL("UPDATE meds SET generated_through=''");s.ensurePlans(today.plusDays(40),now);int index=0;for(Store.Slot slot:s.slots(today.minusDays(6),today)){if(slot.day.equals(today.toString()))continue;index++;if(index%6==0)continue;if(index%7==0)s.recordSlot(slot,"skipped",0);else s.recordSlot(slot,"taken",slot.scheduledAt+(index%4==0?52:8)*60000);}
        }
        activity=startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();snapshot(c,"v2-schedule.png",activity.getWindow().getDecorView());
        runOnMainSync(()->((ScheduleFeed)activity.getWindow().getDecorView().findViewWithTag("schedule-feed")).jumpTo(today));waitForIdleSync();snapshot(c,"v2-timeline.png",activity.getWindow().getDecorView());
        click("Medications");snapshot(c,"v2-medications.png",activity.getWindow().getDecorView());click("Dashboard");snapshot(c,"v2-dashboard.png",activity.getWindow().getDecorView());runOnMainSync(()->{View page=((FrameLayout)activity.getWindow().getDecorView().findViewWithTag("page-host")).getChildAt(0);page.scrollTo(0,UiKit.dp(c,260));});waitForIdleSync();snapshot(c,"v2-dashboard-detail.png",activity.getWindow().getDecorView());click("History");snapshot(c,"v2-history.png",activity.getWindow().getDecorView());
        clickTag("settings-entry");click("Language: English");selectLanguage(1,"Hoy");click("Resumen");snapshot(c,"v2-dashboard-es.png",activity.getWindow().getDecorView());click("Calendario");snapshot(c,"v2-schedule-es.png",activity.getWindow().getDecorView());runOnMainSync(()->activity.finish());reset(c);
    }
    private void snapshot(Context c,String name,View root){
        if(root.findViewWithTag("floating-navigation")!=null)waitNavigation();
        // Let treatment and chart reveals finish before recording a visual reference.
        if(FloatingNavBar.motionEnabled())SystemClock.sleep(850);
        final Bitmap[] b=new Bitmap[1];runOnMainSync(()->{b[0]=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);root.draw(new android.graphics.Canvas(b[0]));});
        try(FileOutputStream f=new FileOutputStream(new File(c.getExternalFilesDir(null),name))){b[0].compress(Bitmap.CompressFormat.PNG,100,f);}catch(IOException ex){throw new AssertionError(ex);}
    }
    private String iconDescription(String tag){final String[] value=new String[1];runOnMainSync(()->{View v=activeRoot().findViewWithTag(tag);value[0]=v==null?"":String.valueOf(v.getContentDescription());});return value[0];}
    private void clickTag(String tag){runOnMainSync(()->{View v=activeRoot().findViewWithTag(tag);if(v==null||!v.performClick())throw new AssertionError("Missing icon choice "+tag);});waitForIdleSync();}
    private void reset(Context c) {
        try(Store s=new Store(c)) {
            for(Store.Med med:s.meds())Reminders.cancel(c,med);
            android.database.sqlite.SQLiteDatabase db=s.getWritableDatabase();db.beginTransaction();
            try {db.delete("events",null,null);db.delete("plans",null,null);db.delete("meds",null,null);db.setTransactionSuccessful();}finally{db.endTransaction();}
        }
    }
    private boolean notificationCount(Context c,int expected) {
        long until=SystemClock.uptimeMillis()+20000;
        do {if(c.getSystemService(NotificationManager.class).getActiveNotifications().length==expected)return true;SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<until);return false;
    }
    private boolean waitRinging(boolean expected) {
        long until=SystemClock.uptimeMillis()+45000;do{if(AlarmService.ringing()==expected)return true;SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<until);return false;
    }
    private boolean waitPlaying() {
        long until=SystemClock.uptimeMillis()+20000;do{final boolean[] value=new boolean[1];runOnMainSync(()->value[0]=AlarmService.playing());if(value[0])return true;SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<until);return false;
    }
    private View activeRoot() {
        for(String field:new String[]{"discardDialog","timeEditor","languageDialog","settingsDialog"})try{java.lang.reflect.Field f=MainActivity.class.getDeclaredField(field);f.setAccessible(true);AlertDialog d=(AlertDialog)f.get(activity);if(d!=null&&d.isShowing())return d.getWindow().getDecorView();}catch(NoSuchFieldException e){continue;}catch(ReflectiveOperationException e){throw new AssertionError(e);}
        try{java.lang.reflect.Field f=MainActivity.class.getDeclaredField("iconPicker");f.setAccessible(true);AlertDialog d=(AlertDialog)f.get(activity);if(d!=null&&d.isShowing())return d.getWindow().getDecorView();}catch(ReflectiveOperationException e){throw new AssertionError(e);}
        try {java.lang.reflect.Field f=MainActivity.class.getDeclaredField("editor");f.setAccessible(true);AlertDialog d=(AlertDialog)f.get(activity);if(d!=null&&d.isShowing())return d.getWindow().getDecorView();}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}return activity.getWindow().getDecorView();
    }
    private View match(View v,String value) {
        if(value.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;
        if(v instanceof TextView&&value.contentEquals(((TextView)v).getText()))return v;
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View result=match(g.getChildAt(i),value);if(result!=null)return result;}}return null;
    }
    private View find(String value) {final View[] out=new View[1];runOnMainSync(()->out[0]=match(activeRoot(),value));return out[0];}
    private void click(String value) {runOnMainSync(()->{View root=activeRoot(),bar=root.findViewWithTag("floating-navigation");View n=bar==null?null:match(bar,value);if(n==null)n=match(root,value);if(n==null)throw new AssertionError("Missing control: "+value);while(!n.isClickable()&&n.getParent() instanceof View)n=(View)n.getParent();if(!n.performClick())throw new AssertionError("Could not click: "+value);});waitForIdleSync();}
    private void setText(EditText n,String value) {runOnMainSync(()->n.setText(value));waitForIdleSync();}
    private void uiAudit(Context c)throws Exception{
        activity=startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
        click("Add medication");List<EditText> fields=new ArrayList<>();runOnMainSync(()->collectEdits(activeRoot(),fields));
        snapshot(c,"audit-form-before.png",activeRoot());
        final boolean[] labeled={true};runOnMainSync(()->{for(EditText field:fields)labeled[0]&=field.getId()!=View.NO_ID&&hasLabel(activeRoot(),field.getId());});
        check(labeled[0],"Every visible medication input has an associated persistent label");
        setText(fields.get(0),"Draft medication");click("Cancel");
        check(find("Discard changes?")!=null,"Canceling a changed medication asks before discarding it");click("Keep editing");
        check(find("Draft medication")!=null,"Keeping a draft preserves entered text");
        java.lang.reflect.Field edit=MainActivity.class.getDeclaredField("editor");edit.setAccessible(true);runOnMainSync(()->{try{((AlertDialog)edit.get(activity)).cancel();}catch(IllegalAccessException e){throw new AssertionError(e);}});waitForIdleSync();
        check(find("Discard changes?")!=null,"Android dialog cancellation also protects a changed draft");click("Discard changes");
        try(Store s=new Store(c)){check(s.meds().isEmpty(),"Discarding a draft never saves a medication");}
        click("Add medication");click("Cancel");check(find("A little breathing room")!=null,"Unchanged forms close without a discard prompt");
        click("Dashboard");waitNavigation();check(find("YOUR PATTERNS")!=null,"Dashboard uses the shared page heading hierarchy");
        Button seven=(Button)find("7 days"),thirty=(Button)find("30 days");check(seven.isSelected()&&!thirty.isSelected(),"Dashboard range exposes its selected state");click("30 days");
        check(find("30 days").isSelected()&&!find("7 days").isSelected(),"Changing the dashboard range updates selection semantics");
        click("Schedule");waitNavigation();View calendar=find("Go to a date");
        check(calendar.createAccessibilityNodeInfo().getClassName().toString().equals(Button.class.getName()),"Icon actions expose a native button role");
        for(int tab=0;tab<4;tab++){
            clickTag("nav-tab-"+tab);waitNavigation();ViewGroup item=activity.getWindow().getDecorView().findViewWithTag("nav-tab-"+tab);final boolean[] readable={false};
            runOnMainSync(()->{for(int i=0;i<item.getChildCount();i++)if(item.getChildAt(i) instanceof TextView){TextView label=(TextView)item.getChildAt(i);readable[0]=label.getLayout()!=null&&label.getLayout().getEllipsisCount(0)==0;if(!readable[0])report.append("Navigation label: ").append(label.getText()).append(" width=").append(label.getWidth()).append(" textSize=").append(label.getTextSize()).append('\n');}});
            check(readable[0],"Selected navigation label remains complete for tab "+tab);
        }
        try(Store s=new Store(c)){
            android.database.sqlite.SQLiteDatabase db=s.getWritableDatabase();db.beginTransaction();try{
                for(int i=0;i<200;i++)db.execSQL("INSERT INTO events(med,day,time,name,dose,status,recorded) VALUES(99,?,'08:00',?,'1','taken',123)",new Object[]{LocalDate.now().minusDays(i).toString(),"History item "+i});
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
        }
        clickTag("nav-tab-0");clickTag("nav-tab-3");waitNavigation();ListView history=activity.getWindow().getDecorView().findViewWithTag("history-feed");
        check(history!=null&&history.getCount()==201&&history.getChildCount()<20,"Long histories mount only visible native list rows");
        runOnMainSync(()->history.setSelectionFromTop(200,0));long historyUntil=SystemClock.uptimeMillis()+5000;while(find("History item 199")==null&&SystemClock.uptimeMillis()<historyUntil){SystemClock.sleep(50);waitForIdleSync();}check(find("History item 199")!=null,"The final entry in a long history remains reachable");
        Activity alarm=startActivitySync(new Intent(c,AlarmActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
        ViewGroup alarmContent=alarm.findViewById(android.R.id.content);check(alarmContent.getChildAt(0) instanceof ScrollView,"Alarm controls use a scrollable viewport on compact screens");
        View stop=match(alarm.getWindow().getDecorView(),"Stop alarm");runOnMainSync(()->stop.requestRectangleOnScreen(new android.graphics.Rect(0,0,stop.getWidth(),stop.getHeight()),true));waitForIdleSync();
        android.graphics.Rect visible=new android.graphics.Rect();check(stop.getGlobalVisibleRect(visible)&&visible.height()==stop.getHeight(),"The entire alarm stop control can be brought into view");runOnMainSync(()->alarm.finish());
        runOnMainSync(()->activity.finish());reset(c);
    }
    private boolean hasLabel(View v,int id){if(v.getLabelFor()==id)return true;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(hasLabel(g.getChildAt(i),id))return true;}return false;}
    private void collectEdits(View v,List<EditText> out) {if(v instanceof EditText)out.add((EditText)v);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)collectEdits(g.getChildAt(i),out);}}
}
