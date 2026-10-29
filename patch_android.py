#!/usr/bin/env python3
"""Run after `npx cap add android`: copies the native alarm code, patches the manifest and installs launcher icons."""
import os, re, shutil, sys
base = os.path.dirname(os.path.abspath(__file__))
app = os.path.join(base, "android", "app", "src", "main")
if not os.path.isdir(app): sys.exit("Run `npx cap add android` first (android/ folder not found).")
java = os.path.join(app, "java", "com", "deenly", "app"); os.makedirs(java, exist_ok=True)
for f in os.listdir(os.path.join(base, "native")):
    if f.endswith(".java"): shutil.copy(os.path.join(base, "native", f), os.path.join(java, f))
man = os.path.join(app, "AndroidManifest.xml"); t = open(man).read()
if "com.deenly.app.ALARM" not in t:
    perms = "".join('    <uses-permission android:name="android.permission.%s"/>\n' % p for p in
        ["POST_NOTIFICATIONS", "SCHEDULE_EXACT_ALARM", "RECEIVE_BOOT_COMPLETED", "FOREGROUND_SERVICE", "FOREGROUND_SERVICE_MEDIA_PLAYBACK", "WAKE_LOCK", "VIBRATE"])
    comp = """    <receiver android:name=".AlarmReceiver" android:exported="false"><intent-filter><action android:name="com.deenly.app.ALARM"/></intent-filter></receiver>
    <receiver android:name=".BootReceiver" android:exported="true"><intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED"/><action android:name="android.intent.action.MY_PACKAGE_REPLACED"/>
        <action android:name="android.intent.action.TIME_SET"/><action android:name="android.intent.action.TIMEZONE_CHANGED"/></intent-filter></receiver>
    <service android:name=".AdhanService" android:exported="false" android:foregroundServiceType="mediaPlayback"/>
"""
    t = t.replace("</application>", comp + "</application>", 1)
    t = re.sub(r"(<manifest[^>]*>)", lambda m: m.group(1) + "\n" + perms, t, count=1)
    open(man, "w").write(t)
icons = os.path.join(base, "icons")
for d in os.listdir(icons):
    dst = os.path.join(app, "res", d); os.makedirs(dst, exist_ok=True)
    for f in os.listdir(os.path.join(icons, d)): shutil.copy(os.path.join(icons, d, f), os.path.join(dst, f))
gr = os.path.join(base, "android", "app", "build.gradle"); g = open(gr).read()
g = re.sub(r"versionCode \d+", "versionCode 209", g, count=1); g = re.sub(r'versionName "[^"]*"', 'versionName "2.0.9"', g, count=1)
open(gr, "w").write(g)
print("Native alarm code, manifest entries and icons installed.")
