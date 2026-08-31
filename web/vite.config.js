import { readFileSync } from "node:fs";
import { sveltekit } from "@sveltejs/kit/vite";
import tailwindcss from "@tailwindcss/vite";
import { SvelteKitPWA } from "@vite-pwa/sveltekit";
import { defineConfig, loadEnv } from "vite";

import { resolveBase } from "./base-path.js";

// Precache the default voice's clips so the app is fully offline-capable
// on first install (without bloating the install with every voice).
// Alternate voices fall through to runtime CacheFirst on first play.
// `audio-manifest.js` is the single source of truth for voice ids; we
// duplicate the read here because vite.config can't import .svelte.js.
const audioManifest = JSON.parse(
  readFileSync("./static/audio/manifest.json", "utf8"),
);
const defaultVoiceId = audioManifest.voices[0].id;
const clipNames = [
  ...Array.from({ length: 90 }, (_, i) => String(i + 1)),
  "cho",
  "kinh",
];
// `additionalManifestEntries` are passed to workbox-build verbatim — the
// plugin only base-prefixes entries it generates itself from globs. Without
// this, the GitHub Pages build (base "/loto") precaches origin-rooted
// "/audio/..." URLs that 404 under "/loto/", and Workbox aborts the SW
// `install` event on the first failed precache request.
const base = resolveBase();
const defaultVoicePrecacheEntries = clipNames.map((n) => ({
  url: `${base}/audio/${defaultVoiceId}/${n}.mp3`,
  // Workbox needs a revision string to invalidate stale clips.
  // Bump the prefix when audio is regenerated.
  revision: `audio-v1-${defaultVoiceId}-${n}`,
}));

export default defineConfig(({ mode }) => {
  // .env.local lives outside process.env at config-eval time; loadEnv reads it.
  const env = loadEnv(mode, process.cwd(), "");
  const isCodeserver = process.env.VITE_DEV_PROFILE === "codeserver";
  const host = env.CODESERVER_HOST;
  const port = Number(env.CODESERVER_PORT ?? 3000);

  return {
    plugins: [
      tailwindcss(),
      sveltekit(),
      SvelteKitPWA({
        // "prompt" (not "autoUpdate"): a deploy must never force-reload a
        // live tab mid-round — that would drop in-memory state
        // (autoRunning, showCongrats, the auto-call countdown). The new SW
        // installs and waits; `+layout.svelte` registers
        // `onNeedRefresh: showUpdatePrompt` (see lib/update-prompt.svelte.js)
        // so the host sees a small "Có bản mới. Tải lại?" banner and the
        // reload only happens on an explicit tap.
        registerType: "prompt",
        // Ship a hand-written manifest so the icons/theme stay aligned
        // with /static; the plugin can also generate one but mixing is
        // confusing.
        strategies: "generateSW",
        manifest: false,
        workbox: {
          globPatterns: ["**/*.{js,css,html,svg,png,woff2,webmanifest}"],
          maximumFileSizeToCacheInBytes: 5 * 1024 * 1024,
          // App shell + DEFAULT voice clips → guaranteed offline. Other
          // voices fall through to the CacheFirst runtime rule below
          // (cached on first play, offline thereafter).
          additionalManifestEntries: defaultVoicePrecacheEntries,
          runtimeCaching: [
            {
              urlPattern: /\/audio\/.*\.mp3$/,
              handler: "CacheFirst",
              options: {
                cacheName: "loto-audio",
                expiration: {
                  // Headroom for future voice growth: 4× current 184 clips.
                  maxEntries: 400,
                  // 7d (was 30d). A clip stays cached as long as it gets
                  // played at least once a week. Voices that go unused
                  // for 7d fall out and re-fetch on next play —
                  // approximates LRU without a custom workbox plugin.
                  // Each clip <200KB so re-fetch cost is negligible.
                  maxAgeSeconds: 60 * 60 * 24 * 7,
                  // Belt-and-suspenders for the rare quota-pressure case
                  // (lots of voices precached + other origins competing).
                  purgeOnQuotaError: true,
                },
                // Audio is same-origin → no need to allow opaque (0).
                cacheableResponse: { statuses: [200] },
              },
            },
          ],
        },
        devOptions: { enabled: false },
      }),
    ],
    server: {
      port,
      host: true,
      strictPort: true,
      // Always allow the codeserver host if .env.local sets one — covers
      // both `npm run dev` (no proxy config) and `npm run dev:codeserver`.
      ...(host ? { allowedHosts: [host] } : {}),
      // HMR through the proxy only in the explicit codeserver profile.
      ...(isCodeserver && host
        ? {
            hmr: {
              host,
              protocol: "wss",
              clientPort: 443,
              path: `/absproxy/${port}/`,
            },
          }
        : {}),
    },
  };
});
