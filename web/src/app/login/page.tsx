"use client"

import { Suspense, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Separator } from "@/components/ui/separator"
import { Sparkles } from "lucide-react"
import { loginAction } from "@/lib/actions"
import { cn } from "@/lib/utils"

function LoginForm() {
  const [error, setError] = useState("")
  const [loading, setLoading] = useState(false)
  const router = useRouter()
  const searchParams = useSearchParams()

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setLoading(true)
    setError("")

    const form = new FormData(e.currentTarget)
    const result = await loginAction(
      form.get("username") as string,
      form.get("password") as string,
    )

    if (result.error) {
      setError(result.error)
      setLoading(false)
    } else {
      router.push(searchParams.get("redirect") ?? "/")
    }
  }

  const hasError = !!error

  return (
    <form onSubmit={handleSubmit} className="space-y-4" noValidate>
      <div className="space-y-2">
        <Label
          htmlFor="username"
          className={cn(hasError && "text-destructive")}
        >
          Username
        </Label>
        <Input
          id="username"
          name="username"
          autoComplete="username"
          autoFocus
          required
          disabled={loading}
          className={cn(
            hasError &&
              "border-destructive focus-visible:ring-destructive/30 bg-destructive/5",
          )}
        />
      </div>

      <div className="space-y-2">
        <Label
          htmlFor="password"
          className={cn(hasError && "text-destructive")}
        >
          Password
        </Label>
        <Input
          id="password"
          name="password"
          type="password"
          autoComplete="current-password"
          required
          disabled={loading}
          className={cn(
            hasError &&
              "border-destructive focus-visible:ring-destructive/30 bg-destructive/5",
          )}
        />
      </div>

      <div className="min-h-[1.25rem]">
        {hasError && <p className="text-sm text-destructive">{error}</p>}
      </div>

      <Button type="submit" className="w-full" disabled={loading}>
        {loading ? "Signing in…" : "Sign in"}
      </Button>
    </form>
  )
}

export default function LoginPage() {
  return (
    <main className="min-h-screen bg-background flex items-center justify-center px-6">
      <div className="w-full max-w-sm space-y-6">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-purple-500/10">
              <Sparkles className="h-4 w-4 text-purple-600 dark:text-purple-400" />
            </div>
            <h1 className="text-2xl font-bold tracking-tight">Turborecs</h1>
          </div>
          <p className="text-sm text-muted-foreground pl-10">
            Sign in to access your library.
          </p>
        </div>

        <Separator />

        <Card className="p-0 gap-0">
          <CardContent className="p-6">
            <Suspense>
              <LoginForm />
            </Suspense>
          </CardContent>
        </Card>
      </div>
    </main>
  )
}
