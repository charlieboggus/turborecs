import type { MediaStatus, MediaType } from "@/lib/apiTypes"

export const MOVIE_STATUSES: MediaStatus[] = [
    "WANT_TO_WATCH",
    "WATCHING",
    "WATCHED",
    "DROPPED",
]
export const BOOK_STATUSES: MediaStatus[] = [
    "WANT_TO_READ",
    "READING",
    "FINISHED",
    "DROPPED",
]

export function statusesForType(type: MediaType): MediaStatus[] {
    return type === "MOVIE" ? MOVIE_STATUSES : BOOK_STATUSES
}

export function statusLabel(s: MediaStatus): string {
    switch (s) {
        case "WANT_TO_WATCH":
            return "Want to watch"
        case "WATCHING":
            return "Watching"
        case "WATCHED":
            return "Watched"
        case "WANT_TO_READ":
            return "Want to read"
        case "READING":
            return "Reading"
        case "FINISHED":
            return "Finished"
        case "DROPPED":
            return "Dropped"
        default:
            return s
    }
}
