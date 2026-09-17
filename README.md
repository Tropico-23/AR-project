# MoneyFlow

A polished, offline-first Android expense tracker built with Kotlin and Jetpack Compose.

## Features

- Dashboard with today/month totals and budget progress
- Expense entry with amount, title, category, date, notes and payment method
- Search and category filtering
- Delete transactions
- Weekly spending chart
- Category breakdown donut chart
- Six-month spending trend
- Monthly budget tracking
- Light/dark mode
- Local-only storage using SharedPreferences + JSON

## Build on Linux without Android Studio

Install a JDK 17, Android SDK Platform 35, Android Build Tools, and Gradle 8.x. Then from the repository root:

```bash
gradle :app:assembleDebug
```

The APK will be under `app/build/outputs/apk/debug/`.

To install on a connected phone:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

No server or account is required by the app.
