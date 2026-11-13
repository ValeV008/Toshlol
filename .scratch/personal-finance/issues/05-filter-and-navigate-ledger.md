# 05: Filter and navigate the ledger

**What to build:** A user can move through the main app areas and investigate Transactions using the agreed filters.

**Blocked by:** 03: Organize Transactions with Categories and Tags, 10: Transfer flows and activity

**Status:** resolved

## Parent-spec requirements

- User stories 28â€“33: show a current-month Overview; browse Transactions newest first; filter by month, Account, Category, and Tag; expose quick-add from main screens; and make Settings discoverable.
- User story 45: preserve the light visual system and readable amounts, statuses, and actions.
- User story 51: cover critical Compose navigation, filter, quick-add, and accessibility behavior.
- User stories 56â€“57: Transfers appear in the corresponding Income/Expense lists and are filterable by month and relevant Account; Category and Tag filters exclude Transfers. Transfer rows follow the filtering behavior defined in the resolved Transfer issue.

## Observable acceptance criteria

- Navigation exposes Overview, Transactions, Budgets, and Settings; a Compose test asserts each destination can be opened and its selected navigation semantics are exposed.
- A quick-add action is reachable from Overview, Transactions, and Budgets; a Compose test opens the transaction form from each destination.
- Transactions can be filtered by month, Account, Category, and Tag; a Compose test verifies each filter changes displayed rows and clearing a filter restores rows.
- Filtered transactions remain newest first and show a specific empty state; a Compose test verifies ordering and empty-state copy.
- Filter controls, navigation destinations, and quick-add have accessible labels/actions; Compose tests assert those semantics.
- Settings exposes currency and a working data-management action; `navigateFilterTransactionsAndQuickAddFromEveryMainArea` opens Category/Tag management from Settings.
- Transfer rows appear in the corresponding Income/Expense lists and are filterable by month and relevant Account; Category and Tag filters exclude them. The resolved Transfer issue adds the Transfer model and flows.

- [x] The app exposes Overview, Transactions, Budgets, and Settings areas.
- [x] A prominent quick-add action is available from the main screens.
- [x] Transactions can be filtered by month, Account, Category, and Tag.
- [x] Filtered results remain newest first and show correct empty states.
- [x] The light visual system keeps amounts, statuses, and actions readable.
- [x] Compose tests cover navigation, filters, quick-add entry, semantics, and important accessibility actions.
- [x] Transfer rows participate in Transactions filtering according to user stories 56–57.

## Answer

Implemented bottom navigation for the four main areas, a dedicated Transactions screen with month/Account/Category/Tag filters, all-month browsing by default, newest-first results, and a specific no-match state. Quick-add and delete dialogs are shared at the app shell so entry works from Overview, Transactions, Budgets, and Settings. Settings shows the selected currency and account and links to Category/Tag management. Navigation exposes its current section through accessibility semantics.

Transfer rows participate in the Transactions list as outgoing Expense and incoming Income rows. Month and relevant Account filters include them; Category and Tag filters hide them. Both rows open the same Transfer. This behavior is covered by `createEditFilterAndDeleteTransferWithoutChangingIncomeOrExpenseTotals` in `TransactionFlowTest`. The navigation, transaction filter, empty-state, quick-add, and Settings journey is covered by `navigateFilterTransactionsAndQuickAddFromEveryMainArea` in `TransactionFlowTest`.

Verification was recorded with the implementation: JVM unit tests, lint, ktlint, Android 10 instrumentation tests, and `assembleDebug` passed. Issue 10 records the current full verification results, including all 24 Android 10 instrumentation tests.
