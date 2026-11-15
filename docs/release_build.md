# Build a locally signed release APK

The release APK is signed with a keystore you control. Keep the keystore and its passwords outside the repository. CI runs verification only and does not need release signing credentials.

## Create a keystore

Run `keytool` from a JDK and choose a path outside this repository. For example, in PowerShell:

```powershell
keytool -genkeypair -v -storetype JKS -keystore "$env:USERPROFILE\.android\finance-release.jks" -alias finance-release -keyalg RSA -keysize 2048 -validity 10000
```

Store the passwords in your local environment or a password manager. Set the four variables in the PowerShell window that will run Gradle:

```powershell
$env:FINANCE_RELEASE_STORE_FILE = "$env:USERPROFILE\.android\finance-release.jks"
$env:FINANCE_RELEASE_STORE_PASSWORD = 'your-keystore-password'
$env:FINANCE_RELEASE_KEY_ALIAS = 'finance-release'
$env:FINANCE_RELEASE_KEY_PASSWORD = 'your-key-password'
```

Build the APK:

```powershell
.\gradlew.bat assembleRelease --console=plain
```

Gradle writes `app/build/outputs/apk/release/app-release.apk`. The `packageRelease` task checks that all four variables are set and that the keystore file exists; a release APK cannot be produced unsigned through this task. Debug builds do not require signing variables.

To verify the APK signature with Android Build Tools, run `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`.

## Install and smoke-test

Follow [the release smoke test](release_smoke_test.md) on an Android 10 (API 29) device or emulator. Keep a copy of the keystore in a secure backup location; losing it prevents producing future updates signed with the same identity.
