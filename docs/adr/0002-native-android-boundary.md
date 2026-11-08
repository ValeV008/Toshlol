# Native Android Boundary

The app is a native Kotlin Android application using Jetpack Compose and Material 3, with `minSdk 29` for the user's Android 10 (API 29) phone. Android 10 is the only required development and test environment; compatibility with newer Android versions is not a project requirement. A single app module, Room, DataStore, Kotlin serialization, and explicit migrations keep the first release platform-focused and offline; the app produces locally signed APKs for sideloading and is not a Play Store product.

## Consequences

- UI and system integration tests run on Android emulators or devices.
- Google Drive support uses the Android document picker rather than a cloud API.
- Signing credentials and release distribution remain outside the repository.
