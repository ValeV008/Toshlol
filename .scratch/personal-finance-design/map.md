# Personal Finance Design Issues

Parent: [Personal Finance Design](spec.md)

The user approved this 11-ticket breakdown, including its sizes and blocking edges. Tickets were initially published as documentation only; issue 01 is implemented and has Android 10 UI screenshots and feature-specific tests, including process-relaunch, activity ordering, independent Budget, and accessibility/edit-restriction assertions.

## Implementation map

| Issue | Blocked by | Status |
| --- | --- | --- |
| [01: Account selection and month-end Overview](issues/01-account-selection-and-month-end-overview.md) | None | resolved |
| [02: Monthly Expenses, Income, and main navigation](issues/02-monthly-expenses-income-and-main-navigation.md) | 01 | resolved |
| [03: Full-screen Transaction entry and editing](issues/03-full-screen-transaction-entry-and-editing.md) | 01 | ready-for-agent |
| [04: Account management and monthly history](issues/04-account-management-and-monthly-history.md) | 01 | ready-for-agent |
| [05: Redesigned Transfer entry and editing](issues/05-redesigned-transfer-entry-and-editing.md) | 02, 03 | ready-for-agent |
| [06: Named recurring Budgets with selected Accounts](issues/06-named-recurring-budgets-with-selected-accounts.md) | 01 | ready-for-agent |
| [07: Category and Tag restrictions for Budgets](issues/07-category-and-tag-restrictions-for-budgets.md) | 06 | ready-for-agent |
| [08: Redesigned Category and Tag management](issues/08-redesigned-category-and-tag-management.md) | 01 | ready-for-agent |
| [09: Redesigned first-run setup](issues/09-redesigned-first-run-setup.md) | 01 | ready-for-agent |
| [10: Redesigned Settings and Backup journeys](issues/10-redesigned-settings-and-backup-journeys.md) | 02, 07 | ready-for-agent |
| [11: Integrated Android 10 design verification](issues/11-integrated-android-10-design-verification.md) | 04, 05, 08, 09, 10 | ready-for-agent |

The ready-for-agent status is a triage label. A ticket can be started only after every listed blocker is resolved.

## Work frontier

- 01 and 02 are resolved.
- 03, 04, 06, 08, and 09 can proceed without waiting for each other.
- 05 opens after both 02 and 03 resolve.
- 07 opens after 06 resolves.
- 10 opens after both 02 and 07 resolve.
- 11 opens after 04, 05, 08, 09, and 10 resolve. Its other prerequisites are included transitively.

## Delivery notes

- Each feature slice includes public behavior, persisted state where applicable, UI integration, named tests, and visual evidence. The final verification ticket does not defer feature-level testing.
- Issue 01 has passing API 29 emulator journeys and screenshots for Overview, Account drawer, selected-account month navigation, and saved/empty selection after host-driven process restarts. The issue records the test and lint commands plus captured UI hierarchy evidence.
- Issue 02 has passing focused API 29 ledger/navigation journeys and screenshots for Expenses, Income, shared Account selection, empty selection, the drawer, and Overview actions. Nine legacy `TransactionFlowTest` journeys still expect the replaced combined Transactions UI and need reconciliation during integration.
- Saved/empty Overview selection is also checked by the reusable [host restart runner](../../scripts/verify_overview_persistence.py), including a second unselected Account, displayed totals/activity, and both drawer switches. CI runs it before the connected suite and uploads each run's evidence.
- The first slice introduces shared month/projection behavior and visual components inside a working Overview/Account-selection journey; later slices reuse them rather than requiring a standalone broad refactor.
- Budget tickets 06-07 include their own schema, migration, validation, and complete-record Backup paths. Ticket 10 delivers the styled document-management journey over those working contracts.
- Before legacy Budget schema/data mutation, 06 must document a deterministic lossless mapping and resolve any product-dependent missing Account-scope choice from the parent spec. The approved breakdown does not invent that mapping or authorize data loss.
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
