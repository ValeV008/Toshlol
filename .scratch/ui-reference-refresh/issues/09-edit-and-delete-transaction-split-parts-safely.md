# 09: Edit and delete Transaction split parts safely

Status: ready-for-agent

Blocked by: 08

Approval: the user approved this breakdown and its blocking edges on 2026-11-23. Publication is documentation only; implementation has not started.

**What to build:** Users correct a part's amount/classification/Note, move the complete group to another Account/date, or delete a selected part without leaving inconsistent relationships or financial totals.

## Parent-spec requirements

[User stories](../spec.md#user-stories) 62, 71-75, 79-81, 95-97; [Implementation Decisions](../spec.md#implementation-decisions) for per-part mutations, current totals, shared fields, sole-survivor normalization, archives, and atomic Backup-aware transitions.

Also apply the parent specification's [shared stories](../spec.md#user-stories) 1-6 and 92-100 where relevant: visual fidelity, currency/direction clarity, readable large-font/empty/error states, accessible controls, existing-data/Backup preservation, native offline Android 10 behavior, and evidence separate from the prior effort. Follow its [Testing Decisions](../spec.md#testing-decisions), [Out of Scope](../spec.md#out-of-scope), and [reference roles](../spec.md#further-notes).

Prerequisite tickets: [08: Create, view, and back up Transaction splits](08-create-view-and-back-up-transaction-splits.md).

## Observable acceptance criteria

- [ ] Editing amount/Category/Tags/Note affects only the opened part. EUR 40 -> EUR 45 in a EUR 40 + EUR 20 group becomes EUR 65; balances, group details, directional lists, filters, and matching Budget spending all reflect the correction once.
- [ ] Account/date changes state their all-parts effect and update the group atomically, including balances and affected month views. Reject archived/invalid targets without partial changes. Direction remains immutable through UI and public operations.
- [ ] Confirmed deletion removes only the selected part; cancellation changes nothing. When one remains, it is an ordinary Transaction without stale membership/marker and can use ordinary Split behavior. Archived history stays read-only.
- [ ] Extend 08's supported persistence/Backup transition behavior in the same slice; stable unaffected identities, coherent snapshots, no deletion propagation, supported repeated Merge, explicit conflict rejection, and rollback remain covered. Do not defer these cases to a final Backup ticket.
- [ ] `editPartAndRecalculateCurrentTotal`, `changeSharedAccountOrDateAtomically`, `deletePartAndNormalizeSoleSurvivor`, and mutation cases of `reconcileSplitTransitionsWithoutViolatingMergePolicy` assert reopened/public snapshots and displayed results. Capture edited/group-shared-field/deletion/sole-survivor states, including large fonts.

## Verification and completion

- [ ] Exercise the named critical journeys above through the confirmed public Transaction/application-repository, monthly-summary, Account/Budget, and Backup interfaces as applicable. Use Compose accessible semantics for in-app behavior and UI Automator for system document-picker flows. Assert displayed values, committed/reopened snapshots, selection, validation, and cancellation rather than private helpers or direct database tables.
- [ ] Match the owning reference as closely as possible at normal font size, including layout, spacing, typography, colors, generic icons, amounts, and control placement, with only agreed adaptations. Retain the normal/large-font screenshots above; do not defer this slice's fidelity or behavior coverage to integration.
- [ ] Pass the full repository [ticket completion checklist](../../../docs/research/ai-ui-testing.md#ticket-completion) against finished changes: all JVM and Python host-runner tests, Android lint/Kotlin formatting and required builds, both host-driven Android 10 Overview restart checks, and the full unfiltered Android 10 connected suite with no failures, errors, or skipped tests. Focused tests alone do not complete the ticket.
- [ ] Record device/API, check results and test counts, screenshots, reports, failure evidence, and limitations in the ticket's Answer. Leave unresolved if a required check is unavailable or failing. Do not claim manual TalkBack/Switch Access testing that the user declined.

## Delivery boundaries

Implement the approved slice end to end, preserving prerequisites and existing working finance behavior. Reuse existing public boundaries and do small necessary prefactoring before changing the live journey; avoid broad rewrites and screen-specific test interfaces. Later capabilities belong to their owning tickets, and incomplete controls must not be exposed. Publication is documentation only and does not authorize implementation, migrations, installation, or release. Keep the previously implemented effort and its evidence unchanged.
