"use client"

import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { StarRating } from "@/components/star-rating"
import type { MediaItem } from "@/lib/types"
import { Film, BookOpen } from "lucide-react"
import Image from "next/image"

interface MediaCardProps {
    item: MediaItem
    onClick: (item: MediaItem) => void
}

const statusLabels: Record<string, string> = {
    WANT_TO_CONSUME: "Watchlist",
    IN_PROGRESS: "In Progress",
    FINISHED: "Finished",
    DNF: "Did Not Finish",
}

export function MediaCard({ item, onClick }: MediaCardProps) {
    const Icon = item.type === "MOVIE" ? Film : BookOpen

    return (
        <Card
            className="cursor-pointer transition-colors hover:bg-accent/50"
            onClick={() => onClick(item)}
        >
            <CardContent className="p-4">
                <div className="mb-3 aspect-[2/3] overflow-hidden rounded-md bg-muted">
                    {item.posterUrl ? (
                        <img
                            src={item.posterUrl}
                            alt={item.title}
                            className="h-full w-full object-cover"
                        />
                    ) : (
                        <div className="flex h-full items-center justify-center">
                            <Icon className="h-12 w-12 text-muted-foreground/40" />
                        </div>
                    )}
                </div>

                <div className="space-y-1.5">
                    <h3 className="line-clamp-2 text-sm font-medium leading-tight">
                        {item.title}
                    </h3>

                    {item.creator && (
                        <p className="text-xs text-muted-foreground truncate">
                            {item.creator}
                            {item.year ? ` · ${item.year}` : ""}
                        </p>
                    )}

                    <div className="flex items-center justify-between">
                        <StarRating rating={item.rating} size="sm" />
                        <Badge variant="secondary" className="text-xs">
                            {item.type === "MOVIE" ? "Film" : "Book"}
                        </Badge>
                    </div>

                    <p className="text-xs text-muted-foreground">
                        {statusLabels[item.status]}
                    </p>
                </div>
            </CardContent>
        </Card>
    )
}
