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
- Android package and application ID: `com.gopu.arrow.puzzle.game`.
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
- [x] Built and verified the debug APK for `com.gopu.arrow.puzzle.game`.
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
- [x] Premium neon maze gameplay screen: the board is now a single dark-plate
  canvas. Multi-cell arrows render as one continuous neon tube — straight, L,
  U, zigzag and winding — with every direction change converted to a rounded
  quadratic Bézier bend (tangent-continuous, radius clamped per segment) and a
  crisp kite arrowhead derived from the arrow's final segment, so heads are
  always aligned and never sit on an intermediate bend. Seven neon colours
  (cyan/pink/purple/green/orange/yellow/blue) are assigned so no two touching
  arrows share one.
- [x] Arrow-level selection and feedback: taps are hit-tested against each
  arrow's drawn spine (falling back to cell ownership), so tapping any part of
  an arrow selects the whole thing and glows. A blocked tap repaints the entire
  arrow red, shakes it, and bursts a red collision glow at the exact blocking
  cell (`PuzzleReducer.blockingCell`). A valid tap launches the complete arrow
  as one rigid body toward the exit with a rocket easing curve, ghost trail,
  sparks and a rocket exhaust flame.
- [x] Dark neon gameplay HUD: level/pack header, lives, arrow and move counters,
  glowing progress bar, status line, neon hint/restart actions, and dark
  result overlays (`NeonIconButton`, `NeonActionButton`, `NeonChip`,
  `NeonProgressBar` in `ui/components/Common.kt`).
- [x] Regenerated all 50 level assets with dense multi-cell bodies and larger
  boards (4x4 up to 8x8). `tools/generate-levels.ps1` now grows walking bodies
  (start behind the head, then turn), which keeps every level solvable under
  the greedy rule; `LevelAssetValidatorTest` and the level viewer confirm all 50.
- [x] The desktop level viewer (`tools/LevelViewer.ps1`) renders the same
  connected neon arrows as the game instead of one square per cell, so
  `tools/snapshots` previews match the board.
- [x] Expanded the gameplay layout to fill the screen: `BoardMetrics` fills the
  plate on both axes with a derived plate inset (cells stay square, with a
  0.78–1.26 cell-aspect fallback to the level's own ratio), and the background
  matrix dot field covers the whole plate including the lanes past the grid. All
  50 levels regenerated as portrait boards (`3x5`…`7x11`, 15→34 arrows) by
  `tools/generate-levels.ps1`, which now takes width and height separately and
  derives its silhouettes from normalised half-extents. Board area up 22–56% on
  a phone, with cells the same size or larger (pack 5: 39→41dp cells and 64→77
  cells). The generator's clearance check now carries bounding boxes and exits
  early (a full regeneration went from tens of minutes to about two), and it
  reads the head off the end of the body list — fixing a mirrored-measurement
  bug that had 23 of 50 levels failing
  `LevelAssetValidatorTest.everyShippedLevelKeepsRenderedArrowsVisuallySeparated`,
  which now passes on all 50.
- [x] One liquid-pipe launch animation per successful tap
  (`ui/components/LaunchAnimations.kt`): the arrow is treated as a pipe and a flow
  front walks its own bent path from tail to tip. The head rides the front as one
  rigid piece, the tube behind the front has collapsed to a thin line on that same
  path, the stretch ahead is drawn through `ArrowShape.drawTubeOn` so it is still
  exactly the arrow the player tapped, and once the whole body is a line that line
  is carried out of the board. Nothing is bent, flexed, scaled, squashed or
  re-pathed, and there is no translation at all until the body is already a line,
  so the arrow is never slid across the board as a rigid sprite. The head is the
  only thing that turns, and only enough to face the way it is travelling, which
  lands it back on the arrow's own axis at the tip. Launch curves are built once per
  level, and a launch frame reuses one `Path` for every stretch it cuts, so it
  allocates nothing. `LaunchCurveTest` covers straight, L, U and zigzag bodies in
  all four directions.
- [x] Seamless launch: the splash is no longer a routed screen that gets swapped
  for the menu. `ArrowPuzzleApp` composes Home from the first frame underneath
  `ui/SplashOverlay.kt` and dissolves the overlay off the top of it, so the menu
  is measured, laid out and drawn (progress flows included) while the brand is
  still on screen and the handoff only has to cross-fade an already-finished
  screen into view. Two clocks in `ui/SplashTransition.kt` (intro 460ms, reveal
  320ms) are read only inside `graphicsLayer` blocks, so a handoff frame repaints
  two layers and recomposes nothing; `SplashLayer` keeps the final unmount out of
  the menu's scope. Both clocks start on a real measured frame rather than on a
  timer, so fast and slow devices begin the same animation at the same point. The
  splash field and the window background are the menu's own `IvoryTop`, so there
  is no colour to change at either end of the launch. `SplashTransitionTest` pins
  the curves, the 250–400ms window and the fact that the menu is fully opaque
  before the overlay leaves.

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
| 2026-10-01 | Rebuilt the gameplay screen as a premium neon maze: single-canvas board with dark plate, multi-cell arrows as continuous glowing tubes with rounded Bézier bends and integrated kite arrowheads, seven-colour non-touching palette, arrow-level tap hit testing, red whole-arrow block feedback with collision glow at `PuzzleReducer.blockingCell`, rigid launch animation (trail, sparks, rocket flame), and a dark neon HUD. `tools/generate-levels.ps1` regenerated all 50 levels with dense multi-cell bodies on 4x4–8x8 boards. `LevelViewer` now previews the same neon arrows. Build + 15 unit tests pass. |
| 2026-10-01 | Fixed arrow bends rendering as sharp elbows: the straight-continuation guard compared `cosine > -0.999`, which also matched every 90 degree corner (cosine 0) and skipped all rounding. Guard corrected to `cosine > 0.999` (viewer uses the equivalent cross/dot test), so every real turn is now a tangent-continuous rounded bend. Build + tests pass. |
| 2026-10-01 | Added five random launch animations for successful arrow moves (`ui/components/LaunchAnimations.kt`): NEON ENERGY FLOW, ELECTRIC PULSE, ROCKET BOOST, PLASMA WARP, SPARK BURST. Effects follow the real smoothed spine by arc length (so L, U and zigzag arrows never get a trail cutting a bend), are sized from the arrow's tube width, keep the arrow body on top, and leave flight distance, easing, hit testing and collision logic untouched. `CurvePath` gained bend arc-lengths and tangent/normal sampling, the per-level curve cache replaced per-frame flattening, and the old fixed trail/exhaust/flowing-energy passes were removed. New `LaunchCurveTest` (10 tests) pins the geometry across straight/L/U/zigzag × four directions; debug build passes. `LevelAssetValidatorTest.everyShippedLevelKeepsRenderedArrowsVisuallySeparated` fails, but it fails identically at HEAD with only the level assets from the earlier uncommitted regeneration, so it is level-content work, not launch work. |
| 2026-10-01 | Fixed bent arrows launching like static curved sprites sliding at the exit. Added a shared `drawTubeFlow` spine fill that every launch personality now sits on: the arrow's own tube is walked in 14 chunks by arc length on the real quadratic Bézier and each chunk is drawn lit or cold according to the travelling front (`flowLitAt`), so light visibly progresses tail → segment → bend → bend → arrowhead on L, U, zigzag and multi-cell arrows. Cold chunks are painted over the already-drawn tube instead of omitted, so the outline never breaks; bends keep a fading afterglow as the front rounds them; `ArrowShape.draw` gained `headLight` so the arrowhead ignites last while staying welded to the tube. The front reads a new linear `clocks` Animatable alongside the existing rocket-eased `flights`, because the rocket easing barely moves at the start of the launch and the flow needs an even rate across every bend. All flow layers are sized in tube widths, widest being a tube and a third. `CurvePath.fillPath` fills a caller-supplied Path so a launch frame allocates nothing. Flight distance, `LaunchMillis`, easing, collision logic, hit testing and rules untouched. `LaunchCurveTest` grew 3 tests (16 total) pinning tail-to-head fill, per-bend reach, and monotonic falloff across straight/L/U/zigzag × four directions; debug build passes. `LevelAssetValidatorTest.everyShippedLevelKeepsRenderedArrowsVisuallySeparated` still fails, unchanged and still level-content work. |
| 2026-10-02 | Expanded the gameplay layout so the board uses the whole screen. `BoardMetrics` now fills the plate on both axes (with a 0.30-cell plate inset derived from a probe fit) instead of locking the level's square ratio and leaving ~40% of the plate bare; cells stay square because the levels are authored to the play area's band, and a window drifting past 0.78–1.26 cell aspect still locks to the level's own ratio so arrows are never stretched. The background matrix dot field now runs on the grid's cell pitch across the entire plate — including the lanes past the grid, which fade into the frame — instead of stopping at the level's cells. Regenerated all 50 level assets as portrait boards (`3x5`…`7x11`, 15→34 arrows) via `tools/generate-levels.ps1`, which now takes width and height separately and derives its silhouettes from normalised half-extents; plate usage rises from 56–58% to 71–88% on a phone with cells the same size or larger (pack 5 goes 39→41dp cells and 64→77 cells). Two generator fixes came with it: `Get-Footprint` was being handed the head-first body when it reads the head off the end of the list, so every multi-cell arrow's clearance was measured mirrored — that bug is why `everyShippedLevelKeepsRenderedArrowsVisuallySeparated` had been failing on 23 of 50 levels, and it now passes on all 50; and the clearance test carries a bounding box and exits early, which cut a full regeneration from tens of minutes to a couple. The generator also reports each level's tightest rendered clearance and sweeps up to 60 seeds for the roomiest layout, warning about any level that cannot reach it. `FallbackLevel` and `LevelGeneratorTest.configFor` follow the new sizes. 35 unit tests and the debug build pass. |
| 2026-10-02 | Replaced the five random launch personalities and the rigid-body slide with a single liquid-flowing-through-a-pipe departure. A flow front (`pipeFront`) walks the arrow's own flattened spine in arc length from tail to tip, and every decision is a function of where it has reached: behind the front the body is drawn as a hairline on that same path at `collapsedWidth` (30% of the tube) with a wetness ramp that brightens toward the head but never below `TrailFloor`, so a whole L, U or zigzag is visibly converted rather than just its last cell; ahead of the front the untouched stretch goes through the new `ArrowShape.drawTubeOn`, which is the arrow's own keyline, glow rims, tube and core at the arrow's own widths and alphas, so the not-yet-drained part is still pixel-identical to the resting arrow; the arrowhead rides the front through the new `ArrowShape.drawHeadAt`, which translates and turns the same kite about its own centroid (`headPivot`, `headAxis`) with no scaling or re-pathing, so it turns each bend instead of sliding across it and lands back on the arrow's own axis at the tip with no snap. A meniscus (ramped cuff + soft pool) covers the step from tube to line. The translation now exists only after the drain finishes: `trailStartAt` walks the line's start forward to meet the head so the trail dissolves from the tail and the head is the last thing gone. Two sequential clocks replaced the rocket-eased flight plus flow clock — a linear drain (`PipeDrainMillis` 400) then `LinearOutSlowInEasing` exit (`PipeExitMillis` 220) — so the front crosses every bend at an even rate and the handover has no stall. Removed `LaunchAnimation`, `LaunchFx`, `LaunchFrame`, the per-personality draws and `ArrowShape.draw`'s now-dead `headLight` knob. `LaunchCurveTest` stays at 16 tests, rewritten against `pipeFront`, `trailLitAt`, `trailStartAt`, `collapsedWidth`, `headLitAt`, `aheadAlphaAt` and `headTurnDegrees` across straight/L/U/zigzag × four directions. `assembleDebug` and all 16 launch tests pass; `LevelAssetValidatorTest.fiftyLevelsAreShippedAcrossFivePacks` now also fails because the assets folder holds a stray `pack-00-*` file (51 files, not 50) — level content, not launch work. |
| 2026-10-02 | Made the launch one continuous animation. The splash is no longer a routed screen: `ArrowPuzzleApp` composes Home from the first frame underneath `ui/SplashOverlay.kt` and dissolves the overlay off the top of it, so the menu is measured, laid out and drawn (progress flows, banner slot and all) while the brand is still on screen, and the handoff only has to cross-fade an already-finished screen into view instead of composing, measuring and loading it in the one frame where the change would be visible. The curves live in `ui/SplashTransition.kt` as pure functions: an intro clock that settles the wordmark and arrow from 1.06 (`SplashIntroMillis` 460), and a reveal clock (`SplashRevealMillis` 320) that both layers read, so the splash field fades to zero exactly as the menu reaches full opacity and neither can disagree about a frame. The mark is gone by the middle of the handoff (`splashMarkAlphaAt`) and the menu is fully opaque at 86% (`HomeFadeEnd`), so nothing is still moving when the overlay leaves the composition. Both clocks are read inside `graphicsLayer` blocks only, so a handoff frame repaints two layers and recomposes nothing, and the final unmount lives in `SplashLayer` so it cannot invalidate the menu underneath. Both clocks wait on a real measured frame (`onSizeChanged` + `withFrameNanos`) instead of a timer, so a slow device reaches the start of the animation later rather than starting it part-way through. The splash field is the menu backdrop's own `IvoryTop`/`Ivory`/`IvoryBottom`, and the theme's `windowBackground` is the same `launch_field` colour, so the launch never flashes the platform white and the cross-fade has no colour to move; the menu arrives slightly oversized so it can only overrun the window and be clipped, never inset and expose bare edges. The overlay swallows touches while it is mounted, so a tap during the hold or the handoff cannot reach the menu behind it. New `SplashTransitionTest` (12 tests) pins the curves, the 250-400ms window, monotonic fades, the absence of a dead frame, and that the menu is finished before the splash leaves. `SplashScreen.kt` is replaced by `SplashOverlay.kt`; `AppScreen.SPLASH` is gone, `screen` starts at `HOME`. All unit tests and the debug build pass; verified on-device over adb. |