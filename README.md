# Arrow Puzzle Android

## Open and Run

1. Open this folder in Android Studio.
2. Install Android SDK Platform 36 and Android SDK Build-Tools through SDK Manager.
3. Let Android Studio create `local.properties`, or add the SDK path manually:

```properties
sdk.dir=C\:\\Users\\your-user\\AppData\\Local\\Android\\Sdk
```

4. Select an Android emulator or connected device.
5. Build a debug APK:

```powershell
.\build.ps1 -Variant Debug
```

The starter opens with the game home screen. Play transitions to an interactive
Level 1 board prototype.

## Current Build Setup

- Kotlin and Jetpack Compose
- Android Gradle Plugin 8.7.0
- Gradle Wrapper 8.10.2
- Compile and target SDK 36
- Minimum SDK 24

AdMob is intentionally deferred until the core game loop is implemented. This
keeps test-ad configuration, consent, and production credentials out of the
initial screen prototype.

## Release Signing

The release build is unsigned until a local `signing.properties` file exists.
Use [signing.properties.example](signing.properties.example) as the template,
replace its placeholder values, and keep both the real properties file and
keystore private.

```powershell
.\build.ps1 -Variant Release
```

The script finds the Android SDK from `ANDROID_HOME`, `ANDROID_SDK_ROOT`,
`local.properties`, `D:\work\android`, or the standard local Android SDK path.
