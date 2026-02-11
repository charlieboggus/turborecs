"use client"

import { useState } from "react"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Library } from "@/components/library"
import { Recommendations } from "@/components/recommendations"
import { Film, BookOpen, Sparkles } from "lucide-react"

export default function Home() {
    const [activeTab, setActiveTab] = useState("library")

    return (
        <main className="mx-auto max-w-7xl px-4 py-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold tracking-tight">
                    🎬 Media Recs
                </h1>
                <p className="mt-1 text-muted-foreground">
                    Track what you watch and read. Discover what to try next.
                </p>
            </div>
            <Tabs value={activeTab} onValueChange={setActiveTab}>
                <TabsList className="mb-6">
                    <TabsTrigger value="library" className="gap-2">
                        <Film className="h-4 w-4" />
                        Library
                    </TabsTrigger>
                    <TabsTrigger value="recommendations" className="gap-2">
                        <Sparkles className="h-4 w-4" />
                        Recommendations
                    </TabsTrigger>
                </TabsList>

                <TabsContent value="library">
                    <Library />
                </TabsContent>

                <TabsContent value="recommendations">
                    <Recommendations />
                </TabsContent>
            </Tabs>
        </main>
    )
}
