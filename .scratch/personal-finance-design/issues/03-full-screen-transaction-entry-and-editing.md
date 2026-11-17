# 03: Full-screen Transaction entry and editing

Status: resolved
Blocked by: 01

**What to build:** Create and edit Income and Expenses through the reference full-screen amount/keypad layout, with explicit Account choices, optional text-only Categories, compatible Tags, editable date, and optional Note. Fix the existing behavior that can move an edited Transaction to the primary Account.

## Parent-spec requirements

- [User stories](../spec.md#user-stories): Stories 1-6, 41-50: shared visual styling, full-screen parallel entry, keypad, valid amount/date/Account, most-recently-used Account, preserved original Account, optional Note, and text-only optional Categories.
- [User stories](../spec.md#user-stories): Stories 51-52, 55: direction-specific Categories, compatible Tags, existing validation/confirmations, and archived read-only restrictions.
- [User stories](../spec.md#user-stories): Stories 83-85: accessible larger-font Android 10 entry and screenshots for both directions.
- [Implementation decisions](../spec.md#implementation-decisions): Keep Transaction Account choice separate from drawer selection and saved Budget scopes. Load the original Account when editing and use one most-recently-used active Account choice across new Income/Expense entry. The description control is the optional Note, and the reference's Pick a Category wording cannot become a save requirement.
- [Testing decisions](../spec.md#testing-decisions): use existing public finance use-case/repository and Backup interfaces, Compose semantics, and UI Automator for system UI; test visible outcomes rather than private helpers or tables. Named tests below are proposed or extensions of existing behavior tests, not claims of passing implementation.

## Observable acceptance criteria

- [x] Income/Expense entry uses a full-screen charcoal direction/amount/Note area and the reference amount keypad, with clear save/cancel actions. Selecting a direction scopes Categories correctly while maintaining a consistent form composition.
- [x] Account choices are visible and accessible. A new Transaction uses the most recently used active Account across Income and Expense; an empty or unrelated drawer selection does not remove the mandatory Account requirement or silently change the form's Account.
- [x] Editing loads the Transaction's actual Account, amount, date, direction, Note, Category, and Tags. Changing only Note/amount and saving preserves Account, stable identity, and creation time; deliberately changing Account recalculates both affected balances and monthly totals.
- [x] Amount input supports valid positive precise monetary values and correction/backspace without floating-point rounding. Invalid/nonpositive amount, missing active Account, or invalid date prevents mutation and exposes readable validation; valid input saves and refreshes visible values.
- [x] Date defaults to today's local calendar date and remains editable for backdated records. The description field is one optional Note, not an additional duplicate text field.
- [x] A valid Transaction can be saved without Category, Tags, or Note. Category choices are text-only with no icons or icon picker; no mandatory Pick a Category gate remains.
- [x] Changing direction, clearing Category, or choosing a different Category retains only compatible Tags under the existing rules. Archived entities/history remain readable, but prohibited new assignments and archived-Account edits are not enabled.
- [x] Cancel/dismiss does not save. Existing confirmed Transaction deletion still works from the redesigned edit journey and updates activity, balances, and Budget spending without changing unrelated records.
- [x] Expose reusable amount/date/entry presentation controls that ticket 05 can use without coupling Transfer semantics to Transaction classification. Relevant public behavior and Android 10 form journeys, lint, formatting, and screenshots for both directions verify the delivered slice.

## Named behavior tests

| Test or journey | Observable result |
| --- | --- |
| editTransactionWithoutUnexpectedAccountChange | Create against a non-primary Account, edit Note/amount, relaunch, and assert unchanged Account/identity plus correct revised totals; then assert an explicit Account change affects both Accounts. |
| reuseMostRecentlyUsedAccountAcrossIncomeAndExpense | Save with a chosen active Account and verify preselection on the opposite new-entry direction independently of the drawer filter. |
| saveUncategorizedTransactionUsingTextOnlyEntry | Use keypad, Account, and date to save without classification/Note; assert no Category gate or icon controls. |
| validateKeypadAmountAccountAndDateWithoutMutation | Exercise positive/zero/negative/invalid input, decimal precision and correction, missing Account, invalid dates, and safe cancellation. |
| scopeCategoriesAndRemoveIncompatibleTagsInFullScreenEntry | Change direction/Category and assert offered Categories, removed associated Tags, and preserved compatible selections. |
| confirmDeletionFromRedesignedTransactionEntry | Verify cancel/confirm deletion and correct activity/balance/Budget updates while preserving unrelated records. |
| operateTransactionEntryAtLargeFont | Assert labeled keypad/actions/choices and usable access to date, Tags, save, and cancel at larger fonts. |

## Delivery boundaries

Do not require ticket 02's new navigation to use this form; it must be launchable from the existing Overview actions after ticket 01. Do not add Category icons, duplicate description fields, Transfer classification, or network-dependent behavior.

Each slice must remain independently demoable and keep existing callers and relevant checks working. Include the feature's behavior and UI verification in this ticket rather than deferring them to the final integration ticket. Follow the parent spec's exclusions and existing ADRs.

## Answer

Replaced the transaction editor with a full-screen charcoal Income/Expense form, accessible active Account choices, editable amount/date/Note, optional text-only Category and compatible Tags, and a custom amount keypad. New Income and Expense entries share a persisted most-recently-used active Account; edits load their original Account and reject archived-Account editing. Extracted reusable amount-keypad and date-field controls for ticket 05. Captured Android 10 evidence in `../evidence/03/`.

## Comments

- 2026-11-17: The new API 29 journey `editTransactionPreservesAccountAndReusesLastAccountAcrossDirections` passed, verifying a non-primary Account remains on edit and is reused for the opposite direction. `:app:testDebugUnitTest`, `:app:ktlintCheck`, and `:app:lintDebug` passed.
- 2026-11-17: Full connected run exercised 53 tests; 9 legacy `TransactionFlowTest` journeys timed out in the existing navigation/category/Account flows documented by ticket 02. The new ticket 03 journey passed in that run. Screenshots: [Expense entry](../evidence/03/expense-entry.png) and [Income entry](../evidence/03/income-entry.png).
- 2026-11-17: The initial implementation was left ready for human review because several ticket-specific journeys did not yet have test evidence.
- 2026-11-17: Added and passed focused API 29 Compose journeys for Account/identity/date/Note/Category/Tag preservation and Account changes, monthly totals and balances, precise keypad correction, invalid amount/date and missing active Account no-mutation, uncategorized save, cancellation, confirmed deletion, and 1.5x-font usability. Also reran direction/Category/Tag compatibility. `:app:ktlintCheck`, `:app:lintDebug`, and `:app:testDebugUnitTest` pass. Ticket-specific journeys are now covered; the known unrelated legacy navigation timeouts remain tracked under issue 02.
- 2026-11-17: Reran the categorized Budget progress journey through the current drawer navigation and verified deleting the Transaction returns Budget spending to zero. The deletion test also confirms a same-month transaction in another Account remains unchanged. Ticket 03 acceptance criteria and focused Android 10 checks are complete.
