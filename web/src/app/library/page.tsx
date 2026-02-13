import Link from "next/link"
import { apiGet } from "@/lib/api"
import type {
    MediaItemResponse,
    PageResponse,
    MediaStatus,
    MediaType,
} from "@/lib/apiTypes"

type Props = {
    searchParams: Promise<{
        page?: string
        size?: string
        type?: MediaType
        status?: MediaStatus
    }>
}

export default async function LibraryPage({ searchParams }: Props) {
    const sp = await searchParams
    const page = Number(sp.page ?? "0")
    const size = Number(sp.size ?? "50")
    const type = sp.type
    const status = sp.status

    const qs = new URLSearchParams()
    qs.set("page", String(page))
    qs.set("size", String(size))
    if (type) qs.set("type", type)
    if (status) qs.set("status", status)

    const data = await apiGet<PageResponse<MediaItemResponse>>(
        `/api/media?${qs.toString()}`,
    )

    return (
        <div className="p-6 space-y-5">
            <div className="sticky top-0 z-20 -mx-6 px-6 pt-4 pb-3 border-b bg-white/85 backdrop-blur dark:bg-black/60">
                <div className="flex items-end justify-between gap-4">
                    <div>
                        <h1 className="text-2xl font-semibold tracking-tight">
                            Library
                        </h1>
                        <div className="text-sm opacity-70">
                            Page {data.page + 1} / {data.totalPages} •{" "}
                            {data.totalItems} items
                        </div>
                    </div>

                    <div className="flex gap-2">
                        <PagerLink
                            page={Math.max(0, page - 1)}
                            size={size}
                            type={type}
                            status={status}
                            disabled={page <= 0}
                        >
                            Prev
                        </PagerLink>
                        <PagerLink
                            page={Math.min(data.totalPages - 1, page + 1)}
                            size={size}
                            type={type}
                            status={status}
                            disabled={page + 1 >= data.totalPages}
                        >
                            Next
                        </PagerLink>
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-5">
                {data.items.map((m) => (
                    <Link
                        key={m.id}
                        href={`/media/${m.id}`}
                        className={[
                            "group rounded-2xl border bg-white/50 dark:bg-white/5",
                            "overflow-hidden transition-all",
                            "hover:-translate-y-0.5 hover:shadow-md hover:border-black/20 dark:hover:border-white/20",
                            "focus:outline-none focus-visible:ring-2 focus-visible:ring-black/20 dark:focus-visible:ring-white/30",
                        ].join(" ")}
                    >
                        <div className="relative aspect-[2/3] bg-black/5 overflow-hidden">
                            {m.posterUrl ? (
                                <img
                                    src={m.posterUrl}
                                    alt={m.title}
                                    className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-[1.04]"
                                    loading="lazy"
                                />
                            ) : (
                                <div className="h-full w-full flex items-center justify-center text-xs opacity-60">
                                    No poster
                                </div>
                            )}

                            {/* subtle hover sheen */}
                            <div className="pointer-events-none absolute inset-0 opacity-0 transition-opacity duration-300 group-hover:opacity-100">
                                <div className="absolute inset-0 bg-gradient-to-t from-black/25 via-transparent to-transparent" />
                            </div>

                            {/* top-left type chip */}
                            <div className="absolute left-2 top-2">
                                <span className="rounded-full bg-black/70 text-white text-[11px] px-2 py-1">
                                    {m.type}
                                </span>
                            </div>
                        </div>

                        <div className="p-3 space-y-1">
                            <div className="text-sm font-semibold leading-snug line-clamp-2">
                                {m.title}
                            </div>

                            <div className="text-xs opacity-70 flex items-center gap-2">
                                <span>{m.year ?? "—"}</span>
                                {m.creator ? (
                                    <span className="line-clamp-1">
                                        • {m.creator}
                                    </span>
                                ) : null}
                            </div>

                            <div className="pt-1 flex flex-wrap items-center gap-2">
                                <MetaChip
                                    label={m.latestStatus ?? "No status"}
                                />
                                {m.latestRating ? (
                                    <MetaChip label={`★ ${m.latestRating}`} />
                                ) : null}
                            </div>
                        </div>
                    </Link>
                ))}
            </div>
        </div>
    )
}

function MetaChip({ label }: { label: string }) {
    return (
        <span className="text-[11px] rounded-full border bg-black/5 px-2 py-1 opacity-80">
            {label}
        </span>
    )
}

function PagerLink(props: {
    page: number
    size: number
    type?: MediaType
    status?: MediaStatus
    disabled?: boolean
    children: React.ReactNode
}) {
    const qs = new URLSearchParams()
    qs.set("page", String(props.page))
    qs.set("size", String(props.size))
    if (props.type) qs.set("type", props.type)
    if (props.status) qs.set("status", props.status)

    if (props.disabled) {
        return (
            <span className="px-3 py-2 rounded-lg border opacity-40">
                {props.children}
            </span>
        )
    }
    return (
        <Link
            href={`/library?${qs.toString()}`}
            className="px-3 py-2 rounded-lg border hover:bg-black/5"
        >
            {props.children}
        </Link>
    )
}
