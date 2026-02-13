import { apiGet } from "@/lib/api"
import type { MediaItemDetailResponse } from "@/lib/apiTypes"
import MediaDetailClient from "./MediaDetailClient"

type Props = {
    params: Promise<{ id: string }>
}

export default async function MediaDetailPage({ params }: Props) {
    const { id } = await params
    const detail = await apiGet<MediaItemDetailResponse>(`/api/media/${id}`)

    return <MediaDetailClient detail={detail} onBackHref="/library" />
}
