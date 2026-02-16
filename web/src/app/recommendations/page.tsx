import { Nav } from "@/components/nav"
import { RecommendationsContent } from "@/components/recommendations-content"
import { Separator } from "@/components/ui/separator"

export default function RecommendationsPage() {
  return (
    <main className="min-h-screen bg-background">
      <div className="mx-auto max-w-5xl px-6 py-10">
        <Nav current="/recommendations" />

        <div>
          <h1 className="text-3xl font-bold tracking-tight">Recommendations</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            AI-powered picks based on your taste profile.
          </p>
        </div>

        <Separator className="my-6" />

        <RecommendationsContent />
      </div>
    </main>
  )
}
