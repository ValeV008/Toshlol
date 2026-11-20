# Issue 10 verification

Verified on 2026-11-20 with the Android 10/API 29 `xperia_xz2_compact(AVD) - 10` emulator.

## Checks

- `.\gradlew.bat :app:ktlintCheck :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` — passed after the final test changes.
- `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain` — the full unfiltered suite passed 95/95, with no failures, errors, or skips. This run preceded the additional round-trip test below.
- `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.toshlol.personalfinance.BackupImportTest" --console=plain` — 10/10 passed, including `exportAndRestoreCompleteCustomBudgetDataset`.
- `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.toshlol.personalfinance.BackupImportTest,com.toshlol.personalfinance.BackupImportUiTest,com.toshlol.personalfinance.SettingsAccessibilityTest" --console=plain` — final focused regression run passed 14/14, including the round-trip, real-picker, restored UI, and accessibility journeys.

## Screenshots

- [`settings.png`](settings.png) — redesigned Settings at normal font size.
- [`settings-import-canceled.png`](settings-import-canceled.png) — Settings after a canceled import picker.
- [`settings-large-font.png`](settings-large-font.png) — Settings at 1.7× font scale.
- [`settings-overwrite-confirmation.png`](settings-overwrite-confirmation.png) — explicit Overwrite confirmation.
- [`settings-backup-status-error.png`](settings-backup-status-error.png) — readable, announced Backup error status.
