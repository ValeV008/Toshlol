# How an AI Agent Can Run Android UI Tests

Research date: 2026-10-10

## Conclusion

An AI coding agent can run and diagnose a substantial portion of Android UI testing when the repository exposes command-line Gradle tasks and an emulator or device is available. The recommended stack for this app is:

1. Compose UI tests for app behavior through the semantics tree.
2. Espresso only for legacy or hybrid View-based surfaces.
3. UI Automator for system UI and cross-application flows such as the document picker.
4. Gradle-managed devices for repeatable emulator execution in local development and CI.
5. Screenshots, semantics-tree dumps, logcat, and HTML/XML reports as agent-readable failure artifacts.
6. Human review for accessibility experience, intentional visual changes, and hardware/OEM behavior.

## What the agent can automate

### Compose behavior

Compose tests can locate nodes, assert properties, and perform actions through `ComposeTestRule`. They operate on the semantics tree, which makes stable text, content descriptions, and state semantics important test interfaces.

Typical tests for this finance app can cover:

- Creating the first account.
- Recording an income or expense.
- Enforcing mandatory account selection.
- Filtering transactions by month, account, category, or tag.
- Showing budget progress.
- Choosing merge versus overwrite during import.

Source: [Test your Compose layout](https://developer.android.com/develop/ui/compose/testing)

### Synchronization and deterministic time

Compose actions and assertions synchronize with Compose automatically. Compose also provides a virtual clock, and tests can disable automatic clock advancement when animation timing must be deterministic. External work should use explicit waits such as `waitUntil`.

Source: [Synchronize your tests](https://developer.android.com/develop/ui/compose/testing/synchronization)

When a matcher fails, the agent can print the semantics tree with `onRoot().printToLog()` and use the output to diagnose missing labels, state, or test tags.

Source: [Debug tests](https://developer.android.com/develop/ui/compose/testing/debug)

### Hybrid Views

Espresso is appropriate for View-based UI. A hybrid screen can use Espresso for Views and Compose test APIs for Compose nodes in the same test.

Source: [Compose and View interoperability](https://developer.android.com/develop/ui/compose/testing/interoperability)

Source: [Espresso](https://developer.android.com/training/testing/espresso)

### System and cross-app flows

UI Automator runs outside the app process and can interact with both the application and system UI. It is the appropriate tool for permissions, notifications, launchers, settings, multi-window behavior, and the Android document picker used for Google Drive export/import.

UI Automator can use accessibility window nodes, predicates, timeouts, lifecycle controls, watchers, and screenshots. Compose content must expose useful text/content descriptions or explicitly expose test tags as resource IDs when UI Automator needs to find it.

Source: [Write automated tests with UI Automator](https://developer.android.com/training/testing/other-components/ui-automator)

### Accessibility semantics

Compose semantics describe the meaning of UI elements for accessibility services and testing. Standard Material and Foundation components provide semantics automatically; custom low-level composables may need explicit semantics.

The Accessibility Test Framework can detect issues such as missing labels, contrast problems, touch-target size, and traversal order. Automated checks do not replace manual TalkBack or Switch Access testing.

Sources:

- [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)
- [Testing accessibility in Compose](https://developer.android.com/develop/ui/compose/accessibility/testing)

### Screenshots and visual evidence

Compose Preview Screenshot Testing compares generated UI images with approved reference images and creates an HTML diff report. It can exercise preview parameters such as `uiMode` and `fontScale`.

The tool is experimental and has Android Gradle Plugin, Kotlin, JDK, and Android Studio requirements. Pixel differences still need human approval to decide whether a change is intentional and acceptable.

Source: [Compose Preview Screenshot Testing](https://developer.android.com/studio/preview/compose-screenshot-testing)

For general device evidence, `adb` can install APKs, run commands, capture screenshots, and pull artifacts. An agent can collect a failure screenshot with `adb exec-out screencap -p` and logs with `adb logcat -d`.

Source: [Android Debug Bridge](https://developer.android.com/tools/adb)

## How the agent executes tests

### Local or CI command line

The Gradle Wrapper is the reproducible entry point. Use JDK 17 and install the Android SDK platform and build tools required by the project. Android Studio's bundled JDK is suitable; otherwise install a separate JDK 17. Set the SDK and JDK paths for the current PowerShell session (replace the example paths with the locations on your machine):

```powershell
$env:JAVA_HOME = 'C:\Path\To\JDK-17'
$env:ANDROID_HOME = 'C:\Path\To\Android\Sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"
```

From the repository root, unit tests can run with `test<Variant>UnitTest`; connected instrumented tests can run with `connected<Variant>AndroidTest`. Gradle emits HTML and XML reports suitable for CI artifacts. Run the fast unit, lint, and formatting checks before the slower connected emulator tests.

Use plain-console execution so an agent can parse the result:

```powershell
.\gradlew.bat testDebugUnitTest --console=plain
.\gradlew.bat lintDebug --console=plain
.\gradlew.bat ktlintCheck --console=plain
.\gradlew.bat connectedDebugAndroidTest --console=plain
```

Gradle's `--tests` option filters local JVM tests; it does not select methods in an instrumented test run. To run one instrumented method, build and install both APKs, then pass the fully qualified class and method to `AndroidJUnitRunner`:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class "com.toshlol.personalfinance.TransactionFlowTest#navigateFilterTransactionsAndQuickAddFromEveryMainArea" "com.toshlol.finance.test/androidx.test.runner.AndroidJUnitRunner"
```

Replace the class and method with the test under investigation. The emulator or device must be connected to `adb`.

### Overview selection across process restarts

Run the host-side persistence check against an Android 10 (API 29) test emulator:

```powershell
python scripts/verify_overview_persistence.py --serial emulator-5554
```

Requires Python 3.10+, `adb` on PATH, and the normal Android build environment. The runner builds and installs both debug APKs; use `--skip-build` after building them yourself. It reads the application IDs and APK filenames from Gradle output metadata. `--serial` defaults to `ANDROID_SERIAL`, or the only online device. It accepts only an API 29 emulator because the seed tests replace the target app's ledger and display selection. A signing mismatch fails installation without uninstalling the existing app.

`OverviewPersistenceHandoffTest` seeds Cash with a 1,000 opening balance and 2 Income, plus an unselected Savings Account with a 5,000 opening balance and 7 Income. The host runs each seed method separately, checks the instrumentation result, force-stops the app, verifies its process has stopped, and launches `MainActivity`. It asserts the displayed Closing balance, Income, Expenses, net, activity, and both Account switches. Selected Cash must show 1,002 and 2 Income; an explicitly empty selection must show zero totals, no matching activity, and both switches off. These are post-relaunch UI assertions, independent of the seed test process.

Every run creates a new directory under `build/overview-persistence/` with Overview/activity/drawer XML and PNG evidence for both states, seed results, command output, logcat, and a success result JSON. Failures return a nonzero exit code and attempt a final screenshot and hierarchy capture. `--output-dir` selects a new evidence directory. The emulator retains the final fixture state. CI runs this check before the connected suite inside the same emulator session, so its evidence is collected even if another Android test subsequently fails, and uploads the evidence even on failure.

The host assertions have emulator-free regression tests:

```powershell
python -m unittest discover -s scripts -p 'test_*.py'
```

### Local Git hooks

The repository includes local hooks and does not require a Git hosting service. Enable them once in each clone from PowerShell:

```powershell
.\scripts\install_git_hooks.ps1
```

The pre-commit hook runs unit tests, lint, and ktlint. Ticket implementation owns full verification through the [completion checklist](#ticket-completion); pushing does not run another local suite. CI continues to verify independently.

Sources:

- [Test from the command line](https://developer.android.com/studio/test/command-line)
- [Gradle Command-Line Interface](https://docs.gradle.org/current/userguide/command_line_interface.html)

### Repeatable emulators

Gradle-managed devices can create, deploy, test, and tear down configured emulators. They support consistent environments, snapshots, caching, device groups, and sharding. Device groups can cover the app's minimum API and a current API.

For headless CI, Android's documentation notes the SwiftShader GPU option may be required:

```powershell
.\gradlew.bat pixel2api30DebugAndroidTest `
  -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect `
  --non-interactive --console=plain
```

Source: [Scale your tests with build-managed devices](https://developer.android.com/studio/test/gradle-managed-devices)

## Recommended agent workflow

### Shared UI helpers and failure artifacts

Use `FinanceUiDriver` in `app/src/androidTest/java/com/toshlol/personalfinance/` for app journeys. `showText` waits for one matching node and scrolls it into view. Pass the container tag for lazy content, for example `ui.clickText("Add category", "category-tag-list")`. `enterText` replaces the field value and dismisses the keyboard. Keyboard dismissal uses `InputMethodManager` with every process window token (including dialogs), then waits for the IME inset to disappear. Lazy-container searches retry while asynchronous saves make their resulting rows available. Assert selection through the control's semantics rather than reading a ViewModel.

Place `UiFailureArtifactsRule(composeRule)` **inside** the Compose activity rule in the `RuleChain`. It captures a screenshot, unmerged semantics for every Compose root (including dialogs), recent logcat, and the original stack trace before activity cleanup. Collection errors become separate `.error.txt` files and suppressed exceptions; the original test failure is rethrown.

When Gradle provides `additionalTestOutputDir`, artifacts are written there and collected under `app/build/outputs/connected_android_test_additional_output/`. Each failure has its own `ui-failures/<class>.<method>/<timestamp>/` directory. Direct instrumentation runs fall back to the target app's external files directory; retrieve them with:

```powershell
adb pull /sdcard/Android/data/com.toshlol.finance/files/test-output ./build/ui-test-artifacts
```

Before changing a failing journey, preserve a baseline log and copy `app/build/outputs/androidTest-results/` and `app/build/reports/androidTests/` into a separate directory under `build/`. Gradle replaces the current run's reports. Compare failures against that baseline before calling them pre-existing. `UiFailureArtifactsRuleTest` verifies the capture path with an intentional assertion caught inside the test, so the verification suite still passes.

### Deterministic Room fixtures

Create one `FinanceTestFixtures(database)` per test/database. Its Account, Category/Tag, Transaction, and Budget repositories share a sequential UUID generator and fixed timestamp. Use `createFirstAccount()` for common setup, and pass `null` as the ID of a new Transaction. To test an edit, reuse the ID returned by a previous repository operation. Tests specifically about IDs, changing time, or migration generation can inject their own providers directly. Keep assertions on public repository results.

### Execution loop

1. Inspect Gradle tasks, test source sets, application ID, test runner, and configured devices.
2. Run fast unit tests and lint.
3. Run the focused Compose or instrumented test on a managed or connected emulator.
4. For document-picker tests, use UI Automator at the system boundary.
5. If a test fails, collect the Gradle report, logcat, screenshot, and semantics-tree output.
6. Reproduce the failure with the narrowest class or method filter before editing code.
7. Make the smallest fix and rerun that focused test. Repeat focused runs while implementation is changing.
8. After related changes settle, pass the [ticket completion checklist](#ticket-completion) once against the finished changes, whether handing off locally or pushing. If later implementation changes invalidate those results, repeat the affected checks, including the full Android suite for app, Android-test, or build changes.
9. Upload reports, screenshots, diffs, and logs as CI artifacts.

For this app, the high-value automated journeys are account creation, transaction entry, budget display, export, import merge, import overwrite, and the mandatory-account validation path.

### Ticket completion

An implementation ticket is complete only when all of the following pass against its finished changes:

1. All JVM tests and Python host-runner tests.
2. Android lint and Kotlin formatting checks.
3. Both host-driven Overview process-restart checks on an Android 10/API 29 test emulator.
4. The full, unfiltered connected Android suite on that emulator, with no failed, errored, or skipped tests.

Select a test emulator from `adb devices`, set its serial, and run the following sequence from PowerShell. The host runner checks that the selected device is an API 29 emulator. The assembly tasks prepare the APKs so the host check can use `--skip-build`.

```powershell
$env:ANDROID_SERIAL = 'emulator-5554'
python -m unittest discover -s scripts -p 'test_*.py'
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:ktlintCheck :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
python scripts/verify_overview_persistence.py --serial $env:ANDROID_SERIAL --skip-build
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

Check each command's exit code and stop to diagnose any failure. A focused run is useful during implementation but does not satisfy this final checklist. Keep runner selection and sharding arguments out of the full-suite invocation. If the environment prevents a required check, report the limitation and leave the ticket unresolved.

Record the device/API, test counts, results, and evidence paths in the ticket's Answer or handoff. Gradle reports are under `app/build/test-results/`, `app/build/reports/`, and `app/build/outputs/androidTest-results/`; the host check saves each run under `build/overview-persistence/`. Pushing requires no additional local run; CI remains an independent check.

## What still needs a human

An AI agent can determine whether assertions pass and can inspect screenshots, logs, semantics, and reports. Human review is still appropriate for:

- Whether a screenshot change is visually intentional and acceptable.
- The real TalkBack, Switch Access, keyboard, focus, and gesture experience.
- Physical-device behavior involving biometrics, sensors, cameras, Bluetooth, OEM differences, and hardware rendering.
- Product-level usability and visual quality.
- Release smoke testing of a locally signed APK with a real document-provider flow.

Source: [Continuous Integration basics](https://developer.android.com/training/testing/continuous-integration)

Source: [Set up continuous integration](https://developer.android.com/studio/projects/continuous-integration)
