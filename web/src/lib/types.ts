export type MediaType = "MOVIE" | "BOOK"

export type MediaStatus =
    | "WATCHED"
    | "FINISHED"
    | "READING"
    | "WATCHING"
    | "WANT_TO_WATCH"
    | "WANT_TO_READ"
    | "DROPPED"
    // legacy
    | "WANT_TO_CONSUME"
    | "IN_PROGRESS"
    | "DNF"

export type TagCategory = "THEME" | "MOOD" | "TONE" | "SETTING"

export interface MediaItem {
    id: string
    tmdbId: string
    openLibraryId: string
    title: string
    type: MediaType
    year: number
    creator: string
    description: string
    posterUrl: string
    createdAt: string
    updatedAt: string
}

export interface Tag {
    id: string
    name: string
    category: TagCategory
}

export interface MediaTag {
    id: string
    mediaId: string
    tagId: string
    weight: number
    generatedAt: string
    modelVersion: string
}

export interface WeightedTag {
    tag: Tag
    weight: number
}

export interface MediaMetadata {
    mediaId: string
    runtimeMinutes: number
    genres: string[]
}

export interface BookMetadata {
    mediaId: string
    pageCount: number
    isbn: string
    publisher: string
}

export interface Recommendation {
    title: string
    type: MediaType
    year: number | null
    creator: string
    reason: string
    matchedThemes: string[]
}

export interface TasteProfile {
    themes: Record<string, number>
    moods: Record<string, number>
    tones: Record<string, number>
    settings: Record<string, number>
    topRatedTitles: string[]
    lowRatedTitles: string[]
}

export interface WatchHistory {
    id: string
    mediaId: string
    watchedAt: string
    rating: number
    status: MediaStatus
    notes: string
    createdAt: string
}