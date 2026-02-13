export type MediaType = "BOOK" | "MOVIE"

export type MediaStatus =
    | "WATCHED"
    | "FINISHED"
    | "READING"
    | "WATCHING"
    | "WANT_TO_WATCH"
    | "WANT_TO_READ"
    | "DROPPED"

export type RecommendationSelection = "BOOKS" | "MOVIES" | "BOTH"

export type PageResponse<T> = {
    items: T[]
    page: number
    size: number
    totalItems: number
    totalPages: number
}

export type MediaItemResponse = {
    id: string
    type: MediaType
    title: string
    year?: number | null
    creator?: string | null
    description?: string | null
    posterUrl?: string | null
    createdAt: string
    updatedAt: string
    latestStatus?: MediaStatus | null
    latestRating?: number | null
}

export type TagCategory = "THEME" | "MOOD" | "TONE" | "SETTING"

export type TagWeightDto = {
    category: TagCategory
    name: string
    weight: number
}

export type MovieMetadataDto = {
    runtimeMinutes?: number | null
    genres: string[]
}

export type BookMetadataDto = {
    pageCount?: number | null
    isbn?: string | null
    publisher?: string | null
}

export type MediaItemDetailResponse = MediaItemResponse & {
    latestNotes?: string | null
    latestWatchedAt?: string | null
    movieMetadata?: MovieMetadataDto | null
    bookMetadata?: BookMetadataDto | null
    tags: TagWeightDto[]
}

export type RecommendationTileResponse = {
    slot: number
    id: string
    title: string
    type: MediaType
    year?: number | null
    creator?: string | null
    reason: string
    matchedThemes: string[]
}

export type RecommendationGridResponse = {
    batchId: string
    selection: RecommendationSelection
    items: RecommendationTileResponse[]
}
