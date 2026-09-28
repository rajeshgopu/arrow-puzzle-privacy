# Arrow Puzzle: Tap Puzzle Games

## Purpose

Build a polished, portrait-mode Android puzzle game where players remove arrow tiles
only when the arrow has an unobstructed path to the edge of the board.

This document defines the first-release system. New work should stay within this
scope unless the product plan is deliberately updated.

## Version 1 Product Scope

### Player Experience

- Offline-first, single-player puzzle game.
- Five packs with ten handcrafted levels each (50 levels total).
- Increasing board size and arrow-layout complexity across packs.
- Three lives per attempt. An invalid tap removes one life.
- A level is won when all tiles are removed.
- A level is lost when all lives are used; the player can restart or opt in to a
  rewarded ad continuation.
- Completed levels record up to three stars and unlock the next level.
- Progress, settings, and earned stars persist locally.

### Screens

1. Home: Play button, current progress, sound, haptics, privacy, and restore
   settings.
2. Level Select: Five packs and individual level tiles with lock, completion,
   and star states.
3. Gameplay: Board, lives, pause, restart, and a compact optional hint control.
4. Pause: Resume, restart, settings, and return to level select.
5. Victory: Stars earned, replay, and next-level action.
6. Failure: Retry and an optional rewarded-ad continuation.
7. Settings: Sound, haptics, privacy options, and app information.

### Explicitly Out of Scope for Version 1

- Login, cloud save, multiplayer, leaderboards, purchases, daily challenges,
  level editor, social sharing, and remote-configured content.
- Ads inside the active puzzle board or while a player is making moves.

## Core Game Rules

### Board Model

- A level is a rectangular grid, initially using 4x4 through 7x7 boards.
- Each occupied cell holds one arrow facing up, right, down, or left.
- An arrow can be removed only when every cell from its next cell in the arrow
  direction through the board edge is empty.
- Empty cells never block a path. Remaining arrow tiles do block a path.
- A valid tap removes the tile and updates the board.
- An invalid tap keeps the board unchanged and removes one life.

### Completion and Stars

- Win: no arrow tiles remain.
- Lose: lives reach zero.
- Three stars: no invalid taps.
- Two stars: one invalid tap.
- One star: two invalid taps or a rewarded continuation was used.
- A rewarded continuation restores one life once per failed attempt; no second
  continuation is offered on that attempt.

### Level Data

Levels will be stored as versioned JSON assets. Each level includes:

```json
{
  "id": "pack-01-level-01",
  "pack": 1,
  "order": 1,
  "width": 4,
  "height": 4,
  "tiles": [
    { "row": 0, "column": 0, "direction": "RIGHT" }
  ],
  "parMoves": 1
}
```

The level validator must confirm unique positions, in-bounds positions, supported
directions, and that a solution exists before a level is shipped.

## Technical Architecture

### Platform and Stack

- Android only, Kotlin, Jetpack Compose, Material 3.
- Target SDK: API 36 for current Google Play submission requirements.
- Minimum SDK: API 24 unless dependency compatibility requires an increase.
- Gradle Kotlin DSL and a single application module for the MVP.
- DataStore Preferences for local save data.

### Package Boundaries

```text
com.example.arrowpuzzle
  app/          Application setup and navigation
  game/         Pure board rules, level parsing, validation, and state reducer
  levels/       JSON assets and level repository
  data/         DataStore-backed player progress and settings
  ui/           Compose screens, components, theme, and view models
  ads/          AdMob setup, consent, placement rules, and ad state
  analytics/    Event interface and implementation
```

### Game State

The gameplay state must be deterministic and independent from the UI:

```text
LevelDefinition + remainingTiles + lives + invalidTapCount + rewardedUsed
  -> Playing | Won | Lost
```

UI gesture handlers dispatch actions to the game reducer. The reducer validates
the move, produces the next immutable state, and never displays an ad directly.

### Persistence

Save the following locally:

- Highest unlocked level.
- Best star result per level.
- Sound and haptics preferences.
- Whether the introductory tutorial has been seen.
- Ad pacing metadata needed to avoid excessive interstitials.

## Monetization and Privacy

### AdMob Placements

| Format | Location | Rule |
| --- | --- | --- |
| Adaptive banner | Home and level select | Hidden during gameplay, pause, victory, and failure screens. |
| Interstitial | After a completed level | At most once after every two completions; show before the next-level control. |
| Rewarded | Failure screen | Optional; restore one life once per failed attempt. |

Ads must be requested ahead of their allowed display point. A missing or failed
ad never blocks level progression, retry, or navigation.

### Compliance Requirements

- Use Google Mobile Ads SDK test ad unit IDs in debug builds only.
- Use User Messaging Platform (UMP) consent flow at app launch before ad
  requests where required.
- Provide a persistent Privacy Options entry in Settings when UMP requires it.
- Keep a public privacy policy URL for the Play Store and in-app settings.
- Do not reward ad clicks. Reward only verified completion of an opt-in rewarded
  ad.
- Never show an interstitial on app launch, app exit, or during puzzle input.

References:

- https://developers.google.com/admob/android/quick-start
- https://developers.google.com/admob/android/privacy
- https://support.google.com/admob/answer/6201362
- https://support.google.com/googleplay/android-developer/answer/11926878

## Analytics Events

Use an analytics abstraction so the provider can be changed later. Version 1
records no puzzle-content text or personal gameplay input.

- `app_opened`
- `tutorial_completed`
- `level_started` (level id)
- `level_completed` (level id, stars, duration, invalid taps)
- `level_failed` (level id, invalid taps)
- `level_restarted` (level id)
- `rewarded_offer_shown` (level id)
- `rewarded_completed` (level id)
- `interstitial_shown` (completed level id)
- `ad_load_failed` (format)

## Quality Bar

- Unit-test every legal and illegal direction/path case in the game engine.
- Validate every released level is solvable and has no overlapping tiles.
- Test on compact and large phones, tablets, Android back navigation, process
  recreation, offline play, and background/foreground transitions.
- Verify ads do not interrupt player input and production builds never use test
  ad IDs.
- Build a signed Android App Bundle for Play Console closed testing.

## Delivery Phases

1. Foundation: Android project, theme, navigation, board model, and reducer.
2. Playable Loop: gameplay UI, rules, lives, victory/failure, restart, save.
3. Content: tutorial and 50 validated handcrafted levels.
4. Monetization: UMP, AdMob placements, ad pacing, and analytics.
5. Release: QA, store assets, privacy policy, closed testing, and Play release.

## Definition of Done for Version 1

- All 50 levels are playable, validated, and locally progressive.
- The complete core loop works without network access.
- Consent, privacy options, and all three planned ad formats work in release
  builds without interrupting gameplay.
- Automated tests cover game-rule correctness and level validation.
- The app is ready for Google Play closed testing with a signed App Bundle and
  required store declarations.
