import { sveltekit } from "@sveltejs/kit/vite";
import tailwindcss from "@tailwindcss/vite";
import { defineConfig, loadEnv } from "vite";

export default defineConfig(({ mode }) => {
  // .env.local lives outside process.env at config-eval time; loadEnv reads it.
  const env = loadEnv(mode, process.cwd(), "");
  const isCodeserver = process.env.VITE_DEV_PROFILE === "codeserver";
  const host = env.CODESERVER_HOST;
  const port = Number(env.CODESERVER_PORT ?? 3000);

  return {
    plugins: [tailwindcss(), sveltekit()],
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
