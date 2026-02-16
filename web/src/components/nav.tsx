import Link from "next/link"
import { Library, Search, Sparkles, Ban, Home } from "lucide-react"

const NAV_ITEMS = [
  { href: "/", label: "Home", icon: Home },
  { href: "/library", label: "Library", icon: Library },
  { href: "/search", label: "Search", icon: Search },
  { href: "/recommendations", label: "Recommendations", icon: Sparkles },
  { href: "/exclusions", label: "Exclusions", icon: Ban },
] as const

export function Nav({ current }: { current: string }) {
  return (
    <nav className="flex items-center gap-1 mb-8">
      {NAV_ITEMS.map((item) => {
        const isActive = current === item.href
        const Icon = item.icon
        return (
          <Link
            key={item.href}
            href={item.href}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-sm transition-colors ${
              isActive
                ? "bg-muted text-foreground font-medium"
                : "text-muted-foreground hover:text-foreground hover:bg-muted/50"
            }`}
          >
            <Icon className="h-3.5 w-3.5" />
            {item.label}
          </Link>
        )
      })}
    </nav>
  )
}
