# 11: Reference Categories and Tags management

Status: ready-for-agent

Blocked by: 01

Approval: the user approved this breakdown and its blocking edges on 2026-11-23. Publication is documentation only; implementation has not started.

**What to build:** The Categories and Tags destination uses separate tabs and the neutral reference style while existing creation/edit/archive and classification compatibility work end to end.

## Parent-spec requirements

[User stories](../spec.md#user-stories) 4, 52-53, 88; [Implementation Decisions](../spec.md#implementation-decisions) for neutral management screens, text-only direction-specific classification, archived reference preservation, and compatible Tags.

Also apply the parent specification's [shared stories](../spec.md#user-stories) 1-6 and 92-100 where relevant: visual fidelity, currency/direction clarity, readable large-font/empty/error states, accessible controls, existing-data/Backup preservation, native offline Android 10 behavior, and evidence separate from the prior effort. Follow its [Testing Decisions](../spec.md#testing-decisions), [Out of Scope](../spec.md#out-of-scope), and [reference roles](../spec.md#further-notes).

Prerequisite tickets: [01: Reference navigation shell and Overview](01-reference-navigation-shell-and-overview.md).

## Observable acceptance criteria

- [ ] Provide blue header, white lists/forms, separate Categories/Tags tabs, and functioning create/edit/archive paths without adding Category artwork or an icon picker.
- [ ] Keep Income/Expense Category distinction and optional Tag/Category association. Do not confuse choosing a Category's direction in classification management with forbidden direction conversion of an existing Transaction.
- [ ] Renames/archives remain intelligible on existing Transactions/parts and Budget scopes; compatible choices/defaults remain correct in whichever Transaction editor version is present. No archived restriction is silently broadened or dropped.
- [ ] `operateAdaptedSecondaryFlows` covers both tabs, saved/reloaded classification changes, history references, and validation. Retain relevant existing classification tests; capture normal/large-font/empty/error layouts within this slice.

## Verification and completion

- [ ] Exercise the named critical journeys above through the confirmed public Transaction/application-repository, monthly-summary, Account/Budget, and Backup interfaces as applicable. Use Compose accessible semantics for in-app behavior and UI Automator for system document-picker flows. Assert displayed values, committed/reopened snapshots, selection, validation, and cancellation rather than private helpers or direct database tables.
- [ ] Match the owning reference as closely as possible at normal font size, including layout, spacing, typography, colors, generic icons, amounts, and control placement, with only agreed adaptations. Retain the normal/large-font screenshots above; do not defer this slice's fidelity or behavior coverage to integration.
- [ ] Pass the full repository [ticket completion checklist](../../../docs/research/ai-ui-testing.md#ticket-completion) against finished changes: all JVM and Python host-runner tests, Android lint/Kotlin formatting and required builds, both host-driven Android 10 Overview restart checks, and the full unfiltered Android 10 connected suite with no failures, errors, or skipped tests. Focused tests alone do not complete the ticket.
- [ ] Record device/API, check results and test counts, screenshots, reports, failure evidence, and limitations in the ticket's Answer. Leave unresolved if a required check is unavailable or failing. Do not claim manual TalkBack/Switch Access testing that the user declined.

## Delivery boundaries

Implement the approved slice end to end, preserving prerequisites and existing working finance behavior. Reuse existing public boundaries and do small necessary prefactoring before changing the live journey; avoid broad rewrites and screen-specific test interfaces. Later capabilities belong to their owning tickets, and incomplete controls must not be exposed. Publication is documentation only and does not authorize implementation, migrations, installation, or release. Keep the previously implemented effort and its evidence unchanged.
