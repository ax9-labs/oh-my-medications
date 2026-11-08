package com.personal.medtracker;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/** Calendar-day schedules follow the device's local timezone, including DST. */
public final class Schedule {
    private Schedule() {}
    public static List<String> parseTimes(String raw) {
        TreeSet<String> times = new TreeSet<>();
        for (String token : raw.split(",")) {
            try {
                LocalTime t = LocalTime.parse(token.trim(), DateTimeFormatter.ofPattern("HH:mm"));
                String value = t.format(DateTimeFormatter.ofPattern("HH:mm"));
                if (!value.equals(token.trim())) throw new IllegalArgumentException("Use HH:mm");
                times.add(value);
            } catch (DateTimeParseException ex) { throw new IllegalArgumentException("Invalid time", ex); }
        }
        if (times.isEmpty() || times.size() > 12) throw new IllegalArgumentException("Choose 1–12 dose times");
        return new ArrayList<>(times);
    }
    public static long next(String time, long now, ZoneId zone) {
        ZonedDateTime current = Instant.ofEpochMilli(now).atZone(zone);
        ZonedDateTime next = current.toLocalDate().atTime(LocalTime.parse(time)).atZone(zone);
        if (next.toInstant().toEpochMilli() <= now)
            next = current.toLocalDate().plusDays(1).atTime(LocalTime.parse(time)).atZone(zone);
        return next.toInstant().toEpochMilli();
    }
    public static String key(long med, String day, String time) { return med + "/" + day + "/" + time; }
    public static String csv(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
}
