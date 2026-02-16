// app/exclusions/page.tsx

import Link from "next/link"
import { getExclusions } from "@/lib/api"
import { Nav } from "@/components/nav"
import { RemoveExclusionButton } from "@/components/remove-exclusion-button"
import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { Film, BookOpen, Ban, Search } from "lucide-react"
import type { Exclusion } from "@/lib/types"

export default async function ExclusionsPage() {
  let exclusions: Exclusion[] = []

  try {
    exclusions = await getExclusions()
  } catch {
    // API might be down
  }

  const movies = exclusions.filter((e) => e.mediaType === "MOVIE")
  const books = exclusions.filter((e) => e.mediaType === "BOOK")

  return (
    <main className="min-h-screen bg-background">
      <div className="mx-auto max-w-5xl px-6 py-10">
        <Nav current="/exclusions" />

        {/* Header */}
        <div className="flex items-end justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Exclusions</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {exclusions.length === 0
                ? "Titles you never want recommended."
                : `${exclusions.length} ${exclusions.length === 1 ? "title" : "titles"} excluded from recommendations.`}
            </p>
          </div>
          <Button variant="outline" size="sm" asChild>
            <Link href="/search">
              <Search className="h-4 w-4 mr-1.5" />
              Search to Exclude
            </Link>
          </Button>
        </div>

        <Separator className="my-6" />

        {/* Empty State */}
        {exclusions.length === 0 && (
          <div className="flex flex-col items-center gap-4 py-20 text-muted-foreground">
            <Ban className="h-10 w-10 text-muted-foreground/20" />
            <div className="text-center">
              <p>No excluded titles.</p>
              <p className="text-xs mt-1">
                Use the search page to exclude titles you don&apos;t want
                recommended.
              </p>
            </div>
          </div>
        )}

        {/* Movie Exclusions */}
        {movies.length > 0 && (
          <section>
            <div className="flex items-center gap-2 mb-3">
              <Film className="h-4 w-4 text-muted-foreground" />
              <h2 className="text-sm font-medium text-muted-foreground uppercase tracking-wide">
                Movies
              </h2>
              <span className="text-xs text-muted-foreground/50">
                {movies.length}
              </span>
            </div>
            <div className="flex flex-col gap-1.5">
              {movies.map((item) => (
                <ExclusionRow key={item.id} item={item} />
              ))}
            </div>
          </section>
        )}

        {/* Spacer */}
        {movies.length > 0 && books.length > 0 && (
          <Separator className="my-6" />
        )}

        {/* Book Exclusions */}
        {books.length > 0 && (
          <section>
            <div className="flex items-center gap-2 mb-3">
              <BookOpen className="h-4 w-4 text-muted-foreground" />
              <h2 className="text-sm font-medium text-muted-foreground uppercase tracking-wide">
                Books
              </h2>
              <span className="text-xs text-muted-foreground/50">
                {books.length}
              </span>
            </div>
            <div className="flex flex-col gap-1.5">
              {books.map((item) => (
                <ExclusionRow key={item.id} item={item} />
              ))}
            </div>
          </section>
        )}
      </div>
    </main>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// Exclusion Row
// ─────────────────────────────────────────────────────────────────────────────

function ExclusionRow({ item }: { item: Exclusion }) {
  return (
    <Card className="p-0 gap-0">
      <div className="flex items-center gap-3 px-3 py-2.5">
        {/* Title + Year */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <span className="text-sm font-medium truncate">{item.title}</span>
            {item.year && (
              <span className="text-xs text-muted-foreground shrink-0">
                {item.year}
              </span>
            )}
          </div>
          {item.reason && (
            <p className="text-xs text-muted-foreground/60 truncate mt-0.5">
              {item.reason}
            </p>
          )}
        </div>

        {/* Date */}
        <span className="text-[11px] text-muted-foreground/40 tabular-nums shrink-0">
          {new Date(item.createdAt).toLocaleDateString("en-US", {
            month: "short",
            day: "numeric",
          })}
        </span>

        {/* Remove */}
        <RemoveExclusionButton id={item.id} />
      </div>
    </Card>
  )
}
