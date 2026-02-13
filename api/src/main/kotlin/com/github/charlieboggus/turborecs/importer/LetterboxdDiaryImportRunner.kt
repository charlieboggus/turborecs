package com.github.charlieboggus.turborecs.importer

import com.github.charlieboggus.turborecs.config.properties.ImportProperties
import com.github.charlieboggus.turborecs.service.EnrichmentService
import com.github.charlieboggus.turborecs.service.TaggingService
import com.github.charlieboggus.turborecs.service.TmdbLookupService
import org.apache.commons.csv.CSVFormat
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil

@Component
class LetterboxdDiaryImportRunner(
    private val props: ImportProperties,
    private val tmdbLookupService: TmdbLookupService,
    private val txService: LetterboxdDiaryImportTxService,
    private val enrichmentService: EnrichmentService,
    private val taggingService: TaggingService
) : ApplicationRunner {

    data class ImportResult(
        var created: Int = 0,
        var skippedExisting: Int = 0,
        var skippedUnresolved: Int = 0,
        var errors: Int = 0,
        var enriched: Int = 0,
        var tagged: Int = 0
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        if (!props.enabled) {
            return
        }
        val resource = props.path
        require(resource.exists()) { "Diary CSV not found at: ${describe(resource)}" }
        require(resource.isReadable) { "Diary CSV not readable at: ${describe(resource)}" }
        val result = importDiary(
            csv = resource,
            limit = props.limit,
            dryRun = props.dryRun,
            enrich = props.enrich,
            tag = props.tag
        )
        log.info(
            "Letterboxd import finished: created={}, skippedExisting={}, skippedUnresolved={}, errors={}, enriched={}, tagged={}",
            result.created, result.skippedExisting, result.skippedUnresolved, result.errors, result.enriched, result.tagged
        )
    }

    fun importDiary(
        csv: Resource,
        limit: Int,
        dryRun: Boolean,
        enrich: Boolean,
        tag: Boolean
    ): ImportResult {
        require(limit > 0) { "limit must be > 0" }
        val res = ImportResult()
        csv.inputStream.bufferedReader().use { reader ->
            val format = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build()
            val records = format.parse(reader)
            var rowNum = 0
            for (r in records) {
                if (rowNum >= limit) {
                    break
                }
                rowNum++
                val title = r.get("Name")?.trim().orEmpty()
                val year = r.get("Year")?.trim()?.toIntOrNull()
                val watchedRaw = r.get("Watched Date")?.trim()
                val watchedAt = parseWatchedDate(watchedRaw)
                val rating = parseLetterboxdRatingToInt(r.get("Rating")?.trim())
                if (title.isBlank() || watchedAt == null) {
                    res.errors++
                    log.warn(
                        "Skipping row {}: missing title or watched date (title='{}', watchedAt='{}')",
                        rowNum, title, watchedRaw
                    )
                    continue
                }
                try {
                    val tmdbId = tmdbLookupService.resolveMovieTmdbId(title, year)
                    if (tmdbId.isNullOrBlank()) {
                        res.skippedUnresolved++
                        log.warn("Row {} unresolved TMDB id for '{}' ({})", rowNum, title, year)
                        continue
                    }
                    if (dryRun) {
                        res.created++
                        continue
                    }
                    val mediaId: UUID? = txService.importOneMovieRow(
                        tmdbId = tmdbId,
                        title = title,
                        year = year,
                        watchedAt = watchedAt,
                        rating = rating
                    )
                    if (mediaId == null) {
                        res.skippedExisting++
                        continue
                    }
                    res.created++
                    // IMPORTANT: enrichment/tagging AFTER DB tx commits (no network inside tx)
                    if (enrich) {
                        try {
                            enrichmentService.enrichItem(mediaId)
                            res.enriched++
                        }
                        catch (e: Exception) {
                            res.errors++
                            log.warn("Enrichment failed for row {} mediaId={} title='{}': {}", rowNum, mediaId, title, e.message)
                        }
                    }
                    if (tag) {
                        try {
                            taggingService.tagItem(mediaId)
                            res.tagged++
                        }
                        catch (e: Exception) {
                            res.errors++
                            log.warn("Tagging failed for row {} mediaId={} title='{}': {}", rowNum, mediaId, title, e.message)
                        }
                    }
                }
                catch (e: Exception) {
                    res.errors++
                    log.warn(
                        "Failed importing row {} (title='{}', year={}): {}",
                        rowNum, title, year, e.message
                    )
                }
            }
        }
        return res
    }

    // Letterboxd export: "8/18/25" (or sometimes "08/18/2025")
    // This makes 2-digit years map to 2000-2099 (25 -> 2025), which matches your data.
    private val letterboxdMdyy: DateTimeFormatter = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendValue(ChronoField.MONTH_OF_YEAR, 1, 2, java.time.format.SignStyle.NOT_NEGATIVE)
        .appendLiteral('/')
        .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, java.time.format.SignStyle.NOT_NEGATIVE)
        .appendLiteral('/')
        .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
        .toFormatter(Locale.US)

    private val letterboxdMdyyyy: DateTimeFormatter =
        DateTimeFormatter.ofPattern("M/d/uuuu", Locale.US)

    private fun parseWatchedDate(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) {
            return null
        }
        val s = raw.trim()
        return runCatching { LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
            ?: runCatching { LocalDate.parse(s, letterboxdMdyyyy) }.getOrNull()
            ?: runCatching { LocalDate.parse(s, letterboxdMdyy) }.getOrNull()
    }

    /**
     * Letterboxd rating is 0.5 increments out of 5. DB model is Int? 1..5.
     * map by ceiling (4.5 -> 5) and clamp.
     */
    private fun parseLetterboxdRatingToInt(raw: String?): Int? {
        if (raw.isNullOrBlank()) {
            return null
        }
        val d = raw.toDoubleOrNull() ?: return null
        if (d <= 0.0) {
            return null
        }
        return ceil(d).toInt().coerceIn(1, 5)
    }

    private fun describe(r: Resource): String =
        runCatching { r.uri.toString() }.getOrElse { r.description }
}