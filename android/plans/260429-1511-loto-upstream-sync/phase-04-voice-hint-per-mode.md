# Phase 04 — Per-mode voice-master hint copy

## Context

Upstream commit `ee71bf0` (2026-04-27, after port baseline). The single
hint string was wrong in master-only mode (no player board → no
Chờ/Kinh). Web split into two:

- mode `both`   → "Đọc số đã xổ + báo Chờ/Kinh."
- mode `master` → "Đọc số đã xổ."

Android currently shows no hint at all under the master-voice toggle.

## Priority

P2 — copy correctness.

## Status

pending.

## Key insights

- The hint should only render when `mode != PLAYER` (matches web's outer
  `{#if settings.mode !== "player"}`). Android `VoiceToggles` doesn't
  currently gate the master switch by mode — confirm the row is intended
  to be visible in master + both, hidden in player. Web hides it in
  player. Port that gating too.

## Related code files

- `app/src/main/res/values/strings.xml`
- `app/src/main/java/com/miti99/loto/ui/settings/VoiceToggles.kt`
- `app/src/main/java/com/miti99/loto/ui/settings/SettingsSheet.kt`
  (verify nothing else duplicates the hint)

## Implementation steps

1. **strings.xml — add hints + mode-gated flag**

   ```xml
   <!-- Voice-master hints, per mode (phase 04) -->
   <string name="voice_master_hint_both">Đọc số đã xổ + báo Chờ/Kinh.</string>
   <string name="voice_master_hint_master_only">Đọc số đã xổ.</string>
   ```

2. **VoiceToggles.kt — gate + hint**

   ```kotlin
   @Composable
   internal fun VoiceToggles(
       state: SettingsState,
       onSetVoiceEnabledMaster: (Boolean) -> Unit,
       onSetVoiceEnabledPlayer: (Boolean) -> Unit,
       onSetVoiceWaitingNumber: (Boolean) -> Unit,
       modifier: Modifier = Modifier,
   ) {
       Column(modifier = modifier) {
           if (state.mode != SettingsState.Mode.PLAYER) {
               SwitchRow(
                   label = stringResource(R.string.voice_enabled_master),
                   checked = state.voiceEnabledMaster,
                   onCheckedChange = onSetVoiceEnabledMaster,
               )
               val hintRes = if (state.mode == SettingsState.Mode.BOTH)
                   R.string.voice_master_hint_both
               else
                   R.string.voice_master_hint_master_only
               Text(
                   text = stringResource(hintRes),
                   style = MaterialTheme.typography.bodySmall,
                   color = MaterialTheme.colorScheme.onSurfaceVariant,
                   modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 8.dp),
               )
           }

           if (state.mode != SettingsState.Mode.MASTER) {
               SwitchRow(
                   label = stringResource(R.string.voice_enabled_player),
                   checked = state.voiceEnabledPlayer,
                   onCheckedChange = onSetVoiceEnabledPlayer,
               )
               AnimatedVisibility(visible = state.voiceEnabledPlayer) {
                   SwitchRow(
                       label = stringResource(R.string.voice_waiting_number),
                       checked = state.voiceWaitingNumber,
                       onCheckedChange = onSetVoiceWaitingNumber,
                   )
               }
           }
       }
   }
   ```

   Note the player toggle gating mirrors web (hide in master-only mode).
   Confirm with `MasterPanel.svelte` / `PlayerBoard.svelte` rendering
   logic — web hides the player row in `mode === "master"`.

## Todo

- [ ] Add 2 strings
- [ ] Gate master row by mode
- [ ] Render per-mode hint under master row
- [ ] Gate player rows by mode (hide in master-only)

## Success criteria

- Mode `player` → only the player switch + (conditional) "đọc số khi Chờ"
  visible. No master switch.
- Mode `master` → only the master switch + correct hint visible.
- Mode `both` → both switches visible; hint shows the "+ báo Chờ/Kinh."
  variant.
- Hint typography matches the rest of the settings sheet ("body-small,
  on-surface-variant" — the existing convention).

## Risks

- If any test asserts `voice_enabled_master` is always present, it will
  fail in mode=player. Update phase-06 test plan accordingly.

## Next

Phase 05 (optional) or Phase 06 directly.
