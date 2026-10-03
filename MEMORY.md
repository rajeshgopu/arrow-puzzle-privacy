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
- Google Play Games is used for player identity only, and is optional throughout:
  a player who has none, is offline, or declines is a guest and loses nothing.
  It is never a precondition for playing, and it never touches progression.

## Technology Decisions

- Kotlin + Jetpack Compose + Material 3.
- Android package and application ID: `com.gopu.arrow.puzzle.game`.
- Android target API 36 and proposed minimum API 24.
- DataStore Preferences for progression and settings.
- Google Play Games Services (`play-services-games-v2`) for player identity,
  read once at launch and never waited on.
- Levels are versioned JSON assets.
- Localization uses Android string resources, with the language chosen in
  `i18n/AppLanguage.kt` and applied as a composition-local overlay in
  `i18n/LocalizedApp.kt`. No third-party i18n library, and no
  `AppCompatDelegate` backport: the app is a plain `ComponentActivity`, and an
  overlay recomposes strings without recreating the activity, so a language
  change cannot disturb the board in play. `LocalAppContext` exists because
  `LocalContext` changes with the language, and anything long-lived (the ad
  controller, the repositories) must not be rebuilt when it does.
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

- [x] Redesigned the first screen to match the store art: a violet-indigo sky
  field with soft-focus lights, distant cubes and planting along the bottom edge;
  a chunky 3D ARROW PUZZLE logotype (gold over ice, cocoa outline, extruded
  edge) with three glossy arrow cubes floating around it; a 4x4 floor of glossy
  cubes in perspective with four arrows still on it and light firing out of the
  blue one; two frosted glass stat plates; a gold PLAY pill; and a glossy blue
  settings disc. New `ui/components/HomeArt.kt` owns all of the vector art behind
  it, `Wordmark`/`GlassStatCard`/`PlayButton`/`GlossyIconButton` replace the old
  ivory menu furniture, and the now-unused `HeroArrow` is gone. The splash field
  and the theme's `windowBackground` were moved to the new `MenuSkyTop`, so the
  launch still has no colour to move at either end.
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
  level select; rewarded `CONTINUE +1 ♥` on failure with 1-star cap. AdMob and
  UMP are now bound for both build types (see the ads release-readiness entry
  below). Enabled `buildConfig`. Debug and release builds pass.
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
- [x] Ads are release-ready apart from two machine-local secrets. Every AdMob
  value now comes from one place: `app/build.gradle.kts` reads `admob.appId`
  and the three `admob.*UnitId` keys from `local.properties` or `-P`, passes
  the units in as `BuildConfig` fields, and `AdUnitIds` just reads them, so the
  production units are no longer source constants that a rebuild cannot change.
  Debug is still pinned to Google's test app and units at the Gradle level, so
  no debug build can serve or click a live ad even if the live IDs are
  configured. The manifest app ID keeps its existing fallback and warning.
  UMP consent is re-read on every foreground pass (`MainActivity.onStart`
  rather than `onCreate`), so a form answered, dismissed or left open across a
  pause still releases the gate and a declined player is asked again next
  launch; `AdConsent` tracks the activity a round-trip is waiting on so forms
  cannot stack and a lost callback cannot wedge the gate shut.
  `AppConfig.privacyOptionsAvailable` is on and Settings' Privacy Options row
  reopens the form through `AdConsent.showPrivacyOptions`, reporting when the
  region needs no choices rather than looking unresponsive. `privacyPolicyUrl`
  moved to a `privacyPolicyUrl` build key so Play's policy page can be set
  without touching Kotlin. Deleted `SimulatedAdController`, which nothing bound
  since the real provider landed. `local.properties.example` and a README
  section document the keys. Debug build, release APK and unit tests pass; the
  two level-content tests still fail, unchanged.
- [x] Localized the game into seven languages - English (default and fallback),
  German, French, Spanish, Brazilian Portuguese, Japanese and Korean - and
  moved every user-visible string out of the Compose sources into
  `res/values*/strings.xml`. The status line used to hold its rendered text and
  clear itself by comparing that text back to an English literal; it now holds
  the *cause* of the flash (`StatusFlash`) and resolves the string when it draws,
  which is what makes the clear rule independent of language. `Language` is
  applied as a composition-local overlay rather than an activity restart, so
  changing it cannot disturb the board in play, the saved stars, the toggles or
  the tutorial flag - and `LocalAppContext` keeps the ad controller and the
  repositories keyed to something that does not change when the language does.
  Poppins is Latin-only, so `Type.kt` routes Japanese and Korean to the platform
  CJK family via `AppLanguage.needsSystemFont` instead of shipping a second
  multi-megabyte font; the wordmark stays on Poppins everywhere. A handful of
  fixed-height controls were made to wrap or shrink rather than clip - the
  gameplay status line, the settings title, the home stat plates, the level
  select title and pack selector, and the result buttons. Three new test classes
  (`AppLanguageTest`, `L10nStringsTest`, `NoHardCodedUiTextTest`) cover the
  locale rules, placeholder parity across all seven files, per-label width
  budgets against the controls they are drawn in, and a source scan that fails
  the build if user-visible text is hard-coded again. Store copy for all seven
  languages is in `docs/store/localization.md`; the Japanese and Korean long
  descriptions there still need a native speaker.

## Next Actions

1. Set `admob.appId` and `privacyPolicyUrl` in `local.properties`; both are
   machine-local and cannot be committed. See `local.properties.example`.
2. Author pack-2 through pack-5 levels (11–50) and keep them passing the
   validator.
3. Have a native speaker write the Japanese store description and proof-read
   the Korean one. Both are marked as such in `docs/store/localization.md`. The
   in-game strings for both languages are finished; only the listing is not.
4. Before release, screenshot the store art in all seven languages or drop the
   captions - text burned into a screenshot PNG cannot be localised after
   upload. See the "In-app text" section of `docs/store/localization.md`.

## Risks and Guardrails

- Confirm the final game name and branding are available before Play Store
  submission. The name "Arrow Puzzle" is now load-bearing in a second way: it is
  deliberately *not* translated, because it is the store listing's search term
  and the wordmark's lettering. Changing the decision means revisiting the store
  copy in `docs/store/localization.md`, not just a resource.
- Keep the language tag in its own DataStore key. It is the one setting that
  changes what the UI says, so it is also the one that must not be able to touch
  progress. `L10nStringsTest` enforces the rest of the localization contract.
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
| 2026-10-02 | Rebuilt the first screen to match the store art. The ivory menu became a violet-indigo sky (`MenuSkyTop`?`MenuSkyDeep`) with soft-focus bokeh, five translucent distant cubes, twinkling glints and three clumps of veined planting along the bottom edge, all drawn back to front in one canvas. The wordmark is no longer two `Text` composables in Playfair: `Wordmark` measures each line once through `rememberTextMeasurer` and paints it twelve times into a single canvas � eight compass passes in the cocoa outline colour to dilate the glyph into a border, three stepped passes in a darker brown for the extruded edge, then the face once in its own gradient (gold `ARROW`, ice `PUZZLE`, Poppins ExtraBold, 0.84 line step) � so it is a chunky logotype that costs two layouts and never reflows. Three `FloatingArrowTile`s (cyan up, green right, red down) orbit it on an aspect-locked block so nothing can shove the mark off centre. `ArrowBoardScene` is a 4x4 floor of plain cubes in perspective: each row is scaled 0.70?1.00, stepped 0.56 cells and re-centred, which is what makes it recede, with four arrows still on it and light firing right out of the blue one over the top of the board. `glossyCube` is the one cube painter the floating tiles, the board and the plain floor all share � silhouette, face, sheen, rim light, and a stamped white glyph with its own shadow and top bevel. `ProgressCard` and `GlassIconButton` became `GlassStatCard` (frosted plate, rim, sheen, deep-blue value shadow, drawn crown/star badge) and `GlossyIconButton` (glossy blue disc with its own glow); `PlayButton` is now a gold pill with a 3D orange underside. The menu reads light on a dark field, so `CardSurface`/`TealInk` stay only for the light screens. The splash field and `launch_field` moved to the new `MenuSkyTop` so the handoff still has no colour to move, and the unused `HeroArrow` is deleted. Debug build passes; `LevelAssetValidatorTest.everyShippedLevelKeepsRenderedArrowsVisuallySeparated` and `LevelDifficultyTest.harderLevelsAreBuiltFromDependencyChainsNotJustMoreArrows` still fail, identically and unchanged at HEAD, so they are level-content work. |
| 2026-10-02 | Corrected the rebuilt menu against the store art. The cube above the title no longer lands on the letters: `WordmarkBlock` now measures the mark (1.70 em, converted through `LocalDensity` so it is right at any font scale) and places every cube from that, with the top cube clearing the mark by a share of its own height instead of by a ratio against a fixed box, and the block is tall enough to hold the lowest cube so nothing spills out of it. The cubes read as moulded plastic now: `glossyCube` gained a clipped bright lens across the top of the face, a harder sheen band, a shaded band along the bottom that turns the flat face into a shallow dome, a crisp specular line just inside the top edge, and a rim that fades down the sides - all clipped to the face path so nothing bleeds past the corners. The board is one uniform pitch and one cube size across all rows, so the columns line up (it was scaling 0.70 to 1.00 per row and re-centring each, which is what read as misalignment); it is 5x3 now so it fills the width, with a contact shadow under the front row carrying the depth. The crown and star badges were invisible because `scale()` pivots on the draw-scope centre, which pushed the unit-space outline into the bottom-right corner and off the plate; both are now built in absolute pixels about the centre, carved with a dropped dark pass, a clipped top bevel, and jewels on the crown. `PlayButton` went deeper orange (`PlayMid` #FF8A12, `PlayBottom` #FF5E00, `PlayEdge` #B83600) and got a real bloom - three rounded plates of falling alpha a little larger than the pill plus a hot core - instead of one radial that sat entirely inside the button and was never visible. Debug build passes; the same two level-content tests still fail, unchanged at HEAD. |

| 2026-10-02 | Made every screen swap instant and blank-free. A tap used to swap what was in the `when` in one frame while the destination had still not been composed, measured or drawn, so the bare window background showed for however long that took - worst on the level select, which was parsing five packs of level JSON inside the branch, on the frame the player tapped Play. A swap is now two screens composed at once rather than two in sequence: `ScreenHost` keeps the screen being left on screen underneath the destination, both at `fillMaxSize`, and the destination is composed, measured and drawn on the very frame after the tap with the outgoing one still fully opaque over it - so the cost is paid where it cannot be seen, which is also why the destination's own entrance animations have already begun by the time any of it is visible. The clock starts only once the destination has been measured and one real frame has been produced (`withTimeoutOrNull` + `snapshotFlow` + `withFrameNanos`, no timer), and then one `ScreenSwapMillis` 200ms clock cross-fades them: the two alphas are built so they can never sum to less than one, which is the property that stops the window showing through the middle of the swap, and the destination is opaque at 70% of the clock so the frame the outgoing screen is dropped on is already the finished destination. Scale is a settle only - 0.965 up into place, 1.015 back - and every value is read inside a `graphicsLayer`, so a frame of a swap repaints two layers, recomposes nothing and runs no second layout pass. The clock is `remember`ed per swap id rather than snapped, so a destination can never flash at the end of the previous transition and a second tap mid-swap cancels the first; the destination subtree is keyed by the same id, which is load bearing rather than tidiness - without it the destination is the same layout node every time and `onSizeChanged` only reports a size *change*, so no destination after the first would ever report itself ready and every swap sat on the backstop. The level packs moved up out of the level-select branch to the top of the app, putting the JSON work on the launch behind the splash instead of on the tap frame. The outgoing screen swallows touches for the length of the swap so a second tap on the screen being left cannot navigate out from under the one already in flight. Curves live in `app/ScreenTransition.kt`; new `ScreenTransitionTest` (8 tests) pins the window, the coverage floor, the opaque-before-the-end guarantee, monotonicity and the scale bounds. Debug build passes; `LevelAssetValidatorTest.everyShippedLevelKeepsRenderedArrowsVisuallySeparated` and `LevelDifficultyTest.harderLevelsAreBuiltFromDependencyChainsNotJustMoreArrows` still fail, unchanged at HEAD. Fixed in the same pass: the destination layer was reading the incoming curve directly, and that curve is zero at rest - correct for a destination, fatal for the menu the splash hands off to, since the clock sits at zero until the first navigation and the splash therefore faded out onto an invisible screen. `screenAlphaAt`/`screenScaleAt` now take whether a swap is actually in flight, so a screen that is simply on show sits at full strength and its own size whatever the clock is doing; two new tests pin both halves of that decision. |

| 2026-10-02 | Gave the menu board real volume and made the level plate a shortcut. The board cubes were flat rounded squares on a grid, which read as a flat diagram: a cube lying on the floor is not a square, so the new `glassFloorCube` draws what one actually is - a squashed top plane at `TopFaceShare` of the edge over a lip of front face at `LipShare`, with a contact shadow under it, a soft halo around it, and the pitch set just under a cube's whole height so each row tucks under the one in front. The plane carries a diagonal specular streak swept across it at `GlassStreakDegrees`, a shaded band along the near edge and a bright line where the two faces meet, which is what makes it read as glass rather than as a swatch; the arrow on each cube is now painted foreshortened onto that plane (`drawCubeGlyph` takes a `squash`) instead of stood up on it. The plain cubes sit in a faint cool halo so the floor glows as well as the arrows, and the columns still line up because there is still one pitch and one cube size for the whole board - the depth is in the cubes, not in the grid. `GlassStatCard` is now a control when given an `onClick`: the menu's level plate goes straight into the level it reports, pressing it dims the sheen and springs the plate back (`MutableInteractionSource`, no ripple to fight the glass), and it announces itself as a button with the level in its content description; the star total stays a tally. `openLevelOrder` in `AppNavigation` loads that level the same way the level select does and falls back to the first level rather than dropping the player on a board that was never read, then swaps to gameplay on the normal two-screen cross-fade. Debug build passes; the same two level-content tests still fail, unchanged at HEAD. |

| 2026-10-02 | Took the board off the menu. The screen is three things now with nothing between them: the wordmark and its three orbiting cubes, the level and star plates directly under it, and the action held at the optical centre of the space below. `HomeScreen` lost `ArrowBoardScene` entirely and the two gaps either side of `PlayButton` are now weighted (`0.85f` above, `1.15f` below) instead of the button sitting in the flow, so the action lands in the same optical spot on a 4:5 phone and a 21:9 one and stays inside the safe area. `ArrowBoardScene`, its art table, `glassFloorCube` and the cube shape constants went with it - they existed only to draw the boxes - and `drawCubeGlyph` lost the `squash` it only had for a floor plane it no longer paints. The level plate still opens its level directly and the star total is still a tally. Debug build passes; the same two level-content tests still fail, unchanged at HEAD. |

| 2026-10-02 | Closed out the ads release-readiness list. The AdMob app ID and all three ad unit IDs are now build inputs: `app/build.gradle.kts` reads `admob.appId`, `admob.bannerUnitId`, `admob.rewardedUnitId` and `admob.interstitialUnitId` from `-P` or `local.properties` through one `adValue` helper, injects the units as `BuildConfig` strings per build type, and `AdUnitIds` reads those - so the live units are no longer constants compiled into the app, and debug is pinned to Google's test app plus test units at the Gradle level, which holds even when the live IDs are configured. `privacyPolicyUrl` became a build key behind `BuildConfig.PRIVACY_POLICY_URL` so the Play listing page can be set without editing Kotlin, and `AppConfig.privacyPolicyUrl` hides the Settings row while it is blank. Consent moved from `onCreate` to `onStart` so it is re-read on every foreground pass: a form answered, dismissed or left open across a pause now releases the gate, and a player who said no is asked again next launch instead of staying off for the life of the install. `AdConsent` tracks the activity a round-trip is waiting on, so a second foreground pass cannot stack a second form and a lost callback cannot wedge the gate shut for later launches. `showPrivacyOptions` backs the Settings row, which Play requires once ads are served, and reports through `onResult` when a region needs no choices so the tap is not silently dead; the answer is shown above the version line. Deleted `SimulatedAdController`, dead since `defaultAdHost` began binding the real provider - nothing referenced it. Added `local.properties.example` and a README section. Debug build, release APK and 145 unit tests pass; `LevelAssetValidatorTest.everyShippedLevelKeepsRenderedArrowsVisuallySeparated` and `LevelDifficultyTest.harderLevelsAreBuiltFromDependencyChainsNotJustMoreArrows` still fail, unchanged level-content work. Still needs the two machine-local values: `admob.appId` and `privacyPolicyUrl`. |
| 2026-10-02 | Put a banner on the gameplay screen and made it safe to look at. The banner is the last child of the GameplayScreen column, below the HINT and Restart row, so it sits furthest from the board - the one large tap target there - and because it is last, adding it cannot move the HINT controls: everything above a weight(1f) child absorbs the change by shrinking the board instead. That shrink is the reason for the new ReservedBannerAdSlot, which reserves AdController.bannerHeight from the first frame and never resizes: the free BannerAdSlot used on Home and level select is still fine because those screens have no board to move, but here a banner filling late would have resized the board under the player's fingers mid-level. The band is pinned to the same height it reserves, so a taller creative is cropped by the band rather than spilling over the controls below, and it is only reserved when a banner can be sized, so an unbound provider (AdHost(NoAds), previews) costs nothing. AdMobController owns the size decision in one `bannerSize` helper that both `createBannerView` and `bannerHeight` call, so the height reserved and the height the view takes cannot drift apart. That helper was also where test banners were silently broken: it passed displayMetrics.widthPixels to getCurrentOrientationAnchoredAdaptiveBannerAdSize, which takes **dp**, so the SDK built a 1080x90 creative for a 412dp-wide phone and refused every request with error code 1, `Ad size will not fit on screen. a_w=1080, a_h=90, s_w=412` - a banner that can never load on any real device. Converting to dp fixed it; the device log now reads `size=411x64dp` followed by `Banner loaded`. Added startup diagnostics under the `Ads` tag - the AdMob app ID actually compiled into the merged manifest, the chosen banner size, the reserved band height, and the existing load failures - because an app-ID/unit-ID mismatch is the failure that otherwise costs the most time to find and the SDK reports it quietly. Verified on a connected device: test banner renders below HINT with no control covered. Debug build and 145 unit tests pass; the two level-content tests still fail, unchanged. |

| 2026-10-03 | Declared the app as a game and opted into Android Game Mode. `<application>` carries `android:appCategory="game"` (the API 26+ attribute, packed as enum value 0 = game) and the launcher intent-filter keeps `android.intent.category.GAME` beside `LAUNCHER`, so game hubs and OEM launchers that filter on `CATEGORY_GAME` can find `MainActivity`; `MAIN` + `LAUNCHER` resolution is unchanged, so the home screen icon and Play Store behaviour are untouched. Added `<meta-data android:name="android.game_mode_config" android:resource="@xml/game_mode_config" />` and the new `res/xml/game_mode_config.xml` with `supportsBatteryGameMode="true"` and `supportsPerformanceGameMode="true"`, which is how an app advertises to Game Mode interventions that it wants both the battery-saver (CPU/GPU/duty-cycle) and performance (peak clocks) interventions. The deprecated `android:isGame` is not used. No in-game Game Mode UI: the config is a static declaration, so only devices and OEMs that ship Game Mode act on it, and nothing in the app pretends a switch exists where it does not. Verified in the packaged debug APK with `aapt2 dump xmltree`: `appCategory(0x01010545)=0`, the `android.game_mode_config` meta-data resolving to `@0x7f0e0000`, `res/xml/game_mode_config.xml` present with both `supportsBatteryGameMode(0x01010656)=true` and `supportsPerformanceGameMode(0x01010657)=true`, and LAUNCHER + GAME both in the filter. Debug build passes and 145 of 147 unit tests pass; `LevelAssetValidatorTest.everyShippedLevelKeepsRenderedArrowsVisuallySeparated` and `LevelDifficultyTest.harderLevelsAreBuiltFromDependencyChainsNotJustMoreArrows` still fail, unchanged level-content work that reads level assets only. Release manifest processing passes but `validateSigningRelease` still fails on this machine because `keystore\arrow-puzzle-release.jks` does not exist - a missing local keystore, not a manifest or resource error. |
| 2026-10-03 | Added Google Play Games identity without putting it in the player's way. `playgames/PlayGamesIdentity.kt` is a process-lifetime singleton on the current `play-services-games-v2` SDK (22.1.0, added as a version-catalog entry and the only new dependency), following the shape `AdConsent` already established: three `StateFlow`s - `isAuthenticated`, `playerName`, `playerId` - a claim flag, a `WeakReference` to the borrowed activity, a `SupervisorJob` scope, and `runCatching` around every SDK call. `MainActivity.onCreate` calls `signInSilently(this)` once, before `setContent`, and deliberately not from `onStart`: the claim flag is taken synchronously, so no resume and no configuration change can start a second attempt or re-ask a player who declined. The flow is Google's own in the order that matters - `PlayGamesSdk.initialize` (idempotent, no UI, a belt-and-braces on top of the SDK's own `PlayGamesInitProvider`), then `GamesSignInClient.isAuthenticated()`, which answers silently and is the whole flow for anyone who has opened the game before, and only if that says no, `signIn()`, the one call that can put a screen up. That screen is Google's `GamesResolutionActivity`, not a stand-in this game draws, and Google decides whether it is needed at all. The device is checked first (`GoogleApiAvailability` plus a Play Store package lookup, `play-services-base` already on the classpath through AdMob) so a phone that cannot reach Play Games is never asked to sign in - the one path that could surface a failure nobody asked for. A Google `Task` carries no timeout, so the whole authentication phase sits under `withTimeoutOrNull(AttemptTimeoutMillis = 10s)` and a `Task.awaitOrNull()` bridge that drops a result whose continuation is already cancelled, which is what keeps a late answer from resuming a dead continuation. Every outcome that is not a player - no Play Games, no account, a decline, offline, a timeout, any throwable - collapses to the same guest state with no dialog and no error surface; `isAuthenticated` only ever flips on Google's own answer. Progression is untouched and still owned by the game: the level stays in DataStore as `highest_unlocked_level`, `HomeScreen` still reads it through `ProgressRepository`, and nothing in `playgames/` reads, writes or resets it, so a failed sign-in cannot cost a player their progress and Play Games is never told what level they are on. There is no cloud save, matching the standing version-1 decision not to add one. On the menu, `PlayerNamePlate` in `ui/components/Common.kt` is a wide shallow plate in the same glass as `GlassStatCard`, composed only when a name exists - a guest sees exactly the previous menu with nothing reserved - and wrapped in `AnimatedVisibility` (`PlayerPlateFadeMillis = 260`) because the weighted gaps around `PlayButton` share out the height it adds, so appearing instantly would nudge the play button under the player's thumb. No new screen, and no manifest edit was needed: the v2 SDK self-initializes from its own ContentProvider and ships its own `<queries>`, verified present in the packaged debug APK along with `PlayGamesInitProvider`, `GamesResolutionActivity` and the `com.google.android.gms.games.version` meta-data. `README.md` documents the Play Console setup - the app must be enrolled with Play Games Services on, and the SHA-1 of every signing key listed - and notes that the game logs its reason under the `PlayGames` tag; until that exists the player is simply a guest, which is a complete and playable state. Debug build and `compileReleaseKotlin` pass and 145 of 147 unit tests pass, the same two level-content tests still failing unchanged. `assembleRelease` still stops at `validateSigningRelease` for the missing local keystore. |
| 2026-10-03 | Made the shipped launch animation `MINIMAL` instead of `GLOW_PULSE`. `LaunchAnimationStyle.Default` is now `MINIMAL` (`ui/components/LaunchAnimation.kt`): a brief quiet bloom with no ring, no sparks and a 0.16 arrow lift, run for 85ms, so an unconfigured install acknowledges a tap without decorating every move and the bigger styles stay something a player opts into. Nothing else moved - the enum order, the persisted `name` values, `launchProfile`, the settings picker and `fromName` are untouched, so a stored choice still reads back and `fromName(null)` now falls back to `Minimal`. `TapGlowPulseTest.styleDefaultsToMinimalSoAMoveStaysQuiet` and `theDefaultStyleIsTheMinimalBloom` and `TapEnergyBurstTest.everyStyleStillRoundTripsThroughStorage` are re-pinned to the new default. 183 unit tests run; `LevelAssetValidatorTest.everyShippedLevelKeepsRenderedArrowsVisuallySeparated` and `LevelDifficultyTest.harderLevelsAreBuiltFromDependencyChainsNotJustMoreArrows` still fail, unchanged level-content work that reads level assets only. |
| 2026-10-03 | Verified the release build now that the keystore is reachable. `:app:assembleRelease` succeeds end to end - `validateSigningRelease`, `lintVitalRelease`, R8/resource shrinking and `packageRelease` all pass - producing a signed `app/build/outputs/apk/release/app-release.apk` (15.0 MiB, CN=Rajesh Gopu I.V, SHA-256 `e617fa2f257a15a748078021ae7edad1526da83fe7ed7f06a69d0136f1e87947`), confirmed with `apksigner verify --print-certs`. This closes the standing blocker recorded on 2026-10-03: `signing.properties` now points at a real keystore, so the earlier `validateSigningRelease` failure for a missing `keystore\arrow-puzzle-release.jks` no longer applies. No code or build-script change was needed. |
| 2026-10-03 | Generated the Play upload artifact. `:app:bundleRelease` succeeds (`validateSigningRelease`, `signReleaseBundle`, R8 and resource shrinking all pass), producing `app/build/outputs/bundle/release/app-release.aab` (14.5 MiB) alongside the APK from the same build. `jarsigner -verify` reports `jar verified` against the same self-signed release key the APK uses; the PKIX and no-timestamp warnings are what an Android upload key always produces and are expected for Play, which re-signs with the app signing key. No code or build-script change was needed. |
