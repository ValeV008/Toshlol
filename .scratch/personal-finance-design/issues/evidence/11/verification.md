# Integrated Android 10 verification

Device: `xperia_xz2_compact(AVD) - 10`, Android 10/API 29, debug build. Final run: 2026-11-21.

## Automated checks

- `python -m unittest discover -s scripts -p 'test_*.py'`: 15/15 passed.
- `:app:testDebugUnitTest :app:lintDebug :app:ktlintCheck :app:assembleDebug :app:assembleDebugAndroidTest`: passed.
- `python scripts/verify_overview_persistence.py --serial emulator-5554 --skip-build`: selected and explicitly empty Account sets persisted after host-driven process restarts. Evidence: `build/overview-persistence/20261121T175946733475Z/`.
- Focused `IntegratedAndroid10Test.operateRedesignedAppOnAndroid10`: 1/1 passed on API 29. Final unfiltered `:app:connectedDebugAndroidTest`: 97/97 passed, zero failures or skips. Report: `app/build/reports/androidTests/connected/debug/index.html`; XML: `app/build/outputs/androidTest-results/connected/debug/`.

The integrated journey starts at first-run setup, creates two Accounts and a Category and Tag, records and edits Income and Expense, validates and edits a Transfer, verifies both directional lists and Account histories, creates overlapping Category- and Tag-filtered Budgets, edits a limit, switches months, clears the drawer selection, and asserts zero ledger totals while Budget progress remains independently scoped. After restarting at 1.3× font scale, it checks persisted empty selection and Budget progress, completes an Android document-picker Export, cancels a second Export and a Merge import, and cancels reset confirmation. Existing owning tests cover archive/history read-only behavior, Account preservation on transaction edit, selected-month Closing balances, supported migration, atomic Backup Merge/Overwrite, and full custom-Budget round trips.

## Visual evidence and correction

Curated PNGs in this directory cover setup, Overview, Expenses, Income, both entry directions, both drawers, Accounts/editor/history, paired Transfer lists and histories, Budgets/editor, Categories & Tags, and Settings. New integrated captures include `transfer-expenses-paired.png`, `transfer-income-paired.png`, `transfer-cash-history.png`, `transfer-reserve-history.png`, `transfer-validation.png`, `budget-over-limit-integrated.png`, `drawer-empty-selection-integrated.png`, `overview-empty-selection-with-budget.png`, `categories-tags-integrated.png`, `settings-export-completed.png`, and `settings-export-canceled.png`.

The 1.3× `overview-restarted-large-font.png` and `expenses-restarted-large-font.png` captures revealed clipped navigation/action labels and a squeezed Tag filter in the first run. The app now switches to a single current-area title and stacked Overview and ledger actions at 1.2×, and stacks ledger filters at that scale. The final focused journey passed and the new captures show readable full labels and amounts. Earlier failed test-navigation and synchronization artifacts are preserved under `build/integrated-test-baseline-01/` through `-04/`.

Screens were spot-checked against `UI_pics_old/overview.jpg` and the relevant supplied references for hierarchy, spacing, typography, rounded cards, yellow selected/actions, charcoal headers, pale background, signed red/green amounts, and bottom action placement. Text-only classification controls, labeled actions, Account switches, and selected-month data are intentional adaptations; reference sample amounts are not acceptance values. This is an agent visual check, not human sign-off.

## Manual review disposition

- TalkBack and Switch Access will not be tested, per the user on 2026-11-21. No manual accessibility pass is claimed for those services.
- Focus movement, gesture discoverability, scrolling, and touch targets on a physical Android 10 device.
- Human visual sign-off on the curated screenshots and intentional reference adaptations.
