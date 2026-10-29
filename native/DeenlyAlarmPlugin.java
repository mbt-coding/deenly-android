package com.deenly.app;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Base64;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import java.io.File;
import java.io.FileOutputStream;

@CapacitorPlugin(name = "DeenlyAlarm", permissions = { @Permission(strings = { Manifest.permission.POST_NOTIFICATIONS }, alias = "notifications") })
public class DeenlyAlarmPlugin extends Plugin {

    private JSObject status() {
        Context c = getContext();
        boolean notif = Build.VERSION.SDK_INT >= 33 ? getPermissionState("notifications") == PermissionState.GRANTED
                : NotificationManagerCompat.from(c).areNotificationsEnabled();
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        JSObject o = new JSObject();
        o.put("notifications", notif);
        o.put("exactAlarm", AlarmScheduler.canExact(c));
        o.put("batteryUnrestricted", pm.isIgnoringBatteryOptimizations(c.getPackageName()));
        return o;
    }
    @PluginMethod public void checkStatus(PluginCall call) { call.resolve(status()); }

    @PluginMethod public void requestNotifications(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 33 && getPermissionState("notifications") != PermissionState.GRANTED)
            requestPermissionForAlias("notifications", call, "notifResult");
        else call.resolve(status());
    }
    @PermissionCallback private void notifResult(PluginCall call) { call.resolve(status()); }

    @PluginMethod public void openExactAlarmSettings(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 31) {
            Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getContext().getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); getContext().startActivity(i);
        }
        call.resolve();
    }
    @PluginMethod public void openBatterySettings(PluginCall call) {
        Intent i = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); getContext().startActivity(i);
        call.resolve();
    }
    /** Stores a recording the user added so the alarm can play it with no internet. */
    @PluginMethod public void saveAudio(PluginCall call) {
        try {
            String key = call.getString("key", "audio").replaceAll("[^A-Za-z0-9_.-]", "_");
            File dir = new File(getContext().getFilesDir(), "adhan"); dir.mkdirs();
            File f = new File(dir, key);
            try (FileOutputStream out = new FileOutputStream(f)) { out.write(Base64.decode(call.getString("data", ""), Base64.DEFAULT)); }
            JSObject o = new JSObject(); o.put("path", f.getAbsolutePath()); call.resolve(o);
        } catch (Exception e) { call.reject("Could not save the recording: " + e.getMessage()); }
    }
    @PluginMethod public void schedule(PluginCall call) {
        try {
            AlarmScheduler.cancelAll(getContext());
            AlarmStore.save(getContext(), call.getArray("alarms").toString());
            int n = AlarmScheduler.scheduleAll(getContext());
            JSObject o = status(); o.put("count", n); call.resolve(o);
        } catch (Exception e) { call.reject("Could not schedule: " + e.getMessage()); }
    }
    @PluginMethod public void cancelAll(PluginCall call) {
        AlarmScheduler.cancelAll(getContext()); AlarmStore.save(getContext(), "[]"); call.resolve();
    }
    @PluginMethod public void test(PluginCall call) {
        try {
            JSObject a = call.getData(); a.put("title", "Deenly test"); a.put("body", "Test alarm");
            ContextCompat.startForegroundService(getContext(), new Intent(getContext(), AdhanService.class).putExtra("json", a.toString()));
            call.resolve();
        } catch (Exception e) { call.reject("Could not start the test: " + e.getMessage()); }
    }
    @PluginMethod public void stopPlayback(PluginCall call) {
        getContext().startService(new Intent(getContext(), AdhanService.class).setAction(AdhanService.ACTION_STOP)); call.resolve();
    }
}
