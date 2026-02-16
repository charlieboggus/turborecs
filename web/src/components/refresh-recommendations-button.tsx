"use client"

import { useTransition } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { RefreshCw, Loader2 } from "lucide-react"
import { refreshRecommendationsAction } from "@/lib/actions"
import type { MediaType } from "@/lib/types"

export function RefreshRecommendationsButton({
  mediaType,
}: {
  mediaType?: MediaType
}) {
  const [isPending, startTransition] = useTransition()
  const router = useRouter()

  function handleRefresh() {
    startTransition(async () => {
      await refreshRecommendationsAction(mediaType)
      router.refresh()
    })
  }

  return (
    <Button
      variant="outline"
      size="sm"
      onClick={handleRefresh}
      disabled={isPending}
    >
      {isPending ? (
        <Loader2 className="h-4 w-4 animate-spin mr-1.5" />
      ) : (
        <RefreshCw className="h-4 w-4 mr-1.5" />
      )}
      {isPending ? "Generating..." : "New Recommendations"}
    </Button>
  )
}
