import Link from "next/link"
import { Nav } from "@/components/nav"
import { RecommendationsContent } from "@/components/recommendations-content"
import { Separator } from "@/components/ui/separator"
import type { MediaType } from "@/lib/types"

interface RecommendationsPageProps {
  searchParams: Promise<{
    type?: MediaType
  }>
}

export default async function RecommendationsPage({
  searchParams,
}: RecommendationsPageProps) {
  const params = await searchParams
  const mediaType = params.type

  return (
    <main className="min-h-screen bg-background">
      <div className="mx-auto max-w-5xl px-6 py-10">
        <Nav current="/recommendations" />
        <div className="flex items-end justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">
              Recommendations
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              AI-powered picks based on your taste profile.
            </p>
          </div>
          <div className="flex items-center gap-2">
            <FilterLink
              label="All"
              href="/recommendations"
              active={!mediaType}
            />
            <FilterLink
              label="Movies"
              href="/recommendations?type=MOVIE"
              active={mediaType === "MOVIE"}
            />
            <FilterLink
              label="Books"
              href="/recommendations?type=BOOK"
              active={mediaType === "BOOK"}
            />
          </div>
        </div>
        <Separator className="my-6" />
        <RecommendationsContent mediaType={mediaType} />
      </div>
    </main>
  )
}

function FilterLink({
  label,
  href,
  active,
}: {
  label: string
  href: string
  active: boolean
}) {
  return (
    <Link
      href={href}
      className={`px-2.5 py-1 rounded-md text-xs transition-colors ${
        active
          ? "bg-foreground text-background font-medium"
          : "text-muted-foreground hover:text-foreground hover:bg-muted"
      }`}
    >
      {label}
    </Link>
  )
}
