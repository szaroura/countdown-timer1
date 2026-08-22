# Countdown Timer (Android)

A native Android app: enter minutes and seconds, tap Start, and it counts
down with a progress ring. The countdown runs in a foreground service, so
it keeps going even if you lock the phone or switch apps — you'll see a
persistent notification with the time remaining, and when it hits zero it
plays an alarm sound (six beeps) and posts a "Time's up" notification.

Built with Kotlin + Jetpack Compose.

## How to build it without installing Android Studio

This project includes a GitHub Actions workflow (`.github/workflows/build.yml`)
that compiles the APK on GitHub's servers — you just upload the code and
download the finished file.

1. Create a free account at [github.com](https://github.com) if you don't
   have one.
2. Create a new repository (any name, e.g. `countdown-timer`) — public or
   private, doesn't matter.
3. Upload this project's contents into it. Easiest way without touching a
   terminal: on the repo page, click **Add file > Upload files**, then drag
   in the whole unzipped `CountdownTimer` folder (or its contents — the
   `app/`, `.github/`, `gradle/` folders and the loose files at the top
   level). Commit the upload.
4. Go to the **Actions** tab of your repo. A workflow run should start
   automatically (or click **Run workflow** if it doesn't). It takes a few
   minutes.
5. Once it finishes with a green checkmark, click into the run, scroll to
   **Artifacts**, and download `countdown-timer-debug-apk` — it's a zip
   containing `app-debug.apk`.
6. Transfer that `.apk` file to your phone (email it to yourself, upload to
   Google Drive and download on the phone, etc.), then tap it to install.
   Your phone will ask you to allow installing from that source the first
   time — allow it, then install.

No Android Studio, no SDK download, no command line required on your end.

## How to open and run it (if you do want Android Studio)

1. Install [Android Studio](https://developer.android.com/studio) if you
   don't have it already.
2. Unzip this project, then in Android Studio choose
   **File > Open** and select the unzipped `CountdownTimer` folder.
3. Let Gradle sync (Android Studio will download the Gradle wrapper jar and
   dependencies automatically on first open — this needs an internet
   connection and can take a few minutes the first time).
4. Plug in an Android phone with USB debugging enabled, or start an emulator
   from **Device Manager**.
5. Click the green **Run** button (or Shift+F10).

The app will install and launch. Set a time, hit Start, and it will beep
when it finishes.

## What's in the code

- `app/src/main/java/com/example/countdowntimer/TimerService.kt` — a
  foreground `Service` that owns the actual `CountDownTimer`, so it keeps
  running independent of the activity. Exposes remaining time and running
  state as `StateFlow`s, manages the ongoing notification, and plays the
  alarm (`ToneGenerator`, no audio file needed) plus a "Time's up"
  notification when it finishes.
- `app/src/main/java/com/example/countdowntimer/MainActivity.kt` — the UI
  (`TimerScreen`, Compose). Binds to `TimerService` and just reflects its
  state; requests the notification permission on Android 13+.
- `app/src/main/AndroidManifest.xml` — declares the activity, the service
  (with the Android 14+ required `foregroundServiceType`), and the
  permissions needed for a foreground service and notifications.
- `app/build.gradle.kts` — dependencies (Compose, Material 3,
  `lifecycle-service`).

## Notes on the background behavior

- The first time you tap Start on Android 13+, you'll be asked to allow
  notifications — that's what lets the app show the running countdown and
  the finished alert while backgrounded.
- The service is a "specialUse" foreground service, the category Google
  recommends for things like this that don't fit media/location/etc. It
  keeps the process alive and shows a required ongoing notification while
  the timer runs, per Android's background-service rules.
- If Android's battery optimization is aggressive on a given phone, you may
  want to exclude the app from battery optimization for the alarm to be
  reliable when the timer runs for a long time.

## Ideas to extend it

- Vibrate on finish (`Vibrator` / `VibratorManager`).
- Add a Pause/Stop action button directly on the notification.
- Let the person pick a custom alarm sound.
- Save the last-used time so it's remembered next launch.
