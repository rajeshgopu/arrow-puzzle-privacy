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

## Languages

The game ships in seven languages: English (the default and the fallback),
German, French, Spanish, Brazilian Portuguese, Japanese and Korean. It follows
the device language automatically and falls back to English for anything it is
not translated into. Settings > Language overrides it per player, and the choice
is remembered across launches.

All user-visible text lives in `app/src/main/res/values*/strings.xml`. English is
in plain `values` rather than `values-en`, because that folder is also what a
device set to an untranslated language resolves to.

Nothing in the game logic holds a translated string. The language is chosen by
`i18n/AppLanguage.kt`, applied by `i18n/LocalizedApp.kt`, and reached from the UI
with `stringResource`. Changing it is a composition-local swap, not an activity
restart, so progress, level, settings, achievements and the board in play are
untouched.

Adding a language is one entry in `AppLanguage` plus one resource folder. See
[docs/store/localization.md](docs/store/localization.md) for the store copy and
the full checklist.

#### Two things that are deliberately not translated

- **The game name.** "Arrow Puzzle" is the store listing's search term and the
  wordmark's own lettering, so it stays English in every locale. The localised
  short descriptions carry the local search terms instead.
- **Technical identifiers.** Arrow directions, level asset filenames, package
  and class names and resource keys are all stable identifiers, not copy. The
  `Direction` enum keeps its four names; only the screen announces them in words.

#### CJK rendering

Poppins and Playfair Display are subset to Latin, so Japanese and Korean resolve
the interface family to the platform's own CJK family rather than shipping a
second multi-megabyte font. The wordmark stays on Poppins in every language,
because it is Latin by design.

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

## Google Play Games

The game asks Google Play Games who the player is, once, at launch, and never
waits for the answer. If Play Games identifies a player, their name appears on
the home menu under the level and star plates; if it cannot - no Play Games
account, no Play Store, offline, the player declined, or Google simply does not
answer - the game carries on as a guest. There is no login screen and no
sign-in prompt of this app's own; the only screen that can ever appear is
Google's own, and only when Google decides it is needed.

Progression is unaffected either way. The level lives in local DataStore
(`highest_unlocked_level`) and is owned by the game alone - Play Games is never
told what level the player is on, and a failed sign-in cannot reset it. There is
no cloud save.

To get a real player identity, set the app up in the Play Console:

1. Publish or enroll the app in the Play Console, then open
   **Play Games Services > Setup** and turn it on.
2. Add the SHA-1 certificate fingerprints of every signing key that will ship
   or be installed - the upload key **and** the app signing key, plus the debug
   key if you want Play Games to work on debug builds. A build signed by a key
   that is not listed gets no player.
3. Leave the other Play Games features (achievements, leaderboards, Saved Games)
   off; this integration only reads the player identity.

Until that is done the app is simply a guest, which is a complete and playable
state - the game logs the reason under the `PlayGames` tag:

```powershell
adb logcat -s PlayGames
```

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
