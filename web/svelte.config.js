import adapter from "@sveltejs/adapter-static";
import { vitePreprocess } from "@sveltejs/vite-plugin-svelte";

const profile = process.env.BUILD_PROFILE;
const isCodeserver = process.env.VITE_DEV_PROFILE === "codeserver";

// basePath resolution:
//   1. NEXT_BASE_PATH (kept name for consistency with prior config; treated as opaque override)
//   2. codeserver dev → /absproxy/{port}
//   3. BUILD_PROFILE=gh → /loto
//   4. default (CF Pages root, local dev) → ""
function resolveBase() {
  if (process.env.NEXT_BASE_PATH != null) return process.env.NEXT_BASE_PATH;
  if (isCodeserver) {
    const port = process.env.CODESERVER_PORT ?? "3000";
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
