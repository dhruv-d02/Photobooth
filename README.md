# Photobooth

A free, offline photobooth app for Android and iOS. Countdown, per-frame capture with a proof-and-retake step, film treatments, and a classic photo strip (or 2×2 grid) you can save and share — the pocket version of a physical photobooth kiosk.

## Status

📋 **Planning and design complete — implementation not yet started.** See [`architecture.md`](./architecture.md) for the full technical design and [`design/handoff/README.md`](./design/handoff/README.md) for the final visual spec.

## What it does (v1)

- Live camera preview with front/back switch and flash toggle
- 3-2-1 countdown, one exposure at a time — each frame gets a proof overlay you can accept or reshoot before the next one fires, so a strip never ends up with a bad frame
- Configurable shot count (2-8 exposures, default 4)
- Film treatments (None, B&W, Sepia, Warm, Steel duotone) and frame colors (Paper, Steel, Ink, Sky) applied after capture
- Vertical strip or 2×2 grid layout, chosen at export time
- Save to your device gallery, or share directly via the native share sheet
- An uncapped local history grid of past strips, with delete

No accounts, no cloud, nothing leaves your device.

## Design

Visual design is final and pixel-faithful — an "Industry" blueprint aesthetic: square corners, hairline borders, "+" registration marks, Barlow/Barlow Condensed type, one steel-blue accent. See [`design/handoff/README.md`](./design/handoff/README.md) for the complete spec.

## Tech stack

Kotlin Multiplatform + Compose Multiplatform, sharing UI and core image logic (film treatments, strip/grid compositing) across both platforms via Skia. See [`architecture.md`](./architecture.md) for the full breakdown of libraries and why each was chosen, including diagrams of the architecture, data flow, and navigation.

## Repo guide

- [`architecture.md`](./architecture.md) — architecture diagrams, data flow, library rationale, domain model, strip-composition formulas
- [`design/handoff/README.md`](./design/handoff/README.md) — the full visual design spec
- [`CLAUDE.md`](./CLAUDE.md) — working conventions and current build status for contributors (human or AI)

## Roadmap

Not yet planned for v1, tracked as future work: live face-tracking AR filters, cloud sync, shared multi-user event galleries, printing, boomerang/video capture, monetization.