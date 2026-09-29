# Project Memory

## Project

- Name: Arrow Puzzle: Tap Puzzle Games (working title)
- Platform: Android
- Release target: Basic, ad-supported version 1
- Current phase: Playable loop — UI polish and content validation

## Product Decisions

- Game is portrait, single-player, and offline-first.
- The player removes arrows only when their route to the board edge is clear.
- Version 1 has 50 handcrafted levels across 5 packs.
- Each attempt starts with 3 lives. Invalid taps cost a life.
- Progress and settings are stored on device.
- No accounts, multiplayer, purchases, or cloud save in version 1.

## Technology Decisions

- Kotlin + Jetpack Compose + Material 3.
- Android package and application ID: `com.gopu.arrowpuzzle`.
- Android target API 36 and proposed minimum API 24.
- DataStore Preferences for progression and settings.
- Levels are versioned JSON assets.
- Pure game engine separated from Compose UI and ad code.

## Monetization Decisions

- Google AdMob is the only ad provider for version 1.
- Banner: home and level-select screens only.
- Interstitial: after every two completed levels at most, before the next-level
  action.
- Rewarded: voluntary one-life continuation after failure, once per attempt.
- UMP consent and a privacy-options entry are required before release.
- Debug builds use Google test ad IDs; release builds use production IDs.

## Completed

- [x] Defined Android MVP scope.
- [x] Defined core arrow-puzzle rules and win/loss states.
- [x] Defined 50-level content target and progression model.
- [x] Selected Kotlin and Jetpack Compose architecture.
- [x] Defined AdMob placement and privacy constraints.
- [x] Created `MAIN_SYSTEM.md`.
- [x] Created this progress memory.

## Active Work

- [x] Created the initial Android/Jetpack Compose build scaffold.
- [x] Created an interactive home-to-level visual prototype.
- [x] Generated the Gradle 8.10.2 wrapper.
- [x] Verified Gradle project configuration reaches Android SDK resolution.
- [x] Built and verified the debug APK for `com.gopu.arrowpuzzle`.
- [x] Pinned Kotlin compilation to JVM 17 to match Android Java compatibility.
- [x] Added reusable Debug and Release PowerShell build commands.
- [x] Added an ignored placeholder-based release signing configuration.
- [x] Verified both Debug and unsigned Release build-script paths.
- [x] Implemented pure board model and move-validation reducer with unit tests.
- [x] Replaced the visual prototype with validated gameplay, lives, restart,
  victory, and failure states for Level 1.
- [x] Added a guided first-move tutorial state for Level 1.
- [x] Added local DataStore progress persistence: best stars per level and
  highest-unlocked level now persist, and winning a level unlocks the next.
- [x] Wired the level-select flow to real progress with lock, completion, and
  star states, and made level 1 load from the JSON asset.
- [x] Restricted the guided tutorial to the intro level and gated it on the
  persisted `tutorial_seen` flag.
- [x] Fixed the `DataStoreHelper` compile errors (missing `edit` import and
  Elvis/`to` precedence) so the debug build assembles.
- [x] Reworked the UI: Material 3 theme, gradient backgrounds, elevated cards,
  stat chips, animated tile removal, and win/failure result overlays.
- [x] Added gameplay feedback: haptic ticks for valid/invalid taps, tile shake
  plus "PATH BLOCKED" message on an invalid tap, a lives row, and a clear
  progress bar.
- [x] Added an automated level validator test that loads every shipped JSON
  asset and asserts id/name match, non-empty tiles, uniqueness, bounds,
  supported directions, the shared `validateLevel`, and greedily-verified
  solvability.
- [x] Fixed the five pack-1 levels the validator flagged as unsolvable
  (`pack-01-level-04/06/07/09/10`) by re-laying them out so every arrow has a
  valid removal order.
- [x] Added a zero-dependency Windows desktop level viewer (`tools/LevelViewer.ps1`)
  that renders each board with arrows, reports solvability using the same
  greedy rule as the engine, and offers `-Dump`, `-Smoke`, and `-ExportTo`
  headless modes. Fixed a PowerShell array-literal arithmetic parse bug in the
  arrow geometry that crashed the paint handler.
- [x] Extracted the game UI into reusable components (`ui/components/GameBoard.kt`,
  `ui/components/Common.kt`) and a dedicated `ui/GameplayScreen.kt`: calm
  single-row HUD, pause overlay, and a limited hint control built on
  `PuzzleReducer.firstValidMove`.
- [x] Finished Phase 1 (Consolidate): moved `HomeScreen` to `ui/HomeScreen.kt` and
  added `app/AppNavigation.kt` (screen routing + selected level), leaving
  `MainActivity` as theme + app entry only.
- [x] Finished Phase 2 (HUD + overlays): staggered star pop-in on the result card
  and a heart-break animation when a life is lost. Kept a quick restart button in
  the HUD in addition to the Pause menu.
- [x] Finished Phase 3 (Settings & privacy): new `ui/SettingsScreen.kt` with
  Sound/Haptics toggles (DataStore-backed), a How-to-Play overlay, version line,
  and conditional Privacy Options / Privacy Policy rows. Reachable from a Home
  gear and the Pause menu (game state preserved). `AppConfig` holds the release
  privacy URL and UMP flag; both are unset until Phase 4.
- [x] Finished Phase 4 (Level select): `LevelSelectScreen` now pages across packs
  1–5 (`HorizontalPager` + prev/next controls) with a total-stars chip; packs
  without shipped levels show a "coming soon" card. `AppNavigation` loads all
  five packs. Pack 2–5 content is still pending.
- [x] Finished Phase 5 (Monetization UI): added `ads/` (`AdController`, `AdHost`
  with 2-completion interstitial pacing, `BannerAdSlot`); banner slots on Home and
  level select; rewarded `CONTINUE +1 ♥` on failure with 1-star cap. Debug uses a
  simulated provider (`BuildConfig.DEBUG`); release uses `NoAds` until AdMob +
  UMP are bound. Enabled `buildConfig`. Debug and release builds pass.
- [x] Finished Phase 6 (Polish, code): staggered board-tile entrance animation,
  TalkBack labels for tiles and level tiles, 48dp icon buttons, and a 520dp board
  cap for tablets. On-device compact/tablet QA still recommended.
- [x] Added pinch-to-zoom and drag-to-pan to the game board (`ui/components/ZoomableBoard.kt`)
  so larger grids stay readable/tappable; zoom resets per level and a reset
  control appears while zoomed.
- [x] Full-screen/insets: `main` now uses `enableEdgeToEdge()` + immersive
  `hide(systemBars())` (re-applied on focus), transparent bar colors and
  `shortEdges` cutout mode in the theme, and every screen pads content with
  `WindowInsets.safeDrawing` so the camera cutout and nav bar never cover the UI.
- [x] Sound + haptic feedback: added `Sounds` (ToneGenerator tones, no audio
  assets) wired to valid/invalid taps and wins; Settings plays a sound sample
  when Sound is enabled and a haptic sample when Haptics is enabled.
- [x] Haptics now use the system `Vibrator` (`VIBRATE` permission) so the sample
  and in-game feedback fire even when view touch-sound feedback is off. Settings
  is reachable from Home, Level Select, and the Gameplay top bar / Pause menu.
- [x] Added an animated splash screen and made the Home hero arrow tappable to
  play; the app now starts on `SPLASH`.
- [x] Neon redesign: arrows are card-free and glowing (`ArrowMark(glow=true)`),
  neon palette in the theme; tapping the Home arrow fires a synthesized rocket
  whoosh (`Sounds.rocketLaunch`, in-memory `AudioTrack`) with an animated
  `RocketFlame` exhaust trail before navigating. UI click sounds softened
  (lower volume, gentle beep).

## Next Actions

1. Add the remaining screens: Pause, Victory, Failure, and Settings with a
   persistent Privacy Options entry.
2. Integrate UMP consent and AdMob test ads (banner, paced interstitial,
   rewarded continuation) now that the core loop is playable.
3. Author pack-2 through pack-5 levels (11–50) and keep them passing the
   validator.

## Risks and Guardrails

- Confirm the final game name and branding are available before Play Store
  submission.
- Do not add ads to the gameplay board or trigger them while the player taps.
- Treat ad load failure as optional: it must never block gameplay.
- Keep all shipped levels solvable through an automated validator.
- Update target SDK and Google SDK versions just before release.
- This development machine needs Android SDK Platform 36 configured in Android
  Studio or `local.properties` before an APK can be assembled.

## Change Log

| Date | Change |
| --- | --- |
| 2026-09-28 | Created the initial system specification and project memory. |
| 2026-09-28 | Added Kotlin/Compose Android build scripts and home-to-level UI prototype. |
| 2026-09-28 | Generated the Gradle wrapper; build check is blocked only by absent Android SDK configuration. |
| 2026-09-28 | Built `app-debug.apk` successfully and verified its package and launcher activity. |
| 2026-09-28 | Added `build.ps1` and optional release signing configuration. |
| 2026-09-28 | Verified `build.ps1` produces Debug and unsigned Release APKs. |
| 2026-09-28 | Implemented and tested the puzzle reducer; connected Level 1 to real game state. |
| 2026-09-28 | Added a guided first-move tutorial flow for Level 1. |
| 2026-09-28 | Fixed the `DataStoreHelper` build errors and verified the debug APK assembles. |
| 2026-09-28 | Wired DataStore progress: best stars and highest-unlocked level persist on win; level-select reads real progress. |
| 2026-09-28 | Made stars monotonic (best result is kept), added `starsForInvalidTaps` and `firstValidMove` with unit tests. |
| 2026-09-28 | Gated the tutorial to Level 1 via the persisted `tutorial_seen` flag. |
| 2026-09-28 | Polished the UI with a Material 3 theme, gradients, elevated cards, animated tile removal, result overlays, and haptic/invalid-tap feedback. |
| 2026-09-28 | Added `LevelAssetValidatorTest` (structure + solvability) and fixed the five unsolvable pack-1 levels it found. |
| 2026-09-28 | Added `tools/LevelViewer.ps1` desktop viewer (WinForms/GDI+) with `-Dump` and `-Smoke` headless verification modes. |
| 2026-09-28 | Fixed the viewer paint crash (PowerShell array-literal arithmetic) and added `-ExportTo` PNG output; documented usage in the README. |
| 2026-09-29 | Added `UI_PLAN.md`; focused the game UI into `ui/GameplayScreen.kt` + `ui/components/`, adding pause and hint. Debug build and unit tests pass. |
| 2026-09-29 | Phase 1 Consolidate: extracted `ui/HomeScreen.kt`, added `app/AppNavigation.kt` nav host; `MainActivity` now only sets the theme. Build + tests pass. |
| 2026-09-29 | Phase 2 HUD + overlays: staggered star pop-in and lives-lost heart animation in `ui/components/Common.kt`. Build passes. |
| 2026-09-29 | Phase 3 Settings & privacy: added `ui/SettingsScreen.kt` (toggles, How-to-Play, version, conditional privacy rows) + `AppConfig`, wired from Home gear and Pause menu. Build passes. |
| 2026-09-29 | Phase 4 Level select: pack pager (1–5) with prev/next + total-stars chip; empty packs show a coming-soon card. Build passes. |
| 2026-09-29 | Phase 5 Monetization UI: `ads/` seam + banner slots, paced interstitial, rewarded continue; debug simulates ads, release is NoAds. Debug + release builds pass. |
| 2026-09-29 | Phase 6 Polish: staggered board entrance, tile/level TalkBack labels, 48dp icon targets, 520dp tablet board cap. Build + tests pass. |
| 2026-09-29 | Made the game board zoomable (pinch/drag) via `ZoomableBoard`; zoom resets per level with an on-board reset control. Build passes. |
| 2026-09-29 | Full-screen + safe insets (`enableEdgeToEdge`, immersive bars, transparent theme, `safeDrawing` padding) and sound/haptic feedback samples in Settings. Build passes. |
| 2026-09-29 | Animated `SplashScreen`, tappable Home arrow, `Vibrator`-based haptics (+`VIBRATE`), and Settings entry on Level Select / Gameplay. Build passes. |
| 2026-09-29 | Neon card-free glowing arrows, rocket-whoosh + `RocketFlame` launch on the Home arrow, and softened click tones. Build passes. |
| 2026-09-30 | Matrix-based engine (`PuzzleLevel.occupancy`), path-clear now ignores an arrow's own body cells, added `PuzzleGenerator` (reverse-placement, guaranteed solvable, disjoint cells), enforced that a bent arrow's direction matches its final body segment, rewrote `ArrowShape` (cell-filling body, clear rounded bends, no double-back nub), and regenerated packs 2–5 assets. Build + tests pass. |
