"use client"

import { useState } from "react"
import type { TasteProfile } from "@/lib/types"

const CATEGORIES = [
  {
    key: "themes",
    label: "Themes",
    accent: "bg-violet-500",
    accentLight: "bg-violet-500/10",
  },
  {
    key: "moods",
    label: "Moods",
    accent: "bg-blue-500",
    accentLight: "bg-blue-500/10",
  },
  {
    key: "tones",
    label: "Tones",
    accent: "bg-amber-500",
    accentLight: "bg-amber-500/10",
  },
  {
    key: "settings",
    label: "Settings",
    accent: "bg-emerald-500",
    accentLight: "bg-emerald-500/10",
  },
] as const

const ACCENT_BORDER: Record<string, string> = {
  themes: "border-b-violet-500",
  moods: "border-b-blue-500",
  tones: "border-b-amber-500",
  settings: "border-b-emerald-500",
}

function strengthLabel(weight: number): string {
  if (weight >= 0.9) return "dominant"
  if (weight >= 0.7) return "strong"
  if (weight >= 0.5) return "moderate"
  return "mild"
}

export function TasteProfileTabs({ profile }: { profile: TasteProfile }) {
  const [activeCategory, setActiveCategory] = useState<string>("themes")

  const activeMeta = CATEGORIES.find((c) => c.key === activeCategory)!
  const activeTags = Object.entries(
    profile[
      activeCategory as keyof Pick<
        TasteProfile,
        "themes" | "moods" | "tones" | "settings"
      >
    ],
  )

  return (
    <div>
      {/* Tabs */}
      <div className="flex gap-1 border-b border-border mb-6">
        {CATEGORIES.map((cat) => {
          const isActive = activeCategory === cat.key
          return (
            <button
              key={cat.key}
              onClick={() => setActiveCategory(cat.key)}
              className={`px-4 py-2 text-sm -mb-px border-b-2 transition-colors cursor-pointer ${
                isActive
                  ? `font-semibold text-foreground ${ACCENT_BORDER[cat.key]}`
                  : "font-normal text-muted-foreground border-b-transparent hover:text-foreground"
              }`}
            >
              {cat.label}
            </button>
          )
        })}
      </div>

      {/* Tag Rows */}
      <div className="flex flex-col gap-0.5">
        {activeTags.map(([name, weight], i) => {
          const isTop = i === 0
          const dotSize = 6 + weight * 6

          return (
            <div
              key={name}
              className={`flex items-center justify-between px-3 py-2.5 rounded-lg transition-colors ${
                isTop ? activeMeta.accentLight : ""
              }`}
            >
              <div className="flex items-center gap-3">
                {/* Rank */}
                <span className="w-5 text-right text-[11px] font-medium tabular-nums text-muted-foreground/50">
                  {i + 1}
                </span>

                {/* Weighted dot */}
                <span
                  className={`rounded-full shrink-0 ${activeMeta.accent}`}
                  style={{
                    width: dotSize,
                    height: dotSize,
                    opacity: 0.35 + weight * 0.65,
                  }}
                />

                {/* Tag name */}
                <span
                  className={`text-sm ${isTop ? "font-semibold" : "font-normal"}`}
                  style={{ opacity: 0.35 + weight * 0.65 }}
                >
                  {name}
                </span>
              </div>

              {/* Strength label */}
              <span className="text-[11px] text-muted-foreground/50">
                {strengthLabel(weight)}
              </span>
            </div>
          )
        })}
      </div>

      <p className="text-[11px] text-muted-foreground/30 mt-3 pl-3">
        Tags are extracted from each item by AI and weighted by your ratings.
        Higher-rated items contribute more.
      </p>
    </div>
  )
}
