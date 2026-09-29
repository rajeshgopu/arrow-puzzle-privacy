# Arrow Puzzle — UI Plan

Reference: [Arrow Puzzle: Tap Puzzle Games](https://play.google.com/store/apps/details?id=com.easybrain.arrow.puzzle.game) (Easybrain).
This plan keeps the existing `MAIN_SYSTEM.md` V1 scope (offline, portrait, 50 levels,
3 lives, stars, AdMob) and lays out the UI to match the reference game's calm,
minimal, tap-to-clear feel.

## 1. Design Language

| Token | Value (existing `ui/theme/Theme.kt`) | Use |
| --- | --- | --- |
| Background | `CanvasWhite` → `Cloud` vertical gradient | Every screen |
| Card / surface | `#FFFFFF` + soft shadow, 16–26dp radius | Panels, overlays, tiles |
| Primary action | `Coral` (+`CoralDark` pressed) | Play, Next, retry |
| Accent tiles | `Coral`, `TileBlue`, `Gold`, `Mint` | Arrow tile faces |
| Text | `Ink` primary, `InkSoft` secondary | All copy |
| Disabled / locked | `Locked` | Locked level tiles |

Rules:
- Portrait only, generous whitespace, no timer anywhere.
- Arrow glyph is a solid white shape (already drawn in `MainActivity.kt:665`) so
  direction is readable without relying on tile color.
- Everything interactive is ≥ 48dp; all cards use `RoundedCornerShape`.
- Corners/typography already fit; no redesign needed, only consolidation.

## 2. Navigation Map

```text
Splash/UMP ──► Home ──► Level Select ──► Gameplay
                 ▲            │              │
                 │            │              ├─► Pause overlay
                 └────────────┘              ├─► Victory overlay ──► Level Select
                 └── Settings ◄──────────────┴─► Failure overlay ──► Retry / Levels
                       └── Privacy Options (UMP)
```

Current state: Home, Level Select, Gameplay, and Win/Loss overlays already exist
inside `MainActivity.kt`. Missing: Pause, Settings, How-to-Play, Hint control,
Privacy Options. Add a real `AppScreen` nav host when Pause/Settings land.

## 3. Screens

### 3.1 Home
Already implemented (`MainActivity.kt:176`).

```text
┌──────────────────────────────┐
│           ARROW              │  large wordmark
│           PUZZLE             │  Coral second line
│        ┌──────────┐          │  hero arrow card (196dp)
│        │    ->    │          │
│        └──────────┘          │
│   [ LEVEL 7 ]  [ STARS 14 ★ ]│  StatChip row
│                              │
│      (           )           │  reserved banner slot
│      (           )           │  (68dp, hidden pre-ad)
│  ██████  P L A Y  ████████   │  Coral primary button
│           [⚙] [🔊]           │  Settings + sound quick toggles
└──────────────────────────────┘
```
Additions: Settings gear, sound/haptic quick toggle, adaptive-banner slot
(`ads/` package) pinned above the safe area. Continue-vs-Play label switches to
`CONTINUE` when `highestUnlocked > 1`.

### 3.2 Level Select
Exists as a 3-column grid (`ui/LevelSelectScreen.kt`). Reference game uses a
chapter ("pack") map. Plan:

```text
┌──────────────────────────────┐
│ [<]      SELECT LEVEL     [★]│
│ ┌──────────────────────────┐ │
│ │ PACK 1  •  3 of 10       │ │  PackHeader (exists)
│ │ ██████░░░░░░░░  ★ 6      │ │
│ └──────────────────────────┘ │
│  ◀ PACK 1 / 5 ▶              │  horizontal pack pager
│  ┌────┐ ┌────┐ ┌────┐        │
│  │ 1  │ │ 2  │ │ 🔒 │        │  LevelTile grid (exists)
│  │★★★ │ │★★☆ │ │ 4  │        │
│  └────┘ └────┘ └────┘        │
│        (           )         │  banner slot
└──────────────────────────────┘
```
Additions: pack pager (1–5), swipe/scroll between packs, total-stars chip, banner
slot. Keep lock/star states and the existing `LevelTile` art.

### 3.3 Gameplay
Core screen (`MainActivity.kt:252`). Reference HUD is a single compact row.

```text
┌──────────────────────────────┐
│ [<]      LEVEL 7        [||] │  back / title / pause (replaces refresh; restart moves into Pause)
│  ♥ ♥ ♡          3 left       │  LivesRow + remaining counter
│  ██████░░░░░░░░░░░░░░        │  progress bar (Mint)
│       CLEAR A PATH TO EDGE   │  contextual status line
│  ┌────┐┌────┐┌────┐┌────┐    │
│  │ -> ││ ↑  ││ ↓  ││ -> │    │  ArrowBoard, square, 8dp gutters
│  ├────┤├────┤├────┤├────┤    │  tiles: gradient face, white glyph
│  │ ←  ││ -> ││ ↑  ││ ←  │    │  removal = scale+fade spring
│  └────┘└────┘└────┘└────┘    │  invalid = shake + Coral border
│                              │
│  [ 💡 HINT 3 ]    [ ⟳ ]      │  hint w/ remaining count, quick restart
└──────────────────────────────┘
```
Additions: pause button, Hint button wired to `PuzzleReducer.firstValidMove`
(pulse the tile 3×, cost 1 hint), move restart to Pause so the HUD stays calm.
Invalid-tap feedback (`PATH BLOCKED`, shake, haptic) already works.

### 3.4 Pause Overlay
Centered card over a dimmed board.

```text
┌──────────────────────────────┐
│           PAUSED             │
│   [   RESUME   ]             │  Coral
│   [   RESTART  ]             │  Cloud
│   [ SETTINGS   ]             │
│   [ LEVELS     ]             │
└──────────────────────────────┘
```

### 3.5 Victory Overlay
Existing `ResultOverlay` (`MainActivity.kt:477`); keep stars, add reward feel.

```text
┌──────────────────────────────┐
│         LEVEL CLEAR          │
│        ★ ★ ★                 │  staggered pop-in
│   +2 ★   Best: ★★★           │
│   [  NEXT LEVEL  ]           │  Mint
│   [    REPLAY    ]           │  Cloud
└──────────────────────────────┘
```
Interstitial (every 2 completions) shows here, before `NEXT LEVEL` enables.

### 3.6 Failure Overlay
Existing overlay; add rewarded continue as the top action when available.

```text
┌──────────────────────────────┐
│        OUT OF LIVES          │
│   [▶ CONTINUE +1 ♥ (ad)]     │  rewarded, once per attempt
│   [    TRY AGAIN    ]        │
│   [      LEVELS     ]        │
└──────────────────────────────┘
```
After the rewarded option is used, hide it and show only Try Again / Levels.

### 3.7 Settings
New screen from a gear on Home / Pause.

```text
┌──────────────────────────────┐
│ [<]        SETTINGS          │
│  Sound            [ on ]     │
│  Haptics          [ on ]     │
│  Privacy Options          >  │  UMP (only when required)
│  Privacy Policy           >  │  web link
│  How to play              >  │
│  Version / About             │
└──────────────────────────────┘
```
Backed by existing `ProgressRepository.setSoundEnabled/setHapticsEnabled`.

### 3.8 How to Play / Tutorial
Overlay on first Level 1 launch (the staged tutorial at
`MainActivity.kt:267` already exists). Keep the guided first-move flow and add a
3-panel "how to play" card for a manual re-open from Settings.

## 4. Component Inventory

Extract shared UI out of `MainActivity.kt` into `ui/components/`:

- `ArrowTileFace` — gradient tile + glyph (exists, move)
- `ArrowBoard` — grid host with entrance/removal animation (exists, move)
- `LivesRow`, `StatChip`, `RoundIconButton`, `ResultOverlay` (exist, move)
- `PrimaryButton` / `SecondaryButton` — unify button styling
- `HintButton` — icon + remaining count badge
- `BannerAdSlot` — fixed-height reserved container, fills when ad loads
- `PackHeader`, `LevelTile` (exist in `LevelSelectScreen.kt`)
- `LevelSelectPager` — new horizontal pack pager
- `DimOverlay` — reusable scrim behind Pause/Victory/Failure

## 5. Motion & Feedback

| Event | Motion |
| --- | --- |
| Board enter | tiles stagger in, scale 0.8→1, ~40ms apart |
| Valid tap | tile scale→0 + fade, spring; light haptic |
| Invalid tap | 3× shake + Coral border, `PATH BLOCKED`, long-press haptic |
| Hint | target tile pulses 3× (scale + white ring) |
| Star earned | staggered scale/overshoot pop-in |
| Overlay in/out | fade + slight scale, 200ms |
| Lives lost | heart fades to outline |

Reuse existing `shake` modifier and `Haptics` from `ui/Feedback.kt`.

## 6. Responsive & Accessibility

- Board sized with `BoxWithConstraints`: `min(maxWidth, maxHeight * 0.62)`, so
  4×4…7×7 and tablets all keep square cells.
- Respect `hapticsEnabled` / `soundEnabled`; scale status/spinner text with
  system font scale; all hit targets ≥ 48dp.
- Direction must be legible from glyph shape alone (color-blind safe).
- Support Android back: Gameplay→Pause→Level Select→Home; overlays close on back.

## 7. Implementation Mapping

| Area | File |
| --- | --- |
| Nav host + screen enum | `app/AppNavigation.kt` (new), replaces `AppScreen` in `MainActivity.kt` |
| Home | `ui/HomeScreen.kt` (extract) |
| Level select + pager | `ui/LevelSelectScreen.kt` (extend) |
| Gameplay + HUD | `ui/GameplayScreen.kt` (extract from `MainActivity.kt`) |
| Pause / Victory / Failure | `ui/overlays/` |
| Settings / Privacy / How-to | `ui/SettingsScreen.kt` |
| Shared components | `ui/components/` |
| Hint logic | `PuzzleReducer.firstValidMove` (already exists) |
| Ad slots | `ads/BannerAdSlot.kt`, `ads/` pacing |

## 8. Phases

1. **Consolidate** ✅ — move components/screens out of `MainActivity.kt`; keep visuals identical.
2. **HUD + overlays** ✅ — Pause button, Pause screen, Hint button, polished Victory/Failure, staggered star pop-in, and lives-lost heart animation.
3. **Settings & privacy** ✅ — Settings screen, toggles wired to DataStore, How-to-Play overlay, version/About, and conditional Privacy Options/Policy entries.
4. **Level select** ✅ — horizontal pack pager (1–5) with prev/next controls, total-stars chip; empty packs show a "coming soon" placeholder until levels 11–50 are authored.
5. **Monetization UI** ✅ — `ads/` seam (`AdController`/`AdHost`/`BannerAdSlot`), banner slots on Home + level select, paced interstitial (every 2 wins), rewarded `CONTINUE +1 ♥`. Debug simulates ads; release is `NoAds` until AdMob is bound.
6. **Polish** ✅ (code) — staggered board entrance, tile/level accessibility labels, 48dp icon targets, and a tablet board size cap. On-device compact/tablet QA still recommended.
