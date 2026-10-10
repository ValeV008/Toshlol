# 09: Verify and package the sideloadable app

**What to build:** The completed MVP is continuously verified and can be packaged as a locally signed APK for sideloading.

**Blocked by:** 07: Merge an imported Backup; 08: Overwrite data and reset locally

**Status:** ready-for-agent

- [ ] GitHub Actions runs formatting/lint, JVM tests, and available Android tests.
- [ ] Critical tests run on Android 10 and a current API emulator when available.
- [ ] Accessibility checks cover semantics, labels, actions, and important state; manual TalkBack and Switch Access checks are documented.
- [ ] A locally signed release APK can be produced without committing signing credentials.
- [ ] The release smoke test covers launch, Account creation, Transaction entry, Budget display, export, import Merge, and import Overwrite.
- [ ] The project does not contain Play Store, App Bundle, or publishing automation.
