# 04: Account management and monthly history

Status: ready-for-agent
Blocked by: 01

**What to build:** Review and manage Accounts in the shared visual style, with selected-month Closing balances and movement histories consistent with Overview and the Account drawer, while preserving signed Opening balances, renames, and archived read-only history.

## Parent-spec requirements

- [User stories](../spec.md#user-stories): Stories 1-6, 83-85: extend reference styling to Account destinations without a dedicated mockup, preserving accessibility and offline Android 10 use.
- [User stories](../spec.md#user-stories): Stories 11-12, 22, 24-28: shared month, consistent Closing balances, selected archived Accounts, signed values, and Opening/date boundaries.
- [User stories](../spec.md#user-stories): Stories 55, 75-76: keep archived history/read-only restrictions, resolve renamed references, and preserve existing finance records.
- [Implementation decisions](../spec.md#implementation-decisions): Consume ticket 01's public monthly projection and shared period rather than a separate all-time/current-balance formula. Main drawer selection scopes the three ledger tabs; Account management must still expose all Accounts for management and explicit history viewing.
- [Testing decisions](../spec.md#testing-decisions): use existing public finance use-case/repository and Backup interfaces, Compose semantics, and UI Automator for system UI; test visible outcomes rather than private helpers or tables. Named tests below are proposed or extensions of existing behavior tests, not claims of passing implementation.

## Observable acceptance criteria

- [ ] The Accounts destination and Account creation/edit controls use the shared headers, cards, typography, money formatting, and accessible actions while retaining name, type, Opening balance, and Opening date management.
- [ ] Account list/card/history balances agree with Overview/right-drawer values for the same Account and selected month. They support positive, zero, and negative values and exclude later movements and premature Opening balances.
- [ ] Opening an Account history retains the selected month and shows that month's relevant Income, Expenses, and Transfers in newest-first deterministic order with clear counterpart Account names. History's Closing balance remains cumulative through month-end rather than just the monthly net.
- [ ] Changing month from the available shared control refreshes Account balances/history consistently; navigating back does not silently return to the device's current month.
- [ ] Create an Account, edit its signed Opening balance/date, and rename it through the public workflow. The persisted values and recalculated displays survive reopen, and renames resolve in already available historical views without rewriting movement identities.
- [ ] Archive retains Account name, balance, and history and marks the Account read-only. The drawer can still select it, but new use and prohibited Transaction/Transfer edits remain unavailable.
- [ ] Creating or managing an Account does not silently select it in the main drawer, and does not add it to any saved Budget Account scope. Preserve existing references and legacy finance data throughout.
- [ ] Verify Account creation/edit/archive/history journeys and repository persistence/boundary behavior on Android 10, run relevant JVM/lint/format checks, and capture Account list/editor/history screenshots including negative balance and archived examples.

## Named behavior tests

| Test or journey | Observable result |
| --- | --- |
| reviewAccountClosingBalanceAndMonthlyHistory | Compare Account list, history, drawer, and Overview for the same viewed month, including prior-month accumulated funds and excluded later records. |
| saveOpeningBalanceAndDateRecalculatesPersistedBalance | Extend the existing public repository pattern with month-end and Opening-date boundary results after reopen. |
| renameAccountUpdatesHistoricalReferences | Assert displayed counterpart/Account names update while movement identities and amounts remain stable. |
| selectArchivedAccountWithoutEnablingEdits | Assert selectable historical balance/activity and enforced read-only behavior after archive. |
| createAccountWithoutChangingSavedScopes | Assert a new Account is not auto-selected in the main drawer and does not expand existing Budget scopes when those are available. |
| operateAccountManagementAtLargeFont | Exercise labeled creation/edit/history/archive actions, scrolling, and readable signed values. |

## Delivery boundaries

Keep all existing Account types and signed Opening balance semantics. Account history is an explicit Account view, not an implicit rewrite of the shared main-ledger Account selection. No bank integration, seeded Accounts, or new deletion policy.

Each slice must remain independently demoable and keep existing callers and relevant checks working. Include the feature's behavior and UI verification in this ticket rather than deferring them to the final integration ticket. Follow the parent spec's exclusions and existing ADRs.
