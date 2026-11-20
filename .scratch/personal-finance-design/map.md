# Personal Finance Design Issues

Parent: [Personal Finance Design](spec.md)

The user approved this 11-ticket breakdown, including its sizes and blocking edges. Tickets were initially published as documentation only; issue 01 is implemented and has Android 10 UI screenshots and feature-specific tests, including process-relaunch, activity ordering, independent Budget, and accessibility/edit-restriction assertions.

## Implementation map

| Issue | Blocked by | Status |
| --- | --- | --- |
| [01: Account selection and month-end Overview](issues/01-account-selection-and-month-end-overview.md) | None | resolved |
| [02: Monthly Expenses, Income, and main navigation](issues/02-monthly-expenses-income-and-main-navigation.md) | 01 | resolved |
| [03: Full-screen Transaction entry and editing](issues/03-full-screen-transaction-entry-and-editing.md) | 01 | resolved |
| [04: Account management and monthly history](issues/04-account-management-and-monthly-history.md) | 01 | resolved |
| [05: Redesigned Transfer entry and editing](issues/05-redesigned-transfer-entry-and-editing.md) | 02, 03 | resolved |
| [06: Named recurring Budgets with selected Accounts](issues/06-named-recurring-budgets-with-selected-accounts.md) | 01 | resolved |
| [07: Category and Tag restrictions for Budgets](issues/07-category-and-tag-restrictions-for-budgets.md) | 06 | resolved |
| [08: Redesigned Category and Tag management](issues/08-redesigned-category-and-tag-management.md) | 01 | resolved |
| [09: Redesigned first-run setup](issues/09-redesigned-first-run-setup.md) | 01 | resolved |
| [10: Redesigned Settings and Backup journeys](issues/10-redesigned-settings-and-backup-journeys.md) | 02, 07 | ready-for-agent |
| [11: Integrated Android 10 design verification](issues/11-integrated-android-10-design-verification.md) | 04, 05, 08, 09, 10 | ready-for-agent |

The ready-for-agent status is a triage label. A ticket can be started only after every listed blocker is resolved.

## Work frontier

- 01, 02, 03, 04, and 05 are resolved.
- 07, 08, and 09 are resolved; 09 completes the first-run setup slice and leaves 11 waiting on 10.
- 03 is resolved with ticket-specific API 29 Compose coverage for transaction editing, Account recalculation, validation, optional classification, deletion and Budget recalculation, and larger text.
- 05 opened after both 02 and 03 resolved and is complete with ticket-specific API 29 creation, editing, paired-list, filter, deletion, archived-counterpart, and larger-font verification.
- 07 opened after 06 resolved and is now complete. Its completion opens issue 10 alongside its existing issue 02 prerequisite.
- 10 opens after both 02 and 07 resolve.
- 11 opens after 04, 05, 08, 09, and 10 resolve. Its other prerequisites are included transitively.

## Delivery notes

- Each feature slice includes public behavior, persisted state where applicable, UI integration, named tests, and visual evidence. The final verification ticket does not defer feature-level testing.
- Issue 01 has passing API 29 emulator journeys and screenshots for Overview, Account drawer, selected-account month navigation, and saved/empty selection after host-driven process restarts. The issue records the test and lint commands plus captured UI hierarchy evidence.
- Issue 02 has passing API 29 ledger/navigation journeys and screenshots for Expenses, Income, shared Account selection, empty selection, the drawer, and Overview actions. Its legacy journeys now use the split ledgers. The unfiltered full connected suite passed on 2026-11-17: 58 tests, zero failures/errors/skips, including all 16 `TransactionFlowTest` journeys. JVM tests, lint, formatting, and all 11 host-runner tests also pass.
- Issue 03 has passing focused API 29 journeys for preserved edit fields and identity, Account changes and monthly balances, amount/date validation, missing active Account, optional Category/Tag/Note, cancellation, deletion, and large-font operation. Screenshots for both transaction directions remain in `evidence/03/`.
- Issue 04 has passing full API 29 verification: 60 connected tests, 0 failures/errors/skips, plus JVM tests, lint, and formatting. Account management/history covers typed creation, signed Opening balance edits and reopen persistence, month-end balances, rename resolution, archived read-only history including archived transfer counterparts, drawer selection, month retention, and Android 10 screenshots in `evidence/04/`.
- Issue 05 has passing full API 29 verification: 59 connected tests, 0 failures/errors/skips, plus JVM tests, lint, formatting, and all 11 Python host-runner tests. The Transfer journey covers the full-screen amount/date form, explicit Accounts, paired rows and histories, filters/totals, cross-Account/month editing with stable identity/timestamps, deletion, archived-counterpart restrictions, and larger text. Screenshots are in `issues/evidence/05/transfer-entry-evidence/`; selected and empty Overview restart checks passed with evidence in `build/overview-persistence/20261118T133519187731Z`.
- The legacy failure records in issues 02 and 03 are historical and superseded by issue 02's passing full-suite verification. They are no longer deferred to integration; CI retains its full connected-suite gate, and ticket implementation owns the final local [completion checklist](../../docs/research/ai-ui-testing.md#ticket-completion).
- Saved/empty Overview selection is also checked by the reusable [host restart runner](../../scripts/verify_overview_persistence.py), including a second unselected Account, displayed totals/activity, and both drawer switches. The runner now scopes Income/Expenses labels to the financial summary to allow duplicate navigation-tab labels. Both selected and empty restart checks passed on 2026-11-17. CI runs it before the connected suite and uploads each run's evidence.
- The first slice introduces shared month/projection behavior and visual components inside a working Overview/Account-selection journey; later slices reuse them rather than requiring a standalone broad refactor.
- Budget tickets 06-07 include their own schema, migration, validation, and complete-record Backup paths. Ticket 10 delivers the styled document-management journey over those working contracts.
- Issue 06 resolved the legacy Budget Account-scope choice by mapping each old monthly record to every Account present at upgrade, preserving prior totals and keeping later Accounts outside the saved scope. Version 1 and 2 Backups use the same mapping.
- Issue 06 is complete with named recurring Budgets, explicit Account scopes, migration and versioned Backup compatibility, passing API 29/JVM/host-runner checks, and screenshots in `issues/evidence/06/`.
- Issue 07 is complete with optional Category and Tag filters, shared matching across Budgets and Overview, migration/reopen and complete-record Backup compatibility, and accessible large-font journeys. On 2026-11-19, host-runner tests passed 15/15, JVM tests passed 15/15, the API 29 Overview restart checks passed for selected and empty states, and the unfiltered connected suite passed 79/79. Screenshots and run details are recorded in issue 07 under `issues/evidence/07/` and `build/overview-persistence/20261119T111721533634Z/`.
- Issue 08 is complete with the text-only Category and Tag manager, stable-reference rename/archive behavior, preserved archived Budget restrictions, and refreshed entry/filter choices. On 2026-11-19, host-runner tests passed 15/15, Gradle JVM/lint/format/assembly checks passed, selected and empty Overview restart checks passed, and the full API 29 connected suite passed 85/85. Screenshots and logs are recorded in `issues/evidence/08/`; restart evidence is under `build/overview-persistence/20261119T174310562546Z/`.
- Issue 09 is complete with the redesigned accessible first-run flow, saved unsubmitted choices, retry/loading/saving feedback, the empty current-month Overview, and large-font layouts. On 2026-11-20, Python host tests passed 15/15, Gradle JVM/lint/format/assembly checks passed, selected and empty Overview process-restart checks passed, the large-font first-run journeys passed 4/4, and the full unfiltered API 29 suite passed 92/92 with no failures, errors, or skips. Screenshots, logs, and the connected XML report are recorded in `issues/evidence/09/`; restart evidence is under `build/overview-persistence/20261120T130844944098Z/`.
- Preserve older supported Backups, stable identities, renamed/archived references, and the existing atomic-import ADR throughout the staged implementation.
- Keep source specifications and earlier resolved issues intact. On future completion, add the ticket's Answer and verification evidence, mark it resolved, and update this map only when its criteria and required checks pass.

## Parent-spec coverage

Numbers reference the parent [User Stories](spec.md#user-stories). Shared requirements intentionally recur across slices. Primary feature tickets 01-10 collectively cover all 85 stories; ticket 11 verifies their integrated behavior.

| Ticket | Applicable User Stories |
| --- | --- |
| 01 | 1-5, 9-20, 22-32, 39, 75, 83-85 |
| 02 | 1-5, 7-12, 21, 29-30, 33-41, 83-85 |
| 03 | 1-6, 41-52, 55, 83-85 |
| 04 | 1-6, 11-12, 22, 24-28, 55, 75-76, 83-85 |
| 05 | 1-6, 37-41, 52-55, 83-85 |
| 06 | 1-6, 9-12, 31-32, 39, 56-60, 63-64, 67-80, 83-85 |
| 07 | 1-6, 31-32, 39, 56-80, 83-85 |
| 08 | 1-6, 36, 49-51, 75, 83-85 |
| 09 | 1-6, 9, 16-20, 81, 83-85 |
| 10 | 1-7, 75-80, 82-85 |
| 11 | 1-85, integrated verification of the completed feature slices |

All ticket files also link the parent's [Implementation Decisions](spec.md#implementation-decisions) and [Testing Decisions](spec.md#testing-decisions), identify their observable criteria, and name the public behavior/UI tests that must verify them. These are planned tests and acceptance criteria, not claims of completed implementation.
