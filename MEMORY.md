# Project Memory

## Project

- Name: Arrow Puzzle: Tap Puzzle Games (working title)
- Platform: Android
- Release target: Basic, ad-supported version 1
- Current phase: Core gameplay implementation

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
- [ ] Add local progress persistence and level-select flow.

## Next Actions

1. Add local DataStore progress and level-select flow.
2. Author and validate the first 10 levels.
3. Integrate UMP and AdMob test ads after the core loop is stable.

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
