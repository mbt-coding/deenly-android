package com.deenly.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import org.json.JSONObject;

/** Foreground media service: plays the whole Adhaan (or a long beep / chime) and can be stopped from the notification. */
public class AdhanService extends Service {
    static final String CH = "deenly_adhan", CH2 = "deenly_reminders", ACTION_STOP = "com.deenly.app.STOP";
    private MediaPlayer mp; private AudioTrack track; private Thread beepThread; private Ringtone rt;
    private volatile boolean stopped; private PowerManager.WakeLock wl;

    static void channels(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel a = new NotificationChannel(CH, "Adhaan", NotificationManager.IMPORTANCE_HIGH);
        a.setSound(null, null); a.setDescription("Shown while the Adhaan is playing");
        nm.createNotificationChannel(a);
        nm.createNotificationChannel(new NotificationChannel(CH2, "Prayer reminders", NotificationManager.IMPORTANCE_HIGH));
    }
    static void postNote(Context c, String title, String body) {
        channels(c);
        Notification n = new NotificationCompat.Builder(c, CH2).setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title).setContentText(body).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build();
        try { NotificationManagerCompat.from(c).notify((int) (System.currentTimeMillis() % 100000), n); } catch (SecurityException e) { /* notifications not allowed */ }
    }

    @Override public int onStartCommand(Intent i, int flags, int startId) {
        if (i != null && ACTION_STOP.equals(i.getAction())) { release(); stopSelf(); return START_NOT_STICKY; }
        String j = i == null ? null : i.getStringExtra("json");
        if (j == null) { stopSelf(); return START_NOT_STICKY; }
        try {
            JSONObject a = new JSONObject(j);
            channels(this);
            PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, AdhanService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            Notification n = new NotificationCompat.Builder(this, CH).setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(a.optString("title", "Deenly")).setContentText(a.optString("body", ""))
                .setOngoing(true).setCategory(NotificationCompat.CATEGORY_ALARM).addAction(0, "Stop", stop).build();
            if (Build.VERSION.SDK_INT >= 29) startForeground(77, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            else startForeground(77, n);
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "deenly:adhan"); wl.acquire(15 * 60 * 1000L);
            release(); stopped = false;
            String mode = a.optString("mode", "chime");
            if ("beep".equals(mode)) beep(a.optInt("beepSec", 10), (float) a.optDouble("vol", 0.8));
            else if ("audio".equals(mode)) audio(a);
            else chime();
        } catch (Exception e) { stopSelf(); }
        return START_NOT_STICKY;
    }
    private AudioAttributes attrs() {
        return new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
    }
    private void audio(JSONObject a) {
        try {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            mp = new MediaPlayer(); mp.setAudioAttributes(attrs());
            String path = a.optString("path", "");
            mp.setDataSource(path.isEmpty() ? a.optString("url") : path);
            float v = (float) a.optDouble("vol", 0.8); mp.setVolume(v, v);
            mp.setOnCompletionListener(m -> { release(); stopSelf(); });
            mp.setOnErrorListener((m, w, e) -> { release(); chime(); return true; });
            mp.setOnPreparedListener(MediaPlayer::start);
            mp.prepareAsync();
        } catch (Exception e) { chime(); }
    }
    private void beep(int sec, float vol) {
        final int rate = 22050, n = rate * Math.max(1, Math.min(60, sec));
        beepThread = new Thread(() -> {
            try {
                short[] buf = new short[n];
                for (int k = 0; k < n; k++) {
                    double env = Math.min(1.0, Math.min(k / (rate * 0.05), (n - k) / (rate * 0.1)));
                    buf[k] = (short) (Math.sin(2 * Math.PI * 880 * k / rate) * env * vol * 0.5 * Short.MAX_VALUE);
                }
                track = new AudioTrack.Builder().setAudioAttributes(attrs())
                    .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(buf.length * 2).setTransferMode(AudioTrack.MODE_STATIC).build();
                track.write(buf, 0, buf.length); track.play();
                long end = System.currentTimeMillis() + sec * 1000L + 300;
                while (!stopped && System.currentTimeMillis() < end) Thread.sleep(100);
            } catch (Exception e) { /* ignore */ }
            new Handler(Looper.getMainLooper()).post(() -> { release(); stopSelf(); });
        });
        beepThread.start();
    }
    private void chime() {
        try {
            rt = RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)); rt.play();
        } catch (Exception e) { /* ignore */ }
        new Handler(Looper.getMainLooper()).postDelayed(() -> { release(); stopSelf(); }, 5000);
    }
    private void release() {
        stopped = true;
        try { if (mp != null) { mp.release(); mp = null; } } catch (Exception e) { }
        try { if (track != null) { track.stop(); track.release(); track = null; } } catch (Exception e) { }
        try { if (rt != null) { rt.stop(); rt = null; } } catch (Exception e) { }
        try { if (wl != null && wl.isHeld()) wl.release(); } catch (Exception e) { }
    }
    @Override public void onDestroy() { release(); super.onDestroy(); }
    @Override public IBinder onBind(Intent i) { return null; }
}
