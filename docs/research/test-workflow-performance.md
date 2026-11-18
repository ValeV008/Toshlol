# Android test workflow performance

Research date: 2026-11-18. Scope: why the approximately 60 Android tests run repeatedly per ticket, and which changes would shorten feedback without losing the required Android 10 checks. The initial investigation used existing results without running tests. The [agreed scope](#agreed-scope-after-research) records subsequent design decisions; the [implementation](#implementation) records changes and their verification separately from the historical measurements below.

## Findings

The largest immediate saving is to stop starting the entire connected suite during each edit cycle and then starting it again on push. Use focused methods or classes while implementing, and let one full run validate the finished revision. At the time of research, the repository already described avoiding manual verification immediately before the pre-push gate, but saved logs show the full-run/then-push pattern still occurred. The agreed replacement puts the full local gate at ticket completion and removes the pre-push hook; see [the current execution loop](ai-ui-testing.md#execution-loop).

This is principally repeated device execution, rather than repeated compilation: a recorded pre-push run executed only one Gradle task while 68 tasks were up to date, yet still took 11 minutes. Build caching can reduce preparation work; it cannot be assumed to eliminate a connected-device run.

### Evidence and limits

**Repository facts observed before implementation:**

- [`.githooks/pre-commit`](../../.githooks/pre-commit) invoked `testDebugUnitTest lintDebug ktlintCheck`; `.githooks/pre-push` invoked the full `connectedDebugAndroidTest` suite. `core.hooksPath` was configured as `.githooks` in this clone. Neither hook filtered by ticket or changed behavior. The pre-commit hook is retained; the pre-push hook is removed by the implementation below.
- [The workflow](../../.github/workflows/android.yml) subscribes to both `push` and `pull_request` and runs the connected suite. An update to a branch with an open pull request can therefore cause two CI workflow executions, in addition to local execution. This is a trigger overlap inferred from the configuration; CI run history was not inspected.
- [The build](../../app/build.gradle.kts) has one app module and uses `AndroidJUnitRunner`; it defines no managed devices, sharding, or Android Test Orchestrator. [The version catalog](../../gradle/libs.versions.toml) pins AGP 9.1.1; [the wrapper](../../gradle/wrapper/gradle-wrapper.properties) pins Gradle 9.3.1. [Project properties](../../gradle.properties) do not enable the build or configuration cache; user-level settings were not audited.
- Android 10/API 29 is the required environment in [ADR 0002](../adr/0002-native-android-boundary.md). Newer API coverage is optional, so adding a device matrix would add work beyond the current requirement.

**Measured historical artifacts, not fresh benchmark results:** the following excerpts are preserved here because ignored `build/` logs and reports are transient.

| Saved local log | Result | Gradle task summary |
| --- | --- | --- |
| `build/issue-02-full-suite.log` | 58 tests; `BUILD SUCCESSFUL in 11m 3s` | 69 actionable: 6 executed, 63 up-to-date |
| `build/push-android-journeys.log` | Hook run, 58 tests; `BUILD SUCCESSFUL in 11m 5s` | 69 actionable: 1 executed, 68 up-to-date |
| `build/test-fix-final-checks.log` | 51 tests; `BUILD SUCCESSFUL in 7m 20s` | JVM tests up-to-date |
| `build/test-fix-push.log` | Hook run, 51 tests; `BUILD SUCCESSFUL in 8m 23s` | 69 actionable: 1 executed, 68 up-to-date |

These establish repeated full-suite execution around the same work. They do **not** establish identical source/APK/environment fingerprints between runs, or prove every repeated run was unnecessary. The older counts differ because these are historical runs.

The latest local XML report, `app/build/outputs/androidTest-results/connected/debug/TEST-xperia_xz2_compact(AVD) - 10-_app-.xml`, records timestamp `2026-11-18T09:57:47`, 60 tests, zero failures/errors/skips, and 623.908 seconds of suite time. Testcase durations group as follows:

| Test group | Tests | Seconds | Approximate suite share |
| --- | ---: | ---: | ---: |
| `TransactionFlowTest` | 18 | 481.926 | 77.2% |
| `OverviewAccountSelectionTest` | 4 | 30.846 | 4.9% |
| `FirstRunFlowTest` | 3 | 29.198 | 4.7% |
| `BackupImportUiTest` | 1 | 27.168 | 4.4% |
| `MonthlyLedgerTest` | 1 | 18.568 | 3.0% |
| `AppDatabaseTest`, `BackupImportTest`, `BackupExportTest` combined | 28 | 8.708 | 1.4% |
| Remaining accessibility, artifact, and persistence-seed tests | 5 | 20.134 | 3.2% |

Rounding and suite overhead mean testcase sums need not equal suite time. The slowest individual journeys were Transfer entry/editing (94.781 seconds), navigation/filter/quick-add (68.052), and Budgets (43.460). The saved JVM reports contain 15 tests totaling 0.790 seconds. Neither XML total includes a complete cold build/emulator startup benchmark.

**Inference:** avoiding one redundant full run would save roughly 10–11 minutes under the observed conditions. Three full device runs cost roughly half an hour. The number of tests alone is misleading: 18 UI journeys consume far more time than 28 persistence/Backup tests. Moving those fast Room tests to a different runner is a low priority. Filtering only to `TransactionFlowTest` still selects about eight minutes of recorded execution; prefer its affected method where possible. The recorded Transfer method alone used about 85% less execution time than the full suite, but actual wall-time savings also depend on build/install overhead and emulator state.

## Recommended workflow now

1. Select tests from the ticket's observable acceptance criteria and affected shared behavior. Run the relevant JVM tests and lint/format checks as required by [the coding rules](../../AI-CODING-RULES.md).
2. During edits, run the smallest affected Android method or class on API 29. After a failure, preserve artifacts, diagnose it, and repeat the focused run.
3. After implementation settles, pass the full [ticket completion checklist](ai-ui-testing.md#ticket-completion) once against the finished changes. This applies to local handoff and push alike; pushing does not repeat local verification.
4. Rerun broader verification when subsequent changes or new failures invalidate the previous evidence. A focused pass is not evidence that all 60 tests passed.

This follows the agreed replacement for the earlier pre-push gate. The existing host persistence runner additionally checks actual process-stop/relaunch state; its two instrumentation seed methods do not replace those host assertions. Keep that distinction when selecting tests.

### Concrete PowerShell commands

Run the fast checks together, preserving Gradle's task dependency reuse in one invocation:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:ktlintCheck --console=plain
```

Focus a JVM class:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests 'com.toshlol.personalfinance.FirstRunViewModelTest' --console=plain
```

Gradle's `--tests` applies to JVM `Test` tasks. Android instrumentation selection is a runner argument. AGP 9.1 documents forwarding runner arguments with `-Pandroid.testInstrumentationRunnerArguments.<key>=<value>`, and `AndroidJUnitRunner` documents `class`, `class#method`, and comma-separated class lists. Together these support the following commands; they were researched, not executed during this investigation. Sources: [AGP 9.1 runner arguments](https://developer.android.com/reference/tools/gradle-api/9.1/com/android/build/api/dsl/BaseFlavor#testInstrumentationRunnerArguments()), [AndroidJUnitRunner selection](https://developer.android.com/reference/androidx/test/runner/AndroidJUnitRunner), [Gradle 9.3.1 test filtering](https://github.com/gradle/gradle/blob/v9.3.1/platforms/documentation/docs/src/docs/userguide/reference/platforms/jvm/java_testing.adoc#test-filtering).

Focus an existing Android method:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.toshlol.personalfinance.TransactionFlowTest#reviewEmptyAccountBeforeAndAfterArchiving' --console=plain
```

Focus related Android classes:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.toshlol.personalfinance.FirstRunFlowTest,com.toshlol.personalfinance.OverviewAccountSelectionTest' --console=plain
```

Use the fully qualified Kotlin class name, including for classes whose filenames use underscores. Check the reported test names/count to confirm the filter actually selected the intended test. Keeping the property on the command line avoids accidentally persisting a focused filter into a future full gate. Gradle handles building/installing the APKs and report collection; direct `adb` selection remains available in [the UI testing guide](ai-ui-testing.md#local-or-ci-command-line). Android documents the build-variant task names and report locations in [command-line testing](https://developer.android.com/studio/test/command-line).

The full handoff command, with no filter, is:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

### Alternative considered: saved verification results

**Not selected:** one alternative was a verification runner whose successful full-run result could be reused by the pre-push hook. Correct reuse would need content, test configuration, tool versions, device/API, and relevant environment to match, and would need to distinguish full from filtered runs. The user instead chose to remove the pre-push hook and require all checks at ticket completion.

`HEAD` or timestamps alone would be insufficient for that alternative: a dirty working tree can differ at the same commit, and committing unchanged already-tested content changes `HEAD`. Removing the duplicated gate avoids needing this reuse machinery.

## Can tests be merged?

Yes, selectively. Consolidating repeated behavior can remove a database reset, Activity launch, and repeated navigation. Merely placing tests in the same file or extracting a shared helper does not remove those executions. Preserve independent behavior coverage and useful method-level filtering; joining unrelated journeys makes a failure near the beginning prevent later assertions from running. The following recommendations come from the current test source, with durations from the saved XML above; savings have not been benchmarked.

| Candidate | Proposed change | Coverage to preserve |
| --- | --- | --- |
| `TransactionFlowTest.accountListShowsTypesBalancesAndHistoryNavigation` (7.535 s) and `archiveAccountKeepsReadOnlyBalanceAndHistory` (8.197 s) | Combine into one empty-Account lifecycle test. Check the active Cash type/balance and empty history, return to Accounts, archive Cash, then check archived state and retained empty history. Both currently start from the same empty Cash fixture. This is the clearest direct merge: potentially 60 tests become 59. | Keep assertions before and after archiving. Keep the separate populated-history archive test: it covers retained Income/Transfer rows, read-only counterpart Transfers, and selected-month retention. |
| `OverviewAccountSelectionTest.showAndPersistEmptyAccountSelectionAndSelectAccountFromRightDrawer` (8.435 s) and `retainSelectedMonthAcrossOverviewAndBudgetNavigation` (8.508 s) | Optional second merge: the first already reaches the selected-Cash/current-month state recreated at the start of the second. Append the second test's previous-month and Budget-navigation assertions, removing repeated drawer selection and one setup/Activity cycle. | Keep initial empty-selection assertions, screenshots, selected-Account amounts, and month retention after navigating to Budgets and back. Accept reduced failure isolation between selection and month navigation. |
| `TransactionFlowTest.recordExpenseAndReviewItOnOverview` (20.363 s) | Keep its successful current-month expense and displayed Overview totals. Remove repeated invalid-date steps and the deletion tail only after confirming the dedicated tests retain their assertions. This removes repeated interactions without joining three journeys. | The initial empty-amount submission differs from the zero-amount case in `validatesAmountAndDateWithoutMutationAndSavesWithoutOptionalFields`; retain both inputs explicitly. The latter owns invalid input/no-mutation behavior; `cancelAndConfirmTransactionDeletionUpdateLedger` owns canceled/confirmed deletion and unaffected records/balances. Preserve the Overview expense/net display checks in the successful-expense test. |
| `OverviewAccountSelectionTest.persistSelectedAndExplicitlyEmptyAccountIdsInDataStore` (6.174 s) | Prefer moving this preference-only case into a separate instrumentation class without the Compose Activity rule. It currently launches the full app and seeds financial rows despite never interacting with UI. | Keep selected-versus-explicitly-empty storage assertions independently runnable. Retain the host-driven process-restart check, which asserts the rendered app after real process death. |

Sources: [Account, expense, validation, and deletion journeys](../../app/src/androidTest/java/com/toshlol/personalfinance/TransactionFlowTest.kt), [selection tests and their shared Activity/fixture rules](../../app/src/androidTest/java/com/toshlol/personalfinance/overview_account_selection_test.kt), and [host restart runner](../../scripts/verify_overview_persistence.py).

The two empty-Account tests currently total 15.732 seconds and the selection/month pair totals 16.943 seconds; those are their existing combined costs, **not** the expected savings. Merged tests must still perform both sets of assertions. Both merges would reduce the current count from 60 to 58. Update any ticket/guide references to renamed methods when implementing them. Even eliminating all of those costs would save far less than avoiding one repeated ten-minute full run.

Do not merge the 94.781-second Transfer journey with the 68.052-second navigation journey. Each already covers many operations; shorten unrelated UI setup through deterministic public-boundary fixtures before adding more responsibilities. Likewise keep large-font/accessibility, Room/migration, and system-picker coverage at their respective seams: similar financial data does not make those tests duplicates. The 28 Room/Backup tests together cost only 8.708 seconds, so merging them offers little performance benefit.

## Further improvements, in priority order

### Profile shared UI helpers before rewriting tests

**Observed:** [`FinanceUiDriver.show()`](../../app/src/androidTest/java/com/toshlol/personalfinance/FinanceUiDriver.kt) unconditionally calls `dismissKeyboard()`. `clickText()` reaches it through `show()`. `enterText()` reaches it through `showText()` and dismisses again after replacement. Dismissal inspects all windows and repeatedly searches UI Automator for a hardcoded Google keyboard package. The associated calls are frequent; their actual share of runtime has not been measured.

**Proposal:** time keyboard dismissal, semantics lookup, scrolling, and setup separately in a few representative journeys. If dismissal dominates, dismiss explicitly after input or only while IME visibility requires it, and await the actual hidden state. Preserve system UI automation for the document picker and accessibility assertions. Keep critical full journeys; seed unrelated prerequisites through public repository operations instead of repeatedly entering them through UI where the ticket does not test that entry path.

Compose actions/assertions synchronize automatically, whereas external asynchronous work can require explicit waits. Therefore do not reduce every timeout or replace conditions with sleeps. The 15-second timeout is an upper bound, not proof that every call sleeps 15 seconds. Source: [Compose synchronization](https://developer.android.com/develop/ui/compose/testing/synchronization). New APIs shown in current documentation may require newer Compose artifacts than this repository's `2024.06.00` BOM; an upgrade is not assumed here.

### Avoid duplicate or obsolete CI work

**Proposal:** choose one workflow execution for a branch revision under an open pull request, while retaining verification for direct changes to the main branch. Add workflow concurrency with cancellation for superseded revisions if CI runs are being queued. GitHub documents [push/pull-request events](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows) and [concurrency cancellation](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-workflow-concurrency).

CI validates a clean environment and can remain a deliberate independent gate. Removing duplicate local runs does not require removing CI. Emulator acceleration and CI provisioning changes are outside the agreed first implementation. Verify emulator acceleration before changing hardware or adding parallel devices; Android documents the performance role of [VM and graphics acceleration](https://developer.android.com/studio/run/emulator-acceleration).

### Trial Gradle caches for preparation overhead

The build cache reuses cacheable task outputs and is disabled by default in Gradle 9.3.1; `--build-cache` enables it for one invocation. JVM `Test` is a built-in cacheable task type. Configuration cache instead reuses the configured task graph; it also is opt-in and requires compatible plugins/build logic. Sources: [Gradle 9.3.1 build cache](https://github.com/gradle/gradle/blob/v9.3.1/platforms/documentation/docs/src/docs/userguide/optimizing-builds/build-cache/build_cache.adoc), [configuration cache](https://github.com/gradle/gradle/blob/v9.3.1/platforms/documentation/docs/src/docs/userguide/optimizing-builds/configuration-cache/configuration_cache.adoc).

**Proposal:** measure the normal task set with `--build-cache`, and separately trial `--configuration-cache` twice to confirm actual reuse before making either permanent. Keep compatibility failures visible. Do not use routine `clean`, `--rerun-tasks`, daemon shutdown, or broad cache deletion in the ordinary edit loop; they discard reuse or force work. Cache adoption is unbenchmarked here.

Do not expect these flags to skip connected-device execution: official AGP [instrumentation task source](https://android.googlesource.com/platform/tools/base/+/67164dc29751e8b4243e7752e090397dcfbc3cf4/build-system/gradle-core/src/main/java/com/android/build/gradle/internal/tasks/DeviceProviderInstrumentTestTask.java) declares `@DisableCachingByDefault` and extends `NonIncrementalTask`. Read-only inspection of this machine's cached `com.android.tools.build:gradle:9.1.1` JAR corroborated both class references; no custom task inspection or execution was performed. The saved hook logs already demonstrate that up-to-date build dependencies can coexist with a complete device rerun.

### Consider sharding only after eliminating repeated suites

AndroidJUnitRunner can select shards with `numShards` and `shardIndex`. Running those shards in parallel requires independent devices and coordinating all shard results; selecting one shard on one device leaves the others untested. Source: [AndroidJUnitRunner sharding](https://developer.android.com/reference/androidx/test/runner/AndroidJUnitRunner).

Managed devices can provision parallel shards, using `android.experimental.androidTest.numManagedDeviceShards`, at increased CPU/RAM cost and possible provisioning timeouts. They require new device configuration in this repo. Automated Test Device images documented as API 30 do not satisfy this project's required API 29 gate. Source: [managed devices and sharding](https://developer.android.com/studio/test/managed-devices).

**Proposal:** benchmark two API 29 devices only if the host has spare resources and the single final full run remains a bottleneck. Do not run simultaneous instrumentation against the same app installation: these tests reset shared app state. Sharding improves elapsed time at extra resource cost; it does not solve redundant gate selection or a slow shared helper.

## Agreed scope after research

The following are design decisions confirmed by the user on 2026-11-18, rather than measured research findings:

- Every finished implementation ticket receives one full local Android 10/API 29 run. Focused tests provide feedback during implementation. CI remains a separate independent check.
- The first implementation covers preventing duplicate local full-suite execution, both proposed UI-test merges, and removal of repeated validation/deletion steps from the expense Overview journey. Preserve all distinct behavior assertions, including empty versus zero amounts; accept reduced failure isolation within the merged journeys.
- Defer shared UI-helper optimization, CI trigger changes, Gradle cache tuning, and sharding until the initial improvement is measured.
- Remove the pre-push hook. Ticket implementation owns the final full local verification, and a ticket is not complete until all tests pass. Pushing does not start another local suite; CI remains independent.
- Keep the existing fast pre-commit hook. Ticket completion requires all JVM and Python host-runner tests, both host-driven Overview restart checks, the full unfiltered API 29 Android suite, Android lint, and Kotlin formatting checks to pass against the finished changes. CI remains independent.
- Do not implement saved verification results or reuse machinery. The user confirmed the complete design before implementation.

## Implementation

- Removed `.githooks/pre-push` and documented the full local gate in the [ticket completion checklist](ai-ui-testing.md#ticket-completion). Coding and issue-tracker instructions point to that single checklist.
- Combined the two empty-Account journeys into `TransactionFlowTest.reviewEmptyAccountBeforeAndAfterArchiving`, retaining assertions on both active and archived history.
- Combined drawer selection and month retention into `OverviewAccountSelectionTest.selectAccountAndRetainMonthAcrossOverviewAndBudgetNavigation`, retaining empty/selected state, screenshots, displayed amounts, and navigation assertions.
- Kept empty and zero amount validation in the dedicated validation test. The expense Overview journey checks its successful save and displayed totals without repeating invalid-date validation. Implementation review found that its deletion tail exercises an Overview-row action, whereas the dedicated deletion journey uses the edit form; both entry points are retained to preserve coverage.
- Updated live ticket references to renamed tests. Historical test counts and baseline method names above remain as research evidence. The test inventory is now 58 Android tests and 15 JVM tests.
- Verification passed on 2026-11-18. The full unfiltered Android run on the API 29 `xperia_xz2_compact` emulator (`emulator-5554`) passed all 58 tests with zero failures, errors, or skips. The passing XML contains every Android test method in the current source inventory. Gradle wall time was 10m 23s; reported suite execution was 596.367 seconds. This was the only full connected-suite invocation for this implementation.
- All 15 JVM tests have passing reports and their final Gradle task was up-to-date; Android lint, Kotlin formatting, and APK assembly passed. Both selected and explicitly empty Account states passed the host-driven process-restart check. The host-runner files changed independently in the shared workspace during verification, before the restart check ran; the quick Python suite was rerun against the current files and all 11 tests passed.
- Logs are under `build/test-workflow-performance/` (`final-preparation.log`, `host-restarts.log`, and `full-android-suite.log`). Baseline reports were preserved under `build/test-workflow-performance/20261118T111315Z/baseline/`; final Gradle reports are archived under `build/test-workflow-performance/final/`. Host UI/restart evidence is under `build/overview-persistence/20261118T111720430582Z/`.
- The merged empty-Account journey took 10.348 seconds versus 15.732 seconds for its two original cases; the merged selection/month journey took 9.389 seconds versus 16.943 seconds. These are single-run observations, not controlled benchmarks. The primary workflow saving is removing a second complete local Android run on push.

## How to evaluate a later implementation

Record revision/content identity, selected tests, API/device, emulator acceleration, warm/cold state, preparation time, actual test execution time, result counts, and artifact paths. Compare the same representative focused journeys and the final full gate before/after a helper or runner change. Require all intended tests to remain present and passing; investigate flakes instead of hiding them with broad retries. Workflow-only deduplication should show fewer full runs per unchanged final revision without changing the test inventory.
