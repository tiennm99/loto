---
title: UI/UX Patterns for "Waiting Cell" Highlights in Lô Tô Grid
date: 2026-04-30
---

## Animation Patterns for "One-Away" Cell Highlighting

**Recommended approach:** Pulse with 2s cycle duration (most common in UI — combines attention without fatigue). [Material 3 Expressive emphasizes "guiding eyes without forcing attention"](https://supercharge.design/blog/material-3-expressive) via soft, physics-based pulses. Ring/glow variants work but consume more visual weight on a dense 9×9 grid. **Scale-bounce** risks overloading the card; **gradient sweep** adds motion fatigue over long dwell times.

## Overlay Opacity for Prominence + Readability

Material Design opacity system: **87% for primary emphasis, 60% for secondary, 38% for tertiary**. For a cell indicator layered over card content, use **70–85% opacity** on the highlight color to maintain readability of underlying numbers while achieving prominence. [MDN confirms static color at reduced opacity suffices for visual hierarchy](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/At-rules/@media/prefers-reduced-motion) without motion dependency.

## Reduced-Motion Compliance

**Fallback:** Static color change (no motion required). Replace pulse animation with persistent 75–80% opacity highlight when `prefers-reduced-motion: reduce` is detected. [W3C guidance](https://www.w3.org/WAI/WCAG22/Techniques/css/C39) confirms static styling communicates state without barriers. **No motion is acceptable; animations are a refinement, not a requirement.**

## Loop Duration & Fatigue Prevention

Standard pulse: **2-second cycle** (common in Material Design and CSS animation libraries). **WCAG 2.2.2** requires any animation >5s to be pausable or auto-stop. For "waiting" state (indefinite), implement: pulse runs **2s cycles but auto-pauses after 3–5 cycles** (6–10s total), then re-engages on user interaction or row status change. Avoids vestibular/cognitive fatigue; [confirmed by accessibility research](https://usability.yale.edu/digital-accessibility/accessibility-resources/accessibility-articles/animated-content-and-timing).

## Color Choice: Amber Context

Vietnamese lotto culture traditionally uses **red/gold** as primary lucky colors (prosperity / Tết connotations). **Amber-500 is a safe choice** — it reads as warm/attention-grabbing without clashing with traditional reds. No specific cultural taboo against amber in lotto/gaming context; amber sits between red (luck) and yellow (wealth). **Recommendation:** Keep amber-500 if already established in your toast pattern; adds visual consistency between notification and card highlight.

---

**Sources:**
- [Material 3 Expressive Design](https://supercharge.design/blog/material-3-expressive)
- [MDN Web Docs: prefers-reduced-motion](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/At-rules/@media/prefers-reduced-motion)
- [W3C WCAG Techniques: CSS prefers-reduced-motion](https://www.w3.org/WAI/WCAG22/Techniques/css/C39)
- [Yale Digital Accessibility: Animation & Timing](https://usability.yale.edu/digital-accessibility/accessibility-resources/accessibility-articles/animated-content-and-timing)
- [Material Design States](https://m3.material.io/foundations/interaction/states/state-layers)
- [CSS-Tricks: Accessible Web Animation & WCAG](https://css-tricks.com/accessible-web-animation-the-wcag-on-animation-explained/)
