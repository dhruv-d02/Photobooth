# Photobooth

A free, offline photobooth app for Android and iOS. Countdown, burst capture, filters, and a classic photo strip you can save and share — the pocket version of a physical photobooth kiosk.

## Status

📋 **Planning complete — implementation not yet started.** See [`architecture.md`](./architecture.md) for the full technical design.

## What it does (v1)

- Live camera preview with front/back switch and flash toggle
- 3-2-1 countdown into an automatic burst of 3-4 shots
- Post-capture filter selection (a handful of color filters) and preset frames
- Compositing the burst into a single classic photo strip
- Save to your device gallery, or share directly via the native share sheet
- A local history grid of past strips, with delete

No accounts, no cloud, nothing leaves your device.

## Tech stack

Kotlin Multiplatform + Compose Multiplatform, sharing UI and core image logic (filters, strip compositing) across both platforms via Skia. See [`architecture.md`](./architecture.md) for the full breakdown of libraries and why each was chosen, including diagrams of the architecture, data flow, and navigation.

## Repo guide

- [`architecture.md`](./architecture.md) — architecture diagrams, data flow, library rationale, domain model
- [`CLAUDE.md`](./CLAUDE.md) — working conventions and current build status for contributors (human or AI)

## Roadmap

Not yet planned for v1, tracked as future work: live face-tracking AR filters, cloud sync, shared multi-user event galleries, printing, boomerang/video capture, monetization.