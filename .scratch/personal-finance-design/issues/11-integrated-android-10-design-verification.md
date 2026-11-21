# 11: Integrated Android 10 design verification

Status: claimed
Blocked by: 04, 05, 08, 09, 10

**What to build:** Review and verify the fully integrated redesigned app on Android 10, with reproducible visual evidence and accessible complete finance journeys, and resolve integration defects while retaining all previously delivered behavior coverage.

## Parent-spec requirements

- [User stories](../spec.md#user-stories): Stories 1-85: integration acceptance across the complete confirmed design, rather than a replacement for the feature tests owned by tickets 01-10.
- [User stories](../spec.md#user-stories): Stories 1-6, 83-85 in particular: shared visual language across every destination, accessible larger-font behavior, offline Android 10 operation, and representative screenshot evidence.
- [User stories](../spec.md#user-stories): Stories 76-80: upgrades/Backups must preserve a consistent complete finance dataset and the reviewed legacy compatibility policy through the final combined build.
- [Implementation decisions](../spec.md#implementation-decisions): Verify the composed feature through its existing public finance/Backup boundaries, Compose semantics, and UI Automator system flows. Compare screenshots with the references while respecting all deliberate adaptations; do not change financial rules merely to reproduce sample image values.
- [Testing decisions](../spec.md#testing-decisions): use existing public finance use-case/repository and Backup interfaces, Compose semantics, and UI Automator for system UI; test visible outcomes rather than private helpers or tables. Named tests below are proposed or extensions of existing behavior tests, not claims of passing implementation.

## Observable acceptance criteria

- [ ] Every parent-spec User Story has an implementing ticket and an observable test/review result. Earlier feature tests remain in their owning slices; this ticket checks their combined behavior rather than postponing missing core feature implementation.
- [ ] A single Android 10 journey completes setup, chooses Accounts, switches months/tabs, records and edits Income/Expense without unexpected Account moves, records/edits a Transfer, checks paired lists/history, creates overlapping filtered Budgets, edits them, and observes correct retrospective/current progress.
- [ ] The combined application retains persistent subset/empty drawer selection, archived Account selection/read-only history, consistent selected-month Closing balances in all views, and zero empty-selection ledger totals without a prompt. Budget progress remains independently scoped.
- [ ] Run public finance/Room/migration/Backup regression coverage alongside Android 10 UI tests, lint, and formatting. Verify the reviewed legacy mapping, complete custom-Budget round-trip, atomic Merge/Overwrite, picker cancellation, and reset confirmations through the integrated navigation.
- [ ] Collect representative screenshots for Overview, Expenses, Income, both entry directions, left drawer, right drawer with switches, Accounts/editor/history, Transfers, Budgets/editor, Categories & Tags, Settings, and setup. Include relevant empty, negative, archived, validation, and over-budget states without adding seeded production data.
- [ ] Compare hierarchy, spacing, typography, rounded cards, selected states, amount signs/colors, and bottom action placement against the supplied references. Treat text-only Categories, consistent labeled actions, Account switches, and selected-month data as intentional differences rather than regressions.
- [ ] Verify accessible open/close alternatives for the edge drawer, labels/selected states for controls, usable scrolling/touch targets, large fonts, and readable values without relying on color alone. Record manual TalkBack/Switch Access review separately from automated checks.
- [ ] Fix integration defects found within the confirmed scope, rerun the checks those fixes affect, and record test reports, screenshots, diagnostics, remaining environment limits, and any human visual/accessibility review still needed. Do not claim unrun checks passed.
- [ ] Confirm the final app remains native, light-only, offline, Android-10-capable, and free of unrequested profiles, network permission, search, Category icons, destructive migration, generated sample data, and extra Budget lifecycle behavior.

## Named behavior tests

| Test or journey | Observable result |
| --- | --- |
| operateRedesignedAppOnAndroid10 | Exercise the full multi-destination finance and data-management journey with asserted values and persisted state. |
| shareAccountSelectionAcrossTabsAndRelaunch | Re-run with the combined navigation/setup implementation, both empty and selected subsets, and app restarts. |
| calculateClosingBalancesAtSelectedMonthEnd | Re-run public boundary fixtures and compare the resulting displayed amounts across Overview, drawer, Account list, and history. |
| editTransactionWithoutUnexpectedAccountChange | Verify a non-primary Account survives edit/relaunch from the final row/form navigation. |
| presentTransfersWithoutInflatingTotals | Verify the combined entry, directional lists, Account history, and Budget views. |
| matchBudgetAcrossAccountCategoryAndTagSelections | Verify matching, overlap, all-month edits, and independent scope through the final UI and public reads. |
| migrateAndRestoreBudgetsWithoutDataLoss | Re-run upgrade and supported-Backup fixtures against the final combined model. |
| reconcileBudgetAsOneAtomicBackupRecord | Re-run complete-record Merge, Overwrite, rejection, cancellation, and failure coverage. |
| reviewReferenceScreensAndLargeFontAccessibility | Record screenshots and semantic assertions for every adapted destination, with manual review clearly distinguished. |

## Delivery boundaries

This is integrated verification and correction of the approved design, not authorization for new features, a release-signing/deployment task, or a substitute for per-slice tests. Keep original source specifications and previously resolved issues intact.

Each slice must remain independently demoable and keep existing callers and relevant checks working. Include the feature's behavior and UI verification in this ticket rather than deferring them to the final integration ticket. Follow the parent spec's exclusions and existing ADRs.

## Answer

Ran the integrated verification on the Android 10/API 29 `xperia_xz2_compact(AVD) - 10` emulator. Python host-runner tests passed 15/15; JVM tests, lint, ktlint formatting, and debug app/test APK assembly passed. The selected and explicitly empty Overview persistence runner passed after process restarts. The full unfiltered connected suite passed 96/96 with no failures, errors, or skips. The Android HTML report is `app/build/reports/androidTests/connected/debug/index.html`; restart evidence is `build/overview-persistence/20261120T214326577741Z/`.

No integration defect was found in these checks. The existing per-slice suite exercises setup, monthly navigation and Account scope, Income/Expense entry and edit, transfers, account history and archive, Budget filters/progress/migrations, and complete Backup journeys. Current run screenshots were visually spot-checked against the supplied Overview reference for the shared charcoal/yellow/pale-card visual language and the accepted adaptations. The curated screenshot bundle is [evidence/11](evidence/11/verification.md).

On 2026-11-21, added and passed the focused `IntegratedAndroid10Test.operateRedesignedAppOnAndroid10` journey on the same API 29 emulator. It starts at setup and exercises both Accounts, the three main tabs, Income/Expense editing, Transfer validation/editing and paired histories, two overlapping filtered Budgets and a limit edit, retrospective/current months, empty drawer selection, and Android document-picker cancellation. Added integrated Transfer list/history, validation, over-budget, empty-selection, and cancellation screenshots to [evidence/11](evidence/11/verification.md). The final completion-checklist run is still pending after this test addition.

This issue remains `claimed` pending the final checklist and recorded human TalkBack/Switch Access, physical-device usability, and visual sign-off. Automated semantics and screenshots do not establish those manual outcomes. Do not treat the ticket as resolved until those reviews are recorded.
