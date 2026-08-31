/**
 * Svelte action for a modal `role="dialog" aria-modal="true"` container:
 * moves focus into the dialog on mount, traps Tab/Shift+Tab among its
 * focusable descendants, restores focus to whatever was focused before
 * the dialog opened, and locks body scroll for as long as any trap is
 * active (reference-counted so two stacked dialogs can't fight over the
 * lock or restore the wrong `overflow` value).
 *
 * Usage: `<div role="dialog" aria-modal="true" use:focusTrap>…</div>`.
 * Escape-to-close is NOT handled here — callers already wire their own
 * window-level Escape listener (see PlayerBoard/SettingsButton) so the
 * Android back-gesture sentinel keeps working unchanged.
 *
 * @module lib/focus-trap
 */

const FOCUSABLE_SELECTOR =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

let lockCount = 0;
let previousBodyOverflow = "";

function lockBodyScroll() {
  if (lockCount === 0) {
    previousBodyOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
  }
  lockCount++;
}

function unlockBodyScroll() {
  lockCount = Math.max(0, lockCount - 1);
  if (lockCount === 0) document.body.style.overflow = previousBodyOverflow;
}

/**
 * @param {HTMLElement} container
 * @returns {HTMLElement[]}
 */
function focusables(container) {
  return /** @type {HTMLElement[]} */ (
    Array.from(container.querySelectorAll(FOCUSABLE_SELECTOR)).filter(
      (el) => el instanceof HTMLElement && el.offsetParent !== null,
    )
  );
}

/**
 * @param {HTMLElement} node
 * @returns {{ destroy(): void }}
 */
export function focusTrap(node) {
  const previouslyFocused =
    document.activeElement instanceof HTMLElement
      ? document.activeElement
      : null;

  if (!node.hasAttribute("tabindex")) node.setAttribute("tabindex", "-1");
  // Focus the container itself rather than a descendant — predictable
  // regardless of dialog layout, and screen readers still announce
  // `aria-labelledby`/`aria-label` on focus.
  node.focus();

  lockBodyScroll();

  /** @param {KeyboardEvent} e */
  function onKeydown(e) {
    if (e.key !== "Tab") return;
    const items = focusables(node);
    if (items.length === 0) {
      // Nothing tabbable inside — keep focus pinned on the dialog so Tab
      // can't escape to the (visually covered but still-in-DOM) page.
      e.preventDefault();
      node.focus();
      return;
    }
    const first = items[0];
    const last = items[items.length - 1];
    const active = document.activeElement;
    if (e.shiftKey && (active === first || !node.contains(active))) {
      e.preventDefault();
      last.focus();
    } else if (!e.shiftKey && (active === last || !node.contains(active))) {
      e.preventDefault();
      first.focus();
    }
  }
  node.addEventListener("keydown", onKeydown);

  return {
    destroy() {
      node.removeEventListener("keydown", onKeydown);
      unlockBodyScroll();
      previouslyFocused?.focus();
    },
  };
}
