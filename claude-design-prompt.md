I'm redesigning a mobile photobooth app and need a complete new visual identity across every
screen. I'll give you the product, the audience, the current problems, and the exact output
structure I need back.

## What the app does
A free, offline, single-user photobooth app (Android first, iOS later — Kotlin Compose
Multiplatform, portrait-locked). The core loop: user starts a session, the front camera fires
N times (default 4) on a countdown, **each exposure is reviewed one at a time** — accept or
reshoot before the next one fires, not a plain burst. Accepted frames get a film treatment,
a frame colour, and a layout (vertical strip or 2x2 grid) applied, get composited into one
image, and get saved + shared. Saved strips live in an on-device gallery the user can reopen,
share, or delete. No accounts, no cloud.

## Audience and direction
I want a creative, distinctive aesthetic that pulls in teenagers and young women specifically —
think the energy of Dispo, retro mall photobooth strips, VSCO, Y2K/scrapbook digital
ephemera — fun, a little nostalgic, shareable, NOT corporate or clinical. Propose a real point
of view (palette, type pairing, a signature motif) rather than a generic "clean modern app"
look. Lead with that creative direction as a short mood statement before you get into screens,
so I can react to the direction itself first.

## Current problems (fix these, don't just reskin around them)
- The landing screen today is almost entirely white/empty — thin hairlines, small grey text,
  no hero moment. It's the user's first impression and currently says nothing.
- The capture review screen has two side-by-side buttons ("SHOOT AGAIN" and "KEEP / NEXT")
  whose alignment/proportions currently feel mismatched and unbalanced — give these an explicit,
  deliberate layout (exact weights/heights/padding), not an afterthought.
- A "+" corner-tick motif currently gets stamped onto five unrelated things (a button, a strip
  graphic, the camera viewfinder, gallery cards, the preview figure) with no consistent rule for
  when it belongs. Either give it one deliberate role and placement rule, or drop it — don't let
  it sprinkle by convention.

## Screens to cover (keep this flow — redesign the visual system, not the structure)
1. **Booth (landing)** — explains the product in one screen, one primary action: START SESSION.
2. **Shoot (capture)** — full-bleed camera viewfinder; countdown → capture → per-frame proof
   overlay with accept/reshoot; a thumbnail row of frames filled so far; bottom tab bar hidden
   (session is modal).
3. **Strip preview & customise** — shows the composited strip; controls for layout (strip vs
   grid), film treatment (multiple presets, horizontally scrollable), frame colour (multiple
   presets, horizontally scrollable); actions RESHOOT and SAVE; a per-cell RETAKE control.
4. **Strips (gallery)** — grid of saved strips, each showing a thumbnail, date, and treatment
   used; tapping opens Strip Detail; empty state when nothing saved yet.
5. **Strip Detail** — full-screen in-app viewer for one saved strip, opened from a gallery card;
   actions to share or delete (with a confirm step — deletion is permanent, no undo).
6. **Bottom tab bar** — 3 destinations (Booth / Shoot / Strips), present on every screen except
   Shoot.

## Output format — please follow this exactly
This is going straight into a spec-verification step, so I need concrete, implementable values,
not mood-board language. For the shared design system, give me:
- A short creative-direction statement (2-4 sentences): mood, why it fits the audience.
- One shared token table covering **Color** (name/value/used for), **Spacing** (name/value/used
  for), and **Type** (role/font+size+weight/used for) — real hex codes, real dp/sp values, real
  font names, used consistently across every screen below.
- Note whether screens are uniformly light, uniformly dark, or intentionally mixed (today,
  capture and the strip detail viewer are deliberately full-bleed dark while the rest are light —
  keep that split, change it, or make it something else, but state the decision).

Then, **for each of the 6 screens above**, give me a spec with exactly this structure and section
order (skip Navigation only for the tab bar, skip Landscape only if you state the screen is
portrait-locked and why):

```
# <Screen name>
Requirement / Theme / OS / Orientation / Screen's job (one sentence)

## Layout
ASCII wireframe of the real layout with real content (not lorem ipsum), sized roughly like a
portrait phone. 2-4 bullets on layout reasoning: what's above the fold, what scrolls, what's
pinned.

## Component tree
Indented tree of the actual components with the key parameters that affect layout.

## Tokens
Only deltas from the shared token table above (colors/spacing/type this screen uses that aren't
already obvious).

## States
Table: state | trigger | UI | copy — cover loading, empty, error, content, and any
interaction-specific states (e.g. capture's countdown/flash/proof/review states, gallery's
empty-vs-populated, strip detail's not-found-because-deleted-elsewhere).

## Interaction
Taps, gestures, transitions, what each produces. One line each.

## Navigation
How the user arrives, where each exit leads, back behavior.

## Landscape / adaptive
Deltas only, or "portrait-locked" with why.

## Accessibility
Only what's specific to this screen: touch target minimums, contrast risks in the new palette,
labels for icon-only controls.
```

Finish with a short flow map showing how the 6 screens connect (arrows + trigger, one line).
