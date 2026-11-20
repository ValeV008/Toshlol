# Issue 09 verification

Date: 2026-11-20
Device: `xperia_xz2_compact(AVD) - 10`, Android 10/API 29

## Checks

- `python -m unittest discover -s scripts -p 'test_*.py'` - 15/15 passed.
- `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:ktlintCheck :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` - passed.
- `python scripts/verify_overview_persistence.py --serial emulator-5554 --skip-build` - selected and empty Account filters survived host-driven process restarts. Evidence is in `build/overview-persistence/20261120T130844944098Z/`.
- `FirstRunFlowTest` at system font scale 2x - 4/4 passed.
- `AccessibilityTest` - 6/6 passed, including large-font selected navigation, loading announcement, and saving progress semantics.
- Unfiltered `:app:connectedDebugAndroidTest` at font scale 1.0 - 92/92 passed, 0 failures, 0 errors, 0 skips; duration 18m 12s.

## Logs and report

- [Full connected-suite log](connected-suite.log)
- [Connected-suite XML report](connected-suite-results.xml)
- [2x first-run flow log](first-run-large-font.log)
- [2x form recreation log](form-recreation-large-font.log)

## Screenshots

| State | Normal font | 2x font |
| --- | --- | --- |
| Setup | [first-run-setup.png](normal/first-run-setup.png) | [first-run-setup.png](large-font/first-run-setup.png) |
| Validation | [first-run-validation.png](normal/first-run-validation.png) | [first-run-validation.png](large-font/first-run-validation.png) |
| Retry | [first-run-retry.png](normal/first-run-retry.png) | [first-run-retry-large-font.png](large-font/first-run-retry-large-font.png) |
| Empty Overview | [first-run-empty-overview.png](normal/first-run-empty-overview.png) | [first-run-empty-overview.png](large-font/first-run-empty-overview.png) |
