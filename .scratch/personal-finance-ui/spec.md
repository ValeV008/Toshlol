# Personal Finance UI

**Status:** ready-for-agent

## Problem Statement

A person needs a clear, quick way to record and understand their own finances. The supplied UI designs show monthly Income and Expense lists, entry forms, and navigation, but the app also needs consistent screens for Accounts and their balances, Transfers, Categories and Tags, Budgets, and data management. The interface must make internal money movements distinguishable from earnings and spending, while keeping the ledger useful offline and faithful to the user's records.

## Solution

Build the personal-finance app UI around the supplied light-theme designs. Provide navigation for Overview, Expenses, Income, Accounts, Categories & Tags, Budgets, Export, and Settings. The Income and Expense forms record the same Transaction concept with different directions; a separate Transfer flow moves money between two Accounts.

Show selected-calendar-month Income and Expense lists and totals, Account balances and histories, Budget progress, and recent activity. Transfers appear in the relevant Account histories and in context on both Income and Expense lists, but never count as Income, Expense, or Budget spending. Preserve the existing native Android, local-first, backup, and privacy decisions in the personal-finance MVP.

## User Stories

1. As a new user, I want to choose one currency and create my first Account, so that I can start with a usable, consistently denominated ledger.
2. As a new user, I want the app to start without seeded Accounts or Categories, so that I can organize my finances using my own data.
3. As a user, I want a main navigation menu for Overview, Expenses, Income, Accounts, Categories & Tags, Budgets, Export, and Settings, so that every major finance task has a discoverable destination.
4. As a user, I want the current month shown by default on Overview, Income, Expense, and Budget screens, so that I immediately see the most relevant period.
5. As a user, I want to move to another calendar month, so that I can review historical or future monthly information.
6. As a user, I want amounts displayed using the selected currency and a readable, consistent number format, so that I can understand financial values at a glance.
7. As a user, I want the Overview to show current-month Income, Expense, net (Income minus Expense), Budget progress, and recent Transactions and Transfers, so that I can scan my finances without conflating internal movements with earnings or spending.
8. As a user, I want recent activity ordered newest first, so that the latest movements are easy to find.
9. As a user, I want a prominent quick-add action on main screens, so that I can begin recording a movement without navigating through several pages.
10. As a user, I want quick-add to offer Income, Expense, and Transfer, so that each movement is recorded with its correct meaning.
11. As a user, I want Income and Expense to use parallel entry flows, so that the form feels familiar whichever kind of Transaction I am recording.
12. As a user, I want to enter a positive amount, optional Note, date, and mandatory Account for a Transaction, so that each Income or Expense has the information needed for the ledger.
13. As a user, I want the Transaction date to default to the device's local calendar date and remain editable, so that entry is quick while historical corrections are possible.
14. As a user, I want the most recently used Account preselected across Income and Expense entry, so that I can record routine movements quickly.
15. As a user, I want the Category to be optional, so that I can save a Transaction before deciding how to classify it.
16. As a user, I want Income and Expense Categories kept in separate sets, so that an Income classification cannot be applied to spending or vice versa.
17. As a user, I want to choose multiple optional Tags during Income or Expense entry, so that I can add cross-cutting context when useful.
18. As a user, I want a Category-associated Tag offered only when its associated Category is selected, so that invalid Tag/Category combinations cannot be created.
19. As a user, I want changing or clearing a Transaction's Category to automatically remove Tags associated with its previous Category, so that saved classification remains valid.
20. As a user, I want the form's description control to represent the optional Note, so that I do not have to enter duplicate free-text fields.
21. As a user, I want to edit an Income or Expense and delete it only after confirmation, so that mistakes can be corrected without accidental data loss.
22. As a user, I want Income and Expense screens to show the total for the selected calendar month, so that their headline numbers match the visible period.
23. As a user, I want to browse Income and Expense newest first, so that recent entries are easy to find.
24. As a user, I want to filter Income and Expense lists by month, Account, Category, and Tag, so that I can investigate selected activity.
25. As a user, I want Transfer rows hidden when a Category or Tag filter is active, so that filters do not imply Transfers have classifications they do not support.
26. As a user, I want a Transfer row included when its month or relevant Account matches the active filters, so that I can find movements involving that Account in the period.
27. As a user, I want to create a Transfer by entering a positive amount, date, source Account, and destination Account, so that moving money does not masquerade as Income or Expense.
28. As a user, I want to choose both Transfer Accounts explicitly each time, so that the direction of movement is deliberate.
29. As a user, I want Transfers to require two different Accounts, so that a movement cannot be recorded as a no-op.
30. As a user, I want the Transfer date to default to today but remain editable, so that routine entry is quick and backdated moves can be recorded.
31. As a user, I want Transfers to have no Category, Tag, or Note, so that the Transfer flow remains distinct from Income and Expense classification.
32. As a user, I want a Transfer to be allowed even if it makes the source Account negative, so that the ledger records what happened instead of pretending to enforce a bank's available funds.
33. As a user, I want an outgoing Transfer row in Expenses and an incoming Transfer row in Income, so that the movement is visible from both directional lists without changing their totals.
34. As a user, I want both Transfer rows to open the same Transfer, so that editing or deleting one movement updates both list views together.
35. As a user, I want each Transfer shown in both affected Account histories and in Overview recent activity, so that its effect can be traced from either Account and from the dashboard.
36. As a user, I want Transfer amounts excluded from Income totals, Expense totals, monthly net, and Budget actual spending, so that internal movements do not inflate financial summaries.
37. As a user, I want to see all Accounts with their names, types, and signed current balances, so that I can understand how money is distributed.
38. As a user, I want to create Accounts with a name and a type such as Cash, Bank, Credit Card, or Other, so that each Account has useful context without a provider connection.
39. As a user, I want an optional signed Opening balance, defaulting to zero, and an as-of date defaulting to today when creating an Account, so that funds already held can be represented without recording them as Income.
40. As a user, I want Opening balances entered as signed amounts, including negative values, so that a Credit Card debt can be represented as a negative balance directly.
41. As a user, I want to edit an Account's Opening balance and as-of date, so that I can correct the starting point used to calculate its balance.
42. As a user, I want an Opening balance to represent the Account amount at the start of its as-of date, so that only movements dated on or after that date affect the current balance.
43. As a user, I want Income to increase an Account balance, Expense to decrease it, and Transfer to decrease its source and increase its destination, so that balances reflect all recorded movements consistently.
44. As a user, I want positive, zero, and negative balances allowed for every Account type, so that overdrafts, card debt, and card overpayments can all be represented.
45. As a user, I want to see an Account's movement history, so that its current balance can be understood from the movements that affect it.
46. As a user, I want to archive an Account while retaining its displayed name, balance, and history as read-only, so that past records remain intelligible without allowing new use.
47. As a user, I want to create, rename, and archive Categories and Tags, so that I can maintain classifications as my needs change.
48. As a user, I want active Account, Category, and Tag names to be trimmed and case-insensitively unique within their scope, so that duplicate concepts do not accumulate.
49. As a user, I want renaming an Account, Category, or Tag to update how it is shown in historical records, so that history remains readable without rewriting each record.
50. As a user, I want archived Categories and Tags to remain visible on historical records but unavailable for new use, so that archiving preserves history.
51. As a user, I want to create monthly Budgets only for Expense Categories, so that spending limits apply to the intended kind of Category.
52. As a user, I want a separate Budgets destination in the main menu, so that monthly limits are easy to find.
53. As a user, I want at most one active Budget per Expense Category per calendar month, so that each progress measure has one clear limit.
54. As a user, I want to see actual spending, remaining amount, and over-budget state for each Budget, so that I can understand my position quickly.
55. As a user, I want Budgets not to roll over and not to include Transfers, so that monthly spending progress has a stable meaning.
56. As a user, I want historical Budgets for archived Categories to remain visible, so that past reports remain truthful.
57. As a user, I want financial data to remain available offline, so that core UI flows do not depend on a network connection.
58. As a user, I want to export a versioned JSON Backup through Android's document picker, so that I can preserve my local finance dataset in a destination I choose.
59. As a user, I want the Backup to include Accounts, signed Opening balances and dates, Categories, Tags, Transactions, Transfers, Budgets, currency, IDs, timestamps, and schema version, so that restoring it preserves the ledger and balances.
60. As a user, I want an older Backup without Transfer or Opening balance data to import with no Transfers and zero Opening balances, so that older data remains usable; its Opening balance date uses the oldest movement date, or import date if there are no movements.
61. As a user, I want a malformed or unsupported Backup rejected before local data changes, so that invalid data cannot corrupt my ledger.
62. As a user, I want to choose Merge or Overwrite when importing a Backup, so that I control whether local data is combined or replaced.
63. As a user, I want Merge to add missing records and apply newer complete records by stable ID and `updatedAt`, without propagating deletions, so that repeated imports do not duplicate data or unexpectedly erase history.
64. As a user, I want invalid or conflicting imported finance data rejected as a whole, so that the app does not silently invent conflict resolution.
65. As a user, I want Overwrite to replace the complete local dataset only after explicit confirmation, so that a Backup can become authoritative deliberately.
66. As a user, I want to delete all local data from Settings only after explicit confirmation, so that I can intentionally reset the app.
67. As a user, I want Import and Overwrite to leave the prior complete dataset visible until the validated replacement commits, so that I never see partial finance data.
68. As a user, I want the interface to provide readable amounts, clear status colors, and accessible labels and actions, so that the screens are usable with accessibility services as well as visually.

## Implementation Decisions

- The app is a native Android application targeting Android 10 and newer, built with Kotlin, Jetpack Compose, and Material 3.
- Use a light-theme visual system based on the supplied designs. Preserve their monthly Income and Expense list layouts, transaction cards, add controls, entry amount keypad, and navigation drawer while adding the agreed Transfer, Account balance, and Budget interactions.
- The app is for one local user and stores finance data on device. It requests no network permission and provides no bank connection or continuous synchronization.
- A Transaction is one Income or Expense movement with a positive amount, local calendar date, mandatory Account, optional Category, optional Note, optional Tags, stable ID, and timestamps.
- A Transfer is one positive-amount movement between distinct source and destination Accounts, with a local calendar date, stable ID, and timestamps. It has no Category, Tags, or Note.
- Income and Expense entry share one most-recently-used Account selection. Transfers require explicit selection of both Accounts.
- Income and Expense Categories are disjoint sets. A Category-associated Tag is assignable only to a Transaction using that Category; changing or clearing the Category silently removes Tags associated with the old Category.
- Each Account has an optional signed Opening balance defaulting to zero and an as-of local date defaulting to today. The Opening balance is the amount at the start of that date. Opening balance and date remain editable.
- Account balances add Income, subtract Expense, subtract Transfers from the source, and add Transfers to the destination, for movements on or after the Opening balance date. The app allows any Account type to have either sign and allows Transfers to overdraw an Account.
- Transfer is a third quick-add flow. It is represented once in the domain but rendered as an outgoing Expense-list row and incoming Income-list row. Both rows open the same Transfer. It is also rendered in both affected Account histories and Overview activity.
- Income and Expense list totals and monthly net are calculated from Income and Expense Transactions only. Budget actual spending includes Expense Transactions only; Transfer rows never contribute.
- Income and Expense lists show selected calendar-month totals and support month, Account, Category, and Tag filters. Transfer rows participate in month and relevant Account filtering and are hidden under Category or Tag filtering.
- Budgets are monthly limits for Expense Categories and have their own navigation destination.
- Stable IDs and Room persistence remain the source of truth. Export/import is explicit, manual, versioned JSON through Android's document picker. Backup import uses the existing single module for full validation and atomic Merge or Overwrite.
- Backups include Transfers and Account Opening balances/dates. Older Backups normalize with no Transfers and zero Opening balances; use each Account's oldest included movement date, or import date if there are no movements, as its Opening balance date.
- Keep first-run setup explicit: users choose a currency and create at least one Account; do not seed Accounts or Categories.
- Monetary values use precise minor-unit storage rather than floating-point arithmetic. Transaction, Transfer, and Budget amounts are positive; Opening balances may be signed. All values are limited to two decimal places for the selected currency.
- No source implementation is currently present in the checkout; implementation should follow the existing architecture decisions and build vertical slices rather than infer hidden behavior from the screenshots.

## Testing Decisions

- Test externally observable behavior through the highest useful seam. Avoid assertions on private methods, persistence tables, or UI implementation details.
- The primary seam is one high-level Finance module interface for recording and editing Transactions and Transfers, maintaining Categories and Tags, managing Accounts and balances, computing monthly summaries and Budgets, and importing/exporting finance data. Exercise its public behavior with an in-memory adapter and deterministic time and ID providers.
- Cover validation and calculation scenarios through that Finance interface: required Account, positive transaction amounts, optional Category, direction-specific Categories, Tag/Category compatibility, automatic removal of mismatched Tags, opening-balance day boundaries, signed Account balances, overdraft Transfers, same-Account Transfer rejection, totals excluding Transfers, and monthly Budget boundaries.
- Verify Room persistence and migrations through repository operations against an in-memory database, including persistence of Transfers and Opening balances/dates.
- Compose tests should exercise user journeys through accessible semantics: first-run setup, Income and Expense entry, optional Category and Tags, Transfer quick-add and paired list presentation, Account balance/history, Budget navigation and display, filters, and validation.
- Compare representative Compose screens against the supplied visual references using screenshot evidence where the Android screenshot-test tooling is available. Treat screenshot differences as evidence for review, not as a replacement for behavior tests.
- UI Automator should cover Android document-picker flows for Backup export/import. Use Espresso only if a View-based or hybrid screen is introduced.
- Backup tests should cover older-schema normalization, Transfers, Opening balances/dates, malformed input, unsupported versions, self-contained references, Merge idempotency, currency mismatch, timestamp conflicts, atomic Overwrite, cancellation before commit, and rollback on storage failure.
- Accessibility tests should assert semantics, labels, actions, and important state; manual TalkBack and Switch Access review remains necessary.
- Prior art: the existing finance MVP spec defines public application/use-case, Room repository, Compose, UI Automator, migration, and Backup test seams. The Android UI testing research documents semantics-driven Compose testing, system document-picker testing, screenshot evidence, and human accessibility review. No source tests currently exist in this checkout.

## Out of Scope

- Bank or card provider integrations and automatic synchronization.
- Cloud accounts, backend services, background uploads, or shared users.
- Investment tracking, recurring Transactions, or scheduled rules.
- Nested Categories, Category or Tag defaults, and Income Budgets.
- Budget rollover, forecasting, advanced reporting, and free-text Transaction search.
- Category or Tag assignment to Transfers, and Transfer Notes.
- Direct Google Drive API integration; use Android's document picker.
- Dark theme, app lock, biometric protection, encrypted or password-protected Backups.
- Play Store publishing, App Bundle distribution, and automated store administration.
- Automatically propagating deletions during Backup Merge.

## Further Notes

The supplied screenshots are design references for the navigation drawer, monthly Income and Expense lists, and Income and Expense entry forms. The agreed Transfer, Account-balance, and Budget screens extend those references. Keep product vocabulary aligned with the Personal Finance glossary and retain the existing architecture and Transfer decisions recorded for this app.
