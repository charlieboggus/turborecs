package com.github.charlieboggus.turborecs.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import kotlin.system.measureTimeMillis

data class ClaudeMessageRequest(
    val model: String,
    @JsonProperty("max_tokens")
    val maxTokens: Int,
    val system: String,
    val messages: List<ClaudeMessage>
)

data class ClaudeMessage(
    val role: String,
    val content: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ClaudeMessageResponse(
    val model: String? = null,
    val content: List<ClaudeContentBlock> = emptyList(),
    val usage: ClaudeUsage? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ClaudeContentBlock(
    val type: String? = null,
    val text: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ClaudeUsage(
    @JsonProperty("input_tokens")
    val inputTokens: Int? = null,
    @JsonProperty("output_tokens")
    val outputTokens: Int? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ClaudeErrorResponse(
    val error: ClaudeError? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ClaudeError(
    val type: String? = null,
    val message: String? = null
)

class ClaudeApiException(
    message: String,
    val statusCode: Int?,
    val responseBody: String?
) : RuntimeException(message)

@Service
class ClaudeApiService(
    private val claudeRestClient: RestClient,
    private val objectMapper: ObjectMapper,
    private val props: ClaudeProperties
) {
    private val log = LoggerFactory.getLogger(ClaudeApiService::class.java)

    fun sendMessage(systemPrompt: String, userMessage: String): String {
        val request = ClaudeMessageRequest(
            model = props.model,
            maxTokens = props.maxTokens,
            system = systemPrompt,
            messages = listOf(ClaudeMessage(role = "user", content = userMessage))
        )

        val entity: ResponseEntity<String>
        val elapsedMs = measureTimeMillis {
            entity = claudeRestClient.post()
                .uri("/v1/messages")
                .body(request)
                .retrieve()
                .toEntity(String::class.java)
        }

        val status = entity.statusCode.value()
        val body = entity.body

        if (status !in 200..299) {
            val parsedErr = body?.let { safeParse<ClaudeErrorResponse>(it, "ClaudeErrorResponse") }
            val msg = parsedErr?.error?.message
                ?: run {
                    log.warn("Claude non-2xx with unparseable error body. status={} bodyPreview='{}'",
                        status,
                        body?.take(500)?.replace("\n", "\\n")
                    )
                    "Claude API error"
                }

            throw ClaudeApiException(
                message = "$msg (HTTP $status)",
                statusCode = status,
                responseBody = body
            )
        }

        if (body.isNullOrBlank()) {
            throw ClaudeApiException(
                message = "Claude API returned empty response body",
                statusCode = status,
                responseBody = body
            )
        }

        val parsed = safeParse<ClaudeMessageResponse>(body, "ClaudeMessageResponse")
            ?: throw ClaudeApiException(
                message = "Claude API response JSON could not be parsed",
                statusCode = status,
                responseBody = body
            )

        val text = parsed.content
            .asSequence()
            .filter { it.type == "text" }
            .mapNotNull { it.text }
            .joinToString("\n")
            .trim()

        if (text.isBlank()) {
            throw ClaudeApiException(
                message = "No text content in Claude response",
                statusCode = status,
                responseBody = body
            )
        }

        log.info(
            "Claude call ok - model={}, elapsedMs={}, inputTokens={}, outputTokens={}",
            parsed.model ?: props.model,
            elapsedMs,
            parsed.usage?.inputTokens,
            parsed.usage?.outputTokens
        )

        return text
    }

    private inline fun <reified T> safeParse(json: String, label: String): T? =
        try {
            objectMapper.readValue(json, T::class.java)
        } catch (e: Exception) {
            log.warn(
                "Claude JSON parse failed for {}: {}. bodyPreview='{}'",
                label,
                e.message,
                json.take(500).replace("\n", "\\n")
            )
            null
        }
}