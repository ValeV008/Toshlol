# 12: Reference Transfer entry and related history

Status: ready-for-agent

Blocked by: 01

Approval: the user approved this breakdown and its blocking edges on 2026-11-23. Publication is documentation only; implementation has not started.

**What to build:** Users create, inspect/edit, and confirm deletion of Transfers through the adapted amount/From/To/Date flow, with correct paired activity and balances.

## Parent-spec requirements

[User stories](../spec.md#user-stories) 10, 41, 89, 95; [Implementation Decisions](../spec.md#implementation-decisions) for distinct Transfers, explicit endpoints, amount/date entry, archive restrictions, and no classification fields.

Also apply the parent specification's [shared stories](../spec.md#user-stories) 1-6 and 92-100 where relevant: visual fidelity, currency/direction clarity, readable large-font/empty/error states, accessible controls, existing-data/Backup preservation, native offline Android 10 behavior, and evidence separate from the prior effort. Follow its [Testing Decisions](../spec.md#testing-decisions), [Out of Scope](../spec.md#out-of-scope), and [reference roles](../spec.md#further-notes).

Prerequisite tickets: [01: Reference navigation shell and Overview](01-reference-navigation-shell-and-overview.md).

## Observable acceptance criteria

- [ ] Transfer money opens reference-style large amount and From Account / To Account / Date controls, configured currency, usable save/cancel/validation, and compatible normal/large-font layouts. Do not add Category, Tags, Note, per-Transfer currency, or Transaction-conversion controls.
- [ ] Require positive precise amount and two explicit distinct active endpoints; preserve existing edit/delete confirmations, identity, validation, and read-only archived history behavior.
- [ ] Saving/reopening reflects correct endpoint balances and related directional/Account-history rows, with one Overview activity item and no Income/Expense/net/Category/Budget inflation. Preserve the current source/destination membership rules.
- [ ] `operateAdaptedSecondaryFlows` and `preserveSelectedMonthFinanceAndTransferRules` cover actual public/displayed amounts and validation. Retain existing Transfer persistence/Backup coverage and capture entry, invalid endpoints, paired activity, and history.

## Verification and completion

- [ ] Exercise the named critical journeys above through the confirmed public Transaction/application-repository, monthly-summary, Account/Budget, and Backup interfaces as applicable. Use Compose accessible semantics for in-app behavior and UI Automator for system document-picker flows. Assert displayed values, committed/reopened snapshots, selection, validation, and cancellation rather than private helpers or direct database tables.
- [ ] Match the owning reference as closely as possible at normal font size, including layout, spacing, typography, colors, generic icons, amounts, and control placement, with only agreed adaptations. Retain the normal/large-font screenshots above; do not defer this slice's fidelity or behavior coverage to integration.
- [ ] Pass the full repository [ticket completion checklist](../../../docs/research/ai-ui-testing.md#ticket-completion) against finished changes: all JVM and Python host-runner tests, Android lint/Kotlin formatting and required builds, both host-driven Android 10 Overview restart checks, and the full unfiltered Android 10 connected suite with no failures, errors, or skipped tests. Focused tests alone do not complete the ticket.
- [ ] Record device/API, check results and test counts, screenshots, reports, failure evidence, and limitations in the ticket's Answer. Leave unresolved if a required check is unavailable or failing. Do not claim manual TalkBack/Switch Access testing that the user declined.

## Delivery boundaries

Implement the approved slice end to end, preserving prerequisites and existing working finance behavior. Reuse existing public boundaries and do small necessary prefactoring before changing the live journey; avoid broad rewrites and screen-specific test interfaces. Later capabilities belong to their owning tickets, and incomplete controls must not be exposed. Publication is documentation only and does not authorize implementation, migrations, installation, or release. Keep the previously implemented effort and its evidence unchanged.
