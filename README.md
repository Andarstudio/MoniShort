# MoniShort

Android automation app inspired by Apple Shortcuts, with its own Material 3 design.
Kotlin · Jetpack Compose · Room · DataStore · Coroutines · min SDK 26 (Android 8.0), target/compile SDK 35.

## Build the APK

- **GitHub Actions** (included): `.github/workflows/build.yml` builds `app-debug.apk` and runs the unit tests on every push
  to `main`. Download it from the Actions run (artifact `MoniShort-debug-apk`) or from the `debug-latest` release.
- **Android Studio**: open this folder (JDK 17), let Gradle sync, press Run.
- **Command line**: `gradle assembleDebug` (Gradle 8.9) -> `app/build/outputs/apk/debug/app-debug.apk`.

Install: copy the APK to the phone, open it, allow "install unknown apps" for your file manager/browser when asked.

## Status: batch 1 (foundation) - NOT yet compiled by the author

This code was written without access to an Android SDK, so it has not been compiled yet. The first CI run may report
compile errors; they are expected to be small and should be fixed from the build log.

Implemented:
- Automation engine, independent of Compose/Android: sequential execution, If/Otherwise, Repeat, Repeat Until,
  `{{variable}}` templates, text/number/boolean/list/dictionary values, cancellation, step/time limits, concurrency cap,
  execution history. Actions/triggers/conditions are interfaces with registries.
- Actions: set/modify variable, wait, notification, play/stop sound, battery, date/time, open URL, HTTP GET.
- Triggers: charger connected/disconnected, battery low, battery level crossing, device boot.
- **Charging automation**: separate connect/disconnect sounds picked with the Storage Access Framework (no storage
  permission needed), preview, volume, notifications, enable/disable, test buttons.
- Room database (shortcuts, automations, folders, templates, execution history), DataStore settings.
- UI: Home, Shortcuts (create one-action shortcuts, run, delete), Automation (create trigger+action, enable/disable,
  delete), Settings (monitor switch, notification and battery-optimization setup), Charging screen.
- Unit tests: values/templates, conditions, engine, JSON codec, validator, triggers.

Not built yet: multi-step visual editor with drag & drop, folders UI, gallery templates, time/Wi-Fi/Bluetooth/
app-launch/notification triggers, file and JSON actions, Accessibility service, full Permission Center, Room
migrations (schema is still version 1).

## Android limitations
- Charger and battery events are not delivered to manifest receivers on Android 8+. MoniShort therefore runs a
  foreground service with a persistent (minimized) notification. Some manufacturers (Xiaomi/Redmi, Huawei, Oppo...)
  also need Autostart enabled and battery restrictions removed for MoniShort.
- Android 10+ restricts starting activities from the background, so "Open URL" can fail when triggered by an automation.
- Plain `http://` requests are blocked by default on modern Android; use `https://`.
- Charging sounds use the notification volume stream and follow Do Not Disturb.
