import com.personal.medtracker.Schedule;
import java.time.*;
import java.util.*;

public class ScheduleTest {
    private static int checks;
    private static void equal(Object expected,Object actual) {
        checks++; if(!expected.equals(actual)) throw new AssertionError("Expected "+expected+", got "+actual);
    }
    private static void invalid(String value) {
        checks++; try {Schedule.parseTimes(value);throw new AssertionError("Accepted "+value);} catch(IllegalArgumentException expected) {}
    }
    private static void next(String now,String time,String zone,String expected) {
        equal(Instant.parse(expected).toEpochMilli(),Schedule.next(time,Instant.parse(now).toEpochMilli(),ZoneId.of(zone)));
    }
    public static void main(String[] args) {
        equal(Arrays.asList("08:00","12:30","23:59"),Schedule.parseTimes("23:59,08:00,12:30,08:00"));
        equal(Arrays.asList("00:00"),Schedule.parseTimes("00:00"));
        for(String s:new String[]{"","25:00","24:00","12:60","8:00","text",","}) invalid(s);
        invalid("00:00,01:00,02:00,03:00,04:00,05:00,06:00,07:00,08:00,09:00,10:00,11:00,12:00");
        next("2026-11-06T07:00:00Z","08:00","UTC","2026-11-06T08:00:00Z");
        next("2026-11-06T08:00:00Z","08:00","UTC","2026-11-07T08:00:00Z");
        next("2026-12-31T23:59:59Z","00:00","UTC","2027-01-01T00:00:00Z");
        next("2026-11-06T04:00:00Z","08:00","Asia/Kolkata","2026-11-07T02:30:00Z");
        // Spring-forward nonexistent times move forward by the DST gap.
        next("2026-03-08T05:00:00Z","02:30","America/New_York","2026-03-08T07:30:00Z");
        // Calendar days preserve wall-clock times across 23-hour days.
        next("2026-03-07T14:00:00Z","08:00","America/New_York","2026-03-08T12:00:00Z");
        // Fall-back schedules fire once at the earlier occurrence.
        next("2026-11-01T04:00:00Z","01:30","America/New_York","2026-11-01T05:30:00Z");
        next("2026-11-01T05:31:00Z","01:30","America/New_York","2026-11-02T06:30:00Z");
        equal("7/2026-11-06/08:00",Schedule.key(7,"2026-11-06","08:00"));
        equal("\"a,\"\"b\"\"\"",Schedule.csv("a,\"b\""));
        System.out.println("PASS: "+checks+" schedule, DST, validation, identity and CSV checks");
    }
}
