# Phase 1 — Tier 1 defects

APK-only bugs. None reproduce in a desktop browser.

## W1 · VIBRATE permission

`android/android/app/src/main/AndroidManifest.xml` — add:

```xml
<uses-permission android:name="android.permission.VIBRATE" />
```

Normal permission: no runtime prompt, no Play data-safety change. Caller is
`PlayerBoard.svelte:341-346`, already guarded by `prefers-reduced-motion`.

## W2 · Screen wake lock

New `web/src/lib/wake-lock.js` (plain `.js` — no reactive state, matches
`game-logic.js` / `player-auto-cross.js` convention; runes files are only those
holding `$state`).

API — single entry point so callers cannot desync:

```js
export function setWakeLock(on)   // idempotent
export function _resetForTest()   // test-only teardown
```

Behaviour:

- `setWakeLock(true)` → `navigator.wakeLock.request("screen")`, installs a
  `visibilitychange` listener that re-acquires on return to visible.
  Android silently releases the lock whenever the page hides — without the
  re-acquire the lock is dead after the first backgrounding.
- `setWakeLock(false)` → releases sentinel, removes listener.
- Generation token guards the async gap: a `request()` that resolves after the
  caller flipped to `false` must release immediately rather than leak a lock.
  Same pattern as `activeToken` in `voice.js`.
- No `navigator.wakeLock` → no-op (old WebViews at minSdk 24).
- `request()` rejects on battery saver / hidden page → swallow, do not throw.

Consumer — `web/src/lib/MasterPanel.svelte`, new `$effect`:

```js
$effect(() => {
  setWakeLock(autoRunning || hasGame);
  return () => setWakeLock(false);
});
```

`autoRunning` (line 47) and `hasGame` (lines 55-57) already exist locally.
MasterPanel only mounts in master/both mode, so player-only mode never holds a
lock — intended: player taps keep their own screen awake.

Tests — new `web/src/lib/wake-lock.test.js`: acquire, release, re-acquire on
`visibilitychange`, no-op when unsupported, late-resolve after `false` releases.

## W3 · Back button

### Web half — `web/src/lib/overlay-history.js` (new)

Each open overlay pushes one sentinel history entry. `popstate` closes the
newest. Works in the browser too — browser back closes the modal, a real PWA win.

```js
export function pushOverlay(close)  // returns dispose() for programmatic close
```

- LIFO stack of `{ id, close }`.
- `popstate` → pop top, run its `close()`, leave history alone.
- `dispose()` (Escape / Xong / backdrop) → remove entry, then `history.back()`
  to drop the sentinel, with a suppression counter so the resulting `popstate`
  does not re-run `close()`.
- Single shared `popstate` listener, installed on first push, removed when the
  stack empties.

Consumers:

- `PlayerBoard.svelte` — `showCongrats` modal.
- `SettingsButton.svelte` — settings sheet.

Both already have Escape handlers in an `$effect`; the overlay push belongs in
the same effect so open/close stays one code path.

Tests — new `web/src/lib/overlay-history.test.js`: single open/close, nested
LIFO ordering, programmatic close does not double-fire, stack drains.

### Native half — `MainActivity.java`

```java
getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
  @Override public void handleOnBackPressed() {
    WebView wv = getBridge().getWebView();
    if (wv != null && wv.canGoBack()) { wv.goBack(); return; }   // pops a sentinel
    confirmExit();
  }
});
```

`history.pushState` adds to the WebView back-forward list, so `canGoBack()` is
true exactly while an overlay is open, and `goBack()` fires `popstate`.

Exit confirm: `androidx.appcompat.app.AlertDialog`, strings from
`strings.xml` (`exit_title`, `exit_message`, `exit_confirm`, `exit_cancel`),
positive button → `finish()`.

Also add `android:enableOnBackInvokedCallback="true"` to `<application>`.
Default at targetSdk 36; explicit is self-documenting.

**Unconfirmed, device-only:** whether the activity theme at dialog time is
AppCompat-derived. Activity theme is `AppTheme.NoActionBarLaunch`
(parent `Theme.SplashScreen`); Capacitor's splash flow normally swaps to
`AppTheme.NoActionBar` post-splash. If the dialog throws on a non-AppCompat
theme, pass an explicit dialog theme to the builder. Flagged in QA checklist.

## Files

| File | Change |
|------|--------|
| `android/android/app/src/main/AndroidManifest.xml` | VIBRATE, enableOnBackInvokedCallback |
| `android/android/app/src/main/java/com/miti99/loto/MainActivity.java` | back callback + confirm dialog |
| `android/android/app/src/main/res/values/strings.xml` | 4 exit-dialog strings |
| `web/src/lib/wake-lock.js` | new |
| `web/src/lib/wake-lock.test.js` | new |
| `web/src/lib/overlay-history.js` | new |
| `web/src/lib/overlay-history.test.js` | new |
| `web/src/lib/MasterPanel.svelte` | wake-lock effect |
| `web/src/lib/PlayerBoard.svelte` | overlay push for bingo modal |
| `web/src/lib/SettingsButton.svelte` | overlay push for settings sheet |

## Validation

- `pnpm test` — new suites green, existing 6 suites unaffected.
- `pnpm lint`, `pnpm build`.
- Device QA: tap buzzes; reduced-motion silent; 3-min auto-call, screen stays
  lit; background/return keeps it lit; back closes modal; back at root confirms;
  confirm dialog renders (theme check).
