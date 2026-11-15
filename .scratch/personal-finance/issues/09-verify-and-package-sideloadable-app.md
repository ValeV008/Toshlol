# 09: Verify and package the sideloadable app

**What to build:** The completed MVP is continuously verified and can be packaged as a locally signed APK for sideloading.

**Blocked by:** 07: Merge an imported Backup; 08: Overwrite data and reset locally

**Status:** resolved

- [x] GitHub Actions runs formatting/lint, JVM tests, and available Android tests.
- [x] Critical Android tests run on Android 10 (API 29); testing on newer Android versions is not required.
- [x] Accessibility checks cover semantics, labels, actions, and important state.
- [x] A locally signed release APK can be produced without committing signing credentials.
- [x] The release smoke test covers launch, Account creation, Transaction entry, Budget display, export, import Merge, and import Overwrite.
- [x] The project does not contain Play Store, App Bundle, or publishing automation.

## Parent-spec requirements

Relevant requirements from [the Personal Finance MVP spec](../spec.md):

- User story 47 and the implementation decision to produce locally signed APKs require a repeatable release APK build suitable for sideloading.
- User story 48 requires signing credentials to stay outside the repository and be supplied locally or through CI secrets.
- User stories 49–51 and the testing decisions require JVM behavior tests, critical Compose journeys, Android document-picker coverage, and accessibility semantics checks. CI must run the Android suite on API 29.
- The out-of-scope list excludes Play Store publishing and App Bundle distribution.

Observable acceptance criteria:

- GitHub Actions runs `ktlintCheck`, `lintDebug`, `testDebugUnitTest`, and the full `connectedDebugAndroidTest` suite on an API 29 emulator. The required Account, Transaction, Budget, Backup, import, and accessibility journeys are included in that suite; in particular `FirstRunFlowTest.createFirstAccountAndKeepOverviewAcrossRelaunch`, `TransactionFlowTest.recordExpenseAndReviewItOnOverview`, `TransactionFlowTest.createBudgetAndSeeExpenseProgressOnBudgetsAndOverview`, `BackupExportTest.exportWritesCompleteVersionedSnapshot`, `BackupImportTest.mergeIsIdempotentAndAppliesNewerWholeRecords`, `BackupImportTest.overwriteReplacesCompleteDatasetAndSettingsAndReportsInsertedCounts`, `BackupImportUiTest.settingsLaunchesDocumentPickerForMergeAndCancellationReturnsToSettings`, and `AccessibilityTest.exposeLabeledInputsSelectedTypesAndValidationAtLargeFont` run in CI.
- A maintainer can configure signing from environment variables and build `app/build/outputs/apk/release/app-release.apk`; CI and repository history contain no keystore or signing passwords.
- `docs/release_smoke_test.md` lists the manual signed-APK journey for launch, Account creation, Transaction entry, Budget display, export, Merge, and Overwrite.
- Workflow files and release docs contain no Play Store publishing or App Bundle automation.

## Comments

- 2026-11-15: Added parent-spec coverage and mapped the release and CI criteria to existing behavior tests before implementation.
- 2026-11-15: Added CI, environment-based local release signing, release build and smoke-test guides, and API 29 UI test fixes. Verified formatting/lint/JVM checks, all 44 API 29 instrumentation tests, and a signed release APK install/launch.

## Answer

CI now runs lint, JVM tests, and the complete API 29 instrumentation suite. Maintainers can build a locally signed sideloadable APK using credentials kept outside the repository. Release and manual smoke-test steps are documented. The final API 29 run passed all 44 tests, and the signed APK was verified, installed, and launched.
