import Link from "next/link"
import { getMediaItemsPaginated } from "@/lib/api"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { Film, BookOpen, Star, ChevronLeft, ChevronRight } from "lucide-react"
import { Nav } from "@/components/nav"
import type { MediaType, MediaSort } from "@/lib/types"

interface LibraryPageProps {
  searchParams: Promise<{
    page?: string
    type?: MediaType
    sortBy?: MediaSort
  }>
}

export default async function LibraryPage({ searchParams }: LibraryPageProps) {
  const params = await searchParams
  const page = Math.max(0, parseInt(params.page ?? "0", 10) || 0)
  const type = params.type
  const sortBy = params.sortBy ?? "DATE_ADDED"
  const pageSize = 24

  const data = await getMediaItemsPaginated(type, page, pageSize, sortBy)

  return (
    <main className="min-h-screen bg-background">
      <div className="mx-auto max-w-5xl px-6 py-10">
        <Nav current="/library" />
        <div className="flex items-end justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Library</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {data.totalElements} {data.totalElements === 1 ? "item" : "items"}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <FilterLink
              label="All"
              href={buildUrl({ sortBy })}
              active={!type}
            />
            <FilterLink
              label="Movies"
              href={buildUrl({ type: "MOVIE", sortBy })}
              active={type === "MOVIE"}
            />
            <FilterLink
              label="Books"
              href={buildUrl({ type: "BOOK", sortBy })}
              active={type === "BOOK"}
            />
            <Separator orientation="vertical" className="h-5 mx-1" />
            <FilterLink
              label="Recent"
              href={buildUrl({ type, sortBy: "DATE_ADDED" })}
              active={sortBy === "DATE_ADDED"}
            />
            <FilterLink
              label="Rating"
              href={buildUrl({ type, sortBy: "RATING" })}
              active={sortBy === "RATING"}
            />
            <FilterLink
              label="Title"
              href={buildUrl({ type, sortBy: "TITLE" })}
              active={sortBy === "TITLE"}
            />
          </div>
        </div>
        <Separator className="my-6" />
        {data.content.length === 0 ? (
          <div className="flex flex-col items-center gap-4 py-20 text-muted-foreground">
            <p>No items found.</p>
            <Button asChild variant="outline">
              <Link href="/search">Search for something to add</Link>
            </Button>
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4">
            {data.content.map((item) => (
              <Link
                key={item.id}
                href={`/media/${item.id}?from=library&page=${page}${type ? `&type=${type}` : ""}${sortBy ? `&sortBy=${sortBy}` : ""}`}
              >
                <Card className="group h-full overflow-hidden p-0 gap-0 transition-all hover:bg-muted/50 hover:shadow-md hover:-translate-y-0.5">
                  {item.posterUrl ? (
                    <div className="aspect-[2/3] overflow-hidden bg-muted">
                      <img
                        src={item.posterUrl}
                        alt={item.title}
                        className="h-full w-full object-cover transition-transform group-hover:scale-[1.02]"
                      />
                    </div>
                  ) : (
                    <div className="aspect-[2/3] flex items-center justify-center bg-muted">
                      {item.mediaType === "MOVIE" ? (
                        <Film className="h-10 w-10 text-muted-foreground/30" />
                      ) : (
                        <BookOpen className="h-10 w-10 text-muted-foreground/30" />
                      )}
                    </div>
                  )}

                  <CardContent className="p-3">
                    <Badge
                      variant="outline"
                      className="mb-1.5 text-[10px] px-1.5 py-0"
                    >
                      {item.mediaType === "MOVIE" ? "Movie" : "Book"}
                    </Badge>
                    <h3 className="text-sm font-medium leading-snug line-clamp-2">
                      {item.title}
                    </h3>
                    {(item.creator || item.year) && (
                      <p className="mt-0.5 text-xs text-muted-foreground truncate">
                        {[item.creator, item.year].filter(Boolean).join(" · ")}
                      </p>
                    )}
                    {item.rating && (
                      <div className="mt-1.5 flex items-center gap-0.5">
                        {Array.from({ length: 5 }, (_, i) => (
                          <Star
                            key={i}
                            className={`h-3 w-3 ${
                              i < item.rating!
                                ? "fill-amber-500 text-amber-500"
                                : "text-muted-foreground/25"
                            }`}
                          />
                        ))}
                      </div>
                    )}
                  </CardContent>
                </Card>
              </Link>
            ))}
          </div>
        )}
        {data.totalPages > 1 && (
          <div className="mt-8 flex items-center justify-center gap-2">
            <Button variant="outline" size="sm" asChild disabled={data.first}>
              <Link
                href={buildUrl({ page: page - 1, type, sortBy })}
                className={data.first ? "pointer-events-none opacity-50" : ""}
              >
                <ChevronLeft className="h-4 w-4 mr-1" />
                Previous
              </Link>
            </Button>

            <span className="text-sm tabular-nums text-muted-foreground px-3">
              {page + 1} / {data.totalPages}
            </span>

            <Button variant="outline" size="sm" asChild disabled={data.last}>
              <Link
                href={buildUrl({ page: page + 1, type, sortBy })}
                className={data.last ? "pointer-events-none opacity-50" : ""}
              >
                Next
                <ChevronRight className="h-4 w-4 ml-1" />
              </Link>
            </Button>
          </div>
        )}
      </div>
    </main>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

function buildUrl(params: {
  page?: number
  type?: MediaType
  sortBy?: MediaSort
}): string {
  const search = new URLSearchParams()
  if (params.page && params.page > 0) search.set("page", String(params.page))
  if (params.type) search.set("type", params.type)
  if (params.sortBy) search.set("sortBy", params.sortBy)
  const query = search.toString()
  return `/library${query ? `?${query}` : ""}`
}

function FilterLink({
  label,
  href,
  active,
}: {
  label: string
  href: string
  active: boolean
}) {
  return (
    <Link
      href={href}
      className={`px-2.5 py-1 rounded-md text-xs transition-colors ${
        active
          ? "bg-foreground text-background font-medium"
          : "text-muted-foreground hover:text-foreground hover:bg-muted"
      }`}
    >
      {label}
    </Link>
  )
}
