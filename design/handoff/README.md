# Handoff: Photobooth Strip App (Android/iOS)

## Overview
A mobile photobooth app. The user starts a session, the front camera fires N times (default 4)
on a 3-2-1 countdown, **each exposure is reviewed and accepted or reshot**, then the accepted
frames are laid into a photobooth strip the user can restyle (frame colour, film treatment,
vertical strip vs 2×2 grid) and save as a print-ready PNG. Saved strips are archived in an
on-device gallery. Nothing is uploaded.

## About the Design Files
The files in this bundle are **design references created in HTML** — prototypes that show the
intended look and behaviour. They are **not production code to copy**. The task is to recreate
these designs in the target codebase's existing environment (React Native, Swift/SwiftUI,
Kotlin/Compose, Flutter, web…) using its established patterns, component library and camera
APIs. If no environment exists yet, pick the most appropriate framework and implement there.

The HTML prototype uses `getUserMedia` + `<canvas>` because it runs in a browser; on native,
substitute the platform camera (CameraX / AVFoundation / expo-camera) and the platform image
compositor, keeping the same geometry and sequencing described below.

## Fidelity
**High-fidelity.** Colours, type, spacing, hit targets and copy are final. Recreate pixel-
faithfully, mapping the values in *Design Tokens* onto the codebase's own token system.

## Design language
"Industry" — a technical blueprint wireframe:
- Light paper ground, one steel-blue accent, dark steel for camera/immersive surfaces.
- **Square corners everywhere.** No rounded cards, no soft filled blocks.
- Hairline 1px borders; cards and figures are transparent line drawings, not surfaces.
- Framed objects wear four `+` registration marks at their corners (12px monospace, accent
  colour, offset ~-7px/-5px outside the box).
- Condensed uppercase headings (Barlow Condensed) over Barlow body text; monospace for
  metadata/labels (uppercase, letter-spacing .10–.16em, 9–11px).
- The solid accent primary button is the one filled object on the screen.
- Photographs are duotoned into the accent where a treatment calls for it.

## Device envelope
Designed at **412 × 892** (Android reference). Android status bar + gesture nav are part of the
frame in the prototype only — on device use real system chrome. Status bar is **dark** on the
capture screen and on landing variant 1b, light elsewhere.

Bottom tab bar (present on every screen **except capture**), 3 equal cells, each min-height 56px,
1px top border, 1px dividers between cells:
- **Booth** (glyph ▣) → landing. Active cell background `#eef6ff`, label `#2c455d`; inactive
  transparent with `#5d5d60` label.
- **Shoot** (glyph ●) → starts a session. Always solid accent `#5980a6` with `#f5f5f8` label.
- **Strips NN** (glyph ▤) → gallery, with the zero-padded strip count in the label.
Labels: Barlow Condensed 600, 11px, uppercase, letter-spacing .14em, above/below an 12px
monospace glyph, 4px gap. Replace the glyphs with Lucide icons at stroke-width 1.5 in production
(camera / aperture / layout-grid).

## Screens / Views

### 1. Landing (three alternative treatments — ship one)
Purpose: explain the product in one screen and start a session. The prototype exposes a
1a/1b/1c switcher **outside** the device frame; it is a design tool, not app UI.

**1a — Spec sheet (default recommendation).** Vertical stack on the light ground with a faint
27.2px grid (`#1d1f2008` 1px lines both axes):
- Top block, padding 27.2px 20.4px 17px, 13.6px gap, 1px bottom border:
  - Kicker, monospace 10px uppercase, letter-spacing .16em, `#416180`:
    `spec 01 — 4 exposures, one strip`
  - H1, Barlow Condensed 700, **52px**, line-height .9, letter-spacing -.01em, uppercase, three
    lines: `Four frames. / One strip. / No booth.`
  - Body, Barlow 15px/1.5, `#424244`: "Your camera fires four times on a countdown, then lays
    the exposures into a photobooth strip you can tint, filter and save to your phone."
- Middle: centred **drawn strip figure**, 150px wide, 10.2px padding, 1px `#5980a6` border on
  `#f5f5f8`, 6.8px gap, four empty 4:3 cells filled with a 45° 4px/4px hatch
  (`#dfe7f0`/`#eef6ff`) and hairline `#1d1f2029` borders; footer row above a 1px rule with the
  brand (Barlow Condensed 600, 10px, letter-spacing .14em) left and `2×6 IN` (monospace 8px,
  `#5d5d60`) right. Four `+` marks on the outer frame.
- Bottom block, 1px top border, padding 17px 20.4px 20.4px: full-width primary button
  **START SESSION** (min-height 56px, Barlow Condensed 600 18px, letter-spacing .12em, accent
  fill, four `+` marks, pressed `#416180`), then a two-line monospace 10px caption, centred,
  `#5d5d60`: "camera permission required · nothing leaves this device".

**1b — Steel field.** Full-bleed `#1d2d3d` with a 27.2px grid in `#f5f5f80a`, centred column,
24px gaps, 34px/20.4px padding:
- Instruction, monospace 10px/1.6, letter-spacing .2em, `#94bce3`, centred, two lines:
  "hold the phone up / look at the lens · hold still"
- H1, Barlow Condensed 700, **64px**, line-height .86, uppercase, `#f5f5f8`:
  `Photobooth, / minus the / booth`
- Three stacked 104px-wide 4:3 outlines, 1px `#94bce3`, opacity .9 / .6 / .3, 6.8px gap
- Full-width button **BEGIN COUNTDOWN**: paper fill `#f5f5f8`, text `#1d2d3d`, min-height 56px,
  Barlow Condensed 600 19px, letter-spacing .14em; pressed `#94bce3`
- Caption, monospace 10px uppercase, `#9ebbd8`: "4 exposures · 3 second countdown · png out"

**1c — Procedure list.** Header (padding 27.2px 20.4px 13.6px, 1px bottom border): kicker
"operating procedure" + H1 Barlow Condensed 700 42px "Make a strip in four steps". Then four
rows, each padding 17px 20.4px with a 1px bottom rule, 13.6px gap: a 34px Barlow Condensed 700
number in `#b5d9fd` (min-width 44px) beside a Barlow Condensed 600 19px uppercase title and
Barlow 14px/1.45 `#424244` body. Row 4 has a `#e9e9ea66` wash. Steps: *Allow camera* / *Four
exposures* / *Tint & filter* / *Save it*. Footer block with the full-width accent
**START SESSION** button.

### 2. Capture
Full-bleed dark screen `#1d2d3d`, no bottom tab bar (session is modal).
- **Top bar**, padding 13.6px 17px, three items: `← EXIT` outlined button (min 44×44,
  1px `#f5f5f833`, Barlow Condensed 500 12px uppercase); centre monospace 10px `#94bce3`
  status `EXPOSURE 01 / 04` (or `RETAKE · FRAME 02`); right monospace 10px `#9ebbd8`
  `○ IDLE` / `● REC`.
- **Viewfinder**: flex-1, full-bleed mirrored front-camera preview (`transform: scaleX(-1)`),
  cropped to fill. 1px `#f5f5f826` inset frame at 13.6px, four `+` marks in `#94bce3` at
  8px/10px insets, 1px top and bottom borders on the region.
  - Countdown: Barlow Condensed 700 **150px**, `#f5f5f8`, text-shadow `0 4px 24px #1d2d3d99`,
    centred, fades in/out via opacity.
  - Flash: full-cover `#f5f5f8` at opacity .92 for ~180ms, 120ms linear opacity transition.
  - Bottom-left monospace 10px `#f5f5f8cc`: `FRONT CAMERA LIVE` / `PLACEHOLDER MODE` /
    `CONNECTING…`; bottom-right `MIRRORED · 4:3` / `NO SIGNAL`.
- **Proof overlay (the review step)** — covers the viewfinder region the moment a frame is
  captured, 200ms rise animation (opacity 0→1, translateY 8px→0):
  - The just-captured frame, mirrored, filling the region.
  - Header strip across the top with a `linear-gradient(#1d2d3de6, #1d2d3d00)` scrim,
    padding 13.6px 20.4px: a solid chip `PROOF 02` (monospace 600 11px, `#1d2d3d` on `#94bce3`,
    6px 9px padding) plus monospace 10px `#f5f5f8` `FRAME 02 OF 04 · YOUR CALL`.
- **Bottom bar**, padding 13.6px 17px, 13.6px gap:
  - Thumbnail row: N equal 4:3 cells, 6.8px gap. Empty = `#f5f5f814` fill with 1px `#f5f5f833`
    border; filled = the mirrored frame with 1px `#94bce3` border and an 8px monospace index
    bottom-left.
  - **Idle state**: last log line (Barlow 11px/1.35 `#9ebbd8`) on the left, primary shutter on
    the right — `SHOOT 4` / `SHOOT 02` (retake) / `EXPOSING…` while running, min 150×56,
    paper fill on dark, pressed `#94bce3`, opacity .45 when disabled.
  - **Review state**: two buttons instead — `SHOOT AGAIN` (ghost, flex 1, 1px `#f5f5f866`
    border, `#f5f5f8` text) and `KEEP · NEXT` (flex 1.4, `#94bce3` fill, `#1d2d3d` text; label
    is just `KEEP` on the final frame). Both min-height 56px. Below them, the log line as
    centred monospace 10px `#9ebbd8`.

### 3. Strip preview & customise
Light ground, scrolling body between a fixed header and a fixed action bar.
- Header, padding 10.2px 17px, 1px bottom border: left monospace 10px `#416180`
  `PROOF · STRIP 001`; right monospace 10px `#5d5d60` `STRIP · F01` (layout + filter code).
- **Strip figure**, centred on a `#e9e9ea66` band (padding 20.4px, 1px bottom border), 260ms
  rise animation, four `+` marks: frame background = chosen frame colour, 12px padding, 8px
  gaps, width **176px** (vertical strip) or **260px** (2×2 grid). Cells are 4:3 (strip) or 1:1
  (grid), object-fit cover, mirrored, with the chosen CSS filter; steel duotone additionally
  overlays `#b5d9fd` at opacity .85 in `multiply`. Each cell carries a top-right
  `RETAKE 02` button (min-height 32px, 9px monospace, `#f5f5f8dd` on the photo, pressed
  accent) that returns to capture for that single frame.
  Footer row above a 1px rule in the frame's rule colour: brand (Barlow Condensed 600 12px,
  letter-spacing .16em) and date stamp (monospace 8px, e.g. `JUL 25, 2026`).
- **Layout** control: label (monospace 10px `#416180` `LAYOUT`) + 2-cell segmented control in a
  1px box, each cell min-height 44px, Barlow Condensed 500 13px uppercase; selected cell accent
  fill with `#f5f5f8` text. Options `VERTICAL STRIP` / `2 × 2 GRID`.
- **Film treatment**: horizontally scrolling chip row (scroll-snap), each chip min-height 44px,
  padding 9px 13.6px, 1px border, two stacked labels — name (Barlow Condensed 500 12px
  uppercase) and code (monospace 9px, opacity .6). Selected chip = accent fill, `#f5f5f8` text.
- **Frame colour**: horizontally scrolling chip row, each chip min-height 44px with a 14×14
  swatch (1px `#1d1f2033`) beside the name. Selected chip = `#eef6ff` fill, `#5980a6` border.
- **Action bar**, 1px top border, padding 13.6px 17px: `RESHOOT` (ghost, min 56×56) beside a
  flex-1 accent **SAVE PNG** (min-height 56px, Barlow Condensed 600 17px). Below, centred
  monospace 10px `#5d5d60`: "saving also archives a copy in strips" → "saved to photos ·
  archived in strips" once saved.

### 4. Gallery ("Past strips")
- Header, padding 20.4px 17px 13.6px, 1px bottom border: kicker "archive · on this device"
  (monospace 10px `#416180`) + H2 Barlow Condensed 700 36px uppercase `PAST STRIPS`.
- Body padding 17px. **Empty state**: 1px dashed `#1d1f2033` box, 34px/20.4px padding, 45°
  6px/6px hatch background (`#e9e9ea`/`#f2f2f3`), monospace 11px `NO STRIPS ON FILE` plus
  Barlow 14px "Strips you save are archived here."
- **Grid**: 2 columns, 13.6px gap. Each card is a blueprint object — 1px `#1d1f2029` border,
  10.2px padding, four `+` marks, the strip image full-width, then a row with the stamp
  (monospace 9px `#5d5d60`, e.g. `JUL 25, 2026 · F01`) and a `SAVE` link (Barlow Condensed 600
  10px, letter-spacing .12em, `#416180`, min-height 32px).

## Interactions & Behaviour

**Session sequence (the core flow)**
1. Start session → clear shots/log, navigate to capture, request camera. On success log
   "Front camera live · tap shoot when ready."; on failure/denial fall to **placeholder mode**
   and log "Camera unavailable — placeholder frames armed."
2. Shutter → build a queue of frame indices (all frames, or a single index for a retake).
3. For the head of the queue: countdown `3`, `2`, `1` at **760ms** per number → flash on,
   capture, 180ms → flash off → **proof overlay** with that frame; log "Frame 0N exposed · keep
   it?".
4. **Keep** → commit the frame into slot N, log "Frame 0N accepted.", 420ms pause, then
   countdown the next queued frame. If the queue is empty: 260ms, stop the camera, go to
   preview.
5. **Shoot again** → discard, log "Frame 0N discarded · shooting again.", 320ms pause, re-fire
   the *same* index. The strip can therefore never end up with a gap.
6. Exit at any time stops the camera, clears the queue and returns to landing.

**Retake from preview** — the per-cell `RETAKE 0N` button returns to capture with a single-index
queue; the shutter reads `SHOOT 0N`; keeping that proof returns to preview with the frame
replaced.

**Save** — compose the strip (below), trigger a download/save-to-photos, prepend the result to
the gallery (cap 12), persist, and flip the caption to the saved state. Changing any styling
option resets `saved` to false.

**Capture geometry** — capture at 1200×900 (4:3), centre-cropped from the sensor. Preview and
proof are mirrored horizontally, and the composed strip is mirrored too, so what the user saw is
what they get. In placeholder mode the fallback frame is drawn **pre-mirrored** so its label
reads correctly after the flip.

**Strip composition (PNG export)** — canvas at 2× scale for print (a vertical strip lands near
2 × 6 in at 300 dpi):
- Content width 320, padding 16, gap 10, footer 30 (design units, ×2 on output).
- 1 column (strip) → each photo 320 × 240; 2 columns (grid) → each 155 × 155.
- Height = 2·padding + rows·photoHeight + (rows−1)·gap + footer.
- Fill the frame colour, draw each photo clipped and mirrored with the film filter applied,
  overlay `#b5d9fd` multiply at .85 for steel duotone.
- Footer: 1px rule at 35% opacity, brand left (Barlow Condensed 600 13px), date right
  (monospace 9px).
- Output `image/png`; frames are held as `image/jpeg` at quality .92 between capture and export.

**Hit targets** — every interactive element is ≥44px, primary actions 56px.
**Focus** — 2px `#5980a6` outline at 2px offset (web); platform equivalents on native.
**Pressed states** — accent buttons darken to `#416180`; paper-on-dark buttons go `#94bce3`;
ghost/outlined buttons take a `#eef6ff` (light) or `#f5f5f81a` (dark) tint.

## State Management
- `screen`: `landing | capture | preview | gallery`
- `variant`: `a | b | c` (design-time landing switcher only — drop in production)
- `shots`: array of N image data URIs (sparse until accepted)
- `queue`: remaining frame indices for the current sequence
- `review`: `null` or `{ idx, src }` — the frame awaiting accept/reshoot
- `shooting`: countdown/capture in progress (shutter disabled)
- `countdown`: `'' | '3' | '2' | '1'`; `flash`: boolean
- `cam`: `idle | live | denied`
- `frame`: `paper | steel | ink | sky`; `filter`: `none | bw | sepia | warm | duo`;
  `layout`: `strip | grid`
- `retakeIdx`: single-frame retake target or null
- `gallery`: array of `{ src, stamp, file }`, newest first, capped at 12, persisted on device
  (localStorage key `pb_strips_v1` in the prototype → use the platform store / photo library)
- `saved`: whether the current strip has been exported
- `log`: session lines; only the newest is shown on screen
- Config: `frames` (2–8, default 4), `brand` (default "Fourframe")

## Design Tokens
Colours
- Ground `#f2f2f3` · Surface wash `#e9e9ea` (and `#e9e9ea66`) · Paper `#f5f5f8`
- Text `#1d1f20` · Body `#424244` · Muted `#5d5d60`
- Accent `#5980a6` · Accent pressed/deep `#416180` · Deeper `#2c455d`
- Accent tints `#b5d9fd`, `#d6ebff`, `#eef6ff`, `#dfe7f0` · On-dark accent `#94bce3`,
  dark-surface secondary text `#9ebbd8`
- Dark surface `#1d2d3d` · Ink `#1d1f20`
- Hairlines: `#1d1f2029` on light, `#f5f5f826`/`#f5f5f833` on dark
- Frame options: Paper `#f5f5f8` (text `#1d1f20`, dim `#5d5d60`), Steel `#2c455d`
  (`#f5f5f8`/`#b5d9fd`), Ink `#1d1f20` (`#f5f5f8`/`#98989b`), Sky `#d6ebff`
  (`#1d2d3d`/`#416180`)

Film treatments (CSS filter equivalents)
- `F00` None — none
- `F01` Black & white — `grayscale(1) contrast(1.12)`
- `F02` Sepia — `sepia(.72) contrast(1.05) saturate(1.1)`
- `F03` Warm — `saturate(1.25) contrast(1.05) brightness(1.05) hue-rotate(-8deg)`
- `F04` Steel duotone — `grayscale(1) contrast(1.06) brightness(1.06)` + `#b5d9fd` multiply .85

Spacing (0.85× density scale, use these literal steps): 3.4 · 5 · 6.8 · 8 · 10.2 · 13.6 · 17 ·
20.4 · 24 · 27.2 · 34 · 44 · 54.4 px. Grid overlay pitch 27.2px.

Type
- Headings: Barlow Condensed 600/700 — 64 / 52 / 42 / 36 / 19 / 18 / 13 / 12 / 11 / 10px,
  uppercase, letter-spacing .04–.16em (negative −.01em on the big display sizes)
- Body: Barlow 300–500 — 15 / 14 / 11px, line-height 1.35–1.5
- Metadata: ui-monospace/Menlo 8–11px, uppercase, letter-spacing .06–.20em

Radius **0** everywhere. Shadows: strip figure `0 10px 28px #2b2b2d26`; gallery/deck cards rely
on borders, not elevation. Countdown shadow `0 4px 24px #1d2d3d99`.

Motion: countdown step 760ms · flash 180ms (120ms linear fade) · inter-frame pause 620ms ·
accept→next 420ms · reshoot→next 320ms · proof rise 200ms · strip rise 260ms ease-out.

## Assets
No image assets. Placeholder frames are generated on canvas (a `#dfe7f0`→`#eef6ff` gradient with
46px 45° `#b5d9fd` strokes and a centred `FRAME 0N · NO CAMERA` label). Fonts: Barlow and Barlow
Condensed (Google Fonts) — substitute the codebase's own families if it already pairs a
condensed display face with a text face. Icons: Lucide at stroke-width 1.5 (the prototype uses
monospace glyph stand-ins in the tab bar).

## Files
- `Photobooth Strip.dc.html` — the mobile design (all four screens, all three landing variants,
  real camera, review step, strip composition, gallery). **This is the primary reference.**
- `Photobooth Strip desktop.dc.html` — the earlier desktop/kiosk layout of the same flow, kept
  for reference only.
- `android-frame.jsx` — the Android device bezel/status bar used to present the design; not part
  of the app.
- `support.js` — prototype runtime. Not part of the app.
