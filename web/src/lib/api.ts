import type {
    MediaItem,
    MediaType,
    MediaStatus,
    Recommendation,
    TasteProfile,
} from "./types"

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api"

async function fetchApi<T>(
    endpoint: string,
    options?: RequestInit,
): Promise<T> {
    const res = await fetch(`${API_BASE}${endpoint}`, {
        headers: { "Content-Type": "application/json" },
        ...options,
    })

    if (!res.ok) {
        const error = await res
            .json()
            .catch(() => ({ message: "Request failed" }))
        throw new Error(error.message || `HTTP ${res.status}`)
    }

    if (res.status === 204) return undefined as T
    return res.json()
}

// Media CRUD
export const mediaApi = {
    getAll: (params?: {
        type?: MediaType
        status?: MediaStatus
        sortBy?: string
    }) => {
        const query = new URLSearchParams()
        if (params?.type) query.set("type", params.type)
        if (params?.status) query.set("status", params.status)
        if (params?.sortBy) query.set("sortBy", params.sortBy)
        const qs = query.toString()
        return fetchApi<MediaItem[]>(`/media${qs ? `?${qs}` : ""}`)
    },

    getById: (id: string) => fetchApi<MediaItem>(`/media/${id}`),

    create: (title: string, type: MediaType) =>
        fetchApi<MediaItem>("/media", {
            method: "POST",
            body: JSON.stringify({ title, type }),
        }),

    updateStatus: (id: string, status: MediaStatus) =>
        fetchApi<MediaItem>(`/media/${id}/status`, {
            method: "PATCH",
            body: JSON.stringify({ status }),
        }),

    updateRating: (id: string, rating: number) =>
        fetchApi<MediaItem>(`/media/${id}/rating`, {
            method: "PATCH",
            body: JSON.stringify({ rating }),
        }),

    updateNotes: (id: string, notes: string | null) =>
        fetchApi<MediaItem>(`/media/${id}/notes`, {
            method: "PATCH",
            body: JSON.stringify({ notes }),
        }),

    delete: (id: string) =>
        fetchApi<void>(`/media/${id}`, { method: "DELETE" }),

    search: (query: string) =>
        fetchApi<MediaItem[]>(
            `/media/search?query=${encodeURIComponent(query)}`,
        ),
}

// Recommendations
export const recommendationsApi = {
    get: (params?: { count?: number; type?: MediaType }) => {
        const query = new URLSearchParams()
        if (params?.count) query.set("count", String(params.count))
        if (params?.type) query.set("type", params.type)
        const qs = query.toString()
        return fetchApi<Recommendation[]>(
            `/recommendations${qs ? `?${qs}` : ""}`,
        )
    },

    pick: (type?: MediaType) => {
        const qs = type ? `?type=${type}` : ""
        return fetchApi<Recommendation>(`/recommendations/pick${qs}`)
    },

    profile: (type?: MediaType) => {
        const qs = type ? `?type=${type}` : ""
        return fetchApi<TasteProfile>(`/recommendations/profile${qs}`)
    },
}
