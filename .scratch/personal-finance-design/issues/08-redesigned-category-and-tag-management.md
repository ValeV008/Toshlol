# 08: Redesigned Category and Tag management

Status: resolved
Blocked by: 01

**What to build:** Create, rename, associate, and archive Categories and Tags in a consistent text-only management interface while preserving direction scopes, historical references, and compatibility with Transaction entry and Budget restrictions.

## Parent-spec requirements

- [User stories](../spec.md#user-stories): Stories 1-6, 83-85: adapt shared reference styling to Category/Tag administration with readable, accessible offline Android 10 behavior.
- [User stories](../spec.md#user-stories): Stories 36, 49-51: text-only optional Category presentation, separate Income/Expense Categories, compatible Tags, and preserved classification/filter capabilities.
- [User stories](../spec.md#user-stories): Story 75: renamed and archived references remain understandable in Transactions, history, and existing or subsequently added Budget views.
- [Implementation decisions](../spec.md#implementation-decisions): Consume stable-reference public classification behavior and ticket 01's visual components. Keep management independent of the Transaction form implementation so this slice can land before or after ticket 03. Archive preserves history; it does not delete referenced entities or silently clear Budget restrictions.
- [Testing decisions](../spec.md#testing-decisions): use existing public finance use-case/repository and Backup interfaces, Compose semantics, and UI Automator for system UI; test visible outcomes rather than private helpers or tables. Named tests below are proposed or extensions of existing behavior tests, not claims of passing implementation.

## Observable acceptance criteria

- [x] The Categories & Tags destination and create/edit/archive controls use the shared visual system with accessible headings, labeled inputs, selection states, validation, and larger-font scrolling. Category rows/choices remain text-only with no icon picker or icon storage.
- [x] Create and rename Income and Expense Categories within their existing direction scopes, preserving trimmed case-insensitive uniqueness rules and stable identities. Offer direction clearly rather than allowing one Category to classify both Income and Expense.
- [x] Create/rename independent Tags and Category-associated Tags, showing their association and enforcing existing validation. Distinct fixture records remain independently editable.
- [x] Renames resolve through stable references in already available Transaction lists/forms, Account history, and Budget views. No movement identity, amount, or chosen scope is rewritten to achieve a displayed name change.
- [x] Archiving prevents prohibited new classification assignments while preserving historical names and references. A selected archived Budget restriction must not be silently removed or converted into an unrestricted group; integrate through the existing stable-reference contracts used by tickets 06-07.
- [x] Changing Transaction Category/direction retains existing automatic removal of incompatible associated Tags, while Categories themselves remain optional. Updated management records become available in already existing entry/filter flows without navigation resets or stale lists.
- [x] Persistence/reopen and public classification tests, Android 10 management/history journeys, relevant lint/formatting checks, and Category/Tag screenshots verify validation, independent records, renamed history, archived state, and larger fonts.

## Named behavior tests

| Test or journey | Observable result |
| --- | --- |
| manageTextOnlyDirectionScopedCategories | Create/rename each direction, exercise uniqueness/validation, and assert no icon controls or mixed-direction assignment. |
| manageIndependentAndAssociatedTags | Create/edit Tags with and without an association and verify offered compatible selections through public reads/entry. |
| renameAndArchiveClassificationsWithoutChangingTransactions | Assert persisted identity, unchanged movement data, historical labels, archived assignment restrictions, and readable references. |
| preserveFiltersThroughRenamesAndArchives | Exercise currently available Budget scopes/public reference behavior so later recurring Budget integration retains archived restrictions. |
| refreshClassificationChoicesWithoutResettingMonth | Return to existing entry/filter views and assert updated labels/choices and retained period. |
| operateClassificationManagementAtLargeFont | Assert input/action names, selected states, archive labels, scrolling, and readable validation. |

## Delivery boundaries

Do not make this ticket wait for the redesigned entry or custom Budget editor: existing public classification flows provide an independently demoable journey. Their own tickets verify the final integration with the revised forms and Budget model. No Category icons, seeded Categories, nested classifications, or new deletion policy.

Each slice must remain independently demoable and keep existing callers and relevant checks working. Include the feature's behavior and UI verification in this ticket rather than deferring them to the final integration ticket. Follow the parent spec's exclusions and existing ADRs.

## Answer

Implemented the accessible text-only Categories & Tags manager, including direction-scoped category creation and renaming, independent and category-associated tags, validation, archiving, stable-reference history, archived Budget restrictions, and refreshed entry/filter choices. Returning from management now waits for related references to refresh, preventing stale category and tag options. When editing a transaction with an archived Category, existing associated Tags remain selected and removable while unassigned Tags under that Category are disabled.

Verification passed on Android 10/API 29: all 85 connected tests passed with no failures or skips. Host-runner tests passed 15/15. `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:ktlintCheck`, `:app:assembleDebug`, and `:app:assembleDebugAndroidTest` passed. The selected and empty Overview process-restart checks passed.

Screenshots: [validation](evidence/08/classification-validation.png), [independent and associated tags](evidence/08/classification-independent-and-associated-tags.png), [renamed history](evidence/08/classification-renamed-history.png), [archived Budget restrictions](evidence/08/classification-archived-budget.png), and [large-font management](evidence/08/large-font-management.png).

Run logs: [connected Android tests](evidence/08/connected-android-test.log), [archived Category edit regression](evidence/08/focused-archived-category-regression.log), [Gradle checks](evidence/08/gradle-checks.log), [host-runner tests](evidence/08/host-tests.log), and [Overview persistence](evidence/08/overview-persistence.log). Overview restart evidence is under `build/overview-persistence/20261119T174310562546Z/`.
