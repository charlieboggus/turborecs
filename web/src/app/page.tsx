import Link from "next/link"
import { getStats } from "@/lib/api"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { Sparkles, Search, Ban, Library, ArrowRight, User } from "lucide-react"

export default async function HomePage() {
  let stats = null
  try {
    stats = await getStats()
  } catch {
    // API might be down
  }

  return (
    <main className="min-h-screen bg-background">
      <div className="mx-auto max-w-5xl px-6 py-16">
        {/* Header */}
        <div className="space-y-2">
          <h1 className="text-4xl font-bold tracking-tight">Turborecs</h1>
          <p className="text-lg text-muted-foreground">
            AI-powered personal media recommendation engine.
          </p>
        </div>
        <Separator className="my-8" />
        {stats && stats.totalItems === 0 ? (
          // empty state
          <Card className="mt-10">
            <CardContent className="flex flex-col items-center gap-4 py-12">
              <p className="text-muted-foreground">
                Your library is empty. Start by searching for something
                you&apos;ve watched or read.
              </p>
              <Button asChild>
                <Link href="/search">
                  <Search className="mr-2 h-4 w-4" />
                  Search for media
                </Link>
              </Button>
            </CardContent>
          </Card>
        ) : (
          <>
            {/* stats */}
            {stats && stats.totalItems !== 0 && (
              <div className="flex rounded-xl overflow-hidden border mb-8">
                <StatCell label="Movies" value={stats.movieCount} />
                <StatCell label="Books" value={stats.bookCount} />
                <StatCell label="Tags" value={stats.uniqueTagCount} />
                <StatCell label="Vectors" value={stats.vectorCount ?? 0} />
                <StatCell label="Recs" value={stats.recommendationCount} />
              </div>
            )}

            {/* Navigation */}
            <Link href="/recommendations">
              <Card className="p-0 gap-0 mb-4 group transition-all hover:-translate-y-0.5 hover:shadow-lg hover:shadow-purple-500/10 border-purple-500/20">
                <div className="flex items-center justify-between p-6">
                  <div className="flex items-center gap-4">
                    <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-purple-500/10">
                      <Sparkles className="h-5 w-5 text-purple-600 dark:text-purple-400" />
                    </div>
                    <div>
                      <h2 className="text-lg font-semibold text-purple-500 dark:text-purple-300">
                        Recommendations
                      </h2>
                      <p className="text-sm text-muted-foreground">
                        AI-powered picks based on your taste profile.
                      </p>
                    </div>
                  </div>
                  <ArrowRight className="h-5 w-5 text-purple-400/50 transition-transform group-hover:translate-x-0.5" />
                </div>
              </Card>
            </Link>
            <div className="grid grid-cols-2 gap-3">
              <NavCard
                href="/library"
                icon={<Library className="h-4 w-4" />}
                title="Library"
                description="Browse your watched movies and read books."
              />
              <NavCard
                href="/search"
                icon={<Search className="h-4 w-4" />}
                title="Search"
                description="Find movies and books to add or exclude."
              />
              <NavCard
                href="/profile"
                icon={<User className="h-4 w-4" />}
                title="Taste Profile"
                description="Your media DNA — dimensions, tags, and preferences."
              />
              <NavCard
                href="/exclusions"
                icon={<Ban className="h-4 w-4" />}
                title="Exclusions"
                description="Titles you never want recommended."
              />
            </div>
          </>
        )}
      </div>
    </main>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// Stats Strip Cell
// ─────────────────────────────────────────────────────────────────────────────

function StatCell({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex-1 bg-muted/30 py-4 px-3 text-center border-r last:border-r-0">
      <div className="text-2xl font-bold tabular-nums">{value}</div>
      <div className="text-[10px] font-semibold text-muted-foreground uppercase tracking-widest mt-1">
        {label}
      </div>
    </div>
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
      <Card className="h-full p-0 gap-0 transition-colors hover:bg-muted/50">
        <div className="flex items-start gap-3 p-4">
          <div className="mt-0.5 text-muted-foreground">{icon}</div>
          <div className="flex-1">
            <div className="flex items-center justify-between">
              <span className="text-sm font-semibold">{title}</span>
              <ArrowRight className="h-3.5 w-3.5 text-muted-foreground/40" />
            </div>
            <p className="mt-1 text-xs text-muted-foreground">{description}</p>
          </div>
        </div>
      </Card>
    </Link>
  )
}
