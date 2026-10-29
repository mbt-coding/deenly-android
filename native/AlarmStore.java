package com.deenly.app;

import android.content.Context;

/** Keeps the scheduled alarm list so it can be re-armed after a reboot or time change. */
public class AlarmStore {
    private static final String PREFS = "deenly_alarms";
    public static void save(Context c, String json) { c.getSharedPreferences(PREFS, 0).edit().putString("list", json).apply(); }
    public static String load(Context c) { return c.getSharedPreferences(PREFS, 0).getString("list", "[]"); }
}
