import { Nav } from "@/components/nav"
import { Separator } from "@/components/ui/separator"
import { SearchInterface } from "@/components/search-interface"

export default function SearchPage() {
  return (
    <main className="min-h-screen bg-background">
      <div className="mx-auto max-w-5xl px-6 py-10">
        <Nav current="/search" />

        <div>
          <h1 className="text-3xl font-bold tracking-tight">Search</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Find movies and books to add to your library or exclude from
            recommendations.
          </p>
        </div>

        <Separator className="my-6" />

        <SearchInterface />
      </div>
    </main>
  )
}
