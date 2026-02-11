"use client"

import { useCallback, useEffect, useState } from "react"
import { Input } from "@/components/ui/input"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { MediaCard } from "@/components/media-card"
import { AddMediaDialog } from "@/components/add-media-dialog"
import { MediaDetailDialog } from "@/components/media-detail-dialog"
import { mediaApi } from "@/lib/api"
import type { MediaItem, MediaType, MediaStatus } from "@/lib/types"
import { Search } from "lucide-react"

export function Library() {
    const [items, setItems] = useState<MediaItem[]>([])
    const [loading, setLoading] = useState(true)
    const [searchQuery, setSearchQuery] = useState("")
    const [typeFilter, setTypeFilter] = useState<MediaType | "ALL">("ALL")
    const [statusFilter, setStatusFilter] = useState<MediaStatus | "ALL">("ALL")
    const [sortBy, setSortBy] = useState("dateAdded")
    const [selectedItem, setSelectedItem] = useState<MediaItem | null>(null)

    const fetchItems = useCallback(async () => {
        setLoading(true)
        try {
            let result: MediaItem[]
            if (searchQuery.trim()) {
                result = await mediaApi.search(searchQuery)
            } else {
                result = await mediaApi.getAll({
                    type: typeFilter !== "ALL" ? typeFilter : undefined,
                    status: statusFilter !== "ALL" ? statusFilter : undefined,
                    sortBy,
                })
            }
            setItems(result)
        } catch (err) {
            console.error("Failed to fetch media:", err)
        } finally {
            setLoading(false)
        }
    }, [searchQuery, typeFilter, statusFilter, sortBy])

    useEffect(() => {
        fetchItems()
    }, [fetchItems])

    useEffect(() => {
        const timeout = setTimeout(() => {
            if (searchQuery.trim()) fetchItems()
        }, 300)
        return () => clearTimeout(timeout)
    }, [searchQuery, fetchItems])

    return (
        <div className="space-y-6">
            <div className="flex flex-wrap items-center gap-3">
                <div className="relative flex-1 min-w-[200px]">
                    <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                    <Input
                        placeholder="Search titles..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="pl-9"
                    />
                </div>

                <Select
                    value={typeFilter}
                    onValueChange={(v) => setTypeFilter(v as MediaType | "ALL")}
                >
                    <SelectTrigger className="w-[130px]">
                        <SelectValue placeholder="Type" />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="ALL">All Types</SelectItem>
                        <SelectItem value="MOVIE">Movies</SelectItem>
                        <SelectItem value="BOOK">Books</SelectItem>
                    </SelectContent>
                </Select>

                <Select
                    value={statusFilter}
                    onValueChange={(v) =>
                        setStatusFilter(v as MediaStatus | "ALL")
                    }
                >
                    <SelectTrigger className="w-[150px]">
                        <SelectValue placeholder="Status" />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="ALL">All Statuses</SelectItem>
                        <SelectItem value="FINISHED">Finished</SelectItem>
                        <SelectItem value="IN_PROGRESS">In Progress</SelectItem>
                        <SelectItem value="WANT_TO_CONSUME">
                            Watchlist
                        </SelectItem>
                        <SelectItem value="DNF">Did Not Finish</SelectItem>
                    </SelectContent>
                </Select>

                <Select value={sortBy} onValueChange={setSortBy}>
                    <SelectTrigger className="w-[140px]">
                        <SelectValue placeholder="Sort" />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="dateAdded">Date Added</SelectItem>
                        <SelectItem value="rating">Rating</SelectItem>
                        <SelectItem value="title">Title</SelectItem>
                    </SelectContent>
                </Select>

                <AddMediaDialog onAdded={fetchItems} />
            </div>

            {loading ? (
                <p className="text-center text-muted-foreground py-12">
                    Loading...
                </p>
            ) : items.length === 0 ? (
                <p className="text-center text-muted-foreground py-12">
                    No items found. Log something!
                </p>
            ) : (
                <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
                    {items.map((item) => (
                        <MediaCard
                            key={item.id}
                            item={item}
                            onClick={setSelectedItem}
                        />
                    ))}
                </div>
            )}

            <MediaDetailDialog
                item={selectedItem}
                onClose={() => setSelectedItem(null)}
                onUpdated={fetchItems}
            />
        </div>
    )
}
