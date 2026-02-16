"use client"

import { useState, useTransition } from "react"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  Film,
  BookOpen,
  Search,
  Plus,
  Ban,
  Loader2,
  Star,
  Check,
  X,
} from "lucide-react"
import { searchAction, logMediaAction, excludeMediaAction } from "@/lib/actions"
import type { MediaType, SearchResult } from "@/lib/types"

type ActionState = "idle" | "loading" | "success" | "error"

interface ResultAction {
  resultKey: string
  type: "log" | "exclude"
  state: ActionState
}

export function SearchInterface() {
  const [query, setQuery] = useState("")
  const [mediaType, setMediaType] = useState<MediaType>("MOVIE")
  const [results, setResults] = useState<SearchResult[]>([])
  const [hasSearched, setHasSearched] = useState(false)
  const [isSearching, startSearch] = useTransition()

  // Track action state per result
  const [actions, setActions] = useState<Record<string, ResultAction>>({})

  // Log dialog
  const [logTarget, setLogTarget] = useState<SearchResult | null>(null)
  const [logRating, setLogRating] = useState(0)
  const [logHover, setLogHover] = useState(0)
  const [isLogging, startLog] = useTransition()

  // Exclude dialog
  const [excludeTarget, setExcludeTarget] = useState<SearchResult | null>(null)
  const [excludeReason, setExcludeReason] = useState("")
  const [isExcluding, startExclude] = useTransition()

  function resultKey(r: SearchResult): string {
    return `${r.mediaType}:${r.tmdbId ?? r.openLibraryId ?? r.title}`
  }

  function handleSearch() {
    if (!query.trim()) return
    startSearch(async () => {
      try {
        const data = await searchAction(query.trim(), mediaType)
        setResults(data)
      } catch {
        setResults([])
      }
      setHasSearched(true)
    })
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === "Enter") handleSearch()
  }

  function openLogDialog(result: SearchResult) {
    setLogTarget(result)
    setLogRating(0)
    setLogHover(0)
  }

  function confirmLog() {
    if (!logTarget) return
    const key = resultKey(logTarget)
    const externalId = logTarget.tmdbId ?? logTarget.openLibraryId
    if (!externalId) return

    startLog(async () => {
      setActions((prev) => ({
        ...prev,
        [key]: { resultKey: key, type: "log", state: "loading" },
      }))
      try {
        await logMediaAction(
          logTarget.mediaType,
          externalId,
          logRating > 0 ? logRating : undefined,
        )
        setActions((prev) => ({
          ...prev,
          [key]: { resultKey: key, type: "log", state: "success" },
        }))
      } catch {
        setActions((prev) => ({
          ...prev,
          [key]: { resultKey: key, type: "log", state: "error" },
        }))
      }
      setLogTarget(null)
    })
  }

  function openExcludeDialog(result: SearchResult) {
    setExcludeTarget(result)
    setExcludeReason("")
  }

  function confirmExclude() {
    if (!excludeTarget) return
    const key = resultKey(excludeTarget)
    const externalId = excludeTarget.tmdbId ?? excludeTarget.openLibraryId

    startExclude(async () => {
      setActions((prev) => ({
        ...prev,
        [key]: { resultKey: key, type: "exclude", state: "loading" },
      }))
      try {
        await excludeMediaAction(
          excludeTarget.mediaType,
          excludeTarget.title,
          excludeTarget.year ?? undefined,
          externalId ?? undefined,
          excludeReason.trim() || undefined,
        )
        setActions((prev) => ({
          ...prev,
          [key]: { resultKey: key, type: "exclude", state: "success" },
        }))
      } catch {
        setActions((prev) => ({
          ...prev,
          [key]: { resultKey: key, type: "exclude", state: "error" },
        }))
      }
      setExcludeTarget(null)
    })
  }

  return (
    <>
      {/* Search Controls */}
      <div className="flex gap-2">
        {/* Type Toggle */}
        <div className="flex rounded-md border">
          <button
            onClick={() => setMediaType("MOVIE")}
            className={`flex items-center gap-1.5 px-3 py-2 text-sm transition-colors rounded-l-md ${
              mediaType === "MOVIE"
                ? "bg-foreground text-background font-medium"
                : "text-muted-foreground hover:text-foreground"
            }`}
          >
            <Film className="h-3.5 w-3.5" />
            Movie
          </button>
          <button
            onClick={() => setMediaType("BOOK")}
            className={`flex items-center gap-1.5 px-3 py-2 text-sm transition-colors rounded-r-md ${
              mediaType === "BOOK"
                ? "bg-foreground text-background font-medium"
                : "text-muted-foreground hover:text-foreground"
            }`}
          >
            <BookOpen className="h-3.5 w-3.5" />
            Book
          </button>
        </div>

        {/* Search Input */}
        <div className="relative flex-1">
          <Input
            placeholder={
              mediaType === "MOVIE"
                ? "Search for a movie..."
                : "Search for a book..."
            }
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
            className="pr-10"
          />
        </div>

        <Button onClick={handleSearch} disabled={isSearching || !query.trim()}>
          {isSearching ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <Search className="h-4 w-4" />
          )}
          <span className="ml-1.5">Search</span>
        </Button>
      </div>

      {/* Results */}
      <div className="mt-6">
        {isSearching && (
          <div className="flex items-center justify-center py-16 text-muted-foreground">
            <Loader2 className="h-5 w-5 animate-spin mr-2" />
            Searching...
          </div>
        )}

        {!isSearching && hasSearched && results.length === 0 && (
          <div className="flex flex-col items-center py-16 text-muted-foreground">
            <p>No results found for &quot;{query}&quot;</p>
            <p className="text-xs mt-1">Try a different search term.</p>
          </div>
        )}

        {!isSearching && results.length > 0 && (
          <div className="flex flex-col gap-2">
            {results.map((result) => {
              const key = resultKey(result)
              const action = actions[key]
              const isActioned =
                action?.state === "success" || action?.state === "loading"

              return (
                <Card
                  key={key}
                  className={`p-0 gap-0 transition-opacity ${
                    action?.state === "success" ? "opacity-50" : ""
                  }`}
                >
                  <div className="flex gap-4 p-3">
                    {/* Poster */}
                    {result.posterUrl ? (
                      <div className="w-16 shrink-0">
                        <div className="aspect-[2/3] overflow-hidden rounded-md">
                          <img
                            src={result.posterUrl}
                            alt={result.title}
                            className="h-full w-full object-cover"
                          />
                        </div>
                      </div>
                    ) : (
                      <div className="w-16 shrink-0">
                        <div className="aspect-[2/3] flex items-center justify-center rounded-md bg-muted">
                          {result.mediaType === "MOVIE" ? (
                            <Film className="h-5 w-5 text-muted-foreground/30" />
                          ) : (
                            <BookOpen className="h-5 w-5 text-muted-foreground/30" />
                          )}
                        </div>
                      </div>
                    )}

                    {/* Info */}
                    <div className="flex-1 min-w-0 flex flex-col justify-center">
                      <div className="flex items-center gap-2">
                        <h3 className="text-sm font-medium truncate">
                          {result.title}
                        </h3>
                        {result.year && (
                          <span className="text-xs text-muted-foreground shrink-0">
                            {result.year}
                          </span>
                        )}
                      </div>
                      {result.creator && (
                        <p className="text-xs text-muted-foreground truncate mt-0.5">
                          {result.creator}
                        </p>
                      )}
                      {result.description && (
                        <p className="text-xs text-muted-foreground/70 line-clamp-2 mt-1 leading-relaxed">
                          {result.description}
                        </p>
                      )}
                    </div>

                    {/* Actions */}
                    <div className="flex items-center gap-1.5 shrink-0">
                      {action?.state === "success" ? (
                        <Badge
                          variant="outline"
                          className="text-xs gap-1 px-2 py-1"
                        >
                          <Check className="h-3 w-3" />
                          {action.type === "log" ? "Added" : "Excluded"}
                        </Badge>
                      ) : (
                        <>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => openLogDialog(result)}
                            disabled={isActioned}
                            className="text-xs h-8 px-2.5"
                          >
                            {action?.state === "loading" &&
                            action.type === "log" ? (
                              <Loader2 className="h-3 w-3 animate-spin" />
                            ) : (
                              <Plus className="h-3 w-3" />
                            )}
                            <span className="ml-1">Add</span>
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => openExcludeDialog(result)}
                            disabled={isActioned}
                            className="text-xs h-8 px-2 text-muted-foreground"
                          >
                            {action?.state === "loading" &&
                            action.type === "exclude" ? (
                              <Loader2 className="h-3 w-3 animate-spin" />
                            ) : (
                              <Ban className="h-3 w-3" />
                            )}
                          </Button>
                        </>
                      )}
                    </div>
                  </div>
                </Card>
              )
            })}
          </div>
        )}
      </div>

      {/* Log Dialog */}
      <Dialog
        open={logTarget !== null}
        onOpenChange={(open) => {
          if (!open) setLogTarget(null)
        }}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Add to Library</DialogTitle>
            <DialogDescription>
              {logTarget && (
                <>
                  <span className="font-medium text-foreground">
                    {logTarget.title}
                  </span>
                  {logTarget.year && (
                    <span className="text-muted-foreground">
                      {" "}
                      ({logTarget.year})
                    </span>
                  )}
                </>
              )}
            </DialogDescription>
          </DialogHeader>

          <div className="py-2">
            <p className="text-sm text-muted-foreground mb-3">
              Rate this {logTarget?.mediaType === "MOVIE" ? "movie" : "book"}{" "}
              <span className="text-xs">(optional)</span>
            </p>
            <div className="flex items-center gap-1">
              {Array.from({ length: 5 }, (_, i) => {
                const starIndex = i + 1
                const filled = starIndex <= (logHover || logRating)
                return (
                  <button
                    key={i}
                    onClick={() =>
                      setLogRating(starIndex === logRating ? 0 : starIndex)
                    }
                    onMouseEnter={() => setLogHover(starIndex)}
                    onMouseLeave={() => setLogHover(0)}
                    className="p-0.5 transition-transform hover:scale-110"
                  >
                    <Star
                      className={`h-6 w-6 transition-colors ${
                        filled
                          ? "fill-amber-500 text-amber-500"
                          : "text-muted-foreground/25"
                      }`}
                    />
                  </button>
                )
              })}
              {logRating > 0 && (
                <button
                  onClick={() => setLogRating(0)}
                  className="ml-2 text-xs text-muted-foreground hover:text-foreground"
                >
                  Clear
                </button>
              )}
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setLogTarget(null)}
              disabled={isLogging}
            >
              Cancel
            </Button>
            <Button onClick={confirmLog} disabled={isLogging}>
              {isLogging ? (
                <Loader2 className="h-4 w-4 animate-spin mr-1.5" />
              ) : (
                <Plus className="h-4 w-4 mr-1.5" />
              )}
              Add to Library
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Exclude Dialog */}
      <Dialog
        open={excludeTarget !== null}
        onOpenChange={(open) => {
          if (!open) setExcludeTarget(null)
        }}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Exclude from Recommendations</DialogTitle>
            <DialogDescription>
              {excludeTarget && (
                <>
                  <span className="font-medium text-foreground">
                    {excludeTarget.title}
                  </span>{" "}
                  will never be recommended to you.
                </>
              )}
            </DialogDescription>
          </DialogHeader>

          <div className="py-2">
            <p className="text-sm text-muted-foreground mb-2">
              Reason <span className="text-xs">(optional)</span>
            </p>
            <Input
              placeholder="e.g. Not interested, Already seen it elsewhere..."
              value={excludeReason}
              onChange={(e) => setExcludeReason(e.target.value)}
            />
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setExcludeTarget(null)}
              disabled={isExcluding}
            >
              Cancel
            </Button>
            <Button
              variant="destructive"
              onClick={confirmExclude}
              disabled={isExcluding}
            >
              {isExcluding ? (
                <Loader2 className="h-4 w-4 animate-spin mr-1.5" />
              ) : (
                <Ban className="h-4 w-4 mr-1.5" />
              )}
              Exclude
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  )
}
