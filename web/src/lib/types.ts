export type MediaType = "MOVIE" | "BOOK"

export type TaggingStatus = "PENDING" | "TAGGED" | "FAILED"

export type TagCategory = "THEME" | "MOOD" | "TONE" | "SETTING"

export type MediaSort = "RATING" | "TITLE" | "DATE_ADDED"

export interface MediaItem {
  id: string
  mediaType: MediaType
  title: string
  year: number | null
  creator: string | null
  description: string | null
  posterUrl: string | null
  rating: number | null
  consumedAt: string | null
  taggingStatus: TaggingStatus
  createdAt: string
  updatedAt: string
  genres: string[]
  runtimeMinutes: number | null
  pageCount: number | null
  isbn: string | null
  publisher: string | null
}

export interface SearchResult {
  mediaType: MediaType
  title: string
  year: number | null
  creator: string | null
  posterUrl: string | null
  description: string | null
  tmdbId: string | null
  openLibraryId: string | null
}

export interface Recommendation {
  id: string
  mediaType: MediaType
  title: string
  year: number | null
  creator: string | null
  reason: string
  matchedTags: string[]
}

export interface RecommendationGrid {
  batchId: string
  items: Recommendation[]
}

export interface Exclusion {
  id: string
  mediaType: MediaType
  title: string
  year: number | null
  reason: string | null
  createdAt: string
}

export interface TasteProfile {
  themes: Record<string, number>
  moods: Record<string, number>
  tones: Record<string, number>
  settings: Record<string, number>
  topRatedTitles: string[]
  lowRatedTitles: string[]
}

export interface Stats {
  totalItems: number
  movieCount: number
  bookCount: number
  uniqueTagCount: number
  tagAssignmentCount: number
  recommendationCount: number
  exclusionCount: number
}
