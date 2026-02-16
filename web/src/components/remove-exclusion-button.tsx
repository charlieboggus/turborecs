// components/remove-exclusion-button.tsx
"use client"

import { useTransition } from "react"
import { Button } from "@/components/ui/button"
import { X, Loader2 } from "lucide-react"
import { removeExclusionAction } from "@/lib/actions"

export function RemoveExclusionButton({ id }: { id: string }) {
  const [isPending, startTransition] = useTransition()

  function handleRemove() {
    startTransition(async () => {
      await removeExclusionAction(id)
    })
  }

  return (
    <Button
      variant="ghost"
      size="icon"
      onClick={handleRemove}
      disabled={isPending}
      className="h-7 w-7 text-muted-foreground hover:text-destructive shrink-0"
    >
      {isPending ? (
        <Loader2 className="h-3.5 w-3.5 animate-spin" />
      ) : (
        <X className="h-3.5 w-3.5" />
      )}
    </Button>
  )
}
