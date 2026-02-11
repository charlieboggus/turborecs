"use client"

import { useState } from "react"
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { Separator } from "@/components/ui/separator"
import { StarRating } from "@/components/star-rating"
import { mediaApi } from "@/lib/api"
import type { MediaItem, MediaStatus } from "@/lib/types"
import { Trash2, Clock, BookOpen, Film } from "lucide-react"

interface MediaDetailDialogProps {
    item: MediaItem | null
    onClose: () => void
    onUpdated: () => void
}

const statusLabels: Record<string, string> = {
    WANT_TO_CONSUME: "Want to Watch/Read",
    IN_PROGRESS: "In Progress",
    FINISHED: "Finished",
    DNF: "Did Not Finish",
}

export function MediaDetailDialog({
    item,
    onClose,
    onUpdated,
}: MediaDetailDialogProps) {
    const [notes, setNotes] = useState(item?.notes ?? "")
    const [saving, setSaving] = useState(false)

    if (!item) return null

    const handleRate = async (rating: number) => {
        await mediaApi.updateRating(item.id, rating)
        onUpdated()
    }

    const handleStatusChange = async (status: MediaStatus) => {
        await mediaApi.updateStatus(item.id, status)
        onUpdated()
    }

    const handleSaveNotes = async () => {
        setSaving(true)
        await mediaApi.updateNotes(item.id, notes.trim() || null)
        setSaving(false)
        onUpdated()
    }

    const handleDelete = async () => {
        await mediaApi.delete(item.id)
        onClose()
        onUpdated()
    }

    return (
        <Dialog open={!!item} onOpenChange={(open) => !open && onClose()}>
            <DialogContent className="max-w-lg">
                <DialogHeader>
                    <DialogTitle className="pr-8">{item.title}</DialogTitle>
                </DialogHeader>

                <div className="space-y-4">
                    <div className="flex gap-4">
                        {item.posterUrl && (
                            <div className="w-28 shrink-0 overflow-hidden rounded-md">
                                <img
                                    src={item.posterUrl}
                                    alt={item.title}
                                    className="h-full w-full object-cover"
                                />
                            </div>
                        )}

                        <div className="space-y-2">
                            <div className="flex flex-wrap items-center gap-2">
                                <Badge variant="secondary">
                                    {item.type === "MOVIE" ? "Film" : "Book"}
                                </Badge>
                                {item.year && (
                                    <span className="text-sm text-muted-foreground">
                                        {item.year}
                                    </span>
                                )}
                            </div>

                            {item.creator && (
                                <p className="text-sm">
                                    {item.type === "MOVIE"
                                        ? "Directed by"
                                        : "By"}{" "}
                                    <span className="font-medium">
                                        {item.creator}
                                    </span>
                                </p>
                            )}

                            {item.runtime && (
                                <p className="flex items-center gap-1 text-sm text-muted-foreground">
                                    <Clock className="h-3.5 w-3.5" />
                                    {Math.floor(item.runtime / 60)}h{" "}
                                    {item.runtime % 60}m
                                </p>
                            )}

                            {item.pageCount && (
                                <p className="flex items-center gap-1 text-sm text-muted-foreground">
                                    <BookOpen className="h-3.5 w-3.5" />
                                    {item.pageCount} pages
                                </p>
                            )}

                            {item.genres.length > 0 && (
                                <div className="flex flex-wrap gap-1">
                                    {item.genres.map((genre) => (
                                        <Badge
                                            key={genre}
                                            variant="outline"
                                            className="text-xs"
                                        >
                                            {genre}
                                        </Badge>
                                    ))}
                                </div>
                            )}

                            {item.description && (
                                <p className="text-sm text-muted-foreground line-clamp-3">
                                    {item.description}
                                </p>
                            )}
                        </div>
                    </div>

                    <Separator />

                    <div className="space-y-2">
                        <Label>Rating</Label>
                        <StarRating rating={item.rating} onRate={handleRate} />
                    </div>

                    <div className="space-y-2">
                        <Label>Status</Label>
                        <Select
                            value={item.status}
                            onValueChange={(v) =>
                                handleStatusChange(v as MediaStatus)
                            }
                        >
                            <SelectTrigger>
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {Object.entries(statusLabels).map(
                                    ([value, label]) => (
                                        <SelectItem key={value} value={value}>
                                            {label}
                                        </SelectItem>
                                    ),
                                )}
                            </SelectContent>
                        </Select>
                    </div>

                    <div className="space-y-2">
                        <Label>Notes</Label>
                        <Textarea
                            value={notes}
                            onChange={(e) => setNotes(e.target.value)}
                            placeholder="Your thoughts..."
                            rows={3}
                        />
                        <Button
                            size="sm"
                            variant="secondary"
                            onClick={handleSaveNotes}
                            disabled={saving}
                        >
                            {saving ? "Saving..." : "Save Notes"}
                        </Button>
                    </div>

                    {item.tags.length > 0 && (
                        <>
                            <Separator />
                            <div className="space-y-2">
                                <Label>Tags</Label>
                                <div className="flex flex-wrap gap-1.5">
                                    {item.tags.map((wt) => (
                                        <Badge
                                            key={`${wt.tag.name}-${wt.tag.category}`}
                                            variant="outline"
                                            className="text-xs"
                                        >
                                            {wt.tag.name}
                                        </Badge>
                                    ))}
                                </div>
                            </div>
                        </>
                    )}

                    <Separator />

                    <div className="flex items-center justify-between">
                        <span className="text-xs text-muted-foreground">
                            Added{" "}
                            {new Date(item.dateAdded).toLocaleDateString()}
                            {item.dateFinished &&
                                ` · Finished ${new Date(item.dateFinished).toLocaleDateString()}`}
                        </span>
                        <Button
                            variant="destructive"
                            size="sm"
                            className="gap-2"
                            onClick={handleDelete}
                        >
                            <Trash2 className="h-4 w-4" />
                            Delete
                        </Button>
                    </div>
                </div>
            </DialogContent>
        </Dialog>
    )
}
