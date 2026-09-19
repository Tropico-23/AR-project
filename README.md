# MoneyFlow

MoneyFlow is a fully offline Android expense-tracking app built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, and StateFlow.

## Requirements

- Linux
- JDK 17
- Android SDK Command-line tools
- Android platform + build tools for API 35
  - `platforms;android-35`
  - `build-tools;35.0.0`

## Build from command line

```bash
./gradlew :app:assembleDebug
```

## Run tests

```bash
./gradlew :app:testDebugUnitTest
```

## APK output path

`app/build/outputs/apk/debug/app-debug.apk`

## Install on physical device

1. Enable developer options + USB debugging on device.
2. Connect device and verify:
   ```bash
   adb devices
   ```
3. Install APK:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
4. Launch **MoneyFlow** from app drawer.

## Notes

- Default currency: **TND**.
- No backend, no analytics SDK, no tracking dependency, no network permission.
- Local persistence only (Room + DataStore).
