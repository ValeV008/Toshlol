# 04: Reference Transaction editors and picker flows

Status: ready-for-agent

Blocked by: 03

Approval: the user approved this breakdown and its blocking edges on 2026-11-23. Publication is documentation only; implementation has not started.

**What to build:** Users create and edit ordinary Income and Expense through the screenshot-based amount, colored picker rows, Note editor, and consistent save/cancel flow.

## Parent-spec requirements

[User stories](../spec.md#user-stories) 45-59; [Implementation Decisions](../spec.md#implementation-decisions) for reference editor layout, active Account/defaults, classification compatibility, keyboard/pickers, and dirty drafts.

Also apply the parent specification's [shared stories](../spec.md#user-stories) 1-6 and 92-100 where relevant: visual fidelity, currency/direction clarity, readable large-font/empty/error states, accessible controls, existing-data/Backup preservation, native offline Android 10 behavior, and evidence separate from the prior effort. Follow its [Testing Decisions](../spec.md#testing-decisions), [Out of Scope](../spec.md#out-of-scope), and [reference roles](../spec.md#further-notes).

Prerequisite tickets: [03: Transaction details and immutable direction](03-transaction-details-and-immutable-direction.md).

## Observable acceptance criteria

- [ ] The two editors reproduce the red/green amount and Category/Tags/Account/Date hierarchy as closely as possible. Currency is display-only; there are no unsupported location/recurrence/reminder/photo controls or direction selector.
- [ ] Amount opens the numeric keypad; Aa opens the optional Note editor; Category/Tags/Account open bottom sheets; Date opens a calendar. Category None and multiple compatible Tags work without Category-specific artwork.
- [ ] Account is mandatory/active, defaults to the most-recently-used active Account on creation, and loads the original on edit. Date defaults to today's local date. Editing another field never moves money to a primary Account.
- [ ] One top-right save check, top-left cancel, Back/dismiss discard behavior, and dirty-draft confirmation work consistently. Invalid positive-amount/date/classification choices show field errors without losing the draft.
- [ ] A saved ordinary Transaction is observable in details, monthly money, and relevant Budget reads; reopen/restart preserves it. Preserve archived classification references under existing edit rules while preventing new invalid assignments.
- [ ] `operateReferenceEntryPickersAndDrafts` plus `inspectDetailsThenEditWithFixedDirection` verify public/screen outcomes. Capture both editors, picker/calendar/Note, empty/validation states, and large-font layouts; do not defer their appearance to final integration.

## Verification and completion

- [ ] Exercise the named critical journeys above through the confirmed public Transaction/application-repository, monthly-summary, Account/Budget, and Backup interfaces as applicable. Use Compose accessible semantics for in-app behavior and UI Automator for system document-picker flows. Assert displayed values, committed/reopened snapshots, selection, validation, and cancellation rather than private helpers or direct database tables.
- [ ] Match the owning reference as closely as possible at normal font size, including layout, spacing, typography, colors, generic icons, amounts, and control placement, with only agreed adaptations. Retain the normal/large-font screenshots above; do not defer this slice's fidelity or behavior coverage to integration.
- [ ] Pass the full repository [ticket completion checklist](../../../docs/research/ai-ui-testing.md#ticket-completion) against finished changes: all JVM and Python host-runner tests, Android lint/Kotlin formatting and required builds, both host-driven Android 10 Overview restart checks, and the full unfiltered Android 10 connected suite with no failures, errors, or skipped tests. Focused tests alone do not complete the ticket.
- [ ] Record device/API, check results and test counts, screenshots, reports, failure evidence, and limitations in the ticket's Answer. Leave unresolved if a required check is unavailable or failing. Do not claim manual TalkBack/Switch Access testing that the user declined.

## Delivery boundaries

Implement the approved slice end to end, preserving prerequisites and existing working finance behavior. Reuse existing public boundaries and do small necessary prefactoring before changing the live journey; avoid broad rewrites and screen-specific test interfaces. Later capabilities belong to their owning tickets, and incomplete controls must not be exposed. Publication is documentation only and does not authorize implementation, migrations, installation, or release. Keep the previously implemented effort and its evidence unchanged.
