package com.github.charlieboggus.turborecs.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class TmdbLookupService(
    private val tmdbRestClient: RestClient,
    private val objectMapper: ObjectMapper
) {

    /**
     * Returns TMDB movie id as String, or null if not found.
     */
    fun resolveMovieTmdbId(title: String, year: Int?): String? {
        val q = title.trim()
        if (q.isEmpty()) return null

        // /search/movie?query=...&year=...
        val encoded = URLEncoder.encode(q, StandardCharsets.UTF_8)
        val uri = buildString {
            append("/search/movie?query=").append(encoded)
            if (year != null && year in 1870..2100) append("&year=").append(year)
            append("&include_adult=false")
        }

        val body = tmdbRestClient.get()
            .uri(uri)
            .retrieve()
            .body(String::class.java)
            ?: return null

        val root = objectMapper.readTree(body)
        val results = root.path("results")
        if (!results.isArray || results.isEmpty) return null

        // Prefer exact-ish year match if present, otherwise first result
        if (year != null) {
            val best = results.firstOrNull { node ->
                val release = node.path("release_date").asText("")
                val y = release.take(4).toIntOrNull()
                y == year
            } ?: results[0]
            return best.path("id").takeIf { it.isInt || it.isNumber }?.asText()
        }

        return results[0].path("id").takeIf { it.isInt || it.isNumber }?.asText()
    }
}