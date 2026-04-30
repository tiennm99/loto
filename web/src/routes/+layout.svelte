<script>
  import { onMount } from "svelte";
  import "../app.css";
  import { loadMaster } from "$lib/master-store.svelte.js";
  import { loadSettings } from "$lib/settings-store.svelte.js";

  let { children } = $props();

  // Hydrate global stores once so children mount with consistent state.
  // Master state must hydrate before PlayerBoard reads `masterState.called`
  // length — see the mode-mount race fix in the consistency refactor.
  onMount(() => {
    loadSettings();
    loadMaster();
  });
</script>

{@render children()}
