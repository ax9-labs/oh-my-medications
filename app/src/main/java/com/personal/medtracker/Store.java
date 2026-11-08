package com.personal.medtracker;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.time.*;
import java.util.*;

final class Store extends SQLiteOpenHelper {
    static final class Med {
        long id, created; String name, dose, notes, times; boolean reminders;
        String iconType="capsule",iconColor="default";
        String startDay="",generatedThrough="",planZone="";int durationDays;long enrolledAt,revisionAt;
    }
    static final class Event {
        long id, med, recorded; String day, time, name, dose, status;
        String iconType="capsule",iconColor="default";
        long scheduledAt,takenAt,updatedAt;String zone="",timingSource="legacy";
    }
    static final class Slot {
        long med,scheduledAt;String day,time,name,dose,iconType,iconColor,zone;boolean tracked;Event event;
        String key(){return Schedule.key(med,day,time);}
    }
    Store(Context c) { this(c,"med-tracker.db"); }
    Store(Context c,String databaseName) { super(c,databaseName,null,3); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS meds(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,dose TEXT NOT NULL,notes TEXT NOT NULL,times TEXT NOT NULL,reminders INTEGER NOT NULL,active INTEGER NOT NULL DEFAULT 1,created INTEGER NOT NULL,icon_type TEXT NOT NULL DEFAULT 'capsule',icon_color TEXT NOT NULL DEFAULT 'default')");
        db.execSQL("CREATE TABLE IF NOT EXISTS events(id INTEGER PRIMARY KEY AUTOINCREMENT,med INTEGER NOT NULL,day TEXT NOT NULL,time TEXT NOT NULL,name TEXT NOT NULL,dose TEXT NOT NULL,status TEXT NOT NULL,recorded INTEGER NOT NULL,icon_type TEXT NOT NULL DEFAULT 'capsule',icon_color TEXT NOT NULL DEFAULT 'default',UNIQUE(med,day,time))");
        upgradeTracking(db);
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion<2){
            for(String table:new String[]{"meds","events"}){
                addColumn(db,table,"icon_type","capsule");addColumn(db,table,"icon_color","default");
            }
        }
        if(oldVersion<3)upgradeTracking(db);
    }
    private void upgradeTracking(SQLiteDatabase db){
        addColumnSql(db,"meds","start_day","TEXT NOT NULL DEFAULT ''");addColumnSql(db,"meds","duration_days","INTEGER NOT NULL DEFAULT 0");
        addColumnSql(db,"meds","enrolled_at","INTEGER NOT NULL DEFAULT 0");addColumnSql(db,"meds","revision_at","INTEGER NOT NULL DEFAULT 0");
        addColumnSql(db,"meds","generated_through","TEXT NOT NULL DEFAULT ''");addColumnSql(db,"meds","plan_zone","TEXT NOT NULL DEFAULT ''");
        addColumnSql(db,"events","scheduled_at","INTEGER NOT NULL DEFAULT 0");addColumnSql(db,"events","taken_at","INTEGER NOT NULL DEFAULT 0");
        addColumnSql(db,"events","timezone","TEXT NOT NULL DEFAULT ''");addColumnSql(db,"events","timing_source","TEXT NOT NULL DEFAULT 'legacy'");addColumnSql(db,"events","updated_at","INTEGER NOT NULL DEFAULT 0");
        db.execSQL("CREATE TABLE IF NOT EXISTS app_meta(key TEXT PRIMARY KEY,value TEXT NOT NULL)");
        long now=System.currentTimeMillis();db.execSQL("INSERT OR IGNORE INTO app_meta(key,value) VALUES('tracking_started',?)",new Object[]{""+now});
        db.execSQL("CREATE TABLE IF NOT EXISTS plans(med INTEGER NOT NULL,day TEXT NOT NULL,time TEXT NOT NULL,name TEXT NOT NULL,dose TEXT NOT NULL,icon_type TEXT NOT NULL,icon_color TEXT NOT NULL,scheduled_at INTEGER NOT NULL,timezone TEXT NOT NULL,tracked INTEGER NOT NULL,PRIMARY KEY(med,day,time))");
        db.execSQL("CREATE INDEX IF NOT EXISTS plans_day ON plans(day,time)");
        try(Cursor c=db.rawQuery("SELECT id,created FROM meds WHERE enrolled_at=0",null)){
            while(c.moveToNext()){
                ContentValues v=new ContentValues();v.put("start_day",Instant.ofEpochMilli(c.getLong(1)).atZone(ZoneId.systemDefault()).toLocalDate().toString());v.put("enrolled_at",now);v.put("revision_at",now);v.put("plan_zone",ZoneId.systemDefault().getId());db.update("meds",v,"id=?",new String[]{""+c.getLong(0)});
            }
        }
    }
    private void addColumnSql(SQLiteDatabase db,String table,String column,String declaration){
        try(Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null)){while(c.moveToNext())if(column.equals(c.getString(c.getColumnIndexOrThrow("name"))))return;}
        db.execSQL("ALTER TABLE "+table+" ADD COLUMN "+column+" "+declaration);
    }
    private void addColumn(SQLiteDatabase db,String table,String column,String defaultValue){
        // Multiple helpers may open after an update; make the additive migration idempotent.
        try(Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null)){
            while(c.moveToNext())if(column.equals(c.getString(c.getColumnIndexOrThrow("name"))))return;
        }
        db.execSQL("ALTER TABLE "+table+" ADD COLUMN "+column+" TEXT NOT NULL DEFAULT '"+defaultValue+"'");
    }
    List<Med> meds() {
        List<Med> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("meds", null, "active=1", null, null, null, "name COLLATE NOCASE")) {
            while (c.moveToNext()) out.add(readMed(c));
        }
        return out;
    }
    Med med(long id) {
        try (Cursor c = getReadableDatabase().query("meds", null, "id=? AND active=1", new String[]{""+id}, null, null, null)) {
            return c.moveToFirst() ? readMed(c) : null;
        }
    }
    private Med readMed(Cursor c) {
        Med m = new Med(); m.id = c.getLong(c.getColumnIndexOrThrow("id"));
        m.name = c.getString(c.getColumnIndexOrThrow("name")); m.dose = c.getString(c.getColumnIndexOrThrow("dose"));
        m.notes = c.getString(c.getColumnIndexOrThrow("notes")); m.times = c.getString(c.getColumnIndexOrThrow("times"));
        m.reminders = c.getInt(c.getColumnIndexOrThrow("reminders")) == 1; m.created = c.getLong(c.getColumnIndexOrThrow("created"));
        m.iconType=MedAppearance.type(c.getString(c.getColumnIndexOrThrow("icon_type")));m.iconColor=MedAppearance.color(c.getString(c.getColumnIndexOrThrow("icon_color")));
        m.startDay=c.getString(c.getColumnIndexOrThrow("start_day"));m.durationDays=c.getInt(c.getColumnIndexOrThrow("duration_days"));m.enrolledAt=c.getLong(c.getColumnIndexOrThrow("enrolled_at"));m.revisionAt=c.getLong(c.getColumnIndexOrThrow("revision_at"));m.generatedThrough=c.getString(c.getColumnIndexOrThrow("generated_through"));m.planZone=c.getString(c.getColumnIndexOrThrow("plan_zone"));return m;
    }
    void save(Med m) {
        if(m.startDay.isEmpty())m.startDay=LocalDate.now().toString();LocalDate.parse(m.startDay);if(m.durationDays<0||m.durationDays>3650)throw new IllegalArgumentException("Invalid duration");
        long now=System.currentTimeMillis();SQLiteDatabase database=getWritableDatabase();database.beginTransaction();try{Med old=m.id==0?null:med(m.id);if(old!=null)ensurePlans(LocalDate.now(),now);
        boolean revision=old==null||!old.times.equals(m.times)||!old.startDay.equals(m.startDay)||old.durationDays!=m.durationDays;
        ContentValues v = new ContentValues(); v.put("name", m.name); v.put("dose", m.dose); v.put("notes", m.notes);
        v.put("times", m.times); v.put("reminders", m.reminders ? 1 : 0);
        v.put("icon_type",MedAppearance.type(m.iconType));v.put("icon_color",MedAppearance.color(m.iconColor));
        v.put("start_day",m.startDay);v.put("duration_days",m.durationDays);
        if(revision){v.put("revision_at",now);v.put("generated_through",LocalDate.now().minusDays(1).toString());v.put("plan_zone",ZoneId.systemDefault().getId());}
        if (m.id == 0) { m.created = now;v.put("enrolled_at",now);v.put("created", m.created); m.id = getWritableDatabase().insertOrThrow("meds", null, v); }
        else getWritableDatabase().update("meds", v, "id=?", new String[]{""+m.id});
        if(revision)pruneFuture(m.id,now);
        else {
            ContentValues snapshot=new ContentValues();snapshot.put("name",m.name);snapshot.put("dose",m.dose);snapshot.put("icon_type",MedAppearance.type(m.iconType));snapshot.put("icon_color",MedAppearance.color(m.iconColor));
            getWritableDatabase().update("plans",snapshot,"med=? AND scheduled_at>? AND NOT EXISTS(SELECT 1 FROM events e WHERE e.med=plans.med AND e.day=plans.day AND e.time=plans.time)",new String[]{""+m.id,""+now});
        }
        ensurePlans(LocalDate.now().plusDays(30),now);database.setTransactionSuccessful();}finally{database.endTransaction();}
    }
    void archive(long id) {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{long now=System.currentTimeMillis();ensurePlans(LocalDate.now(),now);ContentValues v=new ContentValues();v.put("active",0);db.update("meds",v,"id=?",new String[]{""+id});pruneFuture(id,now);db.setTransactionSuccessful();}finally{db.endTransaction();}
    }
    List<Event> events(String day) {return eventQuery(day==null?null:"day=?",day==null?null:new String[]{day});}
    private List<Event> eventQuery(String selection,String[] args){
        List<Event> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("events", null, selection,args, null, null, "day DESC,time DESC,id DESC")) {
            while (c.moveToNext()) {
                Event e = new Event(); e.id=c.getLong(c.getColumnIndexOrThrow("id")); e.med=c.getLong(c.getColumnIndexOrThrow("med"));
                e.day=c.getString(c.getColumnIndexOrThrow("day")); e.time=c.getString(c.getColumnIndexOrThrow("time"));
                e.name=c.getString(c.getColumnIndexOrThrow("name")); e.dose=c.getString(c.getColumnIndexOrThrow("dose"));
                e.status=c.getString(c.getColumnIndexOrThrow("status")); e.recorded=c.getLong(c.getColumnIndexOrThrow("recorded"));
                e.iconType=MedAppearance.type(c.getString(c.getColumnIndexOrThrow("icon_type")));e.iconColor=MedAppearance.color(c.getString(c.getColumnIndexOrThrow("icon_color")));
                e.scheduledAt=c.getLong(c.getColumnIndexOrThrow("scheduled_at"));e.takenAt=c.getLong(c.getColumnIndexOrThrow("taken_at"));e.updatedAt=c.getLong(c.getColumnIndexOrThrow("updated_at"));e.zone=c.getString(c.getColumnIndexOrThrow("timezone"));e.timingSource=c.getString(c.getColumnIndexOrThrow("timing_source"));out.add(e);
            }
        }
        return out;
    }
    boolean handled(long id, String day, String time) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM events WHERE med=? AND day=? AND time=?",new String[]{""+id,day,time})) { return c.moveToFirst(); }
    }
    boolean record(Med m, String day, String time, String status) {
        ensurePlans(LocalDate.parse(day),System.currentTimeMillis());Slot slot=slot(m.id,day,time);
        if(slot==null){slot=makeSlot(m,day,time,ZoneId.systemDefault(),false);insertPlan(getWritableDatabase(),slot);}
        return recordSlot(slot,status,"taken".equals(status)?System.currentTimeMillis():0);
    }
    boolean recordSlot(Slot slot,String status,long takenAt){
        if (!status.equals("taken") && !status.equals("skipped")) throw new IllegalArgumentException("Invalid status");
        long now=System.currentTimeMillis();if(LocalDate.parse(slot.day).isAfter(LocalDate.now())||takenAt>now+1000||("taken".equals(status)&&takenAt<=0))throw new IllegalArgumentException("Future log");
        ContentValues v = new ContentValues(); v.put("med",slot.med); v.put("day",slot.day); v.put("time",slot.time); v.put("name",slot.name); v.put("dose",slot.dose); v.put("status",status); v.put("recorded",now);
        v.put("icon_type",slot.iconType);v.put("icon_color",slot.iconColor);v.put("scheduled_at",slot.scheduledAt);v.put("timezone",slot.zone);v.put("taken_at","taken".equals(status)?takenAt:0);v.put("timing_source","confirmed");
        return getWritableDatabase().insertWithOnConflict("events",null,v,SQLiteDatabase.CONFLICT_IGNORE) != -1;
    }
    void updateTakenTime(Event e,long time){
        if(!"taken".equals(e.status)||time<=0||time>System.currentTimeMillis()+1000)throw new IllegalArgumentException("Invalid taken time");
        ContentValues v=new ContentValues();v.put("taken_at",time);v.put("updated_at",System.currentTimeMillis());v.put("timing_source","confirmed");
        if(e.scheduledAt==0){v.put("scheduled_at",Treatment.scheduled(e.day,e.time,ZoneId.systemDefault()));v.put("timezone",ZoneId.systemDefault().getId());}
        getWritableDatabase().update("events",v,"id=?",new String[]{""+e.id});
    }
    long trackingStarted(){try(Cursor c=getReadableDatabase().rawQuery("SELECT value FROM app_meta WHERE key='tracking_started'",null)){return c.moveToFirst()?Long.parseLong(c.getString(0)):System.currentTimeMillis();}}
    private void pruneFuture(long med,long now){getWritableDatabase().delete("plans","med=? AND scheduled_at>? AND NOT EXISTS(SELECT 1 FROM events e WHERE e.med=plans.med AND e.day=plans.day AND e.time=plans.time)",new String[]{""+med,""+now});}
    void ensurePlans(LocalDate through,long now){
        SQLiteDatabase db=getWritableDatabase();ZoneId zone=ZoneId.systemDefault();db.beginTransaction();
        try{
            for(Med m:meds()){
                if(!zone.getId().equals(m.planZone)){
                    try(Cursor c=db.rawQuery("SELECT day,time FROM plans WHERE med=? AND scheduled_at>? AND NOT EXISTS(SELECT 1 FROM events e WHERE e.med=plans.med AND e.day=plans.day AND e.time=plans.time)",new String[]{""+m.id,""+now})){
                        while(c.moveToNext()){ContentValues v=new ContentValues();v.put("scheduled_at",Treatment.scheduled(c.getString(0),c.getString(1),zone));v.put("timezone",zone.getId());db.update("plans",v,"med=? AND day=? AND time=?",new String[]{""+m.id,c.getString(0),c.getString(1)});}
                    }
                }
                LocalDate first=m.generatedThrough.isEmpty()?Instant.ofEpochMilli(m.revisionAt).atZone(zone).toLocalDate():LocalDate.parse(m.generatedThrough).plusDays(1);
                LocalDate starts=LocalDate.parse(m.startDay);if(first.isBefore(starts))first=starts;LocalDate last=through,end=Treatment.end(m.startDay,m.durationDays);if(end!=null&&last.isAfter(end))last=end;
                for(LocalDate day=first;!day.isAfter(last);day=day.plusDays(1))for(String time:Schedule.parseTimes(m.times)){
                    long when=Treatment.scheduled(day.toString(),time,zone);if(when<m.revisionAt&&m.revisionAt!=m.enrolledAt)continue;
                    insertPlan(db,makeSlot(m,day.toString(),time,zone,when>=m.enrolledAt&&when>=m.revisionAt));
                }
                ContentValues v=new ContentValues();if(m.generatedThrough.isEmpty()||LocalDate.parse(m.generatedThrough).isBefore(through))v.put("generated_through",through.toString());v.put("plan_zone",zone.getId());db.update("meds",v,"id=?",new String[]{""+m.id});
            }db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    private Slot makeSlot(Med m,String day,String time,ZoneId zone,boolean tracked){Slot s=new Slot();s.med=m.id;s.day=day;s.time=time;s.name=m.name;s.dose=m.dose;s.iconType=MedAppearance.type(m.iconType);s.iconColor=MedAppearance.color(m.iconColor);s.scheduledAt=Treatment.scheduled(day,time,zone);s.zone=zone.getId();s.tracked=tracked;return s;}
    private void insertPlan(SQLiteDatabase db,Slot s){ContentValues v=new ContentValues();v.put("med",s.med);v.put("day",s.day);v.put("time",s.time);v.put("name",s.name);v.put("dose",s.dose);v.put("icon_type",s.iconType);v.put("icon_color",s.iconColor);v.put("scheduled_at",s.scheduledAt);v.put("timezone",s.zone);v.put("tracked",s.tracked?1:0);db.insertWithOnConflict("plans",null,v,SQLiteDatabase.CONFLICT_IGNORE);}
    List<Slot> slots(LocalDate from,LocalDate to){
        Map<String,Event> logged=new HashMap<>();for(Event e:eventQuery("day>=? AND day<=?",new String[]{from.toString(),to.toString()}))logged.put(Schedule.key(e.med,e.day,e.time),e);
        List<Slot> out=new ArrayList<>();try(Cursor c=getReadableDatabase().query("plans",null,"day>=? AND day<=?",new String[]{from.toString(),to.toString()},null,null,"day,time,name COLLATE NOCASE")){
            while(c.moveToNext()){Slot s=new Slot();s.med=c.getLong(c.getColumnIndexOrThrow("med"));s.day=c.getString(c.getColumnIndexOrThrow("day"));s.time=c.getString(c.getColumnIndexOrThrow("time"));s.name=c.getString(c.getColumnIndexOrThrow("name"));s.dose=c.getString(c.getColumnIndexOrThrow("dose"));s.iconType=c.getString(c.getColumnIndexOrThrow("icon_type"));s.iconColor=c.getString(c.getColumnIndexOrThrow("icon_color"));s.zone=c.getString(c.getColumnIndexOrThrow("timezone"));s.scheduledAt=c.getLong(c.getColumnIndexOrThrow("scheduled_at"));s.tracked=c.getInt(c.getColumnIndexOrThrow("tracked"))==1;s.event=logged.get(s.key());out.add(s);}
        }return out;
    }
    Slot slot(long med,String day,String time){for(Slot s:slots(LocalDate.parse(day),LocalDate.parse(day)))if(s.med==med&&s.time.equals(time))return s;return null;}
    List<Adherence.Sample> samples(LocalDate from,LocalDate to){
        List<Adherence.Sample> out=new ArrayList<>();Set<String> included=new HashSet<>();for(Slot s:slots(from,to)){out.add(new Adherence.Sample(s.day,s.event!=null&&s.event.scheduledAt>0?s.event.scheduledAt:s.scheduledAt,s.event==null?0:s.event.takenAt,s.event==null?null:s.event.status,s.tracked));included.add(s.key());}
        for(Event e:eventQuery("day>=? AND day<=?",new String[]{from.toString(),to.toString()}))if(!included.contains(Schedule.key(e.med,e.day,e.time)))out.add(new Adherence.Sample(e.day,e.scheduledAt,e.takenAt,e.status,false));return out;
    }
    void undo(long id) { getWritableDatabase().delete("events","id=?",new String[]{""+id}); }
}
