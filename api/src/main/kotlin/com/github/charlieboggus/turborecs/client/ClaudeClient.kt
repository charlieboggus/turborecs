package com.github.charlieboggus.turborecs.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.charlieboggus.turborecs.client.dto.claude.ClaudeErrorResponse
import com.github.charlieboggus.turborecs.client.dto.claude.ClaudeMessage
import com.github.charlieboggus.turborecs.client.dto.claude.ClaudeMessageRequest
import com.github.charlieboggus.turborecs.client.dto.claude.ClaudeMessageResponse
import com.github.charlieboggus.turborecs.client.exception.ClaudeApiException
import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import kotlin.system.measureTimeMillis

@Component
class ClaudeClient(
    private val claudeProperties: ClaudeProperties,
    private val claudeRestClient: RestClient,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun sendMessage(systemPrompt: String, userMessage: String): String {
        val request = ClaudeMessageRequest(
            model = claudeProperties.model,
            maxTokens = 4096,
            system = systemPrompt,
            messages = listOf(ClaudeMessage(role = "user", content = userMessage))
        )

        val responseBody: String
        val responseMs = measureTimeMillis {
            responseBody = try {
                claudeRestClient.post()
                    .uri("/messages")
                    .body(request)
                    .retrieve()
                    .body(String::class.java)
                    ?: throw ClaudeApiException(
                        message = "Claude API returned empty response body",
                        statusCode = null,
                        responseBody = null
                    )
            }
            catch (e: RestClientResponseException) {
                val error = safeParse<ClaudeErrorResponse>(e.responseBodyAsString, "ClaudeErrorResponse")
                throw ClaudeApiException(
                    message = "${error?.error?.message ?: "Claude API error"} (HTTP ${e.statusCode.value()})",
                    statusCode = e.statusCode.value(),
                    responseBody = e.responseBodyAsString
                )
            }
        }

        val parsedBody = safeParse<ClaudeMessageResponse>(responseBody, "ClaudeMessageResponse")
            ?: throw ClaudeApiException(
                message = "Claude API response JSON could not be parsed",
                statusCode = null,
                responseBody = responseBody
            )

        val text = parsedBody.content
            .asSequence()
            .filter { it.type == "text" }
            .mapNotNull { it.text }
            .joinToString("\n")
            .trim()

        if (text.isBlank()) {
            throw ClaudeApiException(
                message = "No text content in Claude response",
                statusCode = null,
                responseBody = responseBody
            )
        }

        log.info(
            "Claude call OK - model={}, responseMs={}, inputTokens={}, outputTokens={}",
            parsedBody.model ?: claudeProperties.model,
            responseMs,
            parsedBody.usage?.inputTokens,
            parsedBody.usage?.outputTokens
        )

        return text
    }

    private inline fun <reified T> safeParse(json: String, label: String): T? {
        return try {
            objectMapper.readValue(json, T::class.java)
        }
        catch (e: Exception) {
            log.debug("Failed to parse {} from response: {}", label, e.message)
            null
        }
    }
}