# UI Reference Refresh: approved ticket breakdown

Plan status: approved and published on 2026-11-23.

Source: [UI Reference Refresh and Transaction Splits](spec.md).

The user approved the 14-ticket breakdown and its blocking edges with "it's ok" after review. All tickets are now published individually with Status: ready-for-agent; the [implementation map](map.md) links them and records dependencies, coverage, and the current frontier. This document retains the approved scope; published ticket files and the map carry ongoing execution status. No implementation is authorized. Keep the implemented Personal Finance Design specification, tickets, map, and verification unchanged.

## Slicing decisions

- Each ticket delivers a usable path through the existing app and owns its public behavior checks, applicable persistence/Backup changes, and screenshot evidence. No standalone schema-only, UI-only-without-behavior, or test-only feature ticket is proposed.
- Reuse the existing app and confirmed testing boundaries: public Transaction/application-repository and monthly-summary behavior, public Backup snapshots, and Android 10 Compose/UI Automator journeys. Do not build a second finance stack or per-screen testing interfaces.
- The first slice delivers the reference shell and a complete Overview; any small extraction of shared reference headers/actions belongs at the start of that slice and must preserve working destinations. No broad mechanical refactor or expand-contract campaign is currently justified.
- The shared shell and reference components from 01 provide the navigation/action and visual integration contract used by the later screen slices. After 01, the independent destination and feature branches can progress without waiting for unrelated finance features.
- Transaction split creation includes durable identities, explicit migrations, compatible Backup handling, and correct money/classification reads immediately. Its later mutation capabilities are separate complete slices. Never temporarily send grouped data through unsafe ordinary single-record mutations or display dead controls for capabilities not delivered yet.
- Ticket 08 establishes the identity/membership and reconciliation contract for ordinary/part transitions before storage mutation, allowing later mutation slices to extend the same model. It is the most involved ticket, but is restricted to creating, loading, displaying, and backing up allocations; group editing/deletion and Unsplit are separate.
- Category summaries and Budget presentation need not block Transaction split creation: existing classifications, filters, Budget matching, and finance snapshots already provide the public behavior. Whichever branch lands second verifies the combined part-aware result. Completion of the whole effort still requires the integrated journeys.
- Every owning UI slice must look the same as, or as close as possible to, its named reference at normal font size, allowing only the specification's agreed adaptations. Compare real Android 10 captures for layout, typography, spacing, colors, icons, amounts, and control placement. Do not defer ordinary visual correction or required tests to ticket 14.
- Every implementation ticket must satisfy the repository's full ticket-completion checklist against its finished changes and record evidence before resolution. The final ticket covers combined journeys and remaining cross-screen discrepancies rather than replacing earlier verification.
- Global requirements apply to every ticket: native offline/light Android 10 behavior, configured-currency formatting, stable finance identities, historical-data preservation, readable long/large-font content, accessible semantics/actions, and contextual empty/error states. No new unsupported tools or finance-policy changes.

## Approved graph and coverage

Numbers and blocking edges below are approved. Blocking lists contain direct prerequisites only; transitive prerequisites are omitted.

| Ticket | Title | Blocked by | Primary parent stories |
| --- | --- | --- | --- |
| 01 | Reference navigation shell and Overview | None | 1-10, 12, 21-22, 30-32, 98-100 |
| 02 | Active Account drawer and archived Account journeys | 01 | 12-29, 31, 87, 95-97 |
| 03 | Transaction details and immutable direction | 01 | 42-46, 59 |
| 04 | Reference Transaction editors and picker flows | 03 | 45-59 |
| 05 | Reference By date Expense and Income lists | 02, 03 | 5-6, 10, 12, 21-22, 33-35, 39-41 |
| 06 | By category summaries and activity drill-down | 05 | 36-40 |
| 07 | Reference Budget cards and separate editing | 02 | 11, 24-26, 29, 82-86, 95 |
| 08 | Create, view, and back up Transaction splits | 04, 05 | 60-70, 78-81, 95-97 |
| 09 | Edit and delete Transaction split parts safely | 08 | 62, 71-75, 79-81, 95-97 |
| 10 | Unsplit with a cancellable recombination draft | 08 | 76-79, 95-97 |
| 11 | Reference Categories and Tags management | 01 | 4, 52-53, 88 |
| 12 | Reference Transfer entry and related history | 01 | 10, 41, 89, 95 |
| 13 | Reference setup, Settings, and data management | 01 | 8, 48, 90-91, 95-98 |
| 14 | Integrated screenshot fidelity and Android 10 journeys | 06, 07, 09, 10, 11, 12, 13 | 1-100, especially 92-94, 98-100 |

Stories 92-94 and 98-100 are also cross-cutting requirements for every applicable owning slice. A repeated story means integration coverage, not permission for either owner to leave its observable behavior untested.

## Approved ticket scope

### 01: Reference navigation shell and Overview

**Blocked by:** None (can start immediately after publication and a separate implementation instruction).

**What it delivers:** The app opens into the new reference shell and a complete blue-header Overview, with correct existing financial information, six-destination drawer navigation, Settings footer access, and the three working bottom actions.

**Parent-spec requirements:** [User stories](spec.md#user-stories) 1-10, 12, 21-22, 30-32, 98-100; [Implementation Decisions](spec.md#implementation-decisions) for visual authority, navigation, Overview order, existing finance preservation, and shared scope. [Testing Decisions](spec.md#testing-decisions) and [reference inventory](spec.md#further-notes) apply.

**Acceptance criteria:**

- The left drawer reaches Overview, Expenses, Incomes, Budgets, Categories and Tags, and Accounts; Settings is at its bottom. The old Overview/Expenses/Income navigation tabs disappear without removing access to any required function.
- Overview uses the agreed blue/off-white/white system and exact section order: combined selected-month Closing balance, monthly Income/Expenses/net, Account cards, Budget progress, recent activity. Existing correct amounts and month context remain unchanged.
- Add expense, Add income, and Transfer money are visible, labeled, and functional. Shared header/action components accommodate later screen tickets without duplicating financial calculations. Do necessary small prefactoring before switching this live journey.
- Overview empty selection remains zero/no matching activity/no selection prompt; independently scoped Budget progress may remain nonzero. No fabricated financial records, percentages, or update timestamps appear.
- Long content and large fonts remain readable and actions do not obscure the final activity row. Other destinations remain usable while their owning refresh tickets are pending.
- `navigateReferenceShellAndBottomActions` and the Overview part of `preserveSelectedMonthFinanceAndTransferRules` assert displayed values and navigation. Save normal/large-font Overview and left-drawer evidence against the reference roles.

### 02: Active Account drawer and archived Account journeys

**Blocked by:** 01 (uses the shared financial shell, clock action, and navigation integration).

**What it delivers:** Users change month/active Account scope through the reference right drawer, archive Accounts without losing history, and manage active versus archived Accounts through the adapted Accounts destination.

**Parent-spec requirements:** [User stories](spec.md#user-stories) 12-29, 31, 87, 95-97; [Implementation Decisions](spec.md#implementation-decisions) for shared month, persistent active-only selection, archive normalization, independent Budget scope, and blue management screens.

**Acceptance criteria:**

- The clock, right-edge gesture, and accessible action reach the same reference drawer from Overview, Expenses, Incomes, and Budgets. It has month selection, active Account Closing balances/switches, active-only All accounts, and no bottom actions or arbitrary date ranges.
- All accounts on selects the currently active Accounts; off clears. Partial selection appears off, new Accounts remain unselected, and initially empty/subset/explicitly empty selections survive actual app/process restarts.
- Archiving removes an Account from all main selections and the drawer, even in earlier-month views. Loading stale preferences or imported archived state cannot reselect it. Existing Account data, historical Closing balances, Budget scopes/spending, and supported Backup content remain intact.
- Accounts has Active/Archived views, reference-style Account management/editing, and retained read-only archived history. Signed Opening balances and opening-date rules remain unchanged. Archive is not permanent deletion.
- Account switches opened from Budgets update the scope used on other main screens without changing Budget header/content/progress; changing month updates its progress. Transaction Account choice and saved Budget scope remain separate.
- `shareMonthAndActiveAccountScopeAcrossDestinations`, `persistEmptyPartialAndBulkAccountSelection`, `archiveSelectedAccountAndKeepHistoricalData`, and `operateBudgetDrawerWithoutChangingBudgetScope` cover public values, restart behavior, and history access. Capture drawer/Account editor/Active/Archived/history evidence at normal and large fonts.

### 03: Transaction details and immutable direction

**Blocked by:** 01 (consumes the shared navigation/actions and reference presentation contract).

**What it delivers:** Ordinary Transaction rows open reference-style details before editing, with correct metadata, existing delete confirmation, and fixed Income/Expense direction through both UI and public editing behavior.

**Parent-spec requirements:** [User stories](spec.md#user-stories) 42-46, 59; [Implementation Decisions](spec.md#implementation-decisions) for details-first navigation, immutable direction, preserved Account/identity, and destructive confirmation.

**Acceptance criteria:**

- Transaction taps from Overview and both directional lists open amount/metadata/optional Note details with back/edit/delete. Omit empty optional metadata and unsupported Make transfer conversion.
- Edit opens the current valid editing flow, retaining identity and original Account. Delete requires confirmation; cancellation and validation failure leave the Transaction unchanged.
- Creation direction follows Add expense/Add income. No direction selector remains and public save/edit rejects conversion of an existing Transaction, preserving all other data on failure.
- Keep Categories appropriate to the immutable direction and existing compatible-Tag/archive restrictions. Do only the minimal public-boundary/state cleanup needed for this behavior; do not create another finance module.
- Split/Unsplit are not dead controls before their owning capability tickets. The details interface must accommodate their later addition without changing ordinary values or navigation.
- `inspectDetailsThenEditWithFixedDirection` asserts displayed detail values, confirmation, Account/identity preservation, and rejection through the public edit boundary. Capture both details directions against their new references.

### 04: Reference Transaction editors and picker flows

**Blocked by:** 03 (relies on details-to-edit navigation and immutable public edit behavior).

**What it delivers:** Users create and edit ordinary Income and Expense through the screenshot-based amount, colored picker rows, Note editor, and consistent save/cancel flow.

**Parent-spec requirements:** [User stories](spec.md#user-stories) 45-59; [Implementation Decisions](spec.md#implementation-decisions) for reference editor layout, active Account/defaults, classification compatibility, keyboard/pickers, and dirty drafts.

**Acceptance criteria:**

- The two editors reproduce the red/green amount and Category/Tags/Account/Date hierarchy as closely as possible. Currency is display-only; there are no unsupported location/recurrence/reminder/photo controls or direction selector.
- Amount opens the numeric keypad; Aa opens the optional Note editor; Category/Tags/Account open bottom sheets; Date opens a calendar. Category None and multiple compatible Tags work without Category-specific artwork.
- Account is mandatory/active, defaults to the most-recently-used active Account on creation, and loads the original on edit. Date defaults to today's local date. Editing another field never moves money to a primary Account.
- One top-right save check, top-left cancel, Back/dismiss discard behavior, and dirty-draft confirmation work consistently. Invalid positive-amount/date/classification choices show field errors without losing the draft.
- A saved ordinary Transaction is observable in details, monthly money, and relevant Budget reads; reopen/restart preserves it. Preserve archived classification references under existing edit rules while preventing new invalid assignments.
- `operateReferenceEntryPickersAndDrafts` plus `inspectDetailsThenEditWithFixedDirection` verify public/screen outcomes. Capture both editors, picker/calendar/Note, empty/validation states, and large-font layouts; do not defer their appearance to final integration.

### 05: Reference By date Expense and Income lists

**Blocked by:** 02, 03 (uses the normalized shared active scope and details-first row actions).

**What it delivers:** Both directional lists show the selected month's correct activity in the red/green reference layout, with day grouping, filters, Transfer rules, and the three bottom actions.

**Parent-spec requirements:** [User stories](spec.md#user-stories) 5-6, 10, 12, 21-22, 33-35, 39-41; [Implementation Decisions](spec.md#implementation-decisions) for By date ordering, hierarchy, main scope, filters, and distinct Transfers.

**Acceptance criteria:**

- Reproduce the appropriate header, Account/month summary, gray date/subtotal bands, white rows, Category/Tags/optional Note hierarchy, amount alignment, and red/green meaning. Omit search and floating plus; retain all three labeled bottom actions.
- By date is the initial list mode. Day/local-month boundaries, newest-first deterministic order, Category/Tag filters, zero selection, and contextual empty states use the existing correct finance data.
- Outgoing/incoming Transfers follow selected source/destination membership, open the existing Transfer, and remain excluded from daily/monthly flows, net, and Budget spending. Category/Tag filters hide Transfers.
- Ordinary Transaction rows open the details from 03, retain month/scope on return, and expose Account context without breaking the reference hierarchy.
- Preserve a working By date route while By category is delivered separately; do not expose a nonfunctional second tab. Keep the mode-state extension local to presentation and existing public summary behavior.
- `preserveSelectedMonthFinanceAndTransferRules` and directional portions of `shareMonthAndActiveAccountScopeAcrossDestinations` assert values, membership, filtering, and navigation. Capture normal/large-font/empty/filtered Expense and Income examples against their references.

### 06: By category summaries and activity drill-down

**Blocked by:** 05 (extends its directional mode, scope/filter context, rows, and return navigation).

**What it delivers:** Users switch between functioning By date/By category tabs, see descending Category totals, and open the matching classified activity without changing scope.

**Parent-spec requirements:** [User stories](spec.md#user-stories) 36-40; [Implementation Decisions](spec.md#implementation-decisions) for Category grouping, Uncategorized, drill-down, Transfer exclusion, and per-part classification reads.

**Acceptance criteria:**

- Both tabs function in Expenses and Incomes; By date remains the initial view. Switching preserves direction, selected month/Accounts, and applicable filters.
- By category shows matching totals largest first, including Uncategorized when applicable. Totals are based on movements, not displayed labels or duplicated card/group metadata; Transfers are absent.
- A Category opens its matching activity, whose rows open details and return without losing context. Existing Tag filtering remains usable.
- Use the existing public Transaction/summary outputs. If Transaction split support is already present, group/count matching parts individually; otherwise the later allocation ticket must verify that combined behavior when it lands.
- `switchCategorySummaryAndDrillDown` asserts actual totals, ordering, unclassified values, filtering, and Transfer absence. Capture the adapted normal/large-font Category summary and drill-down as part of this slice.

### 07: Reference Budget cards and separate editing

**Blocked by:** 02 (requires the shared right drawer's month and scope-independent Budget behavior).

**What it delivers:** Users review one reference-style card per monthly Budget and create/edit definitions on a separate screen while their existing saved scopes remain independent of main Account selection.

**Parent-spec requirements:** [User stories](spec.md#user-stories) 11, 24-26, 29, 82-86, 95; [Implementation Decisions](spec.md#implementation-decisions) for retained Budget model, separate editing, independent header/scope, and progress presentation.

**Acceptance criteria:**

- Use blue header/plus, All budgets / selected month, and one white card per Budget containing name, month, saved scope, spent/limit, remaining/overspent, and conventional spent/limit progress. Omit unexplained black markers and Non-monthly grouping.
- Plus opens separate creation; tapping a card opens editing. Remove the always-visible inline editor while preserving all existing fields, filters, nonempty explicit Account scope, positive amount validation, and stable saved identity.
- Recurrence, no carryover, independent overlap, OR/AND restriction matching, all-month edits, and preserved archived scopes/history remain correct. Drawer Account changes do not alter the header, cards, or definitions.
- Do not redo the already-settled legacy Budget conversion or add Budget deletion/stopping/archival. Existing Backups remain valid.
- `preserveBudgetModelAndSeparateEditor` and `operateBudgetDrawerWithoutChangingBudgetScope` assert public progress and displayed values. Capture list, creation/editing, empty/over-budget, and large-font evidence. Verify part-aware spending when allocation support is present or lands later.

### 08: Create, view, and back up Transaction splits

**Blocked by:** 04, 05 (reuses the reference picker/keypad/draft flow and separate directional activity rows/details integration).

**What it delivers:** An ordinary Expense or Income can be allocated into two or more durable, independently classified parts, seen as separate marked rows and related details, counted once, and preserved through supported Backup workflows.

**Parent-spec requirements:** [User stories](spec.md#user-stories) 60-70, 78-81, 95-97; [Implementation Decisions](spec.md#implementation-decisions) for Transaction split creation, identity/membership, exact initial money, reference form, related reads, part-aware projections/matching, and compatible atomic persistence/Backup.

**Acceptance criteria:**

- Before mutation, document the concrete stable-identity/group-membership, migration/versioning, and Backup reconciliation contract for initial allocation and later ordinary/part transitions. It must support the specified future edits/deletion/Unsplit without violating immutable direction, no-deletion-propagation Merge, conflict rejection, or atomicity. Do not invent a different product policy or discard previous finance/Budget history.
- Ordinary details expose Split for both directions. Follow the supplied Split form: shared Account/date, two initial stacked part cards, Amount/Category/Tags, remaining total, Add part, per-card remove, and Aa Note. Reuse the confirmed picker/keypad behavior and correct red/green styling.
- Save requires at least two positive parts exactly totaling the original; allow incomplete drafts but reject incomplete/invalid Save without mutation. Cancel, discard, storage failure, and validation failure retain the original complete Transaction and ledger.
- Persist/reload the relationship, separate marked rows, own part values, current combined total, and tappable related details. No extra original/group movement remains countable. Public balances, monthly/day totals, Category/Tag filters, and Budget restrictions count matching parts once; EUR 40/20 gives the required EUR 60 total and per-scope amounts.
- If By category or refreshed Budget cards have landed, verify their combined part-aware outputs now; if not, existing public matching/filtering reads must already be correct and those presentation tickets inherit the integration obligation.
- Add and test explicit non-destructive upgrade and supported Backup DTO/validation/export/import handling in this slice. Retain supported older Backups/legacy Budget provenance, idempotent supported Merge, reference/conflict rejection, and complete Overwrite/cancellation/rollback. Never infer groups from equal amounts/dates or silently duplicate money in incompatible Merge outcomes.
- Until 09/10 supply grouped mutations, do not allow new parts to fall through ordinary single-record Account/date/delete logic or expose nonfunctional mutation controls. Ordinary Transaction editing still works; allocation creation, viewing, and Backup behavior are complete and independently demoable.
- `createExactTransactionSplitForBothDirections`, `showSeparatePartsAndRelatedDetails`, `matchPartsToCategoryFiltersAndBudgets`, initial `cancelOrFailGroupOperationWithoutPartialChanges`, and `upgradeAndRoundTripTransactionSplitData` cover public results, reopen/migration, and supported Backup states. Capture both creation directions, marked rows/details, per-part Notes, validation, and large fonts.

### 09: Edit and delete Transaction split parts safely

**Blocked by:** 08 (requires durable group identities, related reads, part-aware money, and Backup contracts).

**What it delivers:** Users correct a part's amount/classification/Note, move the complete group to another Account/date, or delete a selected part without leaving inconsistent relationships or financial totals.

**Parent-spec requirements:** [User stories](spec.md#user-stories) 62, 71-75, 79-81, 95-97; [Implementation Decisions](spec.md#implementation-decisions) for per-part mutations, current totals, shared fields, sole-survivor normalization, archives, and atomic Backup-aware transitions.

**Acceptance criteria:**

- Editing amount/Category/Tags/Note affects only the opened part. EUR 40 -> EUR 45 in a EUR 40 + EUR 20 group becomes EUR 65; balances, group details, directional lists, filters, and matching Budget spending all reflect the correction once.
- Account/date changes state their all-parts effect and update the group atomically, including balances and affected month views. Reject archived/invalid targets without partial changes. Direction remains immutable through UI and public operations.
- Confirmed deletion removes only the selected part; cancellation changes nothing. When one remains, it is an ordinary Transaction without stale membership/marker and can use ordinary Split behavior. Archived history stays read-only.
- Extend 08's supported persistence/Backup transition behavior in the same slice; stable unaffected identities, coherent snapshots, no deletion propagation, supported repeated Merge, explicit conflict rejection, and rollback remain covered. Do not defer these cases to a final Backup ticket.
- `editPartAndRecalculateCurrentTotal`, `changeSharedAccountOrDateAtomically`, `deletePartAndNormalizeSoleSurvivor`, and mutation cases of `reconcileSplitTransitionsWithoutViolatingMergePolicy` assert reopened/public snapshots and displayed results. Capture edited/group-shared-field/deletion/sole-survivor states, including large fonts.

### 10: Unsplit with a cancellable recombination draft

**Blocked by:** 08 (requires the durable relationship, draft integration, and transition/Backup contract; per-part editing is not a logical prerequisite).

**What it delivers:** Users open Unsplit from a part, choose the resulting ordinary Transaction's classification/Note, and save or cancel a complete recombination without changing money accidentally.

**Parent-spec requirements:** [User stories](spec.md#user-stories) 76-79, 95-97; [Implementation Decisions](spec.md#implementation-decisions) for Unsplit, draft boundaries, explicit final metadata, immutable direction, and coherent persistence/Backup transitions.

**Acceptance criteria:**

- Existing part details expose Unsplit instead of Split again. It drafts one ordinary Transaction at the current sum, preserving shared Account/date/fixed direction and allowing explicit Category/Tags/Note choice rather than arbitrary metadata inheritance.
- Save commits the complete replacement once; balances/monthly money remain unchanged and there are no stale countable parts/markers/relationships. Classification changes affect Category/Tag/Budget results according to the chosen final fields.
- Cancel/discard/invalid Save/failure leave every original part, field, identity, and relationship intact. No nested allocation or direction conversion is possible.
- Extend 08's stable transition, migration/serialization where needed, supported Backup reconciliation/conflict handling, and atomic snapshot tests. Use the current sum even if supplied by a coherent supported imported snapshot or later edited group; do not assume the initial allocation total is immutable.
- `unsplitDraftCommitsOnlyOnSave`, Unsplit cases of `cancelOrFailGroupOperationWithoutPartialChanges`, and `reconcileSplitTransitionsWithoutViolatingMergePolicy` cover public and UI results. Capture the Unsplit entry, final-metadata draft, cancel/validation, and resulting ordinary details at normal and large fonts.

### 11: Reference Categories and Tags management

**Blocked by:** 01 (uses the shared neutral reference components and destination navigation).

**What it delivers:** The Categories and Tags destination uses separate tabs and the neutral reference style while existing creation/edit/archive and classification compatibility work end to end.

**Parent-spec requirements:** [User stories](spec.md#user-stories) 4, 52-53, 88; [Implementation Decisions](spec.md#implementation-decisions) for neutral management screens, text-only direction-specific classification, archived reference preservation, and compatible Tags.

**Acceptance criteria:**

- Provide blue header, white lists/forms, separate Categories/Tags tabs, and functioning create/edit/archive paths without adding Category artwork or an icon picker.
- Keep Income/Expense Category distinction and optional Tag/Category association. Do not confuse choosing a Category's direction in classification management with forbidden direction conversion of an existing Transaction.
- Renames/archives remain intelligible on existing Transactions/parts and Budget scopes; compatible choices/defaults remain correct in whichever Transaction editor version is present. No archived restriction is silently broadened or dropped.
- `operateAdaptedSecondaryFlows` covers both tabs, saved/reloaded classification changes, history references, and validation. Retain relevant existing classification tests; capture normal/large-font/empty/error layouts within this slice.

### 12: Reference Transfer entry and related history

**Blocked by:** 01 (consumes the shared Transfer bottom action and neutral reference presentation contract).

**What it delivers:** Users create, inspect/edit, and confirm deletion of Transfers through the adapted amount/From/To/Date flow, with correct paired activity and balances.

**Parent-spec requirements:** [User stories](spec.md#user-stories) 10, 41, 89, 95; [Implementation Decisions](spec.md#implementation-decisions) for distinct Transfers, explicit endpoints, amount/date entry, archive restrictions, and no classification fields.

**Acceptance criteria:**

- Transfer money opens reference-style large amount and From Account / To Account / Date controls, configured currency, usable save/cancel/validation, and compatible normal/large-font layouts. Do not add Category, Tags, Note, per-Transfer currency, or Transaction-conversion controls.
- Require positive precise amount and two explicit distinct active endpoints; preserve existing edit/delete confirmations, identity, validation, and read-only archived history behavior.
- Saving/reopening reflects correct endpoint balances and related directional/Account-history rows, with one Overview activity item and no Income/Expense/net/Category/Budget inflation. Preserve the current source/destination membership rules.
- `operateAdaptedSecondaryFlows` and `preserveSelectedMonthFinanceAndTransferRules` cover actual public/displayed amounts and validation. Retain existing Transfer persistence/Backup coverage and capture entry, invalid endpoints, paired activity, and history.

### 13: Reference setup, Settings, and data management

**Blocked by:** 01 (uses the neutral reference system and new Settings-footer route).

**What it delivers:** First-run setup and Settings follow the new style, with currency/first Account choices and complete supported Export/Import/reset flows accessible from the new navigation.

**Parent-spec requirements:** [User stories](spec.md#user-stories) 8, 48, 90-91, 95-98; [Implementation Decisions](spec.md#implementation-decisions) for neutral setup/Settings, existing currency policy, offline operation, supported Backups, and confirmations.

**Acceptance criteria:**

- First-run setup uses blue/white forms, requires the existing explicit currency and first Account choices, preserves signed Opening balance validation, and seeds no financial records.
- Settings-footer navigation reaches configured currency and Export/Import/reset. Retain the current supported currency behavior and all Overwrite/reset and relevant cancellation confirmations; ordinary entry currency remains display-only.
- Export, supported Merge/Overwrite, cancelled pickers, invalid files, and reset work through existing public Backup/system-picker boundaries with readable results and no lost draft/data. New Transaction split serialization remains owned by 08-10; this slice must not redesign the Backup policy.
- Remain offline on Android 10 with no new permission/cloud requirement. Empty/error/long-label states and large-font scrolling expose every action.
- `operateAdaptedSecondaryFlows` plus the existing public Backup and UI Automator picker journeys verify these flows. Capture setup, Settings, success/error/cancel/confirmation, and large-font examples.

### 14: Integrated screenshot fidelity and Android 10 journeys

**Blocked by:** 06, 07, 09, 10, 11, 12, 13 (the terminal feature branches; earlier prerequisites are transitive).

**What it delivers:** The complete app demonstrates the agreed reference appearance and all combined finance journeys on Android 10, with current evidence and any remaining cross-screen defects fixed.

**Parent-spec requirements:** [User stories](spec.md#user-stories) 1-100, particularly 92-94 and 98-100; all [Testing Decisions](spec.md#testing-decisions), [Out of Scope](spec.md#out-of-scope), and [reference roles](spec.md#further-notes). Existing ticket evidence is input, not a substitute for this integrated state.

**Acceptance criteria:**

- Exercise setup, all drawer destinations, active/archive selection persistence, shared month, both list modes/drill-down/filters, ordinary details/creation/editing, both allocation directions, part correction/shared-field change/deletion, sole survivor, Unsplit, independent overlapping Budgets, Transfers/history, and Settings/picker/reset cancellation using public and displayed assertions.
- Specifically verify combined outputs after every feature exists: per-part Category summaries, refreshed Budget cards, main selection changes opened from Budgets, archive effects without Budget history loss, and immutable direction across all routes. Assert values rather than merely finding controls.
- Compare current normal-size screenshots against every named image and document the agreed exceptions only. Fix remaining incorrect layout, spacing, typography, colors, icons, amounts, or placement; retain all adapted secondary views. The result must be the same as, or as close as possible to, the references within agreed changes, not just the same palette.
- Review larger fonts, long Account/Category/Tag/Budget names, unset/empty/invalid drafts, empty selection, contextual errors, and action stacking. No clipped text, obscured rows, unreachable controls, broken gestures, or unsupported/dead controls remain.
- Confirm supported migrations/Backups and combined Transaction split transition reconciliation under existing identity/Merge/atomicity rules. The implementation is not complete if any required lifecycle import case loses/duplicates countable money or silently invents a policy.
- Reuse the passing per-ticket checks as historical evidence and run the full required completion checks against the final combined changes, retaining device/API, reports, counts, captures, and failure evidence. Do not claim manual TalkBack/Switch Access testing or rewrite the previous effort's verification.
- `verifyReferenceLayoutsAndLargerFonts` and an extended complete Android 10 integrated journey cover the combined results. Fix actual integration defects here; do not use this ticket to defer missing ordinary feature behavior, owner tests, or owner screenshot fidelity.

## Publication

Published one numbered issue per approved slice in dependency order, each with Status: ready-for-agent, direct Blocked by entries, parent-spec links/story coverage, observable acceptance criteria, named critical journeys, and the required completion/evidence rules. The new [implementation map](map.md) records the approved graph and frontier. Parent specifications and the old effort's map are unchanged.

Approval covers the breakdown and publication only. Application implementation, migrations, installation, and release remain outside this session. No ticket is claimed or resolved; only 01 is currently dependency-eligible.
