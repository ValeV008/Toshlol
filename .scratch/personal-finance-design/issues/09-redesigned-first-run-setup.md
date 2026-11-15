# 09: Redesigned first-run setup

Status: ready-for-agent
Blocked by: 01

**What to build:** Complete first-run currency and first-Account setup in the shared visual style, then arrive at the month-aware Overview with no Accounts selected, retaining valid setup and display selection across recreation and relaunch.

## Parent-spec requirements

- [User stories](../spec.md#user-stories): Stories 1-6, 83-85: visually consistent, accessible larger-font, offline Android 10 first-run experience.
- [User stories](../spec.md#user-stories): Stories 9, 16-20, 81: current initial month, explicit currency/first-Account setup without seeds, empty initial drawer selection, zero totals without a selection prompt, and persisted state.
- [Implementation decisions](../spec.md#implementation-decisions): Preserve existing setup validation, loading/error/retry behavior, atomic first-Account/currency persistence, and unsubmitted-form recreation behavior. Setup-created Accounts must not be confused with the independently persisted drawer selection introduced by ticket 01.
- [Testing decisions](../spec.md#testing-decisions): use existing public finance use-case/repository and Backup interfaces, Compose semantics, and UI Automator for system UI; test visible outcomes rather than private helpers or tables. Named tests below are proposed or extensions of existing behavior tests, not claims of passing implementation.

## Observable acceptance criteria

- [ ] First-run currency and first-Account controls, progress/loading, validation, and retry states use shared typography, colors, surfaces, and accessible labels without adding seeded finance data.
- [ ] Setup still requires an allowed currency and valid first Account before entering the ledger. Repeated/concurrent submission does not create duplicate Accounts; errors leave a readable retryable state without partial completed setup.
- [ ] Recreation preserves an unsubmitted setup form under the existing behavior. After successful creation, app relaunch restores completed setup rather than forcing setup again or changing the selected currency.
- [ ] The first Overview opens on the current local month with an empty drawer selection and zero ledger totals and no Select Accounts prompt. The newly created Account is available for an explicit drawer switch and for mandatory Transaction Account selection, without automatically selecting it in the ledger filter.
- [ ] Once the user selects or clears Accounts, a later app restart preserves exactly that saved display selection instead of treating setup completion as a reason to select all or the first Account.
- [ ] Run existing public setup/ViewModel tests extended for the new visual state and Android 10 setup/recreation/relaunch journeys, lint, and formatting. Capture setup, validation/retry, and initial empty Overview examples at normal and larger fonts.

## Named behavior tests

| Test or journey | Observable result |
| --- | --- |
| createFirstAccountAndKeepOverviewAcrossRelaunch | Extend the existing journey to assert completed setup, currency, current-month initial view, and empty Account filter after restart. |
| requireCurrencyAndAccountNameBeforeContinuing | Assert labeled validation and no partial setup or seeded data. |
| retainUnsubmittedFormAcrossActivityRecreation | Retain entered setup choices independently of completed setup/display filtering. |
| completeSetupWithoutAutoSelectingAccounts | Assert the new Account exists for explicit selection/entry while drawer switches are off and totals remain zero without a prompt. |
| retainExplicitDrawerSelectionAfterSetupRelaunch | Assert both nonempty and empty saved selections survive subsequent starts. |
| operateFirstRunAtLargeFontAndRetryFailure | Assert accessible choices, loading/error/retry behavior, and usable form layout. |

## Delivery boundaries

The blocker is the shared visual foundation and empty-selection Overview behavior from ticket 01. Do not introduce onboarding profiles, default Accounts/Categories, network requirements, or additional setup decisions.

Each slice must remain independently demoable and keep existing callers and relevant checks working. Include the feature's behavior and UI verification in this ticket rather than deferring them to the final integration ticket. Follow the parent spec's exclusions and existing ADRs.
