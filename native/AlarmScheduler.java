package com.deenly.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

/** Arms one system alarm per entry using AlarmManager (works with the app closed, subject to Android power rules). */
public class AlarmScheduler {
    private static PendingIntent pending(Context c, JSONObject a, int flag) throws Exception {
        Intent i = new Intent(c, AlarmReceiver.class).setAction("com.deenly.app.ALARM").putExtra("json", a.toString());
        return PendingIntent.getBroadcast(c, a.getString("id").hashCode(), i, flag | PendingIntent.FLAG_IMMUTABLE);
    }
    public static boolean canExact(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        return Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
    }
    public static int scheduleAll(Context c) {
        int n = 0;
        try {
            JSONArray arr = new JSONArray(AlarmStore.load(c));
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            boolean exact = canExact(c);
            long now = System.currentTimeMillis();
            for (int k = 0; k < arr.length(); k++) {
                JSONObject a = arr.getJSONObject(k);
                long t = a.getLong("t");
                if (t <= now) continue;
                PendingIntent p = pending(c, a, PendingIntent.FLAG_UPDATE_CURRENT);
                if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, p);
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, p);
                n++;
            }
        } catch (Exception e) { /* ignore malformed list */ }
        return n;
    }
    public static void cancelAll(Context c) {
        try {
            JSONArray arr = new JSONArray(AlarmStore.load(c));
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            for (int k = 0; k < arr.length(); k++) {
                PendingIntent p = pending(c, arr.getJSONObject(k), PendingIntent.FLAG_NO_CREATE);
                if (p != null) { am.cancel(p); p.cancel(); }
            }
        } catch (Exception e) { /* nothing to cancel */ }
    }
}
