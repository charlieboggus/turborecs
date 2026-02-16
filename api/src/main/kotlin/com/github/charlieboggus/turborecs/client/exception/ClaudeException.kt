package com.github.charlieboggus.turborecs.client.exception

class ClaudeApiException(
    message: String,
    val statusCode: Int?,
    val responseBody: String?
) : RuntimeException(message)