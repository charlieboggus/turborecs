"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { recommendationsApi } from "@/lib/api"
import type { MediaType, Recommendation } from "@/lib/types"
import { Sparkles, Shuffle, Film, BookOpen } from "lucide-react"

export function Recommendations() {
    const [recommendations, setRecommendations] = useState<Recommendation[]>([])
    const [singlePick, setSinglePick] = useState<Recommendation | null>(null)
    const [loading, setLoading] = useState(false)
    const [typeFilter, setTypeFilter] = useState<MediaType | "ALL">("ALL")

    const mediaType = typeFilter !== "ALL" ? typeFilter : undefined

    const handleGenerate = async () => {
        setLoading(true)
        setSinglePick(null)
        try {
            const results = await recommendationsApi.get({
                count: 12,
                type: mediaType,
            })
            setRecommendations(results)
        } catch (err) {
            console.error("Failed to get recommendations:", err)
        } finally {
            setLoading(false)
        }
    }

    const handlePickOne = async () => {
        setLoading(true)
        setRecommendations([])
        try {
            const result = await recommendationsApi.pick(mediaType)
            setSinglePick(result)
        } catch (err) {
            console.error("Failed to pick recommendation:", err)
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="space-y-6">
            <div className="flex flex-wrap items-center gap-3">
                <Select
                    value={typeFilter}
                    onValueChange={(v) => setTypeFilter(v as MediaType | "ALL")}
                >
                    <SelectTrigger className="w-[170px]">
                        <SelectValue placeholder="Media type" />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="ALL">Movies & Books</SelectItem>
                        <SelectItem value="MOVIE">Movies Only</SelectItem>
                        <SelectItem value="BOOK">Books Only</SelectItem>
                    </SelectContent>
                </Select>

                <Button
                    onClick={handleGenerate}
                    disabled={loading}
                    className="gap-2"
                >
                    <Sparkles className="h-4 w-4" />
                    {loading ? "Generating..." : "Generate Recommendations"}
                </Button>

                <Button
                    onClick={handlePickOne}
                    disabled={loading}
                    variant="secondary"
                    className="gap-2"
                >
                    <Shuffle className="h-4 w-4" />
                    {loading ? "Picking..." : "Pick Me Something"}
                </Button>
            </div>

            {singlePick && (
                <Card className="border-primary/50 bg-primary/5">
                    <CardContent className="p-6">
                        <div className="flex items-start gap-4">
                            <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-lg bg-primary/10">
                                {singlePick.type === "MOVIE" ? (
                                    <Film className="h-8 w-8 text-primary" />
                                ) : (
                                    <BookOpen className="h-8 w-8 text-primary" />
                                )}
                            </div>
                            <div className="space-y-2">
                                <div className="flex items-center gap-2">
                                    <h3 className="text-xl font-semibold">
                                        {singlePick.title}
                                    </h3>
                                    {singlePick.year && (
                                        <span className="text-muted-foreground">
                                            ({singlePick.year})
                                        </span>
                                    )}
                                </div>
                                <p className="text-sm text-muted-foreground">
                                    {singlePick.creator}
                                </p>
                                <p className="text-sm leading-relaxed">
                                    {singlePick.reason}
                                </p>
                                <div className="flex flex-wrap gap-1.5 pt-1">
                                    {singlePick.matchedThemes.map((theme) => (
                                        <Badge
                                            key={theme}
                                            variant="secondary"
                                            className="text-xs"
                                        >
                                            {theme}
                                        </Badge>
                                    ))}
                                </div>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            )}

            {recommendations.length > 0 && (
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    {recommendations.map((rec) => (
                        <RecommendationCard
                            key={`${rec.title}-${rec.year}`}
                            rec={rec}
                        />
                    ))}
                </div>
            )}

            {!loading && !singlePick && recommendations.length === 0 && (
                <div className="py-16 text-center">
                    <Sparkles className="mx-auto mb-4 h-12 w-12 text-muted-foreground/30" />
                    <p className="text-muted-foreground">
                        Hit a button above to get personalized recommendations
                        based on your library.
                    </p>
                </div>
            )}
        </div>
    )
}

function RecommendationCard({ rec }: { rec: Recommendation }) {
    const Icon = rec.type === "MOVIE" ? Film : BookOpen

    return (
        <Card>
            <CardContent className="p-4 space-y-3">
                <div className="flex items-start justify-between gap-2">
                    <div>
                        <h3 className="font-medium leading-tight">
                            {rec.title}
                        </h3>
                        <p className="text-sm text-muted-foreground">
                            {rec.creator}
                            {rec.year ? ` · ${rec.year}` : ""}
                        </p>
                    </div>
                    <Badge variant="outline" className="shrink-0 gap-1">
                        <Icon className="h-3 w-3" />
                        {rec.type === "MOVIE" ? "Film" : "Book"}
                    </Badge>
                </div>

                <p className="text-sm leading-relaxed text-muted-foreground">
                    {rec.reason}
                </p>

                <div className="flex flex-wrap gap-1.5">
                    {rec.matchedThemes.map((theme) => (
                        <Badge
                            key={theme}
                            variant="secondary"
                            className="text-xs"
                        >
                            {theme}
                        </Badge>
                    ))}
                </div>
            </CardContent>
        </Card>
    )
}
