package com.deenly.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Re-arms the saved alarms after a reboot, app update, clock change or time-zone change. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) { AlarmScheduler.scheduleAll(c); }
}
