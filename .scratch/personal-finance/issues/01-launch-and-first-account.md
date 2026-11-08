# 01: Launch app and create the first Account

**What to build:** A new user can launch the Android app, select a currency, create the first Account, and reach a persisted empty Overview.

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] The project builds as a native Android app for Android 10 (API 29) with the agreed Compose, Room, DataStore, and test foundation.
- [x] The app uses the light theme and starts without network permission or seeded Accounts and Categories.
- [x] First-run setup requires a currency and a valid user-created Account with a supported type.
- [x] The Account and currency persist across relaunch.
- [x] The empty Overview explains the next useful actions without inventing financial data.
- [x] Unit, persistence, Compose, and accessibility tests cover the first-run path.

## Answer

Implemented the first-run path with a supported-currency picker, a required trimmed Account name, and Cash, Bank, Credit Card, and Other types. Currency and the first Account commit atomically in Room. Accounts use UUIDs and separately injectable timestamps. Repeated submissions are idempotent.

Startup waits for persisted data, reports recoverable storage failures, and retains form state across activity recreation. The light-only, scrollable setup exposes radio selection, headings, validation announcements, and at least 48 dp Account-type targets. The empty Overview contains guidance without sample finance data or references to unavailable controls.

The foundation now uses application ID `com.toshlol.finance`, a version catalog, SDK 37 with `minSdk 29`, JVM 17, Gradle 9.3.1, AGP 9.1.1, built-in Kotlin with the Compose compiler plugin, KSP 2.3.12, and Room 2.8.5. See [Android toolchain compatibility](https://developer.android.com/build/releases/agp-9-1-0-release-notes) and [KSP releases](https://github.com/google/ksp/releases). Network permission and automatic Android backup are absent.

An explicit version 1 to 2 migration preserves existing Accounts, assigns unique UUIDs once, and moves legacy currency into Room. Completed ledgers bypass obsolete preferences; cleanup failures do not prevent launch.

### Verification

- All 9 JVM tests passed.
- All 12 instrumented tests passed on Android 10 (API 29): persistence, close/reopen durability, concurrency, rollback, migration, cross-install UUID uniqueness, legacy preference failures, Compose setup/relaunch, rotation, validation, and large-font accessibility semantics.
- `lintDebug` passed with no errors. Remaining warnings concern available dependency updates, optional newer-Android backup configuration, and the application icon.
- `ktlintCheck` and `assembleDebug` passed.
- Standards review: 0 remaining findings. Spec review: 0 remaining findings.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

Reproduce with JDK 17 and the Android SDK configured:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:ktlintCheck :app:connectedDebugAndroidTest :app:assembleDebug --max-workers=1 --console=plain --no-daemon
```

On a memory-constrained workstation, append `'-Dorg.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8'` and `'-Pkotlin.compiler.execution.strategy=in-process'`.

## Comments

The user removed the manual TalkBack and Switch Access testing requirements. Those checks were not performed. All required automated checks passed, so this issue is resolved.
