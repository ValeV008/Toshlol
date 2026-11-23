# 13: Reference setup, Settings, and data management

Status: ready-for-agent

Blocked by: 01

Approval: the user approved this breakdown and its blocking edges on 2026-11-23. Publication is documentation only; implementation has not started.

**What to build:** First-run setup and Settings follow the new style, with currency/first Account choices and complete supported Export/Import/reset flows accessible from the new navigation.

## Parent-spec requirements

[User stories](../spec.md#user-stories) 8, 48, 90-91, 95-98; [Implementation Decisions](../spec.md#implementation-decisions) for neutral setup/Settings, existing currency policy, offline operation, supported Backups, and confirmations.

Also apply the parent specification's [shared stories](../spec.md#user-stories) 1-6 and 92-100 where relevant: visual fidelity, currency/direction clarity, readable large-font/empty/error states, accessible controls, existing-data/Backup preservation, native offline Android 10 behavior, and evidence separate from the prior effort. Follow its [Testing Decisions](../spec.md#testing-decisions), [Out of Scope](../spec.md#out-of-scope), and [reference roles](../spec.md#further-notes).

Prerequisite tickets: [01: Reference navigation shell and Overview](01-reference-navigation-shell-and-overview.md).

## Observable acceptance criteria

- [ ] First-run setup uses blue/white forms, requires the existing explicit currency and first Account choices, preserves signed Opening balance validation, and seeds no financial records.
- [ ] Settings-footer navigation reaches configured currency and Export/Import/reset. Retain the current supported currency behavior and all Overwrite/reset and relevant cancellation confirmations; ordinary entry currency remains display-only.
- [ ] Export, supported Merge/Overwrite, cancelled pickers, invalid files, and reset work through existing public Backup/system-picker boundaries with readable results and no lost draft/data. New Transaction split serialization remains owned by 08-10; this slice must not redesign the Backup policy.
- [ ] Remain offline on Android 10 with no new permission/cloud requirement. Empty/error/long-label states and large-font scrolling expose every action.
- [ ] `operateAdaptedSecondaryFlows` plus the existing public Backup and UI Automator picker journeys verify these flows. Capture setup, Settings, success/error/cancel/confirmation, and large-font examples.

## Verification and completion

- [ ] Exercise the named critical journeys above through the confirmed public Transaction/application-repository, monthly-summary, Account/Budget, and Backup interfaces as applicable. Use Compose accessible semantics for in-app behavior and UI Automator for system document-picker flows. Assert displayed values, committed/reopened snapshots, selection, validation, and cancellation rather than private helpers or direct database tables.
- [ ] Match the owning reference as closely as possible at normal font size, including layout, spacing, typography, colors, generic icons, amounts, and control placement, with only agreed adaptations. Retain the normal/large-font screenshots above; do not defer this slice's fidelity or behavior coverage to integration.
- [ ] Pass the full repository [ticket completion checklist](../../../docs/research/ai-ui-testing.md#ticket-completion) against finished changes: all JVM and Python host-runner tests, Android lint/Kotlin formatting and required builds, both host-driven Android 10 Overview restart checks, and the full unfiltered Android 10 connected suite with no failures, errors, or skipped tests. Focused tests alone do not complete the ticket.
- [ ] Record device/API, check results and test counts, screenshots, reports, failure evidence, and limitations in the ticket's Answer. Leave unresolved if a required check is unavailable or failing. Do not claim manual TalkBack/Switch Access testing that the user declined.

## Delivery boundaries

Implement the approved slice end to end, preserving prerequisites and existing working finance behavior. Reuse existing public boundaries and do small necessary prefactoring before changing the live journey; avoid broad rewrites and screen-specific test interfaces. Later capabilities belong to their owning tickets, and incomplete controls must not be exposed. Publication is documentation only and does not authorize implementation, migrations, installation, or release. Keep the previously implemented effort and its evidence unchanged.
