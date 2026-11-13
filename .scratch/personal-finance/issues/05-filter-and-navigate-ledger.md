# 05: Filter and navigate the ledger

**What to build:** A user can move through the main app areas and investigate Transactions using the agreed filters.

**Blocked by:** 03: Organize Transactions with Categories and Tags, 10: Transfer flows and activity

**Status:** ready-for-agent

## Parent-spec requirements

- User stories 28–33: show a current-month Overview; browse Transactions newest first; filter by month, Account, Category, and Tag; expose quick-add from main screens; and make Settings discoverable.
- User story 45: preserve the light visual system and readable amounts, statuses, and actions.
- User story 51: cover critical Compose navigation, filter, quick-add, and accessibility behavior.
- User stories 56–57: Transfers appear in the corresponding Income/Expense lists and are filterable by month and relevant Account; Category and Tag filters exclude Transfers. Transfer rows will be integrated when the Transfer flow is implemented.

## Observable acceptance criteria

- Navigation exposes Overview, Transactions, Budgets, and Settings; a Compose test asserts each destination can be opened and its selected navigation semantics are exposed.
- A quick-add action is reachable from Overview, Transactions, and Budgets; a Compose test opens the transaction form from each destination.
- Transactions can be filtered by month, Account, Category, and Tag; a Compose test verifies each filter changes displayed rows and clearing a filter restores rows.
- Filtered transactions remain newest first and show a specific empty state; a Compose test verifies ordering and empty-state copy.
- Filter controls, navigation destinations, and quick-add have accessible labels/actions; Compose tests assert those semantics.
- Settings exposes currency and a working data-management action; `navigateFilterTransactionsAndQuickAddFromEveryMainArea` opens Category/Tag management from Settings.
- Transfer rows appear in the corresponding Income/Expense lists and are filterable by month and relevant Account; Category and Tag filters exclude them. This remains pending issue 10, which adds the Transfer model and flows.

- [x] The app exposes Overview, Transactions, Budgets, and Settings areas.
- [x] A prominent quick-add action is available from the main screens.
- [x] Transactions can be filtered by month, Account, Category, and Tag.
- [x] Filtered results remain newest first and show correct empty states.
- [x] The light visual system keeps amounts, statuses, and actions readable.
- [x] Compose tests cover navigation, filters, quick-add entry, semantics, and important accessibility actions.
- [ ] Transfer rows participate in Transactions filtering according to user stories 56–57.

## Answer

Implemented bottom navigation for the four main areas, a dedicated Transactions screen with month/Account/Category/Tag filters, all-month browsing by default, newest-first results, and a specific no-match state. Quick-add and delete dialogs are shared at the app shell so entry works from Overview, Transactions, Budgets, and Settings. Settings shows the selected currency and account and links to Category/Tag management. Navigation exposes its current section through accessibility semantics.

Transfer rows are not part of the current persisted ledger model. The remaining transfer behavior is tracked in issue 10; the issue stays ready-for-agent until those rows can be integrated and tested. This ticket's transaction journey is covered by `navigateFilterTransactionsAndQuickAddFromEveryMainArea` in `TransactionFlowTest`.

Verification passed on Android 10: 22 instrumentation tests, JVM unit tests, and ktlint.
