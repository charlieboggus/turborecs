import { Nav } from "@/components/nav"
import { getTasteProfile } from "@/lib/api"
import { TasteProfile } from "@/lib/types"
import { Separator } from "@/components/ui/separator"
import { DimensionSpectrum } from "@/components/dimension-spectrum"
import { TasteProfileTabs } from "@/components/taste-profile-tabs"
import { Star, ThumbsDown } from "lucide-react"

export default async function TasteProfilePage() {
  let tasteProfile: TasteProfile | null = null
  try {
    tasteProfile = await getTasteProfile()
  } catch {
    // No taste profile yet
  }
  const hasTags =
    tasteProfile != null && Object.keys(tasteProfile.themes).length > 0
  const hasVectors =
    tasteProfile != null &&
    tasteProfile.tasteVector != null &&
    Object.values(tasteProfile.tasteVector).some((v) => v > 0)
  const hasProfile = hasTags || hasVectors
  return (
    <main className="min-h-screen bg-background">
      <div className="mx-auto max-w-5xl px-6 py-10">
        <Nav current="/profile" />
        {/* Taste Profile */}
        {hasProfile && tasteProfile && (
          <TasteProfileSection
            profile={tasteProfile}
            hasVectors={hasVectors}
            hasTags={hasTags}
          />
        )}
      </div>
    </main>
  )
}

function TasteProfileSection({
  profile,
  hasVectors,
  hasTags,
}: {
  profile: TasteProfile
  hasVectors: boolean
  hasTags: boolean
}) {
  const hasTopRated = profile.topRatedTitles.length > 0
  const hasLowRated = profile.lowRatedTitles.length > 0
  const coveragePct = Math.round((profile.vectorCoverage ?? 0) * 100)

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Taste Profile</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Your media DNA
          {coveragePct > 0 && (
            <> — built from {coveragePct}% of your rated library</>
          )}
          .
        </p>
      </div>
      <Separator />

      {/* Dimensions */}
      {hasVectors && (
        <div>
          <div className="flex items-center gap-2 mb-4">
            <span className="text-[13px] font-semibold text-muted-foreground uppercase tracking-wider">
              Dimensions
            </span>
            <span className="text-[11px] text-muted-foreground/50">
              How you like your media
            </span>
          </div>
          <DimensionSpectrum profile={profile} />
        </div>
      )}

      {hasVectors && hasTags && <Separator />}

      {/* Tags */}
      {hasTags && (
        <div>
          <div className="flex items-center gap-2 mb-4">
            <span className="text-[13px] font-semibold text-muted-foreground uppercase tracking-wider">
              Tags
            </span>
            <span className="text-[11px] text-muted-foreground/50">
              What you like your media to be about
            </span>
          </div>
          <TasteProfileTabs profile={profile} />
        </div>
      )}

      {/* Favorites / Low Rated */}
      {(hasTopRated || hasLowRated) && (
        <>
          <Separator />
          <div className="flex justify-between">
            <div className="grid grid-cols-2 gap-16">
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
          </div>
        </>
      )}
    </div>
  )
}
