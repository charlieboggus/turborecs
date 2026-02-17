"use client"

import type { TasteProfile } from "@/lib/types"

const DIMENSION_LABELS: Record<
  string,
  { name: string; low: string; high: string }
> = {
  EMOTIONAL_INTENSITY: {
    name: "Emotional Intensity",
    low: "Detached",
    high: "Devastating",
  },
  NARRATIVE_COMPLEXITY: {
    name: "Narrative Complexity",
    low: "Linear",
    high: "Complex",
  },
  MORAL_AMBIGUITY: {
    name: "Moral Ambiguity",
    low: "Clear-cut",
    high: "Gray zones",
  },
  TONE_DARKNESS: { name: "Tone", low: "Light", high: "Dark" },
  PACING: { name: "Pacing", low: "Slow-burn", high: "Propulsive" },
  HUMOR: { name: "Humor", low: "Serious", high: "Funny" },
  VIOLENCE_INTENSITY: { name: "Violence", low: "Gentle", high: "Graphic" },
  INTELLECTUAL_DEPTH: {
    name: "Intellectual Depth",
    low: "Escapism",
    high: "Ideas-driven",
  },
  STYLISTIC_BOLDNESS: {
    name: "Stylistic Boldness",
    low: "Conventional",
    high: "Experimental",
  },
  INTIMACY_SCALE: { name: "Intimacy vs Scale", low: "Epic", high: "Personal" },
  REALISM: { name: "Realism", low: "Fantastical", high: "Grounded" },
  CULTURAL_SPECIFICITY: {
    name: "Cultural Specificity",
    low: "Universal",
    high: "Specific",
  },
}

export function DimensionSpectrum({ profile }: { profile: TasteProfile }) {
  const sortedDimensions = Object.entries(profile.tasteVector).sort(
    ([, a], [, b]) => b - a,
  )

  return (
    <div>
      <div className="flex flex-col gap-1.5">
        {sortedDimensions.map(([key, score]) => {
          const dim = DIMENSION_LABELS[key]
          if (!dim) return null
          const antiScore = profile.antiVector?.[key] || 0
          const gap = score - antiScore
          const isStrong = gap > 0.4

          return (
            <div
              key={key}
              className={`px-3 py-2.5 rounded-lg ${
                isStrong ? "bg-foreground/[0.03]" : ""
              }`}
            >
              {/* Label row */}
              <div className="flex justify-between items-center mb-1.5">
                <span
                  className={`text-[13px] ${
                    isStrong
                      ? "font-semibold text-foreground"
                      : "font-normal text-muted-foreground"
                  }`}
                >
                  {dim.name}
                </span>
                <span className="text-[11px] tabular-nums text-muted-foreground/50">
                  {score.toFixed(2)}
                </span>
              </div>

              {/* Spectrum bar */}
              <div className="relative h-5 flex items-center">
                {/* Track */}
                <div className="absolute inset-x-0 top-1/2 -translate-y-1/2 h-[3px] rounded-full bg-gradient-to-r from-border to-muted-foreground/20" />

                {/* Anti marker */}
                {antiScore > 0.05 && (
                  <div
                    className="absolute top-1/2 -translate-x-1/2 -translate-y-1/2 w-2 h-2 rounded-full bg-red-500 opacity-40"
                    style={{ left: `${antiScore * 100}%` }}
                  />
                )}

                {/* Taste marker */}
                <div
                  className="absolute top-1/2 -translate-x-1/2 -translate-y-1/2 w-3 h-3 rounded-full bg-foreground shadow-[0_0_0_2px_var(--background),0_0_0_3px_rgba(250,250,250,0.2)]"
                  style={{ left: `${score * 100}%` }}
                />
              </div>

              {/* Low/High labels */}
              <div className="flex justify-between mt-0.5">
                <span className="text-[10px] text-muted-foreground/30">
                  {dim.low}
                </span>
                <span className="text-[10px] text-muted-foreground/30">
                  {dim.high}
                </span>
              </div>
            </div>
          )
        })}
      </div>

      {/* Legend */}
      <div className="flex gap-4 mt-3 pl-3">
        <div className="flex items-center gap-1.5">
          <div className="w-2.5 h-2.5 rounded-full bg-foreground border border-border" />
          <span className="text-[11px] text-muted-foreground/50">
            Your preference
          </span>
        </div>
        <div className="flex items-center gap-1.5">
          <div className="w-2 h-2 rounded-full bg-red-500 opacity-40" />
          <span className="text-[11px] text-muted-foreground/50">
            Anti-preference
          </span>
        </div>
      </div>
    </div>
  )
}
