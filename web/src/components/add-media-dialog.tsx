"use client"

import { useState } from "react"
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { StarRating } from "@/components/star-rating"
import { mediaApi } from "@/lib/api"
import type { MediaType, MediaStatus } from "@/lib/types"
import { Plus } from "lucide-react"

interface AddMediaDialogProps {
    onAdded: () => void
}

export function AddMediaDialog({ onAdded }: AddMediaDialogProps) {
    const [open, setOpen] = useState(false)
    const [loading, setLoading] = useState(false)
    const [title, setTitle] = useState("")
    const [type, setType] = useState<MediaType>("MOVIE")
    const [status, setStatus] = useState<MediaStatus>("FINISHED")
    const [rating, setRating] = useState<number | null>(null)
    const [notes, setNotes] = useState("")

    const resetForm = () => {
        setTitle("")
        setType("MOVIE")
        setStatus("FINISHED")
        setRating(null)
        setNotes("")
    }

    const handleSubmit = async () => {
        if (!title.trim()) return
        setLoading(true)

        try {
            const item = await mediaApi.create(title.trim(), type)

            if (status !== "WANT_TO_CONSUME") {
                await mediaApi.updateStatus(item.id, status)
            }
            if (rating) {
                await mediaApi.updateRating(item.id, rating)
            }
            if (notes.trim()) {
                await mediaApi.updateNotes(item.id, notes.trim())
            }

            resetForm()
            setOpen(false)
            onAdded()
        } catch (err) {
            console.error("Failed to add media:", err)
        } finally {
            setLoading(false)
        }
    }

    return (
        <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
                <Button className="gap-2">
                    <Plus className="h-4 w-4" />
                    Log Media
                </Button>
            </DialogTrigger>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Log Media</DialogTitle>
                </DialogHeader>

                <div className="space-y-4 pt-2">
                    <div className="space-y-2">
                        <Label htmlFor="title">Title</Label>
                        <Input
                            id="title"
                            placeholder="Enter title..."
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <Label>Type</Label>
                            <Select
                                value={type}
                                onValueChange={(v) => setType(v as MediaType)}
                            >
                                <SelectTrigger>
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="MOVIE">Movie</SelectItem>
                                    <SelectItem value="BOOK">Book</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="space-y-2">
                            <Label>Status</Label>
                            <Select
                                value={status}
                                onValueChange={(v) =>
                                    setStatus(v as MediaStatus)
                                }
                            >
                                <SelectTrigger>
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="WANT_TO_CONSUME">
                                        Want to Watch/Read
                                    </SelectItem>
                                    <SelectItem value="IN_PROGRESS">
                                        In Progress
                                    </SelectItem>
                                    <SelectItem value="FINISHED">
                                        Finished
                                    </SelectItem>
                                    <SelectItem value="DNF">
                                        Did Not Finish
                                    </SelectItem>
                                </SelectContent>
                            </Select>
                        </div>
                    </div>

                    <div className="space-y-2">
                        <Label>Rating</Label>
                        <StarRating rating={rating} onRate={setRating} />
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="notes">Notes</Label>
                        <Textarea
                            id="notes"
                            placeholder="Your thoughts..."
                            value={notes}
                            onChange={(e) => setNotes(e.target.value)}
                            rows={3}
                        />
                    </div>

                    <Button
                        className="w-full"
                        onClick={handleSubmit}
                        disabled={!title.trim() || loading}
                    >
                        {loading ? "Saving..." : "Save"}
                    </Button>
                </div>
            </DialogContent>
        </Dialog>
    )
}
