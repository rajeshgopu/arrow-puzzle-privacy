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

The app opens on the home screen, then the level-select grid. Ten pack-1 levels
ship as JSON assets in `app/src/main/assets/levels`.

## View Levels

A dependency-free desktop viewer renders every level asset and reports whether
it is solvable. Double-click `tools\view-levels.cmd`, or from PowerShell:

```powershell
.\tools\LevelViewer.ps1                       # window with Prev/Next and removal order
.\tools\LevelViewer.ps1 -Dump                 # text summary, no window
.\tools\LevelViewer.ps1 -ExportTo tools\snapshots   # write one PNG per level
```

In the window, use Prev/Next or the arrow keys, and tick "Show removal order"
to overlay a valid clearing sequence.

## Current Build Setup

- Kotlin and Jetpack Compose
- Android Gradle Plugin 8.7.0
- Gradle Wrapper 8.10.2
- Compile and target SDK 36
- Minimum SDK 24

## AdMob Configuration

AdMob is bound for both build types. Debug builds always resolve to Google's
public test app and ad unit IDs, so no development or manual testing ever serves
or clicks a live ad. Release builds resolve to this app's own values, read from
`local.properties` (or `-P<key>=...`), with the published production ad units
as the defaults:

```properties
admob.appId=ca-app-pub-3319834061576964~<digits>
privacyPolicyUrl=https://<your-public-policy-page>
```

Use [local.properties.example](local.properties.example) for the full key list.
Only `admob.appId` and `privacyPolicyUrl` have no usable default: without the
app ID a release build keeps Google's test App ID and the SDK serves nothing
against the real ad units, and without the policy URL the Settings row is
hidden. The build prints a warning for a missing app ID.

Ads sit behind a UMP consent gate, so nothing is requested until Google's
consent state allows it. Settings > Privacy Options reopens the consent form,
which is what the Play Store requires once ads are served.

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
