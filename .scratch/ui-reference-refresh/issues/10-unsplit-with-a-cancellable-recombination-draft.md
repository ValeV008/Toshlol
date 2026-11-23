# 10: Unsplit with a cancellable recombination draft

Status: ready-for-agent

Blocked by: 08

Approval: the user approved this breakdown and its blocking edges on 2026-11-23. Publication is documentation only; implementation has not started.

**What to build:** Users open Unsplit from a part, choose the resulting ordinary Transaction's classification/Note, and save or cancel a complete recombination without changing money accidentally.

## Parent-spec requirements

[User stories](../spec.md#user-stories) 76-79, 95-97; [Implementation Decisions](../spec.md#implementation-decisions) for Unsplit, draft boundaries, explicit final metadata, immutable direction, and coherent persistence/Backup transitions.

Also apply the parent specification's [shared stories](../spec.md#user-stories) 1-6 and 92-100 where relevant: visual fidelity, currency/direction clarity, readable large-font/empty/error states, accessible controls, existing-data/Backup preservation, native offline Android 10 behavior, and evidence separate from the prior effort. Follow its [Testing Decisions](../spec.md#testing-decisions), [Out of Scope](../spec.md#out-of-scope), and [reference roles](../spec.md#further-notes).

Prerequisite tickets: [08: Create, view, and back up Transaction splits](08-create-view-and-back-up-transaction-splits.md).

## Observable acceptance criteria

- [ ] Existing part details expose Unsplit instead of Split again. It drafts one ordinary Transaction at the current sum, preserving shared Account/date/fixed direction and allowing explicit Category/Tags/Note choice rather than arbitrary metadata inheritance.
- [ ] Save commits the complete replacement once; balances/monthly money remain unchanged and there are no stale countable parts/markers/relationships. Classification changes affect Category/Tag/Budget results according to the chosen final fields.
- [ ] Cancel/discard/invalid Save/failure leave every original part, field, identity, and relationship intact. No nested allocation or direction conversion is possible.
- [ ] Extend 08's stable transition, migration/serialization where needed, supported Backup reconciliation/conflict handling, and atomic snapshot tests. Use the current sum even if supplied by a coherent supported imported snapshot or later edited group; do not assume the initial allocation total is immutable.
- [ ] `unsplitDraftCommitsOnlyOnSave`, Unsplit cases of `cancelOrFailGroupOperationWithoutPartialChanges`, and `reconcileSplitTransitionsWithoutViolatingMergePolicy` cover public and UI results. Capture the Unsplit entry, final-metadata draft, cancel/validation, and resulting ordinary details at normal and large fonts.

## Verification and completion

- [ ] Exercise the named critical journeys above through the confirmed public Transaction/application-repository, monthly-summary, Account/Budget, and Backup interfaces as applicable. Use Compose accessible semantics for in-app behavior and UI Automator for system document-picker flows. Assert displayed values, committed/reopened snapshots, selection, validation, and cancellation rather than private helpers or direct database tables.
- [ ] Match the owning reference as closely as possible at normal font size, including layout, spacing, typography, colors, generic icons, amounts, and control placement, with only agreed adaptations. Retain the normal/large-font screenshots above; do not defer this slice's fidelity or behavior coverage to integration.
- [ ] Pass the full repository [ticket completion checklist](../../../docs/research/ai-ui-testing.md#ticket-completion) against finished changes: all JVM and Python host-runner tests, Android lint/Kotlin formatting and required builds, both host-driven Android 10 Overview restart checks, and the full unfiltered Android 10 connected suite with no failures, errors, or skipped tests. Focused tests alone do not complete the ticket.
- [ ] Record device/API, check results and test counts, screenshots, reports, failure evidence, and limitations in the ticket's Answer. Leave unresolved if a required check is unavailable or failing. Do not claim manual TalkBack/Switch Access testing that the user declined.

## Delivery boundaries

Implement the approved slice end to end, preserving prerequisites and existing working finance behavior. Reuse existing public boundaries and do small necessary prefactoring before changing the live journey; avoid broad rewrites and screen-specific test interfaces. Later capabilities belong to their owning tickets, and incomplete controls must not be exposed. Publication is documentation only and does not authorize implementation, migrations, installation, or release. Keep the previously implemented effort and its evidence unchanged.
