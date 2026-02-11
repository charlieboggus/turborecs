"use client"

import { Star } from "lucide-react"
import { cn } from "@/lib/utils"

interface StarRatingProps {
    rating: number | null
    onRate?: (rating: number) => void
    size?: "sm" | "md"
}

export function StarRating({ rating, onRate, size = "md" }: StarRatingProps) {
    const starSize = size === "sm" ? "h-3.5 w-3.5" : "h-5 w-5"

    return (
        <div className="flex gap-0.5">
            {[1, 2, 3, 4, 5].map((star) => (
                <button
                    key={star}
                    type="button"
                    disabled={!onRate}
                    onClick={() => onRate?.(star)}
                    className={cn(
                        "transition-colors",
                        onRate
                            ? "cursor-pointer hover:text-yellow-400"
                            : "cursor-default",
                    )}
                >
                    <Star
                        className={cn(
                            starSize,
                            star <= (rating ?? 0)
                                ? "fill-yellow-400 text-yellow-400"
                                : "text-muted-foreground/30",
                        )}
                    />
                </button>
            ))}
        </div>
    )
}
