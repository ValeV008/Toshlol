# 01: Reference navigation shell and Overview

Status: ready-for-agent

Blocked by: None (can start immediately)

Approval: the user approved this breakdown and its blocking edges on 2026-11-23. Publication is documentation only; implementation has not started.

**What to build:** The app opens into the new reference shell and a complete blue-header Overview, with correct existing financial information, six-destination drawer navigation, Settings footer access, and the three working bottom actions.

## Parent-spec requirements

[User stories](../spec.md#user-stories) 1-10, 12, 21-22, 30-32, 98-100; [Implementation Decisions](../spec.md#implementation-decisions) for visual authority, navigation, Overview order, existing finance preservation, and shared scope. [Testing Decisions](../spec.md#testing-decisions) and [reference inventory](../spec.md#further-notes) apply.

Also apply the parent specification's [shared stories](../spec.md#user-stories) 1-6 and 92-100 where relevant: visual fidelity, currency/direction clarity, readable large-font/empty/error states, accessible controls, existing-data/Backup preservation, native offline Android 10 behavior, and evidence separate from the prior effort. Follow its [Testing Decisions](../spec.md#testing-decisions), [Out of Scope](../spec.md#out-of-scope), and [reference roles](../spec.md#further-notes).

## Observable acceptance criteria

- [ ] The left drawer reaches Overview, Expenses, Incomes, Budgets, Categories and Tags, and Accounts; Settings is at its bottom. The old Overview/Expenses/Income navigation tabs disappear without removing access to any required function.
- [ ] Overview uses the agreed blue/off-white/white system and exact section order: combined selected-month Closing balance, monthly Income/Expenses/net, Account cards, Budget progress, recent activity. Existing correct amounts and month context remain unchanged.
- [ ] Add expense, Add income, and Transfer money are visible, labeled, and functional. Shared header/action components accommodate later screen tickets without duplicating financial calculations. Do necessary small prefactoring before switching this live journey.
- [ ] Overview empty selection remains zero/no matching activity/no selection prompt; independently scoped Budget progress may remain nonzero. No fabricated financial records, percentages, or update timestamps appear.
- [ ] Long content and large fonts remain readable and actions do not obscure the final activity row. Other destinations remain usable while their owning refresh tickets are pending.
- [ ] `navigateReferenceShellAndBottomActions` and the Overview part of `preserveSelectedMonthFinanceAndTransferRules` assert displayed values and navigation. Save normal/large-font Overview and left-drawer evidence against the reference roles.

## Verification and completion

- [ ] Exercise the named critical journeys above through the confirmed public Transaction/application-repository, monthly-summary, Account/Budget, and Backup interfaces as applicable. Use Compose accessible semantics for in-app behavior and UI Automator for system document-picker flows. Assert displayed values, committed/reopened snapshots, selection, validation, and cancellation rather than private helpers or direct database tables.
- [ ] Match the owning reference as closely as possible at normal font size, including layout, spacing, typography, colors, generic icons, amounts, and control placement, with only agreed adaptations. Retain the normal/large-font screenshots above; do not defer this slice's fidelity or behavior coverage to integration.
- [ ] Pass the full repository [ticket completion checklist](../../../docs/research/ai-ui-testing.md#ticket-completion) against finished changes: all JVM and Python host-runner tests, Android lint/Kotlin formatting and required builds, both host-driven Android 10 Overview restart checks, and the full unfiltered Android 10 connected suite with no failures, errors, or skipped tests. Focused tests alone do not complete the ticket.
- [ ] Record device/API, check results and test counts, screenshots, reports, failure evidence, and limitations in the ticket's Answer. Leave unresolved if a required check is unavailable or failing. Do not claim manual TalkBack/Switch Access testing that the user declined.

## Delivery boundaries

Implement the approved slice end to end, preserving prerequisites and existing working finance behavior. Reuse existing public boundaries and do small necessary prefactoring before changing the live journey; avoid broad rewrites and screen-specific test interfaces. Later capabilities belong to their owning tickets, and incomplete controls must not be exposed. Publication is documentation only and does not authorize implementation, migrations, installation, or release. Keep the previously implemented effort and its evidence unchanged.
