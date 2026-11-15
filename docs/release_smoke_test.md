# Release APK smoke test

Install the locally signed `app/build/outputs/apk/release/app-release.apk` on an Android 10 (API 29) device or emulator. Use a disposable dataset because the Overwrite step replaces local finance data.

1. Launch the app, select a currency, create an Account, and confirm that Overview opens.
2. Add an Expense Transaction and confirm it appears in the ledger and Overview.
3. Create an Expense Category and a monthly Budget. Confirm the Budget appears on the Budgets screen with the expected spending progress.
4. Open Settings, export a Backup through the Android document picker, and confirm the JSON file is created at the selected location.
5. Import a compatible Backup using **Merge**. Confirm the imported records appear alongside the existing records without duplicates.
6. Import a different Backup using **Overwrite**, confirm the replacement prompt, and verify that only the replacement dataset remains.

Record the device/API, APK version, result for each step, and any failure details before distributing the build.
