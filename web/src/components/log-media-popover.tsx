"use client"

import * as React from "react"
import { useRouter } from "next/navigation"
import { apiPost } from "@/lib/api"
import type { MediaItemResponse } from "@/lib/apiTypes"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { Separator } from "@/components/ui/separator"

import { Plus, Loader2, Film, BookOpen } from "lucide-react"

type Props = {
  /** Optional: render your own trigger button */
  trigger?: React.ReactNode
  /** Optional: default tab */
  defaultType?: "movie" | "book"
  /** Optional: called after successful create */
  onCreated?: (item: MediaItemResponse) => void
  /** Optional: compact mode for tight headers */
  compact?: boolean
}

type MovieForm = {
  tmdbId: string
  title: string
  year?: string
  creator?: string
  description?: string
  posterUrl?: string
}

type BookForm = {
  openLibraryId: string
  title: string
  year?: string
  creator?: string
  description?: string
  posterUrl?: string
}

function cn(...xs: Array<string | false | null | undefined>) {
  return xs.filter(Boolean).join(" ")
}

function toIntOrUndef(s?: string) {
  const t = (s ?? "").trim()
  if (!t) return undefined
  const n = Number(t)
  return Number.isFinite(n) ? Math.trunc(n) : undefined
}

function cleanOptional(s?: string) {
  const t = (s ?? "").trim()
  return t.length ? t : undefined
}

export function LogMediaPopover({
  trigger,
  defaultType = "movie",
  onCreated,
  compact,
}: Props) {
  const router = useRouter()
  const [open, setOpen] = React.useState(false)
  const [tab, setTab] = React.useState<"movie" | "book">(defaultType)

  const [movie, setMovie] = React.useState<MovieForm>({
    tmdbId: "",
    title: "",
    year: "",
    creator: "",
    description: "",
    posterUrl: "",
  })

  const [book, setBook] = React.useState<BookForm>({
    openLibraryId: "",
    title: "",
    year: "",
    creator: "",
    description: "",
    posterUrl: "",
  })

  const [pending, setPending] = React.useState(false)
  const [err, setErr] = React.useState<string | null>(null)

  function resetError() {
    setErr(null)
  }

  async function submitMovie() {
    resetError()
    const payload = {
      tmdbId: movie.tmdbId.trim(),
      title: movie.title.trim(),
      year: toIntOrUndef(movie.year),
      creator: cleanOptional(movie.creator),
      description: cleanOptional(movie.description),
      posterUrl: cleanOptional(movie.posterUrl),
    }

    if (!payload.tmdbId || !payload.title) {
      setErr("Movie requires TMDB ID and Title.")
      return
    }

    setPending(true)
    try {
      const created = await apiPost<MediaItemResponse>("/api/media/movies", payload)
      onCreated?.(created)
      setOpen(false)
      router.push(`/media/${created.id}`)
      router.refresh()
    } catch (e) {
      console.error(e)
      setErr("Failed to log movie. Check ID/title and try again.")
    } finally {
      setPending(false)
    }
  }

  async function submitBook() {
    resetError()
    const payload = {
      openLibraryId: book.openLibraryId.trim(),
      title: book.title.trim(),
      year: toIntOrUndef(book.year),
      creator: cleanOptional(book.creator),
      description: cleanOptional(book.description),
      posterUrl: cleanOptional(book.posterUrl),
    }

    if (!payload.openLibraryId || !payload.title) {
      setErr("Book requires OpenLibrary ID and Title.")
      return
    }

    setPending(true)
    try {
      const created = await apiPost<MediaItemResponse>("/api/media/books", payload)
      onCreated?.(created)
      setOpen(false)
      router.push(`/media/${created.id}`)
      router.refresh()
    } catch (e) {
      console.error(e)
      setErr("Failed to log book. Check ID/title and try again.")
    } finally {
      setPending(false)
    }
  }

  const Trigger = trigger ?? (
    <Button
      variant={compact ? "outline" : "secondary"}
      size={compact ? "sm" : "default"}
      className="gap-2"
    >
      <Plus className="h-4 w-4" />
      Log
    </Button>
  )

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>{Trigger}</PopoverTrigger>

      <PopoverContent
        align="end"
        sideOffset={10}
        className="w-[360px] p-0 overflow-hidden"
      >
        <div className="px-4 py-3">
          <div className="text-sm font-semibold leading-none">Log to Library</div>
          <div className="text-xs text-muted-foreground mt-1">
            Create a movie/book entry (you can enrich later).
          </div>
        </div>

        <Separator />

        <div className="p-4">
          <Tabs value={tab} onValueChange={(v) => setTab(v as any)}>
            <TabsList className="grid grid-cols-2 w-full">
              <TabsTrigger value="movie" className="gap-2">
                <Film className="h-4 w-4" /> Movie
              </TabsTrigger>
              <TabsTrigger value="book" className="gap-2">
                <BookOpen className="h-4 w-4" /> Book
              </TabsTrigger>
            </TabsList>

            <TabsContent value="movie" className="mt-4 space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <Field label="TMDB ID" required>
                  <Input
                    value={movie.tmdbId}
                    onChange={(e) => setMovie((m) => ({ ...m, tmdbId: e.target.value }))}
                    placeholder="e.g. 550"
                    autoComplete="off"
                    inputMode="numeric"
                  />
                </Field>
                <Field label="Year">
                  <Input
                    value={movie.year ?? ""}
                    onChange={(e) => setMovie((m) => ({ ...m, year: e.target.value }))}
                    placeholder="e.g. 1999"
                    inputMode="numeric"
                  />
                </Field>
              </div>

              <Field label="Title" required>
                <Input
                  value={movie.title}
                  onChange={(e) => setMovie((m) => ({ ...m, title: e.target.value }))}
                  placeholder="Fight Club"
                />
              </Field>

              <Field label="Creator">
                <Input
                  value={movie.creator ?? ""}
                  onChange={(e) => setMovie((m) => ({ ...m, creator: e.target.value }))}
                  placeholder="David Fincher"
                />
              </Field>

              <Field label="Poster URL">
                <Input
                  value={movie.posterUrl ?? ""}
                  onChange={(e) => setMovie((m) => ({ ...m, posterUrl: e.target.value }))}
                  placeholder="https://…"
                />
              </Field>

              <Field label="Description">
                <Textarea
                  value={movie.description ?? ""}
                  onChange={(e) => setMovie((m) => ({ ...m, description: e.target.value }))}
                  placeholder="Optional…"
                  className="min-h-[84px]"
                />
              </Field>

              <Footer
                err={err}
                pending={pending}
                onCancel={() => setOpen(false)}
                onSubmit={submitMovie}
              />
            </TabsContent>

            <TabsContent value="book" className="mt-4 space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <Field label="OpenLibrary ID" required>
                  <Input
                    value={book.openLibraryId}
                    onChange={(e) =>
                      setBook((b) => ({ ...b, openLibraryId: e.target.value }))
                    }
                    placeholder="e.g. OL82563W"
                    autoComplete="off"
                  />
                </Field>
                <Field label="Year">
                  <Input
                    value={book.year ?? ""}
                    onChange={(e) => setBook((b) => ({ ...b, year: e.target.value }))}
                    placeholder="e.g. 2010"
                    inputMode="numeric"
                  />
                </Field>
              </div>

              <Field label="Title" required>
                <Input
                  value={book.title}
                  onChange={(e) => setBook((b) => ({ ...b, title: e.target.value }))}
                  placeholder="The Way of Kings"
                />
              </Field>

              <Field label="Creator">
                <Input
                  value={book.creator ?? ""}
                  onChange={(e) => setBook((b) => ({ ...b, creator: e.target.value }))}
                  placeholder="Brandon Sanderson"
                />
              </Field>

              <Field label="Cover URL">
                <Input
                  value={book.posterUrl ?? ""}
                  onChange={(e) => setBook((b) => ({ ...b, posterUrl: e.target.value }))}
                  placeholder="https://…"
                />
              </Field>

              <Field label="Description">
                <Textarea
                  value={book.description ?? ""}
                  onChange={(e) => setBook((b) => ({ ...b, description: e.target.value }))}
                  placeholder="Optional…"
                  className="min-h-[84px]"
                />
              </Field>

              <Footer
                err={err}
                pending={pending}
                onCancel={() => setOpen(false)}
                onSubmit={submitBook}
              />
            </TabsContent>
          </Tabs>
        </div>
      </PopoverContent>
    </Popover>
  )
}

function Field({
  label,
  required,
  children,
}: {
  label: string
  required?: boolean
  children: React.ReactNode
}) {
  return (
    <div className="space-y-1.5">
      <div className="flex items-center gap-2">
        <Label className="text-xs">{label}</Label>
        {required ? (
          <span className="text-[10px] px-1.5 py-0.5 rounded-md border bg-muted text-muted-foreground">
            required
          </span>
        ) : null}
      </div>
      {children}
    </div>
  )
}

function Footer({
  err,
  pending,
  onCancel,
  onSubmit,
}: {
  err: string | null
  pending: boolean
  onCancel: () => void
  onSubmit: () => void
}) {
  return (
    <div className="pt-2 space-y-2">
      {err ? (
        <div className="text-xs text-destructive">{err}</div>
      ) : (
        <div className="text-[11px] text-muted-foreground">
          Tip: you can enrich + tag after creating.
        </div>
      )}

      <div className="flex items-center justify-end gap-2">
        <Button type="button" variant="ghost" size="sm" onClick={onCancel} disabled={pending}>
          Cancel
        </Button>
        <Button type="button" size="sm" onClick={onSubmit} disabled={pending} className="gap-2">
          {pending ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
          Create
        </Button>
      </div>
    </div>
  )
}