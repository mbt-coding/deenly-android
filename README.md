# Deenly Android project (Capacitor)

This folder turns the Deenly web app (`www/index.html`) into an Android app with **real device alarms**
(AlarmManager + a foreground media service for long Adhaan playback) and builds an **APK** and an **AAB** (Google Play bundle).

> **Status: not compiled.** These files were written and checked in a sandbox that has no Android SDK, Gradle or internet,
> so no APK or AAB has been built and the native code has never been run on a phone. Build it with one of the two options
> below and test on a real device before you rely on it for prayer times.

## Option A: build in the cloud (no Android Studio needed)
1. Create a GitHub repository and upload the contents of this folder as the repository root.
2. Open **Actions > Build Deenly (APK and AAB) > Run workflow**.
3. When it finishes, download the **deenly-android** artifact: it contains `app-debug.apk` (install on a phone) and `app-release.aab`.
4. For Google Play the AAB must be signed. Create a keystore once (keep it safe, you need it for every update):
   `keytool -genkey -v -keystore deenly.jks -alias deenly -keyalg RSA -keysize 2048 -validity 10000`
   then add repository secrets `KEYSTORE_B64` (`base64 -w0 deenly.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` and run the workflow again.

## Option B: build on your computer
Needs Node 18+, JDK 17, the Android SDK (`ANDROID_HOME`), and internet. Then run `bash build.sh`.
Outputs: `android/app/build/outputs/apk/debug/app-debug.apk` and `android/app/build/outputs/bundle/release/app-release.aab`.
To sign the AAB locally, export `KEYSTORE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` before running the script.

## What the native part does
* `DeenlyAlarmPlugin`: the bridge used by the web app (`checkStatus`, `requestNotifications`, `openExactAlarmSettings`, `openBatterySettings`, `saveAudio`, `schedule`, `cancelAll`, `test`, `stopPlayback`).
* `AlarmScheduler`: arms one system alarm per prayer, pre-prayer reminder and Iqamah reminder for the next 7 days (exact alarms when allowed).
* `AlarmReceiver` + `AdhanService`: at alarm time, plays the user's recording for its full length (or a long beep, or a short chime) in a foreground media service, with a Stop button in the notification.
* `BootReceiver`: re-arms the saved alarms after a restart, app update, clock change or time-zone change.
* The web app resends the schedule whenever settings, location or the date change, and only shows "Scheduled N alarms" after the plugin confirms.

## Permissions and what to tell users
* **Notifications** (Android 13+): required for reminders and the Stop button.
* **Exact alarms** (`SCHEDULE_EXACT_ALARM`): Android 14+ turns this off by default; the app has a button that opens the setting. Without it, alarms can be a few minutes late.
* **Battery optimisation / manufacturer power managers** (Xiaomi, Huawei, Oppo, Vivo, Samsung and others) can delay or kill alarms. Users should allow Deenly to run in the background.
* Alarms only exist for the next 7 days; opening the app every few days schedules new ones.

## Adhaan recordings
Deenly bundles **no** recordings. Users add their own files in Settings > Adhaan & Notifications (stored on the device, played offline) or paste a link (needs internet).
Check each recording's licence before redistributing it, and do not bundle a recording in the app unless you have permission.

## Google Play checklist
* Upload the **signed AAB**; enrol in Play App Signing.
* Declare the foreground service type **media playback** and the **exact alarm** use in Play Console; fill in the Data safety form (Deenly stores data on the device only; it calls Open-Meteo, OpenStreetMap Nominatim and alquran.cloud for weather, places and Quran text).
* Launcher icons in `icons/` were rendered from `Logo.svg`; Android's adaptive-icon mask may crop the edges, so check them in Android Studio's Image Asset tool if you want a perfect fit.
