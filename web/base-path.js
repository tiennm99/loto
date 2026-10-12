/**
 * SvelteKit `paths.base` resolution, shared between the `sveltekit()`
 * plugin options (page routing / prerendering) and the PWA precache
 * manifest, both in `vite.config.js`. Both consumers MUST agree on the
 * base or the service worker's `additionalManifestEntries` end up
 * origin-rooted while the app itself serves under a sub-path (e.g. GitHub Pages `/loto`),
 * causing every precached URL to 404 and the SW install to fail.
 *
 * Resolution order:
 *   1. NEXT_BASE_PATH — explicit override (escape hatch)
 *   2. codeserver dev → /absproxy/{port}
 *   3. BUILD_PROFILE=gh → /loto
 *   4. default (local dev / generic static host) → ""
 *
 * @module base-path
 */
import { loadEnv } from "vite";

/**
 * SvelteKit validates the shape (leading `/`, no trailing `/`) when it loads
 * its config, so a malformed NEXT_BASE_PATH fails the build there.
 * @returns {"" | `/${string}`}
 */
export function resolveBase() {
  if (process.env.NEXT_BASE_PATH != null)
    return /** @type {"" | `/${string}`} */ (process.env.NEXT_BASE_PATH);
  if (process.env.VITE_DEV_PROFILE === "codeserver") {
    // .env.local lives outside process.env at config-eval time; loadEnv reads it.
    const env = loadEnv(
      process.env.NODE_ENV ?? "development",
      process.cwd(),
      "",
    );
    const port = env.CODESERVER_PORT ?? "3000";
    return `/absproxy/${port}`;
  }
  if (process.env.BUILD_PROFILE === "gh") return "/loto";
  return "";
}
