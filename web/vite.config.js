import { readFileSync } from "node:fs";
import adapter from "@sveltejs/adapter-static";
import { sveltekit } from "@sveltejs/kit/vite";
import { vitePreprocess } from "@sveltejs/vite-plugin-svelte";
import tailwindcss from "@tailwindcss/vite";
import { defineConfig, loadEnv } from "vite";
import { VitePWA } from "vite-plugin-pwa";

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

// SvelteKit writes its build to `.svelte-kit/output` (`client/` for
// static files and JS, `prerendered/pages/` for the HTML), and the adapter
// copies both into `build/` as its last step.
const KIT_OUTPUT = ".svelte-kit/output";

/**
 * Maps a path under KIT_OUTPUT to the URL the deployed app serves it at.
 * Regular files become paths relative to `sw.js` (which sits at the app
 * root, so Workbox resolves them under the base); the prerendered index
 * page becomes the base URL itself, matching `navigateFallback`.
 * @param {string} file
 * @returns {string}
 */
function precacheUrl(file) {
  if (file.startsWith("client/")) return file.slice("client/".length);
  const page = file
    .replace(/^prerendered\/pages\//, "")
    .replace(/(^|\/)index\.html$/, "$1")
    .replace(/\.html$/, "");
  return page === "" ? `${base}/` : page;
}

/**
 * vite-plugin-pwa wired into SvelteKit's build. The plugin's own build step
 * generates `sw.js` when each Vite environment closes, but SvelteKit 3 builds
 * SSR first, then the client, then prerenders, all inside its `buildApp`
 * hook, so at those moments the app shell isn't on disk yet. Instead this
 * drops that step and generates the service worker from a `buildApp` hook
 * that runs after SvelteKit's (it must come after `sveltekit()` in the
 * plugin list) and before the adapter's `post`-ordered one, which then
 * copies `sw.js` into `build/` along with everything else.
 *
 * Also dropped, because SvelteKit renders `src/app.html` itself and warns
 * about any plugin with a `transformIndexHtml` hook: `pwa-assets` (serves
 * the unused `pwaAssets` icon generator; icons live in /static) and
 * `dev-sw` (only acts when `devOptions.enabled` is true). Re-enabling
 * either feature means keeping its plugin.
 * @param {Partial<import("vite-plugin-pwa").VitePWAOptions>} options
 * @returns {import("vite").Plugin[]}
 */
function svelteKitPWA(options) {
  const plugins = VitePWA(options);
  const main = plugins.find((p) => p.name === "vite-plugin-pwa");
  const dropped = new Set([
    "vite-plugin-pwa:build",
    "vite-plugin-pwa:pwa-assets",
    "vite-plugin-pwa:dev-sw",
  ]);
  return [
    ...plugins.filter((p) => !dropped.has(p.name)),
    {
      name: "loto:pwa-generate-sw",
      apply: "build",
      async buildApp() {
        /** @type {import("vite-plugin-pwa").VitePluginPWAAPI | undefined} */
        const api = main?.api;
        if (!api) throw new Error("vite-plugin-pwa API not found");
        await api.generateSW();
      },
    },
  ];
}

export default defineConfig(({ mode }) => {
  // .env.local lives outside process.env at config-eval time; loadEnv reads it.
  const env = loadEnv(mode, process.cwd(), "");
  const isCodeserver = process.env.VITE_DEV_PROFILE === "codeserver";
  const host = env.CODESERVER_HOST;
  const port = Number(env.CODESERVER_PORT ?? 3000);

  return {
    plugins: [
      tailwindcss(),
      sveltekit({
        preprocess: vitePreprocess(),
        adapter: adapter({
          pages: "build",
          assets: "build",
          fallback: undefined,
          precompress: false,
          strict: true,
        }),
        paths: { base },
        // Kit 3 polls `_app/version.json` hourly by default. The PWA's
        // service worker already owns update detection (see the
        // `registerType: "prompt"` note below), so keep polling off as it
        // was under Kit 2.
        version: { pollInterval: 0 },
      }),
      svelteKitPWA({
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
        // `+layout.svelte` imports `virtual:pwa-register` itself, so the
        // plugin must not inject a registration script of its own.
        injectRegister: false,
        // Absolute SW URL and scope (`/loto/sw.js`, `/loto/`), so
        // registration never depends on the page URL's trailing slash.
        base: `${base}/`,
        outDir: `${KIT_OUTPUT}/client`,
        workbox: {
          globDirectory: KIT_OUTPUT,
          // manifest.webmanifest is left out: the browser fetches it
          // itself when installing, outside the service worker's cache.
          globPatterns: [
            "client/**/*.{js,css,html,svg,png,woff2}",
            "prerendered/pages/**/*.html",
          ],
          manifestTransforms: [
            async (entries) => ({
              manifest: entries.map((e) => ({ ...e, url: precacheUrl(e.url) })),
            }),
          ],
          // Content-hashed files need no revision. Tested against the glob
          // path, before `manifestTransforms` strips the `client/` prefix.
          dontCacheBustURLsMatching: /^client\/_app\/immutable\//,
          navigateFallback: `${base}/`,
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
