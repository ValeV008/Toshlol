# Personal Finance MVP

**Status:** ready-for-agent

## Problem Statement

A person needs a private, dependable way to track personal income and expenses, understand monthly spending against budgets, and preserve their finance data without depending on a bank integration, account system, or cloud backend. The app must make the core ledger trustworthy while keeping data ownership local and allowing deliberate backup through a user-selected storage provider such as Google Drive.

## Solution

Build a native Android personal finance app for Android 10 and newer. The app stores one user's finance data locally, requires a user-created Account for every Transaction, allows users to create their own Categories and Tags, provides monthly Category Budgets, and presents an Overview of the current month. It also records Transfers between the user's Accounts and shows signed Account balances.

The app supports manual versioned JSON Backup export and import through Android's document picker. Import offers validated Merge or Overwrite. The app does not provide continuous synchronization, bank connections, shared users, recurring rules, or Play Store publishing.

## User Stories

1. As a new user, I want to select one currency, so that amounts and Budgets have a consistent meaning.
2. As a new user, I want to create an Account during first-run setup, so that I can record a Transaction immediately.
3. As a user, I want to create an Account with a name and simple type, so that Transactions identify where money came from or went.
4. As a user, I want Account types such as Cash, Bank, Credit Card, and Other, so that Accounts have useful context without provider integrations.
5. As a user, I want every Transaction to require an Account, so that the ledger never contains an unowned financial movement.
6. As a user, I want to record an Income with a positive amount and date, so that money received appears in my ledger.
7. As a user, I want to record an Expense with a positive amount and date, so that spending appears in my ledger.
8. As a user, I want to add an optional Category to a Transaction, so that I can classify spending when useful.
9. As a user, I want to leave a Transaction uncategorized, so that I can record it before organizing my Categories.
10. As a user, I want to add an optional note to a Transaction, so that I can preserve context about the movement.
11. As a user, I want the app to use the device's local calendar date by default, so that a new Transaction is quick to enter.
12. As a user, I want to change a Transaction's date, so that historical movements appear in the correct month.
13. As a user, I want to edit a Transaction, so that mistakes can be corrected.
14. As a user, I want to delete a Transaction after confirmation, so that accidental or invalid movements can be removed.
15. As a user, I want to create Categories manually, so that my classification system reflects my own finances.
16. As a user, I want to rename a Category, so that its new name appears on historical Transactions and Budgets.
17. As a user, I want to archive a Category, so that it cannot be used for new Transactions or Budgets while its history remains intact.
18. As a user, I want to create reusable Tags, so that I can add cross-cutting context to Transactions.
19. As a user, I want a Tag to be independent or associated with one Category, so that tags support both broad and category-specific organization.
20. As a user, I want to assign multiple Tags to a Transaction, so that one movement can carry several useful contexts.
21. As a user, I want to rename a Tag, so that its new name appears on historical Transactions.
22. As a user, I want to archive a Tag, so that it cannot be newly assigned while historical assignments remain visible.
23. As a user, I want active Account, Category, and Tag names to be trimmed and case-insensitively unique within their scope, so that duplicate concepts do not accumulate.
24. As a user, I want to create a monthly Budget for a Category, so that I can set a spending limit.
25. As a user, I want one active Budget per Category per calendar month, so that budget progress is unambiguous.
26. As a user, I want to see actual spending, remaining amount, and over-budget state, so that I can understand my position quickly.
27. As a user, I want historical Budgets for archived Categories to remain visible, so that past reports remain truthful.
28. As a user, I want the Overview to default to the current month, so that the most relevant information is immediately visible.
29. As a user, I want the Overview to show Income, Expense, monthly net (Income minus Expense), Budget progress, and recent Transaction and Transfer activity, so that I can scan my finances.
30. As a user, I want to browse Transactions newest first, so that recent activity is easy to find.
31. As a user, I want to filter Transactions by month, Account, Category, and Tag, so that I can investigate spending.
32. As a user, I want a prominent quick-add action from the main screens, so that recording a movement does not require deep navigation.
33. As a user, I want Settings to expose currency and data-management actions, so that important configuration is discoverable.
34. As a user, I want to export a versioned JSON Backup, so that I can preserve my local dataset.
35. As a user, I want to choose the Backup destination through the Android document picker, so that Google Drive can be used without a cloud account inside the app.
36. As a user, I want a Backup to contain currency, Accounts and their opening balances and dates, Categories, Tags, Transactions, Transfers, Budgets, stable IDs, timestamps, and schema version, so that it can be restored accurately.
37. As a user, I want malformed or unsupported Backups rejected before changing local data, so that invalid input cannot corrupt my ledger.
38. As a user, I want to choose Merge or Overwrite when importing a Backup, so that I control whether local data is combined or replaced.
39. As a user, I want Merge to add missing records and apply newer updates by stable ID and `updatedAt`, so that repeated imports do not create duplicates.
40. As a user, I want Merge not to propagate deletions, so that combining datasets does not unexpectedly remove local history.
41. As a user, I want Overwrite to replace the complete local dataset only after an explicit confirmation, so that a Backup can become authoritative deliberately.
42. As a user, I want to delete all local data from Settings after explicit confirmation, so that I can reset the app intentionally.
43. As a user, I want all records to have stable unique IDs, so that references and Backup operations remain reliable even when timestamps collide.
44. As a user, I want the app to remain usable without network access, so that my finance data is available offline.
45. As a user, I want a light interface with readable amounts and clear status colors, so that the app is easy to scan.
46. As a user, I want to use the app on Android 10 and newer, so that the MVP supports my device.
47. As a maintainer, I want the app to be distributed as a locally signed APK, so that it can be sideloaded without Play Store publishing.
48. As a maintainer, I want signing credentials kept outside the repository, so that release secrets are not exposed.
49. As a maintainer, I want financial rules tested through public behavior seams, so that refactoring implementation details does not erase confidence.
50. As a maintainer, I want backup and migration compatibility tested, so that existing finance data remains usable across app versions.
51. As a maintainer, I want critical Compose and document-picker journeys tested, so that the most important user workflows are executable and diagnosable by an AI agent.
52. As a user, I want an optional signed opening balance and as-of date for each Account, so that its current balance can include money already there without misclassifying it as Income.
53. As a user, I want to see each Account's signed current balance and retain its balance and history after archiving, so that past and present Account activity remains understandable.
54. As a user, I want to move a positive amount between two different Accounts using a Transfer, so that internal movements do not appear as Income or Expense.
55. As a user, I want a Transfer to update the source and destination balances while remaining excluded from Income, Expense, and Budget totals, so that financial summaries reflect actual earnings and spending.
56. As a user, I want Transfer activity visible in Overview, both affected Account histories, and as outgoing/incoming rows in the Expense/Income lists, so that I can find the movement in context.
57. As a user, I want to filter Transfer rows by month and their relevant Account, while Category or Tag filters hide them, so that filters remain meaningful for uncategorized movements.
58. As a user, I want to reach Transfers from quick-add and choose both Accounts explicitly, so that the movement's direction is clear.
59. As a user, I want Income and Expense Categories kept in separate sets and Category-associated Tags limited to matching Transactions, so that classification remains consistent.
60. As a user, I want changing a Transaction's Category to automatically remove Tags associated with its former Category, so that it cannot retain an invalid classification.
61. As a user, I want a separate Budgets destination in navigation, so that monthly spending limits are easy to find.

## Implementation Decisions

- The application is native Kotlin Android using Jetpack Compose and Material 3.
- The minimum supported Android version is API 29 (Android 10). The compile and target SDK use the latest stable SDK available when implementation begins.
- The project uses one app module, Gradle Kotlin DSL, a version catalog, Kotlin/JVM 17, and stable AndroidX/Kotlin dependencies.
- The application ID is `com.toshlol.finance`.
- The app uses layered unidirectional data flow: Compose UI, state holders/ViewModels, application use cases, repositories, and local data sources.
- Room owns relational finance data and all Backup-owned settings, including selected currency, so import and export can use one database transaction. DataStore owns only preferences excluded from Backups, such as theme configuration.
- Kotlin serialization defines versioned Backup DTOs separately from persistence entities.
- Dependencies use manual construction initially rather than a dependency-injection framework.
- The app requests no `INTERNET` permission. Google Drive support comes through Android's document picker rather than a Google Drive API or continuous synchronization.
- The interface is light-theme only. App locking, biometric protection, and dark theme are out of scope for the MVP.
- First-run setup requires a currency and at least one user-created Account. No default Accounts or Categories are seeded.
- A Transaction has a stable unique ID, positive amount, Income or Expense direction, local calendar date, mandatory Account reference, optional Category reference, optional Note, optional multiple Tag references, and timestamps.
- A Transfer has a stable unique ID, positive amount, local calendar date, distinct mandatory source and destination Account references, and timestamps. It has no Category, Tags, or Note.
- Accounts, Categories, Tags, Transactions, Transfers, Budgets, and Backup records use stable unique IDs. UUID generation and time are separate injectable concerns.
- Account, Category, and Tag names are trimmed and case-insensitively unique among active entities in their respective scope.
- Account, Category, and Tag renames update historical displays because records reference entities by ID. Archiving preserves references, displayed names, balances, and history while preventing new use.
- Income and Expense Categories are separate sets; a Category can belong to only one direction. Categories remain optional on Transactions. Only Expense Categories can have Budgets.
- A Category-associated Tag can be assigned only to a Transaction using that Category. Changing or clearing the Transaction's Category automatically removes Tags associated with its former Category without a notice.
- Account types are display metadata only: Cash, Bank, Credit Card, or Other. Any Account type may have a positive, zero, or negative balance.
- Each Account has an optional signed Opening balance, defaulting to zero, and an as-of local calendar date, defaulting to today. The Opening balance is the amount at the start of that date; Income on or after that date adds to the balance, Expense subtracts, and Transfer subtracts from its source and adds to its destination. Movements dated before the as-of date do not affect the current balance. Opening balance and date can be edited.
- Opening balances are entered as signed amounts, including negative amounts, limited to two decimal places for the selected currency. Transaction, Transfer, and Budget amounts are positive and limited to two decimal places. Monetary values use precise minor-unit storage rather than floating-point arithmetic.
- Income and Expense entry flows share one most-recently-used Account selection. A new Transfer requires the user to choose both its distinct source and destination Accounts explicitly. Transfers may overdraw an Account.
- Transfers are available as a third quick-add flow. Each Transfer is one record shown as an outgoing row in the Expense list and source Account history, and as an incoming row in the Income list and destination Account history; both list rows open the same Transfer. Transfers also appear in Overview recent activity. Income and Expense list totals and Budget calculations exclude Transfers. Month and relevant Account filters include Transfer rows; Category and Tag filters hide them.
- Income and Expense screens show the selected calendar month's totals. Budgets have a separate navigation destination and are monthly limits for one Expense Category. Budgets do not roll over, and income budgets are out of scope.
- Export/import is explicit and manual. Export reads one consistent Room snapshot. A Backup includes schema version, selected currency, Backup-owned settings, entities, records, IDs, and timestamps.
- A Backup includes Transfers and each Account's Opening balance and as-of date. Older Backups without these fields import with no Transfers and zero Opening balances; each such Account's as-of date is the oldest included movement date, or the import date when it has no movements.
- One deep Backup import module owns decoding, complete validation, and atomic mutation for both Merge and Overwrite. The Android document-provider adapter supplies bytes, while confirmation and displayed outcomes remain UI concerns.
- Merge accepts only a self-contained Backup with the local currency, adds missing records, replaces a complete local record only when the imported `updatedAt` is newer, and does not propagate deletions. It rejects equal-timestamp divergent records and any reconciled dataset that violates finance rules rather than inventing conflict resolution.
- Overwrite validates and then atomically replaces the complete local dataset and Backup-owned settings after confirmation. Import is cancellable before its non-cancellable Room transaction begins; failures leave the previous complete dataset visible.
- Room uses explicit tested migrations; production does not use destructive migration.
- The app produces debug APKs and locally signed release APKs for sideloading. It does not produce Play Store publishing automation or require an App Bundle workflow.
- Signing credentials remain outside the repository and are supplied locally or through CI secrets.
- GitHub Actions runs formatting/lint, JVM tests, and available Android tests. Product issues remain tracked as local markdown under `.scratch/`.

## Testing Decisions

- Tests verify external behavior through public seams rather than private methods, implementation details, or direct table assertions.
- Each feature is built as a vertical red-green-refactor slice, starting with a Given/When/Then behavior scenario.
- The finance application/use-case seam covers amount validation, mandatory Accounts, optional Categories, direction-specific Categories, Tag/Category compatibility, Account balances and opening dates, Transfers, monthly Budgets, lifecycle rules, monthly boundaries, calculations, and Backup import behavior.
- The repository seam covers Room persistence and migrations through repository operations against an in-memory database.
- Compose screen tests cover critical journeys: first-run Account creation and opening balance, Transaction and Transfer entry, Account balance/history, Budget navigation and display, filters, validation, and Merge versus Overwrite choice.
- UI Automator covers the Android document-provider boundary for export/import and system UI. Espresso is used only if View-based or hybrid screens exist.
- Time and UUID providers are deterministic in tests. In-memory fakes are preferred over mocks; mocks are reserved for genuine external boundaries.
- Backup format tests cover strict decoding, supported-version normalization including older backups without Transfers or opening balances, malformed JSON, unsupported schema versions, bounded input, self-contained references, and valid round trips.
- Backup import tests exercise the public module interface with Room's in-memory configuration and cover currency mismatches, Merge idempotency, whole-record timestamp conflicts, equal-timestamp divergence, non-propagating deletions, final-dataset conflicts, Overwrite, cancellation, and rollback after injected transaction failures.
- Migration tests open representative data from prior schemas and verify preservation after migration.
- Accessibility tests assert semantics, labels, actions, and important state. Manual TalkBack and Switch Access checks remain necessary.
- CI runs JVM tests on every change and Android tests on Android 10 plus a current API emulator when available.
- No arbitrary global coverage percentage is required. Every agreed financial rule and critical user journey must have explicit coverage.
- Each locally signed release APK receives a manual smoke test covering launch, Account creation, Transaction entry, Budget display, export, import, and both import modes.

## Out of Scope

- Bank or card provider synchronization.
- Cloud accounts, backend services, automatic synchronization, or background uploads.
- Shared users, household accounts, and multi-user permissions.
- Investment tracking.
- Recurring Transactions or scheduled rules.
- Nested Categories.
- Category or Tag defaults seeded for new users.
- Income Budgets, Budget rollover, forecasting, and advanced reporting.
- Free-text Transaction search.
- Dark theme.
- App lock, biometric protection, encrypted or password-protected Backups.
- Play Store publishing, App Bundle distribution, and automated store administration.
- Direct Google Drive API integration.
- Automatic deletion propagation during Merge.

## Further Notes

The repository already contains the domain glossary, ADRs, and UI-testing research that informed this spec. The next step is to split this spec into dependency-ordered vertical tickets under the local tracker. The first implementation work should establish the Android project and test harness, then deliver the smallest end-to-end finance workflow before expanding budgets, lifecycle management, and Backup operations.
