// @ts-check

const isProd = process.env.NODE_ENV === "production";
const isCodeserver = process.env.NEXT_DEV_PROFILE === "codeserver";

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
// NEXT_BASE_PATH wins so custom-domain / fork deploys don't have to edit code.
const basePath =
  process.env.NEXT_BASE_PATH ?? cs?.basePath ?? (isProd ? "/loto" : "");

/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "export",
  basePath,
  assetPrefix: basePath || undefined,
  ...(cs?.allowedDevOrigins ? { allowedDevOrigins: cs.allowedDevOrigins } : {}),
};

export default nextConfig;
