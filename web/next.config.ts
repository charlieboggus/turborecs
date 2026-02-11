import type { NextConfig } from "next"

const nextConfig: NextConfig = {
    /* config options here */
    reactCompiler: true,
    output: "standalone",

    async rewrites() {
        return [
            {
                source: "/api/:path*",
                destination: process.env.API_INTERNAL_URL
                    ? `${process.env.API_INTERNAL_URL}/:path*`
                    : "http://localhost:8080/:path*"
            }
        ]
    }
}

export default nextConfig
