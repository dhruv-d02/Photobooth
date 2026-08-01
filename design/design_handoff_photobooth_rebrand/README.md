# Handoff: Photobooth Rebrand (Naming, Visual System, Flow)

## Overview
New visual identity + interaction flow for the pocket photobooth app: naming/logo concepts, a Y2K/scrapbook color & type system, and a full screen-by-screen flow (booth landing → capture countdown/keep-retake → customize flicket/frame/layout → share → strips gallery).

## About the Design Files
The bundled HTML file is a **design reference built in HTML** — an interactive prototype showing intended look, motion, and behavior, not production code to copy directly. The task is to recreate this design in the target codebase's existing environment — here, **Kotlin Multiplatform + Compose Multiplatform** (per the project's `architecture.md`/`CLAUDE.md`) — using its existing composables, theming, and navigation patterns.

## Fidelity
**High-fidelity.** Final colors, typography, spacing, copy, and interaction states are intentional — recreate pixel-close within Compose's layout system.

## Naming Concepts (pick one before implementing)
1. **Boothie** — mascot-style nickname, hot pink wordmark, sparkle mark. (current default)
2. **4EVER** — Y2K numeral-speak, purple wordmark, heart mark.
3. **Picnic** — softer real-word name, gold wordmark, checker mark.

The prototype's `appName` tweak previews any of the three live; current default is **Boothie**.

## Design Tokens

**Color**
- Cream (background) `#FFF7EA`
- Ink (text) `#2B1830`
- Hot pink (primary action) `#FF4FA0` / pressed `#C22A79`
- Purple (accent) `#8A3FFC`
- Gold (accent) `#FFC53D`
- Mint (accent) `#5FE3C4`
- Dark capture/share background: gradient `#3d1f5c → #1B0A2E`

**Frame color presets** (applied to the strip mount)
- Butter `#FFC53D` (dark text `#2B1830`)
- Bubblegum `#FF6FBB` (light text `#FFF7EA`) — default
- Grape `#8A3FFC` (light text `#FFF7EA`)
- Spearmint `#7FEBD1` (dark text `#2B1830`)

**Typography**
- Display / headlines / buttons: **Fredoka**, weight 700
- Body / labels / UI copy: **Nunito**, weight 400/700/800
- Stamps, captions, handwritten asides: **Caveat**, weight 700

**Motifs (use sparingly, one rule each)**
- Tape corners: only on photo/strip objects, ~25° rotation, low-opacity rectangles at two opposing corners
- Sparkle glyph: only for celebration/confirmation moments (saved, strip ready)

## Screens / Views

### 1. Booth (landing)
- Cream dotted background, app wordmark + sparkle mark top-left
- Hand-written "pocket photobooth" tagline (Caveat, rotated -2°)
- Headline "four shots, one strip, zero booth." (Fredoka 700, 36px)
- Rotated 4-cell strip preview graphic with tape corners and a sparkle accent
- Primary button "start a strip" — pill, hot pink `#FF4FA0`, white text, 5px solid drop shadow `#C22A79` (pressed-button style, not blur)
- Helper text: "no accounts · nothing leaves your phone"
- Bottom tab bar: booth / shoot / strips (count badge)

### 2. Capture
- Dark purple gradient background
- Top bar: "exit" pill button, status pill (e.g. "shot 2 of 4")
- Viewfinder card (rounded 20px): idle hint text → 3-2-1 countdown (Fredoka 700, 130px, bounce-in keyframe) → white flash (opacity pulse) → review state showing the shot with a "frame X of 4" badge
- 4-cell thumbnail strip below viewfinder, filled cells show shot number
- States: idle (manual "shoot 4 pics" button) → counting/flash (disabled "shooting…" state) → review (retake / keep it, side-by-side pill buttons, keep it wider + primary)
- One shot at a time — never a blind 4-shot burst; user reviews and confirms each

### 3. Customize
- Cream background, "your strip" handwritten label + "make it yours" headline
- Rotated strip/grid mount in the selected frame color, tape corners, per-cell retake button (circular, top-right of each cell)
- Footer row inside the mount: app name + date, colored to match frame contrast
- **Flicket** row: horizontal scroll of filter chips (no filter, disposable, sunkissed, cyber, dreamy) — active chip filled hot pink
- **Frame** row: horizontal scroll of circular color swatches (butter/bubblegum/grape/spearmint), active swatch gets dark ring
- **Layout** row: segmented control, strip vs. grid (2×2)
- Footer actions: "reshoot" (outline) + "continue" (primary, wider)

### 4. Share
- Dark gradient background matching capture
- Back-to-edit link, sparkle + "strip's ready!" headline, subcopy "save it, send it, stick it on something."
- Rotated strip mount (frame color, tape corners) — rotation driven by the chaos tweak
- Actions: "save to photos" (white pill, primary) + "share" (outline pill)
- Secondary link: "make another strip"
- Toast on save/share: sparkle + confirmation copy, auto-dismiss ~1.8s

### 5. Strips (gallery)
- Cream background, "on this device" label + "your strips" headline
- Empty state: dashed card, "no strips yet" + CTA into capture
- Filled state: 2-column grid of saved strip cards, each rotated slightly (chaos-driven), frame-colored mount, date stamp (Caveat) + share icon button per card

## Interactions & Behavior
- **Chaos tweak** (0–100%): drives rotation amount on the booth strip graphic, share-screen strip, and gallery cards — higher = more scrapbook-tilt chaos, lower = tidier/gallery-like.
- Countdown: 3 → 2 → 1 on ~650ms steps, then flash (~160ms), then review.
- Per-shot flow is sequential across all 4 frames; keep advances the queue automatically, retake redoes the same frame.
- Customize screen's per-cell retake jumps back into capture for just that one frame.
- Bottom tab bar (booth/shoot/strips) persists across booth, customize, and gallery screens; capture and share are modal-like full-screen states reached via primary actions.

## State Management
- Current screen: `booth | capture | customize | share | gallery`
- Capture sub-phase: `idle | counting | flash | review`
- 4-slot shots array (each slot: filled/empty + its image data)
- Selected flicket id, frame color id, layout (`strip`/`grid`)
- Saved strips list (persisted on-device; each entry carries its own shots + flicket + frame + layout + date)
- Transient toast message with auto-dismiss timer

## Assets
No external image assets — photo cells are placeholder gradients standing in for camera captures; all icons are inline SVG (sparkle, tab icons, share icon, retake/undo icon). Real camera frames replace the gradient placeholders in production.

## Files
- `Photobooth Rebrand.dc.html` — full interactive prototype (naming concepts, color/type guide, live 5-screen flow), source of truth for this handoff
- `android-frame.jsx` — Android device bezel used to present the flow
