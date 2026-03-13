import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  distDir: process.env.E2E_BUILD === "1" ? ".next-e2e" : ".next",
  output: "standalone",
  reactStrictMode: true,
  experimental: {
    browserDebugInfoInTerminal: false,
    devtoolSegmentExplorer: false
  },
  async rewrites() {
    const proxyTarget = process.env.API_PROXY_TARGET?.replace(/\/$/, "");
    if (!proxyTarget) {
      return [];
    }

    return [
      {
        source: "/backend-api/:path*",
        destination: `${proxyTarget}/:path*`
      }
    ];
  }
};

export default nextConfig;
