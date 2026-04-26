<script>
  import { base } from "$app/paths";
  import MasterPanel from "$lib/MasterPanel.svelte";
  import PageFooter from "$lib/PageFooter.svelte";
  import SettingsButton from "$lib/SettingsButton.svelte";
</script>

<div class="flex flex-col flex-1 items-center px-2 py-4 sm:px-3 sm:py-12">
  <div class="w-full max-w-2xl">
    <header class="relative text-center mb-6">
      <div class="absolute right-0 top-0">
        <SettingsButton />
      </div>
      <h1
        class="text-3xl sm:text-4xl font-extrabold tracking-tight bg-gradient-to-r from-orange-500 to-red-500 bg-clip-text text-transparent"
      >
        Quản trò
      </h1>
      <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
        Xổ số và theo dõi bảng lô tô
      </p>
      <a
        href="{base}/"
        class="mt-2 inline-block text-xs text-indigo-500 dark:text-indigo-400 hover:underline"
      >
        ← Về trang người chơi
      </a>
    </header>

    <MasterPanel />

    <PageFooter />
  </div>
</div>
