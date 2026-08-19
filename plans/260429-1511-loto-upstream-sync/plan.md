# Loto upstream sync — 2026-04-29

Pull 6 logic/UX changes from `tiennm99/loto` (web) into the Android port.
Behavior-faithful where the platform allows; idiomatic Compose where it
matters.

## Source

- Repo: `tiennm99/loto` @ `2fb35f2` (2026-04-29 14:53 +0700)
- Port baseline: Android `fe52232` (covers upstream through `ad6291e`)
- Drift commits considered: 15 — see recon report

## Mode

`--port` (default). No transplant of Svelte/CSS; reproduce the visual and
behavioral outcomes in Compose.

## Phases

| #  | Phase                              | Status  | Risk | Files (delta) |
|----|------------------------------------|---------|------|---------------|
| 01 | Confetti & celebration tier rule   | pending | low  | 2 |
| 02 | Section "Chờ" ring on label band   | pending | low  | 3 |
| 03 | Solid-color palette refactor       | pending | low  | 8 |
| 04 | Per-mode voice-master hint copy    | pending | low  | 3 |
| 05 | (Optional) Extract auto-tick helper| pending | low  | 3 |
| 06 | Tests + verification               | pending | low  | 2 |

Phase ordering: 01 → 02 → 03 → 04 → (05) → 06. Phases 01-04 are largely
independent; 06 lands tests for all of them at once.

## Out of scope (intentionally skipped)

- Settings modal sticky title/footer — `ModalBottomSheet` is the native
  equivalent; problem already solved.
- Mode picker "Cả hai" glyph — Android picker is text-only segments.
- PWA cache eviction, CSP, npm vuln overrides — web-only.

## Reports

- Recon: `reports/xia-recon-260429-1511.md`

## Rollback

Each phase ships its own commit. Revert order = 06 → 05 → 04 → 03 → 02 → 01.
No data-shape changes, no DataStore migration, no audio-asset changes —
revert is `git revert <sha>` per phase.

## Decisions

- New constant `BrandSky*` colors added (replacing `BrandPink*` in master
  ≤49 path). Old `BrandPink*` and `BrandIndigo*` are removed once unused.
- Wordmark gradient kept — it's the brand's only intentional gradient.
- Section Chờ ring: solid amber band + 1.5 s pulse via Compose `infiniteTransition`.
  Skip animation if `LocalAccessibilityManager.areAnimationsEnabled() == false`.

## Hand-off

Once approved, run: `/ck:cook plans/260429-1511-loto-upstream-sync/plan.md`
