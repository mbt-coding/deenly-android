package com.deenly.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        String j = i.getStringExtra("json");
        if (j == null) return;
        try {
            JSONObject a = new JSONObject(j);
            if ("none".equals(a.optString("mode", "none"))) {
                AdhanService.postNote(c, a.optString("title", "Deenly"), a.optString("body", ""));
                return;
            }
            ContextCompat.startForegroundService(c, new Intent(c, AdhanService.class).putExtra("json", j));
        } catch (Exception e) { /* ignore */ }
    }
}
