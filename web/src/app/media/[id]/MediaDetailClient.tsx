"use client"

import { useMemo, useState, useTransition } from "react"
import type { MediaItemDetailResponse, MediaStatus } from "@/lib/apiTypes"
import { statusesForType, statusLabel } from "@/lib/mediaStatus"
import { apiPatchNoContent } from "@/lib/api"

// shadcn/ui
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
import { Textarea } from "@/components/ui/textarea"

function fmtDateOnly(iso: string) {
    // iso is date-time; render *date only*
    const d = new Date(iso)
    return new Intl.DateTimeFormat(undefined, {
        year: "numeric",
        month: "short",
        day: "2-digit",
    }).format(d)
}

function fmtLocalDateOnly(isoDate: string) {
    // for YYYY-MM-DD values (like latestWatchedAt)
    const d = new Date(`${isoDate}T00:00:00`)
    return new Intl.DateTimeFormat(undefined, {
        year: "numeric",
        month: "short",
        day: "2-digit",
    }).format(d)
}

function formatRuntime(mins: number) {
    if (!Number.isFinite(mins) || mins <= 0) return "—"
    const h = Math.floor(mins / 60)
    const m = mins % 60
    if (h <= 0) return `${m}m`
    if (m === 0) return `${h}h`
    return `${h}h ${m}m`
}

function cx(...xs: Array<string | false | null | undefined>) {
    return xs.filter(Boolean).join(" ")
}

function StatTile({ label, value }: { label: string; value: string }) {
    return (
        <div className="rounded-xl border bg-muted/30 px-3 py-2.5">
            <div className="text-[10px] font-semibold tracking-wide uppercase text-muted-foreground">
                {label}
            </div>
            <div className="mt-1 text-sm font-medium tabular-nums whitespace-nowrap">
                {value}
            </div>
        </div>
    )
}

function MetaTile({
    label,
    value,
    children,
}: {
    label: string
    value?: string
    children?: React.ReactNode
}) {
    return (
        <div className="rounded-xl border bg-muted/20 px-3 py-2.5">
            <div className="text-[10px] font-semibold tracking-wide uppercase text-muted-foreground">
                {label}
            </div>
            {value ? (
                <div className="mt-1 text-sm text-foreground/80">{value}</div>
            ) : null}
            {children ? (
                <div className={value ? "mt-2" : "mt-1"}>{children}</div>
            ) : null}
        </div>
    )
}

export default function MediaDetailClient({
    detail,
    onBackHref = "/library",
}: {
    detail: MediaItemDetailResponse
    onBackHref?: string
}) {
    const [pending, startTransition] = useTransition()

    const allowedStatuses = useMemo(
        () => statusesForType(detail.type),
        [detail.type],
    )

    const [status, setStatus] = useState<MediaStatus | null>(
        detail.latestStatus ?? null,
    )
    const [rating, setRating] = useState<number | null>(
        detail.latestRating ?? null,
    )
    const [notes, setNotes] = useState(detail.latestNotes ?? "")
    const [saveState, setSaveState] = useState<
        "idle" | "saving" | "saved" | "error"
    >("idle")

    const notesDirty = notes !== (detail.latestNotes ?? "")

    const subtitle = [
        detail.year ? String(detail.year) : null,
        detail.creator ?? null,
    ]
        .filter(Boolean)
        .join(" • ")

    const tagsSorted = useMemo(() => {
        const tags = detail.tags ?? []
        return tags.slice().sort((a, b) => {
            const dw = (b.weight ?? 0) - (a.weight ?? 0)
            if (dw !== 0) return dw
            const dc = String(a.category).localeCompare(String(b.category))
            if (dc !== 0) return dc
            return String(a.name).localeCompare(String(b.name))
        })
    }, [detail.tags])

    const tagsByCategory = useMemo(() => {
        const map = new Map<string, typeof tagsSorted>()
        for (const t of tagsSorted) {
            const key = String(t.category)
            const arr = map.get(key) ?? []
            arr.push(t)
            map.set(key, arr)
        }
        return map
    }, [tagsSorted])

    function handleSetStatus(next: MediaStatus) {
        setStatus(next)
        startTransition(async () => {
            try {
                await apiPatchNoContent(`/api/media/${detail.id}/status`, {
                    status: next,
                })
            } catch (e) {
                console.error(e)
                setStatus(detail.latestStatus ?? null)
            }
        })
    }

    function handleSetRating(next: number) {
        setRating(next)
        startTransition(async () => {
            try {
                await apiPatchNoContent(`/api/media/${detail.id}/rating`, {
                    rating: next,
                })
            } catch (e) {
                console.error(e)
                setRating(detail.latestRating ?? null)
            }
        })
    }

    async function saveNotes() {
        if (!notesDirty || saveState === "saving") return
        setSaveState("saving")
        try {
            await apiPatchNoContent(`/api/media/${detail.id}/notes`, { notes })
            setSaveState("saved")
            window.setTimeout(() => setSaveState("idle"), 1100)
        } catch (e) {
            console.error(e)
            setSaveState("error")
            window.setTimeout(() => setSaveState("idle"), 1600)
        }
    }

    return (
        <div className="min-h-screen bg-gradient-to-b from-black/[0.03] to-background">
            {/* Sticky header */}
            <div className="sticky top-0 z-30 border-b bg-background/85 backdrop-blur">
                <div className="mx-auto max-w-6xl px-6 py-3 flex items-center justify-between gap-4">
                    <div className="min-w-0">
                        <div className="text-[11px] font-semibold tracking-wide uppercase text-muted-foreground">
                            {detail.type}
                        </div>
                        <div className="text-lg font-semibold leading-tight truncate">
                            {detail.title}
                        </div>
                        <div className="text-sm text-muted-foreground truncate">
                            {subtitle}
                        </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                        <Button variant="outline" asChild>
                            <a href={onBackHref}>Back</a>
                        </Button>
                        <Button
                            variant="outline"
                            type="button"
                            onClick={() => window.location.reload()}
                        >
                            Refresh
                        </Button>
                    </div>
                </div>
            </div>

            <div className="mx-auto max-w-6xl px-6 py-6 grid gap-6 lg:grid-cols-[320px_1fr]">
                {/* Left rail */}
                <div className="space-y-4">
                    <Card className="overflow-hidden p-0">
                        <CardContent className="p-0">
                            <div className="aspect-[2/3] bg-black/5">
                                {/* eslint-disable-next-line @next/next/no-img-element */}
                                {detail.posterUrl ? (
                                    <img
                                        src={detail.posterUrl}
                                        alt={detail.title}
                                        className="w-full h-full object-cover"
                                    />
                                ) : (
                                    <div className="w-full h-full grid place-items-center text-xs text-muted-foreground">
                                        No poster
                                    </div>
                                )}
                            </div>
                        </CardContent>
                    </Card>

                    <Card className="p-4">
                        <CardHeader className="p-0 pb-0">
                            <CardTitle className="text-base">
                                Status & Rating
                            </CardTitle>
                        </CardHeader>

                        <CardContent className="p-0 space-y-4">
                            <div className="space-y-2">
                                <div className="text-[11px] font-semibold tracking-wide uppercase text-muted-foreground">
                                    Status
                                </div>
                                <div className="flex flex-wrap gap-2">
                                    {allowedStatuses.map((s) => {
                                        const active = status === s
                                        return (
                                            <Button
                                                key={s}
                                                type="button"
                                                size="sm"
                                                variant={
                                                    active
                                                        ? "default"
                                                        : "outline"
                                                }
                                                disabled={pending}
                                                onClick={() =>
                                                    handleSetStatus(s)
                                                }
                                                className="rounded-full h-8 px-3"
                                            >
                                                {statusLabel(s)}
                                            </Button>
                                        )
                                    })}
                                </div>
                            </div>

                            <Separator />

                            <div className="space-y-2">
                                <div className="text-[11px] font-semibold tracking-wide uppercase text-muted-foreground">
                                    Rating
                                </div>
                                <div className="flex items-center gap-1.5">
                                    {[1, 2, 3, 4, 5].map((n) => {
                                        const filled = (rating ?? 0) >= n
                                        return (
                                            <Button
                                                key={n}
                                                type="button"
                                                size="sm"
                                                variant={
                                                    filled
                                                        ? "default"
                                                        : "outline"
                                                }
                                                disabled={pending}
                                                onClick={() =>
                                                    handleSetRating(n)
                                                }
                                                className="h-9 w-9 px-0"
                                                title={`Rate ${n}`}
                                            >
                                                ★
                                            </Button>
                                        )
                                    })}
                                </div>
                            </div>

                            <Separator />

                            <div className="grid grid-cols-2 gap-2">
                                <StatTile
                                    label="Added"
                                    value={fmtDateOnly(detail.createdAt)}
                                />
                                <StatTile
                                    label="Updated"
                                    value={fmtDateOnly(detail.updatedAt)}
                                />
                                <StatTile
                                    label="Last watch"
                                    value={
                                        detail.latestWatchedAt
                                            ? fmtLocalDateOnly(
                                                  detail.latestWatchedAt,
                                              )
                                            : "—"
                                    }
                                />
                                <StatTile
                                    label="Model tags"
                                    value={String(detail.tags?.length ?? 0)}
                                />
                            </div>
                        </CardContent>
                    </Card>
                </div>

                {/* Main */}
                <div className="space-y-4">
                    {detail.description ? (
                        <Card className="p-4">
                            <CardHeader className="p-0 pb-0">
                                <CardTitle className="text-base">
                                    Description
                                </CardTitle>
                            </CardHeader>
                            <CardContent className="p-0">
                                <p className="text-sm leading-relaxed text-foreground/90">
                                    {detail.description}
                                </p>
                            </CardContent>
                        </Card>
                    ) : null}

                    <Card className="p-4">
                        <CardHeader className="p-0 pb-2 flex flex-row items-center justify-between">
                            <CardTitle className="text-base">Notes</CardTitle>
                            <Button
                                type="button"
                                variant="outline"
                                onClick={saveNotes}
                                disabled={!notesDirty || saveState === "saving"}
                                className={cx(
                                    saveState === "saved" &&
                                        "border-foreground",
                                )}
                            >
                                {saveState === "saving"
                                    ? "Saving…"
                                    : saveState === "saved"
                                      ? "Saved"
                                      : saveState === "error"
                                        ? "Error"
                                        : "Save"}
                            </Button>
                        </CardHeader>
                        <CardContent className="p-0">
                            <Textarea
                                value={notes}
                                onChange={(e) => setNotes(e.target.value)}
                                placeholder="Write notes…"
                                className="min-h-[140px]"
                            />
                            <div className="mt-2 text-[11px] text-muted-foreground">
                                Notes are versioned in history on the backend.
                            </div>
                        </CardContent>
                    </Card>

                    {(detail.movieMetadata || detail.bookMetadata) && (
                        <Card className="p-4">
                            <CardHeader className="p-0 pb-0">
                                <CardTitle className="text-base">
                                    Metadata
                                </CardTitle>
                            </CardHeader>
                            <CardContent className="p-0">
                                <div className="grid md:grid-cols-2 gap-3">
                                    {detail.movieMetadata?.runtimeMinutes ? (
                                        <MetaTile
                                            label="Runtime"
                                            value={formatRuntime(
                                                detail.movieMetadata
                                                    .runtimeMinutes,
                                            )}
                                        />
                                    ) : null}

                                    {detail.movieMetadata?.genres?.length ? (
                                        <MetaTile label="Genres">
                                            <div className="flex flex-wrap gap-2">
                                                {detail.movieMetadata.genres.map(
                                                    (g) => (
                                                        <Badge
                                                            key={g}
                                                            variant="secondary"
                                                            className="rounded-full px-2.5 py-1 text-xs font-medium"
                                                        >
                                                            {g}
                                                        </Badge>
                                                    ),
                                                )}
                                            </div>
                                        </MetaTile>
                                    ) : null}

                                    {detail.bookMetadata?.pageCount ? (
                                        <MetaTile
                                            label="Pages"
                                            value={`${detail.bookMetadata.pageCount}`}
                                        />
                                    ) : null}

                                    {detail.bookMetadata?.publisher ? (
                                        <MetaTile
                                            label="Publisher"
                                            value={
                                                detail.bookMetadata.publisher
                                            }
                                        />
                                    ) : null}

                                    {detail.bookMetadata?.isbn ? (
                                        <MetaTile
                                            label="ISBN"
                                            value={detail.bookMetadata.isbn}
                                        />
                                    ) : null}
                                </div>
                            </CardContent>
                        </Card>
                    )}

                    {!!tagsSorted.length && (
                        <Card className="p-4">
                            <CardHeader className="p-0 pb-0 flex flex-row items-center justify-between">
                                <CardTitle className="text-base">
                                    Tags
                                </CardTitle>
                                <div className="text-xs text-muted-foreground">
                                    {tagsSorted.length} total
                                </div>
                            </CardHeader>
                            <CardContent className="p-0 space-y-4">
                                {Array.from(tagsByCategory.entries()).map(
                                    ([cat, tags]) => (
                                        <div key={cat} className="space-y-2">
                                            <div className="text-[11px] font-semibold tracking-wide uppercase text-muted-foreground">
                                                {cat}
                                            </div>
                                            <div className="flex flex-wrap gap-2 pb-1">
                                                {tags.map((t, idx) => (
                                                    <Badge
                                                        key={`${t.category}-${t.name}-${idx}`}
                                                        variant="outline"
                                                        className="rounded-full px-2.5 py-1 text-xs font-medium bg-background"
                                                        title={`${t.category} • ${t.weight.toFixed(2)}`}
                                                    >
                                                        <span className="opacity-90">
                                                            {t.name}
                                                        </span>
                                                        <span className="ml-2 opacity-60 tabular-nums">
                                                            {t.weight.toFixed(
                                                                2,
                                                            )}
                                                        </span>
                                                    </Badge>
                                                ))}
                                            </div>
                                        </div>
                                    ),
                                )}
                            </CardContent>
                        </Card>
                    )}
                </div>
            </div>
        </div>
    )
}
