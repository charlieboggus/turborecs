function isServer() {
    return typeof window === "undefined"
}

function apiBase(): string {
    const base = process.env.API_INTERNAL_URL
    if (!base) throw new Error("Missing API_INTERNAL_URL in environment")
    return base.replace(/\/+$/, "")
}

function buildUrl(path: string): string {
    if (!path.startsWith("/"))
        throw new Error(`api path must start with '/': ${path}`)

    if (!isServer()) return `/api/proxy${path}`

    return `${apiBase()}${path}`
}

async function check(res: Response, method: string, path: string) {
    if (res.ok) return
    const text = await res.text().catch(() => "")
    throw new Error(`${method} ${path} failed: ${res.status} ${text}`)
}

function mustEnv(name: string, value: string | undefined): string {
    if (!value) throw new Error(`Missing env var: ${name}`)
    return value
}

function buildAuthHeaders(
    includeContentType: boolean,
): HeadersInit | undefined {
    if (!isServer()) {
        // Browser never needs tokens; proxy route adds them server-side.
        return includeContentType
            ? { "content-type": "application/json" }
            : undefined
    }

    // Server-side only:
    const h = new Headers()
    if (includeContentType) h.set("content-type", "application/json")

    const internal = process.env.INTERNAL_AUTH_TOKEN
    const admin = process.env.ADMIN_AUTH_TOKEN

    if (internal) h.set("X-Internal-Auth", internal)
    if (admin) h.set("X-Admin-Auth", admin)

    return h
}

export async function apiGet<T>(path: string): Promise<T> {
    const url = buildUrl(path)
    const res = await fetch(url, {
        cache: "no-store",
        headers: buildAuthHeaders(false), // no content-type on GET
    })
    await check(res, "GET", path)
    return (await res.json()) as T
}

export async function apiPost<T>(path: string, body?: unknown): Promise<T> {
    const url = buildUrl(path)
    const res = await fetch(url, {
        method: "POST",
        headers: buildAuthHeaders(true),
        body: body === undefined ? undefined : JSON.stringify(body),
        cache: "no-store",
    })
    await check(res, "POST", path)
    return (await res.json()) as T
}

export async function apiPatch<T>(path: string, body?: unknown): Promise<T> {
    const url = buildUrl(path)
    const res = await fetch(url, {
        method: "PATCH",
        headers: buildAuthHeaders(true),
        body: body === undefined ? undefined : JSON.stringify(body),
        cache: "no-store",
    })
    await check(res, "PATCH", path)

    if (res.status === 204) return undefined as T
    return (await res.json()) as T
}

export async function apiDelete(path: string): Promise<void> {
    const url = buildUrl(path)
    const res = await fetch(url, {
        method: "DELETE",
        cache: "no-store",
        headers: buildAuthHeaders(false),
    })
    await check(res, "DELETE", path)
}

export async function apiPatchNoContent(
    path: string,
    body?: unknown,
): Promise<void> {
    const url = buildUrl(path)
    const res = await fetch(url, {
        method: "PATCH",
        headers: buildAuthHeaders(true),
        body: body === undefined ? undefined : JSON.stringify(body),
        cache: "no-store",
    })
    await check(res, "PATCH", path)
}
