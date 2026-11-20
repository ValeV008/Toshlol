# Integrated Android 10 verification

Run: 2026-11-20 on `xperia_xz2_compact(AVD) - 10` (API 29), debug build.

- `python -m unittest discover -s scripts -p 'test_*.py'`: passed, 15/15.
- `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:ktlintCheck`, `:app:assembleDebug`, and `:app:assembleDebugAndroidTest`: passed.
- `python scripts/verify_overview_persistence.py --serial emulator-5554 --skip-build`: passed for selected and explicitly empty Account subsets after process restarts. Full evidence: `build/overview-persistence/20261120T214326577741Z/` (screenshots, UI hierarchies, seed results, logcat, commands, result JSON).
- `:app:connectedDebugAndroidTest`: passed unfiltered on API 29, 96/96 tests, zero failures, errors, or skips. Report: `app/build/reports/androidTests/connected/debug/index.html`; XML results: `app/build/outputs/androidTest-results/connected/debug/`.

The suite covers the owning journeys for setup, drawer selection, monthly ledgers, transaction editing and account preservation, transfers, account history/archive, Categories and Tags, custom Budget matching/migration, and Backup import/export/reset. Relevant named tests include `createFirstAccountAndKeepOverviewAcrossRelaunch`, `editTransactionPreservesAccountAndReusesLastAccountAcrossDirections`, `recordAndEditTransferThroughFullScreenEntry`, `calculateClosingBalancesAtSelectedMonthEnd`, `matchBudgetAcrossAccountCategoryAndTagSelections`, and `exportAndRestoreCompleteCustomBudgetDataset`.

A curated screenshot set is in this directory. It includes fresh selected/empty Overview and selected-switch drawer captures from this run, and representative screens captured by the resolved feature tickets for Expenses, Income, entry, Account editor/history, Transfers entry, Budgets, Categories & Tags, Settings, and setup. Screens were compared visually against `UI_pics/overview.jpg` and the applicable supplied reference set: charcoal month header, yellow actions/selection, pale background, rounded cards, and signed red/green amounts are present. The references' sample amounts and broader layout are not acceptance data; the approved text-only classification controls, labeled actions, Account switches, and selected-month values remain deliberate adaptations.

No integration defect was identified by the completed automated suite or screenshot spot-check. This ticket remains claimed because the planned single end-to-end Android journey (`operateRedesignedAppOnAndroid10`) is not yet present as one test, and the screenshot set does not yet include Transfer paired-list/Account-history screens or an empty/validation screenshot for each destination. Those scenarios have feature-slice behavioral coverage, but are not claimed as integration screenshot evidence here.

## Human review still needed

- Manually review TalkBack and Switch Access across drawer alternatives, form controls, and system picker navigation.
- Review focus movement, gesture discoverability, and touch targets on a physical Android 10 device.
- Confirm visual quality and reference differences with a human reviewer. The visual comparison above is an agent spot-check, not human sign-off.
