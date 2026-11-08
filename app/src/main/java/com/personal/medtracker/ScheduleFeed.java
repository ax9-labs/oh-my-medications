package com.personal.medtracker;

import android.content.Context;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Virtualized calendar feed; only visible day/dose rows are mounted. */
final class ScheduleFeed extends ListView {
    interface Actions {View brand();void add();void pickDate();void taken(Store.Slot s);void time(Store.Slot s);void skip(Store.Slot s);void undo(Store.Event e);}
    private final Store store;private final Actions actions;private final Palette colors;private final List<Row> rows=new ArrayList<>();private final FeedAdapter adapter=new FeedAdapter();
    private LocalDate first=LocalDate.now(),through=LocalDate.now().plusDays(13);private Store.Slot next;private int overdue,taken,total;private boolean extending;
    private static final class Row {int kind;LocalDate day;Store.Slot slot;Row(int kind,LocalDate day,Store.Slot slot){this.kind=kind;this.day=day;this.slot=slot;}}
    ScheduleFeed(Context c,Store store,LocalDate firstDate,LocalDate lastDate,Actions actions){super(c);this.first=firstDate;this.through=lastDate;this.store=store;this.actions=actions;colors=new Palette(c);setTag("schedule-feed");setDivider(null);setCacheColorHint(0);setSelector(android.R.color.transparent);setPadding(0,0,0,FloatingNavBar.preferredHeight(c)+dp(36));setClipToPadding(false);buildRows();setAdapter(adapter);
        setOnScrollListener(new OnScrollListener(){public void onScrollStateChanged(AbsListView v,int s){}public void onScroll(AbsListView v,int start,int visible,int count){if(!extending&&count>0&&start+visible>=count-4&&through.isBefore(LocalDate.now().plusYears(10))){extending=true;post(()->{through=through.plusDays(14);buildRows();adapter.notifyDataSetChanged();extending=false;});}}});
    }
    LocalDate firstDay(){return first;}
    LocalDate lastDay(){return through;}
    void jumpTo(LocalDate day){if(day.isBefore(first))first=day;if(day.isAfter(through))through=day.plusDays(7);buildRows();adapter.notifyDataSetChanged();for(int i=0;i<rows.size();i++)if(rows.get(i).kind==1&&rows.get(i).day.equals(day)){final int at=i;post(()->setSelectionFromTop(at,dp(8)));return;}}
    private void jumpNext(){if(next==null){jumpTo(LocalDate.now());return;}for(int i=0;i<rows.size();i++)if(rows.get(i).slot!=null&&rows.get(i).slot.key().equals(next.key())){smoothScrollToPosition(i);return;}}
    private void buildRows(){
        long now=System.currentTimeMillis();store.ensurePlans(through,now);List<Store.Slot> all=store.slots(first,through);rows.clear();next=null;overdue=taken=total=0;
        Map<String,List<Store.Slot>> days=new HashMap<>();for(Store.Slot s:all){days.computeIfAbsent(s.day,k->new ArrayList<>()).add(s);if(s.day.equals(LocalDate.now().toString())){total++;if(s.event!=null&&s.event.status.equals("taken"))taken++;if(s.event==null&&s.tracked&&s.scheduledAt+Adherence.WINDOW_MS<now)overdue++;}
            if(s.event==null&&s.scheduledAt>=now-Adherence.WINDOW_MS&&(next==null||s.scheduledAt<next.scheduledAt))next=s;
        }
        rows.add(new Row(0,null,null));
        for(LocalDate day=first;!day.isAfter(through);day=day.plusDays(1)){
            rows.add(new Row(1,day,null));List<Store.Slot> list=days.get(day.toString());boolean marker=false;
            if(list!=null)for(Store.Slot s:list){if(day.equals(LocalDate.now())&&!marker&&s.scheduledAt>now){rows.add(new Row(2,day,null));marker=true;}rows.add(new Row(3,day,s));}
            if(day.equals(LocalDate.now())&&!marker)rows.add(new Row(2,day,null));if(list==null||list.isEmpty())rows.add(new Row(4,day,null));
        }
    }
    private int dp(float n){return UiKit.dp(getContext(),n);}
    private LinearLayout col(){LinearLayout l=new LinearLayout(getContext());l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(getContext());l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView t(String text,int size,int color,boolean bold){TextView v=new TextView(getContext());v.setText(text);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(Typeface.create("sans-serif-medium",0));v.setLineSpacing(dp(2),1);return v;}
    private String s(int id,Object...args){return getContext().getString(id,args);}
    private void gap(LinearLayout l,int n){l.addView(new View(getContext()),new LinearLayout.LayoutParams(1,dp(n)));}
    private LinearLayout outer(){LinearLayout l=col();l.setPadding(dp(22),dp(6),dp(22),dp(8));l.setLayoutParams(new AbsListView.LayoutParams(-1,-2));return l;}
    private View header(){
        LinearLayout l=outer();l.setPadding(dp(22),dp(24),dp(22),dp(14));l.addView(actions.brand());gap(l,22);
        TextView date=t(AppLocale.date(getContext(),LocalDate.now(),false),12,colors.muted,false);l.addView(date);gap(l,5);
        LinearLayout title=row();title.addView(UiKit.heading(t(s(R.string.schedule_title),32,colors.ink,true)),new LinearLayout.LayoutParams(0,-2,1));UiKit.addAction(title,getContext(),IconGlyph.Kind.CALENDAR,s(R.string.choose_calendar_date),false,actions::pickDate);UiKit.addAction(title,getContext(),IconGlyph.Kind.ADD,s(R.string.add_medication),true,actions::add);l.addView(title);gap(l,15);
        LinearLayout hero=col();hero.setPadding(dp(18),dp(17),dp(18),dp(17));hero.setBackground(UiKit.glowCard(getContext()));
        if(next!=null){
            hero.setTag("next-dose-card");hero.addView(t(s(next.scheduledAt<=System.currentTimeMillis()?R.string.due_now:R.string.up_next),12,colors.accentInk,true));gap(hero,10);
            LinearLayout nextRow=row();MedIconView icon=new MedIconView(getContext(),next.iconType,next.iconColor);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(48),dp(48));ip.rightMargin=dp(12);nextRow.addView(icon,ip);
            LinearLayout words=col();words.addView(t(next.name,19,colors.ink,true));words.addView(t(AppLocale.time(getContext(),next.time),25,colors.ink,true));if(!next.day.equals(LocalDate.now().toString()))words.addView(t(AppLocale.date(getContext(),LocalDate.parse(next.day),false),12,colors.muted,false));nextRow.addView(words,new LinearLayout.LayoutParams(0,-2,1));UiKit.addAction(nextRow,getContext(),IconGlyph.Kind.ARROW,s(R.string.jump_next),true,this::jumpNext);hero.addView(nextRow);
        }else{hero.addView(t(s(R.string.schedule_clear),20,colors.ink,true));gap(hero,6);hero.addView(t(s(R.string.schedule_clear_hint),14,colors.muted,false));}
        gap(hero,12);hero.addView(t(getContext().getResources().getQuantityString(R.plurals.taken_progress,taken,taken,total),13,colors.muted,false));
        if(overdue>0){gap(hero,5);TextView past=t(s(R.string.earlier_unlogged,overdue),13,0xFF9B6747,true);past.setMinimumHeight(dp(48));past.setGravity(Gravity.CENTER_VERTICAL);past.setBackground(UiKit.interactive(getContext(),UiKit.rounded(getContext(),0,8),8));UiKit.action(past);past.setOnClickListener(v->jumpTo(LocalDate.now()));hero.addView(past);}l.addView(hero);gap(l,16);
        HorizontalScrollView rail=new HorizontalScrollView(getContext());rail.setHorizontalScrollBarEnabled(false);LinearLayout dates=row();
        for(int i=0;i<7;i++){LocalDate d=LocalDate.now().plusDays(i);LinearLayout chip=col();chip.setGravity(Gravity.CENTER);chip.setMinimumWidth(dp(52));chip.setPadding(dp(9),dp(9),dp(9),dp(9));chip.setBackground(UiKit.interactive(getContext(),UiKit.rounded(getContext(),i==0?0xFF2C293A:0xBBFFFFFF,17),17));chip.addView(t(d.format(DateTimeFormatter.ofPattern("EE",AppLocale.locale(getContext()))),11,i==0?0xFFFFFFFF:colors.muted,false));chip.addView(t(""+d.getDayOfMonth(),19,i==0?0xFFFFFFFF:colors.ink,true));chip.setContentDescription(AppLocale.date(getContext(),d,true));UiKit.action(chip);chip.setMinimumHeight(dp(48));chip.setOnClickListener(v->jumpTo(d));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.rightMargin=dp(7);dates.addView(chip,p);}rail.addView(dates);l.addView(rail);return l;
    }
    private View dayHeader(LocalDate day){LinearLayout l=outer();gap(l,10);LinearLayout r=row();String label=day.equals(LocalDate.now())?s(R.string.today):day.equals(LocalDate.now().plusDays(1))?s(R.string.tomorrow):AppLocale.date(getContext(),day,false);r.addView(UiKit.heading(t(label,20,colors.ink,true)),new LinearLayout.LayoutParams(0,-2,1));r.addView(t(day.format(DateTimeFormatter.ofPattern("d MMM",AppLocale.locale(getContext()))),12,colors.muted,false));l.addView(r);return l;}
    private View now(){LinearLayout l=outer();l.setTag("now-marker");LinearLayout r=row();TextView label=t(s(R.string.now_at,AppLocale.time(getContext(),LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))),14,colors.accentInk,true);r.addView(label);View line=new View(getContext());line.setBackgroundColor(0xFF89B9E3);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(2),1);p.leftMargin=dp(12);r.addView(line,p);l.addView(r);return l;}
    private View dose(Store.Slot slot){
        LinearLayout l=outer(),r=row();r.setGravity(Gravity.TOP);boolean compact=UiKit.compact(getContext());if(compact)r.setOrientation(LinearLayout.VERTICAL);LinearLayout time=compact?row():col();time.setPadding(0,dp(14),dp(8),0);TextView clock=t(AppLocale.time(getContext(),slot.time),20,colors.ink,true);clock.setFontFeatureSettings("tnum");time.addView(clock);
        TextView dot=t("●",10,slot.event!=null?getContext().getColor(R.color.chart_on_time):colors.accentInk,false);dot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);if(!compact)time.addView(dot);r.addView(time,new LinearLayout.LayoutParams(compact?-1:dp(80),-2));if(compact)gap(r,6);
        LinearLayout card=col();card.setTag("dose-"+slot.key());card.setPadding(dp(14),dp(14),dp(14),dp(13));card.setBackground(next!=null&&slot.key().equals(next.key())?UiKit.glowCard(getContext()):UiKit.surface(getContext()));
        LinearLayout name=row();MedIconView icon=new MedIconView(getContext(),slot.iconType,slot.iconColor);icon.setTag("medication-icon");LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(37),dp(37));ip.rightMargin=dp(9);name.addView(icon,ip);LinearLayout words=col();words.addView(t(slot.name,17,colors.ink,true));words.addView(t(slot.dose,12,colors.muted,false));name.addView(words,new LinearLayout.LayoutParams(0,-2,1));card.addView(name);gap(card,10);
        boolean future=LocalDate.parse(slot.day).isAfter(LocalDate.now());
        if(slot.event!=null){Store.Event e=slot.event;card.addView(t(s(e.status.equals("taken")?R.string.taken_badge:R.string.skipped_badge),13,colors.ink,true));if(e.takenAt>0){gap(card,4);card.addView(t(s(R.string.actual_at,AppLocale.timestamp(getContext(),e.takenAt)),11,colors.muted,false));card.addView(t(TimingText.difference(getContext(),e),11,colors.muted,false));}
            LinearLayout buttons=row();buttons.setGravity(Gravity.RIGHT);if(e.status.equals("taken"))UiKit.addAction(buttons,getContext(),IconGlyph.Kind.CLOCK,s(R.string.edit_taken_time),false,()->actions.time(slot));UiKit.addAction(buttons,getContext(),IconGlyph.Kind.UNDO,s(R.string.undo),false,()->actions.undo(e));card.addView(buttons);
        }else if(future){card.addView(t(s(R.string.future_dose),12,colors.muted,false));}
        else{
            String status=!slot.tracked&&slot.scheduledAt<System.currentTimeMillis()?s(R.string.untracked_slot):slot.scheduledAt+Adherence.WINDOW_MS<System.currentTimeMillis()?s(R.string.not_logged):s(R.string.scheduled_unlogged);card.addView(t(status,12,colors.muted,false));gap(card,8);
            LinearLayout buttons=row();buttons.setGravity(Gravity.RIGHT);UiKit.addAction(buttons,getContext(),IconGlyph.Kind.SKIP,s(R.string.skip),false,()->actions.skip(slot));UiKit.addAction(buttons,getContext(),IconGlyph.Kind.CLOCK,s(R.string.log_different_time),false,()->actions.time(slot));UiKit.addAction(buttons,getContext(),IconGlyph.Kind.CHECK,s(R.string.log_taken_now),true,()->actions.taken(slot));card.addView(buttons);
        }
        r.addView(card,compact?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1));l.addView(r);return l;
    }
    private final class FeedAdapter extends BaseAdapter {
        public int getCount(){return rows.size();}public Object getItem(int p){return rows.get(p);}public long getItemId(int p){return p;}public int getViewTypeCount(){return 5;}public int getItemViewType(int p){return rows.get(p).kind;}
        public View getView(int position,View recycled,ViewGroup parent){Row row=rows.get(position);if(row.kind==0)return header();if(row.kind==1)return dayHeader(row.day);if(row.kind==2)return now();if(row.kind==3)return dose(row.slot);LinearLayout l=outer();TextView empty=t(s(R.string.no_doses_day),14,colors.muted,false);empty.setPadding(dp(14),dp(18),dp(14),dp(18));empty.setBackground(UiKit.surface(getContext()));l.addView(empty);return l;}
    }
}
