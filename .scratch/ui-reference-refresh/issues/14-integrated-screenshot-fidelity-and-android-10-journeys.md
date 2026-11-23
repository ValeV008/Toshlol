# 14: Integrated screenshot fidelity and Android 10 journeys

Status: ready-for-agent

Blocked by: 06, 07, 09, 10, 11, 12, 13

Approval: the user approved this breakdown and its blocking edges on 2026-11-23. Publication is documentation only; implementation has not started.

**What to build:** The complete app demonstrates the agreed reference appearance and all combined finance journeys on Android 10, with current evidence and any remaining cross-screen defects fixed.

## Parent-spec requirements

[User stories](../spec.md#user-stories) 1-100, particularly 92-94 and 98-100; all [Testing Decisions](../spec.md#testing-decisions), [Out of Scope](../spec.md#out-of-scope), and [reference roles](../spec.md#further-notes). Existing ticket evidence is input, not a substitute for this integrated state.

Also apply the parent specification's [shared stories](../spec.md#user-stories) 1-6 and 92-100 where relevant: visual fidelity, currency/direction clarity, readable large-font/empty/error states, accessible controls, existing-data/Backup preservation, native offline Android 10 behavior, and evidence separate from the prior effort. Follow its [Testing Decisions](../spec.md#testing-decisions), [Out of Scope](../spec.md#out-of-scope), and [reference roles](../spec.md#further-notes).

Prerequisite tickets: [06: By category summaries and activity drill-down](06-by-category-summaries-and-activity-drill-down.md), [07: Reference Budget cards and separate editing](07-reference-budget-cards-and-separate-editing.md), [09: Edit and delete Transaction split parts safely](09-edit-and-delete-transaction-split-parts-safely.md), [10: Unsplit with a cancellable recombination draft](10-unsplit-with-a-cancellable-recombination-draft.md), [11: Reference Categories and Tags management](11-reference-categories-and-tags-management.md), [12: Reference Transfer entry and related history](12-reference-transfer-entry-and-related-history.md), [13: Reference setup, Settings, and data management](13-reference-setup-settings-and-data-management.md).

## Observable acceptance criteria

- [ ] Exercise setup, all drawer destinations, active/archive selection persistence, shared month, both list modes/drill-down/filters, ordinary details/creation/editing, both allocation directions, part correction/shared-field change/deletion, sole survivor, Unsplit, independent overlapping Budgets, Transfers/history, and Settings/picker/reset cancellation using public and displayed assertions.
- [ ] Specifically verify combined outputs after every feature exists: per-part Category summaries, refreshed Budget cards, main selection changes opened from Budgets, archive effects without Budget history loss, and immutable direction across all routes. Assert values rather than merely finding controls.
- [ ] Compare current normal-size screenshots against every named image and document the agreed exceptions only. Fix remaining incorrect layout, spacing, typography, colors, icons, amounts, or placement; retain all adapted secondary views. The result must be the same as, or as close as possible to, the references within agreed changes, not just the same palette.
- [ ] Review larger fonts, long Account/Category/Tag/Budget names, unset/empty/invalid drafts, empty selection, contextual errors, and action stacking. No clipped text, obscured rows, unreachable controls, broken gestures, or unsupported/dead controls remain.
- [ ] Confirm supported migrations/Backups and combined Transaction split transition reconciliation under existing identity/Merge/atomicity rules. The implementation is not complete if any required lifecycle import case loses/duplicates countable money or silently invents a policy.
- [ ] Reuse the passing per-ticket checks as historical evidence and run the full required completion checks against the final combined changes, retaining device/API, reports, counts, captures, and failure evidence. Do not claim manual TalkBack/Switch Access testing or rewrite the previous effort's verification.
- [ ] `verifyReferenceLayoutsAndLargerFonts` and an extended complete Android 10 integrated journey cover the combined results. Fix actual integration defects here; do not use this ticket to defer missing ordinary feature behavior, owner tests, or owner screenshot fidelity.

## Verification and completion

- [ ] Exercise the named critical journeys above through the confirmed public Transaction/application-repository, monthly-summary, Account/Budget, and Backup interfaces as applicable. Use Compose accessible semantics for in-app behavior and UI Automator for system document-picker flows. Assert displayed values, committed/reopened snapshots, selection, validation, and cancellation rather than private helpers or direct database tables.
- [ ] Match the owning reference as closely as possible at normal font size, including layout, spacing, typography, colors, generic icons, amounts, and control placement, with only agreed adaptations. Retain the normal/large-font screenshots above; do not defer this slice's fidelity or behavior coverage to integration.
- [ ] Pass the full repository [ticket completion checklist](../../../docs/research/ai-ui-testing.md#ticket-completion) against finished changes: all JVM and Python host-runner tests, Android lint/Kotlin formatting and required builds, both host-driven Android 10 Overview restart checks, and the full unfiltered Android 10 connected suite with no failures, errors, or skipped tests. Focused tests alone do not complete the ticket.
- [ ] Record device/API, check results and test counts, screenshots, reports, failure evidence, and limitations in the ticket's Answer. Leave unresolved if a required check is unavailable or failing. Do not claim manual TalkBack/Switch Access testing that the user declined.

## Delivery boundaries

Implement the approved slice end to end, preserving prerequisites and existing working finance behavior. Reuse existing public boundaries and do small necessary prefactoring before changing the live journey; avoid broad rewrites and screen-specific test interfaces. Later capabilities belong to their owning tickets, and incomplete controls must not be exposed. Publication is documentation only and does not authorize implementation, migrations, installation, or release. Keep the previously implemented effort and its evidence unchanged.
