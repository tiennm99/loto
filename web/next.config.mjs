const isProd = process.env.NODE_ENV === "production";
const isCodeserver = process.env.NEXT_DEV_PROFILE === "codeserver";

// Build profiles select basePath for the deploy target:
//   gh — GitHub Pages, served at tiennm99.github.io/loto
//   cf — Cloudflare Pages, served at loto.miti99.com (custom domain, root)
const buildProfile = process.env.BUILD_PROFILE;

// In dev under code-server's reverse proxy, basePath/assetPrefix must match
// the proxy URL so links, assets, and the HMR socket all resolve.
// Use /absproxy/{port} (NOT /proxy/{port}) — /proxy strips the prefix before
// forwarding; /absproxy preserves it so basePath matches the incoming request.
// CODESERVER_HOST and CODESERVER_PORT are loaded from .env.local by Next.
function codeserverConfig() {
  const host = process.env.CODESERVER_HOST;
  if (!host) {
    throw new Error(
      "CODESERVER_HOST is required when NEXT_DEV_PROFILE=codeserver (set it in .env.local)"
    );
  }
  const port = process.env.CODESERVER_PORT ?? "3000";
  return { basePath: `/absproxy/${port}`, allowedDevOrigins: [host] };
}

const cs = isCodeserver ? codeserverConfig() : null;

// basePath resolution order (first non-null wins):
//   1. NEXT_BASE_PATH — explicit override (escape hatch for forks / custom domains)
//   2. codeserver dev profile
//   3. BUILD_PROFILE=gh|cf — explicit deploy target
//   4. NODE_ENV=production — legacy fallback (bare `npm run build` → GH Pages)
//   5. local dev — empty
const basePath =
  process.env.NEXT_BASE_PATH ??
  cs?.basePath ??
  (buildProfile === "gh"
    ? "/loto"
    : buildProfile === "cf"
      ? ""
      : isProd
        ? "/loto"
        : "");

const nextConfig = {
  output: "export",
  basePath,
  assetPrefix: basePath || undefined,
  ...(cs?.allowedDevOrigins ? { allowedDevOrigins: cs.allowedDevOrigins } : {}),
};

export default nextConfig;
