# 08: Create, view, and back up Transaction splits

Status: ready-for-agent

Blocked by: 04, 05

Approval: the user approved this breakdown and its blocking edges on 2026-11-23. Publication is documentation only; implementation has not started.

**What to build:** An ordinary Expense or Income can be allocated into two or more durable, independently classified parts, seen as separate marked rows and related details, counted once, and preserved through supported Backup workflows.

## Parent-spec requirements

[User stories](../spec.md#user-stories) 60-70, 78-81, 95-97; [Implementation Decisions](../spec.md#implementation-decisions) for Transaction split creation, identity/membership, exact initial money, reference form, related reads, part-aware projections/matching, and compatible atomic persistence/Backup.

Also apply the parent specification's [shared stories](../spec.md#user-stories) 1-6 and 92-100 where relevant: visual fidelity, currency/direction clarity, readable large-font/empty/error states, accessible controls, existing-data/Backup preservation, native offline Android 10 behavior, and evidence separate from the prior effort. Follow its [Testing Decisions](../spec.md#testing-decisions), [Out of Scope](../spec.md#out-of-scope), and [reference roles](../spec.md#further-notes).

Prerequisite tickets: [04: Reference Transaction editors and picker flows](04-reference-transaction-editors-and-picker-flows.md), [05: Reference By date Expense and Income lists](05-reference-by-date-expense-and-income-lists.md).

## Observable acceptance criteria

- [ ] Before mutation, document the concrete stable-identity/group-membership, migration/versioning, and Backup reconciliation contract for initial allocation and later ordinary/part transitions. It must support the specified future edits/deletion/Unsplit without violating immutable direction, no-deletion-propagation Merge, conflict rejection, or atomicity. Do not invent a different product policy or discard previous finance/Budget history.
- [ ] Ordinary details expose Split for both directions. Follow the supplied Split form: shared Account/date, two initial stacked part cards, Amount/Category/Tags, remaining total, Add part, per-card remove, and Aa Note. Reuse the confirmed picker/keypad behavior and correct red/green styling.
- [ ] Save requires at least two positive parts exactly totaling the original; allow incomplete drafts but reject incomplete/invalid Save without mutation. Cancel, discard, storage failure, and validation failure retain the original complete Transaction and ledger.
- [ ] Persist/reload the relationship, separate marked rows, own part values, current combined total, and tappable related details. No extra original/group movement remains countable. Public balances, monthly/day totals, Category/Tag filters, and Budget restrictions count matching parts once; EUR 40/20 gives the required EUR 60 total and per-scope amounts.
- [ ] If By category or refreshed Budget cards have landed, verify their combined part-aware outputs now; if not, existing public matching/filtering reads must already be correct and those presentation tickets inherit the integration obligation.
- [ ] Add and test explicit non-destructive upgrade and supported Backup DTO/validation/export/import handling in this slice. Retain supported older Backups/legacy Budget provenance, idempotent supported Merge, reference/conflict rejection, and complete Overwrite/cancellation/rollback. Never infer groups from equal amounts/dates or silently duplicate money in incompatible Merge outcomes.
- [ ] Until 09/10 supply grouped mutations, do not allow new parts to fall through ordinary single-record Account/date/delete logic or expose nonfunctional mutation controls. Ordinary Transaction editing still works; allocation creation, viewing, and Backup behavior are complete and independently demoable.
- [ ] `createExactTransactionSplitForBothDirections`, `showSeparatePartsAndRelatedDetails`, `matchPartsToCategoryFiltersAndBudgets`, initial `cancelOrFailGroupOperationWithoutPartialChanges`, and `upgradeAndRoundTripTransactionSplitData` cover public results, reopen/migration, and supported Backup states. Capture both creation directions, marked rows/details, per-part Notes, validation, and large fonts.

## Verification and completion

- [ ] Exercise the named critical journeys above through the confirmed public Transaction/application-repository, monthly-summary, Account/Budget, and Backup interfaces as applicable. Use Compose accessible semantics for in-app behavior and UI Automator for system document-picker flows. Assert displayed values, committed/reopened snapshots, selection, validation, and cancellation rather than private helpers or direct database tables.
- [ ] Match the owning reference as closely as possible at normal font size, including layout, spacing, typography, colors, generic icons, amounts, and control placement, with only agreed adaptations. Retain the normal/large-font screenshots above; do not defer this slice's fidelity or behavior coverage to integration.
- [ ] Pass the full repository [ticket completion checklist](../../../docs/research/ai-ui-testing.md#ticket-completion) against finished changes: all JVM and Python host-runner tests, Android lint/Kotlin formatting and required builds, both host-driven Android 10 Overview restart checks, and the full unfiltered Android 10 connected suite with no failures, errors, or skipped tests. Focused tests alone do not complete the ticket.
- [ ] Record device/API, check results and test counts, screenshots, reports, failure evidence, and limitations in the ticket's Answer. Leave unresolved if a required check is unavailable or failing. Do not claim manual TalkBack/Switch Access testing that the user declined.

## Delivery boundaries

Implement the approved slice end to end, preserving prerequisites and existing working finance behavior. Reuse existing public boundaries and do small necessary prefactoring before changing the live journey; avoid broad rewrites and screen-specific test interfaces. Later capabilities belong to their owning tickets, and incomplete controls must not be exposed. Publication is documentation only and does not authorize implementation, migrations, installation, or release. Keep the previously implemented effort and its evidence unchanged.
