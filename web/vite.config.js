import { sveltekit } from "@sveltejs/kit/vite";
import tailwindcss from "@tailwindcss/vite";

const isCodeserver = process.env.VITE_DEV_PROFILE === "codeserver";
const host = process.env.CODESERVER_HOST;
const port = Number(process.env.CODESERVER_PORT ?? 3000);

const codeserverServer = host
  ? {
      port,
      host: true,
      strictPort: true,
      allowedHosts: [host],
      hmr: {
        host,
        protocol: "wss",
        clientPort: 443,
        path: `/absproxy/${port}/`,
      },
    }
  : { port: 3000, host: true };

export default {
  plugins: [tailwindcss(), sveltekit()],
  server: isCodeserver ? codeserverServer : { port: 3000 },
};
