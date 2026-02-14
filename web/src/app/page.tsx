import Link from "next/link"
import { apiGet } from "@/lib/api"
import type { MediaItemResponse, PageResponse } from "@/lib/apiTypes"

import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from "@/components/ui/card"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"

import {
    ArrowRight,
    LibraryBig,
    Sparkles,
    Wand2,
    Plus,
    RefreshCw,
    Film,
    BookOpen,
    Activity,
    Tags,
    Database,
} from "lucide-react"

type AdminStatsResponse = {
    totalItems: number
    itemsByType: Record<string, number>
    latestStatusCounts: Record<string, number>
    taggedItemsForModelVersion: number
    movieMetadataCount: number
    bookMetadataCount: number
}

async function getSpotlight(): Promise<MediaItemResponse[]> {
    const qs = new URLSearchParams({ page: "0", size: "3" })
    const res = await apiGet<PageResponse<MediaItemResponse>>(
        `/api/media?${qs.toString()}`,
    )
    return res.items ?? []
}

async function getAdminStats(): Promise<AdminStatsResponse | null> {
    // admin endpoint; your proxy route should attach X-Admin-Auth
    return apiGet<AdminStatsResponse>("/api/admin/stats").catch(() => null)
}

function MiniTile() {
    return (
        <div className="group rounded-xl border bg-background p-3 shadow-sm transition hover:shadow-md">
            <div className="flex items-start justify-between gap-2">
                <div className="h-2.5 w-2.5 rounded-full bg-black/20 mt-1" />
                <div className="h-7 w-7 rounded-lg border bg-black/[0.03] grid place-items-center">
                    <RefreshCw className="h-3.5 w-3.5 text-muted-foreground group-hover:text-foreground/70" />
                </div>
            </div>
            <div className="mt-2 space-y-1">
                <div className="h-3 w-[80%] rounded bg-black/[0.06]" />
                <div className="h-3 w-[60%] rounded bg-black/[0.05]" />
            </div>
        </div>
    )
}

function EmptyStrip() {
    return (
        <div className="flex items-center justify-between gap-4 rounded-xl border bg-background p-4">
            <div className="space-y-1">
                <div className="text-sm font-medium">No items yet</div>
                <div className="text-xs text-muted-foreground">
                    Log a movie or book and your spotlight strip will show up
                    here.
                </div>
            </div>
            <div className="flex gap-2">
                <Button asChild variant="secondary" size="sm" className="gap-2">
                    <Link href="/library?log=movie">
                        <Plus className="h-4 w-4" /> Movie
                    </Link>
                </Button>
                <Button asChild variant="secondary" size="sm" className="gap-2">
                    <Link href="/library?log=book">
                        <Plus className="h-4 w-4" /> Book
                    </Link>
                </Button>
            </div>
        </div>
    )
}

function RowStat(props: {
    label: string
    value: string
    icon?: React.ReactNode
}) {
    return (
        <div className="flex items-center justify-between rounded-xl border bg-black/[0.02] px-3 py-2">
            <div className="text-xs font-semibold tracking-wide uppercase text-muted-foreground flex items-center gap-2">
                {props.icon ? (
                    <span className="opacity-70">{props.icon}</span>
                ) : null}
                {props.label}
            </div>
            <div className="text-sm font-semibold tabular-nums">
                {props.value}
            </div>
        </div>
    )
}

function MiniStat(props: { label: string; value: string }) {
    return (
        <div className="rounded-xl border bg-black/[0.02] p-3">
            <div className="text-[10px] font-semibold tracking-wide uppercase text-muted-foreground">
                {props.label}
            </div>
            <div className="mt-1 text-sm font-semibold tabular-nums">
                {props.value}
            </div>
        </div>
    )
}

function fmtInt(n: number | undefined | null) {
    const v = typeof n === "number" ? n : 0
    return new Intl.NumberFormat().format(v)
}

function pick(map: Record<string, number> | undefined | null, key: string) {
    if (!map) return 0
    return map[key] ?? 0
}

function pct(n: number, d: number) {
    if (!d) return "0%"
    const p = Math.round((n / d) * 100)
    return `${Math.min(100, Math.max(0, p))}%`
}

function StatsSkeletonCard(props: {
    title: string
    icon: React.ReactNode
    desc: string
}) {
    return (
        <Card className="overflow-hidden">
            <CardHeader className="space-y-1.5">
                <CardTitle className="flex items-center gap-2">
                    <span className="opacity-80">{props.icon}</span>
                    {props.title}
                </CardTitle>
                <CardDescription>{props.desc}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
                <Skeleton className="h-9 w-full" />
                <Skeleton className="h-9 w-full" />
                <Skeleton className="h-9 w-full" />
            </CardContent>
        </Card>
    )
}

export default async function HomePage() {
    const spotlight = await getSpotlight().catch(() => [])
    const stats = await getAdminStats()

    const total = stats?.totalItems ?? 0
    const movies = pick(stats?.itemsByType, "MOVIE")
    const books = pick(stats?.itemsByType, "BOOK")

    const tagged = stats?.taggedItemsForModelVersion ?? 0
    const movieMeta = stats?.movieMetadataCount ?? 0
    const bookMeta = stats?.bookMetadataCount ?? 0
    const enriched = movieMeta + bookMeta

    return (
        <main className="min-h-screen">
            <div className="pointer-events-none fixed inset-0 -z-10">
                <div className="absolute inset-0 bg-gradient-to-b from-background via-background to-background" />
                <div className="absolute left-1/2 top-[-220px] h-[520px] w-[920px] -translate-x-1/2 rounded-full bg-black/[0.06] blur-3xl" />
                <div className="absolute right-[10%] top-[240px] h-[320px] w-[320px] rounded-full bg-black/[0.04] blur-3xl" />
                <div className="absolute left-[12%] top-[360px] h-[280px] w-[280px] rounded-full bg-black/[0.04] blur-3xl" />
            </div>

            <div className="mx-auto max-w-6xl px-6 py-12 space-y-10">
                <header className="space-y-5">
                    <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
                        <div className="space-y-3">
                            <div className="flex items-center gap-3">
                                <div className="grid h-11 w-11 place-items-center rounded-2xl border bg-background shadow-sm">
                                    <Wand2 className="h-5 w-5" />
                                </div>
                                <div>
                                    <h1 className="text-4xl font-semibold tracking-tight leading-none">
                                        Turborecs
                                    </h1>
                                </div>
                            </div>
                        </div>

                        <div className="flex flex-col sm:flex-row gap-2">
                            <Button asChild size="lg" className="gap-2">
                                <Link href="/library">
                                    Open Library{" "}
                                    <ArrowRight className="h-4 w-4" />
                                </Link>
                            </Button>
                            <Button
                                asChild
                                size="lg"
                                variant="secondary"
                                className="gap-2"
                            >
                                <Link href="/recommendations">
                                    Get Recs <Sparkles className="h-4 w-4" />
                                </Link>
                            </Button>
                        </div>
                    </div>
                </header>

                <Separator />

                <section className="grid gap-6 lg:grid-cols-3">
                    <Card className="lg:col-span-2 overflow-hidden">
                        <CardHeader className="space-y-1.5">
                            <CardTitle className="flex items-center gap-2">
                                <LibraryBig className="h-5 w-5" />
                                Library
                            </CardTitle>
                            <CardDescription>
                                A quick peek at what's in your orbit right now.
                            </CardDescription>
                        </CardHeader>

                        <CardContent className="space-y-5">
                            <div className="rounded-2xl border bg-black/[0.02] p-4">
                                <div className="flex items-center justify-between gap-3">
                                    <div className="text-xs font-semibold tracking-wide uppercase text-muted-foreground">
                                        Spotlight
                                    </div>
                                </div>
                                <div className="mt-3">
                                    {spotlight.length === 0 ? (
                                        <EmptyStrip />
                                    ) : (
                                        <div className="flex gap-8 justify-center overflow-x-auto pb-2 pr-1 snap-x snap-mandatory">
                                            {spotlight.map((m) => (
                                                <Link
                                                    key={m.id}
                                                    href={`/media/${m.id}`}
                                                    className="group snap-start shrink-0 w-[160px]"
                                                    title={m.title}
                                                >
                                                    <div className="rounded-xl overflow-hidden border bg-background shadow-sm">
                                                        <div className="aspect-[2/3] bg-black/5 overflow-hidden">
                                                            {m.posterUrl ? (
                                                                <img
                                                                    src={
                                                                        m.posterUrl
                                                                    }
                                                                    alt={
                                                                        m.title
                                                                    }
                                                                    className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-[1.04]"
                                                                />
                                                            ) : (
                                                                <div className="h-full w-full grid place-items-center text-xs text-muted-foreground">
                                                                    No poster
                                                                </div>
                                                            )}
                                                        </div>
                                                        <div className="p-2">
                                                            <div className="text-sm font-medium leading-snug line-clamp-2">
                                                                {m.title}
                                                            </div>
                                                            <div className="mt-1 text-xs text-muted-foreground">
                                                                {m.type}
                                                                {m.year
                                                                    ? ` • ${m.year}`
                                                                    : ""}
                                                            </div>
                                                        </div>
                                                    </div>
                                                </Link>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            </div>
                            <div className="flex flex-wrap justify-center gap-2">
                                <Button asChild className="gap-2">
                                    <Link href="/library">
                                        Open Library{" "}
                                        <ArrowRight className="h-4 w-4" />
                                    </Link>
                                </Button>
                                <Button
                                    asChild
                                    variant="secondary"
                                    className="gap-2"
                                >
                                    <Link href="/library?log=movie">
                                        <Plus className="h-4 w-4" /> Log Movie
                                    </Link>
                                </Button>
                                <Button
                                    asChild
                                    variant="secondary"
                                    className="gap-2"
                                >
                                    <Link href="/library?log=book">
                                        <Plus className="h-4 w-4" /> Log Book
                                    </Link>
                                </Button>
                            </div>
                        </CardContent>
                    </Card>

                    <Card className="overflow-hidden">
                        <CardHeader className="space-y-1.5">
                            <CardTitle className="flex items-center gap-2">
                                <Sparkles className="h-5 w-5" />
                                Recommendations
                            </CardTitle>
                            <CardDescription>
                                Generate a grid and “swap” tiles until it feels
                                right.
                            </CardDescription>
                        </CardHeader>

                        <CardContent className="space-y-5">
                            <div className="rounded-2xl border bg-black/[0.02] p-4">
                                <div className="flex items-center justify-between gap-3">
                                    <div className="text-xs font-semibold tracking-wide uppercase text-muted-foreground">
                                        Grid mode
                                    </div>
                                    <Badge
                                        variant="secondary"
                                        className="gap-1"
                                    >
                                        <RefreshCw className="h-3.5 w-3.5" />
                                        tile refresh
                                    </Badge>
                                </div>

                                <div className="mt-3 grid grid-cols-3 gap-2">
                                    <MiniTile />
                                    <MiniTile />
                                    <MiniTile />
                                    <MiniTile />
                                    <MiniTile />
                                    <MiniTile />
                                </div>
                            </div>

                            <div className="grid gap-2">
                                <Button
                                    asChild
                                    size="lg"
                                    className="w-full gap-2"
                                >
                                    <Link href="/recommendations">
                                        Open Recommendations{" "}
                                        <ArrowRight className="h-4 w-4" />
                                    </Link>
                                </Button>
                                <Button
                                    asChild
                                    variant="secondary"
                                    className="w-full gap-2"
                                >
                                    <Link href="/recommendations?autostart=true">
                                        Generate a new grid{" "}
                                        <Sparkles className="h-4 w-4" />
                                    </Link>
                                </Button>
                            </div>
                        </CardContent>
                    </Card>
                </section>
                <section className="grid gap-6 md:grid-cols-3">
                    {!stats ? (
                        <>
                            <StatsSkeletonCard
                                title="Library"
                                icon={<LibraryBig className="h-5 w-5" />}
                                desc="Couldn't load /api/admin/stats"
                            />
                            <StatsSkeletonCard
                                title="Status"
                                icon={<Activity className="h-5 w-5" />}
                                desc="Couldn't load /api/admin/stats"
                            />
                            <StatsSkeletonCard
                                title="Enrichment"
                                icon={<Database className="h-5 w-5" />}
                                desc="Couldn't load /api/admin/stats"
                            />
                        </>
                    ) : (
                        <>
                            <Card className="overflow-hidden">
                                <CardHeader className="space-y-1.5">
                                    <CardTitle className="flex items-center gap-2">
                                        <LibraryBig className="h-5 w-5" />
                                        Library Summary
                                    </CardTitle>
                                    <CardDescription>
                                        What you've logged so far
                                    </CardDescription>
                                </CardHeader>
                                <CardContent className="space-y-3">
                                    <RowStat
                                        label="Movies"
                                        value={fmtInt(movies)}
                                        icon={<Film className="h-4 w-4" />}
                                    />
                                    <RowStat
                                        label="Books"
                                        value={fmtInt(books)}
                                        icon={<BookOpen className="h-4 w-4" />}
                                    />
                                    <RowStat
                                        label="Total"
                                        value={fmtInt(total)}
                                    />
                                </CardContent>
                            </Card>

                            <Card className="overflow-hidden">
                                <CardHeader className="space-y-1.5">
                                    <CardTitle className="flex items-center gap-2">
                                        <Activity className="h-5 w-5" />
                                        Status
                                    </CardTitle>
                                    <CardDescription>
                                        Latest status distribution
                                    </CardDescription>
                                </CardHeader>
                                <CardContent className="space-y-4">
                                    <div className="grid grid-cols-2 gap-2">
                                        <MiniStat
                                            label="Watched"
                                            value={fmtInt(
                                                pick(
                                                    stats.latestStatusCounts,
                                                    "WATCHED",
                                                ),
                                            )}
                                        />
                                        <MiniStat
                                            label="Finished"
                                            value={fmtInt(
                                                pick(
                                                    stats.latestStatusCounts,
                                                    "FINISHED",
                                                ),
                                            )}
                                        />
                                        <MiniStat
                                            label="Reading"
                                            value={fmtInt(
                                                pick(
                                                    stats.latestStatusCounts,
                                                    "READING",
                                                ),
                                            )}
                                        />
                                        <MiniStat
                                            label="Watching"
                                            value={fmtInt(
                                                pick(
                                                    stats.latestStatusCounts,
                                                    "WATCHING",
                                                ),
                                            )}
                                        />
                                        <MiniStat
                                            label="Want (Watch)"
                                            value={fmtInt(
                                                pick(
                                                    stats.latestStatusCounts,
                                                    "WANT_TO_WATCH",
                                                ),
                                            )}
                                        />
                                        <MiniStat
                                            label="Want (Read)"
                                            value={fmtInt(
                                                pick(
                                                    stats.latestStatusCounts,
                                                    "WANT_TO_READ",
                                                ),
                                            )}
                                        />
                                    </div>

                                    <div className="rounded-xl border bg-black/[0.02] p-3 flex items-center justify-between">
                                        <div className="text-xs font-semibold tracking-wide uppercase text-muted-foreground flex items-center gap-2">
                                            <Tags className="h-4 w-4 opacity-70" />
                                            Tagged for model
                                        </div>
                                        <div className="text-sm font-semibold tabular-nums">
                                            {fmtInt(tagged)}
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>

                            <Card className="overflow-hidden">
                                <CardHeader className="space-y-1.5">
                                    <CardTitle className="flex items-center gap-2">
                                        <Database className="h-5 w-5" />
                                        Enrichment
                                    </CardTitle>
                                    <CardDescription>
                                        Metadata coverage
                                    </CardDescription>
                                </CardHeader>
                                <CardContent className="space-y-3">
                                    <RowStat
                                        label="Movie metadata"
                                        value={fmtInt(movieMeta)}
                                    />
                                    <RowStat
                                        label="Book metadata"
                                        value={fmtInt(bookMeta)}
                                    />
                                    <RowStat
                                        label="Coverage"
                                        value={pct(enriched, total)}
                                    />

                                    <div className="pt-1">
                                        <div className="h-2 w-full rounded-full bg-black/[0.06] overflow-hidden">
                                            <div
                                                className="h-full bg-black/60"
                                                style={{
                                                    width: `${total ? Math.min(100, Math.round((enriched / total) * 100)) : 0}%`,
                                                }}
                                            />
                                        </div>
                                        <div className="mt-2 text-xs text-muted-foreground">
                                            {fmtInt(enriched)} / {fmtInt(total)}{" "}
                                            items enriched
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>
                        </>
                    )}
                </section>
            </div>
        </main>
    )
}
