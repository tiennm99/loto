import adapter from "@sveltejs/adapter-static";
import { vitePreprocess } from "@sveltejs/vite-plugin-svelte";
import { loadEnv } from "vite";

const profile = process.env.BUILD_PROFILE;
const isCodeserver = process.env.VITE_DEV_PROFILE === "codeserver";

// Same env-loading concern as vite.config.js — .env.local isn't in
// process.env at config-eval time. loadEnv reads it.
const env = loadEnv(process.env.NODE_ENV ?? "development", process.cwd(), "");

// basePath resolution:
//   1. NEXT_BASE_PATH — explicit override (escape hatch)
//   2. codeserver dev → /absproxy/{port}
//   3. BUILD_PROFILE=gh → /loto
//   4. default (CF Pages root, local dev) → ""
function resolveBase() {
  if (process.env.NEXT_BASE_PATH != null) return process.env.NEXT_BASE_PATH;
  if (isCodeserver) {
    const port = env.CODESERVER_PORT ?? "3000";
    return `/absproxy/${port}`;
  }
  if (profile === "gh") return "/loto";
  return "";
}

export default {
  preprocess: vitePreprocess(),
  kit: {
    adapter: adapter({
      pages: "build",
      assets: "build",
      fallback: undefined,
      precompress: false,
      strict: true,
    }),
    paths: { base: resolveBase() },
  },
};
