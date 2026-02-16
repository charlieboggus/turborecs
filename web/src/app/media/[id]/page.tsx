import Link from "next/link"
import { notFound } from "next/navigation"
import { getMediaItem } from "@/lib/api"
import { Nav } from "@/components/nav"
import { RatingControl } from "@/components/rating-control"
import { DeleteMediaButton } from "@/components/delete-media-button"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
import { Button } from "@/components/ui/button"
import {
  Film,
  BookOpen,
  ArrowLeft,
  Clock,
  BookText,
  Calendar,
  User,
  Tag,
  Clapperboard,
} from "lucide-react"

interface MediaDetailPageProps {
  params: Promise<{ id: string }>
  searchParams: Promise<{
    from?: string
    page?: string
    type?: string
    sortBy?: string
  }>
}

export default async function MediaDetailPage({
  params,
  searchParams,
}: MediaDetailPageProps) {
  const { id } = await params
  const query = await searchParams

  let item
  try {
    item = await getMediaItem(id)
  } catch {
    notFound()
  }

  // Build back link preserving library state
  const backUrl = buildBackUrl(query)

  const isMovie = item.mediaType === "MOVIE"

  return (
    <main className="min-h-screen bg-background">
      <div className="mx-auto max-w-5xl px-6 py-10">
        <Nav current="/library" />

        {/* Back Link */}
        <Button
          variant="ghost"
          size="sm"
          asChild
          className="mb-6 -ml-2 text-muted-foreground"
        >
          <Link href={backUrl}>
            <ArrowLeft className="h-4 w-4 mr-1" />
            Back to Library
          </Link>
        </Button>

        {/* Main Layout */}
        <div className="flex flex-col sm:flex-row gap-8">
          {/* Poster */}
          <div className="w-full sm:w-64 shrink-0">
            {item.posterUrl ? (
              <div className="aspect-[2/3] overflow-hidden rounded-xl border">
                <img
                  src={item.posterUrl}
                  alt={item.title}
                  className="h-full w-full object-cover"
                />
              </div>
            ) : (
              <div className="aspect-[2/3] flex items-center justify-center rounded-xl border bg-muted">
                {isMovie ? (
                  <Film className="h-16 w-16 text-muted-foreground/20" />
                ) : (
                  <BookOpen className="h-16 w-16 text-muted-foreground/20" />
                )}
              </div>
            )}
          </div>

          {/* Details */}
          <div className="flex-1 min-w-0">
            {/* Type Badge */}
            <Badge variant="outline" className="mb-3 text-xs">
              {isMovie ? (
                <Film className="h-3 w-3 mr-1" />
              ) : (
                <BookOpen className="h-3 w-3 mr-1" />
              )}
              {isMovie ? "Movie" : "Book"}
            </Badge>

            {/* Title */}
            <h1 className="text-3xl font-bold tracking-tight">{item.title}</h1>

            {/* Creator / Year */}
            {(item.creator || item.year) && (
              <p className="mt-1 text-muted-foreground">
                {[item.creator, item.year].filter(Boolean).join(" · ")}
              </p>
            )}

            {/* Rating */}
            <div className="mt-4">
              <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-1.5">
                Your Rating
              </p>
              <RatingControl mediaId={item.id} initialRating={item.rating} />
            </div>

            <Separator className="my-5" />

            {/* Metadata Grid */}
            <div className="grid grid-cols-2 gap-x-8 gap-y-3">
              {item.genres && item.genres.length > 0 && (
                <MetadataField
                  icon={<Tag className="h-3.5 w-3.5" />}
                  label="Genres"
                >
                  <div className="flex flex-wrap gap-1">
                    {item.genres.map((genre) => (
                      <Badge
                        key={genre}
                        variant="secondary"
                        className="text-xs"
                      >
                        {genre}
                      </Badge>
                    ))}
                  </div>
                </MetadataField>
              )}

              {isMovie && item.runtimeMinutes && (
                <MetadataField
                  icon={<Clock className="h-3.5 w-3.5" />}
                  label="Runtime"
                >
                  {formatRuntime(item.runtimeMinutes)}
                </MetadataField>
              )}

              {!isMovie && item.pageCount && (
                <MetadataField
                  icon={<BookText className="h-3.5 w-3.5" />}
                  label="Pages"
                >
                  {item.pageCount.toLocaleString()}
                </MetadataField>
              )}

              {!isMovie && item.publisher && (
                <MetadataField
                  icon={<Clapperboard className="h-3.5 w-3.5" />}
                  label="Publisher"
                >
                  {item.publisher}
                </MetadataField>
              )}

              {!isMovie && item.isbn && (
                <MetadataField
                  icon={<BookText className="h-3.5 w-3.5" />}
                  label="ISBN"
                >
                  {item.isbn}
                </MetadataField>
              )}

              {item.consumedAt && (
                <MetadataField
                  icon={<Calendar className="h-3.5 w-3.5" />}
                  label={isMovie ? "Watched" : "Read"}
                >
                  {new Date(item.consumedAt).toLocaleDateString("en-US", {
                    year: "numeric",
                    month: "long",
                    day: "numeric",
                  })}
                </MetadataField>
              )}

              <MetadataField
                icon={<Calendar className="h-3.5 w-3.5" />}
                label="Added"
              >
                {new Date(item.createdAt).toLocaleDateString("en-US", {
                  year: "numeric",
                  month: "long",
                  day: "numeric",
                })}
              </MetadataField>
            </div>

            {/* Description */}
            {item.description && (
              <>
                <Separator className="my-5" />
                <div>
                  <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-2">
                    Description
                  </p>
                  <p className="text-sm leading-relaxed text-muted-foreground">
                    {item.description}
                  </p>
                </div>
              </>
            )}

            {/* Tagging Status */}
            {item.taggingStatus && item.taggingStatus !== "TAGGED" && (
              <>
                <Separator className="my-5" />
                <Badge
                  variant={
                    item.taggingStatus === "FAILED" ? "destructive" : "outline"
                  }
                  className="text-xs"
                >
                  Tagging: {item.taggingStatus.toLowerCase()}
                </Badge>
              </>
            )}

            {/* Actions */}
            <Separator className="my-5" />
            <DeleteMediaButton mediaId={item.id} title={item.title} />
          </div>
        </div>
      </div>
    </main>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

function MetadataField({
  icon,
  label,
  children,
}: {
  icon: React.ReactNode
  label: string
  children: React.ReactNode
}) {
  return (
    <div>
      <div className="flex items-center gap-1.5 text-muted-foreground mb-1">
        {icon}
        <span className="text-xs font-medium uppercase tracking-wide">
          {label}
        </span>
      </div>
      <div className="text-sm">{children}</div>
    </div>
  )
}

function formatRuntime(minutes: number): string {
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  if (h === 0) return `${m}m`
  if (m === 0) return `${h}h`
  return `${h}h ${m}m`
}

function buildBackUrl(query: {
  from?: string
  page?: string
  type?: string
  sortBy?: string
}): string {
  if (query.from !== "library") return "/library"
  const params = new URLSearchParams()
  if (query.page && query.page !== "0") params.set("page", query.page)
  if (query.type) params.set("type", query.type)
  if (query.sortBy) params.set("sortBy", query.sortBy)
  const qs = params.toString()
  return `/library${qs ? `?${qs}` : ""}`
}
