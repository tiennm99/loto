<script>
  import { base } from "$app/paths";
  import PlayerBoard from "$lib/PlayerBoard.svelte";
  import SettingsButton from "$lib/SettingsButton.svelte";

  let showInstructions = $state(false);
</script>

<div class="flex flex-col flex-1 items-center px-3 py-8 sm:py-12">
  <div class="w-full max-w-lg">
    <!-- Header -->
    <header class="relative text-center mb-8">
      <div class="absolute right-0 top-0">
        <SettingsButton />
      </div>
      <h1
        class="text-4xl sm:text-5xl font-extrabold tracking-tight bg-gradient-to-r from-indigo-500 to-purple-500 bg-clip-text text-transparent"
      >
        Lô tô
      </h1>
      <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">
        Lấy cảm hứng từ những buổi họp lớp thiếu giấy chơi lô tô
        <br class="hidden sm:block" /> của TN1 (2014–2017)
      </p>
      <div class="mt-3 flex items-center justify-center gap-3 text-xs">
        <button
          onclick={() => (showInstructions = !showInstructions)}
          class="text-indigo-500 dark:text-indigo-400 hover:underline"
        >
          {showInstructions ? "Ẩn hướng dẫn" : "Hướng dẫn"}
        </button>
        <span class="text-slate-300 dark:text-slate-600">|</span>
        <a
          href="{base}/master"
          class="text-orange-500 dark:text-orange-400 hover:underline"
        >
          Trang quản trò →
        </a>
      </div>
    </header>

    {#if showInstructions}
      <div
        class="mb-6 rounded-xl bg-indigo-50 dark:bg-indigo-950/30 border border-indigo-100 dark:border-indigo-900 p-4 text-sm text-slate-600 dark:text-slate-400"
      >
        <ul class="space-y-1 list-disc list-inside">
          <li>
            Nhấn <strong class="text-slate-800 dark:text-slate-200"
              >Tạo bảng mới</strong
            > để tạo bảng
          </li>
          <li>Nhấn vào ô số để đánh dấu khi số được xổ</li>
          <li>Nhấn lại để bỏ đánh dấu</li>
          <li>Bảng được lưu tự động</li>
        </ul>
      </div>
    {/if}

    <PlayerBoard />

    <footer
      class="mt-10 text-center text-xs text-slate-400 dark:text-slate-600"
    >
      Made with ❤️ by{" "}
      <a
        href="https://miti99.com"
        target="_blank"
        rel="noopener noreferrer"
        class="text-indigo-500 hover:underline"
      >
        miti99
      </a>
    </footer>
  </div>
</div>
