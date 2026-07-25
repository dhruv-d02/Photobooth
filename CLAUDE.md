# CLAUDE.md

Guidance for Claude Code (or any future contributor) working in this repository.

## Project status

**Planning complete, no application code written yet.** This repo currently contains only documentation (`README.md`, `architecture.md`, this file). The Kotlin Multiplatform project has not been scaffolded. Do not assume `composeApp/` or `iosApp/` exist until Phase 0 below is actually done — check the filesystem before referencing a path from this doc or `architecture.md`.

## What this project is

Photobooth: a free, offline, single-user photobooth app for Android + iOS. Countdown-driven burst capture → post-capture filter/frame selection → composited photo strip → save/share. No accounts, no cloud, no backend for v1. Full product scope is in `architecture.md` and the working plan.

## Tech stack (decided)

Kotlin Multiplatform + Compose Multiplatform, sharing UI and domain logic (filter engine, strip compositor, state machines) across Android and iOS via Skia/Skiko. Platform-specific code (camera capture, media save, share sheet, permissions) is isolated behind `expect/actual` interfaces — see `architecture.md` for the full diagram and the library-by-library rationale table. Do not introduce a different cross-platform framework (Flutter, React Native) or swap a chosen library (e.g. Room → SQLDelight, Koin → Hilt) without discussing it first — those were deliberate decisions with tradeoffs already weighed.

## Known blockers

- **iOS builds require a Mac.** Not currently available. Android is the lead platform — build and validate features there first; iOS bring-up (Phase 4) waits until Mac or cloud-CI (Codemagic) access exists. Don't scope work assuming iOS can be tested locally.

## Working conventions

- Default new logic to `commonMain`. Only drop into `androidMain`/`iosMain` when the API genuinely doesn't exist cross-platform (camera, media store/photos, share sheet, permission dialogs).
- Filter and compositing logic must stay Skia-based and shared — this is the architectural reason Compose Multiplatform was chosen over Flutter/React Native. Don't add per-platform native image processing as a shortcut.
- Keep the domain model small (see `architecture.md` § Core domain model) — no sync/account fields until cloud sync is an actual v2 target, not preemptively.
- Follow the phased build sequence in `architecture.md` / the plan — don't jump ahead to filters/compositing before capture (Phase 1) works end-to-end on a real Android device.

## Build sequence (for orientation)

1. Phase 0 — scaffold KMP + Compose Multiplatform project, empty screen running on Android.
2. Phase 1 — CameraX preview, permissions, countdown state machine, burst capture.
3. Phase 2 — shared filter engine + strip compositor.
4. Phase 3 — export (MediaStore save, share intent) + local history (Room).
5. Phase 4 — iOS bring-up (blocked, see above).
6. Phase 5 — branding, store prep.

## Where to look

- `README.md` — project pitch, feature list, status.
- `architecture.md` — architecture diagrams, data flow, navigation graph, library rationale, domain model, non-functional considerations.
- The maintainer's plan file (outside this repo, in their Claude Code plans directory) has the original decision-making context if deeper history is needed.