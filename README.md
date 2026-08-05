# Photobooth

A free, offline photobooth app for Android and iOS. Countdown, per-frame capture with a proof-and-retake step, film treatments, and a classic photo strip (or 2×2 grid) you can save and share — the pocket version of a physical photobooth kiosk.

## Status

🎨 **Phases 0–3 built, and through a full visual rebrand.** Core functionality — KMP scaffold, CameraX capture loop, film treatment engine + strip/grid compositor, export/history + navigation — is implemented and running on Android. The most recent milestone is a complete re-skin from the original "Industry" blueprint look to **Boothie**, a Y2K/scrapbook aesthetic (the decided app name — see below). Phase 4 (iOS) is blocked on Mac/cloud-CI access. See [`architecture.md`](./architecture.md) for the full technical design and [`design/handoff/README.md`](./design/handoff/README.md) for the final visual spec.

## What it does (v1)

- Live camera preview with front/back switch and flash toggle
- 3-2-1 countdown, one exposure at a time — each frame gets a proof overlay you can accept or reshoot before the next one fires, so a strip never ends up with a bad frame
- Configurable shot count (2-8 exposures, default 4)
- Film treatments (None, Disposable, Sunkissed, Cyber, Dreamy, Black & White) and frame colors (Butter, Bubblegum, Grape, Spearmint) applied after capture
- Vertical strip or 2×2 grid layout, chosen at export time
- Save to your device gallery, or share directly via the native share sheet
- An uncapped local history grid of past strips, with delete
- A single AdMob banner ad on the Strips (history) tab

No accounts, no cloud — capture, editing, and your saved strips never leave your device. The one
exception is the banner ad on the Strips tab, which involves a network call and third-party
(Google) data sharing per Google's AdMob SDK; see [`architecture.md`](./architecture.md#adverts-admob)
for what that entails and a link to the privacy policy.

## Design

Visual design is final and pixel-faithful — **Boothie**, a Y2K/scrapbook aesthetic: cream ground, hot-pink primary, purple/gold/mint accents, rounded pill shapes, tape-corner and sparkle motifs, Fredoka/Nunito/Caveat type. See [`design/handoff/README.md`](./design/handoff/README.md) for the complete spec.

## Tech stack

Kotlin Multiplatform + Compose Multiplatform, sharing UI and core image logic (film treatments, strip/grid compositing) across both platforms via Skia. See [`architecture.md`](./architecture.md) for the full breakdown of libraries and why each was chosen, including diagrams of the architecture, data flow, and navigation.

## Repo guide

- [`architecture.md`](./architecture.md) — architecture diagrams, data flow, library rationale, domain model, strip-composition formulas
- [`design/handoff/README.md`](./design/handoff/README.md) — the full visual design spec
- [`CLAUDE.md`](./CLAUDE.md) — working conventions and current build status for contributors (human or AI)

## Roadmap

Not yet planned for v1, tracked as future work: live face-tracking AR filters, cloud sync, shared multi-user event galleries, printing, boomerang/video capture. Monetization (a single AdMob banner) shipped ahead of the rest of this list — see [`architecture.md`](./architecture.md#adverts-admob).