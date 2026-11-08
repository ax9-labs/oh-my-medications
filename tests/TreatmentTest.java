import com.personal.medtracker.*;
import java.time.*;
import java.util.*;

public class TreatmentTest {
    static int count;
    static void check(boolean b,String s){count++;if(!b)throw new AssertionError(s);}
    public static void main(String[] args){
        check(Treatment.end("2026-11-07",0)==null,"Undefined has no end");check(Treatment.end("2026-11-07",7).equals(LocalDate.of(2026,11,13)),"Inclusive end");
        check(!Treatment.includes("2026-11-07",7,LocalDate.of(2026,11,6)),"Before course");check(Treatment.includes("2026-11-07",7,LocalDate.of(2026,11,13)),"Last day included");check(!Treatment.includes("2026-11-07",7,LocalDate.of(2026,11,14)),"After course");
        check(Treatment.progress("2026-11-07",7,LocalDate.of(2026,11,6))==0,"Future progress");check(Treatment.dayNumber("2026-11-07",7,LocalDate.of(2026,11,9))==3,"Day three");check(Treatment.progress("2026-11-07",7,LocalDate.of(2026,11,20))==1,"Completed progress");
        long now=Instant.parse("2026-11-07T09:00:00Z").toEpochMilli();
        check(Treatment.next("08:00","2026-11-07",1,now,ZoneId.of("UTC"))==-1,"No reminder after last dose");
        check(Treatment.next("08:00","2026-11-09",1,now,ZoneId.of("UTC"))==Instant.parse("2026-11-09T08:00:00Z").toEpochMilli(),"Future first dose");
        List<Adherence.Sample> s=new ArrayList<>();String day="2026-11-07";long due=now-2*3600_000;
        s.add(new Adherence.Sample(day,due,due+30*60_000,"taken",true));s.add(new Adherence.Sample(day,due,due-30*60_000,"taken",true));
        s.add(new Adherence.Sample(day,due,due+31*60_000,"taken",true));s.add(new Adherence.Sample(day,due,due-31*60_000,"taken",true));
        s.add(new Adherence.Sample(day,due,0,"skipped",true));s.add(new Adherence.Sample(day,due,0,null,true));
        s.add(new Adherence.Sample(day,now+3600_000,0,null,true));s.add(new Adherence.Sample(day,now-10*60_000,0,null,true));s.add(new Adherence.Sample(day,due,0,null,false));
        Adherence.Report r=Adherence.summarize(s,LocalDate.parse(day),LocalDate.parse(day),now);
        check(r.onTime==2&&r.outside==2,"30 minute inclusive tolerance");check(r.skipped==1&&r.unlogged==1,"Skipped distinct from unlogged");check(r.eligible()==6,"Future, grace and untracked excluded");check(r.accuracy()==50,"Timing denominator is known taken times");check(r.averageDifferenceMinutes()==31,"Absolute deviation does not cancel early and late");
        s.clear();s.add(new Adherence.Sample(day,due,0,"taken",true));r=Adherence.summarize(s,LocalDate.parse(day),LocalDate.parse(day),now);check(r.taken==1&&r.accuracy()==-1,"Legacy unknown actual time is not invented");
        System.out.println("PASS: "+count+" treatment and adherence checks");
    }
}
