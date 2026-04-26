<script>
  import { onMount } from "svelte";
  import "../app.css";
  import { loadSettings } from "$lib/settings-store.svelte.js";

  let { children } = $props();

  // Hydrate settings once so the empty-cell color is applied before
  // either page renders its grid. onMount (not $effect) makes the
  // single-shot intent explicit.
  onMount(loadSettings);
</script>

{@render children()}
