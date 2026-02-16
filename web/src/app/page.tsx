// app/page.tsx

import Link from "next/link"
import { getStats, getTasteProfile } from "@/lib/api"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import {
  Film,
  BookOpen,
  Sparkles,
  Search,
  Ban,
  Tag,
  Library,
  ArrowRight,
  Star,
  ThumbsDown,
} from "lucide-react"
import { TasteProfileTabs } from "@/components/taste-profile-tabs"
import type { TasteProfile } from "@/lib/types"

export default async function HomePage() {
  let stats = null
  let tasteProfile: TasteProfile | null = null
  try {
    stats = await getStats()
  } catch {
    // API might be down
  }
  try {
    tasteProfile = await getTasteProfile()
  } catch {
    // No taste profile yet
  }
  const hasProfile =
    tasteProfile != null && Object.keys(tasteProfile.themes).length > 0

  return (
    <main className="min-h-screen bg-background">
      <div className="mx-auto max-w-5xl px-6 py-16">
        <div className="space-y-2">
          <h1 className="text-4xl font-bold tracking-tight">Turborecs</h1>
          <p className="text-lg text-muted-foreground">
            AI-powered personal media recommendation engine.
          </p>
        </div>
        <Separator className="my-8" />
        {stats && (
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <StatCard
              icon={<Film className="h-4 w-4" />}
              label="Movies"
              value={stats.movieCount}
            />
            <StatCard
              icon={<BookOpen className="h-4 w-4" />}
              label="Books"
              value={stats.bookCount}
            />
            <StatCard
              icon={<Tag className="h-4 w-4" />}
              label="Tags"
              value={stats.uniqueTagCount}
            />
            <StatCard
              icon={<Sparkles className="h-4 w-4" />}
              label="Recommended"
              value={stats.recommendationCount}
            />
          </div>
        )}
        <div className="mt-8 grid gap-4 sm:grid-cols-2">
          <NavCard
            href="/library"
            icon={<Library className="h-5 w-5" />}
            title="Library"
            description="Browse your watched movies and read books."
          />
          <NavCard
            href="/search"
            icon={<Search className="h-5 w-5" />}
            title="Search"
            description="Find movies and books to add to your library or exclude."
          />
          <NavCard
            href="/recommendations"
            icon={<Sparkles className="h-5 w-5" />}
            title="Recommendations"
            description="AI-powered picks based on your taste profile."
          />
          <NavCard
            href="/exclusions"
            icon={<Ban className="h-5 w-5" />}
            title="Exclusions"
            description="Titles you never want recommended."
          />
        </div>
        {hasProfile && tasteProfile && (
          <>
            <Separator className="my-10" />
            <TasteProfileSection profile={tasteProfile} />
          </>
        )}
        {stats && stats.totalItems === 0 && (
          <Card className="mt-10">
            <CardContent className="flex flex-col items-center gap-4 py-12">
              <p className="text-muted-foreground">
                Your library is empty. Start by searching for something you've
                watched or read.
              </p>
              <Button asChild>
                <Link href="/search">
                  <Search className="mr-2 h-4 w-4" />
                  Search for media
                </Link>
              </Button>
            </CardContent>
          </Card>
        )}
      </div>
    </main>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// Stat Card
// ─────────────────────────────────────────────────────────────────────────────

function StatCard({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode
  label: string
  value: number
}) {
  return (
    <Card>
      <CardContent className="p-4">
        <div className="flex items-center gap-1.5 text-muted-foreground">
          {icon}
          <span className="text-xs font-medium uppercase tracking-wide">
            {label}
          </span>
        </div>
        <p className="mt-2 text-3xl font-bold tabular-nums">{value}</p>
      </CardContent>
    </Card>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// Nav Card
// ─────────────────────────────────────────────────────────────────────────────

function NavCard({
  href,
  icon,
  title,
  description,
}: {
  href: string
  icon: React.ReactNode
  title: string
  description: string
}) {
  return (
    <Link href={href}>
      <Card className="h-full transition-colors hover:bg-muted/50">
        <CardHeader className="flex flex-row items-start gap-3 space-y-0">
          <div className="mt-0.5 text-muted-foreground">{icon}</div>
          <div className="flex-1">
            <CardTitle className="flex items-center justify-between text-base">
              {title}
              <ArrowRight className="h-4 w-4 text-muted-foreground" />
            </CardTitle>
            <CardDescription className="mt-1">{description}</CardDescription>
          </div>
        </CardHeader>
      </Card>
    </Link>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// Taste Profile Section
// ─────────────────────────────────────────────────────────────────────────────

function TasteProfileSection({ profile }: { profile: TasteProfile }) {
  const hasTopRated = profile.topRatedTitles.length > 0
  const hasLowRated = profile.lowRatedTitles.length > 0
  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold tracking-tight">Taste Profile</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Built from your ratings and AI-generated tags.
        </p>
      </div>
      <TasteProfileTabs profile={profile} />
      {(hasTopRated || hasLowRated) && (
        <div className="grid grid-cols-2 gap-8 mt-8">
          {hasTopRated && (
            <div>
              <div className="flex items-center gap-1.5 mb-3">
                <Star className="h-3.5 w-3.5 text-amber-500" />
                <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  Favorites
                </span>
              </div>
              <div className="flex flex-col gap-1">
                {profile.topRatedTitles.map((title) => (
                  <span
                    key={title}
                    className="text-sm pl-5 text-muted-foreground"
                  >
                    {title}
                  </span>
                ))}
              </div>
            </div>
          )}
          {hasLowRated && (
            <div>
              <div className="flex items-center gap-1.5 mb-3">
                <ThumbsDown className="h-3.5 w-3.5 text-muted-foreground/50" />
                <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  Low Rated
                </span>
              </div>
              <div className="flex flex-col gap-1">
                {profile.lowRatedTitles.map((title) => (
                  <span
                    key={title}
                    className="text-sm pl-5 text-muted-foreground/60"
                  >
                    {title}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
