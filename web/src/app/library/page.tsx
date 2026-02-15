import Link from "next/link"
import { apiGet } from "@/lib/api"
import type {
  MediaItemResponse,
  PageResponse,
  MediaStatus,
  MediaType,
} from "@/lib/apiTypes"

import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent } from "@/components/ui/card"

import { Home, ChevronLeft, ChevronRight } from "lucide-react"
import { LogMediaPopover } from "@/components/log-media-popover"

type Props = {
  searchParams: Promise<{
    page?: string
    size?: string
    type?: MediaType
    status?: MediaStatus
  }>
}

function cn(...xs: Array<string | false | null | undefined>) {
  return xs.filter(Boolean).join(" ")
}

function qsFor(opts: {
  page: number
  size: number
  type?: MediaType
  status?: MediaStatus
}) {
  const qs = new URLSearchParams()
  qs.set("page", String(opts.page))
  qs.set("size", String(opts.size))
  if (opts.type) qs.set("type", opts.type)
  if (opts.status) qs.set("status", opts.status)
  return qs.toString()
}

function TypePill({ type }: { type: MediaType }) {
  return (
    <div className="rounded-full bg-black/70 text-white text-[11px] px-2 py-1">
      {type}
    </div>
  )
}

function StatusBadge({ status }: { status: string }) {
  return (
    <Badge variant="secondary" className="text-[11px] font-medium">
      {status}
    </Badge>
  )
}

function RatingBadge({ rating }: { rating: number }) {
  return (
    <Badge
      variant="outline"
      className="text-[11px] font-medium tabular-nums"
      title={`Rating: ${rating}`}
    >
      ★ {rating}
    </Badge>
  )
}

export default async function LibraryPage({ searchParams }: Props) {
  const sp = await searchParams
  const page = Number(sp.page ?? "0")
  const size = Number(sp.size ?? "50")
  const type = sp.type
  const status = sp.status

  const data = await apiGet<PageResponse<MediaItemResponse>>(
    `/api/media?${qsFor({ page, size, type, status })}`,
  )

  const prevHref = `/library?${qsFor({
    page: Math.max(0, page - 1),
    size,
    type,
    status,
  })}`

  const nextHref = `/library?${qsFor({
    page: Math.min(data.totalPages - 1, page + 1),
    size,
    type,
    status,
  })}`

  const prevDisabled = page <= 0
  const nextDisabled = page + 1 >= data.totalPages

  return (
    <div className="p-6 space-y-5">
      {/* Sticky Top Bar */}
      <div className="sticky top-0 z-20 -mx-6 px-6 py-3 border-b bg-white/85 backdrop-blur dark:bg-black/60">
        <div className="flex items-start justify-between gap-4">
          {/* Left: nav + title/meta */}
          <div className="flex items-start gap-3 min-w-0">
            {/* Home as icon-only, feels like app chrome */}
            <Button
              asChild
              variant="ghost"
              size="icon"
              className="shrink-0 rounded-xl"
              title="Home"
            >
              <Link href="/">
                <Home className="h-4 w-4" />
              </Link>
            </Button>

            <div className="min-w-0">
              <h1 className="text-2xl font-semibold tracking-tight leading-none">
                Library
              </h1>
              <div className="mt-1 text-sm text-muted-foreground">
                Page <span className="tabular-nums">{data.page + 1}</span> /{" "}
                <span className="tabular-nums">{data.totalPages}</span> •{" "}
                <span className="tabular-nums">{data.totalItems}</span> items
              </div>
            </div>
          </div>

          {/* Right: pagination */}
          <div className="flex items-center gap-2 pt-0.5">
            <LogMediaPopover compact />
            <Button
              asChild={!prevDisabled}
              variant="outline"
              size="sm"
              disabled={prevDisabled}
              className="gap-1"
            >
              {prevDisabled ? (
                <span className="inline-flex items-center gap-1">
                  <ChevronLeft className="h-4 w-4" />
                  Prev
                </span>
              ) : (
                <Link href={prevHref} className="inline-flex items-center gap-1">
                  <ChevronLeft className="h-4 w-4" />
                  Prev
                </Link>
              )}
            </Button>

            <Button
              asChild={!nextDisabled}
              variant="outline"
              size="sm"
              disabled={nextDisabled}
              className="gap-1"
            >
              {nextDisabled ? (
                <span className="inline-flex items-center gap-1">
                  Next
                  <ChevronRight className="h-4 w-4" />
                </span>
              ) : (
                <Link href={nextHref} className="inline-flex items-center gap-1">
                  Next
                  <ChevronRight className="h-4 w-4" />
                </Link>
              )}
            </Button>
          </div>
        </div>
      </div>

      {/* Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-5">
        {data.items.map((m) => (
          <Card
            key={m.id}
            className={cn(
              "group rounded-2xl border bg-white/50 dark:bg-white/5",
              "overflow-hidden transition-all",
              "hover:-translate-y-0.5 hover:shadow-md hover:border-black/20 dark:hover:border-white/20",
              "focus-within:ring-2 focus-within:ring-black/20 dark:focus-within:ring-white/30",
            )}
          >
            <Link href={`/media/${m.id}`} className="block focus:outline-none">
              <div className="relative aspect-[2/3] bg-black/5 overflow-hidden">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                {m.posterUrl ? (
                  <img
                    src={m.posterUrl}
                    alt={m.title}
                    className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-[1.04]"
                    loading="lazy"
                  />
                ) : (
                  <div className="h-full w-full flex items-center justify-center text-xs text-muted-foreground">
                    No poster
                  </div>
                )}

                {/* subtle hover sheen */}
                <div className="pointer-events-none absolute inset-0 opacity-0 transition-opacity duration-300 group-hover:opacity-100">
                  <div className="absolute inset-0 bg-gradient-to-t from-black/25 via-transparent to-transparent" />
                </div>

                {/* top-left type pill */}
                <div className="absolute left-2 top-2">
                  <TypePill type={m.type} />
                </div>
              </div>

              <CardContent className="p-3 space-y-2">
                <div className="text-sm font-semibold leading-snug line-clamp-2">
                  {m.title}
                </div>

                <div className="text-xs text-muted-foreground flex items-center gap-2">
                  <span className="tabular-nums">{m.year ?? "—"}</span>
                  {m.creator ? (
                    <span className="line-clamp-1">• {m.creator}</span>
                  ) : null}
                </div>

                <div className="pt-1 flex flex-wrap items-center gap-2">
                  <StatusBadge status={m.latestStatus ?? "No status"} />
                  {m.latestRating ? <RatingBadge rating={m.latestRating} /> : null}
                </div>
              </CardContent>
            </Link>
          </Card>
        ))}
      </div>
    </div>
  )
}