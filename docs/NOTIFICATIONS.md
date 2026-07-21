# Notifications

Push notifications are handled by Firebase Cloud Messaging (FCM) - see `background/service/FirebaseService.kt`
and `background/notification/Notifications.kt` in [Architecture](ARCHITECTURE.md). This is a manually-operated
channel: staff send a message directly from the Firebase Console, there's no app-side "send" flow to build or
test against.

## Testing with the existing Firebase project

`app/google-services.json` is already checked into this repo, so a normal [Installation](INSTALLATION.md) build
is already wired up to the shared Firebase project - you don't need to do anything extra to receive a push.

To send yourself a test notification:

1. Run a debug build and let the app start once (this registers the device and generates an FCM token).
2. Check Logcat for the tag `FCM_TOKEN` - `FirebaseService.onNewToken()` logs it on debug builds only.
3. In the [Firebase Console](https://console.firebase.google.com/), open this project → **Engage** →
   **Messaging** → **New campaign** → **Notifications**.
4. Under **Target**, choose **Send test message** and paste in the token from step 2. This sends the
   notification to just your device instead of every install.
5. Send it. If the app is backgrounded or not running, Android displays it automatically using the manifest's
   default icon/color/channel. If the app is in the foreground, `FirebaseService.onMessageReceived()` builds
   and shows it manually instead - both paths are worth testing.

Notification permission (Android 13+) is requested during the first-run setup flow
(`feature/setup/Permission.kt`); if you skipped or denied it, re-enable it from the device's app settings.

## Using your own Firebase project instead

You'd want this if you're working somewhere you don't have access to the shared project, or want to send
test pushes without touching the real one everyone else's dev builds are wired to.

1. Create a project at the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app to it with the applicationId from `app/build.gradle.kts` (`edu.rpi.shuttletracker`) -
   or a different one if you also change `applicationId`, since it has to match exactly.
3. Download the generated `google-services.json` and replace `app/google-services.json` with it.
4. Rebuild. The `com.google.gms.google-services` Gradle plugin reads this file at build time, so nothing
   else in the code needs to change.

Don't commit your own `google-services.json` over the shared one unless that's an intentional, agreed-on
switch - anyone else building from `main` would silently start using your project instead of the shared one.
