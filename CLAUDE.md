# CLAUDE.md

Guidance for Claude Code (or any future contributor) working in this repository.

## Project status

**Planning and design complete, no application code written yet.** This repo currently contains documentation (`README.md`, `architecture.md`, this file) and a design handoff (`design/handoff/`). The Kotlin Multiplatform project has not been scaffolded. Do not assume `composeApp/` or `iosApp/` exist until Phase 0 below is actually done — check the filesystem before referencing a path from this doc or `architecture.md`.

## What this project is

Photobooth: a free, offline, single-user photobooth app for Android + iOS. Countdown capture is a **per-frame loop** — countdown, capture one exposure, show a proof overlay, accept or reshoot, repeat — not a plain burst. Accepted frames get a film treatment + frame color + layout (strip or 2×2 grid) applied and composited into one image, then saved/shared. No accounts, no cloud, no backend for v1. Full product scope is in `architecture.md`; the original decision log is the maintainer's plan file.

## Design

Visual design is **final and pixel-faithful** — see [`design/handoff/README.md`](./design/handoff/README.md) for the complete spec (colors, type, spacing, motion timings, exact strip-composition math). The "Industry" blueprint aesthetic (square corners, hairline borders, "+" registration marks, Barlow/Barlow Condensed type, one steel-blue accent) applies to every screen — build the shared design-token/theme module and the `CornerTicks` motif component **before** any screen work, not alongside it. `design/handoff/*.dc.html` are prototype references only, not code to port; do not copy HTML/JS patterns, recreate the described behavior natively.

## Tech stack (decided)

Kotlin Multiplatform + Compose Multiplatform, sharing UI and domain logic (film treatment engine, strip compositor, session state machine) across Android and iOS via Skia/Skiko. Platform-specific code (camera capture, media save, share sheet, permissions) is isolated behind `expect/actual` interfaces — see `architecture.md` for the full diagram and the library-by-library rationale table. Do not introduce a different cross-platform framework (Flutter, React Native) or swap a chosen library (e.g. Room → SQLDelight, Koin → Hilt) without discussing it first — those were deliberate decisions with tradeoffs already weighed.

## Known blockers

- **iOS builds require a Mac.** Not currently available. Android is the lead platform — build and validate features there first; iOS bring-up (Phase 4) waits until Mac or cloud-CI (Codemagic) access exists. Don't scope work assuming iOS can be tested locally.

## Git workflow

Always checkout a new branch scoped to the feature/area being worked on before committing — e.g. `feature/ui`, `feature/camerax-capture`, `feature/filter-engine`, `chore/project-setup`. Never commit directly to `main`. Leave branches for the maintainer to review/merge rather than merging or pushing to `main` automatically.

## Working conventions

- Default new logic to `commonMain`. Only drop into `androidMain`/`iosMain` when the API genuinely doesn't exist cross-platform (camera, media store/photos, share sheet, permission dialogs).
- Film-treatment and compositing logic must stay Skia-based and shared — this is the architectural reason Compose Multiplatform was chosen over Flutter/React Native. Don't add per-platform native image processing as a shortcut.
- Keep the domain model small (see `architecture.md` § Core domain model) — no sync/account fields until cloud sync is an actual v2 target, not preemptively.
- Follow the phased build sequence below — don't jump ahead to film treatments/compositing before capture (Phase 1) works end-to-end on a real Android device.
- Reshoot always re-targets the same frame slot index (never appends/shifts) — this is a deliberate invariant so the strip can never end up with a gap. Preserve it in both the main capture loop and the per-cell retake path.
- Gallery/history has no cap — don't add an eviction limit; deletion is manual only.

## Build sequence (for orientation)

1. Phase 0 — scaffold KMP + Compose Multiplatform project; build the design-token/theme module and `CornerTicks` motif component; empty Compose screen running on Android.
2. Phase 1 — CameraX preview, permissions, session state machine (per-frame countdown → capture → proof → accept/reshoot loop), configurable shot count setting (2-8, default 4).
3. Phase 2 — shared film treatment engine (5 fixed presets) + frame-color presets + strip/grid compositor, per the exact formulas in `architecture.md`.
4. Phase 3 — export (MediaStore save, share intent) + local history (Room, uncapped).
5. Phase 4 — iOS bring-up (blocked, see above).
6. Phase 5 — branding (app name still undecided), icons, store prep.

## Automated review

Every pull request is automatically reviewed by Claude (`.github/workflows/claude-code-review.yml`, using `anthropics/claude-code-action`) against this file and `architecture.md`. It requires an `ANTHROPIC_API_KEY` repository secret to run — set up once via GitHub repo Settings → Secrets and variables → Actions, not committed anywhere.

## Where to look

- `README.md` — project pitch, feature list, status.
- `architecture.md` — architecture diagrams, data flow, navigation graph, library rationale, domain model, strip-composition formulas, design system, non-functional considerations.
- `design/handoff/README.md` — the full visual design spec (final, pixel-faithful).
- The maintainer's plan file (outside this repo, in their Claude Code plans directory) has the original decision-making context if deeper history is needed.