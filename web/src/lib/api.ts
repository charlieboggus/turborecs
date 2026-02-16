import type {
  Exclusion,
  MediaItem,
  MediaSort,
  MediaType,
  PaginatedResponse,
  RecommendationGrid,
  SearchResult,
  Stats,
  TasteProfile,
} from "./types"

const API_BASE = process.env.TURBORECS_API_URL ?? "http://localhost:8080"

const API_INTERNAL_TOKEN = process.env.TURBORECS_INTERNAL_TOKEN ?? ""

const apiFetch = async <T>(path: string, options?: RequestInit): Promise<T> => {
  const url = `${API_BASE}${path}`
  const res = await fetch(url, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      "X-Internal-Auth": API_INTERNAL_TOKEN,
      ...options?.headers,
    },
  })
  if (!res.ok) {
    const body = await res.text().catch(() => "")
    throw new Error(`API error ${res.status}: ${body}`)
  }
  if (res.status === 204 || res.headers.get("content-length") === "0") {
    return undefined as T
  }
  return res.json()
}

// ─────────────────────────────────────────────────────────────────────────────
// Media Items (/api/media)
// ─────────────────────────────────────────────────────────────────────────────

export const getAllMediaItems = async (
  type?: MediaType,
  sortBy?: MediaSort,
  page?: number,
  limit?: number,
): Promise<MediaItem[]> => {
  const params = new URLSearchParams()
  if (type) {
    params.set("type", type)
  }
  if (sortBy) {
    params.set("sortBy", sortBy)
  }
  if (page) {
    params.set("page", page.toString())
  }
  if (limit) {
    params.set("limit", limit.toString())
  }
  const query = params.toString()
  return apiFetch<MediaItem[]>(`/api/media${query ? `?${query}` : ""}`)
}

export const getMediaItemsPaginated = async (
  type?: MediaType,
  page: number = 0,
  limit: number = 24,
  sortBy?: MediaSort,
): Promise<PaginatedResponse<MediaItem>> => {
  const params = new URLSearchParams()
  if (type) {
    params.set("type", type)
  }
  params.set("page", String(page))
  params.set("limit", String(limit))
  if (sortBy) {
    params.set("sortBy", sortBy)
  }
  return apiFetch<PaginatedResponse<MediaItem>>(`/api/media?${params}`)
}

export const getMediaItem = async (id: string): Promise<MediaItem> => {
  return apiFetch<MediaItem>(`/api/media/${id}`)
}

export const logMediaItem = async (body: {
  mediaType: MediaType
  tmdbId?: string
  openLibraryId?: string
  rating?: number
  consumedAt?: string
}): Promise<MediaItem> => {
  return apiFetch<MediaItem>("/api/media", {
    method: "POST",
    body: JSON.stringify(body),
  })
}

export const rateMediaItem = async (
  id: string,
  rating: number,
): Promise<MediaItem> => {
  return apiFetch<MediaItem>(`/api/media/${id}/rating`, {
    method: "PATCH",
    body: JSON.stringify({ rating }),
  })
}

export const deleteMediaItem = async (id: string): Promise<void> => {
  return apiFetch<void>(`/api/media/${id}`, {
    method: "DELETE",
  })
}

// ─────────────────────────────────────────────────────────────────────────────
// Search (/api/search)
// ─────────────────────────────────────────────────────────────────────────────

export const searchMedia = async (
  query: string,
  type: MediaType,
): Promise<SearchResult[]> => {
  const params = new URLSearchParams({ query, type })
  return apiFetch<SearchResult[]>(`/api/search?${params}`)
}

// ─────────────────────────────────────────────────────────────────────────────
// Recommendations (/api/recommendations)
// ─────────────────────────────────────────────────────────────────────────────

export const getRecommendations = async (
  mediaType?: MediaType,
): Promise<RecommendationGrid> => {
  const params = new URLSearchParams()
  if (mediaType) {
    params.set("mediaType", mediaType)
  }
  const query = params.toString()
  return apiFetch<RecommendationGrid>(
    `/api/recommendations${query ? `${query}` : ""}`,
  )
}

export const refreshRecommendations = async (
  mediaType?: MediaType,
): Promise<RecommendationGrid> => {
  const params = new URLSearchParams()
  if (mediaType) {
    params.set("mediaType", mediaType)
  }
  const query = params.toString()
  return apiFetch<RecommendationGrid>(
    `/api/recommendations/refresh${query ? `${query}` : ""}`,
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// Exclusions
// ─────────────────────────────────────────────────────────────────────────────

export const getExclusions = async (): Promise<Exclusion[]> => {
  return apiFetch<Exclusion[]>("/api/exclusions")
}

export const excludeMedia = async (body: {
  mediaType: MediaType
  title: string
  year?: number
  tmdbId?: string
  openLibraryId?: string
  reason?: string
}): Promise<Exclusion> => {
  return apiFetch<Exclusion>("/api/exclusions", {
    method: "POST",
    body: JSON.stringify(body),
  })
}

export const removeExclusion = async (id: string): Promise<void> => {
  return apiFetch<void>(`/api/exclusions/${id}`, {
    method: "DELETE",
  })
}

// ─────────────────────────────────────────────────────────────────────────────
// Stats & Taste Profile (/api/stats & /api/taste-profile)
// ─────────────────────────────────────────────────────────────────────────────

export const getStats = async (): Promise<Stats> => {
  return apiFetch<Stats>("/api/stats")
}

export const getTasteProfile = async (): Promise<TasteProfile> => {
  return apiFetch<TasteProfile>("/api/taste-profile")
}
