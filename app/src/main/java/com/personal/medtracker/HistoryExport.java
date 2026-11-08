package com.personal.medtracker;
import android.content.Context;
import java.time.Instant;
import java.util.List;

final class HistoryExport {
    private HistoryExport(){}
    static String csv(Context c,List<Store.Event> events){
        int[] headers={R.string.csv_date,R.string.csv_time,R.string.csv_medication,R.string.csv_dose,R.string.csv_status,R.string.csv_recorded,R.string.csv_scheduled_at,R.string.csv_taken_at,R.string.csv_timezone,R.string.csv_difference};
        StringBuilder out=new StringBuilder();
        for(int i=0;i<headers.length;i++){if(i>0)out.append(',');out.append(Schedule.csv(AppLocale.text(c,headers[i])));}out.append("\r\n");
        for(Store.Event e:events){
            String status=AppLocale.text(c,"taken".equals(e.status)?R.string.status_taken:R.string.status_skipped);
            out.append(Schedule.csv(e.day)).append(',').append(Schedule.csv(e.time)).append(',').append(Schedule.csv(safe(e.name))).append(',')
                .append(Schedule.csv(safe(e.dose))).append(',').append(Schedule.csv(status)).append(',').append(Schedule.csv(Instant.ofEpochMilli(e.recorded).toString())).append(',').append(Schedule.csv(e.scheduledAt>0?Instant.ofEpochMilli(e.scheduledAt).toString():""))
                .append(',').append(Schedule.csv(e.takenAt>0?Instant.ofEpochMilli(e.takenAt).toString():""))
                .append(',').append(Schedule.csv(e.zone)).append(',').append(Schedule.csv(e.takenAt>0&&e.scheduledAt>0?String.format(java.util.Locale.US,"%.2f",(e.takenAt-e.scheduledAt)/60000d):"")).append("\r\n");
        }return out.toString();
    }
    private static String safe(String value){return value.matches("(?s)^[\\s]*[=+@-].*")?"'"+value:value;}
}
