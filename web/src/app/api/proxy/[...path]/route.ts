import { NextRequest, NextResponse } from "next/server"

export const runtime = "nodejs"
export const dynamic = "force-dynamic"

const API_URL = process.env.API_INTERNAL_URL
const INTERNAL_AUTH = process.env.INTERNAL_AUTH_TOKEN
const ADMIN_AUTH = process.env.ADMIN_AUTH_TOKEN

function mustEnv(name: string, value: string | undefined): string {
    if (!value) throw new Error(`Missing env var: ${name}`)
    return value
}

export async function GET(
    req: NextRequest,
    ctx: { params: Promise<{ path: string[] }> },
) {
    return forward(req, ctx)
}
export async function POST(
    req: NextRequest,
    ctx: { params: Promise<{ path: string[] }> },
) {
    return forward(req, ctx)
}
export async function PATCH(
    req: NextRequest,
    ctx: { params: Promise<{ path: string[] }> },
) {
    return forward(req, ctx)
}
export async function DELETE(
    req: NextRequest,
    ctx: { params: Promise<{ path: string[] }> },
) {
    return forward(req, ctx)
}

async function forward(
    req: NextRequest,
    ctx: { params: Promise<{ path: string[] }> },
) {
    const { path } = await ctx.params

    const base = mustEnv("API_INTERNAL_URL", API_URL)
    const url = new URL(req.url)

    // /api/proxy/<...>?q=... -> <API_URL>/<...>?q=...
    const target = new URL(`${base.replace(/\/$/, "")}/${path.join("/")}`)
    target.search = url.search

    const method = req.method.toUpperCase()
    const hasBody = method !== "GET" && method !== "HEAD"

    const headers = new Headers()
    headers.set("Accept", req.headers.get("accept") ?? "application/json")

    // forward content-type if present
    const ct = req.headers.get("content-type")
    if (ct) headers.set("Content-Type", ct)

    // attach auth server-side
    if (INTERNAL_AUTH) headers.set("X-Internal-Auth", INTERNAL_AUTH)

    // optional: only send admin token for admin endpoints
    const isAdminPath = path[0] === "api" && path[1] === "admin"
    if (isAdminPath && ADMIN_AUTH) headers.set("X-Admin-Auth", ADMIN_AUTH)

    const body = hasBody ? await req.arrayBuffer() : undefined

    console.log(
        "PROXY",
        method,
        req.nextUrl.pathname + req.nextUrl.search,
        "->",
        target.toString(),
    )
    const res = await fetch(target, {
        method,
        headers,
        body: body && body.byteLength > 0 ? body : undefined,
        cache: "no-store",
    })

    // Copy response headers (minus a few hop-by-hop / problematic ones)
    const outHeaders = new Headers(res.headers)
    outHeaders.delete("content-encoding")
    outHeaders.delete("content-length")
    outHeaders.delete("transfer-encoding")
    outHeaders.delete("connection")

    return new NextResponse(res.body, {
        status: res.status,
        headers: outHeaders,
    })
}
