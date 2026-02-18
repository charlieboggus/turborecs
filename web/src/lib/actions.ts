"use server"

import { revalidatePath } from "next/cache"
import { cookies } from "next/headers"
import {
  deleteMediaItem,
  excludeMedia,
  getRecommendations,
  logMediaItem,
  rateMediaItem,
  refreshRecommendations,
  removeExclusion,
  searchMedia,
  verifyCredentials,
} from "./api"
import type { MediaType, SearchResult } from "./types"
import { SignJWT } from "jose"

const secret = new TextEncoder().encode(process.env.TURBORECS_JWT_SECRET)

export const loginAction = async (
  username: string,
  password: string,
): Promise<{ error?: string }> => {
  const authHeader = `Basic ${Buffer.from(`${username}:${password}`).toString("base64")}`
  const valid = await verifyCredentials(authHeader)
  if (!valid) {
    return { error: "Invalid credentials" }
  }
  const token = await new SignJWT({})
    .setProtectedHeader({ alg: "HS256" })
    .setIssuedAt()
    .setExpirationTime("7d")
    .sign(secret)

  const cookieStore = await cookies()
  cookieStore.set("auth_session", token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: 60 * 60 * 24 * 7,
  })
  return {}
}

export const logMediaAction = async (
  mediaType: MediaType,
  externalId: string,
  rating?: number,
  consumedAt?: string,
) => {
  const body =
    mediaType === "MOVIE"
      ? { mediaType, tmdbId: externalId, rating, consumedAt }
      : { mediaType, openLibraryId: externalId, rating, consumedAt }
  const result = await logMediaItem(body)
  revalidatePath("/library")
  return result
}

export const rateMediaAction = async (id: string, rating: number) => {
  const result = await rateMediaItem(id, rating)
  revalidatePath("/library")
  revalidatePath(`/library/${id}`)
  return result
}

export const searchAction = async (
  query: string,
  type: MediaType,
): Promise<SearchResult[]> => {
  if (!query.trim()) {
    return []
  }
  return searchMedia(query.trim(), type)
}

export const excludeMediaAction = async (
  mediaType: MediaType,
  title: string,
  year?: number,
  externalId?: string,
  reason?: string,
) => {
  const body = {
    mediaType,
    title,
    year,
    ...(mediaType === "MOVIE"
      ? { tmdbId: externalId }
      : { openLibraryId: externalId }),
    reason,
  }
  const result = await excludeMedia(body)
  revalidatePath("/exclusions")
  return result
}

export const removeExclusionAction = async (id: string) => {
  await removeExclusion(id)
  revalidatePath("/exclusions")
}

export const deleteMediaAction = async (id: string) => {
  await deleteMediaItem(id)
  revalidatePath("/library")
}

export const getRecommendationsAction = async (mediaType?: MediaType) => {
  return getRecommendations(mediaType, true)
}

export const checkCachedRecommendationsAction = async () => {
  return getRecommendations(undefined, false)
}

export const refreshRecommendationsAction = async (mediaType?: MediaType) => {
  const result = await refreshRecommendations(mediaType)
  revalidatePath("/recommendations")
  return result
}
