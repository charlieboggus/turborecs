"use client"

import { useState, useEffect, useTransition } from "react"
import Link from "next/link"
import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { BouncingDots } from "@/components/bouncing-dots"
import { Film, BookOpen, Sparkles, RefreshCw, Loader2 } from "lucide-react"
import {
  getRecommendationsAction,
  refreshRecommendationsAction,
} from "@/lib/actions"
import type { MediaType, RecommendationGrid } from "@/lib/types"

type PageState = "loading" | "empty" | "generating" | "loaded" | "error"

export function RecommendationsContent({
  mediaType,
}: {
  mediaType?: MediaType
}) {
  const [state, setState] = useState<PageState>("loading")
  const [grid, setGrid] = useState<RecommendationGrid | null>(null)
  const [isRefreshing, startRefresh] = useTransition()

  // Fetch cached grid on mount / filter change
  useEffect(() => {
    let cancelled = false
    setState("loading")
    setGrid(null)

    getRecommendationsAction(mediaType)
      .then((data) => {
        if (cancelled) return
        if (data && data.items.length > 0) {
          setGrid(data)
          setState("loaded")
        } else {
          setState("empty")
        }
      })
      .catch(() => {
        if (!cancelled) setState("empty")
      })

    return () => {
      cancelled = true
    }
  }, [mediaType])

  function handleGenerate() {
    setState("generating")
    startRefresh(async () => {
      try {
        const data = await refreshRecommendationsAction(mediaType)
        if (data && data.items.length > 0) {
          setGrid(data)
          setState("loaded")
        } else {
          setState("error")
        }
      } catch {
        setState("error")
      }
    })
  }

  function handleRefresh() {
    setState("generating")
    startRefresh(async () => {
      try {
        const data = await refreshRecommendationsAction(mediaType)
        if (data && data.items.length > 0) {
          setGrid(data)
          setState("loaded")
        } else {
          setState("error")
        }
      } catch {
        setState("error")
      }
    })
  }

  if (state === "loading") {
    return (
      <div className="flex items-center justify-center py-20">
        <BouncingDots label="Loading recommendations..." />
      </div>
    )
  }

  if (state === "empty") {
    return (
      <div className="flex flex-col items-center gap-5 py-20">
        <Sparkles className="h-10 w-10 text-muted-foreground/20" />
        <div className="text-center">
          <p className="text-muted-foreground">
            No recommendations generated yet.
          </p>
          <p className="text-xs text-muted-foreground/70 mt-1">
            This will use your taste profile to find new movies and books
            you&apos;ll love.
          </p>
        </div>
        <Button onClick={handleGenerate}>
          <Sparkles className="h-4 w-4 mr-1.5" />
          Generate Recommendations
        </Button>
      </div>
    )
  }

  if (state === "generating") {
    return (
      <div className="flex items-center justify-center py-20">
        <BouncingDots label="Generating recommendations..." />
      </div>
    )
  }

  if (state === "error") {
    return (
      <div className="flex flex-col items-center gap-4 py-20">
        <p className="text-muted-foreground">
          Something went wrong generating recommendations.
        </p>
        <p className="text-xs text-muted-foreground/70">
          Make sure you have rated items in your library.
        </p>
        <Button variant="outline" onClick={handleGenerate}>
          Try Again
        </Button>
      </div>
    )
  }

  return (
    <>
      <div className="flex justify-end mb-4">
        <Button
          variant="outline"
          size="sm"
          onClick={handleRefresh}
          disabled={isRefreshing}
        >
          <RefreshCw className="h-4 w-4 mr-1.5" />
          New Recommendations
        </Button>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {grid!.items.map((rec, i) => (
          <Card
            key={rec.id}
            className="p-0 gap-0 overflow-hidden transition-all hover:shadow-md hover:-translate-y-0.5"
          >
            <div className="p-4">
              <div className="flex items-start justify-between gap-3 mb-3">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-0.5">
                    <Badge
                      variant="outline"
                      className="text-[10px] px-1.5 py-0 shrink-0"
                    >
                      {rec.mediaType === "MOVIE" ? (
                        <Film className="h-2.5 w-2.5 mr-0.5" />
                      ) : (
                        <BookOpen className="h-2.5 w-2.5 mr-0.5" />
                      )}
                      {rec.mediaType === "MOVIE" ? "Movie" : "Book"}
                    </Badge>
                    {rec.year && (
                      <span className="text-xs text-muted-foreground">
                        {rec.year}
                      </span>
                    )}
                  </div>
                  <h3 className="text-sm font-semibold leading-snug">
                    {rec.title}
                  </h3>
                  {rec.creator && (
                    <p className="text-xs text-muted-foreground mt-0.5 truncate">
                      {rec.creator}
                    </p>
                  )}
                </div>
                <span className="text-2xl font-bold tabular-nums text-muted-foreground/15 leading-none">
                  {i + 1}
                </span>
              </div>
              <p className="text-xs leading-relaxed text-muted-foreground">
                {rec.reason}
              </p>
              {rec.matchedTags.length > 0 && (
                <div className="flex flex-wrap gap-1 mt-3">
                  {rec.matchedTags.map((tag) => (
                    <span
                      key={tag}
                      className="inline-block rounded-full bg-muted px-2 py-0.5 text-[10px] text-muted-foreground"
                    >
                      {tag}
                    </span>
                  ))}
                </div>
              )}
            </div>
          </Card>
        ))}
      </div>
    </>
  )
}
