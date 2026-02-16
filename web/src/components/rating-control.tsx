"use client"

import { useState, useTransition } from "react"
import { Star, Loader2 } from "lucide-react"
import { rateMediaAction } from "@/lib/actions"

export function RatingControl({
  mediaId,
  initialRating,
}: {
  mediaId: string
  initialRating: number | null
}) {
  const [rating, setRating] = useState(initialRating ?? 0)
  const [hover, setHover] = useState(0)
  const [isPending, startTransition] = useTransition()

  function handleClick(star: number) {
    const newRating = star === rating ? 0 : star
    setRating(newRating)
    if (newRating > 0) {
      startTransition(async () => {
        await rateMediaAction(mediaId, newRating)
      })
    }
  }

  return (
    <div className="flex items-center gap-1">
      {Array.from({ length: 5 }, (_, i) => {
        const star = i + 1
        const filled = star <= (hover || rating)
        return (
          <button
            key={i}
            onClick={() => handleClick(star)}
            onMouseEnter={() => setHover(star)}
            onMouseLeave={() => setHover(0)}
            disabled={isPending}
            className="p-0.5 transition-transform hover:scale-110 disabled:opacity-50"
          >
            <Star
              className={`h-6 w-6 transition-colors ${
                filled
                  ? "fill-amber-500 text-amber-500"
                  : "text-muted-foreground/25"
              }`}
            />
          </button>
        )
      })}
      {isPending && (
        <Loader2 className="h-4 w-4 animate-spin ml-1 text-muted-foreground" />
      )}
    </div>
  )
}
